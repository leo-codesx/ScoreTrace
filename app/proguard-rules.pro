# Retrofit + Gson 模型类需保留泛型与字段名
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit 接口方法上的注解需要保留
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# 保留 dto 包下的数据模型（Gson 反射依赖字段名）
# 注意：DeepSeek/GLM 的 DTO 在 data.remote.deepseek.dto / data.remote.glm.dto 下，
# 因此整体保留 data.remote 及其所有子包的类与字段名。
-keep class com.fenji.scoretrace.data.remote.** { *; }

# asset JSON（schools.json / majors_v2.json）的反序列化模型（Gson 反射依赖字段名）。
# 这些类没有 @SerializedName，一旦被 R8 混淆字段名，Gson 就填不进值，
# 会导致 TargetSchoolScreen 读 school.level/schools 时 NPE 闪退（release 专属）。
-keep class com.fenji.scoretrace.data.model.** { *; }

# Gson 反射模型：一分一段表与音乐歌单缓存（字段名必须保留）
-keep class com.fenji.scoretrace.data.repository.ScoreSectionData { *; }
-keep class com.fenji.scoretrace.data.repository.ScoreSectionTable { *; }
-keep class com.fenji.scoretrace.data.repository.ScoreSectionEntry { *; }
-keep class com.fenji.scoretrace.data.player.TrackInfo { *; }

# OkHttp 平台相关可选项缺失时的警告忽略
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Authing Guard SDK 内置了大量「可选」社交登录 / 推送 handler（微信、QQ、微博、
# Facebook、Google、华为、小米、快手、LINE、支付宝等），但本应用只用了账号密码/验证码，
# 这些第三方 SDK 均未作为依赖引入。R8 全模式下「Missing classes」会直接判定构建失败
# （并非仅告警），故对这批未接入的可选 SDK 包统一 -dontwarn；它们只要不被实际调用即无影响。
-dontwarn android.hardware.fingerprint.**
-dontwarn cn.authing.webauthn.**
-dontwarn com.alipay.**
-dontwarn com.amazon.**
-dontwarn com.android.dingtalk.**
-dontwarn com.baidu.**
-dontwarn com.bytedance.**
-dontwarn com.facebook.**
-dontwarn com.google.android.gms.**
-dontwarn com.heytap.**
-dontwarn com.huawei.**
-dontwarn com.igexin.**
-dontwarn com.kwai.**
-dontwarn com.linecorp.**
-dontwarn com.netease.**
-dontwarn com.sina.**
-dontwarn com.ss.android.**
-dontwarn com.tencent.**
-dontwarn com.twitter.**
-dontwarn com.xiaomi.**
