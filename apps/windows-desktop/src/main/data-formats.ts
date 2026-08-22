import { createHash } from 'node:crypto'
import { backupDocumentSchema } from '../shared/schemas'
import type { BackupDocument, BackupPayload, Expense } from '../shared/types'

export type CsvExpenseRow = {
  entryType: string
  date: string
  time: string
  amount: string
  primaryCategory: string
  secondaryCategory: string
  transactionKind: string
  excludeFromStats: string
  note: string
}

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

export const hashPayload = (payload: unknown): string =>
  createHash('sha256').update(JSON.stringify(canonicalize(payload)), 'utf8').digest('hex')

export const createBackupDocument = (
  payload: BackupPayload,
  appVersion: string,
  exportedAt = new Date().toISOString()
): BackupDocument => {
  const snapshot = structuredClone(payload)
  return {
    magic: 'heima-accounting-backup',
    schemaVersion: 4,
    appVersion,
    exportedAt,
    checksum: hashPayload(snapshot),
    payload: snapshot
  }
}

export const parseBackupDocument = (raw: string): BackupDocument => {
  const parsed = JSON.parse(raw) as Record<string, unknown>
  const rawPayload = parsed.payload
  if (hashPayload(rawPayload) !== parsed.checksum) {
    throw new Error('备份校验失败，文件可能不完整或已被修改')
  }
  if (rawPayload && typeof rawPayload === 'object') {
    const payload = rawPayload as Record<string, unknown>
    const expenses = Array.isArray(payload.expenses) ? payload.expenses : []
    parsed.payload = {
      ...payload,
      categories: Number(parsed.schemaVersion) >= 3 && Array.isArray(payload.categories) ? payload.categories : [],
      expenses: parsed.schemaVersion === 1
        ? expenses.map((expense) => ({ ...(expense as Record<string, unknown>), entryType: 'expense' }))
        : expenses,
      settings: {
        ...(payload.settings as Record<string, unknown>),
        colorTheme: Number(parsed.schemaVersion) >= 3
          ? (payload.settings as Record<string, unknown>)?.colorTheme
          : 'forest'
      },
      budgets: Number(parsed.schemaVersion) >= 4 && Array.isArray(payload.budgets) ? payload.budgets : [],
      templates: Number(parsed.schemaVersion) >= 4 && Array.isArray(payload.templates) ? payload.templates : []
    }
  }
  return backupDocumentSchema.parse(parsed)
}

const csvCell = (value: string | number): string => {
  const text = String(value)
  return /[",\r\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
}

export const createExpensesCsv = (expenses: Expense[]): string => {
  const header = ['类型', '日期', '时间', '金额（元）', '一级分类', '二级分类', '账目性质', '不计入统计', '备注', '创建时间', '更新时间']
  const lines = expenses.map((expense) => [
    expense.entryType === 'income' ? '收入' : '支出',
    expense.spentDate,
    expense.spentTime,
    (expense.amountCents / 100).toFixed(2),
    expense.primaryCategoryName,
    expense.secondaryCategoryName,
    expense.transactionKind === 'refund' ? '退款' : expense.transactionKind === 'reimbursement' ? '报销' : '普通',
    expense.excludeFromStats ? '是' : '否',
    expense.note,
    expense.createdAt,
    expense.updatedAt
  ].map(csvCell).join(','))
  return `\uFEFF${header.map(csvCell).join(',')}\r\n${lines.join('\r\n')}\r\n`
}

const parseCsvTable = (raw: string): string[][] => {
  const rows: string[][] = []
  let row: string[] = []
  let cell = ''
  let quoted = false
  const text = raw.replace(/^\uFEFF/, '')
  for (let index = 0; index < text.length; index += 1) {
    const character = text[index]!
    if (character === '"') {
      if (quoted && text[index + 1] === '"') { cell += '"'; index += 1 } else quoted = !quoted
    } else if (character === ',' && !quoted) {
      row.push(cell); cell = ''
    } else if ((character === '\n' || character === '\r') && !quoted) {
      if (character === '\r' && text[index + 1] === '\n') index += 1
      row.push(cell); cell = ''
      if (row.some((value) => value.trim() !== '')) rows.push(row)
      row = []
    } else cell += character
  }
  if (cell || row.length) { row.push(cell); rows.push(row) }
  if (quoted) throw new Error('CSV 文件中的引号没有正确闭合')
  return rows
}

const headerAliases: Record<keyof CsvExpenseRow, string[]> = {
  entryType: ['类型', '收支类型'],
  date: ['日期', '交易日期'],
  time: ['时间', '交易时间'],
  amount: ['金额（元）', '金额(元)', '金额'],
  primaryCategory: ['一级分类'],
  secondaryCategory: ['二级分类', '分类'],
  transactionKind: ['账目性质'],
  excludeFromStats: ['不计入统计'],
  note: ['备注', '说明']
}

export const parseExpensesCsv = (raw: string): CsvExpenseRow[] => {
  const table = parseCsvTable(raw)
  if (table.length < 2) throw new Error('CSV 文件没有可导入的账目')
  const headers = table[0]!.map((value) => value.trim())
  const indexOf = (key: keyof CsvExpenseRow, required = false): number => {
    const index = headers.findIndex((header) => headerAliases[key].includes(header))
    if (required && index < 0) throw new Error(`CSV 缺少“${headerAliases[key][0]}”列`)
    return index
  }
  const indexes = {
    entryType: indexOf('entryType', true), date: indexOf('date', true), time: indexOf('time'),
    amount: indexOf('amount', true), primaryCategory: indexOf('primaryCategory'),
    secondaryCategory: indexOf('secondaryCategory', true), transactionKind: indexOf('transactionKind'),
    excludeFromStats: indexOf('excludeFromStats'), note: indexOf('note')
  }
  const value = (row: string[], index: number, fallback = ''): string => index < 0 ? fallback : (row[index] ?? '').trim()
  return table.slice(1).map((row) => ({
    entryType: value(row, indexes.entryType), date: value(row, indexes.date), time: value(row, indexes.time, '12:00'),
    amount: value(row, indexes.amount), primaryCategory: value(row, indexes.primaryCategory),
    secondaryCategory: value(row, indexes.secondaryCategory), transactionKind: value(row, indexes.transactionKind, '普通'),
    excludeFromStats: value(row, indexes.excludeFromStats, '否'), note: value(row, indexes.note)
  }))
}
