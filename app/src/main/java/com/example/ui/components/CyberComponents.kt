package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassCardHighlight
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.ObsidianBgDeep
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlin.math.max

@Composable
fun GlassCyberCard(
    modifier: Modifier = Modifier,
    glowAccent: Color = ElectricGreen,
    isHighlighted: Boolean = false,
    cornerRadius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = if (isHighlighted) {
        Brush.linearGradient(
            colors = listOf(
                glowAccent.copy(alpha = 0.85f),
                GlassBorderSubtle,
                glowAccent.copy(alpha = 0.45f),
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                GlassCardHighlight,
                GlassBorderSubtle,
                Color(0x0DFFFFFF),
            )
        )
    }

    Column(
        modifier = modifier
            .drawBehind {
                if (isHighlighted) {
                    drawRoundRect(
                        color = glowAccent.copy(alpha = 0.10f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx()),
                    )
                }
            }
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xD9151926),
                        GlassSurface,
                    )
                )
            )
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun CyberToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color = ElectricGreen,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "switch_thumb",
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) accentColor.copy(alpha = 0.24f) else Color(0xFF1A1F2E),
        animationSpec = tween(220),
        label = "switch_track",
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) accentColor else GlassBorderSubtle,
        animationSpec = tween(220),
        label = "switch_border",
    )

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(trackColor)
                .border(1.2.dp, borderColor, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (checked) accentColor else TextMuted),
            )
        }
    }
}

@Composable
fun CyberRangeSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    accentColor: Color = ElectricGreen,
    testTag: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextMuted,
                fontFamily = JetBrainsMonoFontFamily,
            )
            Text(
                text = valueText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontFamily = JetBrainsMonoFontFamily,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color(0xFF1D2334),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
        )
    }
}

@Composable
fun NeonPulseDot(
    color: Color,
    isAnimating: Boolean = true,
    size: Dp = 9.dp,
) {
    val transition = rememberInfiniteTransition(label = "pulse_dot")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = if (isAnimating) alpha else 0.7f)),
    )
}

@Composable
fun CyberCircularGauge(
    value: Int,
    maxValue: Int,
    unitLabel: String,
    titleLabel: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val targetFraction = (value.toFloat() / maxValue.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "gauge_sweep",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(124.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

            drawArc(
                color = Color(0xFF191F2E),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(NeonCrimson, accentColor, accentColor),
                ),
                startAngle = 135f,
                sweepAngle = 270f * animatedFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$value",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = TextPrimary,
            )
            Text(
                text = unitLabel,
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = accentColor,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = titleLabel,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                color = TextMuted,
            )
        }
    }
}

@Composable
fun CyberSparklineChart(
    samples: List<Int>,
    accentColor: Color,
    minValue: Int = 0,
    maxValue: Int = 120,
    modifier: Modifier = Modifier,
) {
    val safeSamples = if (samples.size < 2) listOf(minValue, minValue) else samples
    val computedMax = max(maxValue, (safeSamples.maxOrNull() ?: maxValue))

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianBgDeep.copy(alpha = 0.7f))
            .padding(horizontal = 6.dp, vertical = 8.dp),
    ) {
        val count = safeSamples.size
        val stepX = size.width / (count - 1).coerceAtLeast(1)
        val range = (computedMax - minValue).coerceAtLeast(1).toFloat()

        val linePath = Path()
        val fillPath = Path()

        safeSamples.forEachIndexed { index, sample ->
            val x = index * stepX
            val normalized = ((sample - minValue) / range).coerceIn(0f, 1f)
            val y = size.height - (normalized * size.height)
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (index == count - 1) {
                fillPath.lineTo(x, size.height)
                fillPath.close()
            }
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(accentColor.copy(alpha = 0.30f), Color.Transparent),
            ),
        )
        drawPath(
            path = linePath,
            color = accentColor,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
