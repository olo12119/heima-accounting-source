import { z } from 'zod'

export const rangePresetSchema = z.enum(['today', 'week', 'month', 'all'])
export const statisticsPresetSchema = z.enum(['today', 'week', 'month'])
export const themeModeSchema = z.enum(['light', 'dark', 'system'])
export const entryTypeSchema = z.enum(['expense', 'income'])
export const entryTypeFilterSchema = z.enum(['expense', 'income', 'all'])

const validDate = (value: string): boolean => {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false
  const [year, month, day] = value.split('-').map(Number)
  const date = new Date(year!, month! - 1, day!)
  return date.getFullYear() === year && date.getMonth() === month! - 1 && date.getDate() === day
}

export const expenseInputSchema = z.object({
  entryType: entryTypeSchema,
  amountCents: z.number().int().positive().max(999_999_999),
  primaryCategoryId: z.string().min(1).max(80),
  secondaryCategoryId: z.string().min(1).max(80),
  spentDate: z.string().refine(validDate, '日期无效'),
  spentTime: z.string().regex(/^(?:[01]\d|2[0-3]):[0-5]\d$/, '时间无效'),
  note: z.string().trim().max(200)
})

export const expenseIdSchema = z.string().uuid()

export const backupExpenseSchema = expenseInputSchema.extend({
  id: expenseIdSchema,
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime()
})

export const appSettingsSchema = z.object({ theme: themeModeSchema })

export const backupPayloadSchema = z.object({
  expenses: z.array(backupExpenseSchema).max(1_000_000),
  settings: appSettingsSchema
})

export const backupDocumentSchema = z.object({
  magic: z.literal('heima-accounting-backup'),
  schemaVersion: z.union([z.literal(1), z.literal(2)]),
  appVersion: z.string().min(1),
  exportedAt: z.string().datetime(),
  checksum: z.string().regex(/^[a-f0-9]{64}$/),
  payload: backupPayloadSchema
})
