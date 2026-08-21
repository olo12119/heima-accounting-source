import { z } from 'zod'
import { CATEGORY_COLOR_CHOICES, CATEGORY_ICON_NAMES } from './categories'

export const rangePresetSchema = z.enum(['today', 'week', 'month', 'all'])
export const statisticsPresetSchema = z.enum(['today', 'week', 'month'])
export const themeModeSchema = z.enum(['light', 'dark', 'system'])
export const colorThemeSchema = z.enum(['forest', 'ocean', 'amber', 'wisteria'])
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
export const categoryIdSchema = z.string().trim().min(1).max(80)
export const customCategoryIdSchema = z.string().regex(/^custom\.[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i)
export const categoryNameSchema = z.string().trim().min(1, '请输入分类名称').max(16, '分类名称最多16个字')
export const categoryIconSchema = z.enum(CATEGORY_ICON_NAMES)
export const categoryColorSchema = z.enum(CATEGORY_COLOR_CHOICES)

export const customPrimaryCategoryInputSchema = z.object({
  entryType: entryTypeSchema,
  name: categoryNameSchema,
  firstSecondaryName: categoryNameSchema,
  icon: categoryIconSchema,
  color: categoryColorSchema
})

export const customSecondaryCategoryInputSchema = z.object({
  parentId: categoryIdSchema,
  name: categoryNameSchema
})

export const customCategoryUpdateSchema = z.object({
  name: categoryNameSchema,
  icon: categoryIconSchema.optional(),
  color: categoryColorSchema.optional()
})

export const categoryOrderDirectionSchema = z.enum(['up', 'down'])

export const backupExpenseSchema = expenseInputSchema.extend({
  id: expenseIdSchema,
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime()
})

export const appSettingsSchema = z.object({ theme: themeModeSchema, colorTheme: colorThemeSchema })

export const backupCategorySchema = z.object({
  id: customCategoryIdSchema,
  parentId: categoryIdSchema.nullable(),
  name: categoryNameSchema,
  icon: categoryIconSchema,
  color: categoryColorSchema,
  sortOrder: z.number().int().min(0).max(1_000_000),
  entryType: entryTypeSchema,
  isActive: z.boolean()
})

export const backupPayloadSchema = z.object({
  categories: z.array(backupCategorySchema).max(10_000),
  expenses: z.array(backupExpenseSchema).max(1_000_000),
  settings: appSettingsSchema
})

export const backupDocumentSchema = z.object({
  magic: z.literal('heima-accounting-backup'),
  schemaVersion: z.union([z.literal(1), z.literal(2), z.literal(3)]),
  appVersion: z.string().min(1),
  exportedAt: z.string().datetime(),
  checksum: z.string().regex(/^[a-f0-9]{64}$/),
  payload: backupPayloadSchema
})
