import { mkdir, readFile, rename, writeFile } from 'node:fs/promises'
import { basename, dirname, join } from 'node:path'
import { app, BrowserWindow, dialog } from 'electron'
import type { FileOperationResult, RestoreResult } from '../shared/types'
import type { AccountingDatabase } from './database'
import { createBackupDocument, createExpensesCsv, parseBackupDocument } from './data-formats'

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
