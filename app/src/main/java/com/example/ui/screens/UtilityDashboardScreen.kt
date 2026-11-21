package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.DnsTarget
import com.example.model.ModuleConfig
import com.example.model.OverlayUiState
import com.example.model.TelemetrySnapshot
import com.example.model.TeleportAnchor
import com.example.ui.components.CyberRangeSlider
import com.example.ui.components.CyberToggleSwitch
import com.example.ui.components.GlassCyberCard
import com.example.ui.components.NeonPulseDot
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.ObsidianBgDeep
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun UtilityDashboardScreen(
    config: ModuleConfig,
    telemetry: TelemetrySnapshot,
    overlayState: OverlayUiState,
    isCleaningMemory: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onToggleOverlayService: () -> Unit,
    onToggleInAppHud: () -> Unit,
    onFreezeToggle: (Boolean) -> Unit,
    onFreezeHoldChange: (Int) -> Unit,
    onTeleportToggle: (Boolean) -> Unit,
    onTeleportAnchorSelect: (TeleportAnchor) -> Unit,
    onTeleportSensitivityChange: (Float) -> Unit,
    onGhostToggle: (Boolean) -> Unit,
    onGhostOpacityChange: (Float) -> Unit,
    onGhostAutoFadeToggle: (Boolean) -> Unit,
    onNetworkBoosterToggle: (Boolean) -> Unit,
    onPingIntervalChange: (Int) -> Unit,
    onDnsTargetSelect: (DnsTarget) -> Unit,
    onProbeNetworkNow: () -> Unit,
    onAntiLagToggle: (Boolean) -> Unit,
    onAutoTrimChange: (Int) -> Unit,
    onCleanMemoryNow: () -> Unit,
    onFpsMonitorToggle: (Boolean) -> Unit,
    onTargetFpsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
        ) {
            // Floating Window Control Center
            item {
                FloatingWindowControlCard(
                    overlayState = overlayState,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onToggleOverlayService = onToggleOverlayService,
                    onToggleInAppHud = onToggleInAppHud,
                )
            }

            // 1. FREEZE: Telemetry Snapshot Lock & Touch Guard
            item {
                UtilityModuleCard(
                    title = "FREEZE",
                    subtitle = "Telemetry Snapshot Lock & Screen-On Guard",
                    icon = Icons.Default.PauseCircle,
                    accent = NeonCrimson,
                    enabled = config.freezeEnabled,
                    statusTag = if (config.freezeEnabled) "LOCKED" else "LIVE",
                    switchTag = "switch_freeze",
                    onToggle = onFreezeToggle,
                ) {
                    Text(
                        text = "Freezes real-time FPS & ping chart sampling for frame inspection and keeps the display awake.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberRangeSlider(
                        label = "SNAPSHOT HOLD WINDOW",
                        valueText = "${config.freezeHoldMs} ms",
                        value = config.freezeHoldMs.toFloat(),
                        valueRange = 500f..5000f,
                        onValueChange = { onFreezeHoldChange(it.roundToInt()) },
                        accentColor = NeonCrimson,
                        testTag = "slider_freeze_hold",
                    )
                }
            }

            // 2. TELEPORT: Overlay Coordinate Anchor & Quick-Snap
            item {
                UtilityModuleCard(
                    title = "TELEPORT",
                    subtitle = "Overlay Coordinate Anchor & Quick-Snap",
                    icon = Icons.Default.GpsFixed,
                    accent = ElectricGreen,
                    enabled = config.teleportEnabled,
                    statusTag = config.teleportAnchor.label,
                    switchTag = "switch_teleport",
                    onToggle = onTeleportToggle,
                ) {
                    Text(
                        text = "Instantly snaps the floating bubble to calibrated screen edge presets without obstructing controls.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        TeleportAnchor.entries.forEach { anchor ->
                            SelectorChip(
                                label = anchor.label,
                                selected = config.teleportAnchor == anchor,
                                accent = ElectricGreen,
                                onClick = { onTeleportAnchorSelect(anchor) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    CyberRangeSlider(
                        label = "EDGE SNAP MAGNETISM",
                        valueText = "${(config.teleportSnapSensitivity * 100).roundToInt()}%",
                        value = config.teleportSnapSensitivity,
                        valueRange = 0.2f..1.0f,
                        onValueChange = onTeleportSensitivityChange,
                        accentColor = ElectricGreen,
                        testTag = "slider_teleport_snap",
                    )
                }
            }

            // 3. GHOST: Low-Profile Translucent Overlay Mode
            item {
                UtilityModuleCard(
                    title = "GHOST",
                    subtitle = "Low-Profile Translucent Overlay Mode",
                    icon = Icons.Default.VisibilityOff,
                    accent = NeonCrimson,
                    enabled = config.ghostEnabled,
                    statusTag = "${(config.ghostOpacity * 100).roundToInt()}% ALPHA",
                    switchTag = "switch_ghost",
                    onToggle = onGhostToggle,
                ) {
                    Text(
                        text = "Reduces floating bubble & panel opacity to minimize visual footprint during active sessions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberRangeSlider(
                        label = "STEALTH OPACITY",
                        valueText = "${(config.ghostOpacity * 100).roundToInt()}%",
                        value = config.ghostOpacity,
                        valueRange = 0.25f..1.0f,
                        onValueChange = onGhostOpacityChange,
                        accentColor = NeonCrimson,
                        testTag = "slider_ghost_opacity",
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "IDLE DIMMING",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = TextMuted,
                        )
                        SelectorChip(
                            label = if (config.ghostAutoFade) "AUTO-FADE ON" else "AUTO-FADE OFF",
                            selected = config.ghostAutoFade,
                            accent = NeonCrimson,
                            onClick = { onGhostAutoFadeToggle(!config.ghostAutoFade) },
                        )
                    }
                }
            }

            // 4. NETWORK BOOSTER: Live TCP Latency Probe & Link Monitor
            item {
                UtilityModuleCard(
                    title = "NETWORK BOOSTER",
                    subtitle = "Live TCP Latency Probe & Link Monitor",
                    icon = Icons.Default.NetworkCheck,
                    accent = ElectricGreen,
                    enabled = config.networkBoosterEnabled,
                    statusTag = if (telemetry.pingMs > 0) "${telemetry.pingMs} ms" else telemetry.connectionType,
                    switchTag = "switch_network_booster",
                    onToggle = onNetworkBoosterToggle,
                ) {
                    Text(
                        text = "Actively measures TCP handshake RTT, jitter, and active transport quality (${telemetry.connectionType}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        DnsTarget.entries.forEach { dns ->
                            SelectorChip(
                                label = dns.label,
                                selected = config.dnsTarget == dns,
                                accent = ElectricGreen,
                                onClick = { onDnsTargetSelect(dns) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    CyberRangeSlider(
                        label = "PROBE CADENCE",
                        valueText = "${config.pingIntervalMs} ms",
                        value = config.pingIntervalMs.toFloat(),
                        valueRange = 500f..5000f,
                        onValueChange = { onPingIntervalChange(it.roundToInt()) },
                        accentColor = ElectricGreen,
                        testTag = "slider_ping_interval",
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onProbeNetworkNow,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGreen.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_probe_network"),
                    ) {
                        Text(
                            text = stringResource(R.string.btn_probe_network) + " (${config.dnsTarget.host})",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            // 5. ANTI-LAG: RAM Cache Cleaner & Auto-Trim Engine
            item {
                UtilityModuleCard(
                    title = "ANTI-LAG",
                    subtitle = "RAM Cache Cleaner & Auto-Trim Engine",
                    icon = Icons.Default.Memory,
                    accent = ElectricGreen,
                    enabled = config.antiLagEnabled,
                    statusTag = "RAM ${telemetry.ramUsagePercent}%",
                    switchTag = "switch_anti_lag",
                    onToggle = onAntiLagToggle,
                ) {
                    Text(
                        text = "Sweeps app cache buffers and triggers garbage collection when system memory pressure crosses threshold.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberRangeSlider(
                        label = "AUTO-TRIM THRESHOLD",
                        valueText = "${config.autoTrimThresholdPercent}% RAM",
                        value = config.autoTrimThresholdPercent.toFloat(),
                        valueRange = 65f..95f,
                        onValueChange = { onAutoTrimChange(it.roundToInt()) },
                        accentColor = ElectricGreen,
                        testTag = "slider_ram_trim",
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onCleanMemoryNow,
                        enabled = !isCleaningMemory,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricGreen,
                            contentColor = ObsidianBgDeep,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_clean_memory_utility"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCleaningMemory) "SWEEPING MEMORY..." else stringResource(R.string.btn_clean_memory),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            // 6. FPS MONITOR: Live Choreographer Frame-Rate Telemetry
            item {
                UtilityModuleCard(
                    title = "FPS MONITOR",
                    subtitle = "Live Choreographer Frame-Rate Telemetry",
                    icon = Icons.Default.Speed,
                    accent = CyberCyan,
                    enabled = config.fpsMonitorEnabled,
                    statusTag = "${telemetry.currentFps} FPS",
                    switchTag = "switch_fps_monitor",
                    onToggle = onFpsMonitorToggle,
                ) {
                    Text(
                        text = "Measures real hardware vsync frame intervals (${telemetry.frameTimeMs} ms) and displays live FPS inside the floating bubble.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberRangeSlider(
                        label = "TARGET FPS ALERT LINE",
                        valueText = "${config.targetFpsThreshold} FPS",
                        value = config.targetFpsThreshold.toFloat(),
                        valueRange = 30f..120f,
                        onValueChange = { onTargetFpsChange(it.roundToInt()) },
                        accentColor = CyberCyan,
                        testTag = "slider_target_fps",
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingWindowControlCard(
    overlayState: OverlayUiState,
    onRequestOverlayPermission: () -> Unit,
    onToggleOverlayService: () -> Unit,
    onToggleInAppHud: () -> Unit,
) {
    GlassCyberCard(
        glowAccent = if (overlayState.isOverlayServiceRunning) ElectricGreen else NeonCrimson,
        isHighlighted = true,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = if (overlayState.isOverlayServiceRunning) ElectricGreen else NeonCrimson,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "FLOATING WINDOW ENGINE",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NeonPulseDot(
                            color = if (overlayState.isOverlayServiceRunning) ElectricGreen else NeonCrimson,
                            size = 7.dp,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                overlayState.isOverlayServiceRunning -> "SYSTEM OVERLAY ACTIVE"
                                overlayState.hasOverlayPermission -> "OVERLAY READY"
                                else -> "PERMISSION REQUIRED"
                            },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = if (overlayState.hasOverlayPermission) ElectricGreen else NeonCrimson,
                        )
                    }
                }
            }

            SelectorChip(
                label = if (overlayState.inAppHudVisible) "IN-APP HUD ON" else "IN-APP HUD OFF",
                selected = overlayState.inAppHudVisible,
                accent = CyberCyan,
                onClick = onToggleInAppHud,
                modifier = Modifier.testTag("in_app_hud_toggle"),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!overlayState.hasOverlayPermission) {
            Text(
                text = stringResource(R.string.overlay_permission_desc),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onRequestOverlayPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCrimson,
                    contentColor = TextPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("grant_overlay_button"),
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_grant_overlay),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        } else {
            Button(
                onClick = onToggleOverlayService,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (overlayState.isOverlayServiceRunning) NeonCrimson else ElectricGreen,
                    contentColor = if (overlayState.isOverlayServiceRunning) TextPrimary else ObsidianBgDeep,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("overlay_service_button"),
            ) {
                Text(
                    text = if (overlayState.isOverlayServiceRunning) {
                        stringResource(R.string.btn_stop_overlay)
                    } else {
                        stringResource(R.string.btn_launch_overlay)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun UtilityModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    enabled: Boolean,
    statusTag: String,
    switchTag: String,
    onToggle: (Boolean) -> Unit,
    expandedContent: @Composable () -> Unit,
) {
    GlassCyberCard(
        glowAccent = accent,
        isHighlighted = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (enabled) accent.copy(alpha = 0.16f) else Color(0xFF151926))
                    .border(
                        width = 1.dp,
                        color = if (enabled) accent.copy(alpha = 0.6f) else GlassBorderSubtle,
                        shape = RoundedCornerShape(12.dp),
                    ),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (enabled) accent else TextMuted,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (enabled) accent.copy(alpha = 0.18f) else ObsidianBgDeep)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = statusTag,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = if (enabled) accent else TextMuted,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }

            CyberToggleSwitch(
                checked = enabled,
                onCheckedChange = onToggle,
                accentColor = accent,
                testTag = switchTag,
            )
        }

        AnimatedVisibility(visible = enabled) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                expandedContent()
            }
        }
    }
}

@Composable
private fun SelectorChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) accent.copy(alpha = 0.18f) else ObsidianBgDeep)
            .border(1.dp, if (selected) accent else GlassBorderSubtle, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = if (selected) accent else TextMuted,
        )
    }
}
