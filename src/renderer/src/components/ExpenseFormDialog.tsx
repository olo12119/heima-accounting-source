import { useEffect, useMemo } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { ArrowDownLeft, ArrowUpRight, CalendarDays, Clock3, Sparkles, X } from 'lucide-react'
import { CategoryIcon } from './CategoryIcon'
import { centsToInput, parseYuanToCents } from '../../../shared/money'
import { formatLocalDate, formatLocalTime } from '../../../shared/dates'
import type { EntryType, Expense, ExpenseInput } from '../../../shared/types'
import { getErrorMessage } from '../lib/errors'

type FormValues = { entryType: EntryType; amount: string; primaryCategoryId: string; secondaryCategoryId: string; spentDate: string; spentTime: string; note: string }

const defaults = (): FormValues => {
  const now = new Date()
  return { entryType: 'expense', amount: '', primaryCategoryId: 'food', secondaryCategoryId: 'food.meal', spentDate: formatLocalDate(now), spentTime: formatLocalTime(now), note: '' }
}

const fallbackChoices = {
  expense: [
    { primaryCategoryId: 'food', secondaryCategoryId: 'food.meal', primaryCategoryName: '餐饮', secondaryCategoryName: '正餐', count: 0 },
    { primaryCategoryId: 'transport', secondaryCategoryId: 'transport.public', primaryCategoryName: '交通', secondaryCategoryName: '公交地铁', count: 0 },
    { primaryCategoryId: 'shopping', secondaryCategoryId: 'shopping.daily', primaryCategoryName: '购物', secondaryCategoryName: '日用百货', count: 0 }
  ],
  income: [
    { primaryCategoryId: 'salary', secondaryCategoryId: 'salary.base', primaryCategoryName: '工资薪酬', secondaryCategoryName: '工资', count: 0 },
    { primaryCategoryId: 'bonus', secondaryCategoryId: 'bonus.performance', primaryCategoryName: '奖金福利', secondaryCategoryName: '绩效奖金', count: 0 },
    { primaryCategoryId: 'reimbursement', secondaryCategoryId: 'reimbursement.work', primaryCategoryName: '报销退款', secondaryCategoryName: '工作报销', count: 0 }
  ]
}

export function ExpenseFormDialog({ expense, onClose }: { expense: Expense | null | undefined; onClose: () => void }): React.JSX.Element {
  const queryClient = useQueryClient()
  const categoriesQuery = useQuery({ queryKey: ['categories'], queryFn: () => window.heima.getCategories() })
  const initial = useMemo<FormValues>(() => expense ? {
    entryType: expense.entryType, amount: centsToInput(expense.amountCents), primaryCategoryId: expense.primaryCategoryId,
    secondaryCategoryId: expense.secondaryCategoryId, spentDate: expense.spentDate, spentTime: expense.spentTime, note: expense.note
  } : defaults(), [expense])
  const { register, handleSubmit, watch, setValue, reset, formState: { errors } } = useForm<FormValues>({ defaultValues: initial })
  const entryType = watch('entryType')
  const primaryId = watch('primaryCategoryId')
  const frequentQuery = useQuery({ queryKey: ['frequent-categories', entryType], queryFn: () => window.heima.getFrequentCategories(entryType) })

  useEffect(() => reset(initial), [initial, reset])
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent): void => { if (event.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [onClose])

  const allCategories = categoriesQuery.data ?? []
  const primaryCategories = allCategories.filter((category) => category.parentId === null && category.entryType === entryType)
  const secondaryCategories = allCategories.filter((category) => category.parentId === primaryId && category.entryType === entryType)
  const selectType = (nextType: EntryType): void => {
    const primary = allCategories.find((category) => category.parentId === null && category.entryType === nextType)
    const secondary = allCategories.find((category) => category.parentId === primary?.id)
    setValue('entryType', nextType)
    setValue('primaryCategoryId', primary?.id ?? (nextType === 'income' ? 'salary' : 'food'))
    setValue('secondaryCategoryId', secondary?.id ?? (nextType === 'income' ? 'salary.base' : 'food.meal'))
  }
  const choosePrimary = (id: string): void => {
    const firstSecondary = allCategories.find((category) => category.parentId === id)
    setValue('primaryCategoryId', id, { shouldValidate: true }); setValue('secondaryCategoryId', firstSecondary?.id ?? '', { shouldValidate: true })
  }
  const quickChoices = frequentQuery.data?.length ? frequentQuery.data : fallbackChoices[entryType]
  const saveMutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const amountCents = parseYuanToCents(values.amount)
      if (amountCents === null) throw new Error('请输入正确金额，最多保留两位小数')
      const input: ExpenseInput = { entryType: values.entryType, amountCents, primaryCategoryId: values.primaryCategoryId, secondaryCategoryId: values.secondaryCategoryId, spentDate: values.spentDate, spentTime: values.spentTime, note: values.note.trim() }
      return expense ? window.heima.updateExpense(expense.id, input) : window.heima.createExpense(input)
    },
    onSuccess: async () => { await queryClient.invalidateQueries(); onClose() }
  })
  const typeLabel = entryType === 'income' ? '收入' : '支出'

  return <div className="dialog-backdrop form-backdrop" role="presentation">
    <section className={`expense-dialog ${entryType}`} role="dialog" aria-modal="true" aria-labelledby="expense-title">
      <div className="dialog-header"><div><span className="eyebrow">{expense ? '修改记录' : '快速记账'}</span><h2 id="expense-title">{expense ? `编辑这笔${typeLabel}` : `记一笔${typeLabel}`}</h2></div><button className="icon-button" aria-label="关闭" onClick={onClose}><X size={20} /></button></div>
      <form onSubmit={handleSubmit((values) => saveMutation.mutate(values))}>
        <div className="entry-type-switch" aria-label="收支类型">
          <button type="button" className={entryType === 'expense' ? 'active expense' : ''} onClick={() => selectType('expense')}><ArrowUpRight size={17} />支出</button>
          <button type="button" className={entryType === 'income' ? 'active income' : ''} onClick={() => selectType('income')}><ArrowDownLeft size={17} />收入</button>
        </div><input type="hidden" {...register('entryType')} />
        <label className="amount-field"><span>{typeLabel}金额</span><div><b>¥</b><input autoFocus inputMode="decimal" placeholder="0.00" aria-label="金额" {...register('amount', { required: '请输入金额', validate: (value) => parseYuanToCents(value) !== null || '请输入正确金额，最多保留两位小数' })} /></div>{errors.amount && <small className="field-error">{errors.amount.message}</small>}</label>
        <div className="quick-categories"><span className="field-label"><Sparkles size={14} /> 常用分类</span><div className="quick-list">{quickChoices.map((choice) => <button type="button" key={choice.secondaryCategoryId} onClick={() => { setValue('primaryCategoryId', choice.primaryCategoryId); setValue('secondaryCategoryId', choice.secondaryCategoryId) }}>{choice.secondaryCategoryName}</button>)}</div></div>
        <fieldset className="category-fieldset"><legend>一级分类</legend><input type="hidden" {...register('primaryCategoryId', { required: true })} /><div className="primary-grid">{primaryCategories.map((category) => <button type="button" key={category.id} className={primaryId === category.id ? 'selected' : ''} onClick={() => choosePrimary(category.id)}><span className="category-icon-tile" data-category={category.id}><CategoryIcon name={category.icon} size={19} strokeWidth={1.8} /></span><span>{category.name}</span></button>)}</div></fieldset>
        <fieldset className="category-fieldset secondary-fieldset"><legend>二级分类</legend><div className="secondary-list">{secondaryCategories.map((category) => <label key={category.id} className={watch('secondaryCategoryId') === category.id ? 'selected' : ''}><input type="radio" value={category.id} {...register('secondaryCategoryId', { required: true })} />{category.name}</label>)}</div></fieldset>
        <div className="form-row"><label><span><CalendarDays size={15} /> 日期</span><input type="date" {...register('spentDate', { required: true })} /></label><label><span><Clock3 size={15} /> 时间</span><input type="time" {...register('spentTime', { required: true })} /></label></div>
        <label className="note-field"><span>备注 <em>可选</em></span><input placeholder={entryType === 'income' ? '例如：八月工资、差旅报销…' : '例如：午餐、超市采购…'} maxLength={200} {...register('note', { maxLength: 200 })} /></label>
        {saveMutation.isError && <div className="form-error" role="alert">{getErrorMessage(saveMutation.error)}</div>}
        <div className="form-footer"><span>按 Enter 快速保存</span><div><button type="button" className="button ghost" onClick={onClose}>取消</button><button type="submit" className="button primary wide" disabled={saveMutation.isPending}>{saveMutation.isPending ? '保存中…' : `保存${typeLabel}`}</button></div></div>
      </form>
    </section>
  </div>
}
