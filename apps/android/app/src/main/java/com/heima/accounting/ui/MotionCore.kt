package com.heima.accounting.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.HeimaMotionTokens
import com.heima.accounting.designsystem.HeimaSurfaceRole
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.LocalHeimaScrolling
import com.heima.accounting.domain.formatYuan
import java.math.BigDecimal
import kotlin.math.roundToLong
import kotlinx.coroutines.delay

/** 千分位金额展示（拍板 3：仅统计页总额 + 首页大数字使用）。 */
internal fun Long.formatThousands(showSymbol: Boolean = true): String {
    val value = BigDecimal(this).movePointLeft(2).setScale(2)
    val plain = value.toPlainString()
    val parts = plain.split('.')
    val integerPart = parts[0]
    val sign = if (integerPart.startsWith("-")) "-" else ""
    val digits = integerPart.removePrefix("-")
    val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
    return (if (showSymbol) "¥" else "") + sign + grouped + "." + parts[1]
}

/**
 * C1 + B1/B8/B16：数字滚动。spring(阻尼.82, 刚度420)，180ms 量级；reduceMotion 直接显示终值。
 * "千分位分组逐组跳入"由 [format] 回调实现（B8 传入 formatThousands）。
 */
@Composable
fun AnimatedAmount(
    targetCents: Long,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    durationMs: Int = 180,
    style: TextStyle,
    color: Color,
    prefix: String = "",
    signed: Boolean = false,
    format: (Long) -> String = { it.formatYuan() },
    onSettled: (() -> Unit)? = null,
) {
    val reduceMotion = HeimaTheme.motion.reduceMotion
    var displayCents by remember { mutableStateOf(targetCents) }
    val progress = remember { Animatable(0f) }
    val currentOnSettled by rememberUpdatedState(onSettled)
    // durationMs 仅为调用方语义提示（与 design system 的 amount() spring 量级一致）；
    // 实际时长由 spring 刚度/阻尼决定，reduceMotion 下直接显示终值。用 0→1 进度插值
    // 而非把 Long 当 Float 动画，避免大额（≥ 千万级分）丢失整数精度。
    LaunchedEffect(targetCents, reduceMotion) {
        if (reduceMotion) {
            displayCents = targetCents
        } else {
            val from = displayCents
            val to = targetCents
            if (from != to) {
                progress.snapTo(0f)
                progress.animateTo(1f, HeimaMotionTokens.amount(reduceMotion)) {
                    displayCents = (from + (to - from) * value).roundToLong()
                }
                displayCents = to
                // E3：滚动到位只响一次；金额未变化不响；快速连续变化由 LaunchedEffect 取消旧动画，
                // 只让最后一次动画完成时触发（天然防抖）。
                currentOnSettled?.invoke()
            }
        }
    }
    val displayValue = if (visible) {
        val sign = if (signed && displayCents > 0L) "+" else ""
        "$prefix$sign${format(displayCents)}"
    } else {
        "$prefix¥••••"
    }
    Text(displayValue, modifier = modifier, style = style, color = color, maxLines = 1)
}

/**
 * C4：按压物理 scale 0.97 + 弹性回弹（60~90ms）。触控目标由调用方保证 ≥44dp。
 */
@Composable
fun Modifier.pressFeedback(
    interactionSource: MutableInteractionSource,
    reduceMotion: Boolean = HeimaTheme.motion.reduceMotion,
    pressedScale: Float = 0.97f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) pressedScale else 1f,
        animationSpec = HeimaMotionTokens.press(reduceMotion),
        label = "press_feedback",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * C3：逐项 stagger 入场（每项 baseDelayMs，40~60ms 可调）。reduceMotion 一次性淡入。
 */
@Composable
fun StaggeredContent(
    itemCount: Int,
    modifier: Modifier = Modifier,
    baseDelayMs: Int = 40,
    reduceMotion: Boolean = HeimaTheme.motion.reduceMotion,
    content: @Composable (index: Int) -> Unit,
) {
    Column(modifier) {
        for (index in 0 until itemCount) {
            StaggeredItem(index, baseDelayMs, reduceMotion, content)
        }
    }
}

@Composable
private fun StaggeredItem(
    index: Int,
    baseDelayMs: Int,
    reduceMotion: Boolean,
    content: @Composable (Int) -> Unit,
) {
    val shiftPx = with(LocalDensity.current) { 12.dp.toPx() }
    val visible = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(index, reduceMotion) {
        if (reduceMotion) {
            visible.snapTo(1f)
        } else {
            delay((index * baseDelayMs).toLong())
            visible.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
        }
    }
    Box(
        Modifier.graphicsLayer {
            alpha = visible.value
            translationY = (1f - visible.value) * shiftPx
        },
    ) {
        content(index)
    }
}

/**
 * C6：成功气泡，底部上浮 + ✓ 描边 + 1300ms 后自动淡出回调 onDismiss。
 */
@Composable
fun SuccessBubble(
    text: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    val reduceMotion = HeimaTheme.motion.reduceMotion
    LaunchedEffect(visible) {
        if (visible) {
            delay(1300)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = if (reduceMotion) {
            fadeIn(tween(90))
        } else {
            slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(220))
        },
        exit = fadeOut(tween(if (reduceMotion) 90 else 260)),
        modifier = modifier,
    ) {
        GlassSurface(
            modifier = Modifier.height(46.dp),
            cornerRadius = 23.dp,
            backdropBlur = false,
            role = HeimaSurfaceRole.OVERLAY,
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Canvas(Modifier.size(18.dp)) {
                    val check = Path().apply {
                        moveTo(size.width * .16f, size.height * .52f)
                        lineTo(size.width * .40f, size.height * .76f)
                        lineTo(size.width * .84f, size.height * .26f)
                    }
                    drawPath(
                        check,
                        color = palette.income,
                        style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
                Text(text, color = palette.textPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * C5：滚动视差（≤6px，反向位移），滚动中禁用（读 LocalHeimaScrolling 省算力）。
 */
@Composable
fun Modifier.scrollParallax(
    scrollOffset: Float,
    maxShiftPx: Float = 6f,
): Modifier {
    val scrolling = LocalHeimaScrolling.current
    val reduceMotion = HeimaTheme.motion.reduceMotion
    val shift = if (scrolling || reduceMotion) 0f else (scrollOffset * 0.05f).coerceIn(0f, maxShiftPx)
    return graphicsLayer { translationY = -shift }
}

/**
 * B14：三联胶囊，选中项流体填充（高亮从选中方向流入，180ms），reduceMotion 直接切换。
 */
@Composable
fun <T> FluidPillSelector(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(options.isNotEmpty())
    val palette = HeimaTheme.palette
    val motion = HeimaTheme.motion
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val shape = RoundedCornerShape(18.dp)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(palette.surfaceMuted.copy(alpha = if (motion.darkTheme) .78f else .66f))
            .padding(4.dp),
    ) {
        val slotWidth = maxWidth / options.size
        val targetX = slotWidth * selectedIndex
        val lensX by androidx.compose.animation.core.animateDpAsState(
            targetValue = targetX,
            animationSpec = HeimaMotionTokens.snap(motion.reduceMotion),
            label = "fluid_pill_lens",
        )
        val lensShape = RoundedCornerShape(14.dp)
        Box(
            Modifier
                .offset { IntOffset(lensX.roundToPx(), 0) }
                .width(slotWidth)
                .fillMaxHeight()
                .clip(lensShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            palette.glassHighlight.copy(alpha = if (motion.darkTheme) .12f else .58f),
                            palette.brandSoft.copy(alpha = if (motion.darkTheme) .38f else .82f),
                            palette.glassTop.copy(alpha = if (motion.darkTheme) .18f else .70f),
                        ),
                    ),
                ),
        )
        Row(Modifier.fillMaxWidth()) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                val interaction = remember(value) { MutableInteractionSource() }
                val color by androidx.compose.animation.animateColorAsState(
                    if (isSelected) palette.brand else palette.textSecondary,
                    animationSpec = tween(if (motion.reduceMotion) HeimaMotionTokens.Instant else HeimaMotionTokens.Fast),
                    label = "fluid_pill_text",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pressFeedback(interaction)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = { onSelected(value) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = color,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/** B25：检查更新状态机动画（IDLE / SPINNING / DONE / ERROR）。 */
enum class SpinCheckState { IDLE, SPINNING, DONE, ERROR }

@Composable
fun SpinCheck(state: SpinCheckState, modifier: Modifier = Modifier) {
    val palette = HeimaTheme.palette
    val reduceMotion = HeimaTheme.motion.reduceMotion
    val rotation = if (reduceMotion) {
        0f
    } else {
        val infinite = rememberInfiniteTransition(label = "spin_check")
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
            label = "spin_rotation",
        ).value
    }
    Canvas(modifier.semantics { contentDescription = "检查更新状态" }) {
        val stroke = 2.dp.toPx()
        when (state) {
            SpinCheckState.SPINNING -> {
                val arcStart = if (reduceMotion) -90f else rotation - 90f
                drawArc(
                    color = palette.brand,
                    startAngle = arcStart,
                    sweepAngle = if (reduceMotion) 270f else 90f,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            SpinCheckState.DONE -> {
                val check = Path().apply {
                    moveTo(size.width * .16f, size.height * .52f)
                    lineTo(size.width * .40f, size.height * .76f)
                    lineTo(size.width * .84f, size.height * .26f)
                }
                drawPath(check, color = palette.income, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            SpinCheckState.ERROR -> {
                drawCircle(palette.expense, radius = size.minDimension * .46f, style = Stroke(stroke))
                drawLine(palette.expense, Offset(size.width * .34f, size.height * .30f), Offset(size.width * .66f, size.height * .70f), stroke, StrokeCap.Round)
                drawLine(palette.expense, Offset(size.width * .66f, size.height * .30f), Offset(size.width * .34f, size.height * .70f), stroke, StrokeCap.Round)
            }
            SpinCheckState.IDLE -> {
                drawCircle(palette.outline, radius = size.minDimension * .46f, style = Stroke(stroke))
                drawLine(
                    palette.outline,
                    Offset(size.width * .5f, size.height * .22f),
                    Offset(size.width * .5f, size.height * .64f),
                    stroke,
                    StrokeCap.Round,
                )
                drawLine(
                    palette.outline,
                    Offset(size.width * .36f, size.height * .42f),
                    Offset(size.width * .5f, size.height * .64f),
                    stroke,
                    StrokeCap.Round,
                )
                drawLine(
                    palette.outline,
                    Offset(size.width * .64f, size.height * .42f),
                    Offset(size.width * .5f, size.height * .64f),
                    stroke,
                    StrokeCap.Round,
                )
            }
        }
    }
}
