package com.heima.accounting.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.domain.DailyTotal
import com.heima.accounting.domain.LedgerSnapshot
import com.heima.accounting.domain.formatYuan
import java.time.format.DateTimeFormatter
import java.util.Locale

/** D1：顶部亮 → 底部暗的同色族渐变；深色下整体 lerp 白 .12 再提一档。 */
@Composable
fun chartFill(color: Color): Brush {
    val dark = HeimaTheme.motion.darkTheme
    val base = if (dark) lerp(color, Color.White, .12f) else color
    return Brush.verticalGradient(listOf(lighten(base, .22f), base, darken(base, .14f)))
}

/** D3/B11：金银铜金属渐变（rank: 0=金 1=银 2=铜，其余返回 null 用 chartFill）。 */
@Composable
fun metalGradient(rank: Int): Brush? {
    val dark = HeimaTheme.motion.darkTheme
    val colors = when (rank) {
        0 -> listOf(Color(0xFFFFE9A8), Color(0xFFF6C445), Color(0xFFD99116))
        1 -> listOf(Color(0xFFF2F4F7), Color(0xFFC9D1DC), Color(0xFF929DAD))
        2 -> listOf(Color(0xFFF3C19B), Color(0xFFD98E5F), Color(0xFFB06A3C))
        else -> return null
    }
    return Brush.verticalGradient(if (dark) colors.map { lerp(it, Color.White, .12f) } else colors)
}

/** D4：虚线 path（点划线 dash 8dp / gap 6dp）。 */
@Composable
fun dashedPathEffect(): PathEffect {
    val density = LocalDensity.current
    return PathEffect.dashPathEffect(floatArrayOf(with(density) { 8.dp.toPx() }, with(density) { 6.dp.toPx() }))
}

/**
 * B10：最后数据点粒子拖尾（沿折线末尾回退 6~8 个点，半径递减 + alpha 递减的圆点）。
 * 只在切换/静止展示，reduceMotion 下直接跳过。
 */
@Composable
fun ParticleTrail(points: List<Offset>, color: Color, modifier: Modifier = Modifier) {
    val reduceMotion = HeimaTheme.motion.reduceMotion
    if (reduceMotion || points.size < 2) return
    val density = LocalDensity.current
    Canvas(modifier) {
        val trailCount = minOf(6, points.size - 1)
        for (i in 1..trailCount) {
            val point = points[points.size - 1 - i]
            val t = i / 6f
            val radius = (3.2f + (0.8f - 3.2f) * t).dp.toPx()
            val alpha = 0.35f + (0f - 0.35f) * t
            drawCircle(color.copy(alpha = alpha), radius = radius, center = point)
        }
    }
}

/**
 * D5：日记式（拍板 2：本月有消费天数 ≤2 时触发），按天分组的小卡片列。
 */
@Composable
fun DiaryTrend(
    totals: List<DailyTotal>,
    snapshot: LedgerSnapshot,
    amountsVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    val formatter = DateTimeFormatter.ofPattern("M月d日", Locale.SIMPLIFIED_CHINESE)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        totals.forEach { day ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        day.date.format(formatter),
                        color = palette.textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (day.incomeCents > 0L) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "收入 ${if (amountsVisible) day.incomeCents.formatYuan() else "¥••••"}",
                            color = palette.income,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                Text(
                    if (amountsVisible) day.expenseCents.formatYuan() else "¥••••",
                    color = palette.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun lighten(color: Color, amount: Float): Color = lerp(color, Color.White, amount)
private fun darken(color: Color, amount: Float): Color = lerp(color, Color.Black, amount)

/**
 * B3：图表切换粒子过渡。旧图淡出 + 微粒上飘、新图淡入生长；reduceMotion 降级为纯淡入淡出。
 * [targetState] 变化时触发；首次进入不触发（由 previousState 判定，避免列表初次挂载就喷粒子）。
 */
@Composable
fun <T> ChartSwitchTransition(
    targetState: T,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val palette = HeimaTheme.palette
    val burst = remember { Animatable(0f) }
    var previous by remember { mutableStateOf(targetState) }
    LaunchedEffect(targetState, reduceMotion) {
        val changed = previous != targetState
        previous = targetState
        if (!changed || reduceMotion) return@LaunchedEffect
        burst.snapTo(0f)
        burst.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    Box(modifier) {
        AnimatedContent(
            targetState = targetState,
            transitionSpec = {
                if (reduceMotion) {
                    fadeIn(tween(90)) togetherWith fadeOut(tween(90))
                } else {
                    (fadeIn(tween(260, delayMillis = 110)) + scaleIn(initialScale = 0.94f, animationSpec = tween(260, delayMillis = 110))) togetherWith
                        (fadeOut(tween(140)) + scaleOut(targetScale = 1.04f, animationSpec = tween(140)))
                }
            },
            label = "chart_switch",
        ) { state -> content(state) }
        if (!reduceMotion && burst.value < 0.999f) {
            ChartSwitchParticles(burst.value, palette.brand, Modifier.fillMaxSize())
        }
    }
}

/** 切换瞬间向上飘散、半径/透明度递减的确定性微粒（复用 B10 ParticleTrail 思路）。 */
@Composable
private fun ChartSwitchParticles(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val particles = remember {
        List(14) { i ->
            val seed = (i * 37) % 100
            ChartParticle(
                x = seed / 100f,
                y = 0.45f + (i % 5) * 0.10f,
                radius = 1.8f + (i % 4) * 0.7f,
                rise = 0.30f + (i % 5) * 0.10f,
                drift = (if (i % 2 == 0) 1f else -1f) * (0.02f + (i % 3) * 0.014f),
            )
        }
    }
    Canvas(modifier) {
        particles.forEach { p ->
            val x = (p.x + progress * p.drift).coerceIn(0f, 1f)
            val y = (p.y - progress * p.rise).coerceIn(0f, 1f)
            val alpha = (0.42f * (1f - progress)).coerceAtLeast(0f)
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = p.radius.dp.toPx() * (1f - progress * 0.5f),
                center = Offset(x * size.width, y * size.height),
            )
        }
    }
}

private data class ChartParticle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val rise: Float,
    val drift: Float,
)
