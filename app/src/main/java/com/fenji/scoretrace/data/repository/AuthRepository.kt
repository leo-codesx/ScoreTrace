package com.fenji.scoretrace.data.repository

import cn.authing.guard.AuthCallback
import cn.authing.guard.Authing
import cn.authing.guard.data.UserInfo
import cn.authing.guard.network.AuthClient
import com.fenji.scoretrace.data.local.UserPreferences
import com.fenji.scoretrace.data.model.AuthUser
import com.fenji.scoretrace.util.AppLogger
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** 账号相关错误：携带 Authing 返回的错误码 [code] 与提示 [message]。 */
class AuthException(val code: Int?, override val message: String) : Exception(message)

/**
 * 账号仓库：把 Authing Guard SDK 的回调式 `AuthClient` 封装成 suspend API。
 *
 * 登录态真源是 [UserPreferences] 里持久化的 [AuthUser]（登录写入、退出/注销清空），
 * 这样进程重启后 UI 也能立即恢复登录态，无需等待 SDK 的 `autoLogin`。
 * Token 由 SDK 自身以加密方式持久化，本仓库只额外镜像一份 access token 供未来云端同步使用。
 */
interface AuthRepository {

    /** 当前登录用户；null 表示未登录。登录/退出后通过 Flow 即时刷新。 */
    fun observeCurrentUser(): Flow<AuthUser?>

    /** 本地是否存在有效登录态（读 SDK 从磁盘恢复的用户，不发起网络请求）。 */
    fun isLoggedIn(): Boolean

    suspend fun sendSmsCode(phone: String): Result<Unit>

    suspend fun sendEmailCode(email: String): Result<Unit>

    suspend fun registerByPhoneCode(phone: String, code: String, password: String): Result<AuthUser>

    /** Authing 的邮箱验证码注册不接受密码（密码后续单独设置），故不带 password 参数。 */
    suspend fun registerByEmailCode(email: String, code: String): Result<AuthUser>

    suspend fun loginByPhoneCode(phone: String, code: String): Result<AuthUser>

    suspend fun loginByEmailCode(email: String, code: String): Result<AuthUser>

    suspend fun loginByPassword(account: String, password: String): Result<AuthUser>

    suspend fun logout(): Result<Unit>

    suspend fun deleteAccount(): Result<Unit>
}

@Singleton
class DefaultAuthRepository @Inject constructor(
    private val userPreferences: UserPreferences,
    private val gson: Gson,
) : AuthRepository {

    override fun observeCurrentUser(): Flow<AuthUser?> = userPreferences.authUserInfo.map { json ->
        json?.let { runCatching { gson.fromJson(it, AuthUser::class.java) }.getOrNull() }
    }

    override fun isLoggedIn(): Boolean = Authing.getCurrentUser() != null

    override suspend fun sendSmsCode(phone: String): Result<Unit> =
        callAuth<Any> { callback -> AuthClient.sendSms(phone, callback) }.map { }

    override suspend fun sendEmailCode(email: String): Result<Unit> =
        callAuth<JSONObject> { callback -> AuthClient.sendEmail(email, EMAIL_SCENE, callback) }.map { }

    override suspend fun registerByPhoneCode(phone: String, code: String, password: String): Result<AuthUser> =
        loginSession { callback -> AuthClient.registerByPhoneCode(phone, code, password, callback) }

    override suspend fun registerByEmailCode(email: String, code: String): Result<AuthUser> =
        loginSession { callback -> AuthClient.registerByEmailCode(email, code, callback) }

    override suspend fun loginByPhoneCode(phone: String, code: String): Result<AuthUser> =
        loginSession { callback -> AuthClient.loginByPhoneCode(phone, code, callback) }

    override suspend fun loginByEmailCode(email: String, code: String): Result<AuthUser> =
        loginSession { callback -> AuthClient.loginByEmailCode(email, code, callback) }

    override suspend fun loginByPassword(account: String, password: String): Result<AuthUser> =
        loginSession { callback -> AuthClient.loginByAccount(account, password, callback) }

    /** 退出登录：无论 SDK 调用成败（如离线）都清空本地登录态，保证用户一定能退出。 */
    override suspend fun logout(): Result<Unit> {
        val result = callAuth<Any> { callback -> AuthClient.logout(callback) }.map { }
        clearSession()
        return result
    }

    /** 注销账号：成功后清空本地登录态（SDK 侧的用户已被删除）。 */
    override suspend fun deleteAccount(): Result<Unit> {
        val result = callAuth<JSONObject> { callback -> AuthClient.deleteAccount(callback) }.map { }
        if (result.isSuccess) clearSession()
        return result
    }

    /** 登录/注册共用：成功后落地登录态并返回领域用户。 */
    private suspend fun loginSession(block: (AuthCallback<UserInfo>) -> Unit): Result<AuthUser> =
        callAuth(block).mapCatching { userInfo ->
            val user = (userInfo ?: throw AuthException(null, "未获取到用户信息")).toAuthUser()
            persistSession(user, userInfo.getAccessToken())
            AppLogger.i(TAG, "login success userId=${user.id}")
            user
        }

    private suspend fun persistSession(user: AuthUser, token: String?) {
        userPreferences.setAuthToken(token)
        userPreferences.setAuthUserId(user.id)
        userPreferences.setAuthUserInfo(gson.toJson(user))
    }

    private suspend fun clearSession() {
        userPreferences.clearAuth()
    }

    private fun UserInfo.toAuthUser(): AuthUser = AuthUser(
        id = getId() ?: getSub() ?: "",
        nickname = getNickname() ?: getName() ?: getUsername(),
        phone = getPhone_number(),
        email = getEmail(),
        photo = getPhoto() ?: getPicture(),
        createdAt = getCreated_at(),
    )

    /** 回调式 API → suspend。成功（code == 200）时返回 data（发送验证码等操作的 data 可能为 null）。 */
    private suspend fun <T> callAuth(block: (AuthCallback<T>) -> Unit): Result<T?> =
        suspendCancellableCoroutine { continuation ->
            block(object : AuthCallback<T> {
                override fun call(code: Int, message: String?, data: T?) {
                    if (!continuation.isActive) return
                    if (code == CODE_SUCCESS) {
                        continuation.resume(Result.success(data))
                    } else {
                        continuation.resume(
                            Result.failure(AuthException(code, errorText(code, message))),
                        )
                    }
                }
            })
        }

    private fun errorText(code: Int, message: String?): String =
        message?.takeIf { it.isNotBlank() } ?: "操作失败（错误码 $code）"

    private companion object {
        const val TAG = "Auth"
        const val CODE_SUCCESS = 200

        /** 邮箱验证码场景，与 Guard 内置 `GetEmailCodeButton` 的默认值一致。 */
        const val EMAIL_SCENE = "VERIFY_CODE"
    }
}
