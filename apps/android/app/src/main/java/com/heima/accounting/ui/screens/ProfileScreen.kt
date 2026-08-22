package com.heima.accounting.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.R
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.HeimaColorMode
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.HeimaThemeStyle
import com.heima.accounting.designsystem.VisualQuality

@Composable
fun ProfileScreen(
    themeStyle: HeimaThemeStyle,
    colorMode: HeimaColorMode,
    visualQuality: VisualQuality,
    reduceMotion: Boolean,
    powerSaveMode: Boolean,
    onThemeStyleChange: (HeimaThemeStyle) -> Unit,
    onColorModeChange: (HeimaColorMode) -> Unit,
    onVisualQualityChange: (VisualQuality) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
) {
    val palette = HeimaTheme.palette

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 50.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { ScreenHeading(title = "我的", eyebrow = "只属于你的本地账本") }
        item {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 27.dp,
                backdropBlur = true,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .background(Color.White.copy(alpha = 0.90f), CircleShape)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.app_icon),
                            contentDescription = null,
                            modifier = Modifier.size(51.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("本地账本", color = palette.textPrimary, style = MaterialTheme.typography.titleLarge)
                        Text("黑马记账 · 数据只保存在这台手机", color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("未连接账号或云服务", color = palette.brand, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        item { SectionHeading(title = "配色风格") }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ThemeChoice(
                    title = "澄澈蓝",
                    subtitle = "清透、冷静、现代",
                    selected = themeStyle == HeimaThemeStyle.CLEAR_BLUE,
                    onClick = { onThemeStyleChange(HeimaThemeStyle.CLEAR_BLUE) },
                    modifier = Modifier
                        .weight(1f)
                        .height(168.dp),
                    colors = listOf(Color(0xFFEAF3FF), Color(0xFF8EBEFF)),
                )
                ThemeChoice(
                    title = "自然治愈",
                    subtitle = "柔和、温暖、放松",
                    selected = themeStyle == HeimaThemeStyle.NATURE_HEALING,
                    onClick = { onThemeStyleChange(HeimaThemeStyle.NATURE_HEALING) },
                    modifier = Modifier
                        .weight(1f)
                        .height(168.dp),
                    colors = listOf(Color(0xFFF3F0DA), Color(0xFF9BC3A0)),
                )
            }
        }
        item {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp,
                backdropBlur = true,
            ) {
                Column(Modifier.padding(horizontal = 17.dp, vertical = 15.dp)) {
                    Text("Liquid Glass 材质已启用", color = palette.textPrimary, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "它是导航、记账按钮、弹窗和重点卡片的玻璃效果，不再被当作主题名称。",
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        item { SectionHeading(title = "明暗外观") }
        item {
            SegmentedOptions(
                options = listOf(
                    HeimaColorMode.SYSTEM to "跟随系统",
                    HeimaColorMode.LIGHT to "浅色",
                    HeimaColorMode.DARK to "深色",
                ),
                selected = colorMode,
                onSelected = onColorModeChange,
            )
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
                    SettingEntry("分类管理", "新增自定义分类；首页不会自动弹出")
                    SettingEntry("数据备份", "导出或恢复本地账本")
                    SettingEntry("隐私与安全", "金额隐藏和本地数据说明")
                    SettingEntry("字体预览", "字体商店将在后续开放实时预览")
                }
            }
        }
        item {
            Text(
                text = "视觉修正版 0.2.0 · 当前尚未接入正式数据保存",
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
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    EntityCard(modifier = modifier.clickable(onClick = onClick)) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Brush.linearGradient(colors), RoundedCornerShape(16.dp)),
            )
            Spacer(Modifier.height(10.dp))
            Text(title, color = if (selected) palette.brand else palette.textPrimary, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = palette.textSecondary, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            Text(
                if (selected) "当前使用" else "点击切换",
                color = if (selected) palette.brand else palette.textTertiary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun <T> SegmentedOptions(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    val palette = HeimaTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surfaceMuted.copy(alpha = 0.74f), RoundedCornerShape(18.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (active) palette.brandSoft else Color.Transparent,
                        RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelected(value) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (active) palette.brand else palette.textSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                )
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
private fun QualityRow(
    selected: VisualQuality,
    onSelected: (VisualQuality) -> Unit,
) {
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
                    Text(
                        label,
                        color = if (selected == value) palette.brand else palette.textSecondary,
                        style = MaterialTheme.typography.labelLarge,
                    )
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
