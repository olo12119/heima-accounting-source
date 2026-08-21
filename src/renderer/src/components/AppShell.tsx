import { createContext, useContext, useState } from 'react'
import { BarChart3, Database, Home, Plus, ReceiptText, Tags } from 'lucide-react'
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
  '/': { title: '今天过得怎么样？', subtitle: '每一笔小记录，都会让生活更清楚。' },
  '/records': { title: '账单', subtitle: '收入与支出，每一笔都清清楚楚。' },
  '/statistics': { title: '统计', subtitle: '看见资金的来处与去向，做更从容的选择。' },
  '/categories': { title: '分类管理', subtitle: '保留可靠的系统分类，也建立属于你的分类。' },
  '/settings': { title: '数据与设置', subtitle: '管理外观、导出与本地备份。' }
}

export function AppShell(): React.JSX.Element {
  const [dialogExpense, setDialogExpense] = useState<Expense | null | undefined>(undefined)
  const location = useLocation()
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
            <img src="./logo.svg" alt="" />
            <div><strong>黑马记账</strong><span>清楚每一笔</span></div>
          </div>
          <nav aria-label="主导航">
            <NavLink to="/" end><Home size={19} /><span>首页</span></NavLink>
            <NavLink to="/records"><ReceiptText size={19} /><span>账单</span></NavLink>
            <NavLink to="/statistics"><BarChart3 size={19} /><span>统计</span></NavLink>
            <NavLink to="/categories"><Tags size={19} /><span>分类管理</span></NavLink>
            <NavLink to="/settings"><Database size={19} /><span>数据与设置</span></NavLink>
          </nav>
          <div className="sidebar-note">
            <span className="status-dot" />
            <div><strong>本地安全保存</strong><span>无需联网 · 无需账号</span></div>
          </div>
        </aside>
        <main className="main-area">
          <header className="topbar">
            <div><h1>{page.title}</h1><p>{page.subtitle}</p></div>
            <button className="button primary add-button" onClick={context.openNewExpense}><Plus size={18} />记一笔</button>
          </header>
          <div className="page-content"><Outlet /></div>
        </main>
      </div>
      {dialogExpense !== undefined && <ExpenseFormDialog expense={dialogExpense} onClose={() => setDialogExpense(undefined)} />}
    </ShellContext.Provider>
  )
}
