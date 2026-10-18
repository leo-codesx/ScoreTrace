package com.fenji.scoretrace.ui.component.kernelsu

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.fenji.scoretrace.ui.theme.ScoreTraceColors

/**
 * 液态玻璃按压反馈：按下时轻微收缩（约 78/56 的视觉比例），
 * 并用一层渐变高光描边模拟折射。真正的高斯背板模糊由 [BlurContainer] 的 Haze 完成。
 */
@Composable
fun Modifier.liquidGlass(
    enabled: Boolean,
    pressed: Boolean,
    shape: Shape = RoundedCornerShape(18.dp),
): Modifier {
    if (!enabled) return this
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "liquidGlassScale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.55f),
                    Color.White.copy(alpha = 0.08f),
                    ScoreTraceColors.AccentCyan.copy(alpha = 0.35f),
                ),
            ),
            shape = shape,
        )
}
