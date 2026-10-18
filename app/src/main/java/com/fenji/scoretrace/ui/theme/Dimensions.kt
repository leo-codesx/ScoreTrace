package com.fenji.scoretrace.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 扩展设计系统：统一的间距、圆角与控件尺寸。
 *
 * 新页面 / 新组件一律引用这里的常量，不再写死 dp；既有页面按模块迭代逐步迁移。
 */
object Dimensions {

    // ── 间距（4 / 8 / 12 / 16 / 24 / 32 节奏）
    val SpacingXSmall = 4.dp
    val SpacingSmall = 8.dp
    val SpacingSmallMedium = 12.dp
    val SpacingMedium = 16.dp
    val SpacingLarge = 24.dp
    val SpacingXLarge = 32.dp

    // ── 圆角
    val CornerSmall = 12.dp
    val CornerMedium = 16.dp
    val CornerCard = 20.dp
    val CornerLarge = 28.dp

    // ── 控件尺寸
    val IconBox = 40.dp
    val CircleButton = 40.dp
    val MinTouchTarget = 48.dp
}
