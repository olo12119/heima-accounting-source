package com.heima.accounting.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.GlassChip
import com.heima.accounting.designsystem.GlassFieldSurface
import com.heima.accounting.designsystem.GlassSegmentedControl
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.HeimaMotionTokens
import com.heima.accounting.designsystem.HeimaSurfaceRole
import com.heima.accounting.designsystem.PressableGlassSurface
import com.heima.accounting.domain.Category
import com.heima.accounting.domain.EntryType
import com.heima.accounting.domain.FinanceRules
import com.heima.accounting.domain.LedgerSnapshot
import com.heima.accounting.domain.Transaction
import com.heima.accounting.ui.CategoryIcon
import com.heima.accounting.ui.HeimaDialogFrame
import com.heima.accounting.ui.LiquidGlassDatePicker
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun RecordSheet(
    snapshot: LedgerSnapshot,
    editing: Transaction? = null,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    onAddCategory: (EntryType) -> Unit,
    onSelectionFeedback: () -> Unit = {},
    onVisibilityProgress: (Float) -> Unit = {},
    onAddSubcategory: suspend (parentId: String, type: EntryType, name: String, iconKey: String, colorArgb: Long) -> Category? = { _, _, _, _, _ -> null },
    onErrorFeedback: () -> Unit = {},
) {
    val palette = HeimaTheme.palette
    val motion = HeimaTheme.motion
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var type by remember(editing?.id) { mutableStateOf(editing?.type ?: EntryType.EXPENSE) }
    var amountInput by remember(editing?.id) { mutableStateOf(editing?.amountCents?.let(::centsToInput).orEmpty()) }
    var primaryId by remember(editing?.id) { mutableStateOf(editing?.categoryId) }
    var secondaryId by remember(editing?.id) { mutableStateOf(editing?.subcategoryId) }
    var secondaryExpanded by remember(editing?.id) { mutableStateOf(editing?.subcategoryId != null) }
    var note by remember(editing?.id) { mutableStateOf(editing?.note.orEmpty()) }
    var date by remember(editing?.id) {
        mutableStateOf(
            editing?.occurredAtEpochMillis?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            } ?: LocalDate.now(),
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddSubcategory by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dismissing by remember { mutableStateOf(false) }
    val entranceOffset = remember { Animatable(with(density) { 28.dp.toPx() }) }
    val scrimAlpha = remember { Animatable(0f) }
    val handleInteraction = remember { MutableInteractionSource() }
    val dragging by handleInteraction.collectIsDraggedAsState()
    val closeThreshold = with(density) { 112.dp.toPx() }
    val dismissDistance = with(density) { 440.dp.toPx() }
    val targetScrimAlpha = 1f
    val primaryCategories = snapshot.topLevelCategories(type)
    val selectedPrimary = snapshot.category(primaryId)
    val secondaryCategories = primaryId?.let(snapshot::childCategories).orEmpty()
    val visibilityProgress = (
        scrimAlpha.value * (1f - dragOffset / dismissDistance)
        ).coerceIn(0f, 1f)

    SideEffect { onVisibilityProgress(visibilityProgress) }
    DisposableEffect(Unit) { onDispose { onVisibilityProgress(0f) } }

    LaunchedEffect(Unit) {
        if (motion.reduceMotion) {
            entranceOffset.snapTo(0f)
            scrimAlpha.snapTo(targetScrimAlpha)
        } else {
            launch { scrimAlpha.animateTo(targetScrimAlpha, tween(HeimaMotionTokens.Fast)) }
            entranceOffset.animateTo(0f, tween(HeimaMotionTokens.Fast, easing = FastOutSlowInEasing))
        }
    }

    fun dismissAnimated() {
        if (dismissing) return
        dismissing = true
        scope.launch {
            if (motion.reduceMotion) {
                onDismiss()
            } else {
                val scrimJob = launch { scrimAlpha.animateTo(0f, tween(HeimaMotionTokens.Instant)) }
                animate(dragOffset, dismissDistance, animationSpec = tween(HeimaMotionTokens.Fast, easing = FastOutSlowInEasing)) { value, _ ->
                    dragOffset = value
                }
                scrimJob.join()
                onDismiss()
            }
        }
    }

    fun chooseType(newType: EntryType) {
        if (newType == type) return
        onSelectionFeedback()
        type = newType
        // A type change must never make a classification decision for the user.
        primaryId = null
        secondaryId = null
        secondaryExpanded = false
        error = null
    }

    BackHandler(onBack = ::dismissAnimated)

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrimAlpha.value }
                .background(
                    if (motion.darkTheme) Color.Black.copy(alpha = .58f)
                    else Color.Black.copy(alpha = .20f),
                )
                .clickable(enabled = !dismissing, onClick = ::dismissAnimated),
        )
        GlassSurface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = 760.dp)
                .graphicsLayer { translationY = entranceOffset.value + dragOffset }
                .navigationBarsPadding(),
            cornerRadius = 34.dp,
            elevation = 16.dp,
            // One opaque-enough modal material is cheaper and cleaner than nested
            // live blur layers, especially while the sheet follows the user's finger.
            backdropBlur = false,
            role = HeimaSurfaceRole.OVERLAY,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = .76f + visibilityProgress * .24f }
                    .background(palette.surface.copy(alpha = if (motion.darkTheme) .96f else .94f))
                    .verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, top = 9.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .width(if (dragging) 58.dp else 42.dp)
                        .height(22.dp)
                        .semantics { contentDescription = "向下拖动关闭记账" }
                        .draggable(
                            state = rememberDraggableState { delta -> dragOffset = (dragOffset + delta).coerceAtLeast(0f) },
                            orientation = Orientation.Vertical,
                            interactionSource = handleInteraction,
                            onDragStopped = { velocity ->
                                if (dragOffset > closeThreshold || velocity > 1_450f) {
                                    dismissAnimated()
                                } else {
                                    scope.launch {
                                        animate(
                                            dragOffset,
                                            0f,
                                            animationSpec = HeimaMotionTokens.soft(motion.reduceMotion),
                                        ) { value, _ -> dragOffset = value }
                                    }
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .alpha(if (dragging) .76f else .45f)
                            .background(palette.textTertiary, RoundedCornerShape(3.dp)),
                    )
                }
                Spacer(Modifier.height(6.dp))
                GlassSegmentedControl(
                    options = listOf(EntryType.EXPENSE to "支出", EntryType.INCOME to "收入"),
                    selected = type,
                    onSelected = ::chooseType,
                    modifier = Modifier.widthIn(max = 330.dp),
                    accessibilityLabel = "收支类型",
                )
                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("¥", color = palette.textPrimary, style = MaterialTheme.typography.headlineLarge)
                        Text(formatAmount(amountInput), color = palette.textPrimary, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "清空",
                        color = if (amountInput.isEmpty()) palette.textTertiary else palette.brand,
                        modifier = Modifier.clickable(enabled = amountInput.isNotEmpty()) { amountInput = "" }.padding(9.dp),
                    )
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(if (type == EntryType.EXPENSE) "支出分类" else "收入分类", color = palette.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("选择一级分类即可保存，也可继续细分", color = palette.textTertiary, style = MaterialTheme.typography.labelMedium)
                    }
                    Text(
                        "管理",
                        color = palette.brand,
                        modifier = Modifier
                            .semantics { contentDescription = "管理${if (type == EntryType.EXPENSE) "支出" else "收入"}分类" }
                            .clickable { onAddCategory(type) }
                            .padding(8.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(primaryCategories, key = Category::id) { category ->
                        CategoryChoice(category, category.id == primaryId) {
                            onSelectionFeedback()
                            if (primaryId != category.id) {
                                primaryId = category.id
                                secondaryId = null
                            }
                            // 细分区选中一级分类后常驻展开，末尾固定提供"＋ 添加"入口。
                            secondaryExpanded = true
                            error = null
                        }
                    }
                }

                AnimatedVisibility(
                    visible = secondaryExpanded && selectedPrimary != null,
                    enter = if (motion.reduceMotion) fadeIn(tween(HeimaMotionTokens.Instant)) else expandVertically(tween(HeimaMotionTokens.Standard)) + fadeIn(tween(HeimaMotionTokens.Fast)),
                    exit = if (motion.reduceMotion) fadeOut(tween(HeimaMotionTokens.Instant)) else shrinkVertically(tween(HeimaMotionTokens.Fast)) + fadeOut(tween(HeimaMotionTokens.Instant)),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .semantics { contentDescription = "二级分类区域" },
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${selectedPrimary?.name.orEmpty()}细分", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                            Text("可不选", color = palette.textTertiary, style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.height(7.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            secondaryCategories.forEach { category ->
                                GlassChip(category.name, secondaryId == category.id, {
                                    onSelectionFeedback()
                                    secondaryId = if (secondaryId == category.id) null else category.id
                                })
                            }
                            AddSubcategoryChip { showAddSubcategory = true }
                        }
                    }
                }

                Spacer(Modifier.height(11.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassFieldSurface(
                        modifier = Modifier.weight(.88f).semantics { contentDescription = "选择记账日期" },
                        onClick = { showDatePicker = true },
                    ) {
                        Text(
                            date.format(DateTimeFormatter.ofPattern("M月d日 E", Locale.SIMPLIFIED_CHINESE)),
                            color = palette.brand,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    GlassFieldSurface(Modifier.weight(1.42f)) {
                        BasicTextField(
                            value = note,
                            onValueChange = { if (it.length <= 200) note = it },
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "备注，可选" },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (note.isEmpty()) Text("备注（可选）", color = palette.textTertiary)
                                    inner()
                                }
                            },
                        )
                    }
                }
                error?.let {
                    Text(it, color = palette.expense, style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth().padding(top = 5.dp))
                }
                Spacer(Modifier.height(9.dp))
                NumericPad(
                    onKey = { amountInput = appendAmountInput(amountInput, it); error = null },
                    onDelete = { amountInput = amountInput.dropLast(1); error = null },
                    onSave = {
                        val cents = amountInputToCents(amountInput)
                        val category = primaryId
                        when {
                            cents == null || cents <= 0L -> {
                                onErrorFeedback()
                                error = "请输入大于 0 的金额"
                            }
                            category == null -> {
                                onErrorFeedback()
                                error = "请选择一个一级分类"
                            }
                            else -> {
                                val localTime = editing?.occurredAtEpochMillis?.let {
                                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
                                } ?: LocalTime.now()
                                val occurredAt = LocalDateTime.of(date, localTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                onSave(
                                    Transaction(
                                        id = editing?.id ?: 0L,
                                        type = type,
                                        amountCents = cents,
                                        categoryId = category,
                                        subcategoryId = secondaryId,
                                        note = note.trim(),
                                        occurredAtEpochMillis = occurredAt,
                                        createdAtEpochMillis = editing?.createdAtEpochMillis ?: System.currentTimeMillis(),
                                    ),
                                )
                            }
                        }
                    },
                )
            }
        }
    }

    if (showDatePicker) {
        LiquidGlassDatePicker(
            initialDate = date,
            onDismiss = { showDatePicker = false },
            onSelectionFeedback = onSelectionFeedback,
            onConfirm = {
                date = it
                showDatePicker = false
            },
        )
    }

    if (showAddSubcategory) {
        selectedPrimary?.let { parent ->
            AddSubcategoryDialog(
                parentName = parent.name,
                siblingNames = secondaryCategories.map(Category::name),
                onDismiss = { showAddSubcategory = false },
                onErrorFeedback = onErrorFeedback,
                onConfirm = { name ->
                    showAddSubcategory = false
                    // 保存即选中：等待仓库落库后直接选中新细分，全程不离开记账页。
                    scope.launch {
                        val created = onAddSubcategory(parent.id, type, name, parent.iconKey, parent.colorArgb)
                        if (created != null) {
                            secondaryId = created.id
                            secondaryExpanded = true
                            onSelectionFeedback()
                        }
                    }
                },
            )
        }
    }
}

/** 细分区末尾的常驻入口：样式对齐 GlassChip，品牌色描边使其可辨识。 */
@Composable
private fun AddSubcategoryChip(onClick: () -> Unit) {
    val palette = HeimaTheme.palette
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .semantics { contentDescription = "添加细分" }
            .clip(shape)
            .background(palette.surfaceMuted.copy(alpha = .5f))
            .border(1.dp, palette.brand.copy(alpha = .6f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("＋ 添加", color = palette.brand, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** 记账页内直接新增细分的小弹窗：自动唤起键盘，保存即选中。 */
@Composable
private fun AddSubcategoryDialog(
    parentName: String,
    siblingNames: List<String>,
    onDismiss: () -> Unit,
    onErrorFeedback: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val palette = HeimaTheme.palette
    var value by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    HeimaDialogFrame(onDismiss) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("添加${parentName}细分", color = palette.textPrimary, style = MaterialTheme.typography.titleLarge)
            GlassFieldSurface(Modifier.fillMaxWidth()) {
                BasicTextField(
                    value = value,
                    onValueChange = { value = it.take(20); error = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .semantics { contentDescription = "新细分名称，最多 20 个字" },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) Text("细分名称，最多 20 个字", color = palette.textTertiary)
                            inner()
                        }
                    },
                )
            }
            error?.let { Text(it, color = palette.expense, style = MaterialTheme.typography.labelMedium) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SubcategoryDialogAction("取消", onDismiss, Modifier.weight(1f))
                SubcategoryDialogAction(
                    label = "保存",
                    onClick = {
                        val validation = FinanceRules.validateCategoryName(value, siblingNames)
                        if (validation != null) {
                            onErrorFeedback()
                            error = validation
                        } else {
                            keyboard?.hide()
                            onConfirm(value.trim())
                        }
                    },
                    modifier = Modifier.weight(1f),
                    accent = palette.brand,
                    enabled = value.isNotBlank(),
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
}

@Composable
private fun SubcategoryDialogAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    enabled: Boolean = true,
) {
    val palette = HeimaTheme.palette
    PressableGlassSurface(
        onClick = { if (enabled) onClick() },
        modifier = modifier
            .height(44.dp)
            .alpha(if (enabled) 1f else .45f)
            .semantics { contentDescription = "对话框操作：$label" },
        cornerRadius = 15.dp,
        backdropBlur = false,
        role = HeimaSurfaceRole.INTERACTIVE,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, color = accent ?: palette.textSecondary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CategoryChoice(category: Category, selected: Boolean, onClick: () -> Unit) {
    val palette = HeimaTheme.palette
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .width(70.dp)
            .semantics { contentDescription = "选择${category.name}分类${if (selected) "，已选择" else ""}" }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CategoryIcon(category.iconKey, selected, size = 60.dp)
        Spacer(Modifier.height(4.dp))
        val color by animateColorAsState(if (selected) palette.brand else palette.textSecondary, label = "category_label")
        Text(
            category.name,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
private fun NumericPad(onKey: (String) -> Unit, onDelete: () -> Unit, onSave: () -> Unit) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", "⌫"))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Column(Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        KeyButton(key, { if (key == "⌫") onDelete() else onKey(key) }, Modifier.weight(1f))
                    }
                }
            }
        }
        SaveButton(onSave, Modifier.weight(1f))
    }
}

@Composable
private fun KeyButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = HeimaTheme.palette
    val motion = HeimaTheme.motion
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !motion.reduceMotion) .975f else 1f,
        animationSpec = HeimaMotionTokens.responsive(motion.reduceMotion),
        label = "number_key_press",
    )
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .height(49.dp)
            .semantics {
                contentDescription = when (text) {
                    "." -> "输入小数点"
                    "⌫" -> "删除一位金额"
                    else -> "输入数字$text"
                }
            }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(palette.glassTop.copy(.74f), palette.glassBottom.copy(.58f))))
            .border(1.dp, palette.glassStroke.copy(.58f), shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = palette.textPrimary, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun SaveButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = HeimaTheme.palette
    PressableGlassSurface(onClick, modifier.height(220.dp), 21.dp, backdropBlur = false, role = HeimaSurfaceRole.INTERACTIVE) {
        Box(
            Modifier.matchParentSize().background(Brush.verticalGradient(listOf(palette.accent, palette.brand)), RoundedCornerShape(21.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("保存", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

internal fun appendAmountInput(current: String, key: String, maxIntegerDigits: Int = 9): String =
    FinanceRules.appendAmount(current, key, maxIntegerDigits)

internal fun formatAmount(input: String): String = input.ifEmpty { "0" }

internal fun amountInputToCents(input: String): Long? = FinanceRules.parseYuanToCents(input)

private fun centsToInput(cents: Long): String = buildString {
    append(cents / 100)
    val fraction = cents % 100
    if (fraction != 0L) append('.').append(fraction.toString().padStart(2, '0').trimEnd('0'))
}
