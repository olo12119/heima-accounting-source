package com.heima.accounting.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.PressableGlassSurface

private enum class CategoryGlyph {
    MEAL,
    TRANSPORT,
    SHOPPING,
    HOME,
    ENTERTAINMENT,
    SALARY,
    BONUS,
    SIDE_JOB,
    INVESTMENT,
    GIFT,
    ADD,
}

private data class QuickCategory(
    val glyph: CategoryGlyph,
    val name: String,
)

private val expenseCategories = listOf(
    QuickCategory(CategoryGlyph.MEAL, "餐饮"),
    QuickCategory(CategoryGlyph.TRANSPORT, "交通"),
    QuickCategory(CategoryGlyph.SHOPPING, "购物"),
    QuickCategory(CategoryGlyph.HOME, "居住"),
    QuickCategory(CategoryGlyph.ENTERTAINMENT, "娱乐"),
    QuickCategory(CategoryGlyph.ADD, "添加"),
)

private val incomeCategories = listOf(
    QuickCategory(CategoryGlyph.SALARY, "工资"),
    QuickCategory(CategoryGlyph.BONUS, "奖金"),
    QuickCategory(CategoryGlyph.SIDE_JOB, "副业"),
    QuickCategory(CategoryGlyph.INVESTMENT, "理财"),
    QuickCategory(CategoryGlyph.GIFT, "红包"),
    QuickCategory(CategoryGlyph.ADD, "添加"),
)

@Composable
fun RecordSheet(
    onDismiss: () -> Unit,
    onVisualSave: (String) -> Unit,
) {
    val palette = HeimaTheme.palette
    val reduceMotion = HeimaTheme.motion.reduceMotion
    val haptic = LocalHapticFeedback.current
    var isExpense by remember { mutableStateOf(true) }
    var amountInput by remember { mutableStateOf("") }
    var categoryIndex by remember { mutableIntStateOf(0) }
    val sheetInteraction = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.20f),
                            palette.brand.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.34f),
                        ),
                    ),
                )
                .clickable(onClick = onDismiss),
        )

        GlassSurface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .navigationBarsPadding()
                .clickable(
                    interactionSource = sheetInteraction,
                    indication = null,
                    onClick = {},
                ),
            cornerRadius = 34.dp,
            elevation = 28.dp,
            backdropBlur = true,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, top = 11.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(width = 42.dp, height = 5.dp)
                        .background(
                            palette.textTertiary.copy(alpha = 0.42f),
                            RoundedCornerShape(3.dp),
                        ),
                )
                Spacer(Modifier.height(14.dp))

                IncomeExpenseSwitch(
                    isExpense = isExpense,
                    onChange = { expense ->
                        if (expense != isExpense) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isExpense = expense
                            categoryIndex = 0
                        }
                    },
                )
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "¥",
                            color = palette.textPrimary,
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Text(
                            text = formatAmount(amountInput),
                            color = palette.textPrimary,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = "清空",
                        color = if (amountInput.isEmpty()) palette.textTertiary else palette.brand,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = amountInput.isNotEmpty()) { amountInput = "" }
                            .padding(horizontal = 11.dp, vertical = 8.dp),
                    )
                }

                Spacer(Modifier.height(11.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "常用一级分类",
                            color = palette.textPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "选择后再进入二级分类",
                            color = palette.textTertiary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Text(
                        text = if (isExpense) "支出分类" else "收入分类",
                        color = palette.brand,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Spacer(Modifier.height(8.dp))

                AnimatedContent(
                    targetState = isExpense,
                    transitionSpec = {
                        if (reduceMotion) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            val direction = if (targetState) -1 else 1
                            (slideInHorizontally { direction * it / 3 } + fadeIn()) togetherWith
                                (slideOutHorizontally { -direction * it / 3 } + fadeOut())
                        }
                    },
                    label = "category_type_change",
                    modifier = Modifier.fillMaxWidth(),
                ) { expense ->
                    val categories = if (expense) expenseCategories else incomeCategories
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        itemsIndexed(categories) { index, category ->
                            CategoryOrb(
                                category = category,
                                selected = categoryIndex == index,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    categoryIndex = index
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(13.dp))
                NumericPad(
                    onKey = { key ->
                        amountInput = appendAmountInput(amountInput, key)
                    },
                    onDelete = { amountInput = amountInput.dropLast(1) },
                    onSave = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val cents = amountInputToCents(amountInput)
                        if (cents != null && cents > 0L) {
                            onVisualSave("记账动效和金额输入正常；正式数据保存将在下一阶段接入")
                        } else {
                            onVisualSave("请输入大于 0 的金额")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun IncomeExpenseSwitch(
    isExpense: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val palette = HeimaTheme.palette
    val motion = HeimaTheme.motion
    BoxWithConstraints(
        modifier = Modifier
            .widthIn(max = 318.dp)
            .fillMaxWidth()
            .height(48.dp)
            .background(palette.surfaceMuted.copy(alpha = 0.68f), RoundedCornerShape(19.dp)),
    ) {
        val thumbWidth = (maxWidth - 8.dp) / 2
        val targetX = if (isExpense) 4.dp else 4.dp + thumbWidth
        val thumbX by animateDpAsState(
            targetValue = targetX,
            animationSpec = if (motion.reduceMotion) {
                tween(durationMillis = 1)
            } else {
                tween(durationMillis = 170, easing = FastOutSlowInEasing)
            },
            label = "income_expense_thumb",
        )
        GlassSurface(
            modifier = Modifier
                .offset {
                    IntOffset(thumbX.roundToPx(), 4.dp.roundToPx())
                }
                .width(thumbWidth)
                .height(40.dp),
            cornerRadius = 16.dp,
            elevation = 7.dp,
            // 整个面板已经完成一次真实背景采样，滑块只保留玻璃轮廓，避免重复模糊。
            backdropBlur = false,
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            if (isExpense) {
                                listOf(palette.accent.copy(alpha = 0.86f), palette.brand.copy(alpha = 0.90f))
                            } else {
                                listOf(palette.income.copy(alpha = 0.72f), palette.accent.copy(alpha = 0.76f))
                            },
                        ),
                        RoundedCornerShape(16.dp),
                    ),
            )
        }

        Row(Modifier.matchParentSize()) {
            listOf(true to "支出", false to "收入").forEach { (expense, label) ->
                val selected = isExpense == expense
                val textColor by animateColorAsState(
                    targetValue = if (selected) Color.White else palette.textSecondary,
                    label = "income_expense_text",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .semantics { contentDescription = "切换到${label}" }
                        .clickable(role = Role.Tab) { onChange(expense) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryOrb(
    category: QuickCategory,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = HeimaTheme.palette
    Column(
        modifier = Modifier
            .width(66.dp)
            .semantics { contentDescription = "选择${category.name}分类" }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(if (selected) 12.dp else 5.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = 0.98f),
                            if (selected) palette.brandSoft else palette.surfaceMuted,
                        ),
                        center = Offset(15f, 10f),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            CategoryIcon(
                glyph = category.glyph,
                color = if (selected) palette.brand else palette.textSecondary,
                modifier = Modifier.size(29.dp),
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = category.name,
            color = if (selected) palette.brand else palette.textSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun CategoryIcon(
    glyph: CategoryGlyph,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = size.minDimension * 0.085f, cap = StrokeCap.Round)
        when (glyph) {
            CategoryGlyph.MEAL -> {
                drawArc(color, 0f, 180f, false, Offset(w * 0.16f, h * 0.30f), androidx.compose.ui.geometry.Size(w * 0.68f, h * 0.48f), style = stroke)
                drawLine(color, Offset(w * 0.20f, h * 0.55f), Offset(w * 0.80f, h * 0.55f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.50f, h * 0.22f), Offset(w * 0.50f, h * 0.12f), stroke.width, StrokeCap.Round)
            }
            CategoryGlyph.TRANSPORT -> {
                drawRoundRect(color, Offset(w * 0.14f, h * 0.34f), androidx.compose.ui.geometry.Size(w * 0.72f, h * 0.34f), androidx.compose.ui.geometry.CornerRadius(w * 0.12f), style = stroke)
                drawLine(color, Offset(w * 0.28f, h * 0.34f), Offset(w * 0.39f, h * 0.20f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.39f, h * 0.20f), Offset(w * 0.68f, h * 0.20f), stroke.width, StrokeCap.Round)
                drawCircle(color, w * 0.075f, Offset(w * 0.30f, h * 0.72f))
                drawCircle(color, w * 0.075f, Offset(w * 0.70f, h * 0.72f))
            }
            CategoryGlyph.SHOPPING -> {
                drawRoundRect(color, Offset(w * 0.22f, h * 0.28f), androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.56f), androidx.compose.ui.geometry.CornerRadius(w * 0.09f), style = stroke)
                drawArc(color, 180f, 180f, false, Offset(w * 0.34f, h * 0.13f), androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.30f), style = stroke)
            }
            CategoryGlyph.HOME -> {
                val path = Path().apply {
                    moveTo(w * 0.16f, h * 0.48f)
                    lineTo(w * 0.50f, h * 0.18f)
                    lineTo(w * 0.84f, h * 0.48f)
                }
                drawPath(path, color, style = stroke)
                drawRoundRect(color, Offset(w * 0.25f, h * 0.45f), androidx.compose.ui.geometry.Size(w * 0.50f, h * 0.38f), androidx.compose.ui.geometry.CornerRadius(w * 0.07f), style = stroke)
            }
            CategoryGlyph.ENTERTAINMENT -> {
                drawCircle(color, w * 0.32f, Offset(w * 0.50f, h * 0.50f), style = stroke)
                val play = Path().apply {
                    moveTo(w * 0.44f, h * 0.36f)
                    lineTo(w * 0.67f, h * 0.50f)
                    lineTo(w * 0.44f, h * 0.64f)
                    close()
                }
                drawPath(play, color)
            }
            CategoryGlyph.SALARY -> {
                drawRoundRect(color, Offset(w * 0.17f, h * 0.25f), androidx.compose.ui.geometry.Size(w * 0.66f, h * 0.52f), androidx.compose.ui.geometry.CornerRadius(w * 0.10f), style = stroke)
                drawLine(color, Offset(w * 0.30f, h * 0.45f), Offset(w * 0.70f, h * 0.45f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.36f, h * 0.60f), Offset(w * 0.58f, h * 0.60f), stroke.width, StrokeCap.Round)
            }
            CategoryGlyph.BONUS -> {
                drawCircle(color, w * 0.27f, Offset(w * 0.50f, h * 0.54f), style = stroke)
                drawLine(color, Offset(w * 0.50f, h * 0.12f), Offset(w * 0.50f, h * 0.24f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.18f, h * 0.28f), Offset(w * 0.29f, h * 0.35f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.82f, h * 0.28f), Offset(w * 0.71f, h * 0.35f), stroke.width, StrokeCap.Round)
            }
            CategoryGlyph.SIDE_JOB -> {
                drawRoundRect(color, Offset(w * 0.16f, h * 0.31f), androidx.compose.ui.geometry.Size(w * 0.68f, h * 0.49f), androidx.compose.ui.geometry.CornerRadius(w * 0.08f), style = stroke)
                drawArc(color, 180f, 180f, false, Offset(w * 0.35f, h * 0.14f), androidx.compose.ui.geometry.Size(w * 0.30f, h * 0.30f), style = stroke)
                drawLine(color, Offset(w * 0.16f, h * 0.52f), Offset(w * 0.84f, h * 0.52f), stroke.width, StrokeCap.Round)
            }
            CategoryGlyph.INVESTMENT -> {
                drawLine(color, Offset(w * 0.18f, h * 0.76f), Offset(w * 0.18f, h * 0.52f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.44f, h * 0.76f), Offset(w * 0.44f, h * 0.38f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.70f, h * 0.76f), Offset(w * 0.70f, h * 0.20f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.62f, h * 0.28f), Offset(w * 0.70f, h * 0.20f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.70f, h * 0.20f), Offset(w * 0.79f, h * 0.29f), stroke.width, StrokeCap.Round)
            }
            CategoryGlyph.GIFT -> {
                drawRoundRect(color, Offset(w * 0.18f, h * 0.35f), androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.46f), androidx.compose.ui.geometry.CornerRadius(w * 0.06f), style = stroke)
                drawLine(color, Offset(w * 0.50f, h * 0.35f), Offset(w * 0.50f, h * 0.81f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.16f, h * 0.49f), Offset(w * 0.84f, h * 0.49f), stroke.width, StrokeCap.Round)
                drawArc(color, 160f, 220f, false, Offset(w * 0.27f, h * 0.13f), androidx.compose.ui.geometry.Size(w * 0.23f, h * 0.27f), style = stroke)
                drawArc(color, 160f, 220f, false, Offset(w * 0.50f, h * 0.13f), androidx.compose.ui.geometry.Size(w * 0.23f, h * 0.27f), style = stroke)
            }
            CategoryGlyph.ADD -> {
                drawCircle(color, w * 0.32f, Offset(w * 0.50f, h * 0.50f), style = stroke)
                drawLine(color, Offset(w * 0.34f, h * 0.50f), Offset(w * 0.66f, h * 0.50f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.50f, h * 0.34f), Offset(w * 0.50f, h * 0.66f), stroke.width, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun NumericPad(
    onKey: (String) -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫"),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Column(
            modifier = Modifier.weight(3f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        KeyButton(
                            text = key,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (key == "⌫") onDelete() else onKey(key)
                            },
                        )
                    }
                }
            }
        }
        SaveButton(onClick = onSave, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun KeyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    PressableGlassSurface(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .semantics {
                contentDescription = when (text) {
                    "." -> "输入小数点"
                    "⌫" -> "删除一位金额"
                    else -> "输入数字$text"
                }
            },
        cornerRadius = 16.dp,
    ) {
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = palette.textPrimary,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Composable
private fun SaveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    PressableGlassSurface(
        onClick = onClick,
        modifier = modifier.height(232.dp),
        cornerRadius = 21.dp,
        // 父面板负责光学玻璃层；保存按钮只做按压反馈和染色，不再重复模糊。
        backdropBlur = false,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(palette.accent.copy(alpha = 0.90f), palette.brand),
                    ),
                    RoundedCornerShape(21.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "保存",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

internal fun appendAmountInput(
    current: String,
    key: String,
    maxIntegerDigits: Int = 9,
): String {
    if (key == ".") {
        return when {
            current.contains('.') -> current
            current.isEmpty() -> "0."
            else -> "$current."
        }
    }
    if (key.length != 1 || !key[0].isDigit()) return current

    val decimalIndex = current.indexOf('.')
    if (decimalIndex >= 0) {
        val decimalPlaces = current.length - decimalIndex - 1
        return if (decimalPlaces < 2) current + key else current
    }

    if (current == "0") {
        return if (key == "0") current else key
    }
    return if (current.length < maxIntegerDigits) current + key else current
}

internal fun formatAmount(input: String): String = input.ifEmpty { "0" }

internal fun amountInputToCents(input: String): Long? {
    val normalized = input.trim().trimEnd('.')
    if (normalized.isEmpty()) return null
    val parts = normalized.split('.', limit = 2)
    val yuan = parts[0].toLongOrNull() ?: return null
    val fraction = parts.getOrNull(1).orEmpty().padEnd(2, '0').take(2)
    val cents = fraction.toLongOrNull() ?: 0L
    return runCatching { Math.addExact(Math.multiplyExact(yuan, 100L), cents) }.getOrNull()
}
