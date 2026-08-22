import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CalendarClock, CheckCircle2, PiggyBank, Plus, Trash2 } from 'lucide-react'
import { centsToInput, formatCents, parseYuanToCents } from '../../../shared/money'
import { formatLocalDate, formatLocalTime } from '../../../shared/dates'
import type { EntryType, RecurrenceFrequency, TransactionTemplateInput } from '../../../shared/types'
import { CategoryIcon } from '../components/CategoryIcon'
import { getErrorMessage } from '../lib/errors'

const now = new Date()
const currentMonth = formatLocalDate(now).slice(0, 7)
const frequencyLabels: Record<RecurrenceFrequency, string> = { none: '快捷模板', weekly: '每周', monthly: '每月', yearly: '每年' }

export function PlanningPage(): React.JSX.Element {
  const queryClient = useQueryClient()
  const [month, setMonth] = useState(currentMonth)
  const [budgetTotal, setBudgetTotal] = useState('')
  const [limits, setLimits] = useState<Record<string, string>>({})
  const [showTemplate, setShowTemplate] = useState(false)
  const [message, setMessage] = useState('')
  const categoriesQuery = useQuery({ queryKey: ['categories'], queryFn: () => window.heima.getCategories() })
  const budgetQuery = useQuery({ queryKey: ['budget', month], queryFn: () => window.heima.getBudget(month) })
  const templatesQuery = useQuery({ queryKey: ['templates'], queryFn: () => window.heima.listTemplates() })
  const primaries = useMemo(() => (categoriesQuery.data ?? []).filter((item) => item.parentId === null && item.entryType === 'expense'), [categoriesQuery.data])
  useEffect(() => {
    if (!budgetQuery.data) return
    setBudgetTotal(budgetQuery.data.totalCents ? centsToInput(budgetQuery.data.totalCents) : '')
    setLimits(Object.fromEntries(budgetQuery.data.categories.map((item) => [item.categoryId, centsToInput(item.limitCents)])))
  }, [budgetQuery.data])
  const budgetMutation = useMutation({
    mutationFn: () => {
      const totalCents = budgetTotal ? parseYuanToCents(budgetTotal) : 0
      if (totalCents === null) throw new Error('请输入正确的月度预算')
      const categoryLimits = Object.entries(limits).filter(([, value]) => value.trim()).map(([categoryId, value]) => {
        const amountCents = parseYuanToCents(value)
        if (!amountCents) throw new Error('分类预算必须是大于0的金额')
        return { categoryId, amountCents }
      })
      return window.heima.saveBudget({ month, totalCents, categoryLimits })
    },
    onSuccess: async () => { setMessage('预算已保存'); await queryClient.invalidateQueries({ queryKey: ['budget', month] }); await queryClient.invalidateQueries({ queryKey: ['dashboard'] }) },
    onError: (error) => setMessage(getErrorMessage(error))
  })
  const templateAction = useMutation({
    mutationFn: async ({ kind, id }: { kind: 'apply' | 'delete'; id: string }): Promise<void> => { if (kind === 'apply') await window.heima.applyTemplate(id); else await window.heima.deleteTemplate(id) },
    onSuccess: async (_, action) => { setMessage(action.kind === 'apply' ? '已根据模板记入一笔账' : '模板已删除'); await queryClient.invalidateQueries() },
    onError: (error) => setMessage(getErrorMessage(error))
  })

  return <div className="planning-page">
    <section className="panel budget-planner">
      <div className="planning-heading"><span className="planning-icon"><PiggyBank size={24} /></span><div><span className="eyebrow">轻量规划</span><h2>月度预算</h2><p>预算只是提醒，不会阻止你记账。</p></div><input type="month" value={month} onChange={(event) => setMonth(event.target.value)} aria-label="预算月份" /></div>
      <div className="budget-overview">
        <label><span>本月总预算</span><div><b>¥</b><input value={budgetTotal} onChange={(event) => setBudgetTotal(event.target.value)} placeholder="例如 5000" aria-label="本月总预算" /></div></label>
        <div className="budget-progress-copy"><span>已支出 {formatCents(budgetQuery.data?.spentCents ?? 0)}</span><strong>{budgetQuery.data?.totalCents ? `${budgetQuery.data.percent}%` : '尚未设置'}</strong><div><i style={{ width: `${Math.min(budgetQuery.data?.percent ?? 0, 100)}%` }} /></div><small>{(budgetQuery.data?.remainingCents ?? 0) >= 0 ? `还可使用 ${formatCents(budgetQuery.data?.remainingCents ?? 0)}` : `已超出 ${formatCents(Math.abs(budgetQuery.data?.remainingCents ?? 0))}`}</small></div>
      </div>
      <div className="category-budget-grid">{primaries.map((category) => <label key={category.id}><span className="budget-category"><span className="category-icon-badge" style={{ color: category.color, background: `${category.color}1f` }}><CategoryIcon name={category.icon} size={18} /></span>{category.name}</span><div><b>¥</b><input aria-label={`${category.name}预算`} value={limits[category.id] ?? ''} onChange={(event) => setLimits((current) => ({ ...current, [category.id]: event.target.value }))} placeholder="不限制" /></div></label>)}</div>
      <button className="button primary" disabled={budgetMutation.isPending} onClick={() => budgetMutation.mutate()}><CheckCircle2 size={16} />保存预算</button>
    </section>

    <section className="panel template-planner">
      <div className="planning-heading"><span className="planning-icon"><CalendarClock size={24} /></span><div><span className="eyebrow">少做重复工作</span><h2>快捷与周期模板</h2><p>到期账目由你确认后生成，不会偷偷写入。</p></div><button className="button secondary" onClick={() => setShowTemplate(true)}><Plus size={16} />新建模板</button></div>
      <div className="template-list">{templatesQuery.data?.length ? templatesQuery.data.map((template) => {
        const primary = categoriesQuery.data?.find((item) => item.id === template.primaryCategoryId)
        const due = template.frequency !== 'none' && template.nextDueDate && template.nextDueDate <= formatLocalDate(new Date())
        return <div className={due ? 'due' : ''} key={template.id}><span className="category-icon-badge" style={{ color: primary?.color, background: `${primary?.color ?? '#7c8580'}1f` }}><CategoryIcon name={primary?.icon ?? 'shapes'} size={19} /></span><span><strong>{template.name}</strong><small>{frequencyLabels[template.frequency]}{template.nextDueDate ? ` · 下次 ${template.nextDueDate}` : ''}</small></span><b className={template.entryType}>{template.entryType === 'income' ? '+' : '−'}{formatCents(template.amountCents)}</b><button className="button secondary" onClick={() => templateAction.mutate({ kind: 'apply', id: template.id })}>{due ? '到期确认' : '记一笔'}</button><button className="icon-button delete" aria-label={`删除模板${template.name}`} onClick={() => templateAction.mutate({ kind: 'delete', id: template.id })}><Trash2 size={15} /></button></div>
      }) : <div className="inline-empty">还没有模板。可以把房租、工资或每天早餐保存成模板。</div>}</div>
    </section>
    {message && <div className="toast" role="status">{message}</div>}
    {showTemplate && <TemplateDialog categories={categoriesQuery.data ?? []} onClose={() => setShowTemplate(false)} onSaved={async () => { setShowTemplate(false); setMessage('模板已保存'); await queryClient.invalidateQueries({ queryKey: ['templates'] }) }} />}
  </div>
}

function TemplateDialog({ categories, onClose, onSaved }: { categories: Awaited<ReturnType<typeof window.heima.getCategories>>; onClose: () => void; onSaved: () => Promise<void> }): React.JSX.Element {
  const [entryType, setEntryType] = useState<EntryType>('expense')
  const primaryOptions = categories.filter((item) => item.parentId === null && item.entryType === entryType)
  const [primaryId, setPrimaryId] = useState('food')
  const secondaryOptions = categories.filter((item) => item.parentId === primaryId)
  const [secondaryId, setSecondaryId] = useState('food.meal')
  const [name, setName] = useState(''); const [amount, setAmount] = useState(''); const [note, setNote] = useState('')
  const [frequency, setFrequency] = useState<RecurrenceFrequency>('none'); const [nextDueDate, setNextDueDate] = useState(formatLocalDate(new Date())); const [error, setError] = useState('')
  const chooseType = (type: EntryType): void => { const primary = categories.find((item) => item.parentId === null && item.entryType === type); const secondary = categories.find((item) => item.parentId === primary?.id); setEntryType(type); setPrimaryId(primary?.id ?? ''); setSecondaryId(secondary?.id ?? '') }
  const save = async (event: React.FormEvent): Promise<void> => {
    event.preventDefault(); setError('')
    const amountCents = parseYuanToCents(amount); if (!amountCents) { setError('请输入正确金额'); return }
    const input: TransactionTemplateInput = { name, entryType, amountCents, primaryCategoryId: primaryId, secondaryCategoryId: secondaryId, spentDate: formatLocalDate(new Date()), spentTime: formatLocalTime(new Date()), note, frequency, nextDueDate: frequency === 'none' ? null : nextDueDate, transactionKind: 'regular', excludeFromStats: false, linkedExpenseId: null }
    try { await window.heima.saveTemplate(input); await onSaved() } catch (reason) { setError(getErrorMessage(reason)) }
  }
  return <div className="dialog-backdrop" role="presentation"><section className="category-dialog template-dialog" role="dialog" aria-modal="true" aria-label="新建记账模板"><div className="dialog-header"><div><span className="eyebrow">快捷记账</span><h2>新建模板</h2></div><button className="icon-button" onClick={onClose}>×</button></div><form onSubmit={save}>
    <div className="entry-type-switch"><button type="button" className={entryType === 'expense' ? 'active expense' : ''} onClick={() => chooseType('expense')}>支出</button><button type="button" className={entryType === 'income' ? 'active income' : ''} onClick={() => chooseType('income')}>收入</button></div>
    <label className="category-name-field"><span>模板名称</span><input required maxLength={30} value={name} onChange={(event) => setName(event.target.value)} placeholder="例如：每月房租" /></label>
    <label className="category-name-field"><span>金额（元）</span><input required value={amount} onChange={(event) => setAmount(event.target.value)} placeholder="0.00" /></label>
    <div className="form-row"><label><span>一级分类</span><select value={primaryId} onChange={(event) => { setPrimaryId(event.target.value); setSecondaryId(categories.find((item) => item.parentId === event.target.value)?.id ?? '') }}>{primaryOptions.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label><label><span>二级分类</span><select value={secondaryId} onChange={(event) => setSecondaryId(event.target.value)}>{secondaryOptions.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label></div>
    <div className="form-row"><label><span>重复方式</span><select value={frequency} onChange={(event) => setFrequency(event.target.value as RecurrenceFrequency)}>{Object.entries(frequencyLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>{frequency !== 'none' && <label><span>下次日期</span><input type="date" value={nextDueDate} onChange={(event) => setNextDueDate(event.target.value)} /></label>}</div>
    <label className="category-name-field"><span>备注</span><input maxLength={200} value={note} onChange={(event) => setNote(event.target.value)} /></label>{error && <div className="form-error">{error}</div>}<button className="button primary" type="submit">保存模板</button>
  </form></section></div>
}
