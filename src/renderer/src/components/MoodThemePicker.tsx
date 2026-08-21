import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Check, Palette, Settings2 } from 'lucide-react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { Link } from 'react-router-dom'
import type { ColorTheme } from '../../../shared/types'
import { MOOD_THEMES } from '../lib/theme-options'

export function MoodThemePicker(): React.JSX.Element {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)
  const reduceMotion = useReducedMotion()
  const queryClient = useQueryClient()
  const settingsQuery = useQuery({ queryKey: ['settings'], queryFn: () => window.heima.getSettings() })
  const mutation = useMutation({
    mutationFn: (colorTheme: ColorTheme) => window.heima.setColorTheme(colorTheme),
    onMutate: (colorTheme) => {
      const previous = document.documentElement.dataset.colorTheme as ColorTheme | undefined
      document.documentElement.dataset.colorTheme = colorTheme
      return { previous }
    },
    onSuccess: (settings) => {
      queryClient.setQueryData(['settings'], settings)
      setOpen(false)
    },
    onError: (_error, _theme, context) => {
      if (context?.previous) document.documentElement.dataset.colorTheme = context.previous
    }
  })

  useEffect(() => {
    if (!open) return
    const close = (event: MouseEvent): void => {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false)
    }
    const escape = (event: KeyboardEvent): void => {
      if (event.key === 'Escape') setOpen(false)
    }
    window.addEventListener('mousedown', close)
    window.addEventListener('keydown', escape)
    return () => {
      window.removeEventListener('mousedown', close)
      window.removeEventListener('keydown', escape)
    }
  }, [open])

  const quickThemes = MOOD_THEMES.filter((theme) => theme.quick)
  return <div className="mood-picker" ref={rootRef}>
    <button className="mood-trigger" aria-label="切换心情主题" aria-expanded={open} onClick={() => setOpen((value) => !value)}>
      <Palette size={18} /><span>换心情</span>
    </button>
    <AnimatePresence>
      {open && <motion.div className="mood-popover" role="dialog" aria-label="心情主题" initial={reduceMotion ? false : { opacity: 0, y: -8, scale: 0.96 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: -5, scale: 0.98 }} transition={{ duration: reduceMotion ? 0 : 0.2, ease: [0.22, 1, 0.36, 1] }}>
        <div className="mood-popover-heading"><div><strong>今天想要什么心情？</strong><span>界面立即变化，账目不会受影响</span></div><Palette size={18} /></div>
        <div className="mood-quick-grid">
          {quickThemes.map((theme) => {
            const selected = settingsQuery.data?.colorTheme === theme.value
            return <button key={theme.value} className={selected ? 'selected' : ''} onClick={() => mutation.mutate(theme.value)} disabled={mutation.isPending}>
              <span className="mood-orb" data-mood={theme.value}>{theme.colors.map((color) => <i key={color} style={{ background: color }} />)}</span>
              <span><strong>{theme.label}</strong><small>{theme.description}</small></span>
              {selected && <Check size={16} />}
            </button>
          })}
        </div>
        <Link to="/settings" onClick={() => setOpen(false)}><Settings2 size={15} />查看全部外观设置</Link>
      </motion.div>}
    </AnimatePresence>
  </div>
}
