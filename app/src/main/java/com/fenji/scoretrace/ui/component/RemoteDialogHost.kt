package com.fenji.scoretrace.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.fenji.scoretrace.data.remote.config.RemoteDialogConfig
import com.fenji.scoretrace.data.repository.RemoteConfigRepository
import com.fenji.scoretrace.ui.theme.Dimensions
import com.fenji.scoretrace.util.AppToast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemoteDialogViewModel @Inject constructor(
    private val repository: RemoteConfigRepository,
) : ViewModel() {

    private val _config = MutableStateFlow<RemoteDialogConfig?>(null)
    val config: StateFlow<RemoteDialogConfig?> = _config.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val config = repository.fetchActive() ?: return@launch
            if (repository.shouldShow(config)) {
                _config.value = config
                repository.markShown(config)
            }
        }
    }

    fun dismiss() {
        _config.value = null
    }
}

/**
 * 远程公告 / 活动弹窗宿主。挂在根内容层，启动海报结束后再拉取，避免被启动页遮挡。
 */
@Composable
fun RemoteDialogHost(
    viewModel: RemoteDialogViewModel = hiltViewModel(),
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        delay(2600)
        viewModel.load()
    }

    val current = config ?: return
    val dismissButton: (@Composable () -> Unit)? = if (current.buttonUrl != null) {
        { TextButton(onClick = viewModel::dismiss) { Text("关闭") } }
    } else {
        null
    }
    AlertDialog(
        onDismissRequest = viewModel::dismiss,
        title = { Text(current.title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                current.imageUrl?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .clip(RoundedCornerShape(Dimensions.CornerMedium))
                            .padding(bottom = Dimensions.SpacingSmall),
                    )
                }
                Text(current.content)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    current.buttonUrl?.let { url ->
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }.onFailure { AppToast.warning("链接无法打开") }
                    }
                    viewModel.dismiss()
                },
            ) {
                Text(current.buttonText ?: "知道了")
            }
        },
        dismissButton = dismissButton,
    )
}
