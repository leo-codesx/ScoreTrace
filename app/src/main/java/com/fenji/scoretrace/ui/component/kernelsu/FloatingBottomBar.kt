package com.fenji.scoretrace.ui.component.kernelsu

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.ui.navigation.BottomBarTab
import com.fenji.scoretrace.ui.navigation.Screen
import dev.chrisbanes.haze.HazeState

/**
 * KernelSU 风格悬浮底栏：圆角 pill + 选中胶囊 + 可选 Haze 背板模糊/液态玻璃。
 * 外层总高度与 LightBottomBar 保持一致（52dp pill + 上下各 6dp + 系统导航栏）。
 */
@Composable
fun FloatingBottomBar(
    tabs: List<BottomBarTab>,
    currentRoute: String?,
    onSelect: (Screen) -> Unit,
    hazeState: HazeState?,
    blurEnabled: Boolean,
    liquidGlassEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        BlurContainer(
            hazeState = hazeState,
            blurEnabled = blurEnabled,
            shape = RoundedCornerShape(26.dp),
            backgroundColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab ->
                    FloatingBarItem(
                        tab = tab,
                        selected = tab.screen.route == currentRoute,
                        liquidGlassEnabled = liquidGlassEnabled,
                        onClick = { onSelect(tab.screen) },
                        modifier = Modifier.weight(1f),
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
    liquidGlassEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val background by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
        } else {
            Color.Transparent
        },
        label = "floatingBarItemBackground",
    )
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .then(
                Modifier.liquidGlass(
                    enabled = liquidGlassEnabled && selected,
                    pressed = pressed,
                    shape = shape,
                ),
            )
            .clip(shape)
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 4.dp),
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
