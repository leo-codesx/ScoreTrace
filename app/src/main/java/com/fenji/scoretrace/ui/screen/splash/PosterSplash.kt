package com.fenji.scoretrace.ui.screen.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar

private val NightTop = Color(0xFF06101F)
private val NightMiddle = Color(0xFF0B2039)
private val NightBottom = Color(0xFF103A5C)
private val StarWhite = Color(0xFFDCEEFF)

/**
 * 启动页海报层：深蓝沉浸式渐变 + 手写风 Slogan + 呼吸 Logo + 高考倒计时 + 几何线条。
 * 展示在 App 首帧之上，由 MainActivity 控制停留时长并在结束时淡出。
 */
@Composable
fun PosterSplash(
    gaokaoTimestamp: Long,
    modifier: Modifier = Modifier,
) {
    val appear = remember { Animatable(0f) }
    val footerAppear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(durationMillis = 900))
        footerAppear.animateTo(1f, tween(durationMillis = 700))
    }

    val breath = rememberInfiniteTransition(label = "splashBreath")
    val breathScale by breath.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathScale",
    )

    val targetYear = remember(gaokaoTimestamp) { yearOf(gaokaoTimestamp) }
    val daysLeft = remember(gaokaoTimestamp) { daysUntil(gaokaoTimestamp) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(NightTop, NightMiddle, NightBottom)),
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 星点：固定归一化坐标，避免每帧随机导致闪烁
            val stars = listOf(
                0.12f to 0.10f, 0.28f to 0.07f, 0.46f to 0.13f, 0.72f to 0.08f,
                0.88f to 0.15f, 0.18f to 0.24f, 0.62f to 0.22f, 0.82f to 0.30f,
                0.08f to 0.42f, 0.92f to 0.45f, 0.20f to 0.62f, 0.76f to 0.58f,
            )
            stars.forEachIndexed { index, (x, y) ->
                drawCircle(
                    color = StarWhite.copy(alpha = if (index % 3 == 0) 0.45f else 0.22f),
                    radius = if (index % 4 == 0) 2.2f else 1.4f,
                    center = Offset(size.width * x, size.height * y),
                )
            }
            // 底部几何斜线：三道由弱到强的水平线，制造海报的版面收束
            val baseY = size.height * 0.86f
            listOf(0.30f to 0.10f, 0.22f to 0.16f, 0.14f to 0.24f).forEachIndexed { index, pair ->
                val lineY = baseY + index * 10.dp.toPx()
                drawLine(
                    color = StarWhite.copy(alpha = pair.second),
                    start = Offset(size.width * 0.12f, lineY),
                    end = Offset(size.width * (0.12f + pair.first), lineY),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            drawCircle(
                color = ScoreTraceColors.AccentCyan.copy(alpha = 0.55f),
                radius = 3.dp.toPx(),
                center = Offset(size.width * 0.12f, baseY + 30.dp.toPx()),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = appear.value
                        translationY = (1f - appear.value) * 24.dp.toPx()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "$targetYear，赴一场青春之约",
                    fontSize = 24.sp,
                    lineHeight = 34.sp,
                    fontFamily = FontFamily.Cursive,
                    fontStyle = FontStyle.Italic,
                    color = Color.White.copy(alpha = 0.94f),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "SCORETRACE  ·  KEEP  YOUR  TRACE",
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    color = StarWhite.copy(alpha = 0.55f),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(190.dp)
                    .graphicsLayer {
                        alpha = appear.value
                        scaleX = breathScale * (0.90f + 0.10f * appear.value)
                        scaleY = breathScale * (0.90f + 0.10f * appear.value)
                    }
                    .drawBehind {
                        // 外发光：大面积低透明度，让 Logo 从深色背景中“浮”出来
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ScoreTraceColors.AccentCyan.copy(alpha = 0.30f),
                                    ScoreTraceColors.AccentBlue.copy(alpha = 0.14f),
                                    Color.Transparent,
                                ),
                                center = center,
                                radius = size.minDimension / 2,
                            ),
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.07f),
                            radius = size.minDimension * 0.26f,
                            center = center,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "ScoreTrace",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(128.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = footerAppear.value
                    translationY = (1f - footerAppear.value) * 18.dp.toPx()
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "距离 $targetYear 高考还有",
                    fontSize = 13.sp,
                    color = StarWhite.copy(alpha = 0.65f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box {
                    Text(
                        text = daysLeft.toString(),
                        fontSize = 64.sp,
                        lineHeight = 68.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                }
                Text(
                    text = "天",
                    fontSize = 13.sp,
                    color = StarWhite.copy(alpha = 0.65f),
                )
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

private fun yearOf(timestamp: Long): Int =
    Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.YEAR)

private fun daysUntil(timestamp: Long): Long {
    val target = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return ChronoUnit.DAYS.between(LocalDate.now(), target).coerceAtLeast(0L)
}
