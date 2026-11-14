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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.ui.theme.LocalAppDarkTheme
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 胶囊底栏本体高度；外层另有上下各 8dp 留白，Scaffold bottomBar 总高 80dp。 */
private val BarHeight = 64.dp

/** 全圆角胶囊。 */
private val BarShape = RoundedCornerShape(32.dp)

/** 单个导航项宽度：底栏宽度贴合内容 = 项宽 × 项数 + 左右内边距。 */
private val ItemWidth = 64.dp
private val MaxBarWidth = 360.dp
private val ContentPadding = 4.dp

/** 选中液滴：固定 56×40，居中于导航项下方，不随项宽拉伸。 */
private val IndicatorWidth = 56.dp
private val IndicatorHeight = 40.dp
private val IndicatorShape = RoundedCornerShape(20.dp)

/** 按住时液滴放大 78/56，松开回落到 1。 */
private const val PressedIndicatorScale = 78f / 56f

/** 选中标签相对未选中标签的放大倍数。 */
private const val SelectedTabScale = 1.1f

private val IndicatorSpring = spring<Float>(
    dampingRatio = 0.72f,
    stiffness = 480f,
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
 * KernelSU 风格悬浮胶囊底栏。
 *
 * 自下而上三层：
 * 1. 胶囊玻璃：Haze 采样页面内容做高斯模糊，再垫一层半透明底色；
 * 2. 选中液滴：固定 56×40 的折射玻璃，按住时放大到 78/56，横向用 spring 滑动；
 * 3. 导航项：图标 + 文字常驻，选中项放大 1.1 并换主题色。
 *
 * 液滴固定尺寸并画在导航项之下，不参与触摸，因此不会出现背景覆盖图标/文字或指示器宽度失控。
 * 底栏宽度贴合内容（[ItemWidth] × 项数），不铺满全宽。
 *
 * @param hazeState 页面内容注册的 Haze 采样源；[enableBlur] 为 false 时只画半透明底色。
 * @param backdrop 页面内容录制的液态玻璃背板；为空时液滴退化为主题色半透明块。
 */
@Composable
fun FloatingBottomBar(
    items: List<NavigationItem>,
    currentRoute: String?,
    onSelect: (NavigationItem) -> Unit,
    hazeState: HazeState,
    enableBlur: Boolean,
    backdrop: Backdrop?,
    enableLiquidGlass: Boolean,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val isDark = LocalAppDarkTheme.current
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }
    val lastIndex = items.lastIndex
    val density = LocalDensity.current
    val barWidth = (ItemWidth * items.size + ContentPadding * 2).coerceAtMost(MaxBarWidth)
    val itemWidth = (barWidth - ContentPadding * 2) / items.size
    val itemWidthPx = with(density) { itemWidth.toPx() }
    // 液滴相对胶囊左缘的起始偏移：内边距 + 项内居中留白
    val indicatorStartPx = with(density) {
        (ContentPadding + (itemWidth - IndicatorWidth) / 2).toPx()
    }

    val indicatorIndex = remember { Animatable(selectedIndex.coerceAtLeast(0).toFloat()) }
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    val pressedTabs = remember { mutableStateListOf<Int>() }

    val pressProgress = animateFloatAsState(
        targetValue = if (dragging || pressedTabs.isNotEmpty()) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "floatingBarPress",
    )

    // 点击/滑动切换或系统返回栈变化后，液滴都从当前位置弹到新选中项
    LaunchedEffect(selectedIndex, items.size) {
        if (selectedIndex >= 0) indicatorIndex.animateTo(selectedIndex.toFloat(), IndicatorSpring)
    }

    val hazeStyle = remember(isDark) {
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
    val barColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
    } else {
        Color.White.copy(alpha = 0.62f)
    }
    val glassTint = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }
    val shadowAlpha = if (isDark) 0.30f else 0.12f

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
                .width(barWidth)
                .height(BarHeight)
                .dropShadow(
                    shape = BarShape,
                    shadow = Shadow(radius = 16.dp, color = Color.Black, alpha = shadowAlpha),
                )
                .clip(BarShape)
                .then(
                    if (enableBlur) {
                        Modifier.hazeBlur(input = HazeInput.Sources(hazeState), style = hazeStyle)
                    } else {
                        Modifier
                    },
                )
                .background(barColor),
        ) {
            if (selectedIndex >= 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset {
                            IntOffset(
                                (indicatorIndex.value * itemWidthPx + indicatorStartPx).roundToInt(),
                                0,
                            )
                        }
                        .size(IndicatorWidth, IndicatorHeight)
                        .graphicsLayer {
                            val scale = 1f + (PressedIndicatorScale - 1f) * pressProgress.value
                            scaleX = scale
                            scaleY = scale
                        }
                        .then(
                            if (enableLiquidGlass && backdrop != null) {
                                Modifier.liquidIndicator(
                                    backdrop = backdrop,
                                    shape = IndicatorShape,
                                    tint = glassTint,
                                    press = pressProgress,
                                )
                            } else {
                                Modifier
                                    .clip(IndicatorShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                            },
                        ),
                )
            }

            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = ContentPadding)
                    .pointerInput(items.size, itemWidthPx) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragging = true },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    indicatorIndex.snapTo(
                                        (indicatorIndex.value + dragAmount / itemWidthPx)
                                            .coerceIn(0f, lastIndex.toFloat()),
                                    )
                                }
                            },
                            onDragEnd = {
                                dragging = false
                                scope.launch {
                                    val target = indicatorIndex.value
                                        .roundToInt()
                                        .coerceIn(0, lastIndex)
                                    indicatorIndex.animateTo(target.toFloat(), IndicatorSpring)
                                    if (target != currentSelected) currentOnSelect(currentItems[target])
                                }
                            },
                            onDragCancel = {
                                dragging = false
                                scope.launch {
                                    indicatorIndex.animateTo(
                                        currentSelected.coerceAtLeast(0).toFloat(),
                                        IndicatorSpring,
                                    )
                                }
                            },
                        )
                    },
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
                            .width(itemWidth)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

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
    val contentScale = animateFloatAsState(
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
                scaleX = contentScale.value
                scaleY = contentScale.value
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

/**
 * 液滴的液态玻璃：只折射页面背板，按下时透镜/高光/内阴影随 [press] 淡入。
 * 阴影显式传 null：默认阴影会画到液滴外一圈，在底栏里盖住相邻图标。
 */
private fun Modifier.liquidIndicator(
    backdrop: Backdrop,
    shape: Shape,
    tint: Color,
    press: State<Float>,
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        val progress = press.value
        lens(
            refractionHeight = 12.dp.toPx() * progress,
            refractionAmount = 16.dp.toPx() * progress,
            depthEffect = true,
            chromaticAberration = true,
        )
    },
    highlight = { Highlight.Default.copy(alpha = 0.8f * press.value) },
    shadow = null,
    innerShadow = { InnerShadow(radius = 8.dp * press.value, alpha = press.value) },
    onDrawSurface = {
        drawRect(tint, alpha = 1f - press.value)
        drawRect(Color.Black.copy(alpha = 0.04f * press.value))
    },
)
