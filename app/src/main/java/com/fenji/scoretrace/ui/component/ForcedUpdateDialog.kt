package com.fenji.scoretrace.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import com.fenji.scoretrace.BuildConfig
import com.fenji.scoretrace.util.AppToast
import com.fenji.scoretrace.util.AppUpdateInfo

/**
 * 本地强制更新弹窗：当前 versionCode 小于 [AppUpdateInfo.LATEST_VERSION_CODE] 时展示，
 * 不可通过返回键/点击外部取消。下载地址固定为蓝奏云分享页。
 */
@Composable
fun ForcedUpdateDialogHost() {
    if (BuildConfig.VERSION_CODE >= AppUpdateInfo.LATEST_VERSION_CODE) return
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = { Text("发现新版本") },
        text = {
            Text(
                "最新版本 v${AppUpdateInfo.LATEST_VERSION_NAME}，请更新后继续使用。\n" +
                    "蓝奏云密码：${AppUpdateInfo.DOWNLOAD_PASSWORD}",
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(AppUpdateInfo.DOWNLOAD_WEB_URL))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }.onFailure { AppToast.error("无法打开浏览器，请手动访问 ${AppUpdateInfo.DOWNLOAD_WEB_URL}") }
                },
            ) { Text("前往蓝奏云下载") }
        },
    )
}
