package com.heima.accounting.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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

private enum class ManagementPage { RECORDS, CATEGORIES, DATA }

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
    var destination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
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
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Message -> snackbar.showSnackbar(event.text)
                is UiEvent.TransactionSaved -> {
                    feedback.confirm()
                    snackbar.showSnackbar(if (event.transaction.id == 0L) "账单已保存" else "账单已保存")
                }
                is UiEvent.TransactionDeleted -> {
                    feedback.important()
                    if (snackbar.showSnackbar("账单已删除", "撤销") == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.transaction)
                }
                is UiEvent.BackupRestored -> snackbar.showSnackbar("账本已恢复，恢复前副本已安全保留")
            }
        }
    }

    BackHandler(enabled = !recordPanelVisible && managementPage != null) {
        managementPage = null
    }

    ProvideCategoryArtwork {
        CompositionLocalProvider(LocalHeimaBackdrop provides backdrop) {
            Box(Modifier.fillMaxSize()) {
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
                    else -> DestinationPage(destination, motion.reduceMotion) { screen ->
                        when (screen) {
                            AppDestination.HOME -> HomeScreen(
                                ledgerState.snapshot, amountsVisible, onAmountsVisibleChange,
                                { recordPanelVisible = true }, { managementPage = ManagementPage.RECORDS },
                                { editing = it.let { id -> ledgerState.snapshot.transactions.firstOrNull { transaction -> transaction.id == id } }; recordPanelVisible = editing != null },
                            )
                            AppDestination.STATISTICS -> StatisticsScreen(ledgerState.snapshot, amountsVisible)
                            AppDestination.BUDGET -> BudgetScreen(ledgerState.snapshot, amountsVisible, viewModel::saveBudget)
                            AppDestination.PROFILE -> ProfileScreen(
                                ledgerState.snapshot.transactions.size, themeStyle, colorMode, visualQuality, reduceMotion, powerSaveMode,
                                liquidGlassEnabled, soundEnabled, hapticEnabled,
                                onThemeStyleChange, onColorModeChange, onVisualQualityChange, onReduceMotionChange,
                                onLiquidGlassEnabledChange, onSoundEnabledChange, onHapticEnabledChange,
                                { managementPage = ManagementPage.CATEGORIES }, { managementPage = ManagementPage.RECORDS }, { managementPage = ManagementPage.DATA },
                            )
                            AppDestination.RECORD -> Unit
                        }
                    }
                }
            }

            if (managementPage == null && ledgerState.integrityOkay && !ledgerState.loading && !recordPanelVisible) {
                HeimaBottomBar(
                    destination,
                    { if (it != AppDestination.RECORD) destination = it },
                    { editing = null; recordPanelVisible = true },
                    recordPanelVisible,
                    backdrop,
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

@Composable
private fun DestinationPage(destination: AppDestination, reduceMotion: Boolean, content: @Composable (AppDestination) -> Unit) {
    // Only the selected page is composed. Keeping the old and new chart-heavy pages
    // alive at the same time was the main source of rapid-tab jank.
    var previousOrdinal by remember { mutableIntStateOf(destination.ordinal) }
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(destination, reduceMotion) {
        val direction = if (destination.ordinal >= previousOrdinal) 1f else -1f
        previousOrdinal = destination.ordinal
        if (reduceMotion) {
            entrance.snapTo(0f)
        } else {
            entrance.snapTo(direction)
            entrance.animateTo(0f, spring(dampingRatio = 0.92f, stiffness = 820f))
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = entrance.value * 14.dp.toPx()
                alpha = 1f - kotlin.math.abs(entrance.value) * 0.055f
            },
    ) {
        content(destination)
    }
}
