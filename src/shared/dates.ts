import type { RangePreset, RecurrenceFrequency } from './types'

const pad = (value: number): string => String(value).padStart(2, '0')

export const formatLocalDate = (date: Date): string =>
  `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`

export const formatLocalTime = (date: Date): string => `${pad(date.getHours())}:${pad(date.getMinutes())}`

export const getPresetRange = (
  preset: RangePreset,
  now = new Date()
): { startDate?: string; endDate?: string } => {
  const endDate = formatLocalDate(now)
  if (preset === 'all') return {}
  if (preset === 'today') return { startDate: endDate, endDate }

  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  if (preset === 'week') {
    const day = start.getDay()
    start.setDate(start.getDate() - (day === 0 ? 6 : day - 1))
  } else if (preset === 'month') {
    start.setDate(1)
  } else {
    start.setMonth(0, 1)
  }
  return { startDate: formatLocalDate(start), endDate }
}

export const getPreviousRange = (startDate: string, endDate: string): { startDate: string; endDate: string } => {
  const [startYear, startMonth, startDay] = startDate.split('-').map(Number)
  const [endYear, endMonth, endDay] = endDate.split('-').map(Number)
  const start = new Date(startYear!, startMonth! - 1, startDay!)
  const end = new Date(endYear!, endMonth! - 1, endDay!)
  const length = Math.round((end.getTime() - start.getTime()) / 86_400_000) + 1
  const previousEnd = new Date(start)
  previousEnd.setDate(previousEnd.getDate() - 1)
  const previousStart = new Date(previousEnd)
  previousStart.setDate(previousStart.getDate() - length + 1)
  return { startDate: formatLocalDate(previousStart), endDate: formatLocalDate(previousEnd) }
}

export const enumerateDates = (startDate: string, endDate: string): string[] => {
  const [startYear, startMonth, startDay] = startDate.split('-').map(Number)
  const [endYear, endMonth, endDay] = endDate.split('-').map(Number)
  const current = new Date(startYear!, startMonth! - 1, startDay!)
  const end = new Date(endYear!, endMonth! - 1, endDay!)
  const dates: string[] = []
  while (current <= end) {
    dates.push(formatLocalDate(current))
    current.setDate(current.getDate() + 1)
  }
  return dates
}

export const advanceRecurrenceDate = (date: string, frequency: Exclude<RecurrenceFrequency, 'none'>): string => {
  const [year, month, day] = date.split('-').map(Number) as [number, number, number]
  if (frequency === 'weekly') {
    const next = new Date(year, month - 1, day)
    next.setDate(next.getDate() + 7)
    return formatLocalDate(next)
  }
  const targetYear = frequency === 'yearly' ? year + 1 : month === 12 ? year + 1 : year
  const targetMonth = frequency === 'yearly' ? month : month === 12 ? 1 : month + 1
  const lastDay = new Date(targetYear, targetMonth, 0).getDate()
  return `${targetYear}-${pad(targetMonth)}-${pad(Math.min(day, lastDay))}`
}

export const formatDisplayDate = (date: string): string => {
  const [year, month, day] = date.split('-').map(Number)
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'long', day: 'numeric', weekday: 'short'
  }).format(new Date(year!, month! - 1, day!))
}
