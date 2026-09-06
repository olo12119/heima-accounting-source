package com.heima.accounting.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.HeimaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** B6 下拉刷新最短展示时长（拍板 1：真实重算 + loading 动效可感知）。 */
internal const val REFRESH_MIN_DISPLAY_MS = 600L

/**
 * B6：材料化下拉刷新封装。isRefreshing 期间跑圆环 + 品牌光弧；成功瞬间 ✓ 描边。
 * 刷新最短展示时长（600ms）由调用方在 onRefresh 内落实（调用方持有 isRefreshing 状态）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: suspend () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { scope.launch { onRefresh() } },
        modifier = modifier,
        state = state,
        indicator = {
            GlassRefreshIndicator(
                isRefreshing = isRefreshing,
                state = state,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
    ) {
        content()
    }
}

@Composable
private fun GlassRefreshIndicator(
    isRefreshing: Boolean,
    state: PullToRefreshState,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    val reduceMotion = HeimaTheme.motion.reduceMotion
    val rotation = if (reduceMotion) {
        0f
    } else {
        val infinite = rememberInfiniteTransition(label = "refresh_arc")
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(950, easing = LinearEasing)),
            label = "refresh_arc_rotation",
        ).value
    }
    var wasRefreshing by remember { mutableStateOf(false) }
    var showCheck by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            wasRefreshing = true
            showCheck = false
        } else if (wasRefreshing) {
            wasRefreshing = false
            showCheck = true
            delay(600)
            showCheck = false
        }
    }

    Canvas(modifier.semantics { contentDescription = if (isRefreshing) "正在刷新账本" else "下拉刷新账本" }) {
        val stroke = 3.dp.toPx()
        val radius = 16.dp.toPx()
        val center = center
        // 底环
        drawCircle(palette.surfaceMuted, radius = radius, center = center, style = Stroke(stroke))
        when {
            showCheck -> {
                val check = Path().apply {
                    moveTo(center.x - radius * .5f, center.y)
                    lineTo(center.x - radius * .12f, center.y + radius * .42f)
                    lineTo(center.x + radius * .56f, center.y - radius * .36f)
                }
                drawPath(
                    check,
                    color = palette.income,
                    style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            isRefreshing -> {
                // 满环 + 旋转品牌光弧
                drawArc(palette.brand, -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(
                    color = palette.accent,
                    startAngle = rotation - 90f,
                    sweepAngle = 90f,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            else -> {
                val progress = state.distanceFraction.coerceIn(0f, 1f)
                if (progress > 0f) {
                    drawArc(
                        color = palette.brand,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
    }
}
