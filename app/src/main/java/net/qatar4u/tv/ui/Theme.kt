package net.qatar4u.tv.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import net.qatar4u.tv.R
import net.qatar4u.tv.data.Accent

object Q4u {
    val Background = Color(0xFF080B12)
    val Surface = Color(0xFF172033)
    val SurfaceHigh = Color(0xFF22304A)
    val Gold = Color(0xFFD9B76E)
    val GoldLight = Color(0xFFFFE4A3)
    val Maroon = Color(0xFF8A1538)
    val Pearl = Color(0xFFE8E4DA)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFAAB4C3)
    val Danger = Color(0xFFFF7A7A)
}

fun Accent.color(): Color = when (this) {
    Accent.GOLD -> Q4u.Gold
    Accent.MAROON -> Color(0xFFC23A62)
    Accent.PEARL -> Q4u.Pearl
}

val LocalAccent = staticCompositionLocalOf { Q4u.Gold }

val Cairo = FontFamily(
    Font(R.font.cairo_regular, FontWeight(400)),
    Font(R.font.cairo_semibold, FontWeight(600)),
    Font(R.font.cairo_bold, FontWeight(700)),
    Font(R.font.cairo_extrabold, FontWeight(800)),
)

@Composable
fun Q4uTheme(accent: Color, content: @Composable () -> Unit) {
    val base = MaterialTheme.typography
    val t = base.copy(
        displayLarge = TextStyle(fontFamily = Cairo, fontWeight = FontWeight(700), fontSize = 88.sp, lineHeight = 96.sp),
        headlineMedium = TextStyle(fontFamily = Cairo, fontWeight = FontWeight(700), fontSize = 30.sp),
        titleLarge = TextStyle(fontFamily = Cairo, fontWeight = FontWeight(700), fontSize = 24.sp),
        titleMedium = TextStyle(fontFamily = Cairo, fontWeight = FontWeight(600), fontSize = 20.sp),
        bodyLarge = TextStyle(fontFamily = Cairo, fontWeight = FontWeight(400), fontSize = 19.sp),
        bodyMedium = TextStyle(fontFamily = Cairo, fontWeight = FontWeight(400), fontSize = 16.sp),
        labelLarge = TextStyle(fontFamily = Cairo, fontWeight = FontWeight(600), fontSize = 17.sp),
    )
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            onPrimary = Q4u.Background,
            background = Q4u.Background,
            surface = Q4u.Surface,
            onSurface = Q4u.TextPrimary,
            onBackground = Q4u.TextPrimary,
            secondary = Q4u.Maroon,
        ),
        typography = t,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalAccent provides accent, content = content)
    }
}
