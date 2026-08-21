import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Check, DatabaseBackup, Download, FileSpreadsheet, HardDrive, Laptop, Moon, RotateCcw, Sun } from 'lucide-react'
import type { ColorTheme, ThemeMode } from '../../../shared/types'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { getErrorMessage } from '../lib/errors'

const themes: Array<{ value: ThemeMode; label: string; description: string; icon: React.ComponentType<{ size?: number }> }> = [
  { value: 'light', label: '浅色', description: '明亮清爽，适合白天', icon: Sun },
  { value: 'dark', label: '深色', description: '柔和护眼，适合夜晚', icon: Moon },
  { value: 'system', label: '跟随系统', description: '自动匹配电脑设置', icon: Laptop }
]

const colorThemes: Array<{ value: ColorTheme; label: string; description: string; colors: [string, string, string] }> = [
  { value: 'forest', label: '黑马墨绿', description: '沉稳、清晰的品牌配色', colors: ['#19664f', '#d6ab69', '#cf684e'] },
  { value: 'ocean', label: '雾蓝海岸', description: '安静清爽的蓝灰气息', colors: ['#316f8f', '#69a9b7', '#d47a67'] },
  { value: 'amber', label: '暖杏琥珀', description: '温暖柔和的生活感', colors: ['#9b633d', '#d89a55', '#c96358'] },
  { value: 'wisteria', label: '紫藤暮色', description: '克制精致的灰紫色', colors: ['#6d5b91', '#a887b7', '#d17378'] }
]

export function SettingsPage(): React.JSX.Element {
  const [confirmRestore, setConfirmRestore] = useState(false)
  const [message, setMessage] = useState<{ kind: 'success' | 'error'; text: string } | null>(null)
  const queryClient = useQueryClient()
  const settingsQuery = useQuery({ queryKey: ['settings'], queryFn: () => window.heima.getSettings() })
  const statusQuery = useQuery({ queryKey: ['status'], queryFn: () => window.heima.getStatus(), staleTime: Infinity })
  const themeMutation = useMutation({
    mutationFn: (theme: ThemeMode) => window.heima.setTheme(theme),
    onSuccess: async () => queryClient.invalidateQueries({ queryKey: ['settings'] })
  })
  const colorThemeMutation = useMutation({
    mutationFn: (colorTheme: ColorTheme) => window.heima.setColorTheme(colorTheme),
    onSuccess: async () => queryClient.invalidateQueries({ queryKey: ['settings'] })
  })
  const operation = useMutation({
    mutationFn: async (type: 'csv' | 'backup' | 'restore') => {
      if (type === 'csv') return window.heima.exportCsv()
      if (type === 'backup') return window.heima.exportBackup()
      return window.heima.restoreBackup()
    },
    onSuccess: async (result, type) => {
      setConfirmRestore(false)
      if (result.canceled) return
      const labels = { csv: 'CSV 已导出', backup: '完整备份已保存', restore: `已恢复 ${result.count ?? 0} 笔账目` }
      setMessage({ kind: 'success', text: `${labels[type]}${result.path ? `：${result.path}` : ''}` })
      if (type === 'restore') await queryClient.invalidateQueries()
    },
    onError: (error) => {
      setConfirmRestore(false)
      setMessage({ kind: 'error', text: getErrorMessage(error) })
    }
  })

  return (
    <div className="settings-grid">
      <section className="panel settings-section appearance-section">
        <div className="settings-heading"><div className="settings-icon"><Sun size={20} /></div><div><h2>外观模式</h2><p>选择一个让眼睛舒服的界面。</p></div></div>
        <div className="setting-subheading"><strong>颜色主题</strong><span>主题与明暗可以自由组合</span></div>
        <div className="palette-grid">
          {colorThemes.map((palette) => {
            const selected = settingsQuery.data?.colorTheme === palette.value
            return <button key={palette.value} className={selected ? 'selected' : ''} onClick={() => colorThemeMutation.mutate(palette.value)}>
              <span className="palette-preview">{palette.colors.map((color) => <i key={color} style={{ background: color }} />)}</span>
              <span><strong>{palette.label}</strong><small>{palette.description}</small></span>{selected && <Check className="theme-check" size={17} />}
            </button>
          })}
        </div>
        <div className="setting-subheading"><strong>明暗方式</strong><span>“跟随系统”会随电脑自动切换</span></div>
        <div className="theme-grid">
          {themes.map((theme) => {
            const Icon = theme.icon
            const selected = settingsQuery.data?.theme === theme.value
            return <button key={theme.value} className={selected ? 'selected' : ''} onClick={() => themeMutation.mutate(theme.value)}>
              <Icon size={21} /><span><strong>{theme.label}</strong><small>{theme.description}</small></span>{selected && <Check className="theme-check" size={17} />}
            </button>
          })}
        </div>
      </section>

      <section className="panel settings-section data-section">
        <div className="settings-heading"><div className="settings-icon"><DatabaseBackup size={20} /></div><div><h2>导出与备份</h2><p>把账目带走，或为全部数据留一份副本。</p></div></div>
        <div className="data-actions">
          <div><span className="action-icon"><FileSpreadsheet size={20} /></span><span><strong>导出 CSV 表格</strong><small>导出全部账目，可使用 Excel 打开</small></span><button className="button secondary" disabled={operation.isPending} onClick={() => operation.mutate('csv')}><Download size={16} />导出</button></div>
          <div><span className="action-icon"><HardDrive size={20} /></span><span><strong>导出完整备份</strong><small>包含全部账目与设置，可供以后恢复</small></span><button className="button secondary" disabled={operation.isPending} onClick={() => operation.mutate('backup')}><Download size={16} />备份</button></div>
          <div><span className="action-icon warm"><RotateCcw size={20} /></span><span><strong>从备份恢复</strong><small>恢复前会自动保存当前数据</small></span><button className="button ghost" disabled={operation.isPending} onClick={() => setConfirmRestore(true)}><RotateCcw size={16} />恢复</button></div>
        </div>
        {message && <div className={`operation-message ${message.kind}`} role="status">{message.text}</div>}
      </section>

      <section className="panel settings-section storage-section">
        <div className="settings-heading"><div className="settings-icon"><HardDrive size={20} /></div><div><h2>本地数据</h2><p>你的账目只保存在这台电脑上。</p></div></div>
        <div className="storage-info"><span className="status-dot" /><div><strong>数据库运行正常</strong><code>{statusQuery.data?.databasePath}</code></div></div>
        <div className="privacy-points"><span><Check size={15} />无需联网</span><span><Check size={15} />无需账号</span><span><Check size={15} />不会上传数据</span></div>
      </section>

      <section className="about-card"><img src="./logo.svg" alt="" /><div><strong>黑马记账</strong><span>版本 {statusQuery.data?.version ?? '1.0.0'}</span></div><p>简单、清楚地知道自己的钱花到哪里去了。</p></section>

      <ConfirmDialog
        open={confirmRestore}
        title="从备份恢复全部数据？"
        description="恢复会用备份中的账目替换当前账目。开始前，应用会自动保存一份当前数据，过程中发生错误则不会更改现有账本。"
        confirmLabel="选择备份并恢复"
        busy={operation.isPending}
        onCancel={() => setConfirmRestore(false)}
        onConfirm={() => operation.mutate('restore')}
      />
    </div>
  )
}
