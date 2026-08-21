import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { ArrowDownLeft, ArrowUpRight, CalendarDays, Clock3, Plus, Sparkles, X } from 'lucide-react'
import { CategoryIcon } from './CategoryIcon'
import { centsToInput, parseYuanToCents } from '../../../shared/money'
import { formatLocalDate, formatLocalTime } from '../../../shared/dates'
import type { Category, EntryType, Expense, ExpenseInput, TransactionKind } from '../../../shared/types'
import { getErrorMessage } from '../lib/errors'

type FormValues = { entryType: EntryType; amount: string; primaryCategoryId: string; secondaryCategoryId: string; spentDate: string; spentTime: string; note: string; transactionKind: TransactionKind; excludeFromStats: boolean; linkedExpenseId: string }

const defaults = (): FormValues => {
  const now = new Date()
  return { entryType: 'expense', amount: '', primaryCategoryId: 'food', secondaryCategoryId: 'food.meal', spentDate: formatLocalDate(now), spentTime: formatLocalTime(now), note: '', transactionKind: 'regular', excludeFromStats: false, linkedExpenseId: '' }
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
  const [categoryCreator, setCategoryCreator] = useState<'primary' | 'secondary' | null>(null)
  const queryClient = useQueryClient()
  const categoriesQuery = useQuery({ queryKey: ['categories'], queryFn: () => window.heima.getCategories() })
  const initial = useMemo<FormValues>(() => expense ? {
    entryType: expense.entryType, amount: centsToInput(expense.amountCents), primaryCategoryId: expense.primaryCategoryId,
    secondaryCategoryId: expense.secondaryCategoryId, spentDate: expense.spentDate, spentTime: expense.spentTime, note: expense.note,
    transactionKind: expense.transactionKind ?? 'regular', excludeFromStats: expense.excludeFromStats ?? false, linkedExpenseId: expense.linkedExpenseId ?? ''
  } : defaults(), [expense])
  const { register, handleSubmit, watch, setValue, reset, formState: { errors } } = useForm<FormValues>({ defaultValues: initial })
  const entryType = watch('entryType')
  const primaryId = watch('primaryCategoryId')
  const transactionKind = watch('transactionKind')
  const linkableQuery = useQuery({ queryKey: ['expenses', 'refund-link'], queryFn: () => window.heima.searchExpenses({ preset: 'all', entryType: 'expense', keyword: '' }), enabled: entryType === 'income' && transactionKind !== 'regular' })
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
    setValue('transactionKind', 'regular'); setValue('linkedExpenseId', '')
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
      const input: ExpenseInput = { entryType: values.entryType, amountCents, primaryCategoryId: values.primaryCategoryId, secondaryCategoryId: values.secondaryCategoryId, spentDate: values.spentDate, spentTime: values.spentTime, note: values.note.trim(), transactionKind: values.transactionKind, excludeFromStats: values.excludeFromStats, linkedExpenseId: values.linkedExpenseId || null }
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
        <fieldset className="category-fieldset"><legend>一级分类 <button type="button" className="inline-add-category" onClick={() => setCategoryCreator('primary')}><Plus size={12} />新建</button></legend><input type="hidden" {...register('primaryCategoryId', { required: true })} /><div className="primary-grid">{primaryCategories.map((category) => <button type="button" key={category.id} className={primaryId === category.id ? 'selected' : ''} onClick={() => choosePrimary(category.id)}><span className="category-icon-tile" style={{ color: category.color, background: `${category.color}1f` }}><CategoryIcon name={category.icon} size={21} /></span><span>{category.name}</span></button>)}</div></fieldset>
        <fieldset className="category-fieldset secondary-fieldset"><legend>二级分类 <button type="button" className="inline-add-category" onClick={() => setCategoryCreator('secondary')}><Plus size={12} />添加到当前分类</button></legend><div className="secondary-list">{secondaryCategories.map((category) => <label key={category.id} className={watch('secondaryCategoryId') === category.id ? 'selected' : ''}><input type="radio" value={category.id} {...register('secondaryCategoryId', { required: true })} />{category.name}</label>)}</div></fieldset>
        {entryType === 'income' && <div className="transaction-kind"><span className="field-label">这笔钱属于</span><div>{([['regular','普通收入'],['refund','消费退款'],['reimbursement','费用报销']] as Array<[TransactionKind,string]>).map(([value,label]) => <label className={transactionKind === value ? 'selected' : ''} key={value}><input type="radio" value={value} {...register('transactionKind')} />{label}</label>)}</div>{transactionKind !== 'regular' && <label><span>关联原支出</span><select aria-label="关联原支出" {...register('linkedExpenseId', { required: true })}><option value="">请选择需要冲减的支出</option>{linkableQuery.data?.map((item) => <option key={item.id} value={item.id}>{item.spentDate} · {item.secondaryCategoryName} · {centsToInput(item.amountCents)}</option>)}</select></label>}</div>}
        <label className="exclude-toggle"><input type="checkbox" {...register('excludeFromStats')} /><span><strong>不计入收支统计</strong><small>适合借还款等只想留痕、不想影响报表的记录</small></span></label>
        <div className="form-row"><label><span><CalendarDays size={15} /> 日期</span><input type="date" {...register('spentDate', { required: true })} /></label><label><span><Clock3 size={15} /> 时间</span><input type="time" {...register('spentTime', { required: true })} /></label></div>
        <label className="note-field"><span>备注 <em>可选</em></span><input placeholder={entryType === 'income' ? '例如：八月工资、差旅报销…' : '例如：午餐、超市采购…'} maxLength={200} {...register('note', { maxLength: 200 })} /></label>
        {saveMutation.isError && <div className="form-error" role="alert">{getErrorMessage(saveMutation.error)}</div>}
        <div className="form-footer"><span>按 Enter 快速保存</span><div><button type="button" className="button ghost" onClick={onClose}>取消</button><button type="submit" className="button primary wide" disabled={saveMutation.isPending}>{saveMutation.isPending ? '保存中…' : `保存${typeLabel}`}</button></div></div>
      </form>
    </section>
    {categoryCreator && <QuickCategoryDialog kind={categoryCreator} entryType={entryType} parent={primaryCategories.find((category) => category.id === primaryId)} onClose={() => setCategoryCreator(null)} onCreated={async (primary, secondary) => { await queryClient.invalidateQueries({ queryKey: ['categories'] }); setValue('primaryCategoryId', primary.id); setValue('secondaryCategoryId', secondary.id); setCategoryCreator(null) }} />}
  </div>
}

function QuickCategoryDialog({ kind, entryType, parent, onClose, onCreated }: { kind: 'primary' | 'secondary'; entryType: EntryType; parent?: Category; onClose: () => void; onCreated: (primary: Category, secondary: Category) => Promise<void> }): React.JSX.Element {
  const [name, setName] = useState(''); const [childName, setChildName] = useState(''); const [error, setError] = useState(''); const [busy, setBusy] = useState(false)
  const save = async (event: React.FormEvent): Promise<void> => { event.preventDefault(); setBusy(true); setError(''); try {
    if (kind === 'primary') {
      const primary = await window.heima.createCustomPrimaryCategory({ entryType, name, firstSecondaryName: childName, icon: entryType === 'income' ? 'wallet' : 'shapes', color: entryType === 'income' ? '#2d9b72' : '#d98257' })
      const categories = await window.heima.getCategories(); const secondary = categories.find((item) => item.parentId === primary.id); if (!secondary) throw new Error('分类创建后未能读取二级分类'); await onCreated(primary, secondary)
    } else {
      if (!parent) throw new Error('请先选择一级分类'); const secondary = await window.heima.createCustomSecondaryCategory({ parentId: parent.id, name }); await onCreated(parent, secondary)
    }
  } catch (reason) { setError(getErrorMessage(reason)) } finally { setBusy(false) } }
  return <div className="dialog-backdrop quick-category-backdrop"><section className="confirm-dialog quick-category-dialog" role="dialog" aria-modal="true" aria-label={kind === 'primary' ? '快速新建一级分类' : '快速添加二级分类'}><h2>{kind === 'primary' ? '新建一级分类' : `添加到“${parent?.name ?? ''}”`}</h2><p>保存后会自动回到这笔账并选中新分类。</p><form onSubmit={save}><input autoFocus aria-label="分类名称" value={name} onChange={(event) => setName(event.target.value)} placeholder={kind === 'primary' ? '一级分类名称' : '二级分类名称'} maxLength={16} required />{kind === 'primary' && <input aria-label="第一个二级分类" value={childName} onChange={(event) => setChildName(event.target.value)} placeholder="第一个二级分类" maxLength={16} required />}{error && <div className="form-error">{error}</div>}<div className="dialog-actions"><button type="button" className="button ghost" onClick={onClose}>取消</button><button className="button primary" disabled={busy}>{busy ? '保存中…' : '保存并选中'}</button></div></form></section></div>
}
