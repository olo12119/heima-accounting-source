import { animate, motion, useMotionValue, useReducedMotion, useTransform } from 'motion/react'
import { useEffect } from 'react'
import { formatCents } from '../../../shared/money'

export function AnimatedAmount({
  cents,
  prefix = '',
  className
}: {
  cents: number
  prefix?: string
  className?: string
}): React.JSX.Element {
  const reduceMotion = useReducedMotion()
  const amount = useMotionValue(reduceMotion ? cents : 0)
  const display = useTransform(amount, (latest) => `${prefix}${formatCents(Math.round(latest))}`)

  useEffect(() => {
    if (reduceMotion) {
      amount.set(cents)
      return
    }
    const controls = animate(amount, cents, { duration: 0.78, ease: [0.22, 1, 0.36, 1] })
    return () => controls.stop()
  }, [amount, cents, reduceMotion])

  return <motion.strong className={className}>{display}</motion.strong>
}
