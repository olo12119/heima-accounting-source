package com.heima.accounting.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.foundation.pager.PagerState
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.HeimaTheme
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

private const val NavigationFlingThresholdPxPerSecond = 760f

private sealed interface NavigationDragCommand {
    data class Position(val visualPosition: Float) : NavigationDragCommand
    data class Finish(
        val visualPosition: Float,
        val fingerVelocityX: Float,
    ) : NavigationDragCommand
    data object Cancel : NavigationDragCommand
}

/**
 * The persistent pager has four pages while the visual bar has five slots.
 * RECORD occupies visual slot 2 but is a modal action, not a pager page.
 */
internal fun pagerPositionToVisualSlot(pagePosition: Float): Float = when {
    pagePosition <= 1f -> pagePosition
    pagePosition <= 2f -> 1f + (pagePosition - 1f) * 2f
    else -> pagePosition + 1f
}.coerceIn(0f, AppDestination.entries.lastIndex.toFloat())

internal fun visualSlotToPagerPosition(visualPosition: Float): Float = when {
    visualPosition <= 1f -> visualPosition
    visualPosition <= 3f -> 1f + (visualPosition - 1f) / 2f
    else -> visualPosition - 1f
}.coerceIn(0f, 3f)

internal fun navigationDragTargetPage(
    pagePosition: Float,
    fingerVelocityX: Float,
    pageCount: Int = 4,
): Int {
    val lastPage = (pageCount - 1).coerceAtLeast(0)
    val target = when {
        fingerVelocityX > NavigationFlingThresholdPxPerSecond -> ceil(pagePosition + 0.001f).toInt()
        fingerVelocityX < -NavigationFlingThresholdPxPerSecond -> floor(pagePosition - 0.001f).toInt()
        else -> pagePosition.roundToInt()
    }
    return target.coerceIn(0, lastPage)
}

/** A continuous glass bar with one restrained sliding selection lens. */
@Composable
fun HeimaBottomBar(
    selected: AppDestination,
    pagerState: PagerState,
    onDestinationSelected: (AppDestination) -> Unit,
    onRecord: () -> Unit,
    recordPanelVisible: Boolean,
    backdrop: Backdrop,
    navigationProgress: Float,
    onBoundaryFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    val motion = HeimaTheme.motion
    val navigationScope = rememberCoroutineScope()
    var dragMorph by remember { mutableFloatStateOf(0f) }
    var navigationDragJob by remember { mutableStateOf<Job?>(null) }

    GlassSurface(
        modifier = modifier.fillMaxWidth().height(76.dp).padding(horizontal = 14.dp),
        cornerRadius = 30.dp,
        elevation = 18.dp,
        backdropBlur = true,
    ) {
        BoxWithConstraints(Modifier.matchParentSize()) {
            val slotWidth = maxWidth / AppDestination.entries.size
            val slotWidthPx = with(LocalDensity.current) { slotWidth.toPx() }
            val lensWidth = slotWidth - 8.dp
            // Recording is a modal action, but while its sheet is visible the same
            // continuous lens rests on the primary slot to preserve a clear active state.
            val displayNavigationProgress = if (recordPanelVisible) 2f else navigationProgress
            val x = slotWidth * displayNavigationProgress + (slotWidth - lensWidth) / 2
            val velocityStretch = if (motion.reduceMotion) 0f else {
                min(abs(dragMorph) * .16f, .12f)
            }

            fun beginNavigationDrag(startingPage: Int): Channel<NavigationDragCommand> {
                navigationDragJob?.cancel()
                // Pointer samples can arrive faster than a frame. Only the newest
                // absolute position matters; conflation prevents a stale drag queue
                // from making the lens trail behind the finger or adding jank.
                val commands = Channel<NavigationDragCommand>(Channel.CONFLATED)
                navigationDragJob = navigationScope.launch {
                    var finishVelocityX = 0f
                    var finishVisualPosition = pagerPositionToVisualSlot(startingPage.toFloat())
                    var cancelled = false
                    try {
                        pagerState.scroll(MutatePriority.UserInput) {
                            while (true) {
                                when (val command = commands.receive()) {
                                    is NavigationDragCommand.Position -> {
                                        val pageSizePx = pagerState.layoutInfo.pageSize.toFloat()
                                        if (pageSizePx <= 0f || slotWidthPx <= 0f) continue
                                        val currentPagePosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
                                        val targetPagePosition = visualSlotToPagerPosition(command.visualPosition)
                                        scrollBy((targetPagePosition - currentPagePosition) * pageSizePx)
                                    }

                                    is NavigationDragCommand.Finish -> {
                                        finishVisualPosition = command.visualPosition
                                        finishVelocityX = command.fingerVelocityX
                                        break
                                    }

                                    NavigationDragCommand.Cancel -> {
                                        cancelled = true
                                        break
                                    }
                                }
                            }
                        }

                        val pagePosition = visualSlotToPagerPosition(finishVisualPosition)
                        val targetPage = if (cancelled) {
                            startingPage
                        } else {
                            navigationDragTargetPage(pagePosition, finishVelocityX, pagerState.pageCount)
                        }
                        if (motion.reduceMotion) {
                            pagerState.scrollToPage(targetPage)
                        } else {
                            pagerState.animateScrollToPage(
                                page = targetPage,
                                animationSpec = spring(dampingRatio = .90f, stiffness = 520f),
                            )
                        }
                        if (!cancelled && targetPage != startingPage) onBoundaryFeedback()
                    } finally {
                        dragMorph = 0f
                        commands.close()
                    }
                }
                return commands
            }

            val directNavigationDrag = Modifier.pointerInput(pagerState, slotWidthPx, motion.reduceMotion) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val velocityTracker = VelocityTracker().apply {
                        addPosition(down.uptimeMillis, down.position)
                    }
                    var overSlopX = 0f
                    val dragStart = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                        if (overSlop != 0f) {
                            overSlopX = overSlop
                            change.consume()
                        }
                    } ?: return@awaitEachGesture

                    val startingPage = pagerState.settledPage
                    val startingVisualPosition = pagerPositionToVisualSlot(startingPage.toFloat())
                    val commands = beginNavigationDrag(startingPage)
                    var finishedNormally = false
                    var latestVisualPosition = startingVisualPosition
                    try {
                        latestVisualPosition = (startingVisualPosition +
                            (dragStart.position.x - down.position.x) / slotWidthPx)
                            .coerceIn(0f, AppDestination.entries.lastIndex.toFloat())
                        commands.trySend(NavigationDragCommand.Position(latestVisualPosition))
                        dragMorph = (overSlopX / slotWidthPx).coerceIn(-1f, 1f)
                        var activeChange = dragStart
                        while (activeChange.pressed) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            latestVisualPosition = (startingVisualPosition +
                                (change.position.x - down.position.x) / slotWidthPx)
                                .coerceIn(0f, AppDestination.entries.lastIndex.toFloat())
                            if (!change.pressed) {
                                finishedNormally = true
                                break
                            }
                            val deltaX = change.positionChange().x
                            if (deltaX != 0f) {
                                change.consume()
                                commands.trySend(NavigationDragCommand.Position(latestVisualPosition))
                                dragMorph = (deltaX / slotWidthPx).coerceIn(-1f, 1f)
                            }
                            activeChange = change
                        }
                    } finally {
                        val command = if (finishedNormally) {
                            NavigationDragCommand.Finish(
                                visualPosition = latestVisualPosition,
                                fingerVelocityX = velocityTracker.calculateVelocity().x,
                            )
                        } else {
                            NavigationDragCommand.Cancel
                        }
                        commands.trySend(command)
                    }
                }
            }
            val baseLens = Modifier
                .offset { IntOffset(x.roundToPx(), 8.dp.roundToPx()) }
                .width(lensWidth)
                .height(60.dp)
                .graphicsLayer {
                    scaleX = 1f + velocityStretch
                    scaleY = 1f - velocityStretch * 0.28f
                }
                .clip(RoundedCornerShape(25.dp))
            val fullGlassSupported = motion.expensiveGlassEnabled && Build.VERSION.SDK_INT >= 33

            Box(
                modifier = if (fullGlassSupported) {
                    baseLens.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(25.dp) },
                        effects = {
                            vibrancy()
                            blur(7.dp.toPx())
                            lens(12.dp.toPx(), 14.dp.toPx(), chromaticAberration = motion.quality.name == "REFINED")
                        },
                        highlight = { Highlight.Default.copy(alpha = if (motion.darkTheme) 0.30f else 0.64f) },
                        shadow = { Shadow(alpha = if (motion.darkTheme) 0.22f else 0.40f) },
                        innerShadow = { InnerShadow(radius = 7.dp, alpha = if (motion.darkTheme) 0.18f else 0.32f) },
                        onDrawSurface = {
                            drawRect(
                                if (motion.darkTheme) Color(0xFF8BB9FF).copy(alpha = 0.10f)
                                else palette.brandSoft.copy(alpha = 0.24f),
                            )
                        },
                    )
                } else {
                    baseLens
                        .background(
                            if (motion.darkTheme) palette.brandSoft.copy(alpha = 0.16f)
                            else palette.brandSoft.copy(alpha = 0.48f),
                        )
                        .border(
                            1.dp,
                            palette.glassStroke.copy(alpha = if (motion.darkTheme) 0.42f else 0.72f),
                            RoundedCornerShape(25.dp),
                        )
                },
            )

            Row(
                Modifier
                    .matchParentSize()
                    .semantics { contentDescription = "底部导航，可直接左右拖动" }
                    .then(directNavigationDrag),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppDestination.entries.forEach { destination ->
                    BottomBarItem(
                        destination = destination,
                        selected = selected == destination || (destination == AppDestination.RECORD && recordPanelVisible),
                        onClick = {
                            if (destination == AppDestination.RECORD) onRecord()
                            else onDestinationSelected(destination)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    destination: AppDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    val motion = HeimaTheme.motion
    val primary = destination == AppDestination.RECORD
    val feedbackSpec = if (motion.reduceMotion || primary) tween<Color>(90) else spring(dampingRatio = 0.9f, stiffness = 500f)
    val color by animateColorAsState(
        if (selected || primary) palette.brand else palette.textTertiary,
        feedbackSpec,
        label = "navigation_color",
    )
    val selection by animateFloatAsState(
        if (selected) 1f else 0f,
        if (motion.reduceMotion || primary) tween(90) else spring(dampingRatio = 0.88f, stiffness = 500f),
        label = "navigation_selection",
    )
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && !motion.reduceMotion) .94f else 1f,
        animationSpec = if (motion.reduceMotion) tween(50) else spring(dampingRatio = .82f, stiffness = 700f),
        label = "primary_navigation_press",
    )
    Column(
        modifier = modifier
            .height(76.dp)
            .semantics {
                contentDescription = destination.accessibilityLabel
                stateDescription = when {
                    primary && selected -> "主操作，已打开"
                    primary -> "主操作"
                    selected -> "已选中"
                    else -> "未选中"
                }
                this.selected = selected
            }
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clickable(source, indication = null, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(if (primary) 44.dp else 23.dp)
                .then(
                    if (primary) {
                        Modifier
                            .clip(RoundedCornerShape(17.dp))
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        palette.glassHighlight.copy(alpha = if (motion.darkTheme) .10f else .62f),
                                        palette.brand.copy(alpha = if (motion.darkTheme) .44f else .34f),
                                        palette.brandSoft.copy(alpha = if (motion.darkTheme) .72f else .94f),
                                    ),
                                ),
                            )
                            .border(
                                1.2.dp,
                                if (motion.darkTheme) palette.accent.copy(alpha = .58f)
                                else palette.glassHighlight.copy(alpha = .92f),
                                RoundedCornerShape(17.dp),
                            )
                            .padding(7.dp)
                    } else Modifier
                )
                .graphicsLayer {
                    scaleX = lerp(1f, 1.08f, selection)
                    scaleY = lerp(1f, 1.08f, selection)
                },
            contentAlignment = Alignment.Center,
        ) {
            HeimaGlyph(destination, color, Modifier.matchParentSize())
        }
        Spacer(Modifier.height(4.dp))
        Text(
            destination.label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected || primary) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
