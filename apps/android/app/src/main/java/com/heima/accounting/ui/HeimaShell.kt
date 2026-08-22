package com.heima.accounting.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
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
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.HeimaThemeStyle
import com.heima.accounting.designsystem.VisualQuality
import com.heima.accounting.ui.screens.BudgetScreen
import com.heima.accounting.ui.screens.HomeScreen
import com.heima.accounting.ui.screens.ProfileScreen
import com.heima.accounting.ui.screens.RecordSheet
import com.heima.accounting.ui.screens.StatisticsScreen
import kotlinx.coroutines.launch

@Composable
fun HeimaShell(
    themeStyle: HeimaThemeStyle,
    visualQuality: VisualQuality,
    reduceMotion: Boolean,
    powerSaveMode: Boolean,
    onThemeStyleChange: (HeimaThemeStyle) -> Unit,
    onVisualQualityChange: (VisualQuality) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    var recordPanelVisible by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val effectiveReduceMotion = HeimaTheme.motion.reduceMotion

    BackHandler(enabled = recordPanelVisible) {
        recordPanelVisible = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackdrop()

        AnimatedContent(
            targetState = destination,
            transitionSpec = {
                destinationTransform(
                    from = initialState,
                    to = targetState,
                    reduceMotion = effectiveReduceMotion,
                )
            },
            contentKey = { it.name },
            label = "primary_navigation",
            modifier = Modifier.fillMaxSize(),
        ) { screen ->
            when (screen) {
                AppDestination.HOME -> HomeScreen(onRecord = { recordPanelVisible = true })
                AppDestination.STATISTICS -> StatisticsScreen()
                AppDestination.BUDGET -> BudgetScreen()
                AppDestination.PROFILE -> ProfileScreen(
                    themeStyle = themeStyle,
                    visualQuality = visualQuality,
                    reduceMotion = reduceMotion,
                    powerSaveMode = powerSaveMode,
                    onThemeStyleChange = onThemeStyleChange,
                    onVisualQualityChange = onVisualQualityChange,
                    onReduceMotionChange = onReduceMotionChange,
                )
                AppDestination.RECORD -> Unit
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 104.dp),
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
                .padding(bottom = 8.dp),
        )

        if (recordPanelVisible) {
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
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
    return (
        slideInHorizontally(
            initialOffsetX = { fullWidth -> direction * fullWidth / 12 },
            animationSpec = movement,
        ) + fadeIn()
        ) togetherWith (
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> -direction * fullWidth / 16 },
            animationSpec = movement,
        ) + fadeOut()
        )
}
