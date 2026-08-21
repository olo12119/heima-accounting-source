import { ReceiptText } from 'lucide-react'

export function EmptyState({
  title,
  description,
  actionLabel,
  onAction
}: {
  title: string
  description: string
  actionLabel?: string
  onAction?: () => void
}): React.JSX.Element {
  return (
    <div className="empty-state">
      <div className="empty-icon"><ReceiptText size={26} /></div>
      <h3>{title}</h3>
      <p>{description}</p>
      {actionLabel && onAction && <button className="button primary" onClick={onAction}>{actionLabel}</button>}
    </div>
  )
}
