package com.kaushik.railway.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** CRIS RailOne palette + iOS liquid-glass surfaces */
val Navy = Color(0xFF0A3D91)
val NavyMid = Color(0xFF1565C0)
val NavyDark = Color(0xFF062A66)
val Orange = Color(0xFFF57C00)
val OrangeDeep = Color(0xFFE65100)
val Cream = Color(0xFFE8EEF8)
val CardWhite = Color(0xCCFFFFFF)
val Success = Color(0xFF2E7D32)
val Waitlist = Color(0xFFC62828)
val Rac = Color(0xFFF9A825)
val TricolorSaffron = Color(0xFFFF9933)
val TricolorGreen = Color(0xFF138808)
val Mute = Color(0xFF5B6B7C)
val GlassFill = Color(0x99FFFFFF)
val GlassStroke = Color(0x73FFFFFF)
val GlassHighlight = Color(0xB3FFFFFF)

private val scheme = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = Orange,
    onSecondary = Color.White,
    background = Cream,
    onBackground = NavyDark,
    surface = Color.White.copy(alpha = 0.72f),
    onSurface = NavyDark,
    error = Waitlist
)

@Composable
fun RailwayTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun LiquidGlassBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFD7E6FF), Color(0xFFF4E8D8), Color(0xFFE8EEF8))
                )
            )
    ) {
        val blob = if (Build.VERSION.SDK_INT >= 31) Modifier.blur(70.dp) else Modifier
        Box(
            Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .then(blob)
                .background(Color(0xFF7EB6FF).copy(alpha = 0.55f), CircleShape)
        )
        Box(
            Modifier
                .size(240.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-80).dp, y = 40.dp)
                .then(blob)
                .background(Color(0xFFFFC38A).copy(alpha = 0.45f), CircleShape)
        )
        Box(
            Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 20.dp)
                .then(blob)
                .background(Color(0xFF9BE7C4).copy(alpha = 0.35f), CircleShape)
        )
        content()
    }
}
