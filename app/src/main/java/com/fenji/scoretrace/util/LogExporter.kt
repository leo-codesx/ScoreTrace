package com.fenji.scoretrace.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 把调试日志 / 崩溃日志导出到系统下载目录 `Download/ScoreTrace/`（走 MediaStore，Android 10+ 无需存储权限）。
 */
object LogExporter {

    private const val DIR = "ScoreTrace"

    /** 导出 [AppLogger] 内存缓冲区的最近日志，返回写入的 Uri（失败返回 null）。 */
    fun exportLogs(context: Context): Uri? =
        writeText(context, "logs_${stamp()}.txt", AppLogger.snapshot().ifBlank { "（暂无日志）" })

    /** 导出 crash/ 目录下全部崩溃堆栈；无崩溃记录返回 null。 */
    fun exportCrashes(context: Context): Uri? {
        val files = CrashHandler.pendingCrashes(context)
        if (files.isEmpty()) return null
        val separator = "\n\n" + "=".repeat(48) + "\n\n"
        val content = files.joinToString(separator) { f ->
            runCatching { f.readText() }.getOrDefault("(读取失败：${f.name})")
        }
        return writeText(context, "crash_${stamp()}.txt", content)
    }

    private fun writeText(context: Context, name: String, content: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + File.separator + DIR,
            )
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            val stream = resolver.openOutputStream(uri)
            if (stream == null) {
                runCatching { resolver.delete(uri, null, null) }
                return null
            }
            stream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            uri
        } catch (t: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            null
        }
    }

    private fun stamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
}
