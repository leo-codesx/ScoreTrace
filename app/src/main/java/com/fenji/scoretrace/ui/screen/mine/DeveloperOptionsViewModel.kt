package com.fenji.scoretrace.ui.screen.mine

import android.content.Context
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.local.ScoreTraceDatabase
import com.fenji.scoretrace.data.local.UserPreferences
import com.fenji.scoretrace.data.model.AuthUser
import com.fenji.scoretrace.data.repository.AuthRepository
import com.fenji.scoretrace.util.AppLogger
import com.fenji.scoretrace.util.AppToastType
import com.fenji.scoretrace.util.LogExporter
import com.fenji.scoretrace.util.ToastMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 开发者选项：导出调试日志 / 崩溃日志、查看运行状态、手动触发崩溃（仅 Debug）。
 * 面向「无 ADB」场景——用户可自行导出日志交给 AiCode 诊断。
 */
@HiltViewModel
class DeveloperOptionsViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
    private val database: ScoreTraceDatabase,
) : ViewModel() {

    data class DevStatus(
        val isLoggedIn: Boolean = false,
        val displayName: String = "-",
        val phone: String = "-",
        val tokenStatus: String = "-",
        val province: String = "-",
        val subjects: String = "-",
        val dbVersion: Int = 0,
    )

    private val _status = MutableStateFlow(DevStatus())
    val status: StateFlow<DevStatus> = _status.asStateFlow()

    private val _toast = MutableSharedFlow<ToastMessage>(extraBufferCapacity = 2)
    val toast: SharedFlow<ToastMessage> = _toast.asSharedFlow()

    fun loadStatus() {
        viewModelScope.launch {
            val user = authRepository.observeCurrentUser().first()
            val token = userPreferences.authToken.first()
            val province = userPreferences.province.first()
            val subjects = userPreferences.selectedSubjects.first()
            val dbVersion = withContext(Dispatchers.IO) {
                runCatching { database.openHelper.readableDatabase.version }.getOrDefault(0)
            }
            _status.value = DevStatus(
                isLoggedIn = user != null,
                displayName = AuthUser.clean(user?.nickname) ?: "-",
                phone = user?.maskedPhone ?: "-",
                tokenStatus = tokenStatusOf(token),
                province = province,
                subjects = subjects.joinToString(" · ").ifBlank { "-" },
                dbVersion = dbVersion,
            )
        }
    }

    fun exportLogs() {
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) { LogExporter.exportLogs(appContext) }
            emit(if (uri != null) "已导出到下载目录 ScoreTrace/" else "导出失败", uri != null)
        }
    }

    fun exportCrashes() {
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) { LogExporter.exportCrashes(appContext) }
            emit(
                when {
                    uri != null -> "崩溃日志已导出到下载目录 ScoreTrace/"
                    else -> "暂无崩溃日志"
                },
                uri != null,
            )
        }
    }

    /** 仅 Debug 构建暴露：故意抛出未捕获异常，验证 CrashHandler 落盘链路。 */
    fun triggerTestCrash() {
        AppLogger.w("Crash", "手动触发测试崩溃")
        throw RuntimeException("测试崩溃（来自开发者选项）")
    }

    private fun emit(text: String, success: Boolean) {
        _toast.tryEmit(
            ToastMessage(text, if (success) AppToastType.Success else AppToastType.Warning),
        )
    }

    /** 解析 Authing access token（JWT）的 exp；无法解析时按「有效」处理。 */
    private fun tokenStatusOf(token: String?): String {
        if (token.isNullOrBlank()) return "无 token"
        val expSeconds = runCatching {
            val payload = token.split(".").getOrNull(1) ?: return@runCatching null
            val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP))
            Regex("\"exp\"\\s*:\\s*(\\d+)").find(json)?.groupValues?.get(1)?.toLong()
        }.getOrNull() ?: return "有效（无法解析 exp）"
        val remainMs = expSeconds * 1000 - System.currentTimeMillis()
        if (remainMs <= 0) return "已过期"
        val hours = remainMs / 3_600_000
        return "有效（剩余约 ${hours} 小时）"
    }
}
