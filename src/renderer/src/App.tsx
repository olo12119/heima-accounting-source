import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Route, Routes } from 'react-router-dom'
import { DatabaseZap } from 'lucide-react'
import { AppShell } from './components/AppShell'
import { DashboardPage } from './pages/DashboardPage'
import { RecordsPage } from './pages/RecordsPage'
import { StatisticsPage } from './pages/StatisticsPage'
import { SettingsPage } from './pages/SettingsPage'
import type { ThemeMode } from '../../shared/types'

const applyTheme = (mode: ThemeMode): (() => void) => {
  const media = window.matchMedia('(prefers-color-scheme: dark)')
  const update = (): void => {
    document.documentElement.dataset.theme = mode === 'system' ? (media.matches ? 'dark' : 'light') : mode
  }
  update()
  media.addEventListener('change', update)
  return () => media.removeEventListener('change', update)
}

export default function App(): React.JSX.Element {
  const statusQuery = useQuery({ queryKey: ['status'], queryFn: () => window.heima.getStatus(), staleTime: Infinity })
  const settingsQuery = useQuery({
    queryKey: ['settings'],
    queryFn: () => window.heima.getSettings(),
    enabled: statusQuery.data?.ready === true
  })
  useEffect(() => applyTheme(settingsQuery.data?.theme ?? 'system'), [settingsQuery.data?.theme])

  if (statusQuery.isLoading) {
    return <div className="splash"><img src="./logo.svg" alt="" /><strong>黑马记账</strong><span>正在准备你的账本…</span></div>
  }
  if (!statusQuery.data?.ready) {
    return (
      <main className="fatal-screen">
        <div className="fatal-icon"><DatabaseZap size={30} /></div>
        <span className="eyebrow">数据保护模式</span>
        <h1>账本暂时无法打开</h1>
        <p>为避免损坏或覆盖原有数据，黑马记账没有自动重建数据库。</p>
        <div className="fatal-detail"><strong>原因</strong><span>{statusQuery.data?.error ?? '数据库初始化失败'}</span></div>
        <div className="fatal-detail"><strong>数据位置</strong><code>{statusQuery.data?.databasePath}</code></div>
        <p className="muted">请保留这个文件，并根据用户指南中的“从备份恢复”章节处理。</p>
      </main>
    )
  }

  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<DashboardPage />} />
        <Route path="records" element={<RecordsPage />} />
        <Route path="statistics" element={<StatisticsPage />} />
        <Route path="settings" element={<SettingsPage />} />
      </Route>
    </Routes>
  )
}
