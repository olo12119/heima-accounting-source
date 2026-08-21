// @vitest-environment jsdom
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { SettingsPage } from '../../src/renderer/src/pages/SettingsPage'
import type { HeimaApi } from '../../src/shared/types'

describe('多配色主题设置', () => {
  it('颜色主题和明暗方式分别保存', async () => {
    const api = {
      getSettings: vi.fn().mockResolvedValue({ theme: 'system', colorTheme: 'forest' }),
      getStatus: vi.fn().mockResolvedValue({ ready: true, databasePath: 'test.sqlite3', version: '1.5.0' }),
      getLockStatus: vi.fn().mockResolvedValue({ enabled: false, locked: false }),
      setColorTheme: vi.fn().mockResolvedValue({ theme: 'system', colorTheme: 'ocean' }),
      setTheme: vi.fn().mockResolvedValue({ theme: 'dark', colorTheme: 'ocean' })
    } as unknown as HeimaApi
    Object.defineProperty(window, 'heima', { configurable: true, value: api })
    const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
    render(<QueryClientProvider client={client}><SettingsPage /></QueryClientProvider>)
    const user = userEvent.setup()

    expect(await screen.findByText('外观与数据，由你自己掌控')).toBeInTheDocument()
    expect(screen.queryByText('简单、清楚地知道自己的钱花到哪里去了。')).not.toBeInTheDocument()
    await user.click(await screen.findByRole('button', { name: /深海专注/ }))
    await waitFor(() => expect(api.setColorTheme).toHaveBeenCalledWith('ocean'))
    await user.click(screen.getByRole('button', { name: /深色/ }))
    await waitFor(() => expect(api.setTheme).toHaveBeenCalledWith('dark'))
  })
})
