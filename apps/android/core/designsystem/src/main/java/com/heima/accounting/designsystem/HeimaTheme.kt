package com.heima.accounting.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class HeimaThemeStyle {
    CLEAR_BLUE,
    NATURE_HEALING,
}

enum class HeimaColorMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class VisualQuality {
    AUTO,
    REFINED,
    POWER_SAVER,
}

@Immutable
data class HeimaPalette(
    val background: Color,
    val backgroundSecondary: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val glassStroke: Color,
    val glassHighlight: Color,
    val brand: Color,
    val brandSoft: Color,
    val accent: Color,
    val income: Color,
    val expense: Color,
    val warning: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val ambientOne: Color,
    val ambientTwo: Color,
)

@Immutable
data class HeimaMotion(
    val reduceMotion: Boolean,
    val quality: VisualQuality,
) {
    val decorationsEnabled: Boolean
        get() = !reduceMotion && quality != VisualQuality.POWER_SAVER
}

private val LiquidLight = HeimaPalette(
    background = Color(0xFFF7F9FD),
    backgroundSecondary = Color(0xFFEEF3FB),
    surface = Color(0xFFFDFEFF),
    surfaceMuted = Color(0xFFF2F5FA),
    glassTop = Color(0xF2FFFFFF),
    glassBottom = Color(0xCDEAF1FF),
    glassStroke = Color(0xC9FFFFFF),
    glassHighlight = Color(0xA8FFFFFF),
    brand = Color(0xFF477FF5),
    brandSoft = Color(0xFFDCE8FF),
    accent = Color(0xFF72B9FF),
    income = Color(0xFF2BA77B),
    expense = Color(0xFF376EDB),
    warning = Color(0xFFE5A84B),
    textPrimary = Color(0xFF131A28),
    textSecondary = Color(0xFF5F6878),
    textTertiary = Color(0xFF9099A8),
    divider = Color(0x1A2C3850),
    ambientOne = Color(0xFFBFD7FF),
    ambientTwo = Color(0xFFD7F2FF),
)

private val LiquidDark = HeimaPalette(
    background = Color(0xFF111722),
    backgroundSecondary = Color(0xFF182130),
    surface = Color(0xFF1C2533),
    surfaceMuted = Color(0xFF222D3D),
    glassTop = Color(0xD9394658),
    glassBottom = Color(0xB5212C3C),
    glassStroke = Color(0x4DFFFFFF),
    glassHighlight = Color(0x5FFFFFFF),
    brand = Color(0xFF76A4FF),
    brandSoft = Color(0xFF263C68),
    accent = Color(0xFF81C8FF),
    income = Color(0xFF61D4A7),
    expense = Color(0xFF83A9FF),
    warning = Color(0xFFF2C46D),
    textPrimary = Color(0xFFF5F7FC),
    textSecondary = Color(0xFFBEC7D6),
    textTertiary = Color(0xFF8C98AA),
    divider = Color(0x24FFFFFF),
    ambientOne = Color(0xFF2E4F89),
    ambientTwo = Color(0xFF224A62),
)

private val NatureLight = HeimaPalette(
    background = Color(0xFFF7F5EC),
    backgroundSecondary = Color(0xFFEFF2E4),
    surface = Color(0xFFFFFEF7),
    surfaceMuted = Color(0xFFF1F0E6),
    glassTop = Color(0xF4FFFFFA),
    glassBottom = Color(0xD8E4EBD8),
    glassStroke = Color(0xD8FFFFFF),
    glassHighlight = Color(0xB8FFFFFF),
    brand = Color(0xFF5D8D70),
    brandSoft = Color(0xFFDCEBDD),
    accent = Color(0xFF89AF8C),
    income = Color(0xFF4A9171),
    expense = Color(0xFFB47B55),
    warning = Color(0xFFC8954D),
    textPrimary = Color(0xFF252A24),
    textSecondary = Color(0xFF666D62),
    textTertiary = Color(0xFF959B90),
    divider = Color(0x1F4B594A),
    ambientOne = Color(0xFFC9DEBA),
    ambientTwo = Color(0xFFD9E9D0),
)

private val NatureDark = HeimaPalette(
    background = Color(0xFF172019),
    backgroundSecondary = Color(0xFF1D2A21),
    surface = Color(0xFF233028),
    surfaceMuted = Color(0xFF29372E),
    glassTop = Color(0xD83A4A3F),
    glassBottom = Color(0xB526352C),
    glassStroke = Color(0x45FFFFFF),
    glassHighlight = Color(0x52FFFFFF),
    brand = Color(0xFF91C7A2),
    brandSoft = Color(0xFF294735),
    accent = Color(0xFFADD19F),
    income = Color(0xFF83D0A8),
    expense = Color(0xFFE1A47C),
    warning = Color(0xFFE3BC70),
    textPrimary = Color(0xFFF3F5EE),
    textSecondary = Color(0xFFC6CEC2),
    textTertiary = Color(0xFF949E92),
    divider = Color(0x24FFFFFF),
    ambientOne = Color(0xFF34583C),
    ambientTwo = Color(0xFF3D5740),
)

val LocalHeimaPalette = staticCompositionLocalOf { LiquidLight }
val LocalHeimaMotion = staticCompositionLocalOf {
    HeimaMotion(reduceMotion = false, quality = VisualQuality.AUTO)
}

object HeimaTheme {
    val palette: HeimaPalette
        @Composable get() = LocalHeimaPalette.current

    val motion: HeimaMotion
        @Composable get() = LocalHeimaMotion.current
}

private val HeimaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 50.sp,
        lineHeight = 56.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
)

private fun HeimaPalette.toMaterialScheme(isDark: Boolean): ColorScheme = if (isDark) {
    darkColorScheme(
        primary = brand,
        onPrimary = Color(0xFF0D1726),
        primaryContainer = brandSoft,
        secondary = accent,
        background = background,
        surface = surface,
        surfaceVariant = surfaceMuted,
        onBackground = textPrimary,
        onSurface = textPrimary,
        onSurfaceVariant = textSecondary,
        outline = divider,
        error = Color(0xFFFF8F87),
    )
} else {
    lightColorScheme(
        primary = brand,
        onPrimary = Color.White,
        primaryContainer = brandSoft,
        secondary = accent,
        background = background,
        surface = surface,
        surfaceVariant = surfaceMuted,
        onBackground = textPrimary,
        onSurface = textPrimary,
        onSurfaceVariant = textSecondary,
        outline = divider,
        error = Color(0xFFB3261E),
    )
}

@Composable
fun HeimaAccountingTheme(
    style: HeimaThemeStyle,
    darkTheme: Boolean = isSystemInDarkTheme(),
    quality: VisualQuality = VisualQuality.AUTO,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = when (style) {
        HeimaThemeStyle.CLEAR_BLUE -> if (darkTheme) LiquidDark else LiquidLight
        HeimaThemeStyle.NATURE_HEALING -> if (darkTheme) NatureDark else NatureLight
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalHeimaPalette provides palette,
        LocalHeimaMotion provides HeimaMotion(reduceMotion, quality),
    ) {
        MaterialTheme(
            colorScheme = palette.toMaterialScheme(darkTheme),
            typography = HeimaTypography,
            content = content,
        )
    }
}
