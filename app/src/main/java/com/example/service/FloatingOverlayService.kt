package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.data.PreferencesRepository
import com.example.engine.SystemTelemetryEngine
import com.example.model.ModuleConfig
import com.example.model.TeleportAnchor
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NipexLagTheme
import com.example.ui.theme.ObsidianBgDeep
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class FloatingOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        const val ACTION_START = "com.example.action.START_OVERLAY"
        const val ACTION_STOP = "com.example.action.STOP_OVERLAY"
        private const val CHANNEL_ID = "nipex_floating_hud_channel"
        private const val NOTIFICATION_ID = 1201
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isViewAttached = false
    private lateinit var prefsRepo: PreferencesRepository

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        SystemTelemetryEngine.initialize(applicationContext)
        prefsRepo = PreferencesRepository(applicationContext)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelfCleanly()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            SystemTelemetryEngine.refreshOverlayPermission(this)
            SystemTelemetryEngine.setOverlayServiceRunning(false)
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundNotification()
        attachOverlayIfNeeded()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        SystemTelemetryEngine.setOverlayServiceRunning(true)

        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        clampOverlayPosition()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        detachOverlaySafely()
        SystemTelemetryEngine.setOverlayServiceRunning(false)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun stopSelfCleanly() {
        detachOverlaySafely()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun startForegroundNotification() {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val stopIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(getString(R.string.fgs_notification_title))
            .setContentText(getString(R.string.fgs_notification_text))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPending)
            .addAction(0, "STOP HUD", stopPending)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.fgs_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.fgs_channel_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun attachOverlayIfNeeded() {
        if (isViewAttached && overlayView != null) return

        val wm = windowManager ?: return
        val metrics = resources.displayMetrics
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (metrics.widthPixels * 0.76f).roundToInt()
            y = (metrics.heightPixels * 0.16f).roundToInt()
        }
        layoutParams = params

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeViewModelStoreOwner(this@FloatingOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayService)
            setContent {
                NipexLagTheme {
                    FloatingOverlayRoot(
                        onDragBy = { dx, dy -> moveOverlayBy(dx, dy) },
                        onTeleportTo = { anchor -> teleportToAnchor(anchor) },
                        onToggleConfig = { transform ->
                            val next = SystemTelemetryEngine.moduleConfig.value.transform()
                            SystemTelemetryEngine.updateModuleConfig(next)
                            serviceScope.launch { prefsRepo.saveModuleConfig(next) }
                        },
                        onCleanRam = {
                            serviceScope.launch { SystemTelemetryEngine.cleanSystemMemory() }
                        },
                        onCloseOverlay = { stopSelfCleanly() },
                    )
                }
            }
        }

        runCatching {
            wm.addView(composeView, params)
            overlayView = composeView
            isViewAttached = true
        }.onFailure {
            isViewAttached = false
            overlayView = null
        }
    }

    private fun detachOverlaySafely() {
        val view = overlayView
        if (isViewAttached && view != null) {
            runCatching { windowManager?.removeViewImmediate(view) }
        }
        overlayView = null
        isViewAttached = false
    }

    private fun moveOverlayBy(dx: Float, dy: Float) {
        val params = layoutParams ?: return
        val view = overlayView ?: return
        if (!isViewAttached) return
        val metrics = resources.displayMetrics
        val maxX = (metrics.widthPixels - 140).coerceAtLeast(0)
        val maxY = (metrics.heightPixels - 140).coerceAtLeast(0)
        params.x = (params.x + dx.roundToInt()).coerceIn(0, maxX)
        params.y = (params.y + dy.roundToInt()).coerceIn(0, maxY)
        runCatching { windowManager?.updateViewLayout(view, params) }
    }

    private fun teleportToAnchor(anchor: TeleportAnchor) {
        val params = layoutParams ?: return
        val view = overlayView ?: return
        if (!isViewAttached) return
        val metrics = resources.displayMetrics
        params.x = (metrics.widthPixels * anchor.xFraction).roundToInt()
        params.y = (metrics.heightPixels * anchor.yFraction).roundToInt()
        runCatching { windowManager?.updateViewLayout(view, params) }
    }

    private fun clampOverlayPosition() {
        val params = layoutParams ?: return
        val view = overlayView ?: return
        if (!isViewAttached) return
        val metrics = resources.displayMetrics
        params.x = params.x.coerceIn(0, (metrics.widthPixels - 140).coerceAtLeast(0))
        params.y = params.y.coerceIn(0, (metrics.heightPixels - 140).coerceAtLeast(0))
        runCatching { windowManager?.updateViewLayout(view, params) }
    }
}

@Composable
private fun FloatingOverlayRoot(
    onDragBy: (Float, Float) -> Unit,
    onTeleportTo: (TeleportAnchor) -> Unit,
    onToggleConfig: (ModuleConfig.() -> ModuleConfig) -> Unit,
    onCleanRam: () -> Unit,
    onCloseOverlay: () -> Unit,
) {
    val config by SystemTelemetryEngine.moduleConfig.collectAsState()
    val telemetry by SystemTelemetryEngine.telemetry.collectAsState()
    val overlayState by SystemTelemetryEngine.overlayUiState.collectAsState()

    val expanded = overlayState.isOverlayExpanded
    val effectiveAlpha = if (config.ghostEnabled) config.ghostOpacity.coerceIn(0.2f, 1f) else 1f

    LaunchedEffect(config.teleportEnabled, config.teleportAnchor) {
        if (config.teleportEnabled) {
            onTeleportTo(config.teleportAnchor)
        }
    }

    Box(modifier = Modifier.alpha(effectiveAlpha)) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                (scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(180))) togetherWith
                    (scaleOut(tween(140)) + fadeOut(tween(140)))
            },
            label = "overlay_expand_transition",
        ) { isExpanded ->
            if (!isExpanded) {
                FloatingBubbleWidget(
                    fps = telemetry.currentFps,
                    pingMs = telemetry.pingMs,
                    fpsEnabled = config.fpsMonitorEnabled,
                    freezeEnabled = config.freezeEnabled,
                    onClick = { SystemTelemetryEngine.setOverlayExpanded(true) },
                    onDrag = onDragBy,
                )
            } else {
                FloatingControlPanel(
                    config = config,
                    fps = telemetry.currentFps,
                    pingMs = telemetry.pingMs,
                    ramPercent = telemetry.ramUsagePercent,
                    onDrag = onDragBy,
                    onCollapse = { SystemTelemetryEngine.setOverlayExpanded(false) },
                    onClose = onCloseOverlay,
                    onToggleConfig = onToggleConfig,
                    onCleanRam = onCleanRam,
                )
            }
        }
    }
}

@Composable
private fun FloatingBubbleWidget(
    fps: Int,
    pingMs: Int,
    fpsEnabled: Boolean,
    freezeEnabled: Boolean,
    onClick: () -> Unit,
    onDrag: (Float, Float) -> Unit,
) {
    val pulseTransition = rememberInfiniteTransition(label = "bubble_pulse")
    val glowAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bubble_glow",
    )

    val ringColor = if (freezeEnabled) NeonCrimson else ElectricGreen

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF171C28), ObsidianBgDeep),
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.sweepGradient(
                    listOf(
                        NeonCrimson.copy(alpha = glowAlpha),
                        ringColor.copy(alpha = glowAlpha),
                        NeonCrimson.copy(alpha = glowAlpha),
                    )
                ),
                shape = CircleShape,
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .clickable(onClick = onClick),
    ) {
        if (fpsEnabled) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$fps",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (fps >= 50) ElectricGreen else NeonCrimson,
                )
                Text(
                    text = if (pingMs > 0) "${pingMs}ms" else "V12",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    color = TextPrimary,
                )
            }
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_nipex_emblem_1795279576883),
                contentDescription = "NIPEX Floating Bubble",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            )
        }
    }
}

@Composable
private fun FloatingControlPanel(
    config: ModuleConfig,
    fps: Int,
    pingMs: Int,
    ramPercent: Int,
    onDrag: (Float, Float) -> Unit,
    onCollapse: () -> Unit,
    onClose: () -> Unit,
    onToggleConfig: (ModuleConfig.() -> ModuleConfig) -> Unit,
    onCleanRam: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    var ramCleanedFeedback by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(276.dp)
            .clip(shape)
            .background(GlassSurfaceElevated)
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(listOf(NeonCrimson, ElectricGreen)),
                shape = shape,
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Draggable Panel Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                },
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_nipex_emblem_1795279576883),
                contentDescription = "NIPEX V12",
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "NIPEX V12 HUD",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = TextPrimary,
                )
                Text(
                    text = "${fps} FPS  •  ${if (pingMs > 0) "${pingMs}ms" else "--"}  •  RAM $ramPercent%",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = ElectricGreen,
                )
            }
            IconButton(onClick = onCollapse, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.ExpandLess,
                    contentDescription = "Collapse Panel",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Floating HUD",
                    tint = NeonCrimson,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // Quick Utility Module Grid (2 columns)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            OverlayMiniToggle(
                label = "FREEZE",
                icon = Icons.Default.PauseCircle,
                active = config.freezeEnabled,
                accent = NeonCrimson,
                onClick = { onToggleConfig { copy(freezeEnabled = !freezeEnabled) } },
                modifier = Modifier.weight(1f),
            )
            OverlayMiniToggle(
                label = "TELEPORT",
                icon = Icons.Default.GpsFixed,
                active = config.teleportEnabled,
                accent = ElectricGreen,
                onClick = {
                    val values = TeleportAnchor.entries
                    val nextAnchor = values[(config.teleportAnchor.ordinal + 1) % values.size]
                    onToggleConfig { copy(teleportEnabled = true, teleportAnchor = nextAnchor) }
                },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            OverlayMiniToggle(
                label = "GHOST",
                icon = Icons.Default.VisibilityOff,
                active = config.ghostEnabled,
                accent = NeonCrimson,
                onClick = { onToggleConfig { copy(ghostEnabled = !ghostEnabled) } },
                modifier = Modifier.weight(1f),
            )
            OverlayMiniToggle(
                label = "NET BOOST",
                icon = Icons.Default.NetworkCheck,
                active = config.networkBoosterEnabled,
                accent = ElectricGreen,
                onClick = { onToggleConfig { copy(networkBoosterEnabled = !networkBoosterEnabled) } },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            OverlayMiniToggle(
                label = "ANTI-LAG",
                icon = Icons.Default.Memory,
                active = config.antiLagEnabled,
                accent = ElectricGreen,
                onClick = { onToggleConfig { copy(antiLagEnabled = !antiLagEnabled) } },
                modifier = Modifier.weight(1f),
            )
            OverlayMiniToggle(
                label = "FPS HUD",
                icon = Icons.Default.Speed,
                active = config.fpsMonitorEnabled,
                accent = ElectricGreen,
                onClick = { onToggleConfig { copy(fpsMonitorEnabled = !fpsMonitorEnabled) } },
                modifier = Modifier.weight(1f),
            )
        }

        AnimatedVisibility(visible = config.ghostEnabled) {
            Column {
                Text(
                    text = "GHOST OPACITY ${(config.ghostOpacity * 100).roundToInt()}%",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = TextMuted,
                )
                Slider(
                    value = config.ghostOpacity,
                    onValueChange = { onToggleConfig { copy(ghostOpacity = it) } },
                    valueRange = 0.25f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCrimson,
                        activeTrackColor = NeonCrimson,
                        inactiveTrackColor = GlassBorderSubtle,
                    ),
                    modifier = Modifier.height(28.dp),
                )
            }
        }

        // Quick Clean RAM Action
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(NeonCrimson.copy(alpha = 0.22f), ElectricGreen.copy(alpha = 0.22f))
                    )
                )
                .border(1.dp, ElectricGreen.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                .clickable {
                    onCleanRam()
                    ramCleanedFeedback = true
                }
                .padding(vertical = 8.dp),
        ) {
            Icon(
                imageVector = if (ramCleanedFeedback) Icons.Default.Bolt else Icons.Default.CleaningServices,
                contentDescription = "Clean RAM",
                tint = ElectricGreen,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (ramCleanedFeedback) "MEMORY OPTIMIZED" else "QUICK CLEAN RAM",
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = TextPrimary,
            )
        }
    }
}

@Composable
private fun OverlayMiniToggle(
    label: String,
    icon: ImageVector,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (active) accent.copy(alpha = 0.16f) else Color(0xFF0C0F17)
    val border = if (active) accent.copy(alpha = 0.8f) else GlassBorderSubtle
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) accent else TextMuted,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontFamily = PlusJakartaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = if (active) TextPrimary else TextMuted,
            maxLines = 1,
        )
    }
}
