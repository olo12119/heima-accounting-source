import type { BrowserWindow, IpcMainInvokeEvent } from 'electron'
import { ipcMain } from 'electron'
import { z } from 'zod'
import {
  categoryOrderDirectionSchema,
  budgetInputSchema,
  colorThemeSchema,
  customCategoryIdSchema,
  customCategoryUpdateSchema,
  customPrimaryCategoryInputSchema,
  customSecondaryCategoryInputSchema,
  entryTypeFilterSchema,
  entryTypeSchema,
  expenseIdSchema,
  expenseInputSchema,
  expenseQuerySchema,
  monthSchema,
  pinSchema,
  rangePresetSchema,
  statisticsPresetSchema,
  statisticsQuerySchema,
  transactionTemplateInputSchema,
  themeModeSchema
} from '../shared/schemas'
import type { AppStatus } from '../shared/types'
import type { AccountingDatabase } from './database'
import { exportBackup, exportCsv, importCsv, restoreBackup } from './portability'

type IpcDependencies = {
  getDatabase: () => AccountingDatabase | null
  getWindow: () => BrowserWindow | null
  getStatus: () => AppStatus
  userDataPath: string
}

const assertTrustedSender = (event: IpcMainInvokeEvent): void => {
  const url = event.senderFrame?.url ?? ''
  const trusted = process.env.NODE_ENV === 'development'
    ? /^https?:\/\/(?:localhost|127\.0\.0\.1)(?::\d+)?\//.test(url)
    : url.startsWith('file://')
  if (!trusted) throw new Error('已拒绝来自非应用页面的请求')
}

export const registerIpcHandlers = (dependencies: IpcDependencies): void => {
  const handle = <TArgs extends unknown[], TResult>(
    channel: string,
    callback: (database: AccountingDatabase, ...args: TArgs) => TResult | Promise<TResult>,
    requiresUnlock = true
  ): void => {
    ipcMain.handle(channel, async (event, ...args: TArgs) => {
      assertTrustedSender(event)
      const database = dependencies.getDatabase()
      if (!database) throw new Error(dependencies.getStatus().error ?? '数据库尚未就绪')
      if (requiresUnlock) database.assertUnlocked()
      return callback(database, ...args)
    })
  }

  ipcMain.handle('system:get-status', (event) => {
    assertTrustedSender(event)
    return dependencies.getStatus()
  })
  handle('system:get-lock-status', (database) => database.getLockStatus(), false)
  handle('system:unlock', (database, pin: unknown) => database.unlock(pinSchema.parse(pin)), false)
  handle('system:set-lock-pin', (database, currentPin: unknown, newPin: unknown) =>
    database.setLockPin(z.union([pinSchema, z.null()]).parse(currentPin), z.union([pinSchema, z.literal(''), z.null()]).parse(newPin)))
  handle('categories:list', (database) => database.getCategories())
  handle('categories:manage-list', (database) => database.getCategoriesForManagement())
  handle('categories:frequent', (database, entryType: unknown) => database.getFrequentCategories(entryTypeSchema.parse(entryType)))
  handle('categories:create-primary', (database, input: unknown) =>
    database.createCustomPrimaryCategory(customPrimaryCategoryInputSchema.parse(input)))
  handle('categories:create-secondary', (database, input: unknown) =>
    database.createCustomSecondaryCategory(customSecondaryCategoryInputSchema.parse(input)))
  handle('categories:update', (database, id: unknown, input: unknown) =>
    database.updateCustomCategory(customCategoryIdSchema.parse(id), customCategoryUpdateSchema.parse(input)))
  handle('categories:set-active', (database, id: unknown, active: unknown) =>
    database.setCustomCategoryActive(customCategoryIdSchema.parse(id), z.boolean().parse(active)))
  handle('categories:delete', (database, id: unknown) =>
    database.deleteCustomCategory(customCategoryIdSchema.parse(id)))
  handle('categories:reorder', (database, id: unknown, direction: unknown) =>
    database.reorderCustomCategory(customCategoryIdSchema.parse(id), categoryOrderDirectionSchema.parse(direction)))
  handle('expenses:list', (database, preset: unknown, entryType: unknown = 'all') =>
    database.listExpenses(rangePresetSchema.parse(preset), entryTypeFilterSchema.parse(entryType)))
  handle('expenses:search', (database, query: unknown) => database.searchExpenses(expenseQuerySchema.parse(query)))
  handle('expenses:create', (database, input: unknown) => database.createExpense(expenseInputSchema.parse(input)))
  handle('expenses:update', (database, id: unknown, input: unknown) =>
    database.updateExpense(expenseIdSchema.parse(id), expenseInputSchema.parse(input)))
  handle('expenses:delete', (database, id: unknown) => database.deleteExpense(expenseIdSchema.parse(id)))
  handle('dashboard:get', (database) => database.getDashboard())
  handle('statistics:get', (database, preset: unknown, entryType: unknown) =>
    database.getStatistics(statisticsPresetSchema.parse(preset), entryTypeSchema.parse(entryType)))
  handle('statistics:query', (database, query: unknown) => database.queryStatistics(statisticsQuerySchema.parse(query)))
  handle('calendar:get-month', (database, month: unknown) => database.getCalendarMonth(monthSchema.parse(month)))
  handle('budgets:get', (database, month: unknown) => database.getBudget(monthSchema.parse(month)))
  handle('budgets:save', (database, input: unknown) => database.saveBudget(budgetInputSchema.parse(input)))
  handle('templates:list', (database) => database.listTemplates())
  handle('templates:save', (database, input: unknown) => database.saveTemplate(transactionTemplateInputSchema.parse(input)))
  handle('templates:delete', (database, id: unknown) => database.deleteTemplate(expenseIdSchema.parse(id)))
  handle('templates:apply', (database, id: unknown, spentDate: unknown) =>
    database.applyTemplate(expenseIdSchema.parse(id), z.string().optional().parse(spentDate)))
  handle('settings:get', (database) => database.getSettings())
  handle('settings:set-theme', (database, theme: unknown) => database.setTheme(themeModeSchema.parse(theme)))
  handle('settings:set-color-theme', (database, colorTheme: unknown) => database.setColorTheme(colorThemeSchema.parse(colorTheme)))
  handle('data:export-csv', async (database) => {
    const window = dependencies.getWindow()
    if (!window) throw new Error('应用窗口不可用')
    return exportCsv(window, database)
  })
  handle('data:export-backup', async (database) => {
    const window = dependencies.getWindow()
    if (!window) throw new Error('应用窗口不可用')
    return exportBackup(window, database)
  })
  handle('data:restore-backup', async (database) => {
    const window = dependencies.getWindow()
    if (!window) throw new Error('应用窗口不可用')
    return restoreBackup(window, database, dependencies.userDataPath)
  })
  handle('data:import-csv', async (database) => {
    const window = dependencies.getWindow()
    if (!window) throw new Error('应用窗口不可用')
    return importCsv(window, database, dependencies.userDataPath)
  })
}
