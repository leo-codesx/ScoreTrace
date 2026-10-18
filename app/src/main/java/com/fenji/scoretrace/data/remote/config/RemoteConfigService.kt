package com.fenji.scoretrace.data.remote.config

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/** 远程公告 / 活动弹窗配置。时间字段为 epoch 毫秒，0 表示不限制。 */
data class RemoteDialogConfig(
    val id: String,
    val title: String,
    val content: String,
    val imageUrl: String? = null,
    val buttonText: String? = null,
    val buttonUrl: String? = null,
    val showOnce: Boolean = true,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
)

private data class RemoteDialogDto(
    val id: String? = null,
    val title: String? = null,
    val content: String? = null,
    val imageUrl: String? = null,
    val buttonText: String? = null,
    val buttonUrl: String? = null,
    val showOnce: Boolean = true,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
)

/**
 * 从 GitHub Gist / 蓝奏云笔记等地址拉取公告 JSON（单对象或数组取第一条）。
 * 地址由 `local.properties` 的 `remote.config.url` 注入，为空时功能自动关闭。
 */
@Singleton
class RemoteConfigService @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson,
) {
    suspend fun fetch(url: String): RemoteDialogConfig? = withContext(Dispatchers.IO) {
        runCatching {
            val body = client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.string()
            } ?: return@withContext null

            val root = JsonParser.parseString(body)
            val objectNode = when {
                root.isJsonArray -> root.asJsonArray.firstOrNull { it.isJsonObject }?.asJsonObject
                root.isJsonObject -> root.asJsonObject
                else -> null
            } ?: return@withContext null

            val dto = gson.fromJson(objectNode, RemoteDialogDto::class.java)
            val title = dto.title?.takeIf { it.isNotBlank() } ?: return@withContext null
            val content = dto.content.orEmpty()
            RemoteDialogConfig(
                id = dto.id?.takeIf { it.isNotBlank() } ?: title.hashCode().toString(),
                title = title,
                content = content,
                imageUrl = dto.imageUrl?.takeIf { it.isNotBlank() },
                buttonText = dto.buttonText?.takeIf { it.isNotBlank() },
                buttonUrl = dto.buttonUrl?.takeIf { it.isNotBlank() },
                showOnce = dto.showOnce,
                startTime = dto.startTime,
                endTime = dto.endTime,
            )
        }.getOrNull()
    }
}
