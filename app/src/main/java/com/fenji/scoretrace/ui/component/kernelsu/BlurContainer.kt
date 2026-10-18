package com.fenji.scoretrace.ui.component.kernelsu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * KernelSU 风格模糊容器。
 *
 * Haze 通过 [hazeChild] 采样 [hazeState] 对应的背板内容并绘制模糊；
 * 未开启模糊或没有 HazeState 时退化为普通半透明/不透明容器。
 */
@Composable
fun BlurContainer(
    hazeState: HazeState?,
    blurEnabled: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    blurRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val effectModifier = if (blurEnabled && hazeState != null) {
        Modifier.hazeChild(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = backgroundColor,
                tint = HazeTint(backgroundColor.copy(alpha = 0.72f)),
                blurRadius = blurRadius,
                noiseFactor = 0.04f,
            ),
        )
    } else {
        Modifier.background(backgroundColor)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(effectModifier),
        content = content,
    )
}
