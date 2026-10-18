package com.fenji.scoretrace.util

import android.content.Context
import android.content.Intent
import com.fenji.scoretrace.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 未捕获异常处理器：把崩溃堆栈写入「应用外部私有目录」的 crash/ 下（`Android/data/<pkg>/files/crash/`），
 * 供下次启动提示用户上报。写完后仍交回系统默认处理器，不改变崩溃本身的行为。
 *
 * 只依赖 Android 框架，零第三方依赖；日志仅保存在本机，不上传。
 */
class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        runCatching { saveCrashLog(t, e) }
        defaultHandler?.uncaughtException(t, e)
    }

    private fun saveCrashLog(t: Thread, e: Throwable) {
        val dir = crashDir(context).apply { if (!exists()) mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "crash_$stamp.txt")

        val text = StringWriter().also { sw ->
            PrintWriter(sw).use { pw ->
                pw.println("time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                pw.println("thread: ${t.name}")
                pw.println("version: ${BuildConfig.VERSION_NAME}")
                pw.println(
                    "device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} " +
                        "/ Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})",
                )
                pw.println()
                e.printStackTrace(pw)
            }
        }.toString()

        file.writeText(text)
        AppLogger.e("Crash", "uncaught thread=${t.name} saved=${file.name}", e)
    }

    companion object {
        private const val DIR_NAME = "crash"
        private const val FILE_PREFIX = "crash_"

        fun crashDir(context: Context): File = File(context.getExternalFilesDir(null), DIR_NAME)

        /** 尚未处理的崩溃日志（已发送/已忽略的会被重命名，前缀不再是 crash_）。 */
        fun pendingCrashes(context: Context): List<File> =
            crashDir(context)
                .listFiles { f -> f.isFile && f.name.startsWith(FILE_PREFIX) }
                ?.sortedByDescending { it.name }
                .orEmpty()

        /** 上报：无后端，故用系统分享把堆栈交给用户选择的应用（邮件/IM 等）。 */
        fun send(context: Context, files: List<File>) {
            val body = files.take(MAX_REPORT_FILES).joinToString("\n\n") { f ->
                runCatching { f.readText() }.getOrDefault("")
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "ScoreTrace 崩溃日志")
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "发送崩溃日志")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(chooser) }
                .onFailure { AppToast.error("未找到可发送的应用") }
            files.forEach { markHandled(it, "sent_") }
        }

        fun markIgnored(files: List<File>) = files.forEach { markHandled(it, "seen_") }

        private fun markHandled(file: File, prefix: String) {
            runCatching { file.renameTo(File(file.parentFile, prefix + file.name)) }
        }

        private const val MAX_REPORT_FILES = 3
    }
}
