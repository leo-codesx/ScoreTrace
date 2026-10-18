package com.fenji.scoretrace.ui.screen.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.theme.CardShape
import com.fenji.scoretrace.ui.theme.ContainerShape
import com.fenji.scoretrace.ui.theme.Dimensions
import com.fenji.scoretrace.ui.theme.ScoreTraceColors

/** KernelSU 风格主题设置：主题模式 + 模糊 / 悬浮底栏 / 液态玻璃 / 预测性返回开关。 */
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit,
    viewModel: ThemeSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ThemeTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimensions.SpacingMedium)
                    .padding(bottom = Dimensions.SpacingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(Dimensions.SpacingSmallMedium))
                ThemePreviewCard()
                Spacer(modifier = Modifier.height(Dimensions.SpacingLarge))

                ThemeModeSelector(
                    current = state.themeMode,
                    onSelect = viewModel::setThemeMode,
                )

                Spacer(modifier = Modifier.height(Dimensions.SpacingMedium))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CardShape)
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    KernelSwitchRow(
                        iconRes = R.drawable.ic_kernel_blur,
                        title = "模糊",
                        subtitle = "启用顶栏和底栏的模糊效果",
                        checked = state.enableBlur,
                        onCheckedChange = viewModel::setEnableBlur,
                    )
                    KernelDivider()
                    KernelSwitchRow(
                        iconRes = R.drawable.ic_kernel_floating,
                        title = "悬浮底栏",
                        subtitle = "使用 Apple 风格的悬浮底栏",
                        checked = state.enableFloatingBottomBar,
                        onCheckedChange = viewModel::setEnableFloatingBottomBar,
                    )
                    KernelDivider()
                    KernelSwitchRow(
                        iconRes = R.drawable.ic_kernel_liquid,
                        title = "液态玻璃",
                        subtitle = "启用悬浮底栏的液态玻璃效果",
                        checked = state.enableLiquidGlass,
                        onCheckedChange = viewModel::setEnableLiquidGlass,
                    )
                    KernelDivider()
                    KernelSwitchRow(
                        iconRes = R.drawable.ic_kernel_predictive,
                        title = "预测性返回手势",
                        subtitle = "启用对预测性返回手势的支持",
                        checked = state.enablePredictiveBack,
                        onCheckedChange = viewModel::setEnablePredictiveBack,
                    )
                }

                Spacer(modifier = Modifier.height(Dimensions.SpacingMedium))
                Text(
                    text = "开关实时生效；悬浮底栏与液态玻璃仅影响底部导航栏。",
                    fontSize = 11.sp,
                    color = ScoreTraceColors.TextTertiaryLight,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ThemeTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.SpacingSmallMedium, vertical = Dimensions.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "返回",
                tint = ScoreTraceColors.TextPrimaryLight,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = "主题设置",
            modifier = Modifier.weight(1f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.width(36.dp))
    }
}

@Composable
private fun ThemePreviewCard() {
    Box(
        modifier = Modifier
            .width(196.dp)
            .height(272.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(28.dp))
            .padding(12.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "ScoreTrace",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = ScoreTraceColors.TextPrimaryLight,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(ContainerShape)
                    .background(ScoreTraceColors.SuccessGreen.copy(alpha = 0.16f)),
            ) {
                Text(
                    text = "今日重点",
                    fontSize = 10.sp,
                    color = ScoreTraceColors.SuccessGreen,
                    modifier = Modifier.padding(10.dp),
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(ContainerShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (index % 2 == 0) 0.9f else 0.65f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                        Spacer(modifier = Modifier.height(7.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(
                    ScoreTraceColors.BrandPrimary,
                    MaterialTheme.colorScheme.onSurface,
                    MaterialTheme.colorScheme.onSurface,
                    MaterialTheme.colorScheme.onSurface,
                ).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(if (color == ScoreTraceColors.BrandPrimary) 12.dp else 9.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color),
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeSelector(
    current: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
    ) {
        listOf(
            "system" to "跟随系统",
            "light" to "浅色",
            "dark" to "深色",
        ).forEach { (mode, label) ->
            val selected = mode == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                    .border(
                        width = 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(18.dp),
                    )
                    .clickable { onSelect(mode) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) ScoreTraceColors.TextPrimaryLight
                    else ScoreTraceColors.TextSecondaryLight,
                )
            }
        }
    }
}

@Composable
private fun KernelSwitchRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = Dimensions.SpacingMedium, vertical = Dimensions.SpacingSmallMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = ScoreTraceColors.TextPrimaryLight,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(Dimensions.SpacingSmallMedium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
    }
}

@Composable
private fun KernelDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(start = 50.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}
