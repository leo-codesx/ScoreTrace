package com.fenji.scoretrace.ui.screen.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.fenji.scoretrace.R
import com.fenji.scoretrace.data.model.AuthUser
import com.fenji.scoretrace.ui.screen.auth.AuthTopBar
import com.fenji.scoretrace.ui.screen.auth.AuthViewModel
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.AppToast

/**
 * 已登录账号管理页：账号信息（只读）+ 退出登录 / 注销账号（均二次确认）。
 * 退出/注销后的跳转由 `AppNavHost` 的登录守卫统一处理（清栈回登录页），本页不自作导航。
 */
@Composable
fun AccountSettingsScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.toast.collect { AppToast.show(it) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = ScoreTraceColors.PageBackgroundLight) {
        Column(modifier = Modifier.fillMaxSize()) {
            AuthTopBar(title = "账号设置", onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                val user = state.user
                Avatar(user)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = AuthUser.clean(user?.nickname) ?: "已登录用户",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScoreTraceColors.TextPrimaryLight,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    InfoRow("手机号", user?.maskedPhone ?: "未绑定")
                    InfoDivider()
                    InfoRow("邮箱", user?.maskedEmail ?: "未绑定")
                    InfoDivider()
                    InfoRow("注册时间", user?.createdAt?.take(DATE_PREFIX_LENGTH) ?: "—")
                }

                Spacer(modifier = Modifier.height(24.dp))

                DangerButton(
                    text = "退出登录",
                    enabled = !state.isLoading,
                    onClick = { showLogoutDialog = true },
                )
                Spacer(modifier = Modifier.height(12.dp))
                DangerButton(
                    text = "注销账号",
                    enabled = !state.isLoading,
                    onClick = { showDeleteDialog = true },
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "注销账号将永久删除你的账号数据，且不可恢复。",
                    fontSize = 12.sp,
                    color = ScoreTraceColors.TextTertiaryLight,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录？") },
            text = { Text("退出后本机数据仍会保留，下次登录可继续使用。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.logout()
                        showLogoutDialog = false
                    },
                ) {
                    Text("退出登录", color = ScoreTraceColors.ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("取消") }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("注销账号？") },
            text = { Text("此操作不可恢复：账号及其云端数据将被永久删除，且无法找回。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount()
                        showDeleteDialog = false
                    },
                ) {
                    Text("确认注销", color = ScoreTraceColors.ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun Avatar(user: AuthUser?) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(ScoreTraceColors.BrandPrimary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        val photo = user?.photo
        if (photo.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = ScoreTraceColors.BrandPrimary,
                modifier = Modifier.size(44.dp),
            )
        } else {
            AsyncImage(
                model = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_launcher_foreground),
                fallback = painterResource(R.drawable.ic_launcher_foreground),
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = ScoreTraceColors.TextSecondaryLight,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            fontSize = 15.sp,
            color = ScoreTraceColors.TextPrimaryLight,
        )
    }
}

@Composable
private fun InfoDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(start = 16.dp)
            .background(ScoreTraceColors.CardBorderLight),
    )
}

@Composable
private fun DangerButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(ScoreTraceColors.ErrorRed.copy(alpha = if (enabled) 0.10f else 0.04f))
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = ScoreTraceColors.ErrorRed.copy(alpha = if (enabled) 1f else 0.4f),
        )
    }
}

private const val DATE_PREFIX_LENGTH = 10
