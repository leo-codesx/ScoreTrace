package com.fenji.scoretrace.ui.component.kernelsu

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

/** 焦点标签相对普通标签的放大倍数。 */
const val LiquidGlassFocusScale = 1.1f

/**
 * 选中标签的液态玻璃胶囊。
 *
 * [backdrop] 是页面内容录成的背板，胶囊只折射它。
 * 静止时是一层淡色玻璃；按下时透镜、高光、内阴影随 [pressProgress] 淡入。
 *
 * 阴影必须显式传 null：[drawBackdrop] 默认的 [Shadow.Default] 是 24dp 模糊、黑色 10%，
 * 它会画到胶囊外一圈并在底栏里盖住相邻的图标与文字。
 */
fun Modifier.liquidGlassDrop(
    backdrop: Backdrop,
    pressProgress: Float,
    tint: Color,
    shape: Shape,
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        lens(
            refractionHeight = 10.dp.toPx() * pressProgress,
            refractionAmount = 14.dp.toPx() * pressProgress,
            depthEffect = true,
            chromaticAberration = true,
        )
    },
    highlight = { Highlight.Default.copy(alpha = 0.75f * pressProgress) },
    shadow = null,
    innerShadow = { InnerShadow(radius = 8.dp * pressProgress, alpha = pressProgress) },
    onDrawSurface = {
        // 静止时用一层极淡的色罩把液滴压成半透明玻璃，按下时让位给折射与高光
        drawRect(tint, alpha = 1f - pressProgress)
        drawRect(Color.Black.copy(alpha = 0.03f * pressProgress))
    },
)
