import { createHash } from 'node:crypto'
import { backupDocumentSchema } from '../shared/schemas'
import type { BackupDocument, BackupPayload, Expense } from '../shared/types'

const canonicalize = (value: unknown): unknown => {
  if (Array.isArray(value)) return value.map(canonicalize)
  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value as Record<string, unknown>)
        .sort(([left], [right]) => left.localeCompare(right))
        .map(([key, entry]) => [key, canonicalize(entry)])
    )
  }
  return value
}

export const hashPayload = (payload: BackupPayload): string =>
  createHash('sha256').update(JSON.stringify(canonicalize(payload)), 'utf8').digest('hex')

export const createBackupDocument = (
  payload: BackupPayload,
  appVersion: string,
  exportedAt = new Date().toISOString()
): BackupDocument => {
  const snapshot = structuredClone(payload)
  return {
    magic: 'heima-accounting-backup',
    schemaVersion: 2,
    appVersion,
    exportedAt,
    checksum: hashPayload(snapshot),
    payload: snapshot
  }
}

export const parseBackupDocument = (raw: string): BackupDocument => {
  const parsed = JSON.parse(raw) as Record<string, unknown>
  const rawPayload = parsed.payload as BackupPayload
  if (hashPayload(rawPayload) !== parsed.checksum) {
    throw new Error('备份校验失败，文件可能不完整或已被修改')
  }
  if (parsed.schemaVersion === 1 && rawPayload && Array.isArray(rawPayload.expenses)) {
    parsed.payload = {
      ...rawPayload,
      expenses: rawPayload.expenses.map((expense) => ({ ...(expense as unknown as Record<string, unknown>), entryType: 'expense' }))
    }
  }
  return backupDocumentSchema.parse(parsed)
}

const csvCell = (value: string | number): string => {
  const text = String(value)
  return /[",\r\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
}

export const createExpensesCsv = (expenses: Expense[]): string => {
  const header = ['类型', '日期', '时间', '金额（元）', '一级分类', '二级分类', '备注', '创建时间', '更新时间']
  const lines = expenses.map((expense) => [
    expense.entryType === 'income' ? '收入' : '支出',
    expense.spentDate,
    expense.spentTime,
    (expense.amountCents / 100).toFixed(2),
    expense.primaryCategoryName,
    expense.secondaryCategoryName,
    expense.note,
    expense.createdAt,
    expense.updatedAt
  ].map(csvCell).join(','))
  return `\uFEFF${header.map(csvCell).join(',')}\r\n${lines.join('\r\n')}\r\n`
}
