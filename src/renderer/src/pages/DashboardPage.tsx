import { useQuery } from '@tanstack/react-query'
import { ArrowDownLeft, ArrowRight, ArrowUpRight, CalendarClock, Plus, Sparkles, TrendingUp, WalletCards } from 'lucide-react'
import { Area, AreaChart, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis } from 'recharts'
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

  return <div className="dashboard-v2">
    <section className="money-hero">
      <div className="money-hero-copy"><span className="hero-kicker"><Sparkles size={14} />本月资金视图</span><p>当前结余</p><strong className={balanceNegative ? 'negative' : ''}>{balanceNegative ? '−' : '+'}{formatCents(Math.abs(data.monthBalanceCents))}</strong><div className="hero-flow"><span><ArrowDownLeft size={15} />收入 <b>+{formatCents(data.monthIncomeCents)}</b></span><span><ArrowUpRight size={15} />支出 <b>−{formatCents(data.monthExpenseCents)}</b></span></div><button onClick={openNewExpense}><Plus size={17} />快速记一笔</button></div>
      <div className="hero-chart"><div><span>最近7天资金流动</span><small>悬停查看每日金额</small></div><ResponsiveContainer width="100%" height="100%"><AreaChart data={data.dailyCashFlow} margin={{ top: 12, right: 4, bottom: 0, left: 4 }}><defs><linearGradient id="incomeGlow" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#7de0b7" stopOpacity=".5"/><stop offset="100%" stopColor="#7de0b7" stopOpacity="0"/></linearGradient><linearGradient id="expenseGlow" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#ff9b7f" stopOpacity=".45"/><stop offset="100%" stopColor="#ff9b7f" stopOpacity="0"/></linearGradient></defs><XAxis dataKey="date" tickFormatter={(value) => String(value).slice(5)} axisLine={false} tickLine={false} tick={{ fill: 'rgba(255,255,255,.55)', fontSize: 9 }} /><Tooltip formatter={(value) => formatCents(Number(value))} /><Area type="monotone" dataKey="incomeCents" name="收入" stroke="#87e4be" strokeWidth={2.5} fill="url(#incomeGlow)" /><Area type="monotone" dataKey="expenseCents" name="支出" stroke="#ff9d83" strokeWidth={2.5} fill="url(#expenseGlow)" /></AreaChart></ResponsiveContainer></div>
    </section>

    <div className="home-insight-grid">
      <section className="insight-card expense"><span className="insight-orb"><ArrowUpRight size={20} /></span><div><small>本月支出</small><strong>{formatCents(data.monthExpenseCents)}</strong><span>今日 −{formatCents(data.todayExpenseCents)}</span></div></section>
      <section className="insight-card income"><span className="insight-orb"><ArrowDownLeft size={20} /></span><div><small>本月收入</small><strong>{formatCents(data.monthIncomeCents)}</strong><span>今日 +{formatCents(data.todayIncomeCents)}</span></div></section>
      <Link className="insight-card budget" to="/planning"><span className="insight-orb"><WalletCards size={20} /></span><div><small>预算进度</small><strong>{data.budget.totalCents ? `${data.budget.percent}%` : '去设置'}</strong><span>{data.budget.totalCents ? `剩余 ${formatCents(data.budget.remainingCents)}` : '为本月消费留出边界'}</span></div><ArrowRight size={17} /></Link>
      <Link className="insight-card schedule" to="/planning"><span className="insight-orb"><CalendarClock size={20} /></span><div><small>待确认计划</small><strong>{data.pendingTemplates}</strong><span>{data.pendingTemplates ? '有周期账目等待确认' : '目前没有到期账目'}</span></div><ArrowRight size={17} /></Link>
    </div>

    <div className="home-content-grid"><section className="panel recent-panel">
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
    </section></div>
  </div>
}

function DashboardSkeleton(): React.JSX.Element {
  return <div className="dashboard-v2 loading-grid">{Array.from({ length: 6 }).map((_, index) => <div className="skeleton" key={index} />)}</div>
}
