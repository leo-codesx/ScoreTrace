package com.fenji.scoretrace.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.repository.LocalDataMigrationRepository
import com.fenji.scoretrace.data.repository.UnclaimedLocalData
import com.fenji.scoretrace.util.AppToast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 首次登录后的本地数据迁移：检测到未绑定账号的本机数据时弹窗询问是否绑定。
 * 每个进程只检测一次（[checked]），避免反复打扰。
 */
@HiltViewModel
class DataMigrationViewModel @Inject constructor(
    private val migrationRepository: LocalDataMigrationRepository,
) : ViewModel() {

    data class UiState(
        val visible: Boolean = false,
        val data: UnclaimedLocalData = UnclaimedLocalData(),
        val claiming: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var checked = false

    /** 登录态变为已登录时调用；仅首次执行检测。 */
    fun checkOnce() {
        if (checked) return
        checked = true
        viewModelScope.launch {
            val data = migrationRepository.countUnclaimed()
            if (data.total > 0) _state.value = UiState(visible = true, data = data)
        }
    }

    fun migrate() {
        viewModelScope.launch {
            _state.value = _state.value.copy(claiming = true)
            migrationRepository.claimToCurrentUser()
            _state.value = _state.value.copy(visible = false, claiming = false)
            AppToast.success("本地数据已绑定到当前账号")
        }
    }

    fun dismiss() {
        _state.value = _state.value.copy(visible = false)
    }
}
