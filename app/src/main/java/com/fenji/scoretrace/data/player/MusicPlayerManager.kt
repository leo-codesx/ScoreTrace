package com.fenji.scoretrace.data.player

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import com.fenji.scoretrace.data.repository.MusicRepository
import com.fenji.scoretrace.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** 播放列表中的一首曲目。 */
data class TrackInfo(
    val id: String,
    val title: String,
    val artist: String,
    val url: String,
)

/** 默认播放列表（列表循环）。 */
val DEFAULT_PLAYLIST = listOf(
    TrackInfo(
        id = "wu-xian",
        title = "无限",
        artist = "周深",
        url = "https://www.lequxiang.com.cn/view.php/43566d645a97e595dd047c7089194558.ogg",
    ),
    TrackInfo(
        id = "chong-qi",
        title = "重启",
        artist = "周深",
        url = "https://www.lequxiang.com.cn/view.php/4a15c67dfc3011f82ef52282c1728d59.ogg",
    ),
    TrackInfo(
        id = "whisper-of-hope",
        title = "Whisper Of Hope",
        artist = "Gothic Storm",
        url = "https://www.lequxiang.com.cn/view.php/80e6affab3677c4e9264648b4cb150b2.mp3",
    ),
)

/**
 * 应用级音乐播放器。
 *
 * 播放器实例不再跟随页面/ViewModel 生命周期，切换底部 Tab 时不会被暂停或释放；
 * 懒加载创建，整个进程内只创建一次。
 *
 * 前后台切换由 [com.fenji.scoretrace.ScoreTraceApp] 的 ActivityLifecycleCallbacks 驱动
 * （不用 ProcessLifecycleOwner，它自带约 700ms 防抖，按 Home 键后要等一会儿才停）。
 */
@Singleton
class MusicPlayerManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val musicCache: MusicCacheRepository,
    private val musicRepository: MusicRepository,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 在线歌单是否已尝试加载（成功或失败都置 true）；用于自动播放前短暂等待。 */
    private val _playlistReady = MutableStateFlow(false)
    val playlistReady: StateFlow<Boolean> = _playlistReady.asStateFlow()

    private var remotePlaylistJob: Job? = null

    private var _exoPlayer: ExoPlayer? = null
    private var _audioCache: Cache? = null

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var hasAudioFocus = false
    private var wasPlayingBeforeFocusLoss = false

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                wasPlayingBeforeFocusLoss = _exoPlayer?.isPlaying == true
                _exoPlayer?.playWhenReady = false
                abandonAudioFocus()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                wasPlayingBeforeFocusLoss = _exoPlayer?.isPlaying == true
                _exoPlayer?.playWhenReady = false
            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                if (wasPlayingBeforeFocusLoss) {
                    wasPlayingBeforeFocusLoss = false
                    play()
                }
            }
        }
    }

    private val focusRequest: AudioFocusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setOnAudioFocusChangeListener(focusChangeListener, Handler(Looper.getMainLooper()))
            .build()
    }

    /** 当前播放列表（可增删）；与播放器的 media items 保持一致 */
    private val _playlist = MutableStateFlow(musicCache.loadPlaylist() ?: DEFAULT_PLAYLIST)
    val playlist: StateFlow<List<TrackInfo>> = _playlist.asStateFlow()

    val exoPlayer: ExoPlayer
        get() {
            if (_exoPlayer == null) {
                // 按上次退出的歌曲与进度恢复起始位置
                val snapshot = musicCache.load()
                val startIndex = snapshot
                    ?.let { s -> _playlist.value.indexOfFirst { it.id == s.songId }.takeIf { it >= 0 } }
                    ?: 0
                val startPosition = snapshot?.positionMs?.coerceAtLeast(0L) ?: 0L

                val mediaSourceFactory = DefaultMediaSourceFactory(context)
                    .setDataSourceFactory(OkHttpDataSource.Factory(createAudioHttpClient()))
                _exoPlayer = ExoPlayer.Builder(context)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .build()
                    .apply {
                        setMediaItems(_playlist.value.map { MediaItem.fromUri(it.url) }, startIndex, startPosition)
                        repeatMode = Player.REPEAT_MODE_ALL
                        volume = DEFAULT_VOLUME
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(C.USAGE_MEDIA)
                                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                                .build(),
                            false,
                        )
                        addListener(object : Player.Listener {
                            override fun onPlayerError(error: PlaybackException) {
                                AppLogger.e(
                                    TAG,
                                    "player error code=${error.errorCode} name=${error.errorCodeName} message=${error.message}",
                                    error,
                                )
                            }

                            override fun onPlaybackStateChanged(playbackState: Int) {
                                AppLogger.i(TAG, "playbackState=$playbackState")
                            }
                        })
                        // 显式声明：只有调用 play() 才出声，不依赖 ExoPlayer 的默认值，
                        // 以免绕过「进入应用自动播放音乐」开关
                        playWhenReady = false
                        prepare()
                    }
            }
            return _exoPlayer!!
        }

    /**
     * 音频磁盘缓存用独立 OkHttpClient：不复用 Retrofit 那个，免得 BODY 级日志把音频字节全打出来。
     * 服务端未返回缓存头时用网络拦截器补上 Cache-Control，保证音频能落盘、二次播放走缓存。
     */
    private fun createAudioHttpClient(): OkHttpClient {
        val cache = Cache(File(context.cacheDir, AUDIO_CACHE_DIR), AUDIO_CACHE_BYTES)
        _audioCache = cache
        return OkHttpClient.Builder()
            .cache(cache)
            .addNetworkInterceptor { chain ->
                chain.proceed(chain.request()).newBuilder()
                    .header("Cache-Control", "public, max-age=$AUDIO_CACHE_MAX_AGE_SECONDS")
                    .removeHeader("Pragma")
                    .build()
            }
            .build()
    }

    /**
     * 进程内只加载一次在线歌单：成功则替换 [playlist]；失败由 Repository 回退静态列表。
     * 若播放器已创建且未在播放，会把新媒体项替换进去；正在播放时保持当前曲目，避免打断。
     */
    fun loadRemotePlaylistOnce() {
        if (remotePlaylistJob != null || _playlistReady.value) return
        remotePlaylistJob = scope.launch {
            val tracks = withTimeoutOrNull(8_000L) {
                musicRepository.fetchDefaultPlaylist()
            }.orEmpty()
            // 只缓存真正的在线歌单，避免接口失败时用静态列表覆盖上一轮缓存。
            if (tracks.isNotEmpty() && tracks != DEFAULT_PLAYLIST) {
                musicCache.savePlaylist(tracks)
            }
            // ExoPlayer 必须在创建它的主线程访问，不能在 IO 协程里 setMediaItems/prepare。
            withContext(Dispatchers.Main.immediate) {
                runCatching {
                    if (tracks.isNotEmpty()) {
                        val player = _exoPlayer
                        // 正在播放时只更新缓存，不替换队列，避免打断当前播放。
                        if (player == null || !player.isPlaying) {
                            _playlist.value = tracks
                            player?.setMediaItems(tracks.map { MediaItem.fromUri(it.url) }, 0, 0L)
                            player?.prepare()
                        }
                    }
                }.onFailure { AppLogger.e(TAG, "应用在线歌单失败", it) }
                _playlistReady.value = true
            }
        }
    }

    /** 载入播放列表并从 [startIndex] 开始（只 prepare，不自动播放，由调用方决定）。 */
    fun setPlaylist(tracks: List<TrackInfo>, startIndex: Int = 0) {
        if (!requireMainThread()) return
        _playlist.value = tracks
        exoPlayer.setMediaItems(tracks.map { MediaItem.fromUri(it.url) }, startIndex, 0L)
        exoPlayer.prepare()
    }

    fun skipToNext(): Boolean {
        if (!requireMainThread()) return false
        exoPlayer.seekToNextMediaItem()
        return true
    }

    fun skipToPrevious(): Boolean {
        if (!requireMainThread()) return false
        exoPlayer.seekToPreviousMediaItem()
        return true
    }

    /** 切到后台前是否正在播放——决定回前台要不要自动恢复 */
    private var wasPlayingBeforeBackground = false

    /**
     * 应用整体退到后台（所有 Activity 都已 stopped）。
     * 用 `_exoPlayer` 判空：从没播过就退后台时，不要凭空创建播放器。
     */
    fun onAppBackground() {
        val player = _exoPlayer
        wasPlayingBeforeBackground = player?.isPlaying == true
        // 记住歌曲与进度，供下次冷启动恢复
        if (player != null) {
            val songId = _playlist.value.getOrNull(player.currentMediaItemIndex)?.id
            if (songId != null) {
                musicCache.save(
                    songId = songId,
                    positionMs = player.currentPosition.coerceAtLeast(0L),
                )
            }
        }
        if (wasPlayingBeforeBackground) pause()
    }

    /** 应用回到前台：只恢复"上次是被自动暂停的"播放，用户手动暂停过的保持暂停。 */
    fun onAppForeground() {
        if (wasPlayingBeforeBackground) {
            wasPlayingBeforeBackground = false
            play()
        }
    }

    fun play(): Boolean {
        if (!requireMainThread()) return false
        if (!requestAudioFocus()) {
            AppLogger.e(TAG, "requestAudioFocus denied")
            return false
        }
        exoPlayer.playWhenReady = true
        return true
    }

    fun pause() {
        if (!requireMainThread()) return
        _exoPlayer?.playWhenReady = false
        abandonAudioFocus()
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        hasAudioFocus = audioManager.requestAudioFocus(focusRequest) ==
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (hasAudioFocus) {
            audioManager.abandonAudioFocusRequest(focusRequest)
            hasAudioFocus = false
        }
    }

    private fun requireMainThread(): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) return true
        AppLogger.e(TAG, "player operation off main thread: ${Thread.currentThread().name}")
        return false
    }

    /**
     * 释放底层播放器。当前没有调用方——播放器按设计活到进程结束；
     * 若将来需要「退出应用就释放」，应在这里接上前台服务或生命周期钩子。
     */
    fun release() {
        abandonAudioFocus()
        _exoPlayer?.release()
        _exoPlayer = null
        _audioCache?.close()
        _audioCache = null
    }

    private companion object {
        const val TAG = "MusicPlayer"
        const val AUDIO_CACHE_DIR = "audio_cache"
        const val AUDIO_CACHE_BYTES = 100L * 1024 * 1024
        const val AUDIO_CACHE_MAX_AGE_SECONDS = 604800L

        /** 默认音量：0.65（原 1.0 偏大） */
        const val DEFAULT_VOLUME = 0.65f
    }
}
