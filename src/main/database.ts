import Database from 'better-sqlite3'
import { randomUUID } from 'node:crypto'
import { dirname } from 'node:path'
import { mkdirSync } from 'node:fs'
import { CATEGORIES, getCategoryColor } from '../shared/categories'
import { enumerateDates, formatLocalDate, getPresetRange } from '../shared/dates'
import { expenseInputSchema } from '../shared/schemas'
import type {
  AppSettings,
  BackupExpense,
  BackupPayload,
  Category,
  CategoryTotal,
  DashboardSummary,
  EntryType,
  EntryTypeFilter,
  Expense,
  ExpenseInput,
  FrequentCategory,
  RangePreset,
  StatisticsSnapshot,
  ThemeMode
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
  created_at: string
  updated_at: string
  primary_category_name: string
  secondary_category_name: string
}

type CategoryTotalRow = {
  category_id: string
  category_name: string
  amount_cents: number
}

const expenseSelect = `
  SELECT e.id, e.entry_type, e.amount_cents, e.primary_category_id, e.secondary_category_id,
         e.spent_date, e.spent_time, e.note, e.created_at, e.updated_at,
         p.name AS primary_category_name, s.name AS secondary_category_name
  FROM expenses e
  JOIN categories p ON p.id = e.primary_category_id
  JOIN categories s ON s.id = e.secondary_category_id
`

export class AccountingDatabase {
  private readonly db: Database.Database

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

      CREATE INDEX IF NOT EXISTS idx_expenses_spent_date
        ON expenses(spent_date DESC, spent_time DESC);
      CREATE INDEX IF NOT EXISTS idx_expenses_primary_category
        ON expenses(primary_category_id, spent_date DESC);
      CREATE INDEX IF NOT EXISTS idx_expenses_secondary_category
        ON expenses(secondary_category_id, spent_date DESC);
    `)
    const categoryColumns = this.db.pragma('table_info(categories)') as Array<{ name: string }>
    if (!categoryColumns.some((column) => column.name === 'entry_type')) {
      this.db.exec("ALTER TABLE categories ADD COLUMN entry_type TEXT NOT NULL DEFAULT 'expense' CHECK(entry_type IN ('expense', 'income'))")
    }
    const expenseColumns = this.db.pragma('table_info(expenses)') as Array<{ name: string }>
    if (!expenseColumns.some((column) => column.name === 'entry_type')) {
      this.db.exec("ALTER TABLE expenses ADD COLUMN entry_type TEXT NOT NULL DEFAULT 'expense' CHECK(entry_type IN ('expense', 'income'))")
    }
    this.db.exec(`
      CREATE INDEX IF NOT EXISTS idx_expenses_type_date
        ON expenses(entry_type, spent_date DESC, spent_time DESC);
    `)
    this.db.prepare(`
      INSERT OR IGNORE INTO schema_migrations(version, applied_at) VALUES(1, ?)
    `).run(new Date().toISOString())
    this.db.prepare(`
      INSERT OR IGNORE INTO schema_migrations(version, applied_at) VALUES(2, ?)
    `).run(new Date().toISOString())
  }

  private seedCategories(): void {
    const statement = this.db.prepare(`
      INSERT INTO categories(id, parent_id, name, icon, sort_order, entry_type, is_system, is_active)
      VALUES(@id, @parentId, @name, @icon, @sortOrder, @entryType, 1, 1)
      ON CONFLICT(id) DO UPDATE SET
        parent_id = excluded.parent_id,
        name = excluded.name,
        icon = excluded.icon,
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

  getCategories(): Category[] {
    return this.db.prepare(`
      SELECT id, parent_id AS parentId, name, icon, sort_order AS sortOrder, entry_type AS entryType
      FROM categories WHERE is_active = 1
      ORDER BY CASE WHEN parent_id IS NULL THEN 0 ELSE 1 END, parent_id, sort_order
    `).all() as Category[]
  }

  private validateCategoryPair(primaryId: string, secondaryId: string, entryType: EntryType): void {
    const result = this.db.prepare(`
      SELECT COUNT(*) AS count FROM categories p
      JOIN categories s ON s.parent_id = p.id
      WHERE p.id = ? AND s.id = ? AND p.entry_type = ? AND s.entry_type = ?
        AND p.is_active = 1 AND s.is_active = 1
    `).get(primaryId, secondaryId, entryType, entryType) as { count: number }
    if (result.count !== 1) throw new Error('所选分类与收支类型不匹配')
  }

  createExpense(rawInput: ExpenseInput): Expense {
    const input = expenseInputSchema.parse(rawInput)
    this.validateCategoryPair(input.primaryCategoryId, input.secondaryCategoryId, input.entryType)
    const id = randomUUID()
    const now = new Date().toISOString()
    this.db.prepare(`
      INSERT INTO expenses(
        id, entry_type, amount_cents, primary_category_id, secondary_category_id,
        spent_date, spent_time, note, created_at, updated_at
      ) VALUES(@id, @entryType, @amountCents, @primaryCategoryId, @secondaryCategoryId,
        @spentDate, @spentTime, @note, @createdAt, @updatedAt)
    `).run({ id, ...input, createdAt: now, updatedAt: now })
    return this.getExpense(id)
  }

  updateExpense(id: string, rawInput: ExpenseInput): Expense {
    const input = expenseInputSchema.parse(rawInput)
    this.validateCategoryPair(input.primaryCategoryId, input.secondaryCategoryId, input.entryType)
    const result = this.db.prepare(`
      UPDATE expenses SET
        entry_type = @entryType,
        amount_cents = @amountCents,
        primary_category_id = @primaryCategoryId,
        secondary_category_id = @secondaryCategoryId,
        spent_date = @spentDate,
        spent_time = @spentTime,
        note = @note,
        updated_at = @updatedAt
      WHERE id = @id
    `).run({ id, ...input, updatedAt: new Date().toISOString() })
    if (result.changes === 0) throw new Error('没有找到要修改的账目')
    return this.getExpense(id)
  }

  deleteExpense(id: string): void {
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
      createdAt: row.created_at,
      updatedAt: row.updated_at,
      primaryCategoryName: row.primary_category_name,
      secondaryCategoryName: row.secondary_category_name
    }
  }

  getExpense(id: string): Expense {
    const row = this.db.prepare(`${expenseSelect} WHERE e.id = ?`).get(id) as ExpenseRow | undefined
    if (!row) throw new Error('没有找到该账目')
    return this.mapExpense(row)
  }

  listExpenses(preset: RangePreset, entryType: EntryTypeFilter = 'all'): Expense[] {
    const range = getPresetRange(preset)
    const conditions: string[] = []
    if (range.startDate) conditions.push('e.spent_date BETWEEN @startDate AND @endDate')
    if (entryType !== 'all') conditions.push('e.entry_type = @entryType')
    const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : ''
    const rows = this.db.prepare(`
      ${expenseSelect} ${where}
      ORDER BY e.spent_date DESC, e.spent_time DESC, e.created_at DESC
    `).all({ ...range, entryType }) as ExpenseRow[]
    return rows.map((row) => this.mapExpense(row))
  }

  private sumBetween(startDate: string, endDate: string, entryType: EntryType): number {
    const row = this.db.prepare(`
      SELECT COALESCE(SUM(amount_cents), 0) AS total
      FROM expenses WHERE spent_date BETWEEN ? AND ? AND entry_type = ?
    `).get(startDate, endDate, entryType) as { total: number }
    return row.total
  }

  private categoryTotals(startDate: string, endDate: string, entryType: EntryType): CategoryTotal[] {
    const rows = this.db.prepare(`
      SELECT e.primary_category_id AS category_id, c.name AS category_name,
             SUM(e.amount_cents) AS amount_cents
      FROM expenses e JOIN categories c ON c.id = e.primary_category_id
      WHERE e.spent_date BETWEEN ? AND ? AND e.entry_type = ?
      GROUP BY e.primary_category_id, c.name
      ORDER BY amount_cents DESC
    `).all(startDate, endDate, entryType) as CategoryTotalRow[]
    return rows.map((row) => ({
      categoryId: row.category_id,
      categoryName: row.category_name,
      amountCents: row.amount_cents,
      color: getCategoryColor(row.category_id)
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
    return {
      todayExpenseCents: this.sumBetween(today.startDate, today.endDate, 'expense'),
      todayIncomeCents: this.sumBetween(today.startDate, today.endDate, 'income'),
      monthExpenseCents,
      monthIncomeCents,
      monthBalanceCents: monthIncomeCents - monthExpenseCents,
      recentEntries: recentRows.map((row) => this.mapExpense(row)),
      categoryTotals: this.categoryTotals(month.startDate, month.endDate, 'expense')
    }
  }

  getStatistics(preset: 'today' | 'week' | 'month', entryType: EntryType): StatisticsSnapshot {
    const { startDate, endDate } = getPresetRange(preset) as { startDate: string; endDate: string }
    const rows = this.db.prepare(`
      SELECT spent_date AS date, SUM(amount_cents) AS amountCents
      FROM expenses WHERE spent_date BETWEEN ? AND ? AND entry_type = ?
      GROUP BY spent_date ORDER BY spent_date
    `).all(startDate, endDate, entryType) as Array<{ date: string; amountCents: number }>
    const totals = new Map(rows.map((row) => [row.date, row.amountCents]))
    return {
      preset,
      startDate,
      endDate,
      entryType,
      totalCents: this.sumBetween(startDate, endDate, entryType),
      categoryTotals: this.categoryTotals(startDate, endDate, entryType),
      dailyTotals: enumerateDates(startDate, endDate).map((date) => ({
        date,
        amountCents: totals.get(date) ?? 0
      }))
    }
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
      GROUP BY e.primary_category_id, e.secondary_category_id, p.name, s.name
      ORDER BY count DESC, MAX(e.spent_date || ' ' || e.spent_time) DESC
      LIMIT 4
    `).all(formatLocalDate(start), formatLocalDate(now), entryType) as FrequentCategory[]
  }

  getSettings(): AppSettings {
    const row = this.db.prepare("SELECT value FROM app_settings WHERE key = 'theme'").get() as { value: ThemeMode } | undefined
    return { theme: row?.value ?? 'system' }
  }

  setTheme(theme: ThemeMode): AppSettings {
    this.db.prepare(`
      INSERT INTO app_settings(key, value) VALUES('theme', ?)
      ON CONFLICT(key) DO UPDATE SET value = excluded.value
    `).run(theme)
    return { theme }
  }

  getBackupPayload(): BackupPayload {
    const rows = this.db.prepare(`
      SELECT id, entry_type, amount_cents, primary_category_id, secondary_category_id,
             spent_date, spent_time, note, created_at, updated_at
      FROM expenses ORDER BY spent_date, spent_time, created_at
    `).all() as Array<Omit<ExpenseRow, 'primary_category_name' | 'secondary_category_name'>>
    const expenses: BackupExpense[] = rows.map((row) => ({
      id: row.id,
      entryType: row.entry_type,
      amountCents: row.amount_cents,
      primaryCategoryId: row.primary_category_id,
      secondaryCategoryId: row.secondary_category_id,
      spentDate: row.spent_date,
      spentTime: row.spent_time,
      note: row.note,
      createdAt: row.created_at,
      updatedAt: row.updated_at
    }))
    return { expenses, settings: this.getSettings() }
  }

  replaceFromBackup(payload: BackupPayload): void {
    for (const expense of payload.expenses) {
      expenseInputSchema.parse(expense)
      this.validateCategoryPair(expense.primaryCategoryId, expense.secondaryCategoryId, expense.entryType)
    }
    const ids = new Set(payload.expenses.map((expense) => expense.id))
    if (ids.size !== payload.expenses.length) throw new Error('备份中存在重复账目编号')

    const insert = this.db.prepare(`
      INSERT INTO expenses(
        id, entry_type, amount_cents, primary_category_id, secondary_category_id,
        spent_date, spent_time, note, created_at, updated_at
      ) VALUES(@id, @entryType, @amountCents, @primaryCategoryId, @secondaryCategoryId,
        @spentDate, @spentTime, @note, @createdAt, @updatedAt)
    `)
    this.db.transaction(() => {
      this.db.prepare('DELETE FROM expenses').run()
      for (const expense of payload.expenses) insert.run(expense)
      this.setTheme(payload.settings.theme)
    })()
  }
}
