package com.fenji.scoretrace.ui.screen.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

private val FirstChoices = listOf("物理", "历史")
private val ElectiveChoices = listOf("化学", "生物", "政治", "地理")

/** 选专业：按 3+1+2 选科组合筛选可报专业，按学科门类分组。 */
@Composable
fun MajorExplorerScreen(
    onBack: () -> Unit,
    viewModel: MajorExplorerViewModel = hiltViewModel(),
) {
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    var firstChoice by rememberSaveable { mutableStateOf("物理") }
    var electives by remember { mutableStateOf(setOf("化学", "生物")) }
    var groups by remember { mutableStateOf<List<MajorGroup>>(emptyList()) }
    var queried by remember { mutableStateOf(false) }

    fun toggleElective(item: String) {
        electives = when {
            item in electives -> electives - item
            electives.size < 2 -> electives + item
            else -> {
                AppToast.warning("再选科目最多选择 2 门")
                electives
            }
        }
        queried = false
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MajorTopBar(onBack = onBack)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = Dimensions.SpacingMedium,
                    end = Dimensions.SpacingMedium,
                    bottom = Dimensions.SpacingLarge,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ContainerShape)
                            .background(ScoreTraceColors.BrandPrimary.copy(alpha = 0.08f))
                            .padding(Dimensions.SpacingMedium),
                    ) {
                        Text(
                            text = "按 3+1+2 选科组合筛选可报专业。专业目录本身不含选科要求，结果按门类与专业名称估算，具体以院校招生专业目录为准。",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = ScoreTraceColors.TextSecondaryLight,
                        )
                    }
                }

                item {
                    SubjectSelector(
                        title = "首选科目",
                        options = FirstChoices,
                        selected = setOf(firstChoice),
                        onToggle = { firstChoice = it; queried = false },
                    )
                }

                item {
                    SubjectSelector(
                        title = "再选科目（选 2 门）",
                        options = ElectiveChoices,
                        selected = electives,
                        onToggle = ::toggleElective,
                    )
                }

                item {
                    AuthGradientButton(
                        text = "查询可报专业",
                        enabled = !loading && electives.size == 2,
                        loading = loading,
                        onClick = {
                            groups = viewModel.filter(firstChoice, electives)
                            queried = true
                        },
                    )
                }

                if (queried) {
                    item {
                        Text(
                            text = "共 ${groups.sumOf { it.total }} 个可报专业 · ${groups.size} 个学科门类",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = ScoreTraceColors.TextSecondaryLight,
                            modifier = Modifier.padding(top = Dimensions.SpacingSmall),
                        )
                    }
                    if (groups.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CardShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(Dimensions.SpacingLarge),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "没有匹配的专业，试试调整选科组合",
                                    fontSize = 13.sp,
                                    color = ScoreTraceColors.TextSecondaryLight,
                                )
                            }
                        }
                    } else {
                        items(groups) { group -> MajorGroupCard(group) }
                    }
                } else {
                    item {
                        Text(
                            text = "选择首选与两门再选科目后点击查询。",
                            fontSize = 12.sp,
                            color = ScoreTraceColors.TextTertiaryLight,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectSelector(
    title: String,
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
        Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)) {
            options.forEach { item ->
                val isSelected = item in selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isSelected) {
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
                        .clickable { onToggle(item) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color.White else ScoreTraceColors.TextSecondaryLight,
                    )
                }
            }
        }
    }
}

@Composable
private fun MajorGroupCard(group: MajorGroup) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(Dimensions.SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = group.categoryName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ScoreTraceColors.TextPrimaryLight,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${group.total} 个",
                fontSize = 12.sp,
                color = ScoreTraceColors.BrandPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ScoreTraceColors.BrandPrimary.copy(alpha = 0.10f))
                    .padding(horizontal = Dimensions.SpacingSmall, vertical = 3.dp),
            )
        }
        group.subGroups.forEach { sub ->
            Column {
                Text(
                    text = sub.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ScoreTraceColors.TextSecondaryLight,
                )
                Text(
                    text = sub.majors.joinToString("、") { it.display },
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = ScoreTraceColors.TextPrimaryLight,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun MajorTopBar(onBack: () -> Unit) {
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
            text = "选专业",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
    }
}
