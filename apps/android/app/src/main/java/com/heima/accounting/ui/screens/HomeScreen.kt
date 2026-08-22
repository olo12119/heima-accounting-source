package com.heima.accounting.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.PressableGlassSurface

@Composable
fun HomeScreen(onRecord: () -> Unit) {
    val palette = HeimaTheme.palette

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 58.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            ScreenHeading(
                title = "2026年8月",
                eyebrow = "你好，今天也轻松记一笔",
                trailing = {
                    GlassSurface(
                        modifier = Modifier.size(46.dp),
                        cornerRadius = 23.dp,
                        elevation = 5.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.matchParentSize()) {
                            Text("◉", color = palette.textSecondary, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                },
            )
        }

        item {
            Column {
                Text(
                    text = "今日消费",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "¥0.00",
                    style = MaterialTheme.typography.displayLarge,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "今日收入  ¥0.00",
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
                EntityCard(modifier = Modifier.weight(1f)) {
                    Column {
                        Text("本月趋势", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(12.dp))
                        MiniTrend(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        )
                        Text("等待第一笔账", color = palette.textTertiary, style = MaterialTheme.typography.labelMedium)
                    }
                }
                EntityCard(modifier = Modifier.weight(1f)) {
                    Column {
                        Text("剩余预算", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(10.dp))
                        Text("未设置", color = palette.textPrimary, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(7.dp))
                        Text("设置后显示进度", color = palette.textTertiary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        item {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.55f),
                cornerRadius = 26.dp,
            ) {
                Row(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(horizontal = 20.dp, vertical = 17.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("财务状态", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                        Text("等待记录", color = palette.brand, style = MaterialTheme.typography.headlineMedium)
                        Text("记下第一笔后，这里会给出温和提示", color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Box(
                        modifier = Modifier.size(74.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.matchParentSize()) {
                            drawCircle(palette.brandSoft.copy(alpha = 0.75f))
                            drawCircle(
                                color = palette.brand.copy(alpha = 0.65f),
                                radius = size.minDimension * 0.30f,
                                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
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
            cubicTo(size.width * 0.30f, y, size.width * 0.58f, y - size.height * 0.18f, size.width, y - size.height * 0.08f)
        }
        drawPath(
            path = path,
            color = palette.brand.copy(alpha = 0.45f),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}
