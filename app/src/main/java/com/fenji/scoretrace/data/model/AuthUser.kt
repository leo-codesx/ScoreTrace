package com.fenji.scoretrace.data.model

/**
 * 账号系统的领域用户信息。
 *
 * 刻意与 Authing SDK 的 `cn.authing.guard.data.UserInfo` 解耦：SDK 类型不外泄到 UI 与持久化，
 * 序列化进 DataStore 的也是本类。放在 data.model 包以复用既有的 R8/Gson 保留规则。
 */
data class AuthUser(
    val id: String,
    val nickname: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val photo: String? = null,
    val createdAt: String? = null,
) {
    /** 脱敏手机号：138****8888；号码异常时原样返回。 */
    val maskedPhone: String?
        get() = phone?.takeIf { it.length == 11 }?.let { "${it.take(3)}****${it.takeLast(4)}" } ?: phone

    /** 脱敏邮箱：ab***@example.com；无 @ 时原样返回。 */
    val maskedEmail: String?
        get() = email?.let { value ->
            val at = value.indexOf('@')
            if (at <= 0) value else value.take(2).take(at) + "***" + value.substring(at)
        }
}
