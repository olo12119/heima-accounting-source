package com.heima.accounting.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.heima.accounting.designsystem.GlassFieldSurface
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.GlassSegmentedControl
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.PressableGlassSurface
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.heima.accounting.domain.DateRange
import com.heima.accounting.domain.FinanceRules

enum class CustomDateMode { SINGLE, RANGE }

@Composable
fun GlassConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    destructive: Boolean = false,
) {
    HeimaDialogFrame(onDismiss) {
        val palette = HeimaTheme.palette
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, color = palette.textPrimary, style = MaterialTheme.typography.titleLarge)
            Text(message, color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogAction("取消", onDismiss, Modifier.weight(1f))
                DialogAction(confirmText, onConfirm, Modifier.weight(1f), if (destructive) palette.expense else palette.brand)
            }
        }
    }
}

@Composable
fun GlassTextInputDialog(
    title: String,
    initialValue: String,
    placeholder: String,
    confirmText: String = "确定",
    validator: (String) -> String? = { null },
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    var error by remember(initialValue) { mutableStateOf<String?>(null) }
    HeimaDialogFrame(onDismiss) {
        val palette = HeimaTheme.palette
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, color = palette.textPrimary, style = MaterialTheme.typography.titleLarge)
            GlassFieldSurface(Modifier.fillMaxWidth()) {
                BasicTextField(
                    value = value,
                    onValueChange = { value = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
                    decorationBox = { input ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isBlank()) Text(placeholder, color = palette.textTertiary)
                            input()
                        }
                    },
                )
            }
            error?.let { Text(it, color = palette.expense, style = MaterialTheme.typography.labelMedium) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogAction("取消", onDismiss, Modifier.weight(1f))
                DialogAction(
                    confirmText,
                    {
                        val validation = validator(value)
                        if (validation == null) onConfirm(value) else error = validation
                    },
                    Modifier.weight(1f),
                    palette.brand,
                )
            }
        }
    }
}

@Composable
fun LiquidGlassDatePicker(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    onSelectionFeedback: () -> Unit = {},
) {
    val palette = HeimaTheme.palette
    val motion = HeimaTheme.motion
    var selectedDate by remember(initialDate) { mutableStateOf(initialDate) }
    var shownMonth by remember(initialDate) { mutableStateOf(YearMonth.from(initialDate)) }
    var direction by remember { mutableIntStateOf(1) }
    val chineseFull = remember { DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.SIMPLIFIED_CHINESE) }

    HeimaDialogFrame(onDismiss, modifier = Modifier.widthIn(max = 390.dp)) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
            Text("选择日期", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                selectedDate.format(chineseFull),
                color = palette.textPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                CalendarArrow("上一个月", "‹") { direction = -1; shownMonth = shownMonth.minusMonths(1) }
                Text("${shownMonth.year}年${shownMonth.monthValue}月", color = palette.textPrimary, style = MaterialTheme.typography.titleMedium)
                CalendarArrow("下一个月", "›") { direction = 1; shownMonth = shownMonth.plusMonths(1) }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                    Text(label, Modifier.weight(1f), color = palette.textTertiary, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(7.dp))
            AnimatedContent(
                targetState = shownMonth,
                transitionSpec = {
                    if (motion.reduceMotion) {
                        fadeIn() togetherWith fadeOut()
                    } else if (direction > 0) {
                        (slideInHorizontally { it / 5 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 5 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 5 } + fadeIn()) togetherWith (slideOutHorizontally { it / 5 } + fadeOut())
                    }
                },
                label = "calendar_month",
            ) { month ->
                CalendarMonthGrid(
                    month = month,
                    selectedStart = selectedDate,
                    selectedEnd = null,
                    today = LocalDate.now(),
                    onSelected = {
                        onSelectionFeedback()
                        selectedDate = it
                    },
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogAction("取消", onDismiss, Modifier.weight(1f))
                DialogAction("确定", { onConfirm(selectedDate) }, Modifier.weight(1f), palette.brand)
            }
        }
    }
}

@Composable
fun LiquidGlassDateRangePicker(
    initialRange: DateRange,
    onDismiss: () -> Unit,
    onConfirm: (DateRange) -> Unit,
    onSelectionFeedback: () -> Unit = {},
) {
    val palette = HeimaTheme.palette
    val motion = HeimaTheme.motion
    val today = remember { LocalDate.now() }
    val safeInitialRange = remember(initialRange, today) {
        val safeEnd = minOf(initialRange.endInclusive, today)
        val safeStart = minOf(initialRange.startInclusive, safeEnd)
        DateRange(safeStart, safeEnd)
    }
    var mode by remember(safeInitialRange) {
        mutableStateOf(if (safeInitialRange.startInclusive == safeInitialRange.endInclusive) CustomDateMode.SINGLE else CustomDateMode.RANGE)
    }
    var startDate by remember(safeInitialRange) { mutableStateOf(safeInitialRange.startInclusive) }
    var endDate by remember(safeInitialRange) { mutableStateOf(safeInitialRange.endInclusive) }
    var selectingEnd by remember(safeInitialRange) { mutableStateOf(false) }
    var shownMonth by remember(safeInitialRange) { mutableStateOf(YearMonth.from(safeInitialRange.startInclusive)) }
    var direction by remember { mutableIntStateOf(1) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.SIMPLIFIED_CHINESE) }
    val selectedLabel = if (mode == CustomDateMode.SINGLE) {
        startDate.format(dateFormatter)
    } else {
        "${startDate.format(dateFormatter)}  至  ${endDate.format(dateFormatter)}"
    }

    HeimaDialogFrame(onDismiss, modifier = Modifier.widthIn(max = 410.dp)) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
            Text("自定义统计日期", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            GlassSegmentedControl(
                options = listOf(CustomDateMode.SINGLE to "单日", CustomDateMode.RANGE to "日期区间"),
                selected = mode,
                onSelected = { selectedMode ->
                    mode = selectedMode
                    selectingEnd = false
                    if (selectedMode == CustomDateMode.SINGLE) endDate = startDate
                },
                accessibilityLabel = "自定义日期模式",
                height = 44.dp,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                selectedLabel,
                color = palette.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (mode == CustomDateMode.RANGE) {
                Text(
                    if (selectingEnd) "请选择结束日期" else "点击一个日期开始重新选择区间",
                    color = palette.textMuted,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                CalendarArrow("上一个月", "‹") { direction = -1; shownMonth = shownMonth.minusMonths(1) }
                Text("${shownMonth.year}年${shownMonth.monthValue}月", color = palette.textPrimary, style = MaterialTheme.typography.titleMedium)
                CalendarArrow(
                    description = "下一个月",
                    glyph = "›",
                    enabled = shownMonth < YearMonth.from(today),
                ) { direction = 1; shownMonth = shownMonth.plusMonths(1) }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                    Text(label, Modifier.weight(1f), color = palette.textMuted, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(7.dp))
            AnimatedContent(
                targetState = shownMonth,
                transitionSpec = {
                    if (motion.reduceMotion) {
                        fadeIn() togetherWith fadeOut()
                    } else if (direction > 0) {
                        (slideInHorizontally { it / 5 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 5 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 5 } + fadeIn()) togetherWith (slideOutHorizontally { it / 5 } + fadeOut())
                    }
                },
                label = "statistics_calendar_month",
            ) { month ->
                CalendarMonthGrid(
                    month = month,
                    selectedStart = startDate,
                    selectedEnd = if (mode == CustomDateMode.RANGE) endDate else null,
                    today = today,
                    maxSelectableDate = today,
                    onSelected = { date ->
                        onSelectionFeedback()
                        if (mode == CustomDateMode.SINGLE) {
                            startDate = date
                            endDate = date
                        } else if (!selectingEnd) {
                            startDate = date
                            endDate = date
                            selectingEnd = true
                        } else {
                            if (date.isBefore(startDate)) {
                                endDate = startDate
                                startDate = date
                            } else {
                                endDate = date
                            }
                            selectingEnd = false
                        }
                    },
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogAction("取消", onDismiss, Modifier.weight(1f))
                DialogAction(
                    "确定",
                    {
                        val first = if (mode == CustomDateMode.SINGLE) startDate else minOf(startDate, endDate)
                        val last = if (mode == CustomDateMode.SINGLE) startDate else maxOf(startDate, endDate)
                        FinanceRules.historicalRangeOrNull(first, last, today)?.let(onConfirm)
                    },
                    Modifier.weight(1f),
                    palette.brand,
                )
            }
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    month: YearMonth,
    selectedStart: LocalDate,
    selectedEnd: LocalDate?,
    today: LocalDate,
    maxSelectableDate: LocalDate? = null,
    onSelected: (LocalDate) -> Unit,
) {
    val offset = month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value
    val days = buildList<LocalDate?> {
        repeat(offset) { add(null) }
        for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
        while (size < 42) add(null)
    }
    Column {
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).height(42.dp), contentAlignment = Alignment.Center) {
                        if (date != null) {
                            val selected = date == selectedStart || date == selectedEnd
                            val inRange = selectedEnd != null && !date.isBefore(selectedStart) && !date.isAfter(selectedEnd)
                            CalendarDay(
                                date = date,
                                selected = selected,
                                inRange = inRange,
                                today = date == today,
                                enabled = maxSelectableDate == null || !date.isAfter(maxSelectableDate),
                            ) { onSelected(date) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    selected: Boolean,
    inRange: Boolean,
    today: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val palette = HeimaTheme.palette
    val motion = HeimaTheme.motion
    val shape = CircleShape
    Box(
        Modifier
            .size(36.dp)
            .semantics {
                contentDescription = "${date.monthValue}月${date.dayOfMonth}日${if (today) "，今天" else ""}${if (selected) "，已选择" else ""}"
                if (!enabled) {
                    disabled()
                    stateDescription = "未来日期不可选择"
                }
            }
            .clip(shape)
            .background(
                when {
                    selected && motion.liquidGlassEnabled -> Brush.radialGradient(listOf(palette.accent.copy(.86f), palette.brand.copy(.94f)))
                    selected -> Brush.radialGradient(listOf(palette.brand, palette.brand))
                    inRange -> Brush.radialGradient(listOf(palette.brandSoft.copy(.72f), palette.brandSoft.copy(.42f)))
                    else -> Brush.radialGradient(listOf(Color.Transparent, Color.Transparent))
                },
            )
            .then(if (today && !selected) Modifier.border(1.dp, palette.brand.copy(.68f), shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            color = when {
                !enabled -> palette.textMuted.copy(alpha = .38f)
                selected -> Color.White
                else -> palette.textPrimary
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected || today) FontWeight.SemiBold else FontWeight.Normal,
        )
        AnimatedVisibility(today && !selected, Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.padding(bottom = 3.dp).size(3.dp).background(palette.brand, CircleShape))
        }
    }
}

@Composable
private fun CalendarArrow(
    description: String,
    glyph: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val palette = HeimaTheme.palette
    Box(
        Modifier
            .size(40.dp)
            .semantics {
                contentDescription = description
                if (!enabled) disabled()
            }
            .clip(CircleShape)
            .background(palette.surfaceMuted.copy(.64f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            color = if (enabled) palette.textPrimary else palette.textMuted.copy(alpha = .36f),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@Composable
internal fun HeimaDialogFrame(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (HeimaTheme.motion.darkTheme) .48f else .24f))
                .clickable(onClick = onDismiss)
                .padding(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            GlassSurface(
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .clickable(onClick = {}),
                cornerRadius = 28.dp,
                elevation = 18.dp,
                backdropBlur = false,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            HeimaTheme.palette.surface.copy(
                                alpha = if (HeimaTheme.motion.darkTheme) .97f else .96f,
                            ),
                        ),
                ) { content() }
            }
        }
    }
}

@Composable
private fun DialogAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
) {
    val palette = HeimaTheme.palette
    PressableGlassSurface(
        onClick,
        modifier.height(44.dp).semantics { contentDescription = "对话框操作：$label" },
        15.dp,
        backdropBlur = false,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, color = accent ?: palette.textSecondary, fontWeight = FontWeight.SemiBold)
        }
    }
}
