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
    ambientOne = Color(0xFFBFD7FF),
    ambientTwo = Color(0xFFD7F2FF),
    chartColors = listOf(Color(0xFF5A86F7), Color(0xFF2BB39A), Color(0xFFFFA85C), Color(0xFFA46AF1), Color(0xFFEF6D8E), Color(0xFF7F8DA6)),
)

private val LiquidDark = AppThemeTokens(
    background = Color(0xFF0C121C),
    backgroundSecondary = Color(0xFF111B29),
    surface = Color(0xFF151E2B),
    surfaceElevated = Color(0xFF1B2635),
    surfaceVariant = Color(0xFF222F40),
    outline = Color(0xFF445269),
    glassBase = Color(0xE61B2737),
    glassTint = Color(0xD9233144),
    glassHighlight = Color(0x24D8E8FF),
    glassOutline = Color(0x33BFD5F4),
    glassShadow = Color(0x8A05080E),
    brand = Color(0xFF76A4FF),
    brandSoft = Color(0xFF263C68),
    accent = Color(0xFF81C8FF),
    positiveColor = Color(0xFF61D4A7),
    negativeColor = Color(0xFF83A9FF),
    warningColor = Color(0xFFF2C46D),
    textPrimary = Color(0xFFF5F7FC),
    textSecondary = Color(0xFFBEC7D6),
    textMuted = Color(0xFF8C98AA),
    divider = Color(0x24FFFFFF),
    ambientOne = Color(0xFF1C3760),
    ambientTwo = Color(0xFF173646),
    chartColors = listOf(Color(0xFF7EA7FF), Color(0xFF51D2B4), Color(0xFFFFBB78), Color(0xFFBE91FF), Color(0xFFFF8EAA), Color(0xFFA7B1C5)),
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
    ambientOne = Color(0xFFC9DEBA),
    ambientTwo = Color(0xFFD9E9D0),
    chartColors = listOf(Color(0xFF5E9673), Color(0xFF7CA8A0), Color(0xFFD69A67), Color(0xFF9C7FBE), Color(0xFFC97E87), Color(0xFF8A9586)),
)

private val NatureDark = AppThemeTokens(
    background = Color(0xFF101712),
    backgroundSecondary = Color(0xFF162119),
    surface = Color(0xFF1A251E),
    surfaceElevated = Color(0xFF202D25),
    surfaceVariant = Color(0xFF28372D),
    outline = Color(0xFF465A4C),
    glassBase = Color(0xE3213027),
    glassTint = Color(0xD1283A2F),
    glassHighlight = Color(0x20DFF5E5),
    glassOutline = Color(0x31C9E0CF),
    glassShadow = Color(0x8A050A06),
    brand = Color(0xFF91C7A2),
    brandSoft = Color(0xFF294735),
    accent = Color(0xFFADD19F),
    positiveColor = Color(0xFF83D0A8),
    negativeColor = Color(0xFFE1A47C),
    warningColor = Color(0xFFE3BC70),
    textPrimary = Color(0xFFF3F5EE),
    textSecondary = Color(0xFFC6CEC2),
    textMuted = Color(0xFF949E92),
    divider = Color(0x24FFFFFF),
    ambientOne = Color(0xFF27452F),
    ambientTwo = Color(0xFF2C4432),
    chartColors = listOf(Color(0xFF8AC8A0), Color(0xFF91C7C0), Color(0xFFE8AD7C), Color(0xFFC29DDF), Color(0xFFE39AA2), Color(0xFFAEBBAA)),
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
