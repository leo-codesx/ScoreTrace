package com.fenji.scoretrace.data.player

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 上次退出应用时的播放快照 */
data class PlaybackSnapshot(
    val songId: String,
    val positionMs: Long,
)

/**
 * 音乐播放进度的本地缓存。
 *
 * 用轻量的 SharedPreferences 而非 DataStore：播放器创建时要**同步**读取上次进度并设置起始位置，
 * 而 DataStore 只有异步 API，同步读会阻塞。这里存的只是播放位置的临时缓存，不属于用户偏好。
 */
@Singleton
class MusicCacheRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun save(songId: String, positionMs: Long) {
        prefs.edit()
            .putString(KEY_SONG_ID, songId)
            .putLong(KEY_POSITION, positionMs)
            .apply()
    }

    /** 无缓存（首次启动）时返回 null。 */
    fun load(): PlaybackSnapshot? {
        val songId = prefs.getString(KEY_SONG_ID, null) ?: return null
        return PlaybackSnapshot(
            songId = songId,
            positionMs = prefs.getLong(KEY_POSITION, 0L),
        )
    }

    /** 读取上次缓存的在线歌单；无缓存或解析失败返回 null。 */
    fun loadPlaylist(): List<TrackInfo>? {
        val json = prefs.getString(KEY_PLAYLIST, null) ?: return null
        return runCatching {
            val type = object : TypeToken<List<TrackInfo>>() {}.type
            gson.fromJson<List<TrackInfo>>(json, type)?.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    /** 缓存在线歌单，供下次启动立即使用。 */
    fun savePlaylist(tracks: List<TrackInfo>) {
        runCatching {
            prefs.edit().putString(KEY_PLAYLIST, gson.toJson(tracks)).apply()
        }
    }

    private companion object {
        const val PREFS_NAME = "music_cache"
        const val KEY_SONG_ID = "last_song_id"
        const val KEY_POSITION = "last_position"
        const val KEY_PLAYLIST = "cached_playlist"
    }
}
