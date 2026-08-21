import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { ArrowDownLeft, ArrowUpRight, BarChart3, TrendingUp } from 'lucide-react'
import { Bar, BarChart, CartesianGrid, Cell, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { formatCents } from '../../../shared/money'
import type { EntryType, RangePreset } from '../../../shared/types'
import { CategoryIcon } from '../components/CategoryIcon'
import { getErrorMessage } from '../lib/errors'

type StatisticsPreset = Exclude<RangePreset, 'all'>
const ranges: Array<{ value: StatisticsPreset; label: string }> = [
  { value: 'today', label: '今日' }, { value: 'week', label: '本周' }, { value: 'month', label: '本月' }
]

export function StatisticsPage(): React.JSX.Element {
  const [preset, setPreset] = useState<StatisticsPreset>('month')
  const [entryType, setEntryType] = useState<EntryType>('expense')
  const query = useQuery({ queryKey: ['statistics', preset, entryType], queryFn: () => window.heima.getStatistics(preset, entryType) })
  const data = query.data
  const label = entryType === 'income' ? '收入' : '支出'
  const accent = entryType === 'income' ? 'var(--income)' : 'var(--expense)'

  return <div className={`statistics-page ${entryType}`}>
    <div className="statistics-controls"><div className="segmented statistics-tabs">{ranges.map((range) => <button key={range.value} className={preset === range.value ? 'active' : ''} onClick={() => setPreset(range.value)}>{range.label}</button>)}</div><div className="stats-type-switch"><button className={entryType === 'expense' ? 'active expense' : ''} onClick={() => setEntryType('expense')}><ArrowUpRight size={15} />支出分析</button><button className={entryType === 'income' ? 'active income' : ''} onClick={() => setEntryType('income')}><ArrowDownLeft size={15} />收入分析</button></div></div>
    {query.isLoading ? <div className="stats-loading skeleton" /> : query.isError ? <div className="page-error">{getErrorMessage(query.error)}</div> : data && <>
      <section className="stats-hero"><div><span>这一阶段共{label}</span><strong>{formatCents(data.totalCents)}</strong><small>{data.startDate} 至 {data.endDate}</small></div><div className="stats-hero-mark"><TrendingUp size={25} /></div></section>
      {data.totalCents === 0 ? <section className="panel stats-empty"><BarChart3 size={32} /><h2>暂无可统计的{label}</h2><p>记录{label}后，这里会自动呈现分类和每日趋势。</p></section> : <div className="stats-grid">
        <section className="panel trend-panel"><div className="panel-heading"><div><span className="eyebrow">每日变化</span><h2>{label}趋势</h2></div></div><div className="chart-area"><ResponsiveContainer width="100%" height="100%"><LineChart data={data.dailyTotals} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}><CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border)" /><XAxis dataKey="date" tickFormatter={(value) => String(value).slice(5)} axisLine={false} tickLine={false} tick={{ fill: 'var(--muted)', fontSize: 11 }} /><YAxis tickFormatter={(value) => `¥${Number(value) / 100}`} axisLine={false} tickLine={false} tick={{ fill: 'var(--muted)', fontSize: 11 }} width={58} /><Tooltip formatter={(value) => formatCents(Number(value))} labelFormatter={(value) => `日期 ${value}`} /><Line type="monotone" dataKey="amountCents" name={label} stroke={accent} strokeWidth={3} dot={{ r: 3, fill: accent, strokeWidth: 0 }} activeDot={{ r: 5 }} /></LineChart></ResponsiveContainer></div></section>
        <section className="panel ranking-panel"><div className="panel-heading"><div><span className="eyebrow">分类比较</span><h2>{label}排行</h2></div></div><div className="chart-area"><ResponsiveContainer width="100%" height="100%"><BarChart data={data.categoryTotals.slice(0, 7)} layout="vertical" margin={{ top: 0, right: 20, left: 0, bottom: 0 }}><CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="var(--border)" /><XAxis type="number" hide /><YAxis dataKey="categoryName" type="category" axisLine={false} tickLine={false} width={68} tick={{ fill: 'var(--text)', fontSize: 12 }} /><Tooltip formatter={(value) => formatCents(Number(value))} /><Bar dataKey="amountCents" name={label} radius={[0, 7, 7, 0]} barSize={17}>{data.categoryTotals.slice(0, 7).map((entry) => <Cell key={entry.categoryId} fill={entry.color} />)}</Bar></BarChart></ResponsiveContainer></div></section>
        <section className="panel breakdown-panel"><div className="panel-heading"><div><span className="eyebrow">占比明细</span><h2>{label}分类情况</h2></div></div><div className="breakdown-list">{data.categoryTotals.map((category, index) => { const percent = Math.round(category.amountCents / data.totalCents * 100); return <div key={category.categoryId}><span className="rank">{index + 1}</span><span className="breakdown-icon" style={{ color: category.color, background: `${category.color}1f` }}><CategoryIcon name={category.icon} size={15} /></span><span><strong>{category.categoryName}</strong><small>{percent}%</small></span><div className="progress"><i style={{ width: `${percent}%`, background: category.color }} /></div><strong>{formatCents(category.amountCents)}</strong></div> })}</div></section>
      </div>}
    </>}
  </div>
}
