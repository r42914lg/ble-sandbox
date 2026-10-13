package com.r42914lg.blesandbox.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.r42914lg.blesandbox.R
import com.r42914lg.blesandbox.mvi.MainContentState

@Composable
fun TopHeaderBar(contentState: MainContentState) {
    val defaultTitle = stringResource(R.string.app_title)
    val title = when (contentState) {
        is MainContentState.Connected -> contentState.deviceName
        is MainContentState.Connecting -> contentState.deviceName
        is MainContentState.Reconnecting -> contentState.deviceName
        is MainContentState.Error -> contentState.deviceAddress ?: defaultTitle
        MainContentState.NoDevice -> defaultTitle
    }

    val subtitle = when (contentState) {
        is MainContentState.Connected -> contentState.deviceAddress
        is MainContentState.Connecting -> contentState.deviceAddress
        is MainContentState.Reconnecting -> contentState.deviceAddress
        is MainContentState.Error -> stringResource(R.string.status_error)
        MainContentState.NoDevice -> stringResource(R.string.empty_state_title)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when (contentState) {
            is MainContentState.Connected -> StatusPill(stringResource(R.string.status_connected), TealAccent, TealSoft, isPulsing = false)
            is MainContentState.Connecting -> StatusPill(stringResource(R.string.status_connecting), Color.Gray, Color(0xFFEEEEEE), isPulsing = true)
            is MainContentState.Reconnecting -> StatusPill(stringResource(R.string.status_reconnecting), WarnAmber, WarnAmberSoft, isPulsing = true)
            is MainContentState.Error -> StatusPill(stringResource(R.string.status_error), BadRed, BadRedSoft, isPulsing = false)
            MainContentState.NoDevice -> {}
        }
    }
}

@Composable
fun StatusPill(text: String, textColor: Color, bgColor: Color, isPulsing: Boolean) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(99.dp),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isPulsing) {
                PulsingDot(color = textColor)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PulsingDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .alpha(alpha)
            .background(color, CircleShape)
    )
}
