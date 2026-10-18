package com.fenji.scoretrace.ui.screen.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.repository.InstallResult
import com.fenji.scoretrace.data.repository.UpdateCheckResult
import com.fenji.scoretrace.data.repository.UpdateInfo
import com.fenji.scoretrace.data.repository.UpdateRepository
import com.fenji.scoretrace.util.AppToast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class UpdateUiState(
    val checking: Boolean = false,
    val available: UpdateInfo? = null,
    val downloading: Boolean = false,
    val progress: Int = 0,
    val downloadedFile: File? = null,
    val showDialog: Boolean = false,
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repository: UpdateRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    fun check() {
        if (_state.value.checking) return
        _state.update { it.copy(checking = true) }
        viewModelScope.launch {
            when (val result = repository.check()) {
                is UpdateCheckResult.Available -> _state.update {
                    it.copy(
                        checking = false,
                        available = result.info,
                        showDialog = true,
                        downloadedFile = null,
                        progress = 0,
                    )
                }

                UpdateCheckResult.UpToDate -> {
                    _state.update { it.copy(checking = false) }
                    AppToast.info("当前已是最新版本")
                }

                UpdateCheckResult.NotConfigured -> {
                    _state.update { it.copy(checking = false) }
                    AppToast.warning("更新服务未配置")
                }

                is UpdateCheckResult.Failed -> {
                    _state.update { it.copy(checking = false) }
                    AppToast.error("检查更新失败：${result.message}")
                }
            }
        }
    }

    fun download() {
        val info = _state.value.available ?: return
        if (_state.value.downloading) return
        _state.update { it.copy(downloading = true, progress = 0) }
        viewModelScope.launch {
            runCatching {
                repository.download(info) { percent ->
                    _state.update { it.copy(progress = percent) }
                }
            }.onSuccess { file ->
                _state.update { it.copy(downloading = false, downloadedFile = file, progress = 100) }
                AppToast.success("下载完成，点击安装")
            }.onFailure { error ->
                _state.update { it.copy(downloading = false, progress = 0) }
                AppToast.error("下载失败：${error.message ?: "网络异常"}")
            }
        }
    }

    fun install() {
        val file = _state.value.downloadedFile ?: return
        when (val result = repository.install(file)) {
            InstallResult.Started -> Unit
            InstallResult.NeedPermission -> {
                repository.openUnknownSourcesSettings()
                AppToast.info("请允许安装未知来源应用后重试")
            }

            is InstallResult.Failed -> AppToast.error(result.message)
        }
    }

    fun dismissDialog() {
        _state.update { it.copy(showDialog = false) }
    }
}
