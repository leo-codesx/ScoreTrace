package com.fenji.scoretrace.ui.screen.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fenji.scoretrace.R
import com.fenji.scoretrace.data.model.AuthUser
import com.fenji.scoretrace.ui.theme.ScoreTraceColors

/**
 * 「我的」页账号卡：未登录时是「登录 / 注册」入口，已登录时展示账号信息与设置入口。
 * 视觉沿用设置分组的白色圆角卡（水平 20dp 外边距、16dp 圆角）。
 */
@Composable
fun AccountCard(
    user: AuthUser?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountAvatar(user)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user?.nickname?.takeIf { it.isNotBlank() } ?: if (user == null) "登录 / 注册账号" else "已登录用户",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitleOf(user),
                fontSize = 13.sp,
                color = ScoreTraceColors.TextTertiaryLight,
            )
        }
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = if (user == null) null else "账号设置",
            tint = ScoreTraceColors.TextTertiaryLight,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun subtitleOf(user: AuthUser?): String {
    if (user == null) return "登录后可在多设备同步数据（即将支持）"
    return user.maskedPhone ?: user.maskedEmail ?: "已登录"
}

@Composable
private fun AccountAvatar(user: AuthUser?) {
    Box(
        modifier = Modifier
            .size(46.dp)
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
                modifier = Modifier.size(26.dp),
            )
        } else {
            AsyncImage(
                model = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_launcher_foreground),
                fallback = painterResource(R.drawable.ic_launcher_foreground),
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape),
            )
        }
    }
}
