package com.heima.accounting.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.PressableGlassSurface

@Composable
fun StatisticsScreen() {
    val palette = HeimaTheme.palette
    var period by remember { mutableIntStateOf(2) }
    val periods = listOf("今日", "本周", "本月")

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 58.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { ScreenHeading(title = "统计", eyebrow = "读懂每一笔真实收支") }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surfaceMuted.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                periods.forEachIndexed { index, label ->
                    PressableGlassSurface(
                        onClick = { period = index },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        cornerRadius = 15.dp,
                    ) {
                        Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                color = if (period == index) palette.brand else palette.textSecondary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (period == index) FontWeight.SemiBold else FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp) {
                Column(Modifier.padding(22.dp)) {
                    Text("本月总览", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("¥0.00", color = palette.textPrimary, style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(4.dp))
                    Text("暂无账目，统计会在记账后自动生成", color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item { SectionHeading(title = "收支结构") }
        item {
            EntityCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Box(modifier = Modifier.size(116.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.matchParentSize()) {
                            drawArc(
                                color = palette.surfaceMuted,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round),
                            )
                            drawArc(
                                color = palette.brand.copy(alpha = 0.36f),
                                startAngle = -90f,
                                sweepAngle = 36f,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round),
                            )
                        }
                        Text("暂无\n数据", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LegendDot("支出", "¥0.00", palette.expense)
                        LegendDot("收入", "¥0.00", palette.income)
                        Text("数据只来自你的真实账目", color = palette.textTertiary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        item { SectionHeading(title = "消费趋势") }
        item {
            EntityCard(modifier = Modifier.fillMaxWidth()) {
                EmptyIllustration(
                    label = "记账后会看到平滑展开的趋势图",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, amount: String, color: androidx.compose.ui.graphics.Color) {
    val palette = HeimaTheme.palette
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(color))
        Text(label, color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(amount, color = palette.textPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
