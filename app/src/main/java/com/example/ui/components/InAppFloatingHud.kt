package com.example.ui.components

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ModuleConfig
import com.example.model.TelemetrySnapshot
import com.example.model.TeleportAnchor
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.ObsidianBgDeep
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlin.math.roundToInt

/**
 * Interactive In-App Draggable Floating Bubble & Expandable Panel.
 * Mirrors the system-wide FloatingOverlayService state seamlessly.
 */
@Composable
fun InAppFloatingHud(
    visible: Boolean,
    expanded: Boolean,
    config: ModuleConfig,
    telemetry: TelemetrySnapshot,
    onToggleExpanded: () -> Unit,
    onHideHud: () -> Unit,
    onToggleFreeze: (Boolean) -> Unit,
    onCycleTeleport: () -> Unit,
    onToggleGhost: (Boolean) -> Unit,
    onToggleNetBooster: (Boolean) -> Unit,
    onToggleAntiLag: (Boolean) -> Unit,
    onToggleFpsMonitor: (Boolean) -> Unit,
    onCleanMemory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val bubblePx = with(density) { 64.dp.toPx() }
        val panelWidthPx = with(density) { 264.dp.toPx() }
        val panelHeightPx = with(density) { 250.dp.toPx() }

        var offsetX by remember { mutableFloatStateOf((maxWidthPx - bubblePx - 32f).coerceAtLeast(16f)) }
        var offsetY by remember { mutableFloatStateOf((maxHeightPx * 0.22f).coerceAtLeast(16f)) }

        LaunchedEffect(config.teleportEnabled, config.teleportAnchor, maxWidthPx, maxHeightPx) {
            if (config.teleportEnabled) {
                offsetX = (maxWidthPx * config.teleportAnchor.xFraction)
                    .coerceIn(12f, (maxWidthPx - bubblePx - 12f).coerceAtLeast(12f))
                offsetY = (maxHeightPx * config.teleportAnchor.yFraction)
                    .coerceIn(12f, (maxHeightPx - bubblePx - 12f).coerceAtLeast(12f))
            }
        }

        val currentWidthPx = if (expanded) panelWidthPx else bubblePx
        val currentHeightPx = if (expanded) panelHeightPx else bubblePx
        val clampedX = offsetX.coerceIn(12f, (maxWidthPx - currentWidthPx - 12f).coerceAtLeast(12f))
        val clampedY = offsetY.coerceIn(12f, (maxHeightPx - currentHeightPx - 12f).coerceAtLeast(12f))

        val effectiveAlpha = if (config.ghostEnabled) config.ghostOpacity.coerceIn(0.25f, 1f) else 1f

        Box(
            modifier = Modifier
                .offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
                .alpha(effectiveAlpha),
        ) {
            AnimatedContent(
                targetState = expanded,
                transitionSpec = {
                    (scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(180))) togetherWith
                        (scaleOut(tween(130)) + fadeOut(tween(130)))
                },
                label = "in_app_hud_expand",
            ) { isExpanded ->
                if (!isExpanded) {
                    val pulse = rememberInfiniteTransition(label = "inapp_bubble_pulse")
                    val glowAlpha by pulse.animateFloat(
                        initialValue = 0.45f,
                        targetValue = 0.95f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1300, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "inapp_bubble_glow",
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(58.dp)
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
                                        (if (config.freezeEnabled) NeonCrimson else ElectricGreen).copy(alpha = glowAlpha),
                                        NeonCrimson.copy(alpha = glowAlpha),
                                    )
                                ),
                                shape = CircleShape,
                            )
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    offsetX = (offsetX + dragAmount.x).coerceIn(
                                        12f,
                                        (maxWidthPx - bubblePx - 12f).coerceAtLeast(12f),
                                    )
                                    offsetY = (offsetY + dragAmount.y).coerceIn(
                                        12f,
                                        (maxHeightPx - bubblePx - 12f).coerceAtLeast(12f),
                                    )
                                }
                            }
                            .clickable(onClick = onToggleExpanded)
                            .testTag("in_app_floating_bubble"),
                    ) {
                        if (config.fpsMonitorEnabled) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${telemetry.currentFps}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (telemetry.currentFps >= 50) ElectricGreen else NeonCrimson,
                                )
                                Text(
                                    text = if (telemetry.pingMs > 0) "${telemetry.pingMs}ms" else "HUD",
                                    fontFamily = OrbitronFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    color = TextPrimary,
                                )
                            }
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_nipex_emblem_1795279576883),
                                contentDescription = "Floating HUD Bubble",
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape),
                            )
                        }
                    }
                } else {
                    val shape = RoundedCornerShape(20.dp)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .width(264.dp)
                            .clip(shape)
                            .background(GlassSurfaceElevated)
                            .border(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(listOf(NeonCrimson, ElectricGreen)),
                                shape = shape,
                            )
                            .padding(12.dp)
                            .testTag("in_app_floating_panel"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        offsetX = (offsetX + dragAmount.x).coerceIn(
                                            12f,
                                            (maxWidthPx - panelWidthPx - 12f).coerceAtLeast(12f),
                                        )
                                        offsetY = (offsetY + dragAmount.y).coerceIn(
                                            12f,
                                            (maxHeightPx - panelHeightPx - 12f).coerceAtLeast(12f),
                                        )
                                    }
                                },
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_nipex_emblem_1795279576883),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "NIPEX HUD",
                                    fontFamily = OrbitronFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "${telemetry.currentFps} FPS • ${telemetry.ramUsagePercent}% RAM",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = ElectricGreen,
                                )
                            }
                            IconButton(onClick = onToggleExpanded, modifier = Modifier.size(30.dp)) {
                                Icon(
                                    imageVector = Icons.Default.ExpandLess,
                                    contentDescription = "Collapse Floating Panel",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            IconButton(onClick = onHideHud, modifier = Modifier.size(30.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hide In-App HUD",
                                    tint = NeonCrimson,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            HudMiniButton(
                                label = "FREEZE",
                                icon = Icons.Default.PauseCircle,
                                active = config.freezeEnabled,
                                accent = NeonCrimson,
                                onClick = { onToggleFreeze(!config.freezeEnabled) },
                                modifier = Modifier.weight(1f),
                            )
                            HudMiniButton(
                                label = config.teleportAnchor.label,
                                icon = Icons.Default.GpsFixed,
                                active = config.teleportEnabled,
                                accent = ElectricGreen,
                                onClick = onCycleTeleport,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            HudMiniButton(
                                label = "GHOST",
                                icon = Icons.Default.VisibilityOff,
                                active = config.ghostEnabled,
                                accent = NeonCrimson,
                                onClick = { onToggleGhost(!config.ghostEnabled) },
                                modifier = Modifier.weight(1f),
                            )
                            HudMiniButton(
                                label = "NET BOOST",
                                icon = Icons.Default.NetworkCheck,
                                active = config.networkBoosterEnabled,
                                accent = ElectricGreen,
                                onClick = { onToggleNetBooster(!config.networkBoosterEnabled) },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            HudMiniButton(
                                label = "ANTI-LAG",
                                icon = Icons.Default.Memory,
                                active = config.antiLagEnabled,
                                accent = ElectricGreen,
                                onClick = { onToggleAntiLag(!config.antiLagEnabled) },
                                modifier = Modifier.weight(1f),
                            )
                            HudMiniButton(
                                label = "FPS HUD",
                                icon = Icons.Default.Speed,
                                active = config.fpsMonitorEnabled,
                                accent = ElectricGreen,
                                onClick = { onToggleFpsMonitor(!config.fpsMonitorEnabled) },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElectricGreen.copy(alpha = 0.16f))
                                .border(1.dp, ElectricGreen.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .clickable(onClick = onCleanMemory)
                                .padding(vertical = 8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = "Clean Memory",
                                tint = ElectricGreen,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CLEAN RAM NOW",
                                fontFamily = OrbitronFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = TextPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HudMiniButton(
    label: String,
    icon: ImageVector,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) accent.copy(alpha = 0.16f) else ObsidianBgDeep)
            .border(1.dp, if (active) accent.copy(alpha = 0.8f) else GlassBorderSubtle, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) accent else TextMuted,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(5.dp))
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
