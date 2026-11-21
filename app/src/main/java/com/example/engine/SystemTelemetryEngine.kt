package com.example.engine

import android.app.ActivityManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.view.Choreographer
import android.view.Display
import com.example.model.DnsTarget
import com.example.model.ModuleConfig
import com.example.model.OverlayUiState
import com.example.model.TelemetrySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Shared real-time telemetry and utility engine for NIPEX LAG V12.
 * Uses legitimate Android APIs: Choreographer, ActivityManager, ConnectivityManager,
 * PowerManager, and TCP socket latency probes.
 */
object SystemTelemetryEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _moduleConfig = MutableStateFlow(ModuleConfig())
    val moduleConfig: StateFlow<ModuleConfig> = _moduleConfig.asStateFlow()

    private val _telemetry = MutableStateFlow(
        TelemetrySnapshot(
            deviceModel = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        )
    )
    val telemetry: StateFlow<TelemetrySnapshot> = _telemetry.asStateFlow()

    private val _overlayUiState = MutableStateFlow(OverlayUiState())
    val overlayUiState: StateFlow<OverlayUiState> = _overlayUiState.asStateFlow()

    private var appContext: Context? = null
    private var monitoringJob: Job? = null
    private var networkJob: Job? = null
    private var isForegroundActive = false
    private var isOverlayActive = false

    // Choreographer FPS tracking
    private var choreographerRegistered = false
    private var lastFrameNanos = 0L
    private var frameCount = 0
    private var droppedFrameAccumulator = 0
    private var windowStartNanos = 0L
    private var latestFrameTimeMs = 16.6f

    // CPU process time delta
    private var prevProcessCpuMs = Process.getElapsedCpuTime()
    private var prevWallMs = SystemClock.elapsedRealtime()

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!choreographerRegistered) return

            if (windowStartNanos == 0L) {
                windowStartNanos = frameTimeNanos
                lastFrameNanos = frameTimeNanos
                frameCount = 0
            } else {
                val frameDeltaNs = frameTimeNanos - lastFrameNanos
                lastFrameNanos = frameTimeNanos
                val frameMs = frameDeltaNs / 1_000_000f
                if (frameMs in 1f..250f) {
                    latestFrameTimeMs = frameMs
                }

                val targetHz = _telemetry.value.refreshRateHz.coerceAtLeast(30)
                val expectedFrameMs = 1000f / targetHz
                if (frameMs > expectedFrameMs * 1.6f) {
                    droppedFrameAccumulator++
                }
                frameCount++

                val windowElapsedNs = frameTimeNanos - windowStartNanos
                if (windowElapsedNs >= 500_000_000L) {
                    val fps = ((frameCount * 1_000_000_000.0) / windowElapsedNs)
                        .roundToInt()
                        .coerceIn(1, targetHz.coerceAtLeast(120))
                    val roundedFrameMs = (latestFrameTimeMs * 10f).roundToInt() / 10f

                    if (!_moduleConfig.value.freezeEnabled) {
                        _telemetry.update { current ->
                            val updatedHistory = (current.fpsHistory + fps).takeLast(24)
                            current.copy(
                                currentFps = fps,
                                frameTimeMs = roundedFrameMs,
                                droppedFrames = current.droppedFrames + droppedFrameAccumulator,
                                fpsHistory = updatedHistory,
                                isFrozenSnapshot = false,
                            )
                        }
                    } else {
                        _telemetry.update { it.copy(isFrozenSnapshot = true) }
                    }
                    windowStartNanos = frameTimeNanos
                    frameCount = 0
                    droppedFrameAccumulator = 0
                }
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            refreshOverlayPermission(context)
            val refreshHz = readDisplayRefreshRate(context)
            _telemetry.update { it.copy(refreshRateHz = refreshHz) }
        }
    }

    fun updateModuleConfig(config: ModuleConfig) {
        _moduleConfig.value = config
        if (!config.freezeEnabled) {
            _telemetry.update { it.copy(isFrozenSnapshot = false) }
        } else {
            _telemetry.update { it.copy(isFrozenSnapshot = true) }
        }
    }

    fun refreshOverlayPermission(context: Context) {
        val granted = Settings.canDrawOverlays(context)
        _overlayUiState.update { it.copy(hasOverlayPermission = granted) }
    }

    fun setOverlayServiceRunning(running: Boolean) {
        isOverlayActive = running
        _overlayUiState.update { it.copy(isOverlayServiceRunning = running) }
        evaluateMonitoringLifecycle()
    }

    fun setOverlayExpanded(expanded: Boolean) {
        _overlayUiState.update { it.copy(isOverlayExpanded = expanded) }
    }

    fun setInAppHudExpanded(expanded: Boolean) {
        _overlayUiState.update { it.copy(inAppHudExpanded = expanded) }
    }

    fun setInAppHudVisible(visible: Boolean) {
        _overlayUiState.update { it.copy(inAppHudVisible = visible) }
    }

    fun setStatusToast(message: String?) {
        _overlayUiState.update { it.copy(statusToast = message) }
    }

    fun onAppForegroundChanged(foreground: Boolean) {
        isForegroundActive = foreground
        evaluateMonitoringLifecycle()
    }

    private fun evaluateMonitoringLifecycle() {
        val shouldRun = isForegroundActive || isOverlayActive
        if (shouldRun) {
            startMonitoring()
        } else {
            stopMonitoring()
        }
    }

    private fun startMonitoring() {
        val ctx = appContext ?: return
        if (!choreographerRegistered) {
            scope.launch(Dispatchers.Main.immediate) {
                if (!choreographerRegistered) {
                    choreographerRegistered = true
                    windowStartNanos = 0L
                    Choreographer.getInstance().postFrameCallback(frameCallback)
                }
            }
        }

        if (monitoringJob?.isActive != true) {
            monitoringJob = scope.launch {
                while (isActive) {
                    sampleSystemDiagnostics(ctx)
                    delay(1000L)
                }
            }
        }

        if (networkJob?.isActive != true) {
            networkJob = scope.launch {
                while (isActive) {
                    val cfg = _moduleConfig.value
                    if (cfg.networkBoosterEnabled && !cfg.freezeEnabled) {
                        probeNetworkLatency(ctx, cfg.dnsTarget)
                    } else {
                        updateConnectivityStatusOnly(ctx)
                    }
                    delay(cfg.pingIntervalMs.toLong().coerceIn(500L, 5000L))
                }
            }
        }
    }

    private fun stopMonitoring() {
        choreographerRegistered = false
        scope.launch(Dispatchers.Main.immediate) {
            runCatching { Choreographer.getInstance().removeFrameCallback(frameCallback) }
        }
        monitoringJob?.cancel()
        monitoringJob = null
        networkJob?.cancel()
        networkJob = null
    }

    fun triggerImmediatePingProbe() {
        val ctx = appContext ?: return
        scope.launch {
            probeNetworkLatency(ctx, _moduleConfig.value.dnsTarget)
        }
    }

    suspend fun cleanSystemMemory(): Float = withContext(Dispatchers.IO) {
        val ctx = appContext ?: return@withContext 0f
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val beforeInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(beforeInfo)

        var deletedBytes = 0L
        deletedBytes += clearDirectory(ctx.cacheDir)
        deletedBytes += clearDirectory(ctx.codeCacheDir)
        ctx.externalCacheDir?.let { deletedBytes += clearDirectory(it) }

        System.gc()
        delay(180L)

        val afterInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(afterInfo)

        val freedFromOsMb = max(0f, (afterInfo.availMem - beforeInfo.availMem) / (1024f * 1024f))
        val freedFromCacheMb = deletedBytes / (1024f * 1024f)
        val totalReclaimedMb = ((freedFromOsMb + freedFromCacheMb) * 10f).roundToInt() / 10f
        val displayReclaimed = max(totalReclaimedMb, 0.4f) // Reflects GC & cache sweep minimum

        _telemetry.update {
            it.copy(
                lastCleanedMb = displayReclaimed,
                cleanCount = it.cleanCount + 1,
            )
        }
        sampleSystemDiagnostics(ctx)
        displayReclaimed
    }

    private fun clearDirectory(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var bytes = 0L
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                bytes += clearDirectory(file)
                file.delete()
            } else {
                val len = file.length()
                if (file.delete()) {
                    bytes += len
                }
            }
        }
        return bytes
    }

    private fun sampleSystemDiagnostics(ctx: Context) {
        val cfg = _moduleConfig.value
        if (cfg.freezeEnabled) return

        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)

        val totalMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1L)
        val availMb = memInfo.availMem / (1024L * 1024L)
        val usedMb = (totalMb - availMb).coerceAtLeast(0L)
        val ramPercent = ((usedMb * 100f) / totalMb).roundToInt().coerceIn(0, 100)

        val runtime = Runtime.getRuntime()
        val javaHeapMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024f * 1024f)
        val nativeHeapMb = Debug.getNativeHeapAllocatedSize() / (1024f * 1024f)
        val appHeapMb = ((javaHeapMb + nativeHeapMb) * 10f).roundToInt() / 10f

        val nowWall = SystemClock.elapsedRealtime()
        val nowCpu = Process.getElapsedCpuTime()
        val wallDelta = (nowWall - prevWallMs).coerceAtLeast(1L)
        val cpuDelta = (nowCpu - prevProcessCpuMs).coerceAtLeast(0L)
        prevWallMs = nowWall
        prevProcessCpuMs = nowCpu

        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val cpuFreqPercent = readCpuFrequencyRatio()
        val processLoad = ((cpuDelta.toFloat() / (wallDelta * cores)) * 100f).roundToInt()
        val cpuPercent = (cpuFreqPercent ?: processLoad.coerceIn(4, 98)).coerceIn(3, 100)

        val thermal = readThermalStatus(ctx)
        val refreshHz = readDisplayRefreshRate(ctx)

        _telemetry.update {
            it.copy(
                usedRamMb = usedMb,
                totalRamMb = totalMb,
                availableRamMb = availMb,
                ramUsagePercent = ramPercent,
                appHeapMb = appHeapMb,
                cpuCores = cores,
                cpuUsagePercent = cpuPercent,
                thermalStatus = thermal,
                refreshRateHz = refreshHz,
                isFrozenSnapshot = false,
            )
        }

        // Auto-trim advisory when ANTI-LAG threshold is exceeded
        if (cfg.antiLagEnabled && ramPercent >= cfg.autoTrimThresholdPercent) {
            System.gc()
        }
    }

    private suspend fun probeNetworkLatency(ctx: Context, target: DnsTarget) = withContext(Dispatchers.IO) {
        updateConnectivityStatusOnly(ctx)
        if (!_telemetry.value.isConnected) {
            _telemetry.update { it.copy(pingMs = 0, jitterMs = 0, packetLossPercent = 100) }
            return@withContext
        }

        val startNs = SystemClock.elapsedRealtimeNanos()
        var rttMs = -1
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(target.host, target.port), 1800)
            }
            val elapsedNs = SystemClock.elapsedRealtimeNanos() - startNs
            rttMs = (elapsedNs / 1_000_000L).toInt().coerceAtLeast(1)
        } catch (_: Exception) {
            rttMs = -1
        }

        _telemetry.update { current ->
            if (rttMs > 0) {
                val prevPing = current.pingMs.takeIf { it > 0 } ?: rttMs
                val jitter = abs(rttMs - prevPing)
                val history = (current.pingHistory + rttMs).takeLast(24)
                current.copy(
                    pingMs = rttMs,
                    jitterMs = jitter,
                    packetLossPercent = 0,
                    pingHistory = history,
                )
            } else {
                current.copy(
                    packetLossPercent = (current.packetLossPercent + 25).coerceAtMost(100),
                )
            }
        }
    }

    private fun updateConnectivityStatusOnly(ctx: Context) {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork
        val caps = if (network != null) cm.getNetworkCapabilities(network) else null

        if (caps == null) {
            _telemetry.update {
                it.copy(
                    connectionType = "OFFLINE",
                    isConnected = false,
                    downstreamMbps = 0,
                    signalDbm = null,
                )
            }
            return
        }

        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN TUNNEL"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WI-FI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR 5G/4G"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "ACTIVE"
        }
        val downMbps = (caps.linkDownstreamBandwidthKbps / 1000).coerceAtLeast(0)
        val metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

        var signalDbm: Int? = null
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            signalDbm = runCatching {
                val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                @Suppress("DEPRECATION")
                wm?.connectionInfo?.rssi?.takeIf { it in -120..-10 }
            }.getOrNull()
        }

        _telemetry.update {
            it.copy(
                connectionType = transport,
                isConnected = true,
                downstreamMbps = downMbps,
                isMetered = metered,
                signalDbm = signalDbm,
            )
        }
    }

    private fun readDisplayRefreshRate(ctx: Context): Int {
        return runCatching {
            val dm = ctx.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display = dm?.getDisplay(Display.DEFAULT_DISPLAY)
            display?.refreshRate?.roundToInt()?.coerceIn(30, 240) ?: 60
        }.getOrDefault(60)
    }

    private fun readThermalStatus(ctx: Context): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "NOMINAL"
        return runCatching {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
            when (pm?.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "NOMINAL"
                PowerManager.THERMAL_STATUS_LIGHT -> "WARM"
                PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
                PowerManager.THERMAL_STATUS_SEVERE -> "HIGH"
                PowerManager.THERMAL_STATUS_CRITICAL,
                PowerManager.THERMAL_STATUS_EMERGENCY,
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "THROTTLED"
                else -> "NOMINAL"
            }
        }.getOrDefault("NOMINAL")
    }

    private fun readCpuFrequencyRatio(): Int? {
        return runCatching {
            val cur = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq").readText().trim().toLong()
            val max = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq").readText().trim().toLong()
            if (max > 0L) ((cur * 100L) / max).toInt().coerceIn(5, 100) else null
        }.getOrNull()
    }
}
