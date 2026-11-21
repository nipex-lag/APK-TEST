package com.example.model

enum class KeyTier(val badgeLabel: String, val validityLabel: String) {
    VIP_PRO("VIP PRO V12", "LIFETIME EDITION"),
    ELITE_V12("ELITE V12", "PRIORITY BUILD"),
    TRIAL_24H("24H VIP TRIAL", "INSTANT ACCESS"),
}

sealed interface AuthState {
    data object Loading : AuthState
    data class Unauthenticated(val errorMessage: String? = null) : AuthState
    data object Verifying : AuthState
    data class Authenticated(
        val keyCode: String,
        val tier: KeyTier,
    ) : AuthState
}

enum class UtilityModuleId {
    FREEZE,
    TELEPORT,
    GHOST,
    NETWORK_BOOSTER,
    ANTI_LAG,
    FPS_MONITOR,
}

enum class TeleportAnchor(val label: String, val xFraction: Float, val yFraction: Float) {
    TOP_LEFT("TOP-L", 0.04f, 0.14f),
    TOP_RIGHT("TOP-R", 0.82f, 0.14f),
    MID_LEFT("MID-L", 0.04f, 0.46f),
    MID_RIGHT("MID-R", 0.82f, 0.46f),
    BOT_LEFT("BOT-L", 0.04f, 0.78f),
    BOT_RIGHT("BOT-R", 0.82f, 0.78f),
}

enum class DnsTarget(val host: String, val port: Int, val label: String) {
    CLOUDFLARE("1.1.1.1", 443, "CF 1.1.1.1"),
    GOOGLE("8.8.8.8", 53, "GG 8.8.8.8"),
    QUAD9("9.9.9.9", 443, "Q9 9.9.9.9"),
}

data class ModuleConfig(
    // FREEZE: Telemetry Snapshot Lock & Touch Guard (Keep Screen On)
    val freezeEnabled: Boolean = false,
    val freezeHoldMs: Int = 2000,

    // TELEPORT: Overlay Coordinate Anchor & Quick-Snap
    val teleportEnabled: Boolean = false,
    val teleportAnchor: TeleportAnchor = TeleportAnchor.TOP_RIGHT,
    val teleportSnapSensitivity: Float = 0.85f,

    // GHOST: Low-Profile Translucent Overlay Mode
    val ghostEnabled: Boolean = false,
    val ghostOpacity: Float = 0.48f,
    val ghostAutoFade: Boolean = true,

    // NETWORK BOOSTER: Live TCP Latency Probe & Link Monitor
    val networkBoosterEnabled: Boolean = true,
    val pingIntervalMs: Int = 1500,
    val dnsTarget: DnsTarget = DnsTarget.CLOUDFLARE,

    // ANTI-LAG: RAM Cache Cleaner & Auto-Trim Engine
    val antiLagEnabled: Boolean = true,
    val autoTrimThresholdPercent: Int = 80,

    // FPS MONITOR: Real-Time Choreographer Frame-Rate Telemetry
    val fpsMonitorEnabled: Boolean = true,
    val targetFpsThreshold: Int = 60,
)

data class TelemetrySnapshot(
    val currentFps: Int = 60,
    val frameTimeMs: Float = 16.6f,
    val refreshRateHz: Int = 60,
    val droppedFrames: Int = 0,
    val fpsHistory: List<Int> = listOf(58, 60, 59, 60, 60),
    val pingMs: Int = 0,
    val jitterMs: Int = 0,
    val packetLossPercent: Int = 0,
    val pingHistory: List<Int> = listOf(24, 22, 25, 21, 23),
    val connectionType: String = "CHECKING",
    val downstreamMbps: Int = 0,
    val signalDbm: Int? = null,
    val isConnected: Boolean = true,
    val isMetered: Boolean = false,
    val usedRamMb: Long = 0L,
    val totalRamMb: Long = 0L,
    val availableRamMb: Long = 0L,
    val ramUsagePercent: Int = 0,
    val appHeapMb: Float = 0f,
    val lastCleanedMb: Float = 0f,
    val cleanCount: Int = 0,
    val cpuCores: Int = Runtime.getRuntime().availableProcessors(),
    val cpuUsagePercent: Int = 0,
    val thermalStatus: String = "NOMINAL",
    val deviceModel: String = "",
    val androidVersion: String = "",
    val isFrozenSnapshot: Boolean = false,
)

data class OverlayUiState(
    val isOverlayServiceRunning: Boolean = false,
    val isOverlayExpanded: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val inAppHudVisible: Boolean = true,
    val inAppHudExpanded: Boolean = false,
    val statusToast: String? = null,
)
