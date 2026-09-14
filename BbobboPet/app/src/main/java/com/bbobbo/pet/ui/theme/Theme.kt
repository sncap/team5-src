package com.bbobbo.pet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---- 디자인 토큰 ----
object Palette {
    val Primary = Color(0xFFF2A0AE)
    val PrimaryDeep = Color(0xFFE47C8E)
    val SubPink = Color(0xFFFBD9DF)
    val CreamBg = Color(0xFFFDF4E6)
    val CardBeige = Color(0xFFF6E7D2)
    val CardWhite = Color(0xFFFFFBF4)
    val TextBrown = Color(0xFF6B4A3A)
    val SubText = Color(0xFFA88A76)
    val Yellow = Color(0xFFF7C873)
    val Mint = Color(0xFFA8D8B9)
    val SkyBlue = Color(0xFFA9CBE8)
    val Lavender = Color(0xFFC9B6E4)

    // 캐릭터
    val Fur = Color(0xFFE8D3B5)
    val FurShade = Color(0xFFD9BE99)
    val Muzzle = Color(0xFF8B5E3C)
    val Blush = Color(0xFFF5A9AE)
    val ScarfRed = Color(0xFFB5654A)
}

private val LightScheme = lightColorScheme(
    primary = Palette.Primary,
    onPrimary = Color.White,
    secondary = Palette.Yellow,
    background = Palette.CreamBg,
    onBackground = Palette.TextBrown,
    surface = Palette.CardWhite,
    onSurface = Palette.TextBrown,
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private fun t(size: Int, weight: FontWeight, ls: Double = 0.0) =
    TextStyle(fontSize = size.sp, fontWeight = weight, letterSpacing = ls.sp)

val AppTypography = Typography(
    displaySmall = t(30, FontWeight.ExtraBold, (-0.5)),
    headlineMedium = t(24, FontWeight.ExtraBold, (-0.3)),
    titleLarge = t(20, FontWeight.Bold),
    titleMedium = t(17, FontWeight.Bold),
    bodyLarge = t(15, FontWeight.Medium),
    bodyMedium = t(13, FontWeight.Medium),
    labelLarge = t(14, FontWeight.Bold),
    labelSmall = t(11, FontWeight.SemiBold),
)

@Composable
fun BbobboTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightScheme,  // 파스텔 톤 유지를 위해 다크 모드 미적용
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
