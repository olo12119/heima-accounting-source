import type { RangePreset } from './types'

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
  } else {
    start.setDate(1)
  }
  return { startDate: formatLocalDate(start), endDate }
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

export const formatDisplayDate = (date: string): string => {
  const [year, month, day] = date.split('-').map(Number)
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'long', day: 'numeric', weekday: 'short'
  }).format(new Date(year!, month! - 1, day!))
}
