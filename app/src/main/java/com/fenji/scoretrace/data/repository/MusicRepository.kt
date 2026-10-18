package com.fenji.scoretrace.data.repository

import com.fenji.scoretrace.BuildConfig
import com.fenji.scoretrace.data.player.DEFAULT_PLAYLIST
import com.fenji.scoretrace.data.player.TrackInfo
import com.fenji.scoretrace.data.remote.music.MusicApiService
import com.fenji.scoretrace.data.remote.music.MusicTrackDto
import com.fenji.scoretrace.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 在线音乐数据源。失败时返回现有静态歌单，保证离线/接口失效时仍可播放。
 */
interface MusicRepository {
    /** 拉取默认歌单；任何异常都降级为 [DEFAULT_PLAYLIST]。 */
    suspend fun fetchDefaultPlaylist(): List<TrackInfo>

    suspend fun search(keyword: String): List<TrackInfo>

    suspend fun lyric(trackId: String): String?
}

@Singleton
class DefaultMusicRepository @Inject constructor(
    private val api: MusicApiService,
) : MusicRepository {

    override suspend fun fetchDefaultPlaylist(): List<TrackInfo> = withContext(Dispatchers.IO) {
        runCatching {
            api.playlist(id = BuildConfig.MUSIC_PLAYLIST_ID)
                .mapNotNull(::toTrackInfo)
                .distinctBy { it.id }
        }.onFailure {
            AppLogger.w(TAG, "在线歌单加载失败，回退本地播放列表：${it.message}")
        }.getOrDefault(emptyList()).ifEmpty { DEFAULT_PLAYLIST }
    }

    override suspend fun search(keyword: String): List<TrackInfo> = withContext(Dispatchers.IO) {
        runCatching {
            api.search(keyword = keyword).mapNotNull(::toTrackInfo)
        }.getOrDefault(emptyList())
    }

    override suspend fun lyric(trackId: String): String? = withContext(Dispatchers.IO) {
        runCatching { api.lyric(id = trackId).takeIf { it.isNotBlank() } }.getOrNull()
    }

    private fun toTrackInfo(dto: MusicTrackDto): TrackInfo? {
        val url = dto.url?.takeIf { it.isNotBlank() } ?: return null
        val title = dto.title ?: dto.name ?: return null
        val artist = dto.author ?: dto.artist ?: "未知歌手"
        val id = songIdFromUrl(url) ?: "$title|$artist".hashCode().toString()
        return TrackInfo(id = id, title = title, artist = artist, url = url)
    }

    /** Meting 的播放链接形如 `...type=url&id=3354563193`，取其 id 作为稳定曲目 id。 */
    private fun songIdFromUrl(url: String): String? = runCatching {
        val query = url.substringAfter('?', missingDelimiterValue = "")
        query.split('&').firstOrNull { it.startsWith("id=") }
            ?.substringAfter('=')
            ?.let { URLDecoder.decode(it, "UTF-8") }
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private companion object {
        const val TAG = "MusicRepository"
    }
}
