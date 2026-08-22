package com.heima.accounting.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.PressableGlassSurface
import com.heima.accounting.domain.Category
import com.heima.accounting.domain.EntryType
import com.heima.accounting.domain.FinanceRules
import com.heima.accounting.domain.LedgerSnapshot
import com.heima.accounting.domain.Transaction
import com.heima.accounting.ui.CategoryArtwork
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
) {
    val palette = HeimaTheme.palette
    val motion = HeimaTheme.motion
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val context = LocalContext.current
    val initialType = editing?.type ?: EntryType.EXPENSE
    var type by remember(editing?.id) { mutableStateOf(initialType) }
    var amountInput by remember(editing?.id) {
        mutableStateOf(editing?.amountCents?.let { centsToInput(it) }.orEmpty())
    }
    var primaryId by remember(editing?.id) { mutableStateOf(editing?.categoryId) }
    var secondaryId by remember(editing?.id) { mutableStateOf(editing?.subcategoryId) }
    var note by remember(editing?.id) { mutableStateOf(editing?.note.orEmpty()) }
    var date by remember(editing?.id) {
        mutableStateOf(
            editing?.occurredAtEpochMillis?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            } ?: LocalDate.now(),
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dismissing by remember { mutableStateOf(false) }
    val entranceOffset = remember { Animatable(with(density) { 32.dp.toPx() }) }
    val scrimAlpha = remember { Animatable(0f) }
    val handleInteraction = remember { MutableInteractionSource() }
    val dragging by handleInteraction.collectIsDraggedAsState()
    val closeThreshold = with(density) { 112.dp.toPx() }
    val primaryCategories = snapshot.topLevelCategories(type)
    val selectedPrimary = snapshot.category(primaryId)
    val secondaryCategories = primaryId?.let(snapshot::childCategories).orEmpty()
    val targetScrimAlpha = if (motion.darkTheme) 0.46f else 0.25f
    val dismissDistance = with(density) { 440.dp.toPx() }

    LaunchedEffect(Unit) {
        if (motion.reduceMotion) {
            entranceOffset.snapTo(0f)
            scrimAlpha.snapTo(targetScrimAlpha)
        } else {
            launch { scrimAlpha.animateTo(targetScrimAlpha, tween(90)) }
            // A short eased lift responds faster than a long decorative spring on a
            // dense form. The drag handle still uses spring restore after a partial pull.
            entranceOffset.animateTo(0f, tween(90, easing = FastOutSlowInEasing))
        }
    }

    fun dismissAnimated() {
        if (dismissing) return
        dismissing = true
        scope.launch {
            if (motion.reduceMotion) {
                onDismiss()
            } else {
                val scrimJob = launch { scrimAlpha.animateTo(0f, tween(95)) }
                animate(
                    initialValue = dragOffset,
                    targetValue = dismissDistance,
                    animationSpec = tween(120, easing = FastOutSlowInEasing),
                ) { value, _ -> dragOffset = value }
                scrimJob.join()
                onDismiss()
            }
        }
    }

    BackHandler(onBack = ::dismissAnimated)

    fun chooseType(newType: EntryType) {
        if (newType == type) return
        type = newType
        primaryId = snapshot.topLevelCategories(newType).firstOrNull()?.id
        secondaryId = null
        error = null
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrimAlpha.value }
                .background(Color.Black.copy(alpha = targetScrimAlpha))
                .clickable(enabled = !dismissing, onClick = ::dismissAnimated),
        )
        GlassSurface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = 760.dp)
                .graphicsLayer {
                    translationY = entranceOffset.value + dragOffset
                }
                .navigationBarsPadding(),
            cornerRadius = 34.dp,
            elevation = 16.dp,
            // A full-screen live backdrop was re-rendered for every drag frame and
            // caused severe jank on mid-range GPUs. The sheet keeps the same glass
            // tint, rim, highlight and translucency while the smaller controls retain
            // the optically expensive lens treatment.
            backdropBlur = false,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        palette.surface.copy(
                            alpha = if (HeimaTheme.motion.darkTheme) 0.84f else 0.46f,
                        ),
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, top = 9.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .width(if (dragging) 58.dp else 42.dp)
                        .height(22.dp)
                        .draggable(
                            state = rememberDraggableState { delta -> dragOffset = (dragOffset + delta).coerceAtLeast(0f) },
                            orientation = Orientation.Vertical,
                            interactionSource = handleInteraction,
                            onDragStopped = { velocity ->
                                if (dragOffset > closeThreshold || velocity > 1_450f) {
                                    dismissAnimated()
                                } else {
                                    scope.launch {
                                        animate(dragOffset, 0f, animationSpec = spring(dampingRatio = 0.84f, stiffness = 520f)) { value, _ ->
                                            dragOffset = value
                                        }
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
                            .alpha(if (dragging) 0.75f else 0.48f)
                            .background(palette.textTertiary, RoundedCornerShape(3.dp)),
                    )
                }
                Spacer(Modifier.height(7.dp))
                IncomeExpenseSwitch(type, ::chooseType)
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
                    Column {
                        Text(if (type == EntryType.EXPENSE) "支出分类" else "收入分类", color = palette.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("选一级可直接保存，也可以继续细分", color = palette.textTertiary, style = MaterialTheme.typography.labelMedium)
                    }
                    Text("添加分类", color = palette.brand, modifier = Modifier.clickable { onAddCategory(type) }.padding(8.dp))
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(primaryCategories, key = Category::id) { category ->
                        CategoryChoice(category, category.id == primaryId) {
                            primaryId = category.id
                            secondaryId = null
                            error = null
                        }
                    }
                }

                AnimatedVisibility(visible = selectedPrimary != null && secondaryCategories.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${selectedPrimary?.name.orEmpty()}细分", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                            Text("可不选", color = palette.textTertiary, style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.height(7.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            secondaryCategories.forEach { category ->
                                DetailChip(category.name, secondaryId == category.id) {
                                    secondaryId = if (secondaryId == category.id) null else category.id
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    DetailChip(date.format(DateTimeFormatter.ofPattern("M月d日 E", Locale.SIMPLIFIED_CHINESE)), true, Modifier.weight(1f)) {
                        DatePickerDialog(
                            context,
                            { _, year, month, day -> date = LocalDate.of(year, month + 1, day) },
                            date.year,
                            date.monthValue - 1,
                            date.dayOfMonth,
                        ).show()
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { if (it.length <= 200) note = it },
                        modifier = Modifier.weight(1.7f).height(54.dp),
                        placeholder = { Text("备注（可选）") },
                        singleLine = true,
                    )
                }
                error?.let { Text(it, color = palette.expense, style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth().padding(top = 5.dp)) }
                Spacer(Modifier.height(9.dp))
                NumericPad(
                    onKey = { amountInput = appendAmountInput(amountInput, it); error = null },
                    onDelete = { amountInput = amountInput.dropLast(1); error = null },
                    onSave = {
                        val cents = amountInputToCents(amountInput)
                        val category = primaryId
                        when {
                            cents == null || cents <= 0L -> error = "请输入大于 0 的金额"
                            category == null -> error = "请选择一个分类"
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
}

@Composable
private fun IncomeExpenseSwitch(type: EntryType, onChange: (EntryType) -> Unit) {
    val palette = HeimaTheme.palette
    BoxWithConstraints(Modifier.widthIn(max = 330.dp).fillMaxWidth().height(48.dp).background(palette.surfaceMuted.copy(alpha = .62f), RoundedCornerShape(19.dp))) {
        val thumbWidth = (maxWidth - 8.dp) / 2
        val x by animateDpAsState(if (type == EntryType.EXPENSE) 4.dp else thumbWidth + 4.dp, tween(180, easing = FastOutSlowInEasing), label = "entry_type")
        GlassSurface(Modifier.offset { IntOffset(x.roundToPx(), 4.dp.roundToPx()) }.width(thumbWidth).height(40.dp), 16.dp, 7.dp, false) {
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(if (type == EntryType.EXPENSE) listOf(palette.accent, palette.brand) else listOf(palette.income, palette.accent)), RoundedCornerShape(16.dp)))
        }
        Row(Modifier.matchParentSize()) {
            listOf(EntryType.EXPENSE to "支出", EntryType.INCOME to "收入").forEach { (value, label) ->
                val selected = type == value
                val color by animateColorAsState(if (selected) Color.White else palette.textSecondary, label = "entry_type_text")
                Box(Modifier.weight(1f).fillMaxSize().semantics { contentDescription = "切换到$label" }.clickable(role = Role.Tab) { onChange(value) }, contentAlignment = Alignment.Center) {
                    Text(label, color = color, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CategoryChoice(category: Category, selected: Boolean, onClick: () -> Unit) {
    val palette = HeimaTheme.palette
    Column(Modifier.width(70.dp).semantics { contentDescription = "选择${category.name}分类" }.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(58.dp).shadow(if (selected) 11.dp else 4.dp, CircleShape).background(
                Brush.radialGradient(listOf(Color.White.copy(.98f), if (selected) palette.brandSoft else palette.surfaceMuted)),
                CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) { CategoryArtwork(category.iconKey, Modifier.size(51.dp)) }
        Spacer(Modifier.height(4.dp))
        Text(category.name, color = if (selected) palette.brand else palette.textSecondary, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

@Composable
private fun DetailChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = HeimaTheme.palette
    Box(
        modifier.background(if (selected) palette.brandSoft else palette.surfaceMuted.copy(alpha = .68f), RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (selected) palette.brand else palette.textSecondary, style = MaterialTheme.typography.labelMedium) }
}

@Composable
private fun NumericPad(onKey: (String) -> Unit, onDelete: () -> Unit, onSave: () -> Unit) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", "⌫"))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Column(Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key -> KeyButton(key, { if (key == "⌫") onDelete() else onKey(key) }, Modifier.weight(1f)) }
                }
            }
        }
        SaveButton(onSave, Modifier.weight(1f))
    }
}

@Composable
private fun KeyButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = HeimaTheme.palette
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f),
        label = "number_key_press",
    )
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .height(49.dp)
            .semantics { contentDescription = if (text == ".") "输入小数点" else if (text == "⌫") "删除一位金额" else "输入数字$text" }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(palette.glassTop.copy(alpha = .72f), palette.glassBottom.copy(alpha = .56f))))
            .border(1.dp, palette.glassStroke.copy(alpha = .62f), shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = palette.textPrimary, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun SaveButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = HeimaTheme.palette
    PressableGlassSurface(onClick, modifier.height(220.dp), 21.dp, backdropBlur = false) {
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(palette.accent, palette.brand)), RoundedCornerShape(21.dp)), contentAlignment = Alignment.Center) {
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
