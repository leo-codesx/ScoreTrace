package com.fenji.scoretrace.ui.screen.mine

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.fenji.scoretrace.BuildConfig
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.AppUpdateInfo

/** 关于 ScoreTrace：Logo + 版本 + 简介 + 检查更新 / 开源许可 / 隐私政策 / 用户协议。 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit = {},
    onOpenAgreement: () -> Unit = {},
) {
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MineSubPageTopBar(title = "关于 ScoreTrace", onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = "ScoreTrace",
                        modifier = Modifier.size(72.dp),
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("ScoreTrace", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextPrimaryLight)
                Spacer(modifier = Modifier.height(4.dp))
                Text("版本 v${BuildConfig.VERSION_NAME}", fontSize = 13.sp, color = ScoreTraceColors.TextTertiaryLight)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "为 2027 届高考生而做的备考追踪工具",
                    fontSize = 14.sp,
                    color = ScoreTraceColors.TextSecondaryLight,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                AboutGroup {
                    AboutRow("前往蓝奏云", "密码 ${AppUpdateInfo.DOWNLOAD_PASSWORD}") {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(AppUpdateInfo.DOWNLOAD_WEB_URL)),
                            )
                        }
                    }
                    AboutDivider()
                    AboutRow("开源许可", "查看使用的第三方开源库") { showLicenses = true }
                    AboutDivider()
                    AboutRow("隐私政策", "") { onOpenPrivacy() }
                    AboutDivider()
                    AboutRow("用户协议", "") { onOpenAgreement() }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "© 2026 ScoreTrace · Made with ❤ for 2027 届",
                    fontSize = 12.sp,
                    color = ScoreTraceColors.TextTertiaryLight,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showLicenses) {
        OpenSourceLicensesSheet(onDismiss = { showLicenses = false })
    }
}

@Composable
internal fun MineSubPageTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
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
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
    }
}

@Composable
private fun AboutGroup(content: @Composable () -> Unit) {
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
private fun AboutRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = ScoreTraceColors.TextPrimaryLight,
            modifier = Modifier.weight(1f),
        )
        if (value.isNotEmpty()) {
            Text(text = value, fontSize = 13.sp, color = ScoreTraceColors.TextTertiaryLight)
        }
    }
}

@Composable
private fun AboutDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(start = 16.dp)
            .background(ScoreTraceColors.CardBorderLight),
    )
}

private data class OpenSourceLibrary(val name: String, val license: String)

private val OPEN_SOURCE_LIBRARIES = listOf(
    OpenSourceLibrary("Jetpack Compose", "Apache-2.0"),
    OpenSourceLibrary("AndroidX (Core / Lifecycle / Activity / Navigation)", "Apache-2.0"),
    OpenSourceLibrary("Material 3", "Apache-2.0"),
    OpenSourceLibrary("Hilt (Dagger)", "Apache-2.0"),
    OpenSourceLibrary("Room", "Apache-2.0"),
    OpenSourceLibrary("DataStore", "Apache-2.0"),
    OpenSourceLibrary("Retrofit", "Apache-2.0"),
    OpenSourceLibrary("OkHttp", "Apache-2.0"),
    OpenSourceLibrary("Gson", "Apache-2.0"),
    OpenSourceLibrary("Kotlin Coroutines", "Apache-2.0"),
    OpenSourceLibrary("Coil", "Apache-2.0"),
    OpenSourceLibrary("AndroidX Media3", "Apache-2.0"),
    OpenSourceLibrary("Authing Guard SDK", "Apache-2.0"),
    OpenSourceLibrary("Sonner (Compose Toast)", "Apache-2.0"),
    OpenSourceLibrary("Haze (dev.chrisbanes.haze)", "Apache-2.0"),
)

/** 开源许可弹层：LazyColumn 展示第三方库与许可证。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenSourceLicensesSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text("开源许可", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextPrimaryLight)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(listOf(ScoreTraceColors.BrandPrimary, ScoreTraceColors.AccentCyan)),
                    ),
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(OPEN_SOURCE_LIBRARIES) { lib ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = lib.name,
                            fontSize = 14.sp,
                            color = ScoreTraceColors.TextPrimaryLight,
                            modifier = Modifier.weight(1f),
                        )
                        Text(text = lib.license, fontSize = 12.sp, color = ScoreTraceColors.TextTertiaryLight)
                    }
                }
            }
        }
    }
}
