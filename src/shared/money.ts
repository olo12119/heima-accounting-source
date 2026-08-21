export const parseYuanToCents = (value: string): number | null => {
  const normalized = value.trim().replace(/,/g, '')
  if (!/^(?:0|[1-9]\d*)(?:\.\d{1,2})?$/.test(normalized)) return null
  const [yuan = '0', fraction = ''] = normalized.split('.')
  const cents = Number(yuan) * 100 + Number(fraction.padEnd(2, '0'))
  if (!Number.isSafeInteger(cents) || cents <= 0 || cents > 999_999_999) return null
  return cents
}

export const formatCents = (cents: number, showSymbol = true): string => {
  const formatted = new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(cents / 100)
  return showSymbol ? `¥${formatted}` : formatted
}

export const centsToInput = (cents: number): string => (cents / 100).toFixed(2)
