package com.fenji.scoretrace.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.fenji.scoretrace.util.CrashHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 启动时若检测到上次崩溃留下的日志，弹出提示让用户选择是否上报。
 * 挂在 [com.fenji.scoretrace.MainActivity] 顶层，与 [AppToastHost] 同级。
 */
@Composable
fun CrashReportDialogHost() {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<List<File>>(emptyList()) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val crashes = withContext(Dispatchers.IO) { CrashHandler.pendingCrashes(context) }
        if (crashes.isNotEmpty()) {
            pending = crashes
            visible = true
        }
    }

    if (visible) {
        AlertDialog(
            onDismissRequest = { visible = false },
            title = { Text("上次运行出错了") },
            text = {
                Text("检测到 ${pending.size} 条崩溃日志。是否发送给我们用于改进？（仅包含崩溃堆栈与设备信息）")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        CrashHandler.send(context, pending)
                        visible = false
                    },
                ) { Text("发送日志") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        CrashHandler.markIgnored(pending)
                        visible = false
                    },
                ) { Text("忽略") }
            },
        )
    }
}
