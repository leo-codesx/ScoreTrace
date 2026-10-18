package com.fenji.scoretrace.ui.screen.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private data class SplashPalette(
    val background: List<Color>,
    val particle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val glow: Color,
    val glowSecondary: Color,
    val skipBackground: Color,
    val skipText: Color,
    val logoBackdrop: Color,
)

private val DarkPalette = SplashPalette(
    background = listOf(Color(0xFF06101F), Color(0xFF0B2039), Color(0xFF103A5C)),
    particle = Color.White,
    textPrimary = Color.White,
    textSecondary = Color.White.copy(alpha = 0.66f),
    glow = ScoreTraceColors.AccentCyan,
    glowSecondary = ScoreTraceColors.AccentBlue,
    skipBackground = Color.White.copy(alpha = 0.10f),
    skipText = Color.White.copy(alpha = 0.80f),
    logoBackdrop = Color.Transparent,
)

private val LightPalette = SplashPalette(
    background = listOf(Color(0xFFFFFFFF), Color(0xFFEAF4FF), Color(0xFFD9EAFF)),
    particle = Color(0xFF7EAED8),
    textPrimary = Color(0xFF1A1A2E),
    textSecondary = Color(0xFF64748B),
    glow = ScoreTraceColors.BrandPrimary,
    glowSecondary = ScoreTraceColors.SuccessGreen,
    skipBackground = Color(0xFF0F172A).copy(alpha = 0.06f),
    skipText = Color(0xFF334155),
    logoBackdrop = Color.White,
)

private data class Particle(
    val x: Float,
    val radius: Float,
    val speed: Float,
    val phase: Float,
    val wobble: Float,
    val alpha: Float,
)

/**
 * 动态启动海报：跟随系统深浅色，粒子缓慢上浮，Logo 弹性入场 + 呼吸光晕，
 * 高考倒计时从 0 滚动到目标天数；2.3 秒后自动结束，也可点右上角「跳过」。
 */
@Composable
fun SplashScreen(
    gaokaoTimestamp: Long,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val palette = if (dark) DarkPalette else LightPalette

    val appear = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.8f) }
    val sloganProgress = remember { Animatable(0f) }
    val countProgress = remember { Animatable(0f) }

    val targetYear = remember(gaokaoTimestamp) { yearOf(gaokaoTimestamp) }
    val days = remember(gaokaoTimestamp) { daysUntil(gaokaoTimestamp).toFloat() }

    val breath = rememberInfiniteTransition(label = "splashBreath")
    val breathScale by breath.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3300),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathScale",
    )
    val particleProgress by breath.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10500, easing = LinearEasing),
        ),
        label = "particleProgress",
    )

    val particles = remember {
        val random = Random(2027)
        List(18) {
            Particle(
                x = random.nextFloat(),
                radius = 1.6f + random.nextFloat() * 3.2f,
                speed = 0.05f + random.nextFloat() * 0.10f,
                phase = random.nextFloat(),
                wobble = 0.008f + random.nextFloat() * 0.026f,
                alpha = 0.10f + random.nextFloat() * 0.20f,
            )
        }
    }

    LaunchedEffect(Unit) {
        launch { appear.animateTo(1f, tween(durationMillis = 750)) }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
        launch {
            sloganProgress.animateTo(1f, tween(durationMillis = 1050, delayMillis = 180))
        }
        launch {
            countProgress.animateTo(
                targetValue = days,
                animationSpec = tween(
                    durationMillis = 2100,
                    delayMillis = 525,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    // 自动结束；点「跳过」时由 onFinished 提前关闭，重复调用是幂等的。
    LaunchedEffect(Unit) {
        delay(3500)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(palette.background)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val progress = particleProgress
            particles.forEach { particle ->
                val cycle = (particle.phase + progress * particle.speed * 10f) % 1f
                val y = size.height * (1f - cycle)
                val wobble = sin((progress * 2f * PI + particle.phase * 2f * PI).toFloat())
                val x = size.width * (particle.x + wobble * particle.wobble)
                drawCircle(
                    color = palette.particle.copy(alpha = particle.alpha),
                    radius = particle.radius.dp.toPx(),
                    center = Offset(x, y),
                )
            }
        }

        Text(
            text = "跳过",
            fontSize = 12.sp,
            color = palette.skipText,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 20.dp)
                .clip(RoundedCornerShape(50))
                .background(palette.skipBackground)
                .clickable(onClick = onFinished)
                .padding(horizontal = 14.dp, vertical = 7.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(76.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = sloganProgress.value
                        translationY = (1f - sloganProgress.value) * 22.dp.toPx()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "$targetYear，赴一场青春之约",
                    fontSize = 24.sp,
                    lineHeight = 34.sp,
                    fontFamily = FontFamily.Cursive,
                    fontStyle = FontStyle.Italic,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "SCORETRACE  ·  KEEP  YOUR  TRACE",
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    color = palette.textSecondary,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(190.dp)
                    .graphicsLayer {
                        alpha = appear.value
                        scaleX = breathScale * logoScale.value
                        scaleY = breathScale * logoScale.value
                    }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    palette.glow.copy(alpha = if (dark) 0.32f else 0.20f),
                                    palette.glowSecondary.copy(alpha = if (dark) 0.14f else 0.10f),
                                    Color.Transparent,
                                ),
                                center = center,
                                radius = size.minDimension / 2,
                            ),
                        )
                        if (!dark) {
                            drawCircle(
                                color = palette.logoBackdrop,
                                radius = size.minDimension * 0.30f,
                                center = center,
                            )
                        }
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
                    alpha = appear.value
                    translationY = (1f - appear.value) * 14.dp.toPx()
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "距离 $targetYear 高考还有",
                    fontSize = 13.sp,
                    color = palette.textSecondary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = countProgress.value.roundToInt().toString(),
                    fontSize = 64.sp,
                    lineHeight = 68.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary,
                )
                Text(
                    text = "天",
                    fontSize = 13.sp,
                    color = palette.textSecondary,
                )
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

private fun yearOf(timestamp: Long): Int =
    Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.YEAR)

private fun daysUntil(timestamp: Long): Long {
    val target = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return ChronoUnit.DAYS.between(LocalDate.now(), target).coerceAtLeast(0L)
}
