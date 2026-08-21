import {
  BadgeDollarSign, BookOpen, BriefcaseBusiness, Car, ChartNoAxesCombined,
  CircleDollarSign, Clapperboard, Gift, HandCoins, HeartPulse, House, Luggage,
  ReceiptText, Shapes, ShoppingBag, Store, Utensils, Wifi, type LucideProps
} from 'lucide-react'

const icons: Record<string, React.ComponentType<LucideProps>> = {
  utensils: Utensils,
  car: Car,
  'shopping-bag': ShoppingBag,
  house: House,
  clapperboard: Clapperboard,
  'heart-pulse': HeartPulse,
  'book-open': BookOpen,
  wifi: Wifi,
  gift: Gift,
  luggage: Luggage,
  shapes: Shapes,
  'briefcase-business': BriefcaseBusiness,
  'badge-dollar-sign': BadgeDollarSign,
  store: Store,
  'chart-no-axes-combined': ChartNoAxesCombined,
  'receipt-text': ReceiptText,
  'hand-coins': HandCoins,
  'circle-dollar-sign': CircleDollarSign
}

export function CategoryIcon({ name, ...props }: { name: string } & LucideProps): React.JSX.Element {
  const Icon = icons[name] ?? Shapes
  return <Icon aria-hidden="true" {...props} />
}
