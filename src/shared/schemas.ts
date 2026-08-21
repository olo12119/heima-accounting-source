import { z } from 'zod'
import { CATEGORY_COLOR_CHOICES, CATEGORY_ICON_NAMES } from './categories'

export const rangePresetSchema = z.enum(['today', 'week', 'month', 'year', 'all'])
export const statisticsPresetSchema = z.enum(['today', 'week', 'month', 'year'])
export const statisticsQueryPresetSchema = z.enum(['today', 'week', 'month', 'year', 'custom'])
export const themeModeSchema = z.enum(['light', 'dark', 'system'])
export const colorThemeSchema = z.enum(['forest', 'ocean', 'amber', 'wisteria'])
export const entryTypeSchema = z.enum(['expense', 'income'])
export const entryTypeFilterSchema = z.enum(['expense', 'income', 'all'])
export const transactionKindSchema = z.enum(['regular', 'refund', 'reimbursement'])
export const recurrenceFrequencySchema = z.enum(['none', 'weekly', 'monthly', 'yearly'])

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
  note: z.string().trim().max(200),
  transactionKind: transactionKindSchema.optional().default('regular'),
  excludeFromStats: z.boolean().optional().default(false),
  linkedExpenseId: z.string().uuid().nullable().optional().default(null)
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
export const monthSchema = z.string().regex(/^\d{4}-(?:0[1-9]|1[0-2])$/, '月份无效')
export const pinSchema = z.string().regex(/^\d{4,12}$/, '密码必须是4至12位数字')

export const expenseQuerySchema = z.object({
  preset: z.enum(['today', 'week', 'month', 'year', 'all', 'custom']),
  entryType: entryTypeFilterSchema,
  keyword: z.string().trim().max(100),
  startDate: z.string().refine(validDate, '开始日期无效').optional(),
  endDate: z.string().refine(validDate, '结束日期无效').optional()
}).superRefine((value, context) => {
  if (value.preset === 'custom' && (!value.startDate || !value.endDate)) {
    context.addIssue({ code: 'custom', message: '请选择完整的日期范围' })
  }
  if (value.startDate && value.endDate && value.startDate > value.endDate) {
    context.addIssue({ code: 'custom', message: '开始日期不能晚于结束日期' })
  }
})

export const statisticsQuerySchema = z.object({
  preset: statisticsQueryPresetSchema,
  entryType: entryTypeSchema,
  startDate: z.string().refine(validDate, '开始日期无效').optional(),
  endDate: z.string().refine(validDate, '结束日期无效').optional()
}).superRefine((value, context) => {
  if (value.preset === 'custom' && (!value.startDate || !value.endDate)) {
    context.addIssue({ code: 'custom', message: '请选择完整的日期范围' })
  }
  if (value.startDate && value.endDate && value.startDate > value.endDate) {
    context.addIssue({ code: 'custom', message: '开始日期不能晚于结束日期' })
  }
})

export const budgetInputSchema = z.object({
  month: monthSchema,
  totalCents: z.number().int().min(0).max(999_999_999),
  categoryLimits: z.array(z.object({
    categoryId: categoryIdSchema,
    amountCents: z.number().int().positive().max(999_999_999)
  })).max(200)
})

export const transactionTemplateInputSchema = expenseInputSchema.extend({
  name: z.string().trim().min(1, '请输入模板名称').max(30),
  frequency: recurrenceFrequencySchema,
  nextDueDate: z.string().refine(validDate, '下次日期无效').nullable()
}).superRefine((value, context) => {
  if (value.frequency !== 'none' && !value.nextDueDate) {
    context.addIssue({ code: 'custom', message: '周期模板需要设置下次日期' })
  }
})

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
  settings: appSettingsSchema,
  budgets: z.array(budgetInputSchema).max(10_000).optional().default([]),
  templates: z.array(transactionTemplateInputSchema.safeExtend({
    id: expenseIdSchema,
    isActive: z.boolean(),
    createdAt: z.string().datetime()
  })).max(10_000).optional().default([])
})

export const backupDocumentSchema = z.object({
  magic: z.literal('heima-accounting-backup'),
  schemaVersion: z.union([z.literal(1), z.literal(2), z.literal(3), z.literal(4)]),
  appVersion: z.string().min(1),
  exportedAt: z.string().datetime(),
  checksum: z.string().regex(/^[a-f0-9]{64}$/),
  payload: backupPayloadSchema
})
