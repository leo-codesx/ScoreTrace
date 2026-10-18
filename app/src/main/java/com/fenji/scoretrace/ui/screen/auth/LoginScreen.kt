package com.fenji.scoretrace.ui.screen.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.AppToast

/**
 * 登录 / 注册页（自绘）。
 *
 * Authing 已开启「登录注册合并」，未注册的手机号/邮箱输入验证码后会自动注册，因此没有独立的注册页。
 * 手机号支持「验证码登录」与「密码登录」两种模式；邮箱只支持验证码（控制台仅开启邮箱+验证码）。
 *
 * 登录成功后的跳转由 `AppNavHost` 的登录守卫统一处理，本页不自作导航。
 */
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    // 未登录时登录页是唯一页，无上一页可返回，默认不显示返回箭头；
    // 从「账号设置」等二级入口进入时传 true 保留返回箭头。
    showBack: Boolean = false,
    onOpenPrivacy: () -> Unit = {},
    onOpenAgreement: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    var channelIndex by rememberSaveable { mutableIntStateOf(0) }   // 0 手机号 / 1 邮箱
    var phoneModeIndex by rememberSaveable { mutableIntStateOf(0) } // 0 验证码 / 1 密码

    var phone by rememberSaveable { mutableStateOf("") }
    var phoneCode by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var emailCode by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.toast.collect { AppToast.show(it) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = ScoreTraceColors.PageBackgroundLight) {
        Column(modifier = Modifier.fillMaxSize()) {
            AuthTopBar(title = "登录 / 注册", onBack = if (showBack) onBack else null)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = "ScoreTrace",
                        modifier = Modifier.size(58.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("欢迎使用 ScoreTrace", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextPrimaryLight)

                Spacer(modifier = Modifier.height(28.dp))

                if (!isOnline) {
                    OfflineNotice(onRetry = { AppToast.info("正在检测网络…") })
                    Spacer(modifier = Modifier.height(12.dp))
                }

                AuthSegmentedTabs(
                    options = listOf("手机号", "邮箱"),
                    selectedIndex = channelIndex,
                    onSelect = { channelIndex = it },
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (channelIndex == 0) {
                    AuthTextField(
                        value = phone,
                        onValueChange = { phone = it.filter(Char::isDigit).take(11) },
                        placeholder = "手机号",
                        keyboardType = KeyboardType.Phone,
                        imeAction = if (phoneModeIndex == 0) ImeAction.Next else ImeAction.Done,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (phoneModeIndex == 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AuthTextField(
                                value = phoneCode,
                                onValueChange = { phoneCode = it.filter(Char::isDigit).take(6) },
                                placeholder = "验证码",
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            AuthCodeButton(
                                countdown = state.phoneCountdown,
                                enabled = !state.isLoading && isOnline,
                                onClick = { viewModel.sendSmsCode(phone) },
                            )
                        }
                    } else {
                        AuthTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "密码（至少 8 位）",
                            isPassword = true,
                            imeAction = ImeAction.Done,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (phoneModeIndex == 0) "使用密码登录" else "使用验证码登录",
                        fontSize = 14.sp,
                        color = ScoreTraceColors.BrandPrimary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable { phoneModeIndex = 1 - phoneModeIndex },
                    )
                } else {
                    AuthTextField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        placeholder = "邮箱地址",
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AuthTextField(
                            value = emailCode,
                            onValueChange = { emailCode = it.filter(Char::isDigit).take(6) },
                            placeholder = "验证码",
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        AuthCodeButton(
                            countdown = state.emailCountdown,
                            enabled = !state.isLoading && isOnline,
                            onClick = { viewModel.sendEmailCode(email) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                AuthGradientButton(
                    text = "登录 / 注册",
                    // 按钮不禁用：不合法的输入由 AuthViewModel 校验并以 AppToast 告知原因，
                    // 否则在按钮上直接禁用会让用户不知道为什么点不动。
                    enabled = true,
                    loading = state.isLoading,
                    onClick = {
                        if (!isOnline) {
                            AppToast.warning("网络未连接，请连接网络后重试")
                        } else when {
                            channelIndex == 0 && phoneModeIndex == 0 -> viewModel.loginByPhone(phone, phoneCode)
                            channelIndex == 0 -> viewModel.loginByPassword(phone, password)
                            else -> viewModel.loginByEmail(email, emailCode)
                        }
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("登录即表示同意", fontSize = 12.sp, color = ScoreTraceColors.TextTertiaryLight)
                    Text(
                        text = "《用户协议》",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ScoreTraceColors.BrandPrimary,
                        modifier = Modifier.clickable(onClick = onOpenAgreement),
                    )
                    Text("和", fontSize = 12.sp, color = ScoreTraceColors.TextTertiaryLight)
                    Text(
                        text = "《隐私政策》",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ScoreTraceColors.BrandPrimary,
                        modifier = Modifier.clickable(onClick = onOpenPrivacy),
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/** 「获取验证码」按钮：发送成功后进入 [AuthViewModel] 驱动的倒计时，期间禁用。 */@Composable
private fun AuthCodeButton(
    countdown: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val counting = countdown > 0
    Box(
        modifier = Modifier
            .width(112.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled && !counting) {
                    ScoreTraceColors.BrandPrimary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .clickable(enabled = enabled && !counting, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (counting) "${countdown}s" else "获取验证码",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled && !counting) ScoreTraceColors.BrandPrimary else ScoreTraceColors.TextTertiaryLight,
        )
    }
}

/** 断网提示条：未连接网络时显示，提示联网后重试（网络恢复由 ConnectivityObserver 自动驱动）。 */
@Composable
private fun OfflineNotice(onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ScoreTraceColors.WarningOrange.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "网络未连接，请连接网络后重试",
            fontSize = 13.sp,
            color = ScoreTraceColors.WarningOrange,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "重试",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = ScoreTraceColors.BrandPrimary,
            modifier = Modifier.clickable(onClick = onRetry),
        )
    }
}
