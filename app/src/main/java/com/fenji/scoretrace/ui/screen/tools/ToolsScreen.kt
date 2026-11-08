package com.fenji.scoretrace.ui.screen.tools

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.theme.CardShape
import com.fenji.scoretrace.ui.theme.ContainerShape
import com.fenji.scoretrace.ui.theme.Dimensions
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.AppToast

/**
 * 工具页：集中承载学习辅助能力。
 * 学习计时器由首页快捷功能迁入；错题本、学习计划为后续能力占位。
 */
@Composable
fun ToolsScreen(
    onOpenScoreConvert: () -> Unit,
    onOpenScoreSection: () -> Unit,
    onOpenMajorExplorer: () -> Unit,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.SpacingMedium, vertical = Dimensions.SpacingSmallMedium),
            ) {
                Text(
                    text = "工具",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScoreTraceColors.TextPrimaryLight,
                )
                Spacer(modifier = Modifier.height(Dimensions.SpacingXSmall))
                Text(
                    text = "赋分、复盘、规划，一站式备考",
                    fontSize = 13.sp,
                    color = ScoreTraceColors.TextSecondaryLight,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimensions.SpacingMedium)
                    .padding(bottom = Dimensions.SpacingLarge + bottomContentPadding),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
            ) {
                ScoreConvertHeroCard(onClick = onOpenScoreConvert)

                Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
                Text(
                    text = "高考工具",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ScoreTraceColors.TextPrimaryLight,
                    modifier = Modifier.padding(start = Dimensions.SpacingXSmall),
                )

                ToolRow(
                    icon = painterResource(R.drawable.ic_bar_chart),
                    tint = ScoreTraceColors.BrandPrimary,
                    title = "一分一段",
                    subtitle = "输入分数查询本段人数与位次",
                    badge = "查询",
                    onClick = onOpenScoreSection,
                )
                ToolRow(
                    icon = painterResource(R.drawable.ic_school),
                    tint = ScoreTraceColors.BrandPrimary,
                    title = "选专业",
                    subtitle = "按 3+1+2 选科组合筛选可报专业",
                    badge = "查询",
                    onClick = onOpenMajorExplorer,
                )

                ToolRow(
                    icon = painterResource(R.drawable.ic_book),
                    tint = ScoreTraceColors.SchoolPurple,
                    title = "错题本",
                    subtitle = "整理高频错题，考前集中复盘",
                    badge = "即将上线",
                    onClick = { AppToast.info("错题本正在开发中") },
                )
                ToolRow(
                    icon = painterResource(R.drawable.ic_calendar_month),
                    tint = ScoreTraceColors.SuccessGreen,
                    title = "学习计划",
                    subtitle = "按周拆解目标，跟踪完成进度",
                    badge = "即将上线",
                    onClick = { AppToast.info("学习计划正在开发中") },
                )
            }
        }
    }
}

/** 新高考赋分主入口：品牌蓝青渐变大卡。 */
@Composable
private fun ScoreConvertHeroCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Brush.linearGradient(ScoreTraceColors.AiGradient))
            .clickable(onClick = onClick)
            .padding(Dimensions.SpacingLarge),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(Dimensions.CircleButton)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_show_chart),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(Dimensions.SpacingSmallMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "新高考赋分",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "化学 / 生物 / 政治 / 地理 等级转换",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.82f),
                )
            }
            Text(
                text = "去换算",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = ScoreTraceColors.BrandPrimaryDark,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .padding(horizontal = Dimensions.SpacingMedium, vertical = Dimensions.SpacingSmall),
            )
        }
    }
}

@Composable
private fun ToolRow(
    icon: Painter,
    tint: Color,
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ContainerShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(Dimensions.SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(Dimensions.IconBox)
                .clip(ContainerShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(modifier = Modifier.width(Dimensions.SpacingSmallMedium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
        }
        Text(
            text = badge,
            fontSize = 11.sp,
            color = ScoreTraceColors.TextTertiaryLight,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = Dimensions.SpacingSmall, vertical = Dimensions.SpacingXSmall),
        )
    }
}
