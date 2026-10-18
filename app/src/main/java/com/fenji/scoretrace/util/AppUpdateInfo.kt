package com.fenji.scoretrace.util

/**
 * 本地强制更新配置。
 *
 * 每次发版只需修改 [LATEST_VERSION_CODE]/[LATEST_VERSION_NAME]；
 * 下载地址必须是蓝奏云固定分享页，不能使用临时 CDN 直链。
 */
object AppUpdateInfo {
    const val LATEST_VERSION_CODE = 1
    const val LATEST_VERSION_NAME = "1.0.1"
    const val DOWNLOAD_WEB_URL = "https://wwbmbz.lanzoul.com/b01i5xlvlc"
    const val DOWNLOAD_PASSWORD = "2ed2"
}
