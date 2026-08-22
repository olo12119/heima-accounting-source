package com.heima.accounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.HeimaThemeStyle
import com.heima.accounting.designsystem.VisualQuality

@Composable
fun ProfileScreen(
    themeStyle: HeimaThemeStyle,
    visualQuality: VisualQuality,
    reduceMotion: Boolean,
    powerSaveMode: Boolean,
    onThemeStyleChange: (HeimaThemeStyle) -> Unit,
    onVisualQualityChange: (VisualQuality) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
) {
    val palette = HeimaTheme.palette

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 58.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { ScreenHeading(title = "我的", eyebrow = "只属于你的本地账本") }
        item {
            EntityCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                Brush.linearGradient(listOf(palette.accent, palette.brand)),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("马", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("黑马记账", color = palette.textPrimary, style = MaterialTheme.typography.titleLarge)
                        Text("数据只保存在你的手机里", color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item { SectionHeading(title = "主题外观") }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemeChoice(
                    title = "Liquid Glass",
                    subtitle = "清透科技",
                    selected = themeStyle == HeimaThemeStyle.LIQUID_GLASS,
                    onClick = { onThemeStyleChange(HeimaThemeStyle.LIQUID_GLASS) },
                    modifier = Modifier.weight(1f),
                )
                ThemeChoice(
                    title = "自然治愈",
                    subtitle = "柔和温暖",
                    selected = themeStyle == HeimaThemeStyle.NATURE_HEALING,
                    onClick = { onThemeStyleChange(HeimaThemeStyle.NATURE_HEALING) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item { SectionHeading(title = "动效与耗电") }
        item {
            EntityCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingToggle(
                        title = "减少动态效果",
                        subtitle = "关闭流体过渡与装饰动效",
                        checked = reduceMotion,
                        onCheckedChange = onReduceMotionChange,
                    )
                    QualityRow(
                        selected = visualQuality,
                        onSelected = onVisualQualityChange,
                    )
                    if (powerSaveMode) {
                        Text(
                            text = "手机正在省电模式：自动画质已临时降低",
                            color = palette.warning,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
        item { SectionHeading(title = "账本管理") }
        item {
            EntityCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingEntry("分类管理", "新增自定义分类；不会在打开首页时弹出")
                    SettingEntry("数据备份", "导出或恢复本地账本")
                    SettingEntry("隐私与安全", "应用锁和本地数据说明")
                }
            }
        }
        item {
            Text(
                text = "视觉体验版 0.1.0 · 当前尚未接入正式数据保存",
                color = palette.textTertiary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ThemeChoice(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    EntityCard(modifier = modifier.clickable(onClick = onClick)) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(
                        Brush.linearGradient(
                            if (title.startsWith("Liquid")) {
                                listOf(androidx.compose.ui.graphics.Color(0xFFEAF3FF), androidx.compose.ui.graphics.Color(0xFF93C3FF))
                            } else {
                                listOf(androidx.compose.ui.graphics.Color(0xFFF2F0DD), androidx.compose.ui.graphics.Color(0xFF9FC3A0))
                            },
                        ),
                        RoundedCornerShape(15.dp),
                    ),
            )
            Spacer(Modifier.height(10.dp))
            Text(title, color = if (selected) palette.brand else palette.textPrimary, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = palette.textSecondary, style = MaterialTheme.typography.labelMedium)
            if (selected) {
                Text("当前使用", color = palette.brand, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val palette = HeimaTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = palette.textPrimary, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun QualityRow(selected: VisualQuality, onSelected: (VisualQuality) -> Unit) {
    val palette = HeimaTheme.palette
    val options = listOf(
        VisualQuality.AUTO to "自动",
        VisualQuality.REFINED to "精美",
        VisualQuality.POWER_SAVER to "省电",
    )
    Column(Modifier.padding(top = 10.dp)) {
        Text("视觉质量", color = palette.textPrimary, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            options.forEach { (value, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selected == value) palette.brandSoft else palette.surfaceMuted,
                            RoundedCornerShape(13.dp),
                        )
                        .clickable { onSelected(value) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = if (selected == value) palette.brand else palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun SettingEntry(title: String, subtitle: String) {
    val palette = HeimaTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = palette.textPrimary, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        Text("›", color = palette.textTertiary, style = MaterialTheme.typography.headlineMedium)
    }
}
