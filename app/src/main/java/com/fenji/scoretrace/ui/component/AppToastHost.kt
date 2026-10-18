package com.fenji.scoretrace.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dokar.sonner.ToastWidthPolicy
import com.dokar.sonner.Toaster
import com.dokar.sonner.rememberToasterState
import com.fenji.scoretrace.ui.theme.Dimensions
import com.fenji.scoretrace.ui.theme.LocalAppDarkTheme
import com.fenji.scoretrace.util.AppToast
import kotlin.math.roundToInt

/**
 * 全局 Toast 宿主：由 Compose Sonner 渲染，底部弹出、最多堆叠 3 条、支持下滑关闭。
 * 深色模式跟随应用主题；位置避让底部导航栏与软键盘。
 *
 * @param bottomPadding 距屏幕底部的额外间距（底栏可见时传底栏高度，全屏页传更小值）。
 */
@Composable
fun AppToastHost(
    bottomPadding: Dp = 80.dp,
    modifier: Modifier = Modifier,
) {
    val toaster = rememberToasterState()
    DisposableEffect(toaster) {
        AppToast.attach(toaster)
        onDispose { AppToast.detach(toaster) }
    }

    val dark = LocalAppDarkTheme.current
    val density = LocalDensity.current
    val insetBottom = WindowInsets.ime.union(WindowInsets.navigationBars)
        .asPaddingValues()
        .calculateBottomPadding()
    val verticalOffset = with(density) {
        -(bottomPadding + insetBottom + Dimensions.SpacingSmall).roundToPx()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Toaster(
            state = toaster,
            maxVisibleToasts = 3,
            expanded = false,
            swipeable = true,
            richColors = true,
            darkTheme = dark,
            showCloseButton = false,
            shape = { RoundedCornerShape(Dimensions.CornerMedium) },
            elevation = 10.dp,
            containerPadding = PaddingValues(horizontal = Dimensions.SpacingLarge),
            widthPolicy = { ToastWidthPolicy(max = 340.dp) },
            alignment = Alignment.BottomCenter,
            offset = IntOffset(0, verticalOffset),
        )
    }
}
