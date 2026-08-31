package com.kaushik.railway.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF0B2545)
val NavyMid = Color(0xFF13315C)
val Orange = Color(0xFFE85D04)
val OrangeDeep = Color(0xFFDC2F02)
val Cream = Color(0xFFFFF7F0)
val CardWhite = Color(0xFFFFFFFF)
val Success = Color(0xFF2D6A4F)
val Waitlist = Color(0xFF9B2226)
val Rac = Color(0xFFBC6C25)

private val scheme = lightColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    secondary = Navy,
    onSecondary = Color.White,
    background = Cream,
    onBackground = Navy,
    surface = CardWhite,
    onSurface = Navy,
    error = Waitlist
)

@Composable
fun RailwayTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
