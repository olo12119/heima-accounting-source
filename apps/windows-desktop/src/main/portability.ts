import { mkdir, readFile, rename, writeFile } from 'node:fs/promises'
import { basename, dirname, join } from 'node:path'
import { app, BrowserWindow, dialog } from 'electron'
import type { CsvImportResult, EntryType, ExpenseInput, FileOperationResult, RestoreResult } from '../shared/types'
import type { AccountingDatabase } from './database'
import { createBackupDocument, createExpensesCsv, parseBackupDocument, parseExpensesCsv } from './data-formats'
import { parseYuanToCents } from '../shared/money'

const timestamp = (): string => new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19)

const atomicWrite = async (targetPath: string, content: string | Uint8Array): Promise<void> => {
  const temporaryPath = join(dirname(targetPath), `.${basename(targetPath)}.${process.pid}.tmp`)
  await writeFile(temporaryPath, content)
  await rename(temporaryPath, targetPath)
}

export const exportCsv = async (
  window: BrowserWindow,
  database: AccountingDatabase
): Promise<FileOperationResult> => {
  const expenses = database.listExpenses('all')
  const defaultName = `黑马记账-账目-${new Date().toISOString().slice(0, 10)}.csv`
  const testDirectory = process.env.HEIMA_TEST_EXPORT_DIR
  const result = testDirectory
    ? { canceled: false, filePath: join(testDirectory, defaultName) }
    : await dialog.showSaveDialog(window, {
      title: '导出账目 CSV',
      defaultPath: defaultName,
      filters: [{ name: 'CSV 表格', extensions: ['csv'] }]
    })
  if (result.canceled || !result.filePath) return { canceled: true }
  await mkdir(dirname(result.filePath), { recursive: true })
  await atomicWrite(result.filePath, createExpensesCsv(expenses))
  return { canceled: false, path: result.filePath, count: expenses.length }
}

export const exportBackup = async (
  window: BrowserWindow,
  database: AccountingDatabase
): Promise<FileOperationResult> => {
  const payload = database.getBackupPayload()
  const defaultName = `黑马记账-完整备份-${timestamp()}.heima-backup.json`
  const testDirectory = process.env.HEIMA_TEST_EXPORT_DIR
  const result = testDirectory
    ? { canceled: false, filePath: join(testDirectory, defaultName) }
    : await dialog.showSaveDialog(window, {
      title: '导出完整备份',
      defaultPath: defaultName,
      filters: [{ name: '黑马记账备份', extensions: ['heima-backup.json', 'json'] }]
    })
  if (result.canceled || !result.filePath) return { canceled: true }
  await mkdir(dirname(result.filePath), { recursive: true })
  await atomicWrite(result.filePath, JSON.stringify(createBackupDocument(payload, app.getVersion()), null, 2))
  return { canceled: false, path: result.filePath, count: payload.expenses.length }
}

export const restoreBackup = async (
  window: BrowserWindow,
  database: AccountingDatabase,
  userDataPath: string
): Promise<RestoreResult> => {
  const testRestorePath = process.env.HEIMA_TEST_RESTORE_PATH
  const result = testRestorePath
    ? { canceled: false, filePaths: [testRestorePath] }
    : await dialog.showOpenDialog(window, {
      title: '选择黑马记账备份',
      properties: ['openFile'],
      filters: [{ name: '黑马记账备份', extensions: ['json'] }]
    })
  if (result.canceled || result.filePaths.length === 0) return { canceled: true }

  const raw = await readFile(result.filePaths[0]!, 'utf8')
  const document = parseBackupDocument(raw)

  const safetyDirectory = join(userDataPath, 'backups')
  await mkdir(safetyDirectory, { recursive: true })
  const safetyBackupPath = join(safetyDirectory, `恢复前自动备份-${timestamp()}.heima-backup.json`)
  const currentPayload = database.getBackupPayload()
  await atomicWrite(safetyBackupPath, JSON.stringify(createBackupDocument(currentPayload, app.getVersion()), null, 2))
  database.replaceFromBackup(document.payload)
  return {
    canceled: false,
    path: result.filePaths[0],
    count: document.payload.expenses.length,
    safetyBackupPath
  }
}

export const importCsv = async (
  window: BrowserWindow,
  database: AccountingDatabase,
  userDataPath: string
): Promise<CsvImportResult> => {
  const testPath = process.env.HEIMA_TEST_IMPORT_PATH
  const selected = testPath
    ? { canceled: false, filePaths: [testPath] }
    : await dialog.showOpenDialog(window, {
      title: '选择要导入的 CSV 账单',
      properties: ['openFile'],
      filters: [{ name: 'CSV 表格', extensions: ['csv'] }]
    })
  if (selected.canceled || selected.filePaths.length === 0) return { canceled: true }

  const filePath = selected.filePaths[0]!
  const rows = parseExpensesCsv(await readFile(filePath, 'utf8'))
  const categories = database.getCategories()
  const inputs: ExpenseInput[] = []
  let invalidCount = 0
  for (const row of rows) {
    try {
      const explicitType: EntryType | null = row.entryType === '收入' || row.entryType.toLowerCase() === 'income'
        ? 'income' : row.entryType === '支出' || row.entryType.toLowerCase() === 'expense' ? 'expense' : null
      const numeric = row.amount.replace(/[¥￥,\s]/g, '')
      const amountCents = parseYuanToCents(numeric.replace(/^-/, ''))
      const entryType = explicitType ?? (numeric.startsWith('-') ? 'expense' : 'income')
      if (!amountCents) throw new Error('金额无效')
      const secondaryMatches = categories.filter((category) =>
        category.parentId !== null && category.entryType === entryType && category.name === row.secondaryCategory)
      const secondary = row.primaryCategory
        ? secondaryMatches.find((category) => categories.find((parent) => parent.id === category.parentId)?.name === row.primaryCategory)
        : secondaryMatches.length === 1 ? secondaryMatches[0] : undefined
      if (!secondary) throw new Error('分类不存在或名称不唯一')
      const primary = categories.find((category) => category.id === secondary.parentId)
      if (!primary) throw new Error('一级分类不存在')
      if (!/^\d{4}-\d{2}-\d{2}$/.test(row.date)) throw new Error('日期无效')
      const time = /^(?:[01]\d|2[0-3]):[0-5]\d$/.test(row.time) ? row.time : '12:00'
      if (row.transactionKind && row.transactionKind !== '普通') throw new Error('退款或报销请使用完整备份恢复，以保留原账关联')
      inputs.push({
        entryType, amountCents, primaryCategoryId: primary.id, secondaryCategoryId: secondary.id,
        spentDate: row.date, spentTime: time, note: row.note.slice(0, 200), transactionKind: 'regular',
        excludeFromStats: row.excludeFromStats === '是', linkedExpenseId: null
      })
    } catch { invalidCount += 1 }
  }
  if (inputs.length === 0) throw new Error(`没有可导入的有效账目；发现 ${invalidCount} 行无法识别`)

  if (!testPath) {
    const confirmation = await dialog.showMessageBox(window, {
      type: 'question',
      buttons: ['取消', '确认导入'],
      defaultId: 0,
      cancelId: 0,
      title: '确认导入账目',
      message: `准备导入 ${inputs.length} 笔账目`,
      detail: `${invalidCount > 0 ? `另有 ${invalidCount} 行无法识别，将跳过。\n` : ''}导入前会自动保存当前完整备份，并跳过重复账目。`
    })
    if (confirmation.response !== 1) return { canceled: true }
  }

  const safetyDirectory = join(userDataPath, 'backups')
  await mkdir(safetyDirectory, { recursive: true })
  const safetyBackupPath = join(safetyDirectory, `导入前自动备份-${timestamp()}.heima-backup.json`)
  await atomicWrite(safetyBackupPath, JSON.stringify(createBackupDocument(database.getBackupPayload(), app.getVersion()), null, 2))
  const result = database.importExpenses(inputs)
  return { canceled: false, path: filePath, ...result, invalidCount, safetyBackupPath }
}
