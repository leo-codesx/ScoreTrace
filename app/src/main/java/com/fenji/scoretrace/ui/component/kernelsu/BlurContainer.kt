package com.fenji.scoretrace.ui.component.kernelsu

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * KernelSU 风格模糊容器。
 *
 * Haze 通过 [hazeChild] 采样 [hazeState] 对应的背板内容并绘制模糊；
 * 未开启模糊或没有 HazeState 时退化为普通半透明/不透明容器。
 *
 * 低端设备（低内存机或 ≤4 核）按半分辨率采样并收窄模糊半径，帧时间优先于观感。
 */
@OptIn(ExperimentalHazeApi::class)
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
    val context = LocalContext.current
    val lowEndDevice = remember(context) { isLowEndDevice(context) }
    val effectModifier = if (blurEnabled && hazeState != null) {
        Modifier.hazeChild(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = backgroundColor,
                tint = HazeTint(backgroundColor.copy(alpha = 0.72f)),
                blurRadius = if (lowEndDevice) blurRadius.coerceAtMost(8.dp) else blurRadius,
                noiseFactor = 0.04f,
            ),
        ) {
            inputScale = if (lowEndDevice) HazeInputScale.Fixed(0.5f) else HazeInputScale.Auto
        }
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

private fun isLowEndDevice(context: Context): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    return activityManager?.isLowRamDevice == true || Runtime.getRuntime().availableProcessors() <= 4
}
