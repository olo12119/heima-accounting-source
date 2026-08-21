import { useQuery } from '@tanstack/react-query'
import { ArrowDownLeft, ArrowRight, ArrowUpRight, Landmark, Plus, TrendingUp } from 'lucide-react'
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import { Link } from 'react-router-dom'
import { formatCents } from '../../../shared/money'
import { CategoryIcon } from '../components/CategoryIcon'
import { EmptyState } from '../components/EmptyState'
import { useAppShell } from '../components/AppShell'
import { getErrorMessage } from '../lib/errors'

export function DashboardPage(): React.JSX.Element {
  const { openNewExpense, openEditExpense } = useAppShell()
  const query = useQuery({ queryKey: ['dashboard'], queryFn: () => window.heima.getDashboard() })
  if (query.isLoading) return <DashboardSkeleton />
  if (query.isError) return <div className="page-error">{getErrorMessage(query.error)}</div>
  const data = query.data!
  const balanceNegative = data.monthBalanceCents < 0

  return <div className="dashboard-grid">
    <section className="summary-card expense-summary"><div className="summary-icon"><ArrowUpRight size={20} /></div><span>本月支出</span><strong>{formatCents(data.monthExpenseCents)}</strong><small>今日 {formatCents(data.todayExpenseCents)}</small></section>
    <section className="summary-card income-summary"><div className="summary-icon"><ArrowDownLeft size={20} /></div><span>本月收入</span><strong>{formatCents(data.monthIncomeCents)}</strong><small>今日 {formatCents(data.todayIncomeCents)}</small></section>
    <section className={`summary-card balance-summary ${balanceNegative ? 'negative' : ''}`}><div className="summary-icon"><Landmark size={20} /></div><span>本月结余</span><strong>{balanceNegative ? '−' : '+'}{formatCents(Math.abs(data.monthBalanceCents))}</strong><small>收入减去支出</small></section>
    <button className="quick-add-card" onClick={openNewExpense}><div><span>快速记账</span><strong>记录一笔收支</strong><small>金额 · 分类 · 保存</small></div><span className="quick-add-icon"><Plus size={24} /></span></button>

    <section className="panel recent-panel">
      <div className="panel-heading"><div><span className="eyebrow">最近动态</span><h2>最近账目</h2></div><Link to="/records">查看全部 <ArrowRight size={15} /></Link></div>
      {data.recentEntries.length === 0 ? <EmptyState title="还没有账目" description="记录第一笔收入或支出，从今天开始看清每一次资金变化。" actionLabel="记下第一笔" onAction={openNewExpense} /> :
        <div className="expense-list compact">{data.recentEntries.map((entry) => <button key={entry.id} className="expense-row" onClick={() => openEditExpense(entry)}>
          <span className="category-icon-badge" style={{ color: entry.primaryCategoryColor, background: `${entry.primaryCategoryColor}1f` }}><CategoryIcon name={entry.primaryCategoryIcon} size={20} /></span>
          <span className="expense-info"><strong>{entry.secondaryCategoryName}</strong><small>{entry.note || entry.primaryCategoryName}</small></span>
          <span className="entry-type-pill" data-type={entry.entryType}>{entry.entryType === 'income' ? '收入' : '支出'}</span>
          <span className="expense-date">{entry.spentDate.slice(5).replace('-', '/')}<small>{entry.spentTime}</small></span>
          <strong className={`expense-amount ${entry.entryType}`}>{entry.entryType === 'income' ? '+' : '−'}{formatCents(entry.amountCents)}</strong>
        </button>)}</div>}
    </section>

    <section className="panel category-panel">
      <div className="panel-heading"><div><span className="eyebrow">本月支出</span><h2>钱花在哪里</h2></div><TrendingUp size={19} className="heading-icon" /></div>
      {data.categoryTotals.length === 0 ? <div className="chart-empty"><span>暂无支出数据</span><small>记账后会自动生成占比</small></div> : <div className="category-overview">
        <div className="donut-wrap" aria-label="本月支出分类占比图"><ResponsiveContainer width="100%" height="100%"><PieChart><Pie data={data.categoryTotals} dataKey="amountCents" nameKey="categoryName" innerRadius={49} outerRadius={72} paddingAngle={3} cornerRadius={4} stroke="none">{data.categoryTotals.map((entry) => <Cell key={entry.categoryId} fill={entry.color} />)}</Pie><Tooltip formatter={(value) => formatCents(Number(value))} /></PieChart></ResponsiveContainer><div><small>本月</small><strong>{data.categoryTotals.length}</strong><span>类支出</span></div></div>
        <div className="category-legend">{data.categoryTotals.slice(0, 5).map((category) => <div key={category.categoryId}><span className="legend-icon" style={{ color: category.color, background: `${category.color}1f` }}><CategoryIcon name={category.icon} size={16} /></span><span>{category.categoryName}</span><strong>{formatCents(category.amountCents)}</strong></div>)}</div>
      </div>}
    </section>
  </div>
}

function DashboardSkeleton(): React.JSX.Element {
  return <div className="dashboard-grid loading-grid">{Array.from({ length: 6 }).map((_, index) => <div className="skeleton" key={index} />)}</div>
}
