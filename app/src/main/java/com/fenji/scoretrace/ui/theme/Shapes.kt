package com.fenji.scoretrace.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/** 列表 / 功能卡片：统一 20dp 圆角。 */
val CardShape = RoundedCornerShape(Dimensions.CornerCard)

/** 次级卡片 / 输入类容器：16dp 圆角。 */
val ContainerShape = RoundedCornerShape(Dimensions.CornerMedium)

/** 小图标底 / 标签底：12dp 圆角。 */
val SmallContainerShape = RoundedCornerShape(Dimensions.CornerSmall)

/** Material 3 全局形状，供 MaterialTheme 使用。 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimensions.SpacingSmall),
    small = SmallContainerShape,
    medium = ContainerShape,
    large = CardShape,
    extraLarge = RoundedCornerShape(Dimensions.CornerLarge),
)
