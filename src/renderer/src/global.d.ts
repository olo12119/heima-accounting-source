import type { HeimaApi } from '../../shared/types'

declare global {
  interface Window {
    heima: HeimaApi
  }
}

export {}
