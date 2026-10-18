package com.fenji.scoretrace

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.fenji.scoretrace.data.local.UserPreferences
import com.fenji.scoretrace.ui.component.AppToastHost
import com.fenji.scoretrace.ui.component.CrashReportDialogHost
import com.fenji.scoretrace.ui.navigation.AppNavHost
import com.fenji.scoretrace.ui.screen.splash.PosterSplash
import com.fenji.scoretrace.ui.theme.ScoreTraceTheme
import com.fenji.scoretrace.util.AppLogger
import com.fenji.scoretrace.util.DateUtils
import com.fenji.scoretrace.util.LocationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        lifecycleScope.launch { resolveProvince(granted) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 12+ 启动画面：必须在 setContentView/setContent 之前安装（文档要求置于 super.onCreate 之前）
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppLogger.i("AppStart", "MainActivity onCreate")
        // 同步读取一次主题偏好作首帧初值：否则首帧会先按「跟随系统」渲染，拿到持久化值后再切，
        // 在「深色/浅色」与系统不一致时出现一次闪色（旧数据 install -r 时更明显）。
        val initialThemeMode = runCatching { runBlocking { userPreferences.themeMode.first() } }
            .getOrDefault("system")
        // 启动海报上的高考倒计时：优先用用户设置的高考日期，读不到时回退默认日期。
        val initialGaokaoTimestamp = runCatching { runBlocking { userPreferences.gaokaoTimestamp.first() } }
            .getOrDefault(DateUtils.defaultGaokaoTimestamp())
        // 保持系统启动画面到首帧完成即可（海报层会立即接管），避免系统 Splash 遮住海报动画。
        var keepSplash by mutableStateOf(true)
        splashScreen.setKeepOnScreenCondition { keepSplash }
        setContent {
            var showPoster by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                delay(SPLASH_MIN_DURATION_MS)
                keepSplash = false
                delay(SPLASH_POSTER_DURATION_MS)
                showPoster = false
            }
            val themeMode by userPreferences.themeMode.collectAsStateWithLifecycle(initialValue = initialThemeMode)
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            ScoreTraceTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 底栏可见性由 AppNavHost 回传，供 Toast 定位避让底栏
                        var bottomBarVisible by remember { mutableStateOf(true) }
                        AppNavHost(onBottomBarVisibleChange = { bottomBarVisible = it })
                        AppToastHost(
                            bottomPadding = if (bottomBarVisible) 72.dp else 16.dp,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                        CrashReportDialogHost()
                        AnimatedVisibility(
                            visible = showPoster,
                            enter = fadeIn(animationSpec = tween(durationMillis = 0)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 450)),
                        ) {
                            PosterSplash(gaokaoTimestamp = initialGaokaoTimestamp)
                        }
                    }
                }
            }
        }
        autoDetectProvinceOnFirstLaunch()
    }

    /**
     * 首次启动（且用户从未设置过省份）时申请粗定位权限并定位一次，写入省份偏好。
     * 用 [UserPreferences.provinceLocated] 标记保证只询问一次；拒绝或失败时保持默认「云南」。
     */
    private fun autoDetectProvinceOnFirstLaunch() {
        lifecycleScope.launch {
            if (userPreferences.provinceLocated.first()) return@launch
            if (userPreferences.hasExplicitProvince()) {
                userPreferences.setProvinceLocated(true)
                return@launch
            }
            if (hasCoarseLocationPermission()) {
                resolveProvince(granted = true)
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    }

    private suspend fun resolveProvince(granted: Boolean) {
        if (granted) {
            LocationHelper.getCurrentProvince(this)?.let { userPreferences.setProvince(it) }
        }
        // 无论授权、拒绝还是定位失败都标记完成，避免下次启动重复打扰
        userPreferences.setProvinceLocated(true)
    }

    private fun hasCoarseLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        /** 系统 Splash 最短展示时长：只覆盖首帧，随后交给海报层。 */
        const val SPLASH_MIN_DURATION_MS = 300L

        /** 海报层停留时长（不含淡出）。 */
        const val SPLASH_POSTER_DURATION_MS = 2000L
    }
}
