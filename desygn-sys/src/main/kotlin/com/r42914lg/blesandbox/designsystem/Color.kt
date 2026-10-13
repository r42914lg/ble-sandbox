package com.r42914lg.blesandbox.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val TealAccent = Color(0xFF0B7A85)
val TealSoft = Color(0xFFD6EEF0)
val WarnAmber = Color(0xFF9A5B00)
val WarnAmberSoft = Color(0xFFFFF8E1)
val BadRed = Color(0xFFB3261E)
val BadRedSoft = Color(0xFFFADFDC)
val NeutralLine = Color(0xFFD3DBE2)

internal val LightColorScheme = lightColorScheme(
    primary = TealAccent,
    secondary = TealSoft,
    tertiary = WarnAmber,
    error = BadRed,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = TealAccent,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B)
)

internal val DarkColorScheme = darkColorScheme(
    primary = TealAccent,
    secondary = TealSoft,
    tertiary = WarnAmber,
    error = BadRed,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFF94A3B8)
)
