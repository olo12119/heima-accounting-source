import { createContext, useContext, useEffect, useRef, useState } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { BarChart3, CalendarClock, Database, Home, Plus, ReceiptText, ShieldCheck, Tags } from 'lucide-react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import type { Expense } from '../../../shared/types'
import { ExpenseFormDialog } from './ExpenseFormDialog'

type ShellContextValue = {
  openNewExpense: () => void
  openEditExpense: (expense: Expense) => void
}

const ShellContext = createContext<ShellContextValue | null>(null)
export const useAppShell = (): ShellContextValue => useContext(ShellContext)!

const pages: Record<string, { title: string; subtitle: string }> = {
  '/': { title: '今天过得怎么样？', subtitle: '不只记下数字，也看见生活的节奏。' },
  '/records': { title: '账单', subtitle: '每一笔来去，都有清楚的线索。' },
  '/statistics': { title: '统计', subtitle: '读懂资金变化，让下一次选择更从容。' },
  '/categories': { title: '分类管理', subtitle: '让常用分类更顺手，也保留自己的习惯。' },
  '/planning': { title: '预算与计划', subtitle: '提前安排固定收支，为生活留出余地。' },
  '/settings': { title: '数据与设置', subtitle: '外观、隐私和备份，都在这里安心管理。' }
}

export function AppShell(): React.JSX.Element {
  const [dialogExpense, setDialogExpense] = useState<Expense | null | undefined>(undefined)
  const location = useLocation()
  const reduceMotion = useReducedMotion()
  const mainAreaRef = useRef<HTMLElement>(null)
  useEffect(() => { mainAreaRef.current?.scrollTo({ top: 0 }) }, [location.pathname])
  const page = pages[location.pathname] ?? pages['/']!
  const context = {
    openNewExpense: () => setDialogExpense(null),
    openEditExpense: (expense: Expense) => setDialogExpense(expense)
  }

  return (
    <ShellContext.Provider value={context}>
      <div className="app-shell">
        <aside className="sidebar">
          <div className="brand">
            <motion.img
              src="./logo-app-v2.png"
              alt=""
              initial={reduceMotion ? false : { opacity: 0, scale: 0.72, rotate: -8 }}
              animate={{ opacity: 1, scale: 1, rotate: 0 }}
              transition={{ type: 'spring', stiffness: 220, damping: 18 }}
            />
            <div><strong>黑马记账</strong><span>让每一笔都有去处</span></div>
          </div>
          <nav aria-label="主导航">
            <NavItem to="/" label="首页" end icon={Home} />
            <NavItem to="/records" label="账单" icon={ReceiptText} />
            <NavItem to="/statistics" label="统计" icon={BarChart3} />
            <NavItem to="/categories" label="分类管理" shortLabel="分类" icon={Tags} />
            <NavItem to="/planning" label="预算与计划" shortLabel="计划" icon={CalendarClock} />
            <NavItem to="/settings" label="数据与设置" shortLabel="设置" icon={Database} />
          </nav>
          <div className="sidebar-note">
            <ShieldCheck size={18} />
            <div><strong>只属于你的账本</strong><span>本地保存 · 无需账号</span></div>
          </div>
        </aside>
        <main className="main-area" ref={mainAreaRef}>
          <header className="topbar">
            <motion.div key={location.pathname} initial={reduceMotion ? false : { opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }}>
              <h1>{page.title}</h1><p>{page.subtitle}</p>
            </motion.div>
            <button className="button primary add-button" onClick={context.openNewExpense}><Plus size={18} />记一笔<span className="button-shine" /></button>
          </header>
          <div className="page-content">
            <motion.div
              className="page-stage"
              key={location.pathname}
              initial={reduceMotion ? false : { opacity: 0, y: 14, scale: 0.995 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              transition={{ duration: reduceMotion ? 0 : 0.3, ease: [0.22, 1, 0.36, 1] }}
            ><Outlet /></motion.div>
          </div>
        </main>
        <button className="mobile-add" aria-label="记一笔" onClick={context.openNewExpense}><Plus size={23} /></button>
      </div>
      <AnimatePresence>{dialogExpense !== undefined && <ExpenseFormDialog expense={dialogExpense} onClose={() => setDialogExpense(undefined)} />}</AnimatePresence>
    </ShellContext.Provider>
  )
}

function NavItem({ to, label, shortLabel = label, icon: Icon, end = false }: { to: string; label: string; shortLabel?: string; icon: React.ComponentType<{ size?: number }>; end?: boolean }): React.JSX.Element {
  return <NavLink to={to} end={end}>{({ isActive }) => <>
    <span className="nav-icon"><Icon size={19} />{isActive && <motion.i layoutId="active-nav" transition={{ type: 'spring', stiffness: 340, damping: 30 }} />}</span>
    <span className="nav-label" data-short={shortLabel}>{label}</span>
  </>}</NavLink>
}
