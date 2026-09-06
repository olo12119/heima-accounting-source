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
data class AppThemeTokens(
    val background: Color,
    val backgroundSecondary: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceVariant: Color,
    val outline: Color,
    val glassBase: Color,
    val glassTint: Color,
    val glassHighlight: Color,
    val glassOutline: Color,
    val glassShadow: Color,
    val brand: Color,
    val brandSoft: Color,
    val accent: Color,
    val positiveColor: Color,
    val negativeColor: Color,
    val warningColor: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val divider: Color,
    val ambientOne: Color,
    val ambientTwo: Color,
    val chartColors: List<Color>,
) {
    // Compatibility names keep feature code readable while all values originate
    // from the independent light/dark token set above.
    val surfaceMuted: Color get() = surfaceVariant
    val glassTop: Color get() = glassBase
    val glassBottom: Color get() = glassTint
    val glassStroke: Color get() = glassOutline
    val income: Color get() = positiveColor
    val expense: Color get() = negativeColor
    val warning: Color get() = warningColor
    val textTertiary: Color get() = textMuted
}

typealias HeimaPalette = AppThemeTokens

@Immutable
data class HeimaMotion(
    val reduceMotion: Boolean,
    val quality: VisualQuality,
    val liquidGlassEnabled: Boolean,
    val darkTheme: Boolean,
) {
    val decorationsEnabled: Boolean
        get() = liquidGlassEnabled && quality != VisualQuality.POWER_SAVER

    val expensiveGlassEnabled: Boolean
        get() = liquidGlassEnabled && quality != VisualQuality.POWER_SAVER
}

private val LiquidLight = AppThemeTokens(
    background = Color(0xFFF7F9FD),
    backgroundSecondary = Color(0xFFEEF3FB),
    surface = Color(0xFFFDFEFF),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2F5FA),
    outline = Color(0xFFCBD4E2),
    glassBase = Color(0xF2FFFFFF),
    glassTint = Color(0xCDEAF1FF),
    glassHighlight = Color(0xA8FFFFFF),
    glassOutline = Color(0xC9FFFFFF),
    glassShadow = Color(0x24274768),
    brand = Color(0xFF477FF5),
    brandSoft = Color(0xFFDCE8FF),
    accent = Color(0xFF72B9FF),
    positiveColor = Color(0xFF2BA77B),
    negativeColor = Color(0xFF376EDB),
    warningColor = Color(0xFFE5A84B),
    textPrimary = Color(0xFF131A28),
    textSecondary = Color(0xFF5F6878),
    textMuted = Color(0xFF9099A8),
    divider = Color(0x1A2C3850),
    ambientOne = Color(0xFF9FC4FF),
    ambientTwo = Color(0xFFB7E6FF),
    chartColors = listOf(Color(0xFF5A86F7), Color(0xFF2BB39A), Color(0xFFFFA85C), Color(0xFFA46AF1), Color(0xFFEF6D8E), Color(0xFF7F8DA6)),
)

// 深色三层明度：background（最暗）< surface（卡片）< surfaceElevated（浮层），层级用"更亮"表达。
private val LiquidDark = AppThemeTokens(
    background = Color(0xFF0B101A),
    backgroundSecondary = Color(0xFF101826),
    surface = Color(0xFF182334),
    surfaceElevated = Color(0xFF223148),
    surfaceVariant = Color(0xFF2A3A52),
    outline = Color(0xFF4A5A74),
    glassBase = Color(0xE6202C40),
    glassTint = Color(0xD9283850),
    glassHighlight = Color(0x2EDCEAFF),
    glassOutline = Color(0x5ACFE5FF),
    glassShadow = Color(0x9903070C),
    brand = Color(0xFF8AB2FF),
    brandSoft = Color(0xFF2A4270),
    accent = Color(0xFF8FD0FF),
    positiveColor = Color(0xFF6CDDB2),
    negativeColor = Color(0xFF8FB2FF),
    warningColor = Color(0xFFF5CC7A),
    textPrimary = Color(0xFFF2F5FA),
    textSecondary = Color(0xFFC3CCD9),
    textMuted = Color(0xFF97A2B4),
    divider = Color(0x29FFFFFF),
    ambientOne = Color(0xFF234A80),
    ambientTwo = Color(0xFF1D4A5E),
    chartColors = listOf(Color(0xFF8AB2FF), Color(0xFF5CD8BC), Color(0xFFFFC284), Color(0xFFC69AFF), Color(0xFFFF97B2), Color(0xFFB0BAC9)),
)

private val NatureLight = AppThemeTokens(
    background = Color(0xFFF7F5EC),
    backgroundSecondary = Color(0xFFEFF2E4),
    surface = Color(0xFFFFFEF7),
    surfaceElevated = Color(0xFFFFFFFB),
    surfaceVariant = Color(0xFFF1F0E6),
    outline = Color(0xFFCAD0C2),
    glassBase = Color(0xF4FFFFFA),
    glassTint = Color(0xD8E4EBD8),
    glassHighlight = Color(0xB8FFFFFF),
    glassOutline = Color(0xD8FFFFFF),
    glassShadow = Color(0x24384535),
    brand = Color(0xFF5D8D70),
    brandSoft = Color(0xFFDCEBDD),
    accent = Color(0xFF89AF8C),
    positiveColor = Color(0xFF4A9171),
    negativeColor = Color(0xFFB47B55),
    warningColor = Color(0xFFC8954D),
    textPrimary = Color(0xFF252A24),
    textSecondary = Color(0xFF666D62),
    textMuted = Color(0xFF959B90),
    divider = Color(0x1F4B594A),
    ambientOne = Color(0xFFA9D69A),
    ambientTwo = Color(0xFFBDE8B0),
    chartColors = listOf(Color(0xFF5E9673), Color(0xFF7CA8A0), Color(0xFFD69A67), Color(0xFF9C7FBE), Color(0xFFC97E87), Color(0xFF8A9586)),
)

private val NatureDark = AppThemeTokens(
    background = Color(0xFF0D130E),
    backgroundSecondary = Color(0xFF131C15),
    surface = Color(0xFF1C2820),
    surfaceElevated = Color(0xFF26352C),
    surfaceVariant = Color(0xFF2E4034),
    outline = Color(0xFF4E6355),
    glassBase = Color(0xE324332A),
    glassTint = Color(0xD92C4033),
    glassHighlight = Color(0x28E2F7E9),
    glassOutline = Color(0x5ACFE6D6),
    glassShadow = Color(0x99040805),
    brand = Color(0xFFA0D5B1),
    brandSoft = Color(0xFF2D5039),
    accent = Color(0xFFB8DBAB),
    positiveColor = Color(0xFF8DD7B2),
    negativeColor = Color(0xFFE8AE84),
    warningColor = Color(0xFFE8C47C),
    textPrimary = Color(0xFFF1F4EC),
    textSecondary = Color(0xFFCBD3C6),
    textMuted = Color(0xFF9DA896),
    divider = Color(0x29FFFFFF),
    ambientOne = Color(0xFF30623E),
    ambientTwo = Color(0xFF3A6044),
    chartColors = listOf(Color(0xFF95D0AA), Color(0xFF9ACEC6), Color(0xFFEDB586), Color(0xFFC8A5E4), Color(0xFFE7A2AA), Color(0xFFB5C2B1)),
)

val LocalHeimaPalette = staticCompositionLocalOf { LiquidLight }
val LocalHeimaMotion = staticCompositionLocalOf {
    HeimaMotion(
        reduceMotion = false,
        quality = VisualQuality.AUTO,
        liquidGlassEnabled = true,
        darkTheme = false,
    )
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

private fun AppThemeTokens.toMaterialScheme(isDark: Boolean): ColorScheme = if (isDark) {
    darkColorScheme(
        primary = brand,
        onPrimary = Color(0xFF0D1726),
        primaryContainer = brandSoft,
        secondary = accent,
        background = background,
        surface = surface,
        surfaceContainer = surfaceElevated,
        surfaceVariant = surfaceVariant,
        onBackground = textPrimary,
        onSurface = textPrimary,
        onSurfaceVariant = textSecondary,
        outline = outline,
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
        surfaceContainer = surfaceElevated,
        surfaceVariant = surfaceVariant,
        onBackground = textPrimary,
        onSurface = textPrimary,
        onSurfaceVariant = textSecondary,
        outline = outline,
        error = Color(0xFFB3261E),
    )
}

@Composable
fun HeimaAccountingTheme(
    style: HeimaThemeStyle,
    darkTheme: Boolean = isSystemInDarkTheme(),
    quality: VisualQuality = VisualQuality.AUTO,
    reduceMotion: Boolean = false,
    liquidGlassEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val palette = when (style) {
        HeimaThemeStyle.CLEAR_BLUE -> if (darkTheme) LiquidDark else LiquidLight
        HeimaThemeStyle.NATURE_HEALING -> if (darkTheme) NatureDark else NatureLight
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalHeimaPalette provides palette,
        LocalHeimaMotion provides HeimaMotion(reduceMotion, quality, liquidGlassEnabled, darkTheme),
    ) {
        MaterialTheme(
            colorScheme = palette.toMaterialScheme(darkTheme),
            typography = HeimaTypography,
            content = content,
        )
    }
}
