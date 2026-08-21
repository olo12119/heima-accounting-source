import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Edit3, Trash2 } from 'lucide-react'
import { formatCents } from '../../../shared/money'
import { formatDisplayDate } from '../../../shared/dates'
import type { EntryTypeFilter, Expense, RangePreset } from '../../../shared/types'
import { CategoryIcon } from '../components/CategoryIcon'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { EmptyState } from '../components/EmptyState'
import { useAppShell } from '../components/AppShell'
import { getErrorMessage } from '../lib/errors'

const ranges: Array<{ value: RangePreset; label: string }> = [
  { value: 'today', label: '今天' }, { value: 'week', label: '本周' }, { value: 'month', label: '本月' }, { value: 'all', label: '全部' }
]
const types: Array<{ value: EntryTypeFilter; label: string }> = [
  { value: 'all', label: '全部收支' }, { value: 'expense', label: '仅支出' }, { value: 'income', label: '仅收入' }
]
export function RecordsPage(): React.JSX.Element {
  const [preset, setPreset] = useState<RangePreset>('month')
  const [entryType, setEntryType] = useState<EntryTypeFilter>('all')
  const [deleteTarget, setDeleteTarget] = useState<Expense | null>(null)
  const { openNewExpense, openEditExpense } = useAppShell()
  const queryClient = useQueryClient()
  const query = useQuery({ queryKey: ['expenses', preset, entryType], queryFn: () => window.heima.listExpenses(preset, entryType) })
  const deleteMutation = useMutation({ mutationFn: (id: string) => window.heima.deleteExpense(id), onSuccess: async () => { setDeleteTarget(null); await queryClient.invalidateQueries() } })
  const groups = useMemo(() => {
    const result = new Map<string, Expense[]>()
    for (const entry of query.data ?? []) { const items = result.get(entry.spentDate) ?? []; items.push(entry); result.set(entry.spentDate, items) }
    return Array.from(result.entries())
  }, [query.data])
  const expenseTotal = (query.data ?? []).filter((item) => item.entryType === 'expense').reduce((sum, item) => sum + item.amountCents, 0)
  const incomeTotal = (query.data ?? []).filter((item) => item.entryType === 'income').reduce((sum, item) => sum + item.amountCents, 0)

  return <div className="records-page">
    <section className="filter-bar records-toolbar">
      <div><div className="segmented" aria-label="账单时间范围">{ranges.map((range) => <button key={range.value} className={preset === range.value ? 'active' : ''} onClick={() => setPreset(range.value)}>{range.label}</button>)}</div>
        <div className="type-filter" aria-label="收支筛选">{types.map((type) => <button key={type.value} className={entryType === type.value ? 'active' : ''} onClick={() => setEntryType(type.value)}>{type.label}</button>)}</div>
      </div>
      <div className="period-flow"><div><span>收入</span><strong className="income">+{formatCents(incomeTotal)}</strong></div><i /><div><span>支出</span><strong>−{formatCents(expenseTotal)}</strong></div><i /><div><span>结余</span><strong className={incomeTotal - expenseTotal >= 0 ? 'income' : ''}>{incomeTotal - expenseTotal >= 0 ? '+' : '−'}{formatCents(Math.abs(incomeTotal - expenseTotal))}</strong></div><small>{query.data?.length ?? 0} 笔</small></div>
    </section>
    <section className="panel records-panel">
      {query.isLoading ? <div className="list-loading">正在读取账单…</div> : query.isError ? <div className="page-error">{getErrorMessage(query.error)}</div> : groups.length === 0 ? <EmptyState title="这个时间段还没有账目" description="切换筛选条件，或者现在记录一笔收入或支出。" actionLabel="记一笔" onAction={openNewExpense} /> : groups.map(([date, entries]) => {
        const dayIncome = entries.filter((item) => item.entryType === 'income').reduce((sum, item) => sum + item.amountCents, 0)
        const dayExpense = entries.filter((item) => item.entryType === 'expense').reduce((sum, item) => sum + item.amountCents, 0)
        return <div className="date-group" key={date}><div className="date-heading"><strong>{formatDisplayDate(date)}</strong><span><em>收 {formatCents(dayIncome)}</em> · 支 {formatCents(dayExpense)}</span></div><div className="expense-list">{entries.map((entry) => <div className="expense-row" key={entry.id}>
          <span className="category-icon-badge" style={{ color: entry.primaryCategoryColor, background: `${entry.primaryCategoryColor}1f` }}><CategoryIcon name={entry.primaryCategoryIcon} size={20} /></span>
          <span className="expense-time">{entry.spentTime}</span><span className="expense-info"><strong>{entry.secondaryCategoryName}</strong><small>{entry.note || entry.primaryCategoryName}</small></span>
          <span className="entry-type-pill" data-type={entry.entryType}>{entry.entryType === 'income' ? '收入' : '支出'}</span>
          <strong className={`expense-amount ${entry.entryType}`}>{entry.entryType === 'income' ? '+' : '−'}{formatCents(entry.amountCents)}</strong>
          <div className="row-actions"><button className="icon-button" aria-label={`编辑${entry.secondaryCategoryName}`} onClick={() => openEditExpense(entry)}><Edit3 size={16} /></button><button className="icon-button delete" aria-label={`删除${entry.secondaryCategoryName}`} onClick={() => setDeleteTarget(entry)}><Trash2 size={16} /></button></div>
        </div>)}</div></div>
      })}
    </section>
    {deleteMutation.isError && <div className="toast error">{getErrorMessage(deleteMutation.error)}</div>}
    <ConfirmDialog open={deleteTarget !== null} title="删除这笔账目？" description={deleteTarget ? `${deleteTarget.spentDate} 的“${deleteTarget.secondaryCategoryName}”${deleteTarget.entryType === 'income' ? '收入' : '支出'} ${formatCents(deleteTarget.amountCents)} 将被永久删除。` : ''} confirmLabel="确认删除" danger busy={deleteMutation.isPending} onCancel={() => setDeleteTarget(null)} onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)} />
  </div>
}
