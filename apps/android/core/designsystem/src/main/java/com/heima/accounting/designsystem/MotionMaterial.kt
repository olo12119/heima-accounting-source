package com.heima.accounting.designsystem

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class HeimaSurfaceRole {
    HERO,
    METRIC,
    INSIGHT,
    CHART,
    LIST,
    INTERACTIVE,
    OVERLAY,
}

enum class GlassQuality {
    HIGH,
    BALANCED,
    PERFORMANCE,
    DISABLED,
}

enum class HeimaShadowLevel {
    NONE,
    SOFT,
    FLOAT,
    MODAL,
}

@Immutable
data class HeimaMaterialSpec(
    val backdropBlur: Boolean,
    val blurRadius: Dp,
    val refractionHeight: Dp,
    val refractionAmount: Dp,
    val surfaceAlpha: Float,
    val highlightAlpha: Float,
    val innerShadowAlpha: Float,
    val shadowLevel: HeimaShadowLevel,
)

object HeimaMaterialSystem {
    @Composable
    fun quality(): GlassQuality {
        val motion = HeimaTheme.motion
        return resolveGlassQuality(motion.liquidGlassEnabled, motion.reduceMotion, motion.quality)
    }

    @Composable
    fun spec(role: HeimaSurfaceRole, requestBackdrop: Boolean): HeimaMaterialSpec {
        val quality = quality()
        val dark = HeimaTheme.motion.darkTheme
        val allowBackdrop = requestBackdrop && (
            quality == GlassQuality.HIGH || quality == GlassQuality.BALANCED
            )
        val high = quality == GlassQuality.HIGH
        return when (role) {
            HeimaSurfaceRole.HERO -> HeimaMaterialSpec(
                backdropBlur = allowBackdrop,
                blurRadius = if (high) 12.dp else 8.dp,
                refractionHeight = if (high) 4.dp else 2.dp,
                refractionAmount = if (high) 5.dp else 3.dp,
                surfaceAlpha = if (allowBackdrop) .28f else .86f,
                highlightAlpha = if (dark) .10f else .34f,
                innerShadowAlpha = if (dark) .07f else .17f,
                shadowLevel = HeimaShadowLevel.FLOAT,
            )
            HeimaSurfaceRole.INSIGHT -> HeimaMaterialSpec(
                backdropBlur = allowBackdrop,
                blurRadius = if (high) 10.dp else 7.dp,
                refractionHeight = if (high) 3.dp else 1.5.dp,
                refractionAmount = if (high) 4.dp else 2.dp,
                surfaceAlpha = if (allowBackdrop) .32f else .88f,
                highlightAlpha = if (dark) .09f else .29f,
                innerShadowAlpha = if (dark) .06f else .14f,
                shadowLevel = HeimaShadowLevel.SOFT,
            )
            HeimaSurfaceRole.OVERLAY -> HeimaMaterialSpec(
                backdropBlur = allowBackdrop,
                blurRadius = if (high) 14.dp else 9.dp,
                refractionHeight = if (high) 3.dp else 1.dp,
                refractionAmount = if (high) 4.dp else 2.dp,
                surfaceAlpha = if (allowBackdrop) .52f else .96f,
                highlightAlpha = if (dark) .08f else .26f,
                innerShadowAlpha = if (dark) .07f else .16f,
                shadowLevel = HeimaShadowLevel.MODAL,
            )
            HeimaSurfaceRole.METRIC,
            HeimaSurfaceRole.CHART,
            HeimaSurfaceRole.LIST,
            HeimaSurfaceRole.INTERACTIVE -> {
                val alpha = when (role) {
                    HeimaSurfaceRole.CHART -> .91f
                    HeimaSurfaceRole.LIST -> .88f
                    HeimaSurfaceRole.INTERACTIVE -> .86f
                    else -> .89f
                }
                HeimaMaterialSpec(
                    backdropBlur = allowBackdrop && role == HeimaSurfaceRole.INTERACTIVE,
                    blurRadius = if (high) 7.dp else 5.dp,
                    refractionHeight = if (high) 2.dp else 1.dp,
                    refractionAmount = if (high) 3.dp else 1.5.dp,
                    surfaceAlpha = if (allowBackdrop) .36f else alpha,
                    highlightAlpha = if (dark) .07f else .22f,
                    innerShadowAlpha = if (dark) .05f else .10f,
                    shadowLevel = if (role == HeimaSurfaceRole.LIST) HeimaShadowLevel.NONE else HeimaShadowLevel.SOFT,
                )
            }
        }
    }
}

internal fun resolveGlassQuality(
    liquidGlassEnabled: Boolean,
    reduceMotionEnabled: Boolean,
    visualQuality: VisualQuality,
): GlassQuality = when {
    !liquidGlassEnabled -> GlassQuality.DISABLED
    reduceMotionEnabled || visualQuality == VisualQuality.POWER_SAVER -> GlassQuality.PERFORMANCE
    visualQuality == VisualQuality.REFINED -> GlassQuality.HIGH
    else -> GlassQuality.BALANCED
}

object HeimaMotionTokens {
    const val Instant = 90
    const val Fast = 150
    const val Standard = 220
    const val Emphasized = 320

    fun <T> responsive(reduceMotion: Boolean): AnimationSpec<T> = if (reduceMotion) {
        tween(durationMillis = Instant)
    } else {
        spring(dampingRatio = .92f, stiffness = 760f, visibilityThreshold = null)
    }

    fun <T> snap(reduceMotion: Boolean): AnimationSpec<T> = if (reduceMotion) {
        tween(durationMillis = Fast)
    } else {
        spring(dampingRatio = .88f, stiffness = 560f, visibilityThreshold = null)
    }

    fun <T> soft(reduceMotion: Boolean): AnimationSpec<T> = if (reduceMotion) {
        tween(durationMillis = Fast)
    } else {
        spring(dampingRatio = .86f, stiffness = 420f, visibilityThreshold = null)
    }

    /** 四期：弹性回弹（按压物理 scale 0.97 → 回弹）。 */
    fun <T> bounce(reduceMotion: Boolean): AnimationSpec<T> = if (reduceMotion) {
        tween(durationMillis = Fast)
    } else {
        spring(dampingRatio = .75f, stiffness = 480f, visibilityThreshold = null)
    }

    /** 四期：数字滚动到位（AnimatedAmount，180ms 量级的低过冲 spring）。 */
    fun <T> amount(reduceMotion: Boolean): AnimationSpec<T> = if (reduceMotion) {
        tween(durationMillis = Fast)
    } else {
        spring(dampingRatio = .82f, stiffness = 420f, visibilityThreshold = null)
    }

    /** 四期：按压物理快回弹（60~90ms 量级）。 */
    fun <T> press(reduceMotion: Boolean): AnimationSpec<T> = if (reduceMotion) {
        tween(durationMillis = Instant)
    } else {
        spring(dampingRatio = .80f, stiffness = 900f, visibilityThreshold = null)
    }

    fun sharedAxisX(forward: Boolean, reduceMotion: Boolean, distance: Int): ContentTransform {
        if (reduceMotion) return fadeIn(tween(Instant)) togetherWith fadeOut(tween(Instant))
        val outgoing = (Standard * .35f).toInt()
        val incoming = Standard - outgoing
        val enter: EnterTransition = slideInHorizontally(
            animationSpec = tween(Standard, easing = FastOutSlowInEasing),
            initialOffsetX = { if (forward) distance else -distance },
        ) + fadeIn(tween(incoming, delayMillis = outgoing, easing = LinearOutSlowInEasing))
        val exit: ExitTransition = slideOutHorizontally(
            animationSpec = tween(Standard, easing = FastOutSlowInEasing),
            targetOffsetX = { if (forward) -distance else distance },
        ) + fadeOut(tween(outgoing, easing = FastOutLinearInEasing))
        return enter togetherWith exit
    }
}

object HeimaType {
    val displayAmount: TextStyle
        @Composable get() = androidx.compose.material3.MaterialTheme.typography.displayLarge
    val heroNumber: TextStyle
        @Composable get() = androidx.compose.material3.MaterialTheme.typography.headlineLarge
    val sectionTitle: TextStyle
        @Composable get() = androidx.compose.material3.MaterialTheme.typography.headlineMedium
    val cardTitle: TextStyle
        @Composable get() = androidx.compose.material3.MaterialTheme.typography.titleMedium
    val body: TextStyle
        @Composable get() = androidx.compose.material3.MaterialTheme.typography.bodyLarge
    val caption: TextStyle
        @Composable get() = androidx.compose.material3.MaterialTheme.typography.bodyMedium
    val dataLabel: TextStyle
        @Composable get() = androidx.compose.material3.MaterialTheme.typography.labelLarge
}

internal fun HeimaShadowLevel.elevation(): Dp = when (this) {
    HeimaShadowLevel.NONE -> 0.dp
    HeimaShadowLevel.SOFT -> 8.dp
    HeimaShadowLevel.FLOAT -> 16.dp
    HeimaShadowLevel.MODAL -> 24.dp
}

internal fun AppThemeTokens.solidSurface(role: HeimaSurfaceRole): Color = when (role) {
    HeimaSurfaceRole.HERO,
    HeimaSurfaceRole.OVERLAY -> surfaceElevated
    HeimaSurfaceRole.CHART,
    HeimaSurfaceRole.LIST -> surface
    else -> surfaceVariant
}
