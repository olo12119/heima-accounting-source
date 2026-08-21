import { describe, expect, it } from 'vitest'
import { createBackupDocument, createExpensesCsv, parseBackupDocument } from '../../src/main/data-formats'
import type { BackupPayload, Expense } from '../../src/shared/types'

const payload: BackupPayload = {
  expenses: [{
    id: '2ef99495-7651-493c-a0d6-aaa0cc968523',
    entryType: 'expense',
    amountCents: 1250,
    primaryCategoryId: 'food',
    secondaryCategoryId: 'food.meal',
    spentDate: '2026-08-20',
    spentTime: '12:30',
    note: '午餐',
    createdAt: '2026-08-20T04:30:00.000Z',
    updatedAt: '2026-08-20T04:30:00.000Z'
  }],
  settings: { theme: 'system' }
}

describe('完整备份格式', () => {
  it('生成可校验和可恢复的版本化备份', () => {
    const document = createBackupDocument(payload, '1.0.0', '2026-08-20T05:00:00.000Z')
    expect(document.checksum).toMatch(/^[a-f0-9]{64}$/)
    expect(parseBackupDocument(JSON.stringify(document)).payload).toEqual(payload)
  })

  it('拒绝被修改过的备份', () => {
    const document = createBackupDocument(payload, '1.0.0')
    document.payload.expenses[0]!.amountCents = 999999
    expect(() => parseBackupDocument(JSON.stringify(document))).toThrow('备份校验失败')
  })

  it('兼容没有收支类型的第1版备份并按支出恢复', () => {
    const legacyPayload = structuredClone(payload) as unknown as { expenses: Array<Record<string, unknown>>; settings: { theme: string } }
    delete legacyPayload.expenses[0]!.entryType
    const legacyDocument = {
      magic: 'heima-accounting-backup', schemaVersion: 1, appVersion: '1.0.0',
      exportedAt: '2026-08-20T05:00:00.000Z', checksum: '', payload: legacyPayload
    }
    legacyDocument.checksum = createBackupDocument(legacyPayload as unknown as BackupPayload, '1.0.0').checksum
    expect(parseBackupDocument(JSON.stringify(legacyDocument)).payload.expenses[0]?.entryType).toBe('expense')
  })
})

describe('CSV 格式', () => {
  it('包含 UTF-8 BOM、中文表头、两位金额并正确转义', () => {
    const expense: Expense = {
      ...payload.expenses[0]!,
      primaryCategoryName: '餐饮',
      secondaryCategoryName: '正餐',
      note: '午餐,"套餐"'
    }
    const csv = createExpensesCsv([expense])
    expect(csv.startsWith('\uFEFF类型,日期,时间,金额（元）')).toBe(true)
    expect(csv).toContain('支出,2026-08-20')
    expect(csv).toContain('12.50')
    expect(csv).toContain('"午餐,""套餐"""')
  })
})
