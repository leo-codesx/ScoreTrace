package com.fenji.scoretrace.ui.screen.mine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fenji.scoretrace.ui.component.ai.MarkdownText
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 隐私政策 / 用户协议等法务文本页：从 assets 读取 Markdown（如 privacy.md / agreement.md）并按
 * [MarkdownText] 渲染。文本内置在 App 内，无需网络。
 */
@Composable
fun LegalDocumentScreen(
    title: String,
    assetName: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val content by produceState(initialValue = null as String?) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(assetName).bufferedReader().use { it.readText() }
            }.getOrNull()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = ScoreTraceColors.PageBackgroundLight) {
        Column(modifier = Modifier.fillMaxSize()) {
            MineSubPageTopBar(title = title, onBack = onBack)
            when (val text = content) {
                null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ScoreTraceColors.BrandPrimary)
                }

                else -> Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 32.dp),
                ) {
                    MarkdownText(markdown = text)
                }
            }
        }
    }
}
