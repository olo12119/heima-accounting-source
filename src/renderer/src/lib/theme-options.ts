import type { ColorTheme } from '../../../shared/types'

export type MoodThemeOption = {
  value: ColorTheme
  label: string
  shortLabel: string
  description: string
  colors: [string, string, string]
  quick: boolean
}

export const MOOD_THEMES: MoodThemeOption[] = [
  {
    value: 'forest',
    label: '黑马经典',
    shortLabel: '经典',
    description: '白瓷、墨绿与克制金光',
    colors: ['#17624c', '#d6ab69', '#f7f4ed'],
    quick: true
  },
  {
    value: 'amber',
    label: '暖阳活力',
    shortLabel: '暖阳',
    description: '奶油暖黄与醒目石墨色',
    colors: ['#f1ba3d', '#292720', '#fff8e8'],
    quick: true
  },
  {
    value: 'wisteria',
    label: '云朵治愈',
    shortLabel: '治愈',
    description: '雾粉、鼠尾草与柔和天蓝',
    colors: ['#8ba895', '#e9a6a7', '#eaf1f7'],
    quick: true
  },
  {
    value: 'ocean',
    label: '深海专注',
    shortLabel: '深海',
    description: '保留原有冷静蓝灰主题',
    colors: ['#316f8f', '#69a9b7', '#e8f1f4'],
    quick: false
  }
]
