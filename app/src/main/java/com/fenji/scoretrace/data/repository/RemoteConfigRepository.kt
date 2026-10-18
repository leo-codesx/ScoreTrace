package com.fenji.scoretrace.data.repository

import android.content.Context
import com.fenji.scoretrace.BuildConfig
import com.fenji.scoretrace.data.remote.config.RemoteConfigService
import com.fenji.scoretrace.data.remote.config.RemoteDialogConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface RemoteConfigRepository {
    /** 拉取当前时间窗内的公告；未配置地址、请求失败或不在时间窗时返回 null。 */
    suspend fun fetchActive(): RemoteDialogConfig?

    /** showOnce=true 时永久去重；false 时仅在本进程内去重。 */
    fun shouldShow(config: RemoteDialogConfig): Boolean

    fun markShown(config: RemoteDialogConfig)
}

@Singleton
class DefaultRemoteConfigRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val service: RemoteConfigService,
) : RemoteConfigRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val shownThisProcess = mutableSetOf<String>()

    override suspend fun fetchActive(): RemoteDialogConfig? {
        val url = BuildConfig.REMOTE_CONFIG_URL
        if (url.isBlank()) return null
        val config = service.fetch(url) ?: return null
        val now = System.currentTimeMillis()
        val started = config.startTime <= 0L || now >= config.startTime
        val notEnded = config.endTime <= 0L || now <= config.endTime
        return config.takeIf { started && notEnded }
    }

    override fun shouldShow(config: RemoteDialogConfig): Boolean {
        if (config.id in shownThisProcess) return false
        return !(config.showOnce && prefs.getStringSet(KEY_SHOWN_IDS, emptySet())?.contains(config.id) == true)
    }

    override fun markShown(config: RemoteDialogConfig) {
        shownThisProcess.add(config.id)
        if (config.showOnce) {
            val ids = prefs.getStringSet(KEY_SHOWN_IDS, emptySet()).orEmpty().toMutableSet()
            ids.add(config.id)
            prefs.edit().putStringSet(KEY_SHOWN_IDS, ids).apply()
        }
    }

    private companion object {
        const val PREFS_NAME = "remote_config"
        const val KEY_SHOWN_IDS = "shown_dialog_ids"
    }
}
