package com.fenji.scoretrace.ui.screen.notification

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scoretrace.data.repository.NotificationType
import com.fenji.scoretrace.ui.component.ai.MarkdownText
import com.fenji.scoretrace.ui.theme.CardShape
import com.fenji.scoretrace.ui.theme.Dimensions
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 通知详情：分类标签 + 时间戳 + 完整正文。
 * 正文走 [MarkdownText]，纯文本通知同样可正常展示；进入页面即由 ViewModel 标记已读。
 */
@Composable
fun NotificationDetailScreen(
    onBack: () -> Unit,
    viewModel: NotificationDetailViewModel = hiltViewModel(),
) {
    val notification by viewModel.notification.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(onBack = onBack)

            val item = notification
            if (item == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "通知不存在或已删除",
                        fontSize = 14.sp,
                        color = ScoreTraceColors.TextTertiaryLight,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Dimensions.SpacingMedium)
                        .padding(bottom = Dimensions.SpacingLarge),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CardShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(Dimensions.SpacingLarge),
                        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
                    ) {
                        CategoryChip(type = item.type)
                        Text(
                            text = item.title,
                            fontSize = 22.sp,
                            lineHeight = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = ScoreTraceColors.TextPrimaryLight,
                        )
                        Text(
                            text = formatFullTime(item.timestamp),
                            fontSize = 12.sp,
                            color = ScoreTraceColors.TextTertiaryLight,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(ScoreTraceColors.CardBorderLight),
                        )
                        MarkdownText(
                            markdown = item.content,
                            modifier = Modifier.padding(top = Dimensions.SpacingXSmall),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTopBar(onBack: () -> Unit) {
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
        Spacer(modifier = Modifier.width(Dimensions.SpacingSmallMedium))
        Text(
            text = "通知详情",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
    }
}

@Composable
private fun CategoryChip(type: String) {
    val (label, tint) = categoryOf(type)
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = tint,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = Dimensions.SpacingSmallMedium, vertical = 4.dp),
    )
}

@Composable
private fun categoryOf(type: String): Pair<String, Color> = when (type) {
    NotificationType.STUDY -> "学习" to ScoreTraceColors.WarningOrange
    NotificationType.SCORE -> "成绩" to ScoreTraceColors.BrandPrimary
    NotificationType.AI -> "AI 建议" to ScoreTraceColors.AccentCyan
    else -> "系统" to ScoreTraceColors.TextSecondaryLight
}

private fun formatFullTime(timestamp: Long): String =
    SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.getDefault()).format(Date(timestamp))
