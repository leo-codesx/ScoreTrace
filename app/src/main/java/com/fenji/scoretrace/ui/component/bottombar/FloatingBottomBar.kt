package com.fenji.scoretrace.ui.component.bottombar

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.fenji.scoretrace.ui.theme.LocalAppDarkTheme
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 胶囊高度 64dp；外层上下各 8dp 留白，Scaffold bottomBar 合计 80dp。 */
private val BarHeight = 64.dp

/** 全圆角胶囊。 */
private val BarShape = RoundedCornerShape(32.dp)

/** 单个导航项宽度，胶囊宽度 = 项宽 × 项数 + 左右内边距（贴合内容，不铺满全宽）。 */
private val ItemWidth = 64.dp
private val ContentPadding = 4.dp

/** 选中指示器：固定 56×40，画在图标层下方。 */
private val IndicatorWidth = 56.dp
private val IndicatorHeight = 40.dp
private val IndicatorShape = RoundedCornerShape(20.dp)

/** 选中态图标/文字缩放。 */
private const val SelectedTabScale = 1.1f

/** 按住底栏时的整体缩放。 */
private const val PressedBarScale = 0.98f

private val IndicatorSpring = spring<Float>(
    dampingRatio = 0.8f,
    stiffness = 400f,
)

/**
 * 底部导航项。图标用 [Painter]（core 图标走 rememberVectorPainter，缺失图标走本地矢量 drawable）。
 * 标 @Immutable 让底栏在按压/切换时按下标跳过未变化的条目。
 */
@Immutable
data class NavigationItem(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: Painter,
)

/**
 * 悬浮胶囊底栏（Compose 原生实现，唯一采样源是 Haze）。
 *
 * 自下而上三层：
 * 1. 胶囊：Haze 20dp 背板模糊 + 半透明垫底色；模糊关闭时退化为纯 Surface 色；
 * 2. 选中指示器：56×40 圆角块，蓝青主题色 15% 渐变；液态玻璃开启时叠加 Haze 模糊；
 * 3. 导航项：图标 + 文字，选中项放大 1.1f 并换主题色。
 *
 * 指示器与导航项都放在同一个 [Box] 里，靠绘制顺序 + zIndex 保证指示器永远在图标之下，
 * 不做任何折射/色散着色器，因此不会出现色块串色或遮挡文字。
 *
 * @param hazeState 页面内容注册的 Haze 采样源；[enableBlur] 为 false 时只画纯色底。
 * @param enableLiquidGlass 指示器是否叠加 Haze 模糊（关闭后为纯半透明色块，低端机零额外开销）。
 */
@Composable
fun FloatingBottomBar(
    items: List<NavigationItem>,
    currentRoute: String?,
    onSelect: (NavigationItem) -> Unit,
    hazeState: HazeState,
    enableBlur: Boolean,
    enableLiquidGlass: Boolean,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val isDark = LocalAppDarkTheme.current
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }
    val lastIndex = items.lastIndex
    val density = LocalDensity.current
    val itemWidthPx = with(density) { ItemWidth.toPx() }
    // 指示器相对胶囊左缘的起始偏移：内边距 + 项内居中留白
    val indicatorStartPx = with(density) {
        ContentPadding.toPx() + (ItemWidth - IndicatorWidth).toPx() / 2f
    }

    val indicatorIndex = remember { Animatable(selectedIndex.coerceAtLeast(0).toFloat()) }
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    var dragIndex by remember { mutableFloatStateOf(0f) }
    val pressedTabs = remember { mutableStateListOf<Int>() }

    // 点击切换 / 系统返回栈变化后，指示器从当前位置弹到新选中项
    LaunchedEffect(selectedIndex, items.size) {
        if (selectedIndex >= 0) indicatorIndex.animateTo(selectedIndex.toFloat(), IndicatorSpring)
    }

    val barScale by animateFloatAsState(
        targetValue = if (dragging || pressedTabs.isNotEmpty()) PressedBarScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "floatingBarPressScale",
    )

    val barHazeStyle = remember(isDark) {
        HazeBlurStyle {
            blurRadius(20.dp)
            noiseFactor(0.02f)
            backgroundColor(Color.Transparent)
            colorEffects(
                listOf(
                    HazeColorEffect.tint(
                        if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.22f),
                    ),
                ),
            )
        }
    }
    val indicatorHazeStyle = remember {
        HazeBlurStyle {
            blurRadius(12.dp)
            noiseFactor(0.01f)
            backgroundColor(Color.Transparent)
        }
    }

    // 模糊关闭时用不透明 Surface 色，避免半透明底在纯色页面上显得发灰
    val barSurface = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val barColor = if (enableBlur) barSurface.copy(alpha = 0.6f) else barSurface

    val indicatorBrush = Brush.horizontalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            ScoreTraceColors.AccentCyan.copy(alpha = 0.15f),
        ),
    )

    val currentSelected by rememberUpdatedState(selectedIndex)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentItems by rememberUpdatedState(items)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .graphicsLayer {
                    scaleX = barScale
                    scaleY = barScale
                }
                .shadow(elevation = 8.dp, shape = BarShape, clip = false)
                .clip(BarShape)
                .then(
                    if (enableBlur) {
                        Modifier.hazeBlur(
                            input = HazeInput.Sources(hazeState),
                            style = barHazeStyle,
                            performanceMode = HazePerformanceMode.Adaptive,
                        )
                    } else {
                        Modifier
                    },
                )
                .background(barColor),
        ) {
            if (selectedIndex >= 0) {
                // 指示器：绘制层级最低，不参与触摸
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset {
                            IntOffset(
                                (indicatorStartPx + indicatorFraction(dragging, dragIndex, indicatorIndex) * itemWidthPx)
                                    .roundToInt(),
                                0,
                            )
                        }
                        .size(IndicatorWidth, IndicatorHeight)
                        .zIndex(0f)
                        .clip(IndicatorShape)
                        .then(
                            if (enableLiquidGlass && enableBlur) {
                                Modifier.hazeBlur(
                                    input = HazeInput.Sources(hazeState),
                                    style = indicatorHazeStyle,
                                    performanceMode = HazePerformanceMode.Adaptive,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .background(indicatorBrush),
                )
            }

            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .height(BarHeight)
                    .zIndex(1f)
                    .pointerInput(items.size, itemWidthPx) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragIndex = indicatorIndex.value
                                dragging = true
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragIndex = (dragIndex + dragAmount / itemWidthPx)
                                    .coerceIn(0f, lastIndex.toFloat())
                            },
                            onDragEnd = {
                                val from = dragIndex
                                val target = from.roundToInt().coerceIn(0, lastIndex)
                                dragging = false
                                scope.launch {
                                    // 先对齐手指停下的位置，再弹到最近的一项，避免松手瞬间跳回旧位置
                                    indicatorIndex.snapTo(from)
                                    indicatorIndex.animateTo(target.toFloat(), IndicatorSpring)
                                }
                                if (target != currentSelected) currentOnSelect(currentItems[target])
                            },
                            onDragCancel = {
                                val from = dragIndex
                                dragging = false
                                scope.launch {
                                    indicatorIndex.snapTo(from)
                                    indicatorIndex.animateTo(
                                        currentSelected.coerceAtLeast(0).toFloat(),
                                        IndicatorSpring,
                                    )
                                }
                            },
                        )
                    }
                    .padding(horizontal = ContentPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    NavigationTab(
                        item = item,
                        selected = index == selectedIndex,
                        onClick = { if (index != selectedIndex) onSelect(item) },
                        onPressedChange = { down ->
                            if (down) {
                                if (index !in pressedTabs) pressedTabs.add(index)
                            } else {
                                pressedTabs.remove(index)
                            }
                        },
                        modifier = Modifier
                            .width(ItemWidth)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

/** 拖拽中跟随手指，否则用弹簧动画值。 */
private fun indicatorFraction(dragging: Boolean, dragIndex: Float, animated: Animatable<Float, *>): Float =
    if (dragging) dragIndex else animated.value

@Composable
private fun NavigationTab(
    item: NavigationItem,
    selected: Boolean,
    onClick: () -> Unit,
    onPressedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    LaunchedEffect(pressed) { onPressedChange(pressed) }
    DisposableEffect(Unit) { onDispose { onPressedChange(false) } }

    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 200),
        label = "floatingBarTabColor",
    )
    val contentScale by animateFloatAsState(
        targetValue = if (selected) SelectedTabScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "floatingBarTabScale",
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .graphicsLayer {
                scaleX = contentScale
                scaleY = contentScale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Icon(
            painter = item.icon,
            contentDescription = stringResource(item.labelRes),
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = stringResource(item.labelRes),
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
        )
    }
}
