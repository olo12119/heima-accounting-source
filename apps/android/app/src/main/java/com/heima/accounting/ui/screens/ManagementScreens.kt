package com.heima.accounting.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.PressableGlassSurface
import com.heima.accounting.domain.Category
import com.heima.accounting.domain.EntryType
import com.heima.accounting.domain.LedgerSnapshot
import com.heima.accounting.domain.Transaction
import com.heima.accounting.ui.CategoryArtwork
import com.heima.accounting.ui.TransactionRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RecordsScreen(
    snapshot: LedgerSnapshot,
    amountsVisible: Boolean,
    onBack: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
) {
    val palette = HeimaTheme.palette
    var filter by remember { mutableStateOf<EntryType?>(null) }
    var deleting by remember { mutableStateOf<Transaction?>(null) }
    val visible = snapshot.transactions.filter { filter == null || it.type == filter }
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 42.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { BackHeading("全部账单", onBack) }
        item { SegmentedFilter(filter) { filter = it } }
        if (visible.isEmpty()) {
            item { GlassSurface(Modifier.fillMaxWidth(), 25.dp, backdropBlur = false) { EmptyIllustration("还没有符合条件的账单", Modifier.fillMaxWidth().padding(vertical = 32.dp)) } }
        } else {
            val groups = visible.groupBy { Instant.ofEpochMilli(it.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() }
            groups.forEach { (date, transactions) ->
                item { Text(date.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)), color = palette.textSecondary, style = MaterialTheme.typography.labelLarge) }
                item {
                    GlassSurface(Modifier.fillMaxWidth(), 24.dp, backdropBlur = false) {
                        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            transactions.forEach { transaction ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.weight(1f)) { TransactionRow(transaction, snapshot, amountsVisible, onClick = { onEdit(transaction) }) }
                                    Text("删除", color = palette.expense, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable { deleting = transaction }.padding(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    deleting?.let { transaction ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除这笔账单？") },
            text = { Text("删除后可以立即通过底部提示撤销。") },
            confirmButton = { TextButton({ deleting = null; onDelete(transaction) }) { Text("删除", color = palette.expense) } },
            dismissButton = { TextButton({ deleting = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun SegmentedFilter(selected: EntryType?, onSelected: (EntryType?) -> Unit) {
    val palette = HeimaTheme.palette
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(null to "全部", EntryType.EXPENSE to "支出", EntryType.INCOME to "收入").forEach { (value, label) ->
            PressableGlassSurface({ onSelected(value) }, Modifier.weight(1f).height(43.dp), 15.dp, backdropBlur = selected == value) {
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) { Text(label, color = if (selected == value) palette.brand else palette.textSecondary) }
            }
        }
    }
}

@Composable
fun CategoriesScreen(
    snapshot: LedgerSnapshot,
    onBack: () -> Unit,
    onSave: (String?, EntryType, String, String?) -> Unit,
    onDelete: (Category) -> Unit,
) {
    val palette = HeimaTheme.palette
    var type by remember { mutableStateOf(EntryType.EXPENSE) }
    var editing by remember { mutableStateOf<Category?>(null) }
    var addParent by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<Category?>(null) }
    val topLevel = snapshot.topLevelCategories(type)
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 42.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item { BackHeading("分类管理", onBack) }
        item { SegmentedFilter(type) { if (it != null) type = it } }
        item {
            PressableGlassSurface({ editing = Category("", type, "", "other", 0xFF8794A8) }, Modifier.fillMaxWidth().height(48.dp), 17.dp) {
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) { Text("添加一级分类", color = palette.brand, fontWeight = FontWeight.SemiBold) }
            }
        }
        items(topLevel, key = Category::id) { category ->
            GlassSurface(Modifier.fillMaxWidth(), 23.dp, backdropBlur = false) {
                Column(Modifier.padding(17.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryArtwork(category.iconKey, Modifier.size(48.dp))
                        Spacer(Modifier.size(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(category.name, color = palette.textPrimary, style = MaterialTheme.typography.titleMedium)
                            Text(if (category.isCustom) "自定义分类" else "系统分类", color = palette.textTertiary, style = MaterialTheme.typography.labelMedium)
                        }
                        if (category.isCustom) {
                            Text("编辑", color = palette.brand, modifier = Modifier.clickable { editing = category }.padding(8.dp))
                            Text("删除", color = palette.expense, modifier = Modifier.clickable { deleting = category }.padding(8.dp))
                        }
                        Text("＋细分", color = palette.brand, modifier = Modifier.clickable { addParent = category.id }.padding(8.dp))
                    }
                    val children = snapshot.childCategories(category.id)
                    if (children.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(children.joinToString("  ·  ") { it.name }, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                        children.filter(Category::isCustom).forEach { child ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("自定义细分：${child.name}", Modifier.weight(1f), color = palette.textSecondary, style = MaterialTheme.typography.labelMedium)
                                Text("编辑", color = palette.brand, modifier = Modifier.clickable { editing = child; addParent = category.id }.padding(8.dp))
                                Text("删除", color = palette.expense, modifier = Modifier.clickable { deleting = child }.padding(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    val draft = editing
    if (draft != null || addParent != null) {
        CategoryDialog(draft?.name.orEmpty(), if (addParent == null) "分类名称" else "细分名称") { name ->
            if (name != null) onSave(draft?.id?.takeIf(String::isNotBlank), type, name, addParent ?: draft?.parentId)
            editing = null
            addParent = null
        }
    }
    deleting?.let { category ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除“${category.name}”？") },
            text = {
                Text(
                    "没有账单使用时会直接删除；已有账单使用时只会从可选列表中停用，历史账单不会丢失。",
                )
            },
            confirmButton = {
                TextButton({ deleting = null; onDelete(category) }) {
                    Text("确认删除", color = palette.expense)
                }
            },
            dismissButton = { TextButton({ deleting = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun CategoryDialog(initial: String, title: String, onResult: (String?) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = { onResult(null) },
        title = { Text(title) },
        text = { OutlinedTextField(value, { value = it.take(20) }, singleLine = true) },
        confirmButton = { TextButton({ if (value.isNotBlank()) onResult(value.trim()) }) { Text("保存") } },
        dismissButton = { TextButton({ onResult(null) }) { Text("取消") } },
    )
}

@Composable
fun DataScreen(onBack: () -> Unit, onExportCsv: () -> Unit, onExportBackup: () -> Unit, onRestoreBackup: () -> Unit) {
    val palette = HeimaTheme.palette
    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(22.dp, 42.dp, 22.dp, 80.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { BackHeading("数据备份", onBack) }
        item {
            GlassSurface(Modifier.fillMaxWidth(), 25.dp, backdropBlur = false) {
                Column(Modifier.padding(20.dp)) {
                    Text("数据只保存在你的手机里", color = palette.textPrimary, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text("建议定期导出完整备份，并把文件复制到另一台设备或网盘。CSV 适合用表格查看；完整备份用于恢复账本。", color = palette.textSecondary)
                }
            }
        }
        item { DataAction("导出 Excel 可读 CSV", "包含全部真实账单", onExportCsv) }
        item { DataAction("导出完整备份", "包含账单、分类和预算，并带校验值", onExportBackup) }
        item { DataAction("从完整备份恢复", "恢复前会保留当前账本的安全副本", onRestoreBackup) }
    }
}

@Composable
private fun DataAction(title: String, subtitle: String, onClick: () -> Unit) {
    val palette = HeimaTheme.palette
    PressableGlassSurface(onClick, Modifier.fillMaxWidth(), 22.dp) {
        Row(Modifier.padding(19.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, color = palette.textPrimary, style = MaterialTheme.typography.titleMedium); Text(subtitle, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall) }
            Text("›", color = palette.brand, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun BackHeading(title: String, onBack: () -> Unit) {
    val palette = HeimaTheme.palette
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("‹", color = palette.brand, style = MaterialTheme.typography.displaySmall, modifier = Modifier.clickable(onClick = onBack).padding(end = 16.dp))
        Text(title, color = palette.textPrimary, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
    }
}
