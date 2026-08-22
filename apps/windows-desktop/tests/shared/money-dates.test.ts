import { describe, expect, it } from 'vitest'
import { advanceRecurrenceDate, enumerateDates, formatLocalDate, getPresetRange } from '../../src/shared/dates'
import { formatCents, parseYuanToCents } from '../../src/shared/money'

describe('金额处理', () => {
  it('将人民币输入安全转换成整数分', () => {
    expect(parseYuanToCents('12.50')).toBe(1250)
    expect(parseYuanToCents('0.01')).toBe(1)
    expect(parseYuanToCents('1,234.5')).toBe(123450)
    expect(parseYuanToCents('12.345')).toBeNull()
    expect(parseYuanToCents('0')).toBeNull()
    expect(parseYuanToCents('-1')).toBeNull()
  })

  it('始终显示两位小数', () => {
    expect(formatCents(1250)).toBe('¥12.50')
    expect(formatCents(100)).toBe('¥1.00')
  })
})

describe('日期范围', () => {
  const now = new Date(2026, 7, 20, 13, 30)

  it('本周从周一开始', () => {
    expect(getPresetRange('week', now)).toEqual({ startDate: '2026-08-17', endDate: '2026-08-20' })
  })

  it('本月从一号开始且全部不限制日期', () => {
    expect(getPresetRange('month', now)).toEqual({ startDate: '2026-08-01', endDate: '2026-08-20' })
    expect(getPresetRange('all', now)).toEqual({})
  })

  it('枚举日期时可以跨月', () => {
    expect(enumerateDates('2026-07-30', '2026-08-02')).toEqual([
      '2026-07-30', '2026-07-31', '2026-08-01', '2026-08-02'
    ])
    expect(formatLocalDate(now)).toBe('2026-08-20')
  })

  it('周期日期跨过短月份时停在当月最后一天', () => {
    expect(advanceRecurrenceDate('2026-01-31', 'monthly')).toBe('2026-02-28')
    expect(advanceRecurrenceDate('2024-02-29', 'yearly')).toBe('2025-02-28')
    expect(advanceRecurrenceDate('2026-12-31', 'monthly')).toBe('2027-01-31')
  })
})
