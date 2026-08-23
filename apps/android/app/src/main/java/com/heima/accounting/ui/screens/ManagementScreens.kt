package com.heima.accounting.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.GlassFieldSurface
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.GlassSegmentedControl
import com.heima.accounting.designsystem.GlassToggle
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.PressableGlassSurface
import com.heima.accounting.domain.Category
import com.heima.accounting.domain.EntryType
import com.heima.accounting.domain.LedgerSnapshot
import com.heima.accounting.domain.Transaction
import com.heima.accounting.ui.CategoryIcon
import com.heima.accounting.ui.CategoryIconChoices
import com.heima.accounting.ui.categoryColorFromArgb
import com.heima.accounting.ui.GlassConfirmDialog
import com.heima.accounting.ui.GlassTextInputDialog
import com.heima.accounting.ui.HeimaDialogFrame
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
    val listState = rememberLazyListState()
    LazyColumn(
        Modifier.fillMaxWidth(),
        state = listState,
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
        GlassConfirmDialog(
            title = "删除这笔账单？",
            message = "删除后可以立即通过底部提示撤销。",
            confirmText = "删除",
            onDismiss = { deleting = null },
            onConfirm = { deleting = null; onDelete(transaction) },
            destructive = true,
        )
    }
}

@Composable
private fun SegmentedFilter(selected: EntryType?, onSelected: (EntryType?) -> Unit) {
    GlassSegmentedControl(
        options = listOf<Pair<EntryType?, String>>(null to "全部", EntryType.EXPENSE to "支出", EntryType.INCOME to "收入"),
        selected = selected,
        onSelected = onSelected,
        accessibilityLabel = "账单类型筛选",
    )
}

@Composable
fun CategoriesScreen(
    snapshot: LedgerSnapshot,
    onBack: () -> Unit,
    onSave: (Category) -> Unit,
    onDelete: (Category) -> Unit,
    onReorder: (List<String>) -> Unit,
) {
    val palette = HeimaTheme.palette
    var typeFilter by remember { mutableStateOf<EntryType?>(null) }
    var editing by remember { mutableStateOf<Category?>(null) }
    var deleting by remember { mutableStateOf<Category?>(null) }
    var expandedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sortingGroup by remember { mutableStateOf<EntryType?>(null) }
    val types = typeFilter?.let(::listOf) ?: listOf(EntryType.EXPENSE, EntryType.INCOME)
    val listState = rememberLazyListState()
    LazyColumn(
        Modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 42.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item { BackHeading("分类管理", onBack) }
        item { SegmentedFilter(typeFilter) { typeFilter = it } }
        item {
            PressableGlassSurface(
                {
                    editing = Category(
                        id = "",
                        type = typeFilter ?: EntryType.EXPENSE,
                        name = "",
                        iconKey = "other",
                        colorArgb = 0xFF7593B8,
                        isCustom = true,
                    )
                },
                Modifier.fillMaxWidth().height(48.dp),
                17.dp,
            ) {
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) { Text("添加一级分类", color = palette.brand, fontWeight = FontWeight.SemiBold) }
            }
        }
        types.forEach { sectionType ->
            val topLevel = snapshot.categories
                .filter { it.type == sectionType && it.parentId == null }
                .sortedBy(Category::sortOrder)
            item(key = "section_${sectionType.name}") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        if (sectionType == EntryType.EXPENSE) "支出分类" else "收入分类",
                        color = palette.textPrimary,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        if (sortingGroup == sectionType) "完成排序" else "按住分类拖动排序",
                        color = palette.brand,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clickable {
                            sortingGroup = if (sortingGroup == sectionType) null else sectionType
                        }.padding(4.dp),
                    )
                }
            }
            items(topLevel, key = { "${sectionType.name}_${it.id}" }) { category ->
                CategoryManagementCard(
                    category = category,
                    children = snapshot.categories.filter { it.parentId == category.id }.sortedBy(Category::sortOrder),
                    expanded = category.id in expandedIds,
                    sorting = sortingGroup == sectionType,
                    onToggleExpanded = {
                        expandedIds = if (category.id in expandedIds) expandedIds - category.id else expandedIds + category.id
                    },
                    onEdit = { editing = it },
                    onAddChild = {
                        editing = Category(
                            id = "",
                            type = category.type,
                            name = "",
                            iconKey = category.iconKey,
                            colorArgb = category.colorArgb,
                            parentId = category.id,
                            isCustom = true,
                            sortOrder = snapshot.categories.count { it.parentId == category.id },
                        )
                    },
                    onDelete = { deleting = it },
                    onMove = { direction ->
                        val currentIndex = topLevel.indexOfFirst { it.id == category.id }
                        val target = (currentIndex + direction).coerceIn(topLevel.indices)
                        if (currentIndex >= 0 && currentIndex != target) {
                            val reordered = topLevel.toMutableList().apply { add(target, removeAt(currentIndex)) }
                            onReorder(reordered.map(Category::id))
                        }
                    },
                    onLongPress = { sortingGroup = sectionType },
                )
            }
        }
    }
    editing?.let { draft ->
        CategoryEditorDialog(
            draft = draft,
            parent = draft.parentId?.let(snapshot::category),
            onDismiss = { editing = null },
            onSave = { editing = null; onSave(it) },
        )
    }
    deleting?.let { category ->
        GlassConfirmDialog(
            title = if (category.isCustom) "删除“${category.name}”？" else "隐藏“${category.name}”？",
            message = if (category.isCustom) {
                "没有账单使用时会直接删除；已有账单或细分时只会停用，历史账单不会丢失。"
            } else {
                "预设分类只会从新记账选项中隐藏，已有账单和历史统计不会丢失。"
            },
            confirmText = if (category.isCustom) "确认删除" else "确认隐藏",
            onDismiss = { deleting = null },
            onConfirm = { deleting = null; onDelete(category) },
            destructive = true,
        )
    }
}

@Composable
private fun CategoryManagementCard(
    category: Category,
    children: List<Category>,
    expanded: Boolean,
    sorting: Boolean,
    onToggleExpanded: () -> Unit,
    onEdit: (Category) -> Unit,
    onAddChild: () -> Unit,
    onDelete: (Category) -> Unit,
    onMove: (Int) -> Unit,
    onLongPress: () -> Unit,
) {
    val palette = HeimaTheme.palette
    val reorderThreshold = with(LocalDensity.current) { 42.dp.toPx() }
    var draggedDistance by remember(category.id) { mutableFloatStateOf(0f) }
    GlassSurface(Modifier.fillMaxWidth(), 23.dp, backdropBlur = false) {
        Column(Modifier.padding(17.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .pointerInput(category.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedDistance = 0f
                                onLongPress()
                            },
                            onDragCancel = { draggedDistance = 0f },
                            onDragEnd = { draggedDistance = 0f },
                            onDrag = { change, amount ->
                                change.consume()
                                draggedDistance += amount.y
                                when {
                                    draggedDistance >= reorderThreshold -> {
                                        onMove(1)
                                        draggedDistance = 0f
                                    }
                                    draggedDistance <= -reorderThreshold -> {
                                        onMove(-1)
                                        draggedDistance = 0f
                                    }
                                }
                            },
                        )
                    }
                    .semantics { contentDescription = "分类：${category.name}" }
                    .combinedClickable(onClick = onToggleExpanded, onLongClick = onLongPress),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryIcon(category.iconKey, selected = false, size = 48.dp)
                Spacer(Modifier.size(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(category.name, color = if (category.isActive) palette.textPrimary else palette.textMuted, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${if (category.isCustom) "自定义" else "预设"} · ${children.size} 个细分${if (!category.isActive) " · 已隐藏" else ""}",
                        color = palette.textTertiary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                if (sorting) {
                    Text("≡", color = palette.textSecondary, modifier = Modifier.padding(horizontal = 5.dp))
                    Text("↑", color = palette.brand, modifier = Modifier.clickable { onMove(-1) }.padding(9.dp))
                    Text("↓", color = palette.brand, modifier = Modifier.clickable { onMove(1) }.padding(9.dp))
                } else {
                    Text(
                        "编辑",
                        color = palette.brand,
                        modifier = Modifier
                            .semantics { contentDescription = "编辑${category.name}" }
                            .clickable { onEdit(category) }
                            .padding(8.dp),
                    )
                    Text(if (expanded) "⌃" else "⌄", color = palette.textSecondary, modifier = Modifier.padding(8.dp))
                }
            }
            if (expanded && !sorting) {
                Spacer(Modifier.height(10.dp))
                children.forEach { child ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(categoryColorFromArgb(category.colorArgb), CircleShape))
                        Text(child.name, Modifier.weight(1f).padding(start = 10.dp), color = if (child.isActive) palette.textSecondary else palette.textMuted)
                        Text("编辑", color = palette.brand, modifier = Modifier.clickable { onEdit(child) }.padding(8.dp))
                        if (child.isCustom) Text("删除", color = palette.expense, modifier = Modifier.clickable { onDelete(child) }.padding(8.dp))
                    }
                }
                Text("＋ 添加二级分类", color = palette.brand, modifier = Modifier.clickable(onClick = onAddChild).padding(vertical = 9.dp))
            }
        }
    }
}

private val CategoryColors = listOf(
    0xFFF2A65AL, 0xFF55A6D9L, 0xFFE97868L, 0xFF8A77D5L,
    0xFF39A878L, 0xFFDF729FL, 0xFFE35D6AL, 0xFF8A96A8L,
)

@Composable
private fun CategoryEditorDialog(
    draft: Category,
    parent: Category?,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit,
) {
    var name by remember(draft.id) { mutableStateOf(draft.name) }
    var type by remember(draft.id) { mutableStateOf(draft.type) }
    var iconKey by remember(draft.id) { mutableStateOf(draft.iconKey) }
    var colorArgb by remember(draft.id) {
        mutableLongStateOf(if (draft.colorArgb > 0xFFFFFFFFL) draft.colorArgb ushr 8 else draft.colorArgb)
    }
    var active by remember(draft.id) { mutableStateOf(draft.isActive) }
    var error by remember(draft.id) { mutableStateOf<String?>(null) }
    val palette = HeimaTheme.palette
    val isNew = draft.id.isBlank()
    val isTopLevel = draft.parentId == null
    HeimaDialogFrame(onDismiss = onDismiss) {
        LazyColumn(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
                    item {
                        Text(if (isNew) "添加分类" else "编辑分类", color = palette.textPrimary, style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (isTopLevel) "一级分类" else "二级分类 · ${parent?.name.orEmpty()}",
                            color = palette.textSecondary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    item {
                        GlassFieldSurface(Modifier.fillMaxWidth()) {
                            BasicTextField(
                                value = name,
                                onValueChange = { name = it.take(20); error = null },
                                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "分类名称" },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
                                decorationBox = { input ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (name.isBlank()) Text("分类名称", color = palette.textMuted)
                                        input()
                                    }
                                },
                            )
                        }
                    }
                    if (isTopLevel) {
                        item {
                            if (isNew) {
                                GlassSegmentedControl(
                                    listOf(EntryType.EXPENSE to "支出", EntryType.INCOME to "收入"),
                                    type,
                                    { type = it },
                                    accessibilityLabel = "分类类型",
                                )
                            } else {
                                Text(
                                    if (type == EntryType.EXPENSE) "支出分类 · 已有分类类型不可更改" else "收入分类 · 已有分类类型不可更改",
                                    color = palette.textSecondary,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        item { Text("分类图标", color = palette.textPrimary, style = MaterialTheme.typography.titleMedium) }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(CategoryIconChoices, key = { it.first }) { choice ->
                                    Column(
                                        Modifier
                                            .semantics { contentDescription = "分类图标：${choice.second}" }
                                            .clickable { iconKey = choice.first },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        CategoryIcon(choice.first, selected = choice.first == iconKey, size = 54.dp)
                                        Text(choice.second, color = if (choice.first == iconKey) palette.brand else palette.textMuted, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                        item { Text("分类颜色", color = palette.textPrimary, style = MaterialTheme.typography.titleMedium) }
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                CategoryColors.forEach { color ->
                                    Box(
                                        Modifier
                                            .size(30.dp)
                                            .semantics { contentDescription = "选择分类颜色 ${color.toString(16)}" }
                                            .clip(CircleShape)
                                            .background(Color(color))
                                            .then(if (color == colorArgb) Modifier.border(3.dp, palette.textPrimary, CircleShape) else Modifier)
                                            .clickable { colorArgb = color },
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("在记账中显示", color = palette.textPrimary, style = MaterialTheme.typography.titleMedium)
                                Text("关闭后历史账单仍会保留", color = palette.textSecondary, style = MaterialTheme.typography.labelMedium)
                            }
                            GlassToggle(active, { active = it }, "分类显示开关")
                        }
                    }
                    error?.let { message -> item { Text(message, color = palette.expense) } }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PressableGlassSurface(onDismiss, Modifier.weight(1f).height(48.dp), 16.dp) {
                                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) { Text("取消", color = palette.textSecondary) }
                            }
                            PressableGlassSurface(
                                {
                                    if (name.isBlank()) error = "请输入分类名称" else onSave(
                                        draft.copy(
                                            type = type,
                                            name = name.trim(),
                                            iconKey = if (isTopLevel) iconKey else parent?.iconKey ?: iconKey,
                                            colorArgb = if (isTopLevel) colorArgb else parent?.colorArgb ?: colorArgb,
                                            isActive = active,
                                            isCustom = if (isNew) true else draft.isCustom,
                                        ),
                                    )
                                },
                                Modifier.weight(1f).height(48.dp),
                                16.dp,
                            ) {
                                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) { Text("保存", color = palette.brand, fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
        }
    }
}

@Composable
fun DataScreen(onBack: () -> Unit, onExportCsv: () -> Unit, onExportBackup: () -> Unit, onRestoreBackup: () -> Unit) {
    val palette = HeimaTheme.palette
    val listState = rememberLazyListState()
    LazyColumn(Modifier.fillMaxWidth(), state = listState, contentPadding = PaddingValues(22.dp, 42.dp, 22.dp, 80.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
