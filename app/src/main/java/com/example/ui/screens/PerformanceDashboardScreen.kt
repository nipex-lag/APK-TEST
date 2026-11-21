package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AuthState
import com.example.model.ModuleConfig
import com.example.model.TelemetrySnapshot
import com.example.ui.components.CyberCircularGauge
import com.example.ui.components.CyberSparklineChart
import com.example.ui.components.GlassCyberCard
import com.example.ui.components.NeonPulseDot
import com.example.ui.theme.CyberAmber
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

@Composable
fun PerformanceDashboardScreen(
    telemetry: TelemetrySnapshot,
    config: ModuleConfig,
    authState: AuthState.Authenticated,
    isCleaningMemory: Boolean,
    onCleanMemoryNow: () -> Unit,
    onProbeNetworkNow: () -> Unit,
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
            // 1. Live FPS & Frame Time Telemetry Card
            item {
                val fpsColor = when {
                    telemetry.currentFps >= config.targetFpsThreshold -> ElectricGreen
                    telemetry.currentFps >= 40 -> CyberAmber
                    else -> NeonCrimson
                }
                GlassCyberCard(
                    glowAccent = if (telemetry.isFrozenSnapshot) NeonCrimson else fpsColor,
                    isHighlighted = true,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = fpsColor,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "REAL-TIME FRAME TELEMETRY",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NeonPulseDot(
                                color = if (telemetry.isFrozenSnapshot) NeonCrimson else ElectricGreen,
                                isAnimating = !telemetry.isFrozenSnapshot,
                                size = 8.dp,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (telemetry.isFrozenSnapshot) "FROZEN" else "${telemetry.refreshRateHz}Hz PANEL",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = if (telemetry.isFrozenSnapshot) NeonCrimson else ElectricGreen,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        CyberCircularGauge(
                            value = telemetry.currentFps,
                            maxValue = telemetry.refreshRateHz.coerceAtLeast(60),
                            unitLabel = "FPS",
                            titleLabel = "${telemetry.frameTimeMs} ms",
                            accentColor = fpsColor,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            DiagnosticRow("TARGET THRESHOLD", "${config.targetFpsThreshold} FPS", CyberCyan)
                            DiagnosticRow("FRAME INTERVAL", "${telemetry.frameTimeMs} ms", TextPrimary)
                            DiagnosticRow(
                                "JANK / DROPPED",
                                "${telemetry.droppedFrames} frames",
                                if (telemetry.droppedFrames > 5) NeonCrimson else ElectricGreen,
                            )
                            DiagnosticRow("DISPLAY REFRESH", "${telemetry.refreshRateHz} Hz", TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    CyberSparklineChart(
                        samples = telemetry.fpsHistory,
                        accentColor = fpsColor,
                        minValue = 0,
                        maxValue = telemetry.refreshRateHz.coerceAtLeast(60),
                    )
                }
            }

            // 2. Network Latency & Connectivity Card
            item {
                val pingColor = when {
                    !telemetry.isConnected -> NeonCrimson
                    telemetry.pingMs in 1..60 -> ElectricGreen
                    telemetry.pingMs in 61..120 -> CyberAmber
                    else -> NeonCrimson
                }
                GlassCyberCard(
                    glowAccent = pingColor,
                    isHighlighted = telemetry.isConnected,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NetworkCheck,
                                contentDescription = null,
                                tint = pingColor,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NETWORK & LATENCY MONITOR",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(pingColor.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = telemetry.connectionType,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = pingColor,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MetricTile(
                            label = "PING RTT",
                            value = if (telemetry.pingMs > 0) "${telemetry.pingMs} ms" else "--",
                            accent = pingColor,
                            modifier = Modifier.weight(1f),
                        )
                        MetricTile(
                            label = "JITTER",
                            value = "${telemetry.jitterMs} ms",
                            accent = CyberCyan,
                            modifier = Modifier.weight(1f),
                        )
                        MetricTile(
                            label = "BANDWIDTH",
                            value = if (telemetry.downstreamMbps > 0) "${telemetry.downstreamMbps} Mbps" else "--",
                            accent = ElectricGreen,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DNS ${config.dnsTarget.host}  •  LOSS ${telemetry.packetLossPercent}%" +
                                    (telemetry.signalDbm?.let { "  •  $it dBm" } ?: ""),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = TextSecondary,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    CyberSparklineChart(
                        samples = telemetry.pingHistory,
                        accentColor = pingColor,
                        minValue = 0,
                        maxValue = 150,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onProbeNetworkNow,
                        border = androidx.compose.foundation.BorderStroke(1.dp, pingColor.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = pingColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("perf_ping_probe_button"),
                    ) {
                        Text(
                            text = stringResource(R.string.btn_probe_network),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            // 3. System Memory, CPU & Thermal Optimizer Card
            item {
                val ramColor = when {
                    telemetry.ramUsagePercent >= 85 -> NeonCrimson
                    telemetry.ramUsagePercent >= 70 -> CyberAmber
                    else -> ElectricGreen
                }
                GlassCyberCard(
                    glowAccent = ramColor,
                    isHighlighted = true,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = ramColor,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MEMORY & CPU DIAGNOSTICS",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeviceThermostat,
                                contentDescription = null,
                                tint = if (telemetry.thermalStatus == "NOMINAL") ElectricGreen else CyberAmber,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = telemetry.thermalStatus,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = if (telemetry.thermalStatus == "NOMINAL") ElectricGreen else CyberAmber,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "SYSTEM RAM USAGE",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = TextMuted,
                        )
                        Text(
                            text = "${telemetry.usedRamMb} / ${telemetry.totalRamMb} MB (${telemetry.ramUsagePercent}%)",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = ramColor,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (telemetry.ramUsagePercent / 100f).coerceIn(0f, 1f) },
                        color = ramColor,
                        trackColor = Color(0xFF1A2030),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MetricTile(
                            label = "FREE RAM",
                            value = "${telemetry.availableRamMb} MB",
                            accent = ElectricGreen,
                            modifier = Modifier.weight(1f),
                        )
                        MetricTile(
                            label = "CPU (${telemetry.cpuCores}C)",
                            value = "${telemetry.cpuUsagePercent}%",
                            accent = CyberCyan,
                            modifier = Modifier.weight(1f),
                        )
                        MetricTile(
                            label = "APP HEAP",
                            value = "${telemetry.appHeapMb} MB",
                            accent = TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (telemetry.cleanCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Last sweep reclaimed ${telemetry.lastCleanedMb} MB  •  Total sweeps: ${telemetry.cleanCount}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = ElectricGreen,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                            .testTag("perf_clean_memory_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCleaningMemory) "OPTIMIZING MEMORY..." else stringResource(R.string.btn_clean_memory),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            // 4. Hardware & License Spec Sheet
            item {
                GlassCyberCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "HARDWARE & LICENSE SPECS",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    DiagnosticRow("DEVICE", telemetry.deviceModel, TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    DiagnosticRow("OS BUILD", telemetry.androidVersion, TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    DiagnosticRow("LICENSE TIER", authState.tier.badgeLabel, ElectricGreen)
                    Spacer(modifier = Modifier.height(6.dp))
                    DiagnosticRow("KEY ID", authState.keyCode, CyberCyan)
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String, valueColor: Color) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = TextMuted,
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = valueColor,
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianBgDeep.copy(alpha = 0.78f))
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
    ) {
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = accent,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontFamily = OrbitronFontFamily,
            fontSize = 9.sp,
            color = TextMuted,
            maxLines = 1,
        )
    }
}
