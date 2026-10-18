package com.fenji.scoretrace.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.fenji.scoretrace.BuildConfig
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val changelog: String,
)

sealed interface UpdateCheckResult {
    data object NotConfigured : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class Available(val info: UpdateInfo) : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

sealed interface InstallResult {
    data object Started : InstallResult
    data object NeedPermission : InstallResult
    data class Failed(val message: String) : InstallResult
}

private data class UpdateConfigDto(
    val versionCode: Int = 0,
    val versionName: String = "",
    val downloadUrl: String = "",
    val changelog: String = "",
)

/**
 * 云更新：从远程 JSON 检查版本，OkHttp 下载 APK，FileProvider 安装。
 * JSON 地址由 `local.properties` 的 `update.config.url` 注入，为空时功能自动关闭。
 */
@Singleton
class UpdateRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Named("config") private val configClient: OkHttpClient,
    @param:Named("download") private val downloadClient: OkHttpClient,
    private val gson: Gson,
) {

    suspend fun check(): UpdateCheckResult = withContext(Dispatchers.IO) {
        val url = BuildConfig.UPDATE_CONFIG_URL
        if (url.isBlank()) return@withContext UpdateCheckResult.NotConfigured
        runCatching {
            val body = configClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext UpdateCheckResult.Failed("HTTP ${response.code}")
                response.body?.string().orEmpty()
            }
            val dto = gson.fromJson(body, UpdateConfigDto::class.java)
            if (dto.downloadUrl.isBlank() || dto.versionCode <= 0) {
                return@withContext UpdateCheckResult.Failed("更新配置格式不正确")
            }
            if (dto.versionCode > BuildConfig.VERSION_CODE) {
                UpdateCheckResult.Available(
                    UpdateInfo(
                        versionCode = dto.versionCode,
                        versionName = dto.versionName.ifBlank { dto.versionCode.toString() },
                        downloadUrl = dto.downloadUrl,
                        changelog = dto.changelog,
                    ),
                )
            } else {
                UpdateCheckResult.UpToDate
            }
        }.getOrElse { UpdateCheckResult.Failed(it.message ?: "网络异常") }
    }

    suspend fun download(info: UpdateInfo, onProgress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val root = context.getExternalFilesDir(null) ?: context.filesDir
            val dir = File(root, "ScoreTrace").apply { mkdirs() }
            val target = File(dir, "ScoreTrace-${info.versionName}.apk")
            if (target.exists()) target.delete()
            val response = downloadClient.newCall(
                Request.Builder().url(info.downloadUrl).build(),
            ).execute()
            if (!response.isSuccessful) error("下载失败 HTTP ${response.code}")
            val body = response.body ?: error("下载内容为空")
            val total = body.contentLength()
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var written = 0L
                    var lastPercent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0) {
                            val percent = (written * 100 / total).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
            onProgress(100)
            target
        }

    fun install(file: File): InstallResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            return InstallResult.NeedPermission
        }
        return runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            InstallResult.Started
        }.getOrElse { InstallResult.Failed(it.message ?: "无法启动安装") }
    }

    fun openUnknownSourcesSettings() {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
