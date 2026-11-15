package com.fenji.scoretrace

import android.app.Application
import cn.authing.guard.Authing
import com.fenji.scoretrace.util.CrashHandler
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用入口：Hilt 依赖注入的宿主。
 *
 * 音乐的前后台暂停/恢复由 `AppNavHost` 的 LifecycleEventObserver 驱动，
 * 这里不再注册 ActivityLifecycleCallbacks，避免两套逻辑重复暂停。
 */
@HiltAndroidApp
class ScoreTraceApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 崩溃上报（自研）：把未捕获异常堆栈落到 app 私有目录 crash/，下次启动时提示上报。
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))

        // Authing 全局初始化：只做一次。默认协议是私有化部署的 EInHouse，SaaS 应用须显式改为 EOIDC。
        // 不调用 Authing.setHost()——SDK 默认 host=authing.cn，public-config 走 console.authing.cn，
        // 已对当前 App ID 实测可用；传域名反而会被 SDK 加上 console. 前缀而打不通。
        Authing.init(this, BuildConfig.AUTHING_APP_ID)
        Authing.setAuthProtocol(Authing.AuthProtocol.EOIDC)
    }
}
