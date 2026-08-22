import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { BadgeCheck, Check, DatabaseBackup, Download, FileSpreadsheet, HardDrive, Laptop, LockKeyhole, Moon, Palette, RotateCcw, ShieldCheck, Sun, Upload } from 'lucide-react'
import { motion, useReducedMotion } from 'motion/react'
import type { ColorTheme, ThemeMode } from '../../../shared/types'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { getErrorMessage } from '../lib/errors'
import { MOOD_THEMES } from '../lib/theme-options'

const themes: Array<{ value: ThemeMode; label: string; description: string; icon: React.ComponentType<{ size?: number }> }> = [
  { value: 'light', label: '浅色', description: '明亮清爽，适合白天', icon: Sun },
  { value: 'dark', label: '深色', description: '柔和护眼，适合夜晚', icon: Moon },
  { value: 'system', label: '跟随系统', description: '自动匹配电脑设置', icon: Laptop }
]

export function SettingsPage(): React.JSX.Element {
  const reduceMotion = useReducedMotion()
  const [confirmRestore, setConfirmRestore] = useState(false)
  const [message, setMessage] = useState<{ kind: 'success' | 'error'; text: string } | null>(null)
  const [currentPin, setCurrentPin] = useState('')
  const [newPin, setNewPin] = useState('')
  const queryClient = useQueryClient()
  const settingsQuery = useQuery({ queryKey: ['settings'], queryFn: () => window.heima.getSettings() })
  const statusQuery = useQuery({ queryKey: ['status'], queryFn: () => window.heima.getStatus(), staleTime: Infinity })
  const lockQuery = useQuery({ queryKey: ['lock-status'], queryFn: () => window.heima.getLockStatus() })
  const themeMutation = useMutation({
    mutationFn: (theme: ThemeMode) => window.heima.setTheme(theme),
    onSuccess: async () => queryClient.invalidateQueries({ queryKey: ['settings'] })
  })
  const colorThemeMutation = useMutation({
    mutationFn: (colorTheme: ColorTheme) => window.heima.setColorTheme(colorTheme),
    onSuccess: async () => queryClient.invalidateQueries({ queryKey: ['settings'] })
  })
  const operation = useMutation({
    mutationFn: async (type: 'csv' | 'backup' | 'restore' | 'import') => {
      if (type === 'csv') return window.heima.exportCsv()
      if (type === 'backup') return window.heima.exportBackup()
      if (type === 'import') return window.heima.importCsv()
      return window.heima.restoreBackup()
    },
    onSuccess: async (result, type) => {
      setConfirmRestore(false)
      if (result.canceled) return
      const labels = { csv: 'CSV 已导出', backup: '完整备份已保存', restore: `已恢复 ${result.count ?? 0} 笔账目`, import: `已导入 ${'importedCount' in result ? result.importedCount ?? 0 : 0} 笔账目` }
      setMessage({ kind: 'success', text: `${labels[type]}${result.path ? `：${result.path}` : ''}` })
      if (type === 'restore' || type === 'import') await queryClient.invalidateQueries()
    },
    onError: (error) => {
      setConfirmRestore(false)
      setMessage({ kind: 'error', text: getErrorMessage(error) })
    }
  })
  const lockMutation = useMutation({
    mutationFn: () => window.heima.setLockPin(lockQuery.data?.enabled ? currentPin : null, lockQuery.data?.enabled ? null : newPin),
    onSuccess: async () => {
      setCurrentPin(''); setNewPin(''); setMessage({ kind: 'success', text: lockQuery.data?.enabled ? '隐私密码已关闭' : '隐私密码已开启' })
      await queryClient.invalidateQueries({ queryKey: ['lock-status'] })
    },
    onError: (error) => setMessage({ kind: 'error', text: getErrorMessage(error) })
  })

  const enter = (delay: number) => reduceMotion ? {} : {
    initial: { opacity: 0, y: 16 }, animate: { opacity: 1, y: 0 },
    transition: { delay, duration: 0.42, ease: [0.22, 1, 0.36, 1] as const }
  }

  return (
    <div className="settings-page">
      <motion.section className="settings-overview" {...enter(0)}>
        <div className="settings-overview-mark"><ShieldCheck size={24} /></div>
        <div><span>LOCAL FIRST · 本地优先</span><strong>外观与数据，由你自己掌控</strong><p>主题、备份和隐私都只作用于这台电脑，不需要登录任何账号。</p></div>
        <div className="settings-health"><i /><span><strong>账本运行正常</strong><small>数据未上传网络</small></span></div>
      </motion.section>

      <div className="settings-grid">
      <motion.section className="panel settings-section appearance-section" {...enter(0.06)}>
        <div className="settings-heading"><div className="settings-icon"><Sun size={20} /></div><div><h2>心情主题</h2><p>同一套操作方式，换一种今天喜欢的氛围。</p></div></div>
        <div className="setting-subheading"><strong>完整视觉主题</strong><span>颜色、卡片、图标底座和动效会一起变化</span></div>
        <div className="palette-grid">
          {MOOD_THEMES.map((palette) => {
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
      </motion.section>

      <motion.section className="panel settings-section privacy-section" {...enter(0.12)}>
        <div className="settings-heading"><div className="settings-icon"><LockKeyhole size={20} /></div><div><h2>隐私密码</h2><p>防止身边的人随手打开查看账目。</p></div></div>
        <div className="pin-setting">
          <div><strong>{lockQuery.data?.enabled ? '保护已开启' : '保护未开启'}</strong><small>密码仅在本机校验；它不是数据库文件加密。</small></div>
          <input aria-label={lockQuery.data?.enabled ? '当前隐私密码' : '新隐私密码'} type="password" inputMode="numeric" maxLength={12} value={lockQuery.data?.enabled ? currentPin : newPin} onChange={(event) => lockQuery.data?.enabled ? setCurrentPin(event.target.value.replace(/\D/g, '')) : setNewPin(event.target.value.replace(/\D/g, ''))} placeholder={lockQuery.data?.enabled ? '输入当前密码以关闭' : '设置4至12位数字'} />
          <button className={`button ${lockQuery.data?.enabled ? 'ghost' : 'primary'}`} disabled={lockMutation.isPending || (lockQuery.data?.enabled ? currentPin.length < 4 : newPin.length < 4)} onClick={() => lockMutation.mutate()}>{lockQuery.data?.enabled ? '关闭密码' : '开启密码'}</button>
        </div>
      </motion.section>

      <motion.section className="panel settings-section storage-section" {...enter(0.16)}>
        <div className="settings-heading"><div className="settings-icon"><HardDrive size={20} /></div><div><h2>本地数据</h2><p>你的账目只保存在这台电脑上。</p></div></div>
        <div className="storage-info"><span className="status-dot" /><div><strong>数据库运行正常</strong><code>{statusQuery.data?.databasePath}</code></div></div>
        <div className="privacy-points"><span><Check size={15} />无需联网</span><span><Check size={15} />无需账号</span><span><Check size={15} />不会上传数据</span></div>
      </motion.section>

      <motion.section className="panel settings-section data-section" {...enter(0.2)}>
        <div className="settings-heading"><div className="settings-icon"><DatabaseBackup size={20} /></div><div><h2>导出与备份</h2><p>把账目带走，或为全部数据留一份副本。</p></div></div>
        <div className="data-actions">
          <div><span className="action-icon"><Upload size={20} /></span><span><strong>导入 CSV 账单</strong><small>导入前预览数量、跳过重复并自动备份</small></span><button className="button secondary" disabled={operation.isPending} onClick={() => operation.mutate('import')}><Upload size={16} />导入</button></div>
          <div><span className="action-icon"><FileSpreadsheet size={20} /></span><span><strong>导出 CSV 表格</strong><small>导出全部账目，可使用 Excel 打开</small></span><button className="button secondary" disabled={operation.isPending} onClick={() => operation.mutate('csv')}><Download size={16} />导出</button></div>
          <div><span className="action-icon"><HardDrive size={20} /></span><span><strong>导出完整备份</strong><small>包含全部账目与设置，可供以后恢复</small></span><button className="button secondary" disabled={operation.isPending} onClick={() => operation.mutate('backup')}><Download size={16} />备份</button></div>
          <div><span className="action-icon warm"><RotateCcw size={20} /></span><span><strong>从备份恢复</strong><small>恢复前会自动保存当前数据</small></span><button className="button ghost" disabled={operation.isPending} onClick={() => setConfirmRestore(true)}><RotateCcw size={16} />恢复</button></div>
        </div>
        {message && <div className={`operation-message ${message.kind}`} role="status">{message.text}</div>}
      </motion.section>
      </div>

      <motion.section className="about-strip" {...enter(0.25)}>
        <img src="./logo-app-v3.png" alt="" />
        <div><strong>黑马记账</strong><span>版本 {statusQuery.data?.version ?? '1.0.0'} · 本地个人收支账本</span></div>
        <span><BadgeCheck size={16} />数据由你保管</span>
        <span><Palette size={16} />界面可自由搭配</span>
      </motion.section>

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
