import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ChevronDown, ChevronUp, Eye, EyeOff, LockKeyhole, Pencil, Plus,
  RotateCcw, Trash2, X
} from 'lucide-react'
import { CATEGORY_COLOR_CHOICES, CATEGORY_ICON_NAMES } from '../../../shared/categories'
import type {
  CategoryDeleteResult, CategoryManagementItem, EntryType
} from '../../../shared/types'
import { CategoryIcon } from '../components/CategoryIcon'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { getErrorMessage } from '../lib/errors'

type CategoryDialog =
  | { kind: 'create-primary'; entryType: EntryType }
  | { kind: 'create-secondary'; parent: CategoryManagementItem }
  | { kind: 'edit'; category: CategoryManagementItem }

type CategoryAction =
  | { kind: 'create-primary'; entryType: EntryType; name: string; firstSecondaryName: string; icon: string; color: string }
  | { kind: 'create-secondary'; parentId: string; name: string }
  | { kind: 'update'; category: CategoryManagementItem; name: string; icon?: string; color?: string }
  | { kind: 'active'; category: CategoryManagementItem; active: boolean }
  | { kind: 'delete'; category: CategoryManagementItem }
  | { kind: 'reorder'; category: CategoryManagementItem; direction: 'up' | 'down' }

const iconLabels: Record<string, string> = {
  utensils: '餐具', car: '汽车', 'shopping-bag': '购物袋', house: '房屋', clapperboard: '影视',
  'heart-pulse': '健康', 'book-open': '书籍', wifi: '网络', gift: '礼物', luggage: '旅行箱',
  shapes: '综合', 'briefcase-business': '工作', 'badge-dollar-sign': '奖金', store: '商店',
  'chart-no-axes-combined': '增长', 'receipt-text': '票据', 'hand-coins': '收款',
  'circle-dollar-sign': '钱币', coffee: '咖啡', bus: '公交', 't-shirt': '服饰',
  'paw-print': '宠物', baby: '育儿', airplane: '飞机', 'game-controller': '游戏',
  'graduation-cap': '学习', 'first-aid': '医疗', wallet: '钱包'
}

export function CategoriesPage(): React.JSX.Element {
  const [entryType, setEntryType] = useState<EntryType>('expense')
  const [dialog, setDialog] = useState<CategoryDialog | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<CategoryManagementItem | null>(null)
  const [message, setMessage] = useState<{ kind: 'success' | 'error'; text: string } | null>(null)
  const queryClient = useQueryClient()
  const query = useQuery({ queryKey: ['categories-management'], queryFn: () => window.heima.getCategoriesForManagement() })

  const mutation = useMutation({
    mutationFn: async (action: CategoryAction): Promise<unknown> => {
      if (action.kind === 'create-primary') return window.heima.createCustomPrimaryCategory({
        entryType: action.entryType,
        name: action.name,
        firstSecondaryName: action.firstSecondaryName,
        icon: action.icon,
        color: action.color
      })
      if (action.kind === 'create-secondary') return window.heima.createCustomSecondaryCategory({
        parentId: action.parentId,
        name: action.name
      })
      if (action.kind === 'update') return window.heima.updateCustomCategory(action.category.id, {
        name: action.name, icon: action.icon, color: action.color
      })
      if (action.kind === 'active') return window.heima.setCustomCategoryActive(action.category.id, action.active)
      if (action.kind === 'reorder') return window.heima.reorderCustomCategory(action.category.id, action.direction)
      return window.heima.deleteCustomCategory(action.category.id)
    },
    onSuccess: async (result, action) => {
      setDialog(null)
      setDeleteTarget(null)
      if (action.kind === 'delete') {
        const mode = (result as CategoryDeleteResult).mode
        setMessage({ kind: 'success', text: mode === 'deleted' ? '自定义分类已删除' : '分类已有历史账目，已安全停用' })
      } else if (action.kind !== 'reorder') {
        setMessage({ kind: 'success', text: action.kind === 'active' ? (action.active ? '分类已启用' : '分类已停用') : '分类设置已保存' })
      }
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['categories-management'] }),
        queryClient.invalidateQueries({ queryKey: ['categories'] }),
        queryClient.invalidateQueries({ queryKey: ['frequent-categories'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
        queryClient.invalidateQueries({ queryKey: ['statistics'] })
      ])
    },
    onError: (error) => setMessage({ kind: 'error', text: getErrorMessage(error) })
  })

  const primaries = useMemo(() => (query.data ?? []).filter((category) =>
    category.parentId === null && category.entryType === entryType), [entryType, query.data])
  const childrenFor = (id: string): CategoryManagementItem[] => (query.data ?? []).filter((category) => category.parentId === id)

  return <div className="categories-page">
    <section className="category-toolbar panel">
      <div className="entry-type-switch category-type-switch">
        <button className={entryType === 'expense' ? 'active expense' : ''} onClick={() => setEntryType('expense')}>支出分类</button>
        <button className={entryType === 'income' ? 'active income' : ''} onClick={() => setEntryType('income')}>收入分类</button>
      </div>
      <div><p>系统分类会保持锁定；你创建的分类可以编辑、排序和停用。</p><button className="button primary" onClick={() => setDialog({ kind: 'create-primary', entryType })}><Plus size={16} />新增一级分类</button></div>
    </section>

    {message && <div className={`operation-message category-message ${message.kind}`} role="status">{message.text}</div>}
    {query.isLoading ? <div className="category-loading skeleton" /> : query.isError ? <div className="page-error">{getErrorMessage(query.error)}</div> :
      <div className="category-manager-grid">{primaries.map((primary) => {
        const children = childrenFor(primary.id)
        return <section className={`panel category-manager-card ${primary.isActive ? '' : 'inactive'}`} key={primary.id}>
          <div className="category-card-heading">
            <span className="category-art" style={{ '--category-color': primary.color } as React.CSSProperties}><CategoryIcon name={primary.icon} size={28} /></span>
            <div><strong>{primary.name}</strong><span>{primary.isSystem ? <><LockKeyhole size={12} />系统预设</> : <>自定义 · {primary.usageCount} 笔账目</>}</span></div>
            {!primary.isSystem && <CategoryActions category={primary} busy={mutation.isPending} onEdit={() => setDialog({ kind: 'edit', category: primary })} onAction={(action) => {
              if (action === 'delete') setDeleteTarget(primary)
              else if (action === 'active') mutation.mutate({ kind: 'active', category: primary, active: !primary.isActive })
              else mutation.mutate({ kind: 'reorder', category: primary, direction: action })
            }} />}
          </div>
          <div className="secondary-category-list">
            {children.map((secondary) => <div className={secondary.isActive ? '' : 'inactive'} key={secondary.id}>
              <span className="secondary-dot" style={{ background: primary.color }} />
              <span><strong>{secondary.name}</strong>{secondary.isSystem ? <small>系统</small> : <small>自定义 · {secondary.usageCount}笔</small>}</span>
              {!secondary.isSystem && <CategoryActions compact category={secondary} busy={mutation.isPending} onEdit={() => setDialog({ kind: 'edit', category: secondary })} onAction={(action) => {
                if (action === 'delete') setDeleteTarget(secondary)
                else if (action === 'active') mutation.mutate({ kind: 'active', category: secondary, active: !secondary.isActive })
                else mutation.mutate({ kind: 'reorder', category: secondary, direction: action })
              }} />}
            </div>)}
          </div>
          <button className="add-secondary-button" disabled={!primary.isActive} onClick={() => setDialog({ kind: 'create-secondary', parent: primary })}><Plus size={14} />添加二级分类</button>
        </section>
      })}</div>}

    {dialog && <CategoryFormDialog dialog={dialog} busy={mutation.isPending} onClose={() => setDialog(null)} onSubmit={(action) => mutation.mutate(action)} />}
    <ConfirmDialog open={deleteTarget !== null} title={deleteTarget?.usageCount ? '停用这个分类？' : '删除这个自定义分类？'}
      description={deleteTarget ? (deleteTarget.usageCount > 0
        ? `“${deleteTarget.name}”已有 ${deleteTarget.usageCount} 笔历史账目，为保护数据，它会被停用并从新记账中隐藏，历史账单仍保留。`
        : `“${deleteTarget.name}”尚未被账目使用，可以安全删除。${deleteTarget.parentId === null ? '它下面的自定义二级分类也会一起删除。' : ''}`) : ''}
      confirmLabel={deleteTarget?.usageCount ? '确认停用' : '确认删除'} danger busy={mutation.isPending}
      onCancel={() => setDeleteTarget(null)} onConfirm={() => deleteTarget && mutation.mutate({ kind: 'delete', category: deleteTarget })} />
  </div>
}

function CategoryActions({ category, compact = false, busy, onEdit, onAction }: {
  category: CategoryManagementItem
  compact?: boolean
  busy: boolean
  onEdit: () => void
  onAction: (action: 'up' | 'down' | 'active' | 'delete') => void
}): React.JSX.Element {
  return <div className={`category-actions ${compact ? 'compact' : ''}`}>
    <button aria-label={`编辑${category.name}`} disabled={busy} onClick={onEdit}><Pencil size={14} /></button>
    <button aria-label={`${category.name}上移`} disabled={busy} onClick={() => onAction('up')}><ChevronUp size={14} /></button>
    <button aria-label={`${category.name}下移`} disabled={busy} onClick={() => onAction('down')}><ChevronDown size={14} /></button>
    <button aria-label={`${category.isActive ? '停用' : '启用'}${category.name}`} disabled={busy} onClick={() => onAction('active')}>{category.isActive ? <EyeOff size={14} /> : <Eye size={14} />}</button>
    <button className="danger" aria-label={`删除${category.name}`} disabled={busy} onClick={() => onAction('delete')}><Trash2 size={14} /></button>
  </div>
}

function CategoryFormDialog({ dialog, busy, onClose, onSubmit }: {
  dialog: CategoryDialog
  busy: boolean
  onClose: () => void
  onSubmit: (action: CategoryAction) => void
}): React.JSX.Element {
  const editing = dialog.kind === 'edit' ? dialog.category : null
  const isPrimary = dialog.kind === 'create-primary' || editing?.parentId === null
  const [name, setName] = useState(editing?.name ?? '')
  const [firstSecondaryName, setFirstSecondaryName] = useState('其他')
  const [icon, setIcon] = useState(editing?.icon ?? (dialog.kind === 'create-primary' && dialog.entryType === 'income' ? 'wallet' : 'shapes'))
  const [color, setColor] = useState(editing?.color ?? (dialog.kind === 'create-primary' && dialog.entryType === 'income' ? '#2d9b72' : '#d98257'))
  const title = dialog.kind === 'create-primary' ? '新增一级分类' : dialog.kind === 'create-secondary' ? `在“${dialog.parent.name}”下添加` : `编辑“${editing?.name}”`

  const submit = (event: React.FormEvent): void => {
    event.preventDefault()
    const trimmedName = name.trim()
    if (!trimmedName) return
    if (dialog.kind === 'create-primary') {
      if (!firstSecondaryName.trim()) return
      onSubmit({ kind: 'create-primary', entryType: dialog.entryType, name: trimmedName, firstSecondaryName: firstSecondaryName.trim(), icon, color })
    } else if (dialog.kind === 'create-secondary') {
      onSubmit({ kind: 'create-secondary', parentId: dialog.parent.id, name: trimmedName })
    } else {
      onSubmit({ kind: 'update', category: dialog.category, name: trimmedName, icon: isPrimary ? icon : undefined, color: isPrimary ? color : undefined })
    }
  }

  return <div className="dialog-backdrop category-dialog-backdrop" role="presentation">
    <section className="category-dialog" role="dialog" aria-modal="true" aria-labelledby="category-dialog-title">
      <div className="dialog-header"><div><span className="eyebrow">自定义分类</span><h2 id="category-dialog-title">{title}</h2></div><button className="icon-button" aria-label="关闭" onClick={onClose}><X size={19} /></button></div>
      <form onSubmit={submit}>
        <label className="category-name-field"><span>{isPrimary ? '一级分类名称' : '二级分类名称'}</span><input aria-label={isPrimary ? '一级分类名称' : '二级分类名称'} autoFocus maxLength={16} value={name} onChange={(event) => setName(event.target.value)} placeholder={isPrimary ? '例如：家庭生活' : '例如：公司食堂'} required /></label>
        {dialog.kind === 'create-primary' && <label className="category-name-field"><span>第一个二级分类</span><input aria-label="第一个二级分类" maxLength={16} value={firstSecondaryName} onChange={(event) => setFirstSecondaryName(event.target.value)} placeholder="例如：日常开销" required /><small>一级分类至少需要一个二级分类才能用于记账。</small></label>}
        {isPrimary && <>
          <fieldset className="icon-picker"><legend>选择双色图标</legend><div>{CATEGORY_ICON_NAMES.map((iconName) => <button type="button" title={iconLabels[iconName]} aria-label={`选择${iconLabels[iconName]}图标`} className={icon === iconName ? 'selected' : ''} key={iconName} onClick={() => setIcon(iconName)}><CategoryIcon name={iconName} size={23} /></button>)}</div></fieldset>
          <fieldset className="color-picker"><legend>选择分类颜色</legend><div>{CATEGORY_COLOR_CHOICES.map((choice) => <button type="button" aria-label={`选择颜色${choice}`} className={color === choice ? 'selected' : ''} style={{ '--choice-color': choice } as React.CSSProperties} key={choice} onClick={() => setColor(choice)} />)}</div></fieldset>
          <div className="category-preview"><span className="category-art" style={{ '--category-color': color } as React.CSSProperties}><CategoryIcon name={icon} size={30} /></span><div><small>效果预览</small><strong>{name.trim() || '你的分类'}</strong></div><RotateCcw size={15} /></div>
        </>}
        <div className="form-footer"><span>系统预设分类不会被改变</span><div><button type="button" className="button ghost" onClick={onClose}>取消</button><button type="submit" className="button primary" disabled={busy}>{busy ? '保存中…' : '保存分类'}</button></div></div>
      </form>
    </section>
  </div>
}
