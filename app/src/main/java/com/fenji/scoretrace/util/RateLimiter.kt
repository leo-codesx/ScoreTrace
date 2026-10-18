package com.fenji.scoretrace.util

import android.content.Context
import com.fenji.scoretrace.data.local.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** 需要按天限额的 AI 能力。 */
enum class AiFeature(val storageKey: String, val dailyLimit: Int) {
    Chat("chat", 50),
    Vision("vision", 10),
}

/**
 * 本地每日 AI 配额。
 *
 * 以 Authing userId + 自然日 + 能力类型作为 key，写入独立的 SharedPreferences；
 * 未登录时用 guest 兜底。只防本机恶意连点，不替代服务端额度。
 */
@Singleton
class RateLimiter @Inject constructor(
    @ApplicationContext context: Context,
    private val userPreferences: UserPreferences,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    suspend fun tryConsume(feature: AiFeature): Boolean = mutex.withLock {
        val key = keyOf(feature)
        val used = prefs.getInt(key, 0)
        if (used >= feature.dailyLimit) {
            false
        } else {
            prefs.edit().putInt(key, used + 1).apply()
            true
        }
    }

    suspend fun remaining(feature: AiFeature): Int = mutex.withLock {
        (feature.dailyLimit - prefs.getInt(keyOf(feature), 0)).coerceAtLeast(0)
    }

    private suspend fun keyOf(feature: AiFeature): String {
        val userId = userPreferences.authUserId.first()?.ifBlank { null } ?: "guest"
        return "${feature.storageKey}:$userId:${LocalDate.now()}"
    }

    private companion object {
        const val PREFS_NAME = "ai_rate_limits"
    }
}
