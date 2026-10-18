package com.fenji.scoretrace.util

import com.dokar.sonner.ToastType
import com.dokar.sonner.ToasterDefaults
import com.dokar.sonner.ToasterState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** 轻提示的四种语义类型。 */
enum class AppToastType { Success, Error, Warning, Info }

/** 带类型的一次提示事件，供 ViewModel 经 Flow 上报，由页面转交 [AppToast]。 */
data class ToastMessage(val text: String, val type: AppToastType)

/**
 * 全局轻提示门面。底层由 Compose Sonner（[ToasterState]）渲染，
 * 对外仍保持 `AppToast.success(...)` 这类零上下文调用。
 *
 * Host 尚未挂载时（极早的启动阶段）先入队，[attach] 后按顺序补发。
 */
object AppToast {

    private var toaster: ToasterState? = null
    private val pending = ArrayDeque<Pair<String, AppToastType>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun show(message: CharSequence, type: AppToastType = AppToastType.Info) {
        val text = message.toString()
        scope.launch {
            val state = toaster
            if (state == null) {
                pending.addLast(text to type)
            } else {
                state.show(
                    message = text,
                    type = type.toSonnerType(),
                    duration = type.duration(),
                )
            }
        }
    }

    fun show(event: ToastMessage) = show(event.text, event.type)

    fun success(message: CharSequence) = show(message, AppToastType.Success)

    fun error(message: CharSequence) = show(message, AppToastType.Error)

    fun warning(message: CharSequence) = show(message, AppToastType.Warning)

    fun info(message: CharSequence) = show(message, AppToastType.Info)

    /** 由 `AppToastHost` 在组合时注入渲染器；组合销毁时解绑。 */
    internal fun attach(state: ToasterState) {
        toaster = state
        while (pending.isNotEmpty()) {
            val (text, type) = pending.removeFirst()
            state.show(message = text, type = type.toSonnerType(), duration = type.duration())
        }
    }

    internal fun detach(state: ToasterState) {
        if (toaster === state) toaster = null
    }
}

private fun AppToastType.toSonnerType(): ToastType = when (this) {
    AppToastType.Success -> ToastType.Success
    AppToastType.Error -> ToastType.Error
    AppToastType.Warning -> ToastType.Warning
    AppToastType.Info -> ToastType.Info
}

private fun AppToastType.duration(): Duration = when (this) {
    AppToastType.Error, AppToastType.Warning -> 3600.milliseconds
    AppToastType.Success, AppToastType.Info -> ToasterDefaults.DurationShort
}
