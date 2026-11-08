package com.fenji.scoretrace.ui.component.kernelsu

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.ui.navigation.BottomBarTab
import com.fenji.scoretrace.ui.navigation.Screen
import com.fenji.scoretrace.ui.theme.LocalAppDarkTheme
import com.kyant.backdrop.Backdrop
import dev.chrisbanes.haze.HazeState
import kotlin.math.roundToInt

/** 胶囊底栏高度：未变，切开关或切页面都不会让内容跳动。 */
private val BarHeight = 52.dp

/** 单个标签宽度：底栏宽度贴合内容 = 标签宽 × 标签数，最多 [MaxBarWidth]。 */
private val ItemWidth = 64.dp
private val MaxBarWidth = 360.dp

/** 胶囊内左右留白：可见标签与选中指示器必须用同一个值，否则指示器会与标签错位。 */
private val ContentPadding = 4.dp

private val BarShape = RoundedCornerShape(26.dp)
private val IndicatorShape = RoundedCornerShape(22.dp)

/**
 * KernelSU 风格悬浮 Pill 底栏。
 *
 * 自下而上三层：
 * 1. 胶囊玻璃：[BlurContainer] 采样页面内容做轻模糊，容器色垫底；
 * 2. 选中指示器：固定为一格大小并裁剪，横向动画滑动；开液态玻璃时用折射胶囊，否则用主题色半透明胶囊；
 * 3. 可见标签：正常字号，焦点标签放大 [LiquidGlassFocusScale]。
 *
 * 指示器必须画在标签之下、且被裁剪到格子内：底栏是 52dp 的窄条，
 * 任何越界绘制的模糊/阴影都会盖住相邻标签和上方页面的内容。
 */
@Composable
fun FloatingBottomBar(
    tabs: List<BottomBarTab>,
    currentRoute: String?,
    onSelect: (Screen) -> Unit,
    hazeState: HazeState?,
    screenBackdrop: Backdrop?,
    blurEnabled: Boolean,
    liquidGlassEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = tabs.indexOfFirst { it.screen.route == currentRoute }
    val barWidth = (ItemWidth * tabs.size).coerceAtMost(MaxBarWidth)
    val itemWidth = (barWidth - ContentPadding * 2) / tabs.size
    val itemWidthPx = with(LocalDensity.current) { itemWidth.toPx() }
    val glassBackdrop = if (liquidGlassEnabled && blurEnabled) screenBackdrop else null

    // 按下态在底栏层聚合：一个标签一个 InteractionSource，任一标签的释放都会把 pressedIndex 复位，
    // 不会像逐项 LaunchedEffect 回调那样出现「松手了但按下态还挂着」的残留。
    val interactionSources = remember(tabs.size) { List(tabs.size) { MutableInteractionSource() } }
    val pressedIndex = interactionSources.indexOfFirst { it.collectIsPressedAsState().value }

    // 切换标签时指示器横向滑过去，而不是瞬移
    val indicatorIndex = animateFloatAsState(
        targetValue = selectedIndex.coerceAtLeast(0).toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "floatingBarIndicatorIndex",
    )

    val pressProgress by animateFloatAsState(
        targetValue = if (selectedIndex >= 0 && pressedIndex == selectedIndex) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "floatingBarPress",
    )

    val glassTint = if (LocalAppDarkTheme.current) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(BarHeight),
        ) {
            BlurContainer(
                hazeState = hazeState,
                blurEnabled = blurEnabled,
                shape = BarShape,
                backgroundColor = MaterialTheme.colorScheme.surface,
                blurRadius = 4.dp,
                modifier = Modifier.matchParentSize(),
            ) {}

            if (selectedIndex >= 0) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(horizontal = ContentPadding),
                ) {
                    val cell = Modifier
                        .offset { IntOffset((indicatorIndex.value * itemWidthPx).roundToInt(), 0) }
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(IndicatorShape)
                    if (glassBackdrop != null) {
                        Box(
                            modifier = cell.liquidGlassDrop(
                                backdrop = glassBackdrop,
                                pressProgress = pressProgress,
                                tint = glassTint,
                                shape = IndicatorShape,
                            ),
                        )
                    } else {
                        Box(
                            modifier = cell.background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                            ),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = ContentPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == selectedIndex
                    FloatingBarItem(
                        tab = tab,
                        selected = selected,
                        contentScale = if (selected) LiquidGlassFocusScale else 1f,
                        interactionSource = interactionSources[index],
                        onClick = { onSelect(tab.screen) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingBarItem(
    tab: BottomBarTab,
    selected: Boolean,
    contentScale: Float,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 200),
        label = "floatingBarContentColor",
    )

    Box(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                scaleX = contentScale
                scaleY = contentScale
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                painter = tab.icon,
                contentDescription = stringResource(tab.labelRes),
                tint = contentColor,
                modifier = Modifier.size(21.dp),
            )
            Text(
                text = stringResource(tab.labelRes),
                fontSize = 10.sp,
                color = contentColor,
            )
        }
    }
}
