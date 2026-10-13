package com.r42914lg.blesandbox.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Spacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 20.dp,
    val huge: Dp = 24.dp
)

internal val LocalSpacing = staticCompositionLocalOf { Spacing() }

val AppSpacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
