package com.fenji.scoretrace.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fenji.scoretrace.util.AppLogger
import com.fenji.scoretrace.util.DateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/** 用户偏好设置，目前承载高考日期 */
@Singleton
class UserPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /** 高考日期当天 0 点 0 分 0 秒的时间戳；未设置过时给出默认值（今年或明年 6 月 7 日） */
    val gaokaoTimestamp: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_GAOKAO_TIMESTAMP] ?: DateUtils.defaultGaokaoTimestamp()
    }

    suspend fun saveGaokaoTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GAOKAO_TIMESTAMP] = timestamp
        }
    }

    /** 进入应用是否自动播放音乐 */
    val autoPlayMusic: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_PLAY_MUSIC] ?: true
    }

    suspend fun setAutoPlayMusic(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_PLAY_MUSIC] = enabled
        }
    }

    /** 顶栏/底栏模糊 */
    val enableBlur: Flow<Boolean> = context.dataStore.data.map { it[KEY_ENABLE_BLUR] ?: true }
    suspend fun setEnableBlur(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ENABLE_BLUR] = enabled }
    }

    /** KernelSU 风格悬浮底栏 */
    val enableFloatingBottomBar: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_ENABLE_FLOATING_BOTTOM_BAR] ?: true }
    suspend fun setEnableFloatingBottomBar(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ENABLE_FLOATING_BOTTOM_BAR] = enabled }
    }

    /** 悬浮底栏液态玻璃：默认关闭，回退到干净的半透明胶囊 */
    val enableLiquidGlass: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_ENABLE_LIQUID_GLASS] ?: false }
    suspend fun setEnableLiquidGlass(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ENABLE_LIQUID_GLASS] = enabled }
    }

    /** 预测性返回手势 */
    val enablePredictiveBack: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_ENABLE_PREDICTIVE_BACK] ?: true }
    suspend fun setEnablePredictiveBack(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ENABLE_PREDICTIVE_BACK] = enabled }
    }

    /**
     * 主题模式：[THEME_LIGHT] 浅色 / [THEME_DARK] 深色 / [THEME_SYSTEM] 跟随系统。
     *
     * 兼容旧版布尔开关：未写过 theme_mode 时，若旧键 dark_theme 为 true 则视为深色，否则跟随系统。
     */
    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE]
            ?: if (preferences[KEY_DARK_THEME] == true) THEME_DARK else THEME_SYSTEM
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }

    /** 用户选科（3+1+2），逗号分隔存库；未设置时默认「物理,化学,生物」 */
    val selectedSubjects: Flow<List<String>> = context.dataStore.data.map { preferences ->
        (preferences[KEY_SELECTED_SUBJECTS] ?: DEFAULT_SELECTED_SUBJECTS)
            .split(SUBJECT_SEPARATOR)
            .filter { it.isNotBlank() }
    }

    suspend fun setSelectedSubjects(subjects: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_SUBJECTS] = subjects.joinToString(SUBJECT_SEPARATOR)
        }
        AppLogger.i("SubjectChange", subjects.joinToString(SUBJECT_SEPARATOR))
    }

    /**
     * 用户所在省份（省级行政区名称）；未设置时默认「云南」（兼容存量用户）。
     *
     * 注意：本字段目前仅用于 System Prompt 注入与多地区推广预留设置入口。
     * 分数线相关的展示逻辑仍按云南写定——本科线 435/465、特招线 505/545，
     * 以及云南 C9 投档线均为硬编码，未随省份切换，属于下一阶段任务。
     */
    val province: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_PROVINCE] ?: DEFAULT_PROVINCE
    }

    suspend fun setProvince(value: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PROVINCE] = value
        }
        AppLogger.i("ProvinceChange", value)
    }

    /**
     * 是否已完成「首次启动自动定位」流程（成功、失败、用户拒绝都算完成），
     * 保证只询问一次，不重复打扰用户。
     */
    val provinceLocated: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_PROVINCE_LOCATED] ?: false
    }

    suspend fun setProvinceLocated(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PROVINCE_LOCATED] = value
        }
    }

    /** 用户是否显式设置过省份（用于区分默认「云南」与用户手动选择，避免定位覆盖用户选择）。 */
    suspend fun hasExplicitProvince(): Boolean =
        context.dataStore.data.first()[KEY_PROVINCE] != null

    /** 用户昵称（顶部用户卡显示），默认「备考人」 */
    val nickname: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_NICKNAME] ?: DEFAULT_NICKNAME
    }

    suspend fun setNickname(value: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NICKNAME] = value
        }
    }

    /** 本地头像文件路径；为 null 时使用默认头像（App 私有目录内，不依赖外部 URI 授权）。 */
    val avatarPath: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_AVATAR_PATH]
    }

    suspend fun setAvatarPath(value: String?) {
        context.dataStore.edit { preferences ->
            if (value == null) preferences.remove(KEY_AVATAR_PATH) else preferences[KEY_AVATAR_PATH] = value
        }
    }

    /**
     * 预留：本机用户标识。空串表示尚未接入账号系统的本地用户；
     * 未来接入云端同步时，登录后写入服务端下发的 userId 用于归属与增量同步。
     */
    val userId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_ID] ?: ""
    }

    suspend fun setUserId(value: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = value
        }
    }

    /**
     * 账号登录态：Authing 下发的 access token 镜像。
     * SDK 自身已以加密方式持久化 token（`Authing.getCurrentUser()` 会自动从磁盘恢复），
     * 这里额外落一份，用于快速判断登录态与未来云端同步。未登录时为 null。
     */
    val authToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTH_TOKEN]
    }

    suspend fun setAuthToken(value: String?) {
        context.dataStore.edit { preferences ->
            if (value == null) preferences.remove(KEY_AUTH_TOKEN) else preferences[KEY_AUTH_TOKEN] = value
        }
    }

    /** 账号用户 ID（Authing userId）；未登录时为 null。 */
    val authUserId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTH_USER_ID]
    }

    suspend fun setAuthUserId(value: String?) {
        context.dataStore.edit { preferences ->
            if (value == null) preferences.remove(KEY_AUTH_USER_ID) else preferences[KEY_AUTH_USER_ID] = value
        }
    }

    /**
     * 账号用户信息（[com.fenji.scoretrace.data.repository.AuthUser] 的 JSON 序列化）。
     * 作为 UI 登录态的唯一真源：登录写入、退出/注销清空。
     */
    val authUserInfo: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTH_USER_INFO]
    }

    suspend fun setAuthUserInfo(value: String?) {
        context.dataStore.edit { preferences ->
            if (value == null) preferences.remove(KEY_AUTH_USER_INFO) else preferences[KEY_AUTH_USER_INFO] = value
        }
    }

    /** 清空账号登录态（authToken / authUserId / authUserInfo）。 */
    suspend fun clearAuth() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_AUTH_TOKEN)
            preferences.remove(KEY_AUTH_USER_ID)
            preferences.remove(KEY_AUTH_USER_INFO)
        }
    }

    /** 今日 AI 重点文案（缓存） */
    val aiFocus: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_AI_FOCUS]
    }

    /** 今日 AI 重点的生成日期（yyyy-MM-dd） */
    val aiFocusDate: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_AI_FOCUS_DATE]
    }

    /** 生成该 AI 重点时的数据指纹（成绩 + 目标院校）；与当前指纹不一致即视为缓存失效。 */
    val aiFocusSignal: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_AI_FOCUS_SIGNAL]
    }

    suspend fun saveAiFocus(text: String, date: String, signal: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AI_FOCUS] = text
            preferences[KEY_AI_FOCUS_DATE] = date
            preferences[KEY_AI_FOCUS_SIGNAL] = signal
        }
    }

    /** 清空全部偏好（用于「清除全部数据」，各字段会回落到默认值）。账号登录态不在清除范围内。 */
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            val auth = snapshotAuth(preferences)
            preferences.clear()
            restoreAuth(preferences, auth)
        }
    }

    /**
     * 清空全部偏好，但保留定位结果（省份 + 已定位标记）。
     * 「清除全部数据」后不应丢失定位缓存，免得用户重新触发一轮定位。
     * 同样保留账号登录态——清除本地学习数据不应把用户踢下线。
     */
    suspend fun clearAllKeepLocation() {
        context.dataStore.edit { preferences ->
            val province = preferences[KEY_PROVINCE]
            val located = preferences[KEY_PROVINCE_LOCATED]
            val auth = snapshotAuth(preferences)
            preferences.clear()
            if (province != null) preferences[KEY_PROVINCE] = province
            if (located != null) preferences[KEY_PROVINCE_LOCATED] = located
            restoreAuth(preferences, auth)
        }
    }

    private fun snapshotAuth(preferences: Preferences): List<Pair<Preferences.Key<String>, String>> =
        listOfNotNull(
            preferences[KEY_AUTH_TOKEN]?.let { Pair(KEY_AUTH_TOKEN, it) },
            preferences[KEY_AUTH_USER_ID]?.let { Pair(KEY_AUTH_USER_ID, it) },
            preferences[KEY_AUTH_USER_INFO]?.let { Pair(KEY_AUTH_USER_INFO, it) },
        )

    private fun restoreAuth(
        preferences: MutablePreferences,
        auth: List<Pair<Preferences.Key<String>, String>>,
    ) {
        auth.forEach { (key, value) -> preferences[key] = value }
    }

    private companion object {
        val KEY_GAOKAO_TIMESTAMP = longPreferencesKey("gaokao_timestamp")
        val KEY_AUTO_PLAY_MUSIC = booleanPreferencesKey("auto_play_music")
        val KEY_ENABLE_BLUR = booleanPreferencesKey("enable_blur")
        val KEY_ENABLE_FLOATING_BOTTOM_BAR = booleanPreferencesKey("enable_floating_bottom_bar")
        val KEY_ENABLE_LIQUID_GLASS = booleanPreferencesKey("enable_liquid_glass")
        val KEY_ENABLE_PREDICTIVE_BACK = booleanPreferencesKey("enable_predictive_back")
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_SELECTED_SUBJECTS = stringPreferencesKey("selected_subjects")
        val KEY_PROVINCE = stringPreferencesKey("province")
        val KEY_PROVINCE_LOCATED = booleanPreferencesKey("province_located")
        val KEY_AI_FOCUS = stringPreferencesKey("ai_focus")
        val KEY_AI_FOCUS_DATE = stringPreferencesKey("ai_focus_date")
        val KEY_AI_FOCUS_SIGNAL = stringPreferencesKey("ai_focus_signal")
        val KEY_NICKNAME = stringPreferencesKey("nickname")
        val KEY_AVATAR_PATH = stringPreferencesKey("avatar_path")
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        val KEY_AUTH_USER_ID = stringPreferencesKey("auth_user_id")
        val KEY_AUTH_USER_INFO = stringPreferencesKey("auth_user_info")

        const val SUBJECT_SEPARATOR = ","
        const val DEFAULT_SELECTED_SUBJECTS = "物理,化学,生物"
        const val DEFAULT_NICKNAME = "备考人"

        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"

        /** 默认省份：项目面向云南考生，存量用户无该字段时回落到此值。 */
        const val DEFAULT_PROVINCE = "云南"
    }
}
