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

private val Categories = listOf("物理", "历史")

/** 一分一段表查询：云南 2026 物理类占位数据。 */
@Composable
fun ScoreSectionScreen(
    onBack: () -> Unit,
    viewModel: ScoreSectionViewModel = hiltViewModel(),
) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf("物理") }
    var scoreText by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<SectionQuery?>(null) }

    val hasData = data?.tableForCategory(category) != null

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SectionTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimensions.SpacingMedium)
                    .padding(bottom = Dimensions.SpacingLarge),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium),
            ) {
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
                        Text("当前省份", fontSize = 12.sp, color = ScoreTraceColors.TextSecondaryLight)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("云南", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextPrimaryLight)
                    }
                    Text(
                        text = "2026 届",
                        fontSize = 12.sp,
                        color = ScoreTraceColors.BrandPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = Dimensions.SpacingSmallMedium, vertical = 6.dp),
                    )
                }

                Text(
                    text = "科类",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ScoreTraceColors.TextPrimaryLight,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)) {
                    Categories.forEach { item ->
                        val selected = item == category
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
                                .clickable { category = item; result = null }
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ContainerShape)
                        .background(
                            if (hasData) {
                                ScoreTraceColors.SuccessGreen.copy(alpha = 0.10f)
                            } else {
                                ScoreTraceColors.WarningOrange.copy(alpha = 0.10f)
                            },
                        )
                        .padding(Dimensions.SpacingMedium),
                ) {
                    Text(
                        text = if (hasData) {
                            "已加载云南 2026「$category」类一分一段占位数据。"
                        } else {
                            "暂无「$category」类一分一段数据。当前仅录入物理类占位数据。"
                        },
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = if (hasData) ScoreTraceColors.SuccessGreen else ScoreTraceColors.WarningOrange,
                    )
                }

                OutlinedTextField(
                    value = scoreText,
                    onValueChange = { input ->
                        scoreText = input.filter(Char::isDigit).take(3)
                        result = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("输入分数") },
                    placeholder = { Text(if (category == "物理") "例如 580" else "暂无历史类数据") },
                    singleLine = true,
                    shape = RoundedCornerShape(Dimensions.CornerSmall),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScoreTraceColors.BrandPrimary,
                        unfocusedBorderColor = ScoreTraceColors.CardBorderLight,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        cursorColor = ScoreTraceColors.BrandPrimary,
                    ),
                )

                AuthGradientButton(
                    text = "查询位次",
                    enabled = hasData,
                    loading = false,
                    onClick = {
                        when {
                            scoreText.isBlank() -> AppToast.warning("请输入分数")
                            else -> {
                                val query = viewModel.query(category, scoreText)
                                if (query == null) {
                                    AppToast.warning("该分数不在当前占位数据范围内")
                                } else {
                                    result = query
                                }
                            }
                        }
                    },
                )

                result?.let { SectionResultCard(it) }

                Text(
                    text = "数据为 2026 云南物理类占位示例，正式一分一段表待后续脚本抓取；同位分需要历年数据，当前暂不提供。",
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = ScoreTraceColors.TextTertiaryLight,
                )
            }
        }
    }
}

@Composable
private fun SectionTopBar(onBack: () -> Unit) {
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
            text = "一分一段查询",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
    }
}

@Composable
private fun SectionResultCard(result: SectionQuery) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(Dimensions.SpacingLarge),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
    ) {
        Text(
            text = "${result.category}类 · ${result.score} 分",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)) {
            SectionStat("本段人数", result.count.toString(), Modifier.weight(1f))
            SectionStat("累计人数", result.cumulative.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)) {
            SectionStat(
                "排名百分比",
                String.format("%.1f%%", result.percentile),
                Modifier.weight(1f),
            )
            SectionStat("同位分", "待补充", Modifier.weight(1f))
        }
        Text(
            text = "累计人数即该分数及以上的考生人数，可作为位次参考。",
            fontSize = 11.sp,
            color = ScoreTraceColors.TextTertiaryLight,
        )
    }
}

@Composable
private fun SectionStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(ContainerShape)
            .background(ScoreTraceColors.PageBackgroundLight)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.BrandPrimary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = ScoreTraceColors.TextSecondaryLight)
    }
}
