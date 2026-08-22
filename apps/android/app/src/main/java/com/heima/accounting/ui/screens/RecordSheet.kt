package com.heima.accounting.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.HeimaTheme

private data class QuickCategory(val icon: String, val name: String)

@Composable
fun RecordSheet(
    onDismiss: () -> Unit,
    onVisualSave: (String) -> Unit,
) {
    val palette = HeimaTheme.palette
    val reduceMotion = HeimaTheme.motion.reduceMotion
    var isExpense by remember { mutableStateOf(true) }
    var amountDigits by remember { mutableStateOf("") }
    var categoryIndex by remember { mutableIntStateOf(0) }
    val categories = remember {
        listOf(
            QuickCategory("🍳", "餐饮"),
            QuickCategory("🚙", "交通"),
            QuickCategory("☕", "饮品"),
            QuickCategory("🛍", "购物"),
            QuickCategory("🎧", "娱乐"),
            QuickCategory("＋", "添加分类"),
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable(onClick = onDismiss),
        )
        GlassSurface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            cornerRadius = 34.dp,
            elevation = 26.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 104.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(width = 42.dp, height = 5.dp)
                        .background(palette.textTertiary.copy(alpha = 0.48f), RoundedCornerShape(3.dp)),
                )
                Spacer(Modifier.height(16.dp))
                IncomeExpenseSwitch(isExpense = isExpense, onChange = { isExpense = it })
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("¥", color = palette.textPrimary, style = MaterialTheme.typography.headlineLarge)
                    AnimatedContent(
                        targetState = formatAmount(amountDigits),
                        transitionSpec = {
                            if (reduceMotion) {
                                androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                            } else {
                                androidx.compose.animation.fadeIn(spring(stiffness = Spring.StiffnessMedium)) togetherWith
                                    androidx.compose.animation.fadeOut(spring(stiffness = Spring.StiffnessMedium)) using
                                    SizeTransform(clip = false)
                            }
                        },
                        label = "rolling_amount",
                    ) { amount ->
                        Text(
                            text = amount,
                            color = palette.textPrimary,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("选择分类", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    categories.forEachIndexed { index, category ->
                        CategoryOrb(
                            category = category,
                            selected = categoryIndex == index,
                            onClick = { categoryIndex = index },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                NumericPad(
                    onDigit = { digit -> if (amountDigits.length < 9) amountDigits += digit },
                    onDelete = { amountDigits = amountDigits.dropLast(1) },
                    onClear = { amountDigits = "" },
                    onSave = {
                        if (amountDigits.any { it != '0' }) {
                            onVisualSave("界面操作正常；正式账目保存将在数据阶段接入")
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
private fun IncomeExpenseSwitch(isExpense: Boolean, onChange: (Boolean) -> Unit) {
    val palette = HeimaTheme.palette
    Row(
        modifier = Modifier
            .widthIn(max = 310.dp)
            .fillMaxWidth()
            .background(palette.surfaceMuted.copy(alpha = 0.86f), RoundedCornerShape(18.dp))
            .padding(4.dp),
    ) {
        listOf(true to "支出", false to "收入").forEach { (expense, label) ->
            val selected = isExpense == expense
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(
                        if (selected) Brush.horizontalGradient(listOf(palette.accent, palette.brand))
                        else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                        RoundedCornerShape(14.dp),
                    )
                    .clickable(role = Role.Tab) { onChange(expense) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (selected) Color.White else palette.textSecondary, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun CategoryOrb(category: QuickCategory, selected: Boolean, onClick: () -> Unit) {
    val palette = HeimaTheme.palette
    Column(
        modifier = Modifier
            .widthIn(min = 48.dp)
            .semantics { contentDescription = "选择${category.name}分类" }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(if (selected) 9.dp else 3.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = 0.96f),
                            if (selected) palette.brandSoft else palette.surfaceMuted,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(category.icon, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = category.name,
            color = if (selected) palette.brand else palette.textSecondary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Composable
private fun NumericPad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("清空", "0", "⌫"),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(modifier = Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        KeyButton(
                            text = key,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                when (key) {
                                    "清空" -> onClear()
                                    "⌫" -> onDelete()
                                    else -> onDigit(key)
                                }
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
private fun KeyButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = HeimaTheme.palette
    Box(
        modifier = modifier
            .height(52.dp)
            .background(palette.surface.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = palette.textPrimary, style = if (text.length == 1) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SaveButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = HeimaTheme.palette
    Box(
        modifier = modifier
            .height(232.dp)
            .background(Brush.verticalGradient(listOf(palette.accent, palette.brand)), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("保存", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

internal fun formatAmount(digits: String): String {
    if (digits.isEmpty()) return "0.00"
    val normalized = digits.trimStart('0').ifEmpty { "0" }
    return when (normalized.length) {
        1 -> "0.0$normalized"
        2 -> "0.$normalized"
        else -> normalized.dropLast(2) + "." + normalized.takeLast(2)
    }
}
