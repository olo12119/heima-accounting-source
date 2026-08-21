import { AlertTriangle, X } from 'lucide-react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  danger = false,
  busy = false,
  onConfirm,
  onCancel
}: {
  open: boolean
  title: string
  description: string
  confirmLabel: string
  danger?: boolean
  busy?: boolean
  onConfirm: () => void
  onCancel: () => void
}): React.JSX.Element | null {
  const reduceMotion = useReducedMotion()
  return (
    <AnimatePresence>{open && <motion.div className="dialog-backdrop" role="presentation" initial={reduceMotion ? false : { opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onMouseDown={(event) => {
      if (event.currentTarget === event.target && !busy) onCancel()
    }}>
      <motion.section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="confirm-title" initial={reduceMotion ? false : { opacity: 0, scale: 0.9, y: 18 }} animate={{ opacity: 1, scale: 1, y: 0 }} exit={{ opacity: 0, scale: 0.95, y: 8 }} transition={reduceMotion ? { duration: 0 } : { type: 'spring', stiffness: 330, damping: 26 }}>
        <button className="icon-button dialog-close" aria-label="关闭" onClick={onCancel} disabled={busy}><X size={18} /></button>
        <div className={`confirm-icon ${danger ? 'danger' : ''}`}><AlertTriangle size={23} /></div>
        <h2 id="confirm-title">{title}</h2>
        <p>{description}</p>
        <div className="dialog-actions">
          <button className="button ghost" onClick={onCancel} disabled={busy}>取消</button>
          <button className={`button ${danger ? 'danger' : 'primary'}`} onClick={onConfirm} disabled={busy}>
            {busy ? '处理中…' : confirmLabel}
          </button>
        </div>
      </motion.section>
    </motion.div>}</AnimatePresence>
  )
}
