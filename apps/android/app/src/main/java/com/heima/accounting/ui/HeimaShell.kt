package com.heima.accounting.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.heima.accounting.HeimaViewModel
import com.heima.accounting.UiEvent
import com.heima.accounting.data.LedgerState
import com.heima.accounting.designsystem.AmbientBackdrop
import com.heima.accounting.designsystem.HeimaColorMode
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.HeimaThemeStyle
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.LocalHeimaBackdrop
import com.heima.accounting.designsystem.VisualQuality
import com.heima.accounting.domain.EntryType
import com.heima.accounting.domain.Transaction
import com.heima.accounting.ui.screens.BudgetScreen
import com.heima.accounting.ui.screens.CategoriesScreen
import com.heima.accounting.ui.screens.DataScreen
import com.heima.accounting.ui.screens.HomeScreen
import com.heima.accounting.ui.screens.ProfileScreen
import com.heima.accounting.ui.screens.RecordSheet
import com.heima.accounting.ui.screens.RecordsScreen
import com.heima.accounting.ui.screens.StatisticsScreen
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

private enum class ManagementPage { RECORDS, CATEGORIES, DATA }
private val PagerDestinations = listOf(
    AppDestination.HOME,
    AppDestination.STATISTICS,
    AppDestination.BUDGET,
    AppDestination.PROFILE,
)

@Composable
fun HeimaShell(
    viewModel: HeimaViewModel,
    ledgerState: LedgerState,
    feedback: InteractionFeedback,
    themeStyle: HeimaThemeStyle,
    colorMode: HeimaColorMode,
    visualQuality: VisualQuality,
    reduceMotion: Boolean,
    powerSaveMode: Boolean,
    amountsVisible: Boolean,
    liquidGlassEnabled: Boolean,
    soundEnabled: Boolean,
    hapticEnabled: Boolean,
    onThemeStyleChange: (HeimaThemeStyle) -> Unit,
    onColorModeChange: (HeimaColorMode) -> Unit,
    onVisualQualityChange: (VisualQuality) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
    onAmountsVisibleChange: (Boolean) -> Unit,
    onLiquidGlassEnabledChange: (Boolean) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onHapticEnabledChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { PagerDestinations.size })
    val destination by remember {
        derivedStateOf { PagerDestinations[pagerState.currentPage.coerceIn(PagerDestinations.indices)] }
    }
    val navigationProgress by remember {
        derivedStateOf {
            val pagePosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
            when {
                pagePosition <= 1f -> pagePosition
                pagePosition <= 2f -> 1f + (pagePosition - 1f) * 2f
                else -> pagePosition + 1f
            }.coerceIn(0f, AppDestination.entries.lastIndex.toFloat())
        }
    }
    var managementPage by rememberSaveable { mutableStateOf<ManagementPage?>(null) }
    var recordPanelVisible by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Transaction?>(null) }
    var restoreCandidate by remember { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val motion = HeimaTheme.motion
    val backdrop = rememberLayerBackdrop()
    val backdropRecorder = if (motion.expensiveGlassEnabled && Build.VERSION.SDK_INT >= 33) {
        Modifier.layerBackdrop(backdrop)
    } else {
        Modifier
    }
    val contentAlpha by animateFloatAsState(
        targetValue = if (recordPanelVisible) .58f else 1f,
        animationSpec = tween(if (motion.reduceMotion) 60 else 120),
        label = "modal_background_weight",
    )

    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val content = pendingExport
        if (uri != null && content != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(content) } }
                .onSuccess { scope.launch { snackbar.showSnackbar("文件已导出") } }
                .onFailure { scope.launch { snackbar.showSnackbar("导出失败：${it.message}") } }
        }
        pendingExport = null
    }
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: error("无法读取备份文件") }
                .onSuccess { restoreCandidate = it }
                .onFailure { scope.launch { snackbar.showSnackbar("读取失败：${it.message}") } }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            // A new one-shot event owns the single snackbar slot. Cancelling the
            // previous collector must never restart or indefinitely extend its timer.
            snackbar.currentSnackbarData?.dismiss()
            when (event) {
                is UiEvent.Message -> snackbar.showSnackbar(event.text, duration = SnackbarDuration.Short)
                is UiEvent.TransactionSaved -> {
                    feedback.confirm()
                    snackbar.showSnackbar("账单已保存", duration = SnackbarDuration.Short)
                }
                is UiEvent.TransactionDeleted -> {
                    feedback.important()
                    if (
                        snackbar.showSnackbar(
                            message = "账单已删除",
                            actionLabel = "撤销",
                            duration = SnackbarDuration.Short,
                        ) == SnackbarResult.ActionPerformed
                    ) {
                        viewModel.undoDelete(event.transaction)
                    }
                }
                is UiEvent.TransactionRestored -> {
                    feedback.undo()
                    snackbar.showSnackbar("已恢复这笔账单", duration = SnackbarDuration.Short)
                }
                is UiEvent.BackupRestored -> snackbar.showSnackbar(
                    "账本已恢复，恢复前副本已安全保留",
                    duration = SnackbarDuration.Short,
                )
            }
        }
    }

    BackHandler(enabled = !recordPanelVisible && managementPage != null) {
        managementPage = null
    }

    ProvideCategoryArtwork {
        CompositionLocalProvider(LocalHeimaBackdrop provides backdrop) {
            Box(
                Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = buildString {
                            append(if (liquidGlassEnabled) "Liquid Glass 已开启" else "Liquid Glass 已关闭")
                            append("；")
                            append(if (soundEnabled) "操作音效已开启" else "操作音效已关闭")
                            append("；")
                            append(if (hapticEnabled) "触觉反馈已开启" else "触觉反馈已关闭")
                            append("；")
                            append(if (reduceMotion) "减少动态效果已开启" else "减少动态效果已关闭")
                        }
                    },
            ) {
            Box(
                Modifier
                    .fillMaxSize()
                    // Do not run a full-screen RenderEffect while the sheet moves.
                    // The dim layer and reduced contrast create modal depth without
                    // forcing every background pixel through another blur pass.
                    .graphicsLayer { alpha = contentAlpha },
            ) {
            AmbientBackdrop(Modifier.fillMaxSize().then(backdropRecorder))
            Box(Modifier.fillMaxSize()) {
                when {
                    ledgerState.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    !ledgerState.integrityOkay -> IntegrityError(ledgerState.errorMessage)
                    managementPage == ManagementPage.RECORDS -> RecordsScreen(
                        ledgerState.snapshot, amountsVisible, { managementPage = null },
                        { editing = it; recordPanelVisible = true }, { viewModel.deleteTransaction(it.id) },
                    )
                    managementPage == ManagementPage.CATEGORIES -> CategoriesScreen(
                        ledgerState.snapshot, { managementPage = null },
                        { id, type, name, parent -> viewModel.saveCustomCategory(id, type, name, parent) },
                        viewModel::deleteCustomCategory,
                    )
                    managementPage == ManagementPage.DATA -> DataScreen(
                        { managementPage = null },
                        {
                            scope.launch {
                                pendingExport = viewModel.exportCsv()
                                createDocument.launch("黑马记账-${LocalDate.now()}.csv")
                            }
                        },
                        {
                            scope.launch {
                                pendingExport = viewModel.exportBackup()
                                createDocument.launch("黑马记账-${LocalDate.now()}.heima-backup.json")
                            }
                        },
                        { openDocument.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    )
                    else -> HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 0,
                        userScrollEnabled = !recordPanelVisible,
                        key = { PagerDestinations[it] },
                    ) { page ->
                        val screen = PagerDestinations[page]
                        when (screen) {
                            AppDestination.HOME -> HomeScreen(
                                ledgerState.snapshot, amountsVisible, onAmountsVisibleChange,
                                { recordPanelVisible = true }, { managementPage = ManagementPage.RECORDS },
                                { editing = it.let { id -> ledgerState.snapshot.transactions.firstOrNull { transaction -> transaction.id == id } }; recordPanelVisible = editing != null },
                            )
                            AppDestination.STATISTICS -> StatisticsScreen(
                                ledgerState.snapshot,
                                amountsVisible,
                                viewModel::loadStatistics,
                                feedback::selection,
                            )
                            AppDestination.BUDGET -> BudgetScreen(ledgerState.snapshot, amountsVisible, viewModel::saveBudget)
                            AppDestination.PROFILE -> ProfileScreen(
                                ledgerState.snapshot.transactions.size, themeStyle, colorMode, visualQuality, reduceMotion, powerSaveMode,
                                liquidGlassEnabled, soundEnabled, hapticEnabled,
                                onThemeStyleChange, onColorModeChange, onVisualQualityChange, onReduceMotionChange,
                                onLiquidGlassEnabledChange, onSoundEnabledChange, onHapticEnabledChange,
                                { managementPage = ManagementPage.CATEGORIES }, { managementPage = ManagementPage.RECORDS }, { managementPage = ManagementPage.DATA },
                            )
                            AppDestination.RECORD -> error("记账是主操作，不是 Pager 页面")
                        }
                    }
                }
            }

            if (managementPage == null && ledgerState.integrityOkay && !ledgerState.loading && !recordPanelVisible) {
                HeimaBottomBar(
                    destination,
                    { target ->
                        val page = PagerDestinations.indexOf(target)
                        if (page >= 0) {
                            feedback.selection()
                            scope.launch {
                                if (motion.reduceMotion) pagerState.scrollToPage(page)
                                else pagerState.animateScrollToPage(page)
                            }
                        }
                    },
                    { feedback.selection(); editing = null; recordPanelVisible = true },
                    recordPanelVisible,
                    backdrop,
                    navigationProgress,
                    feedback::selection,
                    Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 7.dp),
                )
            }
            }
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 18.dp).padding(bottom = if (managementPage == null) 92.dp else 14.dp),
            ) { data ->
                GlassSurface(Modifier.fillMaxWidth(), 18.dp, 8.dp, backdropBlur = false) {
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(data.visuals.message, color = HeimaTheme.palette.textPrimary, modifier = Modifier.weight(1f))
                        data.visuals.actionLabel?.let { label ->
                            Text(label, color = HeimaTheme.palette.brand, modifier = Modifier.clickable { data.performAction() }.padding(6.dp))
                        }
                    }
                }
            }
                if (recordPanelVisible) {
                    RecordSheet(
                        ledgerState.snapshot,
                        editing,
                        { recordPanelVisible = false; editing = null },
                        { transaction -> viewModel.saveTransaction(transaction); recordPanelVisible = false; editing = null },
                        { managementPage = ManagementPage.CATEGORIES; recordPanelVisible = false },
                        feedback::selection,
                    )
                }
            }
        }
    }

    restoreCandidate?.let { content ->
        GlassConfirmDialog(
            title = "恢复完整账本？",
            message = "恢复会替换现有账单、分类和预算。开始前会自动保存当前账本副本，校验失败则不会修改任何数据。",
            confirmText = "确认恢复",
            onDismiss = { restoreCandidate = null },
            onConfirm = { restoreCandidate = null; viewModel.restoreBackup(content) },
            destructive = true,
        )
    }
}

@Composable
private fun IntegrityError(message: String?) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("本地账本暂时无法打开", style = MaterialTheme.typography.headlineSmall)
            Text(message ?: "原数据文件已保留，应用没有自动删除或重建它。", Modifier.padding(top = 10.dp))
        }
    }
}
