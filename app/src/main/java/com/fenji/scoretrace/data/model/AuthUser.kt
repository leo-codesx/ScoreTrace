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
    /** 脱敏手机号：180****4493；号码缺失或不足 [MASK_MIN_LENGTH] 位时返回 null（不展示）。 */
    val maskedPhone: String?
        get() = phone
            ?.takeIf { it.length >= MASK_MIN_LENGTH }
            ?.let { "${it.take(3)}****${it.takeLast(4)}" }

    /** 脱敏邮箱：ab***@example.com；无 @ 时原样返回。 */
    val maskedEmail: String?
        get() = email?.let { value ->
            val at = value.indexOf('@')
            if (at <= 0) value else value.take(2).take(at) + "***" + value.substring(at)
        }

    companion object {
        private const val MASK_MIN_LENGTH = 7

        /** Authing 未设置的字段会返回这些字面量字符串，而非 Java null。 */
        private val PLACEHOLDERS = setOf("null", "undefined")

        /**
         * 归一化 Authing 返回的文本：未设置时 SDK 给的是字面量 `"null"`/`"undefined"`，
         * 直接展示就会出现「昵称 null」。统一折算为 null（空白同样视为无值）。
         */
        fun clean(value: String?): String? =
            value?.trim()?.takeIf { it.isNotEmpty() && it.lowercase() !in PLACEHOLDERS }
    }
}
