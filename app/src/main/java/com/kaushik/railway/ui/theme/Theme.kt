package com.kaushik.railway.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** CRIS RailOne-inspired palette */
val Navy = Color(0xFF0A3D91)
val NavyMid = Color(0xFF1565C0)
val NavyDark = Color(0xFF062A66)
val Orange = Color(0xFFF57C00)
val OrangeDeep = Color(0xFFE65100)
val Cream = Color(0xFFEEF2F7)
val CardWhite = Color(0xFFFFFFFF)
val Success = Color(0xFF2E7D32)
val Waitlist = Color(0xFFC62828)
val Rac = Color(0xFFF9A825)
val TricolorSaffron = Color(0xFFFF9933)
val TricolorGreen = Color(0xFF138808)
val Mute = Color(0xFF5B6B7C)

private val scheme = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = Orange,
    onSecondary = Color.White,
    background = Cream,
    onBackground = NavyDark,
    surface = CardWhite,
    onSurface = NavyDark,
    error = Waitlist
)

@Composable
fun RailwayTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = Cream, content = content)
    }
}
