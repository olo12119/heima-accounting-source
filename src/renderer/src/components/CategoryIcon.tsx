import { AirplaneTilt } from '@phosphor-icons/react/dist/csr/AirplaneTilt'
import { Baby } from '@phosphor-icons/react/dist/csr/Baby'
import { BookOpen } from '@phosphor-icons/react/dist/csr/BookOpen'
import { Briefcase } from '@phosphor-icons/react/dist/csr/Briefcase'
import { Bus } from '@phosphor-icons/react/dist/csr/Bus'
import { Car } from '@phosphor-icons/react/dist/csr/Car'
import { ChartLineUp } from '@phosphor-icons/react/dist/csr/ChartLineUp'
import { Coffee } from '@phosphor-icons/react/dist/csr/Coffee'
import { CurrencyCircleDollar } from '@phosphor-icons/react/dist/csr/CurrencyCircleDollar'
import { FilmSlate } from '@phosphor-icons/react/dist/csr/FilmSlate'
import { FirstAidKit } from '@phosphor-icons/react/dist/csr/FirstAidKit'
import { ForkKnife } from '@phosphor-icons/react/dist/csr/ForkKnife'
import { GameController } from '@phosphor-icons/react/dist/csr/GameController'
import { Gift } from '@phosphor-icons/react/dist/csr/Gift'
import { GraduationCap } from '@phosphor-icons/react/dist/csr/GraduationCap'
import { HandCoins } from '@phosphor-icons/react/dist/csr/HandCoins'
import { Heartbeat } from '@phosphor-icons/react/dist/csr/Heartbeat'
import { House } from '@phosphor-icons/react/dist/csr/House'
import { Medal } from '@phosphor-icons/react/dist/csr/Medal'
import { PawPrint } from '@phosphor-icons/react/dist/csr/PawPrint'
import { Receipt } from '@phosphor-icons/react/dist/csr/Receipt'
import { Shapes } from '@phosphor-icons/react/dist/csr/Shapes'
import { ShoppingBag } from '@phosphor-icons/react/dist/csr/ShoppingBag'
import { Storefront } from '@phosphor-icons/react/dist/csr/Storefront'
import { SuitcaseRolling } from '@phosphor-icons/react/dist/csr/SuitcaseRolling'
import { TShirt } from '@phosphor-icons/react/dist/csr/TShirt'
import { Wallet } from '@phosphor-icons/react/dist/csr/Wallet'
import { WifiHigh } from '@phosphor-icons/react/dist/csr/WifiHigh'
import type { Icon, IconProps } from '@phosphor-icons/react'

const icons: Record<string, Icon> = {
  utensils: ForkKnife,
  car: Car,
  'shopping-bag': ShoppingBag,
  house: House,
  clapperboard: FilmSlate,
  'heart-pulse': Heartbeat,
  'book-open': BookOpen,
  wifi: WifiHigh,
  gift: Gift,
  luggage: SuitcaseRolling,
  shapes: Shapes,
  'briefcase-business': Briefcase,
  'badge-dollar-sign': Medal,
  store: Storefront,
  'chart-no-axes-combined': ChartLineUp,
  'receipt-text': Receipt,
  'hand-coins': HandCoins,
  'circle-dollar-sign': CurrencyCircleDollar,
  coffee: Coffee,
  bus: Bus,
  't-shirt': TShirt,
  'paw-print': PawPrint,
  baby: Baby,
  airplane: AirplaneTilt,
  'game-controller': GameController,
  'graduation-cap': GraduationCap,
  'first-aid': FirstAidKit,
  wallet: Wallet
}

const spriteOrder = [
  'utensils', 'car', 'shopping-bag', 'house', 'clapperboard', 'heart-pulse', 'book-open',
  'wifi', 'gift', 'luggage', 'shapes', 'briefcase-business', 'badge-dollar-sign', 'store',
  'chart-no-axes-combined', 'receipt-text', 'hand-coins', 'circle-dollar-sign', 'coffee', 'bus', 't-shirt',
  'paw-print', 'baby', 'airplane', 'game-controller', 'graduation-cap', 'first-aid', 'wallet'
] as const

export function CategoryIcon({ name, size = 24, className, ...props }: { name: string } & Omit<IconProps, 'weight'>): React.JSX.Element {
  const IconComponent = icons[name] ?? Shapes
  const index = Math.max(0, spriteOrder.indexOf(name as typeof spriteOrder[number]))
  const numericSize = typeof size === 'number' ? size : Number.parseFloat(String(size)) || 24
  const column = index % 7
  const row = Math.floor(index / 7)
  return <span className={`category-3d-icon ${className ?? ''}`} style={{ width: numericSize, height: numericSize }} aria-hidden="true">
    <IconComponent className="category-vector-fallback" size={numericSize} weight="duotone" {...props} />
    <img src="./category-3d-atlas.png" alt="" draggable={false} onError={(event) => { event.currentTarget.hidden = true }} style={{ width: numericSize * 7, height: numericSize * 4, left: -column * numericSize, top: -row * numericSize }} />
  </span>
}
