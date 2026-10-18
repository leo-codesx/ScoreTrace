package com.fenji.scoretrace.ui.component.score

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.ui.screen.score.SubjectComparison
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import kotlin.math.max

private val LatestLineBrush = Brush.linearGradient(ScoreTraceColors.AiGradient)
private val PreviousLineColor = Color(0xFF94A3B8)

/**
 * 最近两次考试的单科双折线对比。
 * 本次：蓝青渐变实线；上次：灰色虚线。下方按科目显示差值（涨绿跌红）。
 */
@Composable
fun SubjectComparisonChart(
    comparisons: List<SubjectComparison>,
    latestExamName: String,
    previousExamName: String,
    modifier: Modifier = Modifier,
) {
    if (comparisons.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("暂无单科对比数据", fontSize = 14.sp, color = ScoreTraceColors.TextSecondaryLight)
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "━ 本次",
                fontSize = 11.sp,
                color = ScoreTraceColors.BrandPrimary,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = latestExamName.ifBlank { "最近一次" },
                fontSize = 11.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "┄ 上次",
                fontSize = 11.sp,
                color = PreviousLineColor,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = previousExamName.ifBlank { "上一次" },
                fontSize = 11.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val maxScore = comparisons.maxOf {
            max(it.latestScore ?: 0, it.previousScore ?: 0)
        }.coerceAtLeast(1)
        val gridColor = ScoreTraceColors.CardBorderLight

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp),
        ) {
            val left = 10.dp.toPx()
            val right = size.width - 10.dp.toPx()
            val top = 12.dp.toPx()
            val bottom = size.height - 10.dp.toPx()
            val chartWidth = (right - left).coerceAtLeast(1f)
            val chartHeight = (bottom - top).coerceAtLeast(1f)
            val count = comparisons.size

            fun xOf(index: Int): Float =
                if (count <= 1) (left + right) / 2f
                else left + chartWidth * index / (count - 1)

            fun yOf(score: Int): Float =
                bottom - chartHeight * score.toFloat() / maxScore

            // 三条横向参考线（0 / 1/2 / 最大值）
            repeat(3) { index ->
                val y = bottom - chartHeight * index / 2f
                drawLine(
                    color = gridColor.copy(alpha = 0.75f),
                    start = Offset(left, y),
                    end = Offset(right, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            // 本次：蓝青渐变实线
            val latestPath = Path()
            var latestStarted = false
            comparisons.forEachIndexed { index, item ->
                val score = item.latestScore ?: return@forEachIndexed
                val x = xOf(index)
                val y = yOf(score)
                if (latestStarted) latestPath.lineTo(x, y) else {
                    latestPath.moveTo(x, y)
                    latestStarted = true
                }
            }
            if (latestStarted) {
                drawPath(
                    path = latestPath,
                    brush = LatestLineBrush,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }

            // 上次：灰色虚线；缺考处断开
            val previousPath = Path()
            var previousStarted = false
            comparisons.forEachIndexed { index, item ->
                val score = item.previousScore
                if (score == null) {
                    previousStarted = false
                    return@forEachIndexed
                }
                val x = xOf(index)
                val y = yOf(score)
                if (previousStarted) previousPath.lineTo(x, y) else {
                    previousPath.moveTo(x, y)
                    previousStarted = true
                }
            }
            drawPath(
                path = previousPath,
                color = PreviousLineColor,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(7.dp.toPx(), 6.dp.toPx()),
                    ),
                ),
            )

            // 数据点
            comparisons.forEachIndexed { index, item ->
                val x = xOf(index)
                item.previousScore?.let { score ->
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(x, yOf(score)),
                    )
                    drawCircle(
                        color = PreviousLineColor,
                        radius = 2.4.dp.toPx(),
                        center = Offset(x, yOf(score)),
                    )
                }
                item.latestScore?.let { score ->
                    drawCircle(
                        color = Color.White,
                        radius = 4.6.dp.toPx(),
                        center = Offset(x, yOf(score)),
                    )
                    drawCircle(
                        brush = LatestLineBrush,
                        radius = 3.dp.toPx(),
                        center = Offset(x, yOf(score)),
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            comparisons.forEach { item ->
                Text(
                    text = item.subject,
                    fontSize = 11.sp,
                    color = ScoreTraceColors.TextSecondaryLight,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            comparisons.forEach { item ->
                val latest = item.latestScore
                val previous = item.previousScore
                val delta = if (latest != null && previous != null) latest - previous else null
                val color = when {
                    delta == null -> ScoreTraceColors.TextTertiaryLight
                    delta >= 0 -> ScoreTraceColors.SuccessGreen
                    else -> ScoreTraceColors.ErrorRed
                }
                val text = when {
                    latest == null -> "缺考"
                    previous == null -> "新增"
                    delta == 0 -> "持平"
                    delta!! > 0 -> "↑+$delta"
                    else -> "↓$delta"
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color.copy(alpha = 0.10f))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = text,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = color,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
