package com.heima.accounting.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.AmbientBackdrop
import com.heima.accounting.designsystem.HeimaColorMode
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.HeimaThemeStyle
import com.heima.accounting.designsystem.LocalHeimaHazeState
import com.heima.accounting.designsystem.VisualQuality
import com.heima.accounting.ui.screens.BudgetScreen
import com.heima.accounting.ui.screens.HomeScreen
import com.heima.accounting.ui.screens.ProfileScreen
import com.heima.accounting.ui.screens.RecordSheet
import com.heima.accounting.ui.screens.StatisticsScreen
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch

@Composable
fun HeimaShell(
    themeStyle: HeimaThemeStyle,
    colorMode: HeimaColorMode,
    visualQuality: VisualQuality,
    reduceMotion: Boolean,
    powerSaveMode: Boolean,
    amountsVisible: Boolean,
    onThemeStyleChange: (HeimaThemeStyle) -> Unit,
    onColorModeChange: (HeimaColorMode) -> Unit,
    onVisualQualityChange: (VisualQuality) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
    onAmountsVisibleChange: (Boolean) -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    var recordPanelVisible by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val motion = HeimaTheme.motion
    val hazeState = rememberHazeState(
        blurEnabled = motion.quality != VisualQuality.POWER_SAVER,
    )
    SideEffect {
        hazeState.blurEnabled = motion.quality != VisualQuality.POWER_SAVER
    }

    BackHandler(enabled = recordPanelVisible) {
        recordPanelVisible = false
    }

    CompositionLocalProvider(LocalHeimaHazeState provides hazeState) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState),
            ) {
                AmbientBackdrop()

                AnimatedContent(
                    targetState = destination,
                    transitionSpec = {
                        destinationTransform(
                            from = initialState,
                            to = targetState,
                            reduceMotion = motion.reduceMotion,
                        )
                    },
                    contentKey = { it.name },
                    label = "primary_navigation",
                    modifier = Modifier.fillMaxSize(),
                ) { screen ->
                    when (screen) {
                        AppDestination.HOME -> HomeScreen(
                            amountsVisible = amountsVisible,
                            onAmountsVisibleChange = onAmountsVisibleChange,
                            onRecord = { recordPanelVisible = true },
                        )
                        AppDestination.STATISTICS -> StatisticsScreen(amountsVisible = amountsVisible)
                        AppDestination.BUDGET -> BudgetScreen()
                        AppDestination.PROFILE -> ProfileScreen(
                            themeStyle = themeStyle,
                            colorMode = colorMode,
                            visualQuality = visualQuality,
                            reduceMotion = reduceMotion,
                            powerSaveMode = powerSaveMode,
                            onThemeStyleChange = onThemeStyleChange,
                            onColorModeChange = onColorModeChange,
                            onVisualQualityChange = onVisualQualityChange,
                            onReduceMotionChange = onReduceMotionChange,
                        )
                        AppDestination.RECORD -> Unit
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 124.dp),
            )

            HeimaBottomBar(
                selected = destination,
                onDestinationSelected = { selected ->
                    if (selected != AppDestination.RECORD) destination = selected
                },
                onRecord = { recordPanelVisible = true },
                recordPanelVisible = recordPanelVisible,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 4.dp),
            )

            AnimatedVisibility(
                visible = recordPanelVisible,
                enter = if (motion.reduceMotion) {
                    fadeIn(tween(90))
                } else {
                    fadeIn(tween(150)) +
                        slideInVertically(
                            initialOffsetY = { it / 3 },
                            animationSpec = spring(
                                dampingRatio = 0.78f,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        ) + scaleIn(initialScale = 0.96f)
                },
                exit = if (motion.reduceMotion) {
                    fadeOut(tween(70))
                } else {
                    fadeOut(tween(120)) +
                        slideOutVertically(
                            targetOffsetY = { it / 4 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        ) + scaleOut(targetScale = 0.97f)
                },
            ) {
                RecordSheet(
                    onDismiss = { recordPanelVisible = false },
                    onVisualSave = { description ->
                        recordPanelVisible = false
                        scope.launch {
                            snackbarHostState.showSnackbar(description)
                        }
                    },
                )
            }
        }
    }
}

private fun destinationTransform(
    from: AppDestination,
    to: AppDestination,
    reduceMotion: Boolean,
): ContentTransform {
    if (reduceMotion) {
        return EnterTransition.None togetherWith ExitTransition.None
    }
    val direction = if (to.ordinal > from.ordinal) 1 else -1
    val movement = spring<IntOffset>(
        dampingRatio = 0.82f,
        stiffness = Spring.StiffnessMediumLow,
    )
    return (
        slideInHorizontally(
            initialOffsetX = { fullWidth -> direction * fullWidth / 10 },
            animationSpec = movement,
        ) + fadeIn(tween(120))
        ) togetherWith (
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> -direction * fullWidth / 14 },
            animationSpec = movement,
        ) + fadeOut(tween(100))
        )
}
