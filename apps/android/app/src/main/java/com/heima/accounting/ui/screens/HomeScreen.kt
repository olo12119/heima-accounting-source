package com.heima.accounting.ui.screens

import android.icu.util.Calendar
import android.icu.util.ULocale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.PressableGlassSurface
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    amountsVisible: Boolean,
    onAmountsVisibleChange: (Boolean) -> Unit,
    onRecord: () -> Unit,
) {
    val palette = HeimaTheme.palette
    val today = remember { LocalDate.now() }
    val weekday = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE", Locale.SIMPLIFIED_CHINESE))
    }
    val lunar = remember(today) { formatLunarDate(today) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 50.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dailyGreeting(today),
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.textSecondary,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "${today.monthValue}月${today.dayOfMonth}日 $weekday",
                        style = MaterialTheme.typography.headlineLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${today.year}年 · $lunar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textTertiary,
                    )
                }
                PressableGlassSurface(
                    onClick = { onAmountsVisibleChange(!amountsVisible) },
                    modifier = Modifier
                        .size(50.dp)
                        .semantics {
                            contentDescription = if (amountsVisible) "隐藏所有金额" else "显示所有金额"
                        },
                    cornerRadius = 25.dp,
                    backdropBlur = true,
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.matchParentSize()) {
                        PrivacyEyeIcon(
                            visible = amountsVisible,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
        }

        item {
            Column {
                Text(
                    text = "今日消费",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(4.dp))
                AnimatedContent(
                    targetState = amountsVisible,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "home_amount_privacy",
                ) { visible ->
                    Text(
                        text = privateAmount("¥0.00", visible),
                        style = MaterialTheme.typography.displayLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = "今日收入  ${privateAmount("¥0.00", amountsVisible)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textSecondary,
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EntityCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(148.dp),
                ) {
                    Column {
                        Text("本月趋势", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(11.dp))
                        MiniTrend(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        )
                        Spacer(Modifier.height(5.dp))
                        Text("等待第一笔账", color = palette.textTertiary, style = MaterialTheme.typography.labelMedium)
                    }
                }
                EntityCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(148.dp),
                ) {
                    Column {
                        Text("剩余预算", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(11.dp))
                        Text("未设置", color = palette.textPrimary, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("设置后显示进度", color = palette.textTertiary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        item {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(154.dp),
                cornerRadius = 28.dp,
                backdropBlur = true,
            ) {
                Row(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("财务状态", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(3.dp))
                        Text("等待记录", color = palette.brand, style = MaterialTheme.typography.headlineMedium)
                        Text("记下第一笔后，这里会给出温和提示", color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Box(
                        modifier = Modifier.size(76.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.matchParentSize()) {
                            drawCircle(palette.brandSoft.copy(alpha = 0.78f))
                            drawCircle(
                                color = palette.brand.copy(alpha = 0.66f),
                                radius = size.minDimension * 0.30f,
                                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
                            )
                            drawCircle(
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.52f),
                                radius = size.minDimension * 0.38f,
                                center = Offset(size.width * 0.38f, size.height * 0.32f),
                                style = Stroke(width = 1.5.dp.toPx()),
                            )
                        }
                    }
                }
            }
        }

        item { SectionHeading(title = "分类支出洞察") }

        item {
            EntityCard(modifier = Modifier.fillMaxWidth()) {
                EmptyIllustration(
                    label = "有账目后，这里会展示花得最多的分类",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
            }
        }

        item { SectionHeading(title = "最近账单", action = "查看全部") }

        item {
            PressableGlassSurface(
                onClick = onRecord,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                backdropBlur = true,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                ) {
                    Text("还没有账单", color = palette.textPrimary, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(5.dp))
                    Text("点击这里或底部记账按钮，开始记录真实收支", color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun PrivacyEyeIcon(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    val progress by animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "privacy_eye",
    )
    val color by animateColorAsState(
        targetValue = if (visible) palette.brand else palette.textSecondary,
        label = "privacy_eye_color",
    )
    Canvas(modifier) {
        val eye = Path().apply {
            moveTo(size.width * 0.10f, size.height * 0.50f)
            cubicTo(
                size.width * 0.27f,
                size.height * 0.20f,
                size.width * 0.73f,
                size.height * 0.20f,
                size.width * 0.90f,
                size.height * 0.50f,
            )
            cubicTo(
                size.width * 0.73f,
                size.height * 0.80f,
                size.width * 0.27f,
                size.height * 0.80f,
                size.width * 0.10f,
                size.height * 0.50f,
            )
        }
        drawPath(eye, color, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(
            color = color.copy(alpha = 1f - progress * 0.52f),
            radius = size.minDimension * 0.13f,
            center = center,
        )
        if (progress > 0.01f) {
            drawLine(
                color = color,
                start = Offset(size.width * (0.18f - progress * 0.04f), size.height * 0.18f),
                end = Offset(size.width * (0.82f + progress * 0.04f), size.height * 0.82f),
                strokeWidth = 2.6.dp.toPx() * progress,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun MiniTrend(modifier: Modifier = Modifier) {
    val palette = HeimaTheme.palette
    Canvas(modifier = modifier) {
        val y = size.height * 0.68f
        drawLine(
            color = palette.divider,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx(),
        )
        val path = Path().apply {
            moveTo(0f, y)
            cubicTo(
                size.width * 0.30f,
                y,
                size.width * 0.58f,
                y - size.height * 0.18f,
                size.width,
                y - size.height * 0.08f,
            )
        }
        drawPath(
            path = path,
            color = palette.brand.copy(alpha = 0.48f),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

internal fun dailyGreeting(date: LocalDate): String {
    val greetings = listOf(
        "你好，今天也轻松记一笔",
        "把日常记清，也把生活过轻",
        "每一笔，都是生活留下的脚印",
        "慢慢记录，心里自然更有数",
        "今天的钱，也值得被温柔看见",
        "简单记下，安心生活",
        "看见收支，也看见自己的节奏",
    )
    return greetings[Math.floorMod(date.toEpochDay(), greetings.size.toLong()).toInt()]
}

internal fun formatLunarDate(date: LocalDate): String {
    val calendar = Calendar.getInstance(ULocale("zh_CN@calendar=chinese"))
    calendar.timeInMillis = date
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    val months = listOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
    val days = listOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十",
    )
    val month = calendar.get(Calendar.MONTH).coerceIn(0, months.lastIndex)
    val day = (calendar.get(Calendar.DAY_OF_MONTH) - 1).coerceIn(0, days.lastIndex)
    val leap = if (calendar.get(Calendar.IS_LEAP_MONTH) == 1) "闰" else ""
    return "农历$leap${months[month]}月${days[day]}"
}

private fun privateAmount(value: String, visible: Boolean): String =
    if (visible) value else "¥••••"
