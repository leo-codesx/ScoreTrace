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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scoretrace.ui.screen.auth.AuthGradientButton
import com.fenji.scoretrace.ui.theme.CardShape
import com.fenji.scoretrace.ui.theme.ContainerShape
import com.fenji.scoretrace.ui.theme.Dimensions
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.AppToast

private val ElectiveSubjects = listOf("化学", "生物", "政治", "地理")

/** 新高考 3+1+2 再选科目赋分工具。 */
@Composable
fun ScoreConvertScreen(
    onBack: () -> Unit,
    viewModel: ScoreConvertViewModel = hiltViewModel(),
) {
    val province by viewModel.province.collectAsStateWithLifecycle()
    val supported = viewModel.isSupported(province)

    var subject by rememberSaveable { mutableStateOf(ElectiveSubjects.first()) }
    var rawScore by rememberSaveable { mutableStateOf("") }
    var percentile by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<ConvertResult?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ConvertTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimensions.SpacingMedium)
                    .padding(bottom = Dimensions.SpacingLarge),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium),
            ) {
                ProvinceCard(province = province)

                if (!supported) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ContainerShape)
                            .background(ScoreTraceColors.WarningOrange.copy(alpha = 0.10f))
                            .padding(Dimensions.SpacingMedium),
                    ) {
                        Text(
                            text = "暂不支持「$province」的赋分规则。当前版本仅支持云南 3+1+2 再选科目规则。",
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = ScoreTraceColors.WarningOrange,
                        )
                    }
                }

                Text(
                    text = "再选科目",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ScoreTraceColors.TextPrimaryLight,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)) {
                    ElectiveSubjects.forEach { item ->
                        val selected = item == subject
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selected) {
                                        Brush.linearGradient(ScoreTraceColors.AiGradient)
                                    } else {
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.surface,
                                                MaterialTheme.colorScheme.surface,
                                            ),
                                        )
                                    },
                                )
                                .clickable { subject = item; result = null }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = item,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) Color.White else ScoreTraceColors.TextSecondaryLight,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = rawScore,
                    onValueChange = { input ->
                        rawScore = input.filter { it.isDigit() || it == '.' }.take(5)
                        result = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("原始分（0-100）") },
                    placeholder = { Text("例如 78") },
                    singleLine = true,
                    shape = RoundedCornerShape(Dimensions.CornerSmall),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = convertFieldColors(),
                )

                OutlinedTextField(
                    value = percentile,
                    onValueChange = { input ->
                        percentile = input.filter { it.isDigit() || it == '.' }.take(5)
                        result = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("该科预估排名百分比（0-100）") },
                    placeholder = { Text("例如 12.5，表示前 12.5%") },
                    singleLine = true,
                    shape = RoundedCornerShape(Dimensions.CornerSmall),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = convertFieldColors(),
                )

                Text(
                    text = "云南等级比例：A 17% · B 33% · C 33% · D 16% · E 1%；等级分区间：A 86-100 · B 71-85 · C 56-70 · D 41-55 · E 30-40。",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = ScoreTraceColors.TextTertiaryLight,
                )

                AuthGradientButton(
                    text = "计算赋分",
                    enabled = supported,
                    loading = false,
                    onClick = {
                        val calculated = viewModel.calculate(subject, rawScore, percentile, province)
                        if (calculated == null) {
                            AppToast.warning("请输入 0-100 的原始分和排名百分比")
                        } else {
                            result = calculated
                        }
                    },
                )

                result?.takeIf { it.supported }?.let { ConvertResultCard(it) }

                Text(
                    text = "估算模式 · 结果仅供参考，实际等级分以省考试院公布为准。",
                    fontSize = 11.sp,
                    color = ScoreTraceColors.TextTertiaryLight,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ConvertTopBar(onBack: () -> Unit) {
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
            text = "新高考赋分",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
    }
}

@Composable
private fun ProvinceCard(province: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        ScoreTraceColors.BrandPrimary.copy(alpha = 0.12f),
                        ScoreTraceColors.AccentCyan.copy(alpha = 0.10f),
                    ),
                ),
            )
            .padding(Dimensions.SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "当前省份",
                fontSize = 12.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = province,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ScoreTraceColors.TextPrimaryLight,
            )
        }
        Text(
            text = "3+1+2",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = ScoreTraceColors.BrandPrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = Dimensions.SpacingSmallMedium, vertical = 6.dp),
        )
    }
}

@Composable
private fun ConvertResultCard(result: ConvertResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(Dimensions.SpacingLarge),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${result.subject} 等级分",
                    fontSize = 13.sp,
                    color = ScoreTraceColors.TextSecondaryLight,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = result.convertedScore.toString(),
                        fontSize = 48.sp,
                        lineHeight = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = ScoreTraceColors.BrandPrimary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "分",
                        fontSize = 14.sp,
                        color = ScoreTraceColors.TextSecondaryLight,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(ScoreTraceColors.AiGradient)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = result.grade.label,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        Text(
            text = "原始分 ${result.rawScore} · 预估排名前 ${String.format("%.1f", result.percentile)}%",
            fontSize = 13.sp,
            color = ScoreTraceColors.TextSecondaryLight,
        )
        Text(
            text = "按等级 ${result.grade.label}（${result.grade.t1}-${result.grade.t2} 分）等比例转换",
            fontSize = 12.sp,
            color = ScoreTraceColors.TextTertiaryLight,
        )
    }
}

@Composable
private fun convertFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ScoreTraceColors.BrandPrimary,
    unfocusedBorderColor = ScoreTraceColors.CardBorderLight,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    cursorColor = ScoreTraceColors.BrandPrimary,
)
