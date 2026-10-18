package com.fenji.scoretrace.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 统一的业务日志出口。
 *
 * 双写：一方面按固定 tag [TAG] 输出到 Logcat（便于 `adb logcat -s ScoreTrace` 过滤）；
 * 另一方面写入内存环形缓冲区（最近 [MAX_LINES] 条），供「开发者选项 → 导出日志」在
 * 无 ADB 的情况下取回关键链路日志。缓冲区是进程级、非持久化，重启即清空。
 */
object AppLogger {

    const val TAG = "ScoreTrace"

    /** 环形缓冲区最多保留的日志条数。 */
    private const val MAX_LINES = 500

    private val buffer = java.util.ArrayDeque<String>(MAX_LINES)
    private val lock = Any()
    private val timeFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    fun d(event: String, detail: String? = null) {
        record("D", event, detail, null)
        Log.d(TAG, format(event, detail))
    }

    fun i(event: String, detail: String? = null) {
        record("I", event, detail, null)
        Log.i(TAG, format(event, detail))
    }

    fun w(event: String, detail: String? = null) {
        record("W", event, detail, null)
        Log.w(TAG, format(event, detail))
    }

    fun e(event: String, detail: String? = null, throwable: Throwable? = null) {
        record("E", event, detail, throwable)
        Log.e(TAG, format(event, detail), throwable)
    }

    /** 导出当前缓冲区全部内容（按时间顺序，每行一条）。 */
    fun snapshot(): String = synchronized(lock) { buffer.joinToString("\n") }

    fun clear() = synchronized(lock) { buffer.clear() }

    private fun record(level: String, event: String, detail: String?, throwable: Throwable?) {
        val line = buildString {
            append(timeFormat.format(Date()))
            append(' ').append(level).append(' ')
            append(format(event, detail))
            if (throwable != null) append('\n').append(Log.getStackTraceString(throwable))
        }
        synchronized(lock) {
            buffer.addLast(line)
            while (buffer.size > MAX_LINES) buffer.removeFirst()
        }
    }

    private fun format(event: String, detail: String?): String =
        if (detail.isNullOrBlank()) "[$event]" else "[$event] $detail"
}
