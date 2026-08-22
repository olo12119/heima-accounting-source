package com.heima.accounting.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.PressableGlassSurface

@Composable
fun BudgetScreen() {
    val palette = HeimaTheme.palette

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 50.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { ScreenHeading(title = "预算", eyebrow = "给生活留一点从容") }
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 30.dp) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(modifier = Modifier.size(154.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.matchParentSize()) {
                            drawArc(
                                color = palette.surfaceMuted,
                                startAngle = 140f,
                                sweepAngle = 260f,
                                useCenter = false,
                                style = Stroke(width = 15.dp.toPx(), cap = StrokeCap.Round),
                            )
                            drawArc(
                                color = palette.brand.copy(alpha = 0.24f),
                                startAngle = 140f,
                                sweepAngle = 18f,
                                useCenter = false,
                                style = Stroke(width = 15.dp.toPx(), cap = StrokeCap.Round),
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("本月预算", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                            Text("未设置", color = palette.textPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text("设置预算后，这里会显示剩余额度和每日建议", color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(18.dp))
                    PressableGlassSurface(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        cornerRadius = 18.dp,
                    ) {
                        Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                            Text("设置本月预算", color = palette.brand, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        item { SectionHeading(title = "分类预算") }
        item {
            EntityCard(modifier = Modifier.fillMaxWidth()) {
                EmptyIllustration(
                    label = "第一阶段先展示界面，预算保存将在数据阶段接入",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                )
            }
        }
        item { SectionHeading(title = "本月提醒") }
        item {
            EntityCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                        Text("◌", color = palette.brand, style = MaterialTheme.typography.headlineMedium)
                    }
                    Column {
                        Text("温和提醒", color = palette.textPrimary, style = MaterialTheme.typography.titleMedium)
                        Text("只有接近预算时才提醒，不制造焦虑", color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
