package com.fenji.scoretrace.ui.screen.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scoretrace.BuildConfig
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.AppToast

/**
 * 开发者选项：无 ADB 场景下自行导出日志 / 查看运行状态 / 触发崩溃测试。
 * 入口在「我的」页底部（仅登录用户可见）。
 */
@Composable
fun DeveloperOptionsScreen(
    onBack: () -> Unit,
    viewModel: DeveloperOptionsViewModel = hiltViewModel(),
) {
    val status by viewModel.status.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadStatus()
        viewModel.toast.collect { AppToast.show(it) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = ScoreTraceColors.PageBackgroundLight) {
        Column(modifier = Modifier.fillMaxSize()) {
            MineSubPageTopBar(title = "开发者选项", onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                SectionTitle("运行状态")
                Group {
                    InfoRow("登录状态", if (status.isLoggedIn) "已登录" else "未登录")
                    Divider()
                    InfoRow("用户名", status.displayName)
                    Divider()
                    InfoRow("手机号", status.phone)
                    Divider()
                    InfoRow("Token", status.tokenStatus)
                    Divider()
                    InfoRow("省份", status.province)
                    Divider()
                    InfoRow("选科", status.subjects)
                    Divider()
                    InfoRow("数据库版本", "v${status.dbVersion}")
                }

                Spacer(modifier = Modifier.height(20.dp))

                SectionTitle("日志导出")
                Group {
                    ActionRow("导出调试日志", "AppLogger 最近 500 条") { viewModel.exportLogs() }
                    Divider()
                    ActionRow("导出崩溃日志", "crash/ 目录全部堆栈") { viewModel.exportCrashes() }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "导出位置：/sdcard/Download/ScoreTrace/（文件管理器可直接查看）",
                    fontSize = 12.sp,
                    color = ScoreTraceColors.TextTertiaryLight,
                )

                if (BuildConfig.DEBUG) {
                    Spacer(modifier = Modifier.height(20.dp))
                    SectionTitle("危险操作（仅 Debug）")
                    Group {
                        ActionRow(
                            title = "手动触发崩溃测试",
                            subtitle = "验证 CrashHandler 是否落盘",
                            danger = true,
                        ) { viewModel.triggerTestCrash() }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = ScoreTraceColors.TextTertiaryLight,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = ScoreTraceColors.TextSecondaryLight,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = ScoreTraceColors.TextPrimaryLight,
        )
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (danger) ScoreTraceColors.ErrorRed else ScoreTraceColors.TextPrimaryLight,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 12.sp, color = ScoreTraceColors.TextTertiaryLight)
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(start = 16.dp)
            .background(ScoreTraceColors.CardBorderLight),
    )
}
