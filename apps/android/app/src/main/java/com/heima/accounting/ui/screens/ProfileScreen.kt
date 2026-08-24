package com.heima.accounting.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.heima.accounting.R
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.GlassSegmentedControl
import com.heima.accounting.designsystem.GlassToggle
import com.heima.accounting.designsystem.HeimaColorMode
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.HeimaSurfaceRole
import com.heima.accounting.designsystem.HeimaThemeStyle
import com.heima.accounting.designsystem.VisualQuality
import com.heima.accounting.update.UpdateCheckResult
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    transactionCount: Int,
    themeStyle: HeimaThemeStyle,
    colorMode: HeimaColorMode,
    visualQuality: VisualQuality,
    reduceMotion: Boolean,
    powerSaveMode: Boolean,
    liquidGlassEnabled: Boolean,
    soundEnabled: Boolean,
    hapticEnabled: Boolean,
    currentVersion: String,
    onCheckUpdate: suspend () -> UpdateCheckResult,
    onThemeStyleChange: (HeimaThemeStyle) -> Unit,
    onColorModeChange: (HeimaColorMode) -> Unit,
    onVisualQualityChange: (VisualQuality) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
    onLiquidGlassEnabledChange: (Boolean) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onHapticEnabledChange: (Boolean) -> Unit,
    onCategories: () -> Unit,
    onRecords: () -> Unit,
    onData: () -> Unit,
) {
    val palette = HeimaTheme.palette
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    val listState = rememberLazyListState()
    LazyColumn(
        Modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 50.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { ScreenHeading("我的", "只属于你的本地账本") }
        item {
            GlassSurface(Modifier.fillMaxWidth(), 27.dp, backdropBlur = false, role = HeimaSurfaceRole.HERO) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                    Box(Modifier.size(64.dp).background(Color.White.copy(.92f), CircleShape).clip(CircleShape), contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.app_icon), null, Modifier.size(53.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("黑马记账", color = palette.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("$transactionCount 笔账单 · 仅保存在本机", color = palette.textSecondary)
                    }
                }
            }
        }
        item { SectionHeading("主题外观") }
        item {
            GlassSurface(Modifier.fillMaxWidth(), 23.dp, backdropBlur = true, role = HeimaSurfaceRole.INTERACTIVE) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        ThemeSwatch("澄澈蓝", listOf(Color(0xFFE9F4FF), Color(0xFF6C9FFF)), themeStyle == HeimaThemeStyle.CLEAR_BLUE) { onThemeStyleChange(HeimaThemeStyle.CLEAR_BLUE) }
                        ThemeSwatch("自然治愈", listOf(Color(0xFFF3F0D8), Color(0xFF83B892)), themeStyle == HeimaThemeStyle.NATURE_HEALING) { onThemeStyleChange(HeimaThemeStyle.NATURE_HEALING) }
                    }
                    SegmentedOptions(listOf(HeimaColorMode.SYSTEM to "跟随系统", HeimaColorMode.LIGHT to "浅色", HeimaColorMode.DARK to "深色"), colorMode, onColorModeChange)
                }
            }
        }
        item { SectionHeading("体验设置") }
        item {
            GlassSurface(Modifier.fillMaxWidth(), 24.dp, backdropBlur = false, role = HeimaSurfaceRole.INTERACTIVE) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                    SettingToggle("Liquid Glass", "使用折射、透光与玻璃选中镜片", liquidGlassEnabled, onLiquidGlassEnabledChange)
                    SettingToggle("操作音效", "仅在保存和重要确认时轻声反馈", soundEnabled, onSoundEnabledChange)
                    SettingToggle("触觉反馈", "轻触选择和确认时提供震动反馈", hapticEnabled, onHapticEnabledChange)
                    SettingToggle("减少动态效果", "保留功能并缩短流体与弹性动画", reduceMotion, onReduceMotionChange)
                    QualityRow(visualQuality, onVisualQualityChange)
                    if (powerSaveMode) Text("系统省电模式下将自动降低高成本视觉效果", color = palette.warning, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
        item { SectionHeading("账本管理") }
        item {
            GlassSurface(Modifier.fillMaxWidth(), 24.dp, backdropBlur = false, role = HeimaSurfaceRole.LIST) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 5.dp)) {
                    SettingEntry("全部账单", "查看、编辑、删除与撤销", onRecords)
                    SettingEntry("分类管理", "添加或调整自己的分类", onCategories)
                    SettingEntry("数据备份", "导出 CSV 或完整备份并恢复", onData)
                    when (val result = updateResult) {
                        is UpdateCheckResult.Available -> SettingEntry(
                            "发现新版本 ${result.update.version}",
                            "点击后由浏览器下载，安装仍由你确认",
                        ) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, result.update.downloadUrl.toUri()))
                        }
                        is UpdateCheckResult.Failed -> SettingEntry("重新检查更新", result.userMessage) {
                            if (!checkingUpdate) scope.launch {
                                checkingUpdate = true
                                updateResult = onCheckUpdate()
                                checkingUpdate = false
                            }
                        }
                        UpdateCheckResult.UpToDate -> SettingEntry("已是最新版本", "当前版本 $currentVersion") {}
                        null -> SettingEntry("检查更新", if (checkingUpdate) "正在连接 GitHub…" else "免费从 GitHub 获取正式更新") {
                            if (!checkingUpdate) scope.launch {
                                checkingUpdate = true
                                updateResult = onCheckUpdate()
                                checkingUpdate = false
                            }
                        }
                    }
                }
            }
        }
        item { Text("黑马记账 $currentVersion", color = palette.textTertiary, style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable
private fun ThemeSwatch(name: String, colors: List<Color>, selected: Boolean, onClick: () -> Unit) {
    val palette = HeimaTheme.palette
    Column(Modifier.semantics { contentDescription = "$name 主题${if (selected) "，当前使用" else ""}" }.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).background(Brush.linearGradient(colors), CircleShape).then(if (selected) Modifier.padding(5.dp).background(palette.brand.copy(.22f), CircleShape) else Modifier))
        Spacer(Modifier.height(6.dp))
        Text(name, color = if (selected) palette.brand else palette.textSecondary, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun <T> SegmentedOptions(options: List<Pair<T, String>>, selected: T, onSelected: (T) -> Unit) {
    GlassSegmentedControl(options, selected, onSelected, accessibilityLabel = "设置选项")
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val palette = HeimaTheme.palette
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = palette.textPrimary, style = MaterialTheme.typography.titleMedium); Text(subtitle, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall) }
        GlassToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            contentDescription = "$title 开关",
        )
    }
}

@Composable
private fun QualityRow(selected: VisualQuality, onSelected: (VisualQuality) -> Unit) {
    val palette = HeimaTheme.palette
    Column(Modifier.padding(vertical = 9.dp)) {
        Text("视觉质量", color = palette.textPrimary, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        SegmentedOptions(listOf(VisualQuality.AUTO to "自动", VisualQuality.REFINED to "精美", VisualQuality.POWER_SAVER to "省电"), selected, onSelected)
    }
}

@Composable
private fun SettingEntry(title: String, subtitle: String, onClick: () -> Unit) {
    val palette = HeimaTheme.palette
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = palette.textPrimary, style = MaterialTheme.typography.titleMedium); Text(subtitle, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall) }
        Text("›", color = palette.textTertiary, style = MaterialTheme.typography.headlineSmall)
    }
}
