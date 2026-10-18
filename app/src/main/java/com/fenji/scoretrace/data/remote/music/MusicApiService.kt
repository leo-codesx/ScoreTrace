package com.fenji.scoretrace.data.remote.music

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Meting-API 兼容接口。
 *
 * 不同公开实例字段命名略有差异：有的返回 title/author，有的返回 name/artist，
 * DTO 同时保留两组字段，由 Repository 统一归一。
 */
interface MusicApiService {

    @GET("api")
    suspend fun playlist(
        @Query("server") server: String = "netease",
        @Query("type") type: String = "playlist",
        @Query("id") id: String,
    ): List<MusicTrackDto>

    @GET("api")
    suspend fun search(
        @Query("server") server: String = "netease",
        @Query("type") type: String = "search",
        @Query("id") id: String = "0",
        @Query("keyword") keyword: String,
    ): List<MusicTrackDto>

    /** 歌词接口返回纯文本 LRC。 */
    @GET("api")
    suspend fun lyric(
        @Query("server") server: String = "netease",
        @Query("type") type: String = "lrc",
        @Query("id") id: String,
    ): String
}

data class MusicTrackDto(
    val title: String? = null,
    val name: String? = null,
    val author: String? = null,
    val artist: String? = null,
    val url: String? = null,
    val pic: String? = null,
    val lrc: String? = null,
)
