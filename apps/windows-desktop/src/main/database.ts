import Database from 'better-sqlite3'
import { randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto'
import { dirname } from 'node:path'
import { mkdirSync } from 'node:fs'
import { CATEGORIES } from '../shared/categories'
import { advanceRecurrenceDate, enumerateDates, formatLocalDate, getPresetRange, getPreviousRange } from '../shared/dates'
import {
  backupPayloadSchema,
  budgetInputSchema,
  customCategoryUpdateSchema,
  customPrimaryCategoryInputSchema,
  customSecondaryCategoryInputSchema,
  expenseInputSchema,
  expenseQuerySchema,
  pinSchema,
  statisticsQuerySchema,
  transactionTemplateInputSchema
} from '../shared/schemas'
import type {
  AppSettings,
  BackupCategory,
  BackupExpense,
  BackupPayload,
  BudgetInput,
  BudgetProgress,
  CalendarMonth,
  Category,
  CategoryDeleteResult,
  CategoryManagementItem,
  CategoryTotal,
  ColorTheme,
  CustomCategoryUpdate,
  CustomPrimaryCategoryInput,
  CustomSecondaryCategoryInput,
  DashboardSummary,
  EntryType,
  EntryTypeFilter,
  Expense,
  ExpenseInput,
  ExpenseQuery,
  FrequentCategory,
  RangePreset,
  StatisticsQuery,
  StatisticsSnapshot,
  ThemeMode,
  TransactionKind,
  TransactionTemplate,
  TransactionTemplateInput,
  LockStatus
} from '../shared/types'

type ExpenseRow = {
  id: string
  entry_type: EntryType
  amount_cents: number
  primary_category_id: string
  secondary_category_id: string
  spent_date: string
  spent_time: string
  note: string
  transaction_kind: TransactionKind
  exclude_from_stats: number
  linked_expense_id: string | null
  created_at: string
  updated_at: string
  primary_category_name: string
  secondary_category_name: string
  primary_category_icon: string
  primary_category_color: string
}

type CategoryRow = {
  id: string
  parent_id: string | null
  name: string
  icon: string
  color: string
  sort_order: number
  entry_type: EntryType
  is_system: number
  is_active: number
  usage_count?: number
}

type CategoryTotalRow = {
  category_id: string
  category_name: string
  amount_cents: number
  color: string
  icon: string
}

const expenseSelect = `
  SELECT e.id, e.entry_type, e.amount_cents, e.primary_category_id, e.secondary_category_id,
         e.spent_date, e.spent_time, e.note, e.transaction_kind, e.exclude_from_stats,
         e.linked_expense_id, e.created_at, e.updated_at,
         p.name AS primary_category_name, s.name AS secondary_category_name,
         p.icon AS primary_category_icon, p.color AS primary_category_color
  FROM expenses e
  JOIN categories p ON p.id = e.primary_category_id
  JOIN categories s ON s.id = e.secondary_category_id
`

export class AccountingDatabase {
  private readonly db: Database.Database
  private unlocked = false

  constructor(readonly path: string) {
    mkdirSync(dirname(path), { recursive: true })
    this.db = new Database(path)
    this.db.pragma('foreign_keys = ON')
    this.db.pragma('journal_mode = WAL')
    this.db.pragma('synchronous = NORMAL')
    this.db.pragma('busy_timeout = 5000')
    this.migrate()
    this.seedCategories()
    this.assertIntegrity()
    this.unlocked = this.readSetting('lock_pin_hash') === null
  }

  private migrate(): void {
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS schema_migrations (
        version INTEGER PRIMARY KEY,
        applied_at TEXT NOT NULL
      );

      CREATE TABLE IF NOT EXISTS categories (
        id TEXT PRIMARY KEY,
        parent_id TEXT REFERENCES categories(id),
        name TEXT NOT NULL,
        icon TEXT NOT NULL,
        color TEXT NOT NULL DEFAULT '#7c8580',
        sort_order INTEGER NOT NULL,
        entry_type TEXT NOT NULL DEFAULT 'expense' CHECK(entry_type IN ('expense', 'income')),
        is_system INTEGER NOT NULL DEFAULT 1,
        is_active INTEGER NOT NULL DEFAULT 1
      );

      CREATE TABLE IF NOT EXISTS expenses (
        id TEXT PRIMARY KEY,
        entry_type TEXT NOT NULL DEFAULT 'expense' CHECK(entry_type IN ('expense', 'income')),
        amount_cents INTEGER NOT NULL CHECK(amount_cents > 0 AND amount_cents <= 999999999),
        primary_category_id TEXT NOT NULL REFERENCES categories(id),
        secondary_category_id TEXT NOT NULL REFERENCES categories(id),
        spent_date TEXT NOT NULL,
        spent_time TEXT NOT NULL,
        note TEXT NOT NULL DEFAULT '' CHECK(length(note) <= 200),
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL
      );

      CREATE TABLE IF NOT EXISTS app_settings (
        key TEXT PRIMARY KEY,
        value TEXT NOT NULL
      );

      CREATE TABLE IF NOT EXISTS budget_months (
        month TEXT PRIMARY KEY,
        total_cents INTEGER NOT NULL CHECK(total_cents >= 0 AND total_cents <= 999999999)
      );

      CREATE TABLE IF NOT EXISTS budget_categories (
        month TEXT NOT NULL REFERENCES budget_months(month) ON DELETE CASCADE,
        primary_category_id TEXT NOT NULL REFERENCES categories(id),
        amount_cents INTEGER NOT NULL CHECK(amount_cents > 0 AND amount_cents <= 999999999),
        PRIMARY KEY(month, primary_category_id)
      );

      CREATE TABLE IF NOT EXISTS transaction_templates (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        entry_type TEXT NOT NULL CHECK(entry_type IN ('expense', 'income')),
        amount_cents INTEGER NOT NULL CHECK(amount_cents > 0 AND amount_cents <= 999999999),
        primary_category_id TEXT NOT NULL REFERENCES categories(id),
        secondary_category_id TEXT NOT NULL REFERENCES categories(id),
        spent_date TEXT NOT NULL,
        spent_time TEXT NOT NULL,
        note TEXT NOT NULL DEFAULT '',
        transaction_kind TEXT NOT NULL DEFAULT 'regular' CHECK(transaction_kind IN ('regular', 'refund', 'reimbursement')),
        exclude_from_stats INTEGER NOT NULL DEFAULT 0,
        frequency TEXT NOT NULL DEFAULT 'none' CHECK(frequency IN ('none', 'weekly', 'monthly', 'yearly')),
        next_due_date TEXT,
        is_active INTEGER NOT NULL DEFAULT 1,
        created_at TEXT NOT NULL
      );

      CREATE INDEX IF NOT EXISTS idx_expenses_spent_date
        ON expenses(spent_date DESC, spent_time DESC);
      CREATE INDEX IF NOT EXISTS idx_expenses_primary_category
        ON expenses(primary_category_id, spent_date DESC);
      CREATE INDEX IF NOT EXISTS idx_expenses_secondary_category
        ON expenses(secondary_category_id, spent_date DESC);
    `)
    const categoryColumns = this.db.pragma('table_info(categories)') as Array<{ name: string }>
    if (!categoryColumns.some((column) => column.name === 'color')) {
      this.db.exec("ALTER TABLE categories ADD COLUMN color TEXT NOT NULL DEFAULT '#7c8580'")
    }
    if (!categoryColumns.some((column) => column.name === 'entry_type')) {
      this.db.exec("ALTER TABLE categories ADD COLUMN entry_type TEXT NOT NULL DEFAULT 'expense' CHECK(entry_type IN ('expense', 'income'))")
    }
    const expenseColumns = this.db.pragma('table_info(expenses)') as Array<{ name: string }>
    if (!expenseColumns.some((column) => column.name === 'entry_type')) {
      this.db.exec("ALTER TABLE expenses ADD COLUMN entry_type TEXT NOT NULL DEFAULT 'expense' CHECK(entry_type IN ('expense', 'income'))")
    }
    if (!expenseColumns.some((column) => column.name === 'transaction_kind')) {
      this.db.exec("ALTER TABLE expenses ADD COLUMN transaction_kind TEXT NOT NULL DEFAULT 'regular' CHECK(transaction_kind IN ('regular', 'refund', 'reimbursement'))")
    }
    if (!expenseColumns.some((column) => column.name === 'exclude_from_stats')) {
      this.db.exec('ALTER TABLE expenses ADD COLUMN exclude_from_stats INTEGER NOT NULL DEFAULT 0')
    }
    if (!expenseColumns.some((column) => column.name === 'linked_expense_id')) {
      this.db.exec('ALTER TABLE expenses ADD COLUMN linked_expense_id TEXT REFERENCES expenses(id)')
    }
    const templateColumns = this.db.pragma('table_info(transaction_templates)') as Array<{ name: string }>
    if (!templateColumns.some((column) => column.name === 'spent_date')) {
      this.db.exec("ALTER TABLE transaction_templates ADD COLUMN spent_date TEXT NOT NULL DEFAULT '2000-01-01'")
    }
    this.db.exec(`
      CREATE INDEX IF NOT EXISTS idx_expenses_type_date
        ON expenses(entry_type, spent_date DESC, spent_time DESC);
      CREATE INDEX IF NOT EXISTS idx_expenses_search
        ON expenses(note, spent_date DESC);
      CREATE INDEX IF NOT EXISTS idx_templates_due
        ON transaction_templates(is_active, next_due_date);
    `)
    this.db.prepare(`
      INSERT OR IGNORE INTO schema_migrations(version, applied_at) VALUES(1, ?)
    `).run(new Date().toISOString())
    this.db.prepare(`
      INSERT OR IGNORE INTO schema_migrations(version, applied_at) VALUES(2, ?)
    `).run(new Date().toISOString())
    this.db.prepare(`
      INSERT OR IGNORE INTO schema_migrations(version, applied_at) VALUES(3, ?)
    `).run(new Date().toISOString())
    this.db.prepare(`
      INSERT OR IGNORE INTO schema_migrations(version, applied_at) VALUES(4, ?)
    `).run(new Date().toISOString())
  }

  private seedCategories(): void {
    const statement = this.db.prepare(`
      INSERT INTO categories(id, parent_id, name, icon, color, sort_order, entry_type, is_system, is_active)
      VALUES(@id, @parentId, @name, @icon, @color, @sortOrder, @entryType, 1, 1)
      ON CONFLICT(id) DO UPDATE SET
        parent_id = excluded.parent_id,
        name = excluded.name,
        icon = excluded.icon,
        color = excluded.color,
        sort_order = excluded.sort_order,
        entry_type = excluded.entry_type,
        is_active = 1
    `)
    this.db.transaction(() => {
      for (const category of CATEGORIES) statement.run(category)
    })()
  }

  private assertIntegrity(): void {
    const result = this.db.pragma('quick_check', { simple: true })
    if (result !== 'ok') throw new Error(`数据库完整性检查失败：${String(result)}`)
  }

  close(): void {
    if (this.db.open) this.db.close()
  }

  private readSetting(key: string): string | null {
    const row = this.db.prepare('SELECT value FROM app_settings WHERE key = ?').get(key) as { value: string } | undefined
    return row?.value ?? null
  }

  assertUnlocked(): void {
    if (!this.unlocked) throw new Error('账本已锁定，请先输入隐私密码')
  }

  getLockStatus(): LockStatus {
    const enabled = this.readSetting('lock_pin_hash') !== null
    return { enabled, locked: enabled && !this.unlocked }
  }

  unlock(rawPin: string): LockStatus {
    const pin = pinSchema.parse(rawPin)
    const stored = this.readSetting('lock_pin_hash')
    if (!stored) { this.unlocked = true; return this.getLockStatus() }
    const [saltHex, hashHex] = stored.split(':')
    if (!saltHex || !hashHex) throw new Error('隐私密码数据损坏')
    const actual = scryptSync(pin, Buffer.from(saltHex, 'hex'), 32)
    const expected = Buffer.from(hashHex, 'hex')
    if (expected.length !== actual.length || !timingSafeEqual(expected, actual)) throw new Error('密码不正确')
    this.unlocked = true
    return this.getLockStatus()
  }

  setLockPin(currentPin: string | null, newPin: string | null): LockStatus {
    const stored = this.readSetting('lock_pin_hash')
    if (stored) {
      if (!currentPin) throw new Error('请输入当前密码')
      this.unlock(currentPin)
    }
    if (newPin === null || newPin === '') {
      this.db.prepare("DELETE FROM app_settings WHERE key = 'lock_pin_hash'").run()
      this.unlocked = true
      return this.getLockStatus()
    }
    const pin = pinSchema.parse(newPin)
    const salt = randomBytes(16)
    const hash = scryptSync(pin, salt, 32)
    this.db.prepare(`
      INSERT INTO app_settings(key, value) VALUES('lock_pin_hash', ?)
      ON CONFLICT(key) DO UPDATE SET value = excluded.value
    `).run(`${salt.toString('hex')}:${hash.toString('hex')}`)
    this.unlocked = true
    return this.getLockStatus()
  }

  private mapCategory(row: CategoryRow): Category {
    return {
      id: row.id,
      parentId: row.parent_id,
      name: row.name,
      icon: row.icon,
      color: row.color,
      sortOrder: row.sort_order,
      entryType: row.entry_type,
      isSystem: row.is_system === 1,
      isActive: row.is_active === 1
    }
  }

  private categoryRow(id: string): CategoryRow {
    const row = this.db.prepare(`
      SELECT id, parent_id, name, icon, color, sort_order, entry_type, is_system, is_active
      FROM categories WHERE id = ?
    `).get(id) as CategoryRow | undefined
    if (!row) throw new Error('没有找到该分类')
    return row
  }

  private assertCustomCategory(row: CategoryRow): void {
    if (row.is_system === 1 || !row.id.startsWith('custom.')) throw new Error('系统预设分类不能修改')
  }

  private ensureUniqueCategoryName(parentId: string | null, entryType: EntryType, name: string, excludeId?: string): void {
    const row = this.db.prepare(`
      SELECT id FROM categories
      WHERE ((parent_id IS NULL AND @parentId IS NULL) OR parent_id = @parentId)
        AND entry_type = @entryType AND name = @name AND id <> COALESCE(@excludeId, '')
      LIMIT 1
    `).get({ parentId, entryType, name, excludeId: excludeId ?? null }) as { id: string } | undefined
    if (row) throw new Error('同一级别已经存在同名分类')
  }

  private nextCategorySortOrder(parentId: string | null, entryType: EntryType): number {
    const row = this.db.prepare(`
      SELECT COALESCE(MAX(sort_order), -1) + 1 AS nextOrder FROM categories
      WHERE ((parent_id IS NULL AND @parentId IS NULL) OR parent_id = @parentId)
        AND entry_type = @entryType
    `).get({ parentId, entryType }) as { nextOrder: number }
    return row.nextOrder
  }

  getCategories(): Category[] {
    const rows = this.db.prepare(`
      SELECT id, parent_id, name, icon, color, sort_order, entry_type, is_system, is_active
      FROM categories WHERE is_active = 1
      ORDER BY entry_type, CASE WHEN parent_id IS NULL THEN 0 ELSE 1 END, parent_id, sort_order, name
    `).all() as CategoryRow[]
    return rows.map((row) => this.mapCategory(row))
  }

  getCategoriesForManagement(): CategoryManagementItem[] {
    const rows = this.db.prepare(`
      SELECT c.id, c.parent_id, c.name, c.icon, c.color, c.sort_order, c.entry_type,
             c.is_system, c.is_active,
             CASE WHEN c.parent_id IS NULL
               THEN (SELECT COUNT(*) FROM expenses e WHERE e.primary_category_id = c.id)
               ELSE (SELECT COUNT(*) FROM expenses e WHERE e.secondary_category_id = c.id)
             END AS usage_count
      FROM categories c
      ORDER BY c.entry_type, CASE WHEN c.parent_id IS NULL THEN 0 ELSE 1 END,
               c.parent_id, c.sort_order, c.name
    `).all() as CategoryRow[]
    return rows.map((row) => ({ ...this.mapCategory(row), usageCount: row.usage_count ?? 0 }))
  }

  createCustomPrimaryCategory(rawInput: CustomPrimaryCategoryInput): Category {
    const input = customPrimaryCategoryInputSchema.parse(rawInput)
    this.ensureUniqueCategoryName(null, input.entryType, input.name)
    const primaryId = `custom.${randomUUID()}`
    const secondaryId = `custom.${randomUUID()}`
    const create = this.db.prepare(`
      INSERT INTO categories(id, parent_id, name, icon, color, sort_order, entry_type, is_system, is_active)
      VALUES(@id, @parentId, @name, @icon, @color, @sortOrder, @entryType, 0, 1)
    `)
    this.db.transaction(() => {
      create.run({
        id: primaryId,
        parentId: null,
        name: input.name,
        icon: input.icon,
        color: input.color,
        sortOrder: this.nextCategorySortOrder(null, input.entryType),
        entryType: input.entryType
      })
      this.ensureUniqueCategoryName(primaryId, input.entryType, input.firstSecondaryName)
      create.run({
        id: secondaryId,
        parentId: primaryId,
        name: input.firstSecondaryName,
        icon: input.icon,
        color: input.color,
        sortOrder: 0,
        entryType: input.entryType
      })
    })()
    return this.mapCategory(this.categoryRow(primaryId))
  }

  createCustomSecondaryCategory(rawInput: CustomSecondaryCategoryInput): Category {
    const input = customSecondaryCategoryInputSchema.parse(rawInput)
    const parent = this.categoryRow(input.parentId)
    if (parent.parent_id !== null) throw new Error('二级分类只能添加到一级分类下')
    if (parent.is_active !== 1) throw new Error('请先启用所属一级分类')
    this.ensureUniqueCategoryName(parent.id, parent.entry_type, input.name)
    const id = `custom.${randomUUID()}`
    this.db.prepare(`
      INSERT INTO categories(id, parent_id, name, icon, color, sort_order, entry_type, is_system, is_active)
      VALUES(@id, @parentId, @name, @icon, @color, @sortOrder, @entryType, 0, 1)
    `).run({
      id,
      parentId: parent.id,
      name: input.name,
      icon: parent.icon,
      color: parent.color,
      sortOrder: this.nextCategorySortOrder(parent.id, parent.entry_type),
      entryType: parent.entry_type
    })
    return this.mapCategory(this.categoryRow(id))
  }

  updateCustomCategory(id: string, rawInput: CustomCategoryUpdate): Category {
    const input = customCategoryUpdateSchema.parse(rawInput)
    const category = this.categoryRow(id)
    this.assertCustomCategory(category)
    this.ensureUniqueCategoryName(category.parent_id, category.entry_type, input.name, id)
    const icon = category.parent_id === null ? (input.icon ?? category.icon) : category.icon
    const color = category.parent_id === null ? (input.color ?? category.color) : category.color
    this.db.transaction(() => {
      this.db.prepare('UPDATE categories SET name = ?, icon = ?, color = ? WHERE id = ?')
        .run(input.name, icon, color, id)
      if (category.parent_id === null) {
        this.db.prepare('UPDATE categories SET icon = ?, color = ? WHERE parent_id = ? AND is_system = 0')
          .run(icon, color, id)
      }
    })()
    return this.mapCategory(this.categoryRow(id))
  }

  setCustomCategoryActive(id: string, active: boolean): Category[] {
    const category = this.categoryRow(id)
    this.assertCustomCategory(category)
    if (active && category.parent_id !== null) {
      const parent = this.categoryRow(category.parent_id)
      if (parent.is_active !== 1) throw new Error('请先启用所属一级分类')
    }
    this.db.transaction(() => {
      this.db.prepare('UPDATE categories SET is_active = ? WHERE id = ?').run(active ? 1 : 0, id)
      if (category.parent_id === null) {
        this.db.prepare('UPDATE categories SET is_active = ? WHERE parent_id = ? AND is_system = 0')
          .run(active ? 1 : 0, id)
      }
    })()
    return this.getCategories()
  }

  deleteCustomCategory(id: string): CategoryDeleteResult {
    const category = this.categoryRow(id)
    this.assertCustomCategory(category)
    const usage = category.parent_id === null
      ? this.db.prepare('SELECT COUNT(*) AS count FROM expenses WHERE primary_category_id = ?').get(id) as { count: number }
      : this.db.prepare('SELECT COUNT(*) AS count FROM expenses WHERE secondary_category_id = ?').get(id) as { count: number }
    if (usage.count > 0) {
      this.setCustomCategoryActive(id, false)
      return { mode: 'deactivated' }
    }
    this.db.transaction(() => {
      if (category.parent_id === null) {
        this.db.prepare('DELETE FROM categories WHERE parent_id = ? AND is_system = 0').run(id)
      }
      this.db.prepare('DELETE FROM categories WHERE id = ?').run(id)
    })()
    return { mode: 'deleted' }
  }

  reorderCustomCategory(id: string, direction: 'up' | 'down'): Category[] {
    const category = this.categoryRow(id)
    this.assertCustomCategory(category)
    const siblings = this.db.prepare(`
      SELECT id, parent_id, name, icon, color, sort_order, entry_type, is_system, is_active
      FROM categories
      WHERE is_system = 0 AND entry_type = @entryType
        AND ((parent_id IS NULL AND @parentId IS NULL) OR parent_id = @parentId)
      ORDER BY sort_order, name
    `).all({ parentId: category.parent_id, entryType: category.entry_type }) as CategoryRow[]
    const index = siblings.findIndex((sibling) => sibling.id === id)
    const swapIndex = direction === 'up' ? index - 1 : index + 1
    if (index < 0 || swapIndex < 0 || swapIndex >= siblings.length) return this.getCategories()
    const other = siblings[swapIndex]!
    this.db.transaction(() => {
      this.db.prepare('UPDATE categories SET sort_order = ? WHERE id = ?').run(other.sort_order, category.id)
      this.db.prepare('UPDATE categories SET sort_order = ? WHERE id = ?').run(category.sort_order, other.id)
    })()
    return this.getCategories()
  }

  private validateCategoryPair(primaryId: string, secondaryId: string, entryType: EntryType, requireActive = true): void {
    const activeCondition = requireActive ? 'AND p.is_active = 1 AND s.is_active = 1' : ''
    const result = this.db.prepare(`
      SELECT COUNT(*) AS count FROM categories p
      JOIN categories s ON s.parent_id = p.id
      WHERE p.id = ? AND s.id = ? AND p.entry_type = ? AND s.entry_type = ?
        ${activeCondition}
    `).get(primaryId, secondaryId, entryType, entryType) as { count: number }
    if (result.count !== 1) throw new Error('所选分类与收支类型不匹配')
  }

  private validateExpenseRelationship(input: ReturnType<typeof expenseInputSchema.parse>, editingId?: string): void {
    if (input.transactionKind === 'regular') {
      if (input.linkedExpenseId) throw new Error('普通账目不能关联退款来源')
      return
    }
    if (input.entryType !== 'income' || !input.linkedExpenseId) throw new Error('退款或报销必须关联一笔原支出')
    if (input.linkedExpenseId === editingId) throw new Error('账目不能关联自己')
    const linked = this.db.prepare("SELECT entry_type FROM expenses WHERE id = ?").get(input.linkedExpenseId) as { entry_type: EntryType } | undefined
    if (!linked || linked.entry_type !== 'expense') throw new Error('没有找到可关联的原支出')
  }

  createExpense(rawInput: ExpenseInput): Expense {
    const input = expenseInputSchema.parse(rawInput)
    this.validateCategoryPair(input.primaryCategoryId, input.secondaryCategoryId, input.entryType)
    this.validateExpenseRelationship(input)
    const id = randomUUID()
    const now = new Date().toISOString()
    this.db.prepare(`
      INSERT INTO expenses(
        id, entry_type, amount_cents, primary_category_id, secondary_category_id,
        spent_date, spent_time, note, transaction_kind, exclude_from_stats, linked_expense_id,
        created_at, updated_at
      ) VALUES(@id, @entryType, @amountCents, @primaryCategoryId, @secondaryCategoryId,
        @spentDate, @spentTime, @note, @transactionKind, @excludeFromStats, @linkedExpenseId,
        @createdAt, @updatedAt)
    `).run({ ...input, excludeFromStats: input.excludeFromStats ? 1 : 0, id, createdAt: now, updatedAt: now })
    return this.getExpense(id)
  }

  importExpenses(rawInputs: ExpenseInput[]): { importedCount: number; duplicateCount: number } {
    let importedCount = 0
    let duplicateCount = 0
    this.db.transaction(() => {
      for (const rawInput of rawInputs) {
        const input = expenseInputSchema.parse(rawInput)
        this.validateCategoryPair(input.primaryCategoryId, input.secondaryCategoryId, input.entryType)
        this.validateExpenseRelationship(input)
        const duplicate = this.db.prepare(`
          SELECT 1 FROM expenses
          WHERE entry_type = @entryType AND amount_cents = @amountCents
            AND primary_category_id = @primaryCategoryId AND secondary_category_id = @secondaryCategoryId
            AND spent_date = @spentDate AND spent_time = @spentTime AND note = @note
            AND transaction_kind = @transactionKind AND exclude_from_stats = @excludeFromStats
          LIMIT 1
        `).get({ ...input, excludeFromStats: input.excludeFromStats ? 1 : 0 })
        if (duplicate) { duplicateCount += 1; continue }
        this.createExpense(input)
        importedCount += 1
      }
    })()
    return { importedCount, duplicateCount }
  }

  updateExpense(id: string, rawInput: ExpenseInput): Expense {
    const input = expenseInputSchema.parse(rawInput)
    this.validateCategoryPair(input.primaryCategoryId, input.secondaryCategoryId, input.entryType)
    this.validateExpenseRelationship(input, id)
    const result = this.db.prepare(`
      UPDATE expenses SET
        entry_type = @entryType,
        amount_cents = @amountCents,
        primary_category_id = @primaryCategoryId,
        secondary_category_id = @secondaryCategoryId,
        spent_date = @spentDate,
        spent_time = @spentTime,
        note = @note,
        transaction_kind = @transactionKind,
        exclude_from_stats = @excludeFromStats,
        linked_expense_id = @linkedExpenseId,
        updated_at = @updatedAt
      WHERE id = @id
    `).run({ id, ...input, excludeFromStats: input.excludeFromStats ? 1 : 0, updatedAt: new Date().toISOString() })
    if (result.changes === 0) throw new Error('没有找到要修改的账目')
    return this.getExpense(id)
  }

  deleteExpense(id: string): void {
    const linked = this.db.prepare('SELECT COUNT(*) AS count FROM expenses WHERE linked_expense_id = ?').get(id) as { count: number }
    if (linked.count > 0) throw new Error('这笔支出关联了退款或报销记录，请先处理关联记录')
    const result = this.db.prepare('DELETE FROM expenses WHERE id = ?').run(id)
    if (result.changes === 0) throw new Error('没有找到要删除的账目')
  }

  private mapExpense(row: ExpenseRow): Expense {
    return {
      id: row.id,
      entryType: row.entry_type,
      amountCents: row.amount_cents,
      primaryCategoryId: row.primary_category_id,
      secondaryCategoryId: row.secondary_category_id,
      spentDate: row.spent_date,
      spentTime: row.spent_time,
      note: row.note,
      transactionKind: row.transaction_kind,
      excludeFromStats: row.exclude_from_stats === 1,
      linkedExpenseId: row.linked_expense_id,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
      primaryCategoryName: row.primary_category_name,
      secondaryCategoryName: row.secondary_category_name,
      primaryCategoryIcon: row.primary_category_icon,
      primaryCategoryColor: row.primary_category_color
    }
  }

  getExpense(id: string): Expense {
    const row = this.db.prepare(`${expenseSelect} WHERE e.id = ?`).get(id) as ExpenseRow | undefined
    if (!row) throw new Error('没有找到该账目')
    return this.mapExpense(row)
  }

  listExpenses(preset: RangePreset, entryType: EntryTypeFilter = 'all'): Expense[] {
    return this.searchExpenses({ preset, entryType, keyword: '' })
  }

  searchExpenses(rawQuery: ExpenseQuery): Expense[] {
    const query = expenseQuerySchema.parse(rawQuery)
    const range = query.preset === 'custom'
      ? { startDate: query.startDate, endDate: query.endDate }
      : getPresetRange(query.preset)
    const conditions: string[] = []
    if (range.startDate) conditions.push('e.spent_date BETWEEN @startDate AND @endDate')
    if (query.entryType !== 'all') conditions.push('e.entry_type = @entryType')
    if (query.keyword) conditions.push(`(
      e.note LIKE @keyword ESCAPE '\\' OR p.name LIKE @keyword ESCAPE '\\'
      OR s.name LIKE @keyword ESCAPE '\\' OR printf('%.2f', e.amount_cents / 100.0) LIKE @keyword ESCAPE '\\'
    )`)
    const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : ''
    const escapedKeyword = query.keyword.replace(/[\\%_]/g, (character) => `\\${character}`)
    const rows = this.db.prepare(`
      ${expenseSelect} ${where}
      ORDER BY e.spent_date DESC, e.spent_time DESC, e.created_at DESC
    `).all({ ...range, entryType: query.entryType, keyword: `%${escapedKeyword}%` }) as ExpenseRow[]
    return rows.map((row) => this.mapExpense(row))
  }

  private sumBetween(startDate: string, endDate: string, entryType: EntryType): number {
    const row = this.db.prepare(`
      SELECT COALESCE(SUM(CASE
        WHEN @entryType = 'income' AND entry_type = 'income' AND transaction_kind = 'regular' THEN amount_cents
        WHEN @entryType = 'expense' AND entry_type = 'expense' THEN amount_cents
        WHEN @entryType = 'expense' AND entry_type = 'income'
          AND transaction_kind IN ('refund', 'reimbursement') AND linked_expense_id IS NOT NULL THEN -amount_cents
        ELSE 0 END), 0) AS total
      FROM expenses WHERE spent_date BETWEEN @startDate AND @endDate AND exclude_from_stats = 0
    `).get({ startDate, endDate, entryType }) as { total: number }
    return row.total
  }

  private categoryTotals(startDate: string, endDate: string, entryType: EntryType): CategoryTotal[] {
    const rows = entryType === 'income'
      ? this.db.prepare(`
        SELECT e.primary_category_id AS category_id, c.name AS category_name,
               SUM(e.amount_cents) AS amount_cents, c.color, c.icon
        FROM expenses e JOIN categories c ON c.id = e.primary_category_id
        WHERE e.spent_date BETWEEN ? AND ? AND e.entry_type = 'income'
          AND e.transaction_kind = 'regular' AND e.exclude_from_stats = 0
        GROUP BY e.primary_category_id, c.name, c.color, c.icon
        ORDER BY amount_cents DESC
      `).all(startDate, endDate)
      : this.db.prepare(`
        SELECT values_by_category.category_id, c.name AS category_name,
               SUM(values_by_category.amount_cents) AS amount_cents, c.color, c.icon
        FROM (
          SELECT e.primary_category_id AS category_id, e.amount_cents
          FROM expenses e
          WHERE e.spent_date BETWEEN @startDate AND @endDate AND e.entry_type = 'expense'
            AND e.exclude_from_stats = 0
          UNION ALL
          SELECT original.primary_category_id AS category_id, -refund.amount_cents
          FROM expenses refund JOIN expenses original ON original.id = refund.linked_expense_id
          WHERE refund.spent_date BETWEEN @startDate AND @endDate
            AND refund.entry_type = 'income' AND refund.transaction_kind IN ('refund', 'reimbursement')
            AND refund.exclude_from_stats = 0
        ) values_by_category JOIN categories c ON c.id = values_by_category.category_id
        GROUP BY values_by_category.category_id, c.name, c.color, c.icon
        HAVING SUM(values_by_category.amount_cents) > 0
        ORDER BY amount_cents DESC
      `).all({ startDate, endDate })
    const typedRows = rows as CategoryTotalRow[]
    return typedRows.map((row) => ({
      categoryId: row.category_id,
      categoryName: row.category_name,
      amountCents: row.amount_cents,
      color: row.color,
      icon: row.icon
    }))
  }

  getDashboard(): DashboardSummary {
    const today = getPresetRange('today') as { startDate: string; endDate: string }
    const month = getPresetRange('month') as { startDate: string; endDate: string }
    const recentRows = this.db.prepare(`
      ${expenseSelect} ORDER BY e.spent_date DESC, e.spent_time DESC, e.created_at DESC LIMIT 5
    `).all() as ExpenseRow[]
    const monthExpenseCents = this.sumBetween(month.startDate, month.endDate, 'expense')
    const monthIncomeCents = this.sumBetween(month.startDate, month.endDate, 'income')
    const cashFlowStart = new Date()
    cashFlowStart.setDate(cashFlowStart.getDate() - 6)
    const dailyCashFlow = enumerateDates(formatLocalDate(cashFlowStart), today.endDate).map((date) => ({
      date,
      expenseCents: this.sumBetween(date, date, 'expense'),
      incomeCents: this.sumBetween(date, date, 'income')
    }))
    return {
      todayExpenseCents: this.sumBetween(today.startDate, today.endDate, 'expense'),
      todayIncomeCents: this.sumBetween(today.startDate, today.endDate, 'income'),
      monthExpenseCents,
      monthIncomeCents,
      monthBalanceCents: monthIncomeCents - monthExpenseCents,
      recentEntries: recentRows.map((row) => this.mapExpense(row)),
      categoryTotals: this.categoryTotals(month.startDate, month.endDate, 'expense'),
      dailyCashFlow,
      budget: this.getBudget(month.startDate.slice(0, 7)),
      pendingTemplates: this.listTemplates().filter((template) => template.isActive && template.frequency !== 'none' && Boolean(template.nextDueDate) && template.nextDueDate! <= today.endDate).length
    }
  }

  getStatistics(preset: 'today' | 'week' | 'month' | 'year', entryType: EntryType): StatisticsSnapshot {
    return this.queryStatistics({ preset, entryType })
  }

  queryStatistics(rawQuery: StatisticsQuery): StatisticsSnapshot {
    const query = statisticsQuerySchema.parse(rawQuery)
    const range = query.preset === 'custom'
      ? { startDate: query.startDate!, endDate: query.endDate! }
      : getPresetRange(query.preset) as { startDate: string; endDate: string }
    const { startDate, endDate } = range
    const rows = this.db.prepare(`
      SELECT spent_date AS date, SUM(CASE
        WHEN @entryType = 'income' AND entry_type = 'income' AND transaction_kind = 'regular' THEN amount_cents
        WHEN @entryType = 'expense' AND entry_type = 'expense' THEN amount_cents
        WHEN @entryType = 'expense' AND entry_type = 'income'
          AND transaction_kind IN ('refund', 'reimbursement') AND linked_expense_id IS NOT NULL THEN -amount_cents
        ELSE 0 END) AS amountCents
      FROM expenses WHERE spent_date BETWEEN @startDate AND @endDate AND exclude_from_stats = 0
      GROUP BY spent_date ORDER BY spent_date
    `).all({ startDate, endDate, entryType: query.entryType }) as Array<{ date: string; amountCents: number }>
    const totals = new Map(rows.map((row) => [row.date, row.amountCents]))
    const previous = getPreviousRange(startDate, endDate)
    const totalCents = this.sumBetween(startDate, endDate, query.entryType)
    const previousTotalCents = this.sumBetween(previous.startDate, previous.endDate, query.entryType)
    return {
      preset: query.preset,
      startDate,
      endDate,
      entryType: query.entryType,
      totalCents,
      previousTotalCents,
      changePercent: previousTotalCents === 0 ? null : Math.round((totalCents - previousTotalCents) / previousTotalCents * 1000) / 10,
      categoryTotals: this.categoryTotals(startDate, endDate, query.entryType),
      dailyTotals: enumerateDates(startDate, endDate).map((date) => ({
        date,
        amountCents: totals.get(date) ?? 0
      }))
    }
  }

  getCalendarMonth(rawMonth: string): CalendarMonth {
    const month = /^\d{4}-(?:0[1-9]|1[0-2])$/.test(rawMonth) ? rawMonth : (() => { throw new Error('月份无效') })()
    const [year, monthNumber] = month.split('-').map(Number)
    const lastDay = new Date(year!, monthNumber!, 0).getDate()
    const startDate = `${month}-01`
    const endDate = `${month}-${String(lastDay).padStart(2, '0')}`
    return {
      month,
      days: enumerateDates(startDate, endDate).map((date) => ({
        date,
        expenseCents: this.sumBetween(date, date, 'expense'),
        incomeCents: this.sumBetween(date, date, 'income'),
        count: (this.db.prepare('SELECT COUNT(*) AS count FROM expenses WHERE spent_date = ?').get(date) as { count: number }).count
      }))
    }
  }

  getBudget(rawMonth: string): BudgetProgress {
    if (!/^\d{4}-(?:0[1-9]|1[0-2])$/.test(rawMonth)) throw new Error('月份无效')
    const [year, monthNumber] = rawMonth.split('-').map(Number)
    const endDate = `${rawMonth}-${String(new Date(year!, monthNumber!, 0).getDate()).padStart(2, '0')}`
    const total = this.db.prepare('SELECT total_cents AS totalCents FROM budget_months WHERE month = ?')
      .get(rawMonth) as { totalCents: number } | undefined
    const rows = this.db.prepare(`
      SELECT b.primary_category_id AS categoryId, c.name AS categoryName, c.color, c.icon,
             b.amount_cents AS limitCents
      FROM budget_categories b JOIN categories c ON c.id = b.primary_category_id
      WHERE b.month = ? ORDER BY c.sort_order, c.name
    `).all(rawMonth) as Array<{ categoryId: string; categoryName: string; color: string; icon: string; limitCents: number }>
    const spending = new Map(this.categoryTotals(`${rawMonth}-01`, endDate, 'expense').map((item) => [item.categoryId, item.amountCents]))
    const spentCents = this.sumBetween(`${rawMonth}-01`, endDate, 'expense')
    const totalCents = total?.totalCents ?? 0
    return {
      month: rawMonth,
      totalCents,
      spentCents,
      remainingCents: totalCents - spentCents,
      percent: totalCents > 0 ? Math.round(spentCents / totalCents * 1000) / 10 : 0,
      categories: rows.map((row) => ({ ...row, spentCents: spending.get(row.categoryId) ?? 0 }))
    }
  }

  saveBudget(rawInput: BudgetInput): BudgetProgress {
    const input = budgetInputSchema.parse(rawInput)
    const ids = new Set(input.categoryLimits.map((item) => item.categoryId))
    if (ids.size !== input.categoryLimits.length) throw new Error('分类预算存在重复分类')
    for (const limit of input.categoryLimits) {
      const category = this.categoryRow(limit.categoryId)
      if (category.parent_id !== null || category.entry_type !== 'expense') throw new Error('分类预算只能选择支出一级分类')
    }
    this.db.transaction(() => {
      this.db.prepare(`INSERT INTO budget_months(month, total_cents) VALUES(?, ?)
        ON CONFLICT(month) DO UPDATE SET total_cents = excluded.total_cents`).run(input.month, input.totalCents)
      this.db.prepare('DELETE FROM budget_categories WHERE month = ?').run(input.month)
      const insert = this.db.prepare('INSERT INTO budget_categories(month, primary_category_id, amount_cents) VALUES(?, ?, ?)')
      for (const limit of input.categoryLimits) insert.run(input.month, limit.categoryId, limit.amountCents)
    })()
    return this.getBudget(input.month)
  }

  listTemplates(): TransactionTemplate[] {
    const rows = this.db.prepare(`
      SELECT id, name, entry_type, amount_cents, primary_category_id, secondary_category_id,
             spent_date, spent_time, note, transaction_kind, exclude_from_stats, frequency,
             next_due_date, is_active, created_at
      FROM transaction_templates ORDER BY is_active DESC, next_due_date, created_at DESC
    `).all() as Array<{
      id: string; name: string; entry_type: EntryType; amount_cents: number; primary_category_id: string
      secondary_category_id: string; spent_date: string; spent_time: string; note: string
      transaction_kind: TransactionKind; exclude_from_stats: number; frequency: TransactionTemplate['frequency']
      next_due_date: string | null; is_active: number; created_at: string
    }>
    return rows.map((row) => ({
      id: row.id, name: row.name, entryType: row.entry_type, amountCents: row.amount_cents,
      primaryCategoryId: row.primary_category_id, secondaryCategoryId: row.secondary_category_id,
      spentDate: row.spent_date, spentTime: row.spent_time, note: row.note,
      transactionKind: row.transaction_kind, excludeFromStats: row.exclude_from_stats === 1,
      linkedExpenseId: null, frequency: row.frequency, nextDueDate: row.next_due_date,
      isActive: row.is_active === 1, createdAt: row.created_at
    }))
  }

  saveTemplate(rawInput: TransactionTemplateInput): TransactionTemplate {
    const input = transactionTemplateInputSchema.parse(rawInput)
    if (input.transactionKind !== 'regular' || input.linkedExpenseId) throw new Error('退款和报销不能创建周期模板')
    this.validateCategoryPair(input.primaryCategoryId, input.secondaryCategoryId, input.entryType)
    const id = randomUUID()
    this.db.prepare(`
      INSERT INTO transaction_templates(id, name, entry_type, amount_cents, primary_category_id,
        secondary_category_id, spent_date, spent_time, note, transaction_kind, exclude_from_stats,
        frequency, next_due_date, is_active, created_at)
      VALUES(@id, @name, @entryType, @amountCents, @primaryCategoryId, @secondaryCategoryId,
        @spentDate, @spentTime, @note, @transactionKind, @excludeFromStats, @frequency,
        @nextDueDate, 1, @createdAt)
    `).run({ ...input, id, excludeFromStats: input.excludeFromStats ? 1 : 0, createdAt: new Date().toISOString() })
    return this.listTemplates().find((template) => template.id === id)!
  }

  deleteTemplate(id: string): void {
    const result = this.db.prepare('DELETE FROM transaction_templates WHERE id = ?').run(id)
    if (result.changes === 0) throw new Error('没有找到该模板')
  }

  applyTemplate(id: string, rawSpentDate?: string): Expense {
    const template = this.listTemplates().find((item) => item.id === id)
    if (!template) throw new Error('没有找到该模板')
    const spentDate = rawSpentDate ?? template.nextDueDate ?? formatLocalDate(new Date())
    expenseInputSchema.pick({ spentDate: true }).parse({ spentDate })
    return this.db.transaction(() => {
      const expense = this.createExpense({
        entryType: template.entryType, amountCents: template.amountCents,
        primaryCategoryId: template.primaryCategoryId, secondaryCategoryId: template.secondaryCategoryId,
        spentDate, spentTime: template.spentTime, note: template.note,
        transactionKind: 'regular', excludeFromStats: template.excludeFromStats, linkedExpenseId: null
      })
      if (template.frequency !== 'none' && template.nextDueDate) {
        const nextDueDate = advanceRecurrenceDate(template.nextDueDate, template.frequency)
        this.db.prepare('UPDATE transaction_templates SET next_due_date = ? WHERE id = ?').run(nextDueDate, id)
      }
      return expense
    })()
  }

  getFrequentCategories(entryType: EntryType): FrequentCategory[] {
    const now = new Date()
    const start = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 29)
    return this.db.prepare(`
      SELECT e.primary_category_id AS primaryCategoryId,
             e.secondary_category_id AS secondaryCategoryId,
             p.name AS primaryCategoryName, s.name AS secondaryCategoryName,
             COUNT(*) AS count
      FROM expenses e
      JOIN categories p ON p.id = e.primary_category_id
      JOIN categories s ON s.id = e.secondary_category_id
      WHERE e.spent_date BETWEEN ? AND ? AND e.entry_type = ?
        AND p.is_active = 1 AND s.is_active = 1
      GROUP BY e.primary_category_id, e.secondary_category_id, p.name, s.name
      ORDER BY count DESC, MAX(e.spent_date || ' ' || e.spent_time) DESC
      LIMIT 4
    `).all(formatLocalDate(start), formatLocalDate(now), entryType) as FrequentCategory[]
  }

  getSettings(): AppSettings {
    const rows = this.db.prepare("SELECT key, value FROM app_settings WHERE key IN ('theme', 'color_theme')")
      .all() as Array<{ key: string; value: string }>
    const values = new Map(rows.map((row) => [row.key, row.value]))
    return {
      theme: (values.get('theme') as ThemeMode | undefined) ?? 'system',
      colorTheme: (values.get('color_theme') as ColorTheme | undefined) ?? 'forest'
    }
  }

  setTheme(theme: ThemeMode): AppSettings {
    this.db.prepare(`
      INSERT INTO app_settings(key, value) VALUES('theme', ?)
      ON CONFLICT(key) DO UPDATE SET value = excluded.value
    `).run(theme)
    return this.getSettings()
  }

  setColorTheme(colorTheme: ColorTheme): AppSettings {
    this.db.prepare(`
      INSERT INTO app_settings(key, value) VALUES('color_theme', ?)
      ON CONFLICT(key) DO UPDATE SET value = excluded.value
    `).run(colorTheme)
    return this.getSettings()
  }

  getBackupPayload(): BackupPayload {
    const rows = this.db.prepare(`
      SELECT id, entry_type, amount_cents, primary_category_id, secondary_category_id,
             spent_date, spent_time, note, transaction_kind, exclude_from_stats, linked_expense_id,
             created_at, updated_at
      FROM expenses ORDER BY spent_date, spent_time, created_at
    `).all() as Array<{
      id: string
      entry_type: EntryType
      amount_cents: number
      primary_category_id: string
      secondary_category_id: string
      spent_date: string
      spent_time: string
      note: string
      transaction_kind: TransactionKind
      exclude_from_stats: number
      linked_expense_id: string | null
      created_at: string
      updated_at: string
    }>
    const expenses: BackupExpense[] = rows.map((row) => ({
      id: row.id,
      entryType: row.entry_type,
      amountCents: row.amount_cents,
      primaryCategoryId: row.primary_category_id,
      secondaryCategoryId: row.secondary_category_id,
      spentDate: row.spent_date,
      spentTime: row.spent_time,
      note: row.note,
      transactionKind: row.transaction_kind,
      excludeFromStats: row.exclude_from_stats === 1,
      linkedExpenseId: row.linked_expense_id,
      createdAt: row.created_at,
      updatedAt: row.updated_at
    }))
    const categoryRows = this.db.prepare(`
      SELECT id, parent_id, name, icon, color, sort_order, entry_type, is_system, is_active
      FROM categories WHERE is_system = 0
      ORDER BY CASE WHEN parent_id IS NULL THEN 0 ELSE 1 END, parent_id, sort_order, name
    `).all() as CategoryRow[]
    const categories: BackupCategory[] = categoryRows.map((row) => ({
      id: row.id,
      parentId: row.parent_id,
      name: row.name,
      icon: row.icon,
      color: row.color,
      sortOrder: row.sort_order,
      entryType: row.entry_type,
      isActive: row.is_active === 1
    }))
    const budgetRows = this.db.prepare('SELECT month, total_cents AS totalCents FROM budget_months ORDER BY month')
      .all() as Array<{ month: string; totalCents: number }>
    const budgetLimitStatement = this.db.prepare(`
      SELECT primary_category_id AS categoryId, amount_cents AS amountCents
      FROM budget_categories WHERE month = ? ORDER BY primary_category_id
    `)
    const budgets = budgetRows.map((budget) => ({
      ...budget,
      categoryLimits: budgetLimitStatement.all(budget.month) as Array<{ categoryId: string; amountCents: number }>
    }))
    return { categories, expenses, settings: this.getSettings(), budgets, templates: this.listTemplates() }
  }

  replaceFromBackup(rawPayload: BackupPayload): void {
    const payload = backupPayloadSchema.parse(rawPayload)
    const expenseIds = new Set(payload.expenses.map((expense) => expense.id))
    if (expenseIds.size !== payload.expenses.length) throw new Error('备份中存在重复账目编号')
    const categoryIds = new Set(payload.categories.map((category) => category.id))
    if (categoryIds.size !== payload.categories.length) throw new Error('备份中存在重复分类编号')

    const insertExpense = this.db.prepare(`
      INSERT INTO expenses(
        id, entry_type, amount_cents, primary_category_id, secondary_category_id,
        spent_date, spent_time, note, transaction_kind, exclude_from_stats, linked_expense_id,
        created_at, updated_at
      ) VALUES(@id, @entryType, @amountCents, @primaryCategoryId, @secondaryCategoryId,
        @spentDate, @spentTime, @note, @transactionKind, @excludeFromStats, @linkedExpenseId,
        @createdAt, @updatedAt)
    `)
    const insertCategory = this.db.prepare(`
      INSERT INTO categories(id, parent_id, name, icon, color, sort_order, entry_type, is_system, is_active)
      VALUES(@id, @parentId, @name, @icon, @color, @sortOrder, @entryType, 0, @isActive)
    `)
    this.db.transaction(() => {
      this.db.prepare('DELETE FROM transaction_templates').run()
      this.db.prepare('DELETE FROM budget_categories').run()
      this.db.prepare('DELETE FROM budget_months').run()
      this.db.prepare('UPDATE expenses SET linked_expense_id = NULL WHERE linked_expense_id IS NOT NULL').run()
      this.db.prepare('DELETE FROM expenses').run()
      this.db.prepare('DELETE FROM categories WHERE is_system = 0').run()

      const orderedCategories = [...payload.categories].sort((left, right) =>
        Number(left.parentId !== null) - Number(right.parentId !== null) || left.sortOrder - right.sortOrder)
      for (const category of orderedCategories) {
        const systemCollision = this.db.prepare('SELECT 1 FROM categories WHERE id = ? AND is_system = 1')
          .get(category.id)
        if (systemCollision) throw new Error('备份中的自定义分类与系统分类冲突')
        if (category.parentId !== null) {
          const parent = this.categoryRow(category.parentId)
          if (parent.parent_id !== null || parent.entry_type !== category.entryType) {
            throw new Error('备份中的分类层级或收支类型不正确')
          }
          if (category.isActive && parent.is_active !== 1) throw new Error('备份中启用的二级分类所属一级分类已停用')
        }
        this.ensureUniqueCategoryName(category.parentId, category.entryType, category.name)
        insertCategory.run({ ...category, isActive: category.isActive ? 1 : 0 })
      }

      const orderedExpenses = [...payload.expenses].sort((left, right) => Number(Boolean(left.linkedExpenseId)) - Number(Boolean(right.linkedExpenseId)))
      for (const rawExpense of orderedExpenses) {
        const expense = expenseInputSchema.parse(rawExpense)
        this.validateCategoryPair(expense.primaryCategoryId, expense.secondaryCategoryId, expense.entryType, false)
        this.validateExpenseRelationship(expense, rawExpense.id)
        insertExpense.run({ ...rawExpense, ...expense, excludeFromStats: expense.excludeFromStats ? 1 : 0 })
      }
      for (const budget of payload.budgets) this.saveBudget(budget)
      for (const template of payload.templates) {
        const parsed = transactionTemplateInputSchema.parse(template)
        this.validateCategoryPair(parsed.primaryCategoryId, parsed.secondaryCategoryId, parsed.entryType, false)
        this.db.prepare(`
          INSERT INTO transaction_templates(id, name, entry_type, amount_cents, primary_category_id,
            secondary_category_id, spent_date, spent_time, note, transaction_kind, exclude_from_stats,
            frequency, next_due_date, is_active, created_at)
          VALUES(@id, @name, @entryType, @amountCents, @primaryCategoryId, @secondaryCategoryId,
            @spentDate, @spentTime, @note, @transactionKind, @excludeFromStats, @frequency,
            @nextDueDate, @isActive, @createdAt)
        `).run({ ...template, ...parsed, excludeFromStats: parsed.excludeFromStats ? 1 : 0, isActive: template.isActive ? 1 : 0 })
      }
      this.setTheme(payload.settings.theme)
      this.setColorTheme(payload.settings.colorTheme)
    })()
  }
}
