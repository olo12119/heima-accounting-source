import { mkdirSync, mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import Sqlite from 'better-sqlite3'
import { AccountingDatabase } from '../../src/main/database'
import { formatLocalDate, formatLocalTime } from '../../src/shared/dates'
import type { ExpenseInput } from '../../src/shared/types'

const temporaryDirectories: string[] = []
const createDatabase = (): { database: AccountingDatabase; path: string; directory: string } => {
  const directory = mkdtempSync(join(tmpdir(), 'heima-accounting-test-'))
  temporaryDirectories.push(directory)
  const path = join(directory, 'data', 'test.sqlite3')
  return { database: new AccountingDatabase(path), path, directory }
}

const todayInput = (overrides: Partial<ExpenseInput> = {}): ExpenseInput => {
  const now = new Date()
  return {
    entryType: 'expense',
    amountCents: 1250,
    primaryCategoryId: 'food',
    secondaryCategoryId: 'food.meal',
    spentDate: formatLocalDate(now),
    spentTime: formatLocalTime(now),
    note: '午餐',
    ...overrides
  }
}

afterEach(() => {
  for (const directory of temporaryDirectories.splice(0)) rmSync(directory, { recursive: true, force: true })
})

describe('SQLite 账本', () => {
  it('初始化完整的两级分类', () => {
    const { database } = createDatabase()
    const categories = database.getCategories()
    expect(categories.filter((category) => category.parentId === null && category.entryType === 'expense')).toHaveLength(11)
    expect(categories.filter((category) => category.parentId === null && category.entryType === 'income')).toHaveLength(7)
    expect(categories.find((category) => category.id === 'food.meal')?.parentId).toBe('food')
    database.close()
  })

  it('允许创建和编辑自定义分类，但锁定系统预设分类', () => {
    const { database } = createDatabase()
    const primary = database.createCustomPrimaryCategory({
      entryType: 'expense', name: '家庭生活', firstSecondaryName: '家庭日用', icon: 'house', color: '#5579a7'
    })
    expect(primary.isSystem).toBe(false)
    const firstChild = database.getCategoriesForManagement().find((category) => category.parentId === primary.id)
    expect(firstChild?.name).toBe('家庭日用')

    const secondary = database.createCustomSecondaryCategory({ parentId: 'food', name: '公司食堂' })
    expect(secondary.parentId).toBe('food')
    expect(secondary.isSystem).toBe(false)

    const updated = database.updateCustomCategory(primary.id, { name: '家庭开销', icon: 'wallet', color: '#7667b8' })
    expect(updated.name).toBe('家庭开销')
    expect(updated.icon).toBe('wallet')
    expect(database.getCategoriesForManagement().find((category) => category.id === firstChild?.id)?.color).toBe('#7667b8')
    expect(() => database.updateCustomCategory('food', { name: '吃饭' })).toThrow('系统预设分类不能修改')
    database.close()
  })

  it('删除未使用的自定义分类，并安全停用已有账目的分类', () => {
    const { database } = createDatabase()
    const unused = database.createCustomPrimaryCategory({
      entryType: 'expense', name: '临时分类', firstSecondaryName: '临时二级', icon: 'shapes', color: '#7c8580'
    })
    expect(database.deleteCustomCategory(unused.id).mode).toBe('deleted')
    expect(database.getCategoriesForManagement().some((category) => category.id === unused.id)).toBe(false)

    const used = database.createCustomPrimaryCategory({
      entryType: 'expense', name: '家庭生活', firstSecondaryName: '家庭日用', icon: 'house', color: '#5579a7'
    })
    const child = database.getCategories().find((category) => category.parentId === used.id)!
    database.createExpense(todayInput({ primaryCategoryId: used.id, secondaryCategoryId: child.id, note: '家庭采购' }))
    expect(database.deleteCustomCategory(used.id).mode).toBe('deactivated')
    expect(database.getCategories().some((category) => category.id === used.id)).toBe(false)
    expect(database.listExpenses('all')[0]?.primaryCategoryName).toBe('家庭生活')
    expect(database.getCategoriesForManagement().find((category) => category.id === used.id)?.isActive).toBe(false)
    database.close()
  })

  it('只在自定义同级分类之间调整顺序', () => {
    const { database } = createDatabase()
    const first = database.createCustomPrimaryCategory({
      entryType: 'income', name: '自定义甲', firstSecondaryName: '甲明细', icon: 'wallet', color: '#2d9b72'
    })
    const second = database.createCustomPrimaryCategory({
      entryType: 'income', name: '自定义乙', firstSecondaryName: '乙明细', icon: 'hand-coins', color: '#3f8c88'
    })
    database.reorderCustomCategory(second.id, 'up')
    const customPrimaries = database.getCategoriesForManagement().filter((category) =>
      !category.isSystem && category.parentId === null && category.entryType === 'income')
    expect(customPrimaries.map((category) => category.id)).toEqual([second.id, first.id])
    database.close()
  })

  it('新增、修改、筛选和删除账目', () => {
    const { database } = createDatabase()
    const created = database.createExpense(todayInput())
    expect(created.amountCents).toBe(1250)
    expect(database.listExpenses('today')).toHaveLength(1)

    const updated = database.updateExpense(created.id, todayInput({ amountCents: 2088, note: '聚餐' }))
    expect(updated.amountCents).toBe(2088)
    expect(updated.note).toBe('聚餐')

    database.deleteExpense(created.id)
    expect(database.listExpenses('all')).toHaveLength(0)
    database.close()
  })

  it('拒绝不匹配的两级分类和无效金额', () => {
    const { database } = createDatabase()
    expect(() => database.createExpense(todayInput({ secondaryCategoryId: 'transport.taxi' }))).toThrow('不匹配')
    expect(() => database.createExpense(todayInput({ amountCents: 0 }))).toThrow()
    database.close()
  })

  it('正确汇总首页、今日、本周和本月统计', () => {
    const { database } = createDatabase()
    database.createExpense(todayInput({ amountCents: 1250 }))
    database.createExpense(todayInput({ amountCents: 800, primaryCategoryId: 'transport', secondaryCategoryId: 'transport.public', note: '地铁' }))
    expect(database.getDashboard().todayExpenseCents).toBe(2050)
    expect(database.getStatistics('today', 'expense').totalCents).toBe(2050)
    expect(database.getStatistics('week', 'expense').categoryTotals).toHaveLength(2)
    expect(database.getStatistics('month', 'expense').dailyTotals.some((day) => day.amountCents === 2050)).toBe(true)
    database.close()
  })

  it('记录收入并分别汇总收支与结余', () => {
    const { database } = createDatabase()
    database.createExpense(todayInput({ amountCents: 3000 }))
    database.createExpense(todayInput({
      entryType: 'income', amountCents: 100000, primaryCategoryId: 'salary', secondaryCategoryId: 'salary.base', note: '工资'
    }))
    const dashboard = database.getDashboard()
    expect(dashboard.todayExpenseCents).toBe(3000)
    expect(dashboard.todayIncomeCents).toBe(100000)
    expect(dashboard.monthBalanceCents).toBe(97000)
    expect(database.listExpenses('all', 'income')).toHaveLength(1)
    expect(database.getStatistics('month', 'income').categoryTotals[0]?.categoryName).toBe('工资薪酬')
    database.close()
  })

  it('关闭再打开后数据仍然存在', () => {
    const { database, path } = createDatabase()
    database.createExpense(todayInput())
    database.close()
    const reopened = new AccountingDatabase(path)
    expect(reopened.listExpenses('all')).toHaveLength(1)
    reopened.close()
  })

  it('从第1版数据库迁移时完整保留旧支出', () => {
    const directory = mkdtempSync(join(tmpdir(), 'heima-accounting-test-'))
    temporaryDirectories.push(directory)
    const path = join(directory, 'data', 'legacy.sqlite3')
    mkdirSync(join(directory, 'data'))
    const legacy = new Sqlite(path)
    legacy.exec(`
      CREATE TABLE categories (id TEXT PRIMARY KEY, parent_id TEXT REFERENCES categories(id), name TEXT NOT NULL, icon TEXT NOT NULL, sort_order INTEGER NOT NULL, is_system INTEGER NOT NULL DEFAULT 1, is_active INTEGER NOT NULL DEFAULT 1);
      CREATE TABLE expenses (id TEXT PRIMARY KEY, amount_cents INTEGER NOT NULL, primary_category_id TEXT NOT NULL REFERENCES categories(id), secondary_category_id TEXT NOT NULL REFERENCES categories(id), spent_date TEXT NOT NULL, spent_time TEXT NOT NULL, note TEXT NOT NULL DEFAULT '', created_at TEXT NOT NULL, updated_at TEXT NOT NULL);
      INSERT INTO categories VALUES ('food', NULL, '餐饮', 'utensils', 0, 1, 1);
      INSERT INTO categories VALUES ('food.meal', 'food', '正餐', 'utensils', 0, 1, 1);
      INSERT INTO expenses VALUES ('2ef99495-7651-493c-a0d6-aaa0cc968523', 1250, 'food', 'food.meal', '${formatLocalDate(new Date())}', '12:30', '旧账目', '2026-08-20T04:30:00.000Z', '2026-08-20T04:30:00.000Z');
    `)
    legacy.close()
    const migrated = new AccountingDatabase(path)
    const entry = migrated.listExpenses('all')[0]
    expect(entry?.entryType).toBe('expense')
    expect(entry?.amountCents).toBe(1250)
    expect(entry?.note).toBe('旧账目')
    migrated.close()
  })

  it('完整备份恢复以事务替换数据和设置', () => {
    const { database } = createDatabase()
    const original = database.createExpense(todayInput())
    database.setTheme('dark')
    database.setColorTheme('ocean')
    const backup = database.getBackupPayload()
    database.createExpense(todayInput({ amountCents: 999 }))
    database.setTheme('light')
    database.replaceFromBackup(backup)
    expect(database.listExpenses('all').map((expense) => expense.id)).toEqual([original.id])
    expect(database.getSettings().theme).toBe('dark')
    expect(database.getSettings().colorTheme).toBe('ocean')
    database.close()
  })

  it('完整备份包含自定义分类，错误恢复会整体回滚', () => {
    const { database } = createDatabase()
    const primary = database.createCustomPrimaryCategory({
      entryType: 'expense', name: '家庭生活', firstSecondaryName: '家庭日用', icon: 'house', color: '#5579a7'
    })
    const secondary = database.getCategories().find((category) => category.parentId === primary.id)!
    const original = database.createExpense(todayInput({ primaryCategoryId: primary.id, secondaryCategoryId: secondary.id }))
    const backup = database.getBackupPayload()
    expect(backup.categories).toHaveLength(2)

    database.createExpense(todayInput({ amountCents: 999 }))
    database.replaceFromBackup(backup)
    expect(database.listExpenses('all').map((expense) => expense.id)).toEqual([original.id])
    expect(database.getCategories().some((category) => category.id === primary.id)).toBe(true)

    const invalid = structuredClone(backup)
    invalid.expenses[0]!.secondaryCategoryId = 'missing.category'
    expect(() => database.replaceFromBackup(invalid)).toThrow('不匹配')
    expect(database.listExpenses('all').map((expense) => expense.id)).toEqual([original.id])
    expect(database.getCategories().some((category) => category.id === primary.id)).toBe(true)
    database.close()
  })
})
