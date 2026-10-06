package com.fenji.scoretrace.ui.screen.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.model.AuthUser
import com.fenji.scoretrace.data.repository.AuthRepository
import com.fenji.scoretrace.util.AppToastType
import com.fenji.scoretrace.util.ToastMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 账号相关页面的状态与操作（登录页 / 账号设置页共用）。
 *
 * 登录态由 [AuthRepository.observeCurrentUser] 驱动；操作结果与校验错误统一经 [toast] 上报，
 * 由页面转交 `AppToast`（沿用 v5.16 的全局轻提示）。
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    data class UiState(
        val isLoggedIn: Boolean = false,
        val user: AuthUser? = null,
        val isLoading: Boolean = false,
        /** 手机号验证码剩余秒数，0 表示可重新发送 */
        val phoneCountdown: Int = 0,
        /** 邮箱验证码剩余秒数 */
        val emailCountdown: Int = 0,
    )

    /**
     * [isLoggedIn] 初值同步读本地登录态：首帧就要正确，否则已登录用户会先闪一下登录页。
     * 之后由 [AuthRepository.observeCurrentUser] 驱动刷新。
     */
    private val _uiState = MutableStateFlow(UiState(isLoggedIn = authRepository.isLoggedIn()))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _toast = MutableSharedFlow<ToastMessage>(extraBufferCapacity = 4)
    val toast: SharedFlow<ToastMessage> = _toast.asSharedFlow()

    private var phoneCountdownJob: Job? = null
    private var emailCountdownJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.observeCurrentUser().collect { user ->
                _uiState.update { it.copy(isLoggedIn = user != null, user = user) }
            }
        }
    }

    fun sendSmsCode(phone: String) {
        if (!isValidPhone(phone)) {
            notify("请输入 11 位手机号", AppToastType.Warning)
            return
        }
        submit {
            authRepository.sendSmsCode(phone)
                .onSuccess {
                    startCountdown(isEmail = false)
                    notify("验证码已发送", AppToastType.Success)
                }
                .onFailure { notify(messageOf(it), AppToastType.Error) }
        }
    }

    fun sendEmailCode(email: String) {
        if (!isValidEmail(email)) {
            notify("请输入正确的邮箱地址", AppToastType.Warning)
            return
        }
        submit {
            authRepository.sendEmailCode(email)
                .onSuccess {
                    startCountdown(isEmail = true)
                    notify("验证码已发送", AppToastType.Success)
                }
                .onFailure { notify(messageOf(it), AppToastType.Error) }
        }
    }

    fun loginByPhone(phone: String, code: String) {
        if (!isValidPhone(phone)) {
            notify("请输入 11 位手机号", AppToastType.Warning)
            return
        }
        if (code.isBlank()) {
            notify("请输入验证码", AppToastType.Warning)
            return
        }
        submit {
            authRepository.loginByPhoneCode(phone, code)
                .onSuccess { notify("登录成功", AppToastType.Success) }
                .onFailure { notify(messageOf(it), AppToastType.Error) }
        }
    }

    fun loginByEmail(email: String, code: String) {
        if (!isValidEmail(email)) {
            notify("请输入正确的邮箱地址", AppToastType.Warning)
            return
        }
        if (code.isBlank()) {
            notify("请输入验证码", AppToastType.Warning)
            return
        }
        submit {
            authRepository.loginByEmailCode(email, code)
                .onSuccess { notify("登录成功", AppToastType.Success) }
                .onFailure { notify(messageOf(it), AppToastType.Error) }
        }
    }

    fun loginByPassword(account: String, password: String) {
        if (account.isBlank()) {
            notify("请输入手机号或邮箱", AppToastType.Warning)
            return
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            notify("密码至少 $MIN_PASSWORD_LENGTH 位", AppToastType.Warning)
            return
        }
        submit {
            authRepository.loginByPassword(account.trim(), password)
                .onSuccess { notify("登录成功", AppToastType.Success) }
                .onFailure { notify(messageOf(it), AppToastType.Error) }
        }
    }

    fun registerByPhone(phone: String, code: String, password: String) {
        if (!isValidPhone(phone)) {
            notify("请输入 11 位手机号", AppToastType.Warning)
            return
        }
        if (code.isBlank()) {
            notify("请输入验证码", AppToastType.Warning)
            return
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            notify("密码至少 $MIN_PASSWORD_LENGTH 位", AppToastType.Warning)
            return
        }
        submit {
            authRepository.registerByPhoneCode(phone, code, password)
                .onSuccess { notify("注册成功", AppToastType.Success) }
                .onFailure { notify(messageOf(it), AppToastType.Error) }
        }
    }

    fun registerByEmail(email: String, code: String) {
        if (!isValidEmail(email)) {
            notify("请输入正确的邮箱地址", AppToastType.Warning)
            return
        }
        if (code.isBlank()) {
            notify("请输入验证码", AppToastType.Warning)
            return
        }
        submit {
            authRepository.registerByEmailCode(email, code)
                .onSuccess { notify("注册成功", AppToastType.Success) }
                .onFailure { notify(messageOf(it), AppToastType.Error) }
        }
    }

    fun logout() {
        submit {
            authRepository.logout()
                .onSuccess { notify("已退出登录", AppToastType.Success) }
                .onFailure { notify(messageOf(it), AppToastType.Error) }
        }
    }

    fun deleteAccount() {
        submit {
            authRepository.deleteAccount()
                .onSuccess { notify("账号已注销", AppToastType.Success) }
                .onFailure { notify(messageOf(it), AppToastType.Error) }
        }
    }

    private fun submit(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                block()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun startCountdown(isEmail: Boolean) {
        if (isEmail) emailCountdownJob?.cancel() else phoneCountdownJob?.cancel()
        val job = viewModelScope.launch {
            for (remaining in COUNTDOWN_SECONDS downTo 1) {
                setCountdown(isEmail, remaining)
                delay(1000L)
            }
            setCountdown(isEmail, 0)
        }
        if (isEmail) emailCountdownJob = job else phoneCountdownJob = job
    }

    private fun setCountdown(isEmail: Boolean, value: Int) {
        _uiState.update { if (isEmail) it.copy(emailCountdown = value) else it.copy(phoneCountdown = value) }
    }

    private fun notify(text: String, type: AppToastType) {
        _toast.tryEmit(ToastMessage(text, type))
    }

    private fun messageOf(error: Throwable): String =
        error.message?.takeIf { it.isNotBlank() } ?: "操作失败，请重试"

    private fun isValidPhone(phone: String): Boolean = PHONE_REGEX.matches(phone)

    private fun isValidEmail(email: String): Boolean =
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private companion object {
        const val COUNTDOWN_SECONDS = 60
        const val MIN_PASSWORD_LENGTH = 8
        val PHONE_REGEX = Regex("^1\\d{10}$")
    }
}
