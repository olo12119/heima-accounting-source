// @vitest-environment jsdom
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ConfirmDialog } from '../../src/renderer/src/components/ConfirmDialog'

describe('二次确认对话框', () => {
  it('取消时不执行危险操作，确认时才执行', async () => {
    const onCancel = vi.fn()
    const onConfirm = vi.fn()
    const user = userEvent.setup()
    render(<ConfirmDialog open title="删除这笔账目？" description="删除后无法撤销" confirmLabel="确认删除" danger onCancel={onCancel} onConfirm={onConfirm} />)
    await user.click(screen.getByRole('button', { name: '取消' }))
    expect(onCancel).toHaveBeenCalledOnce()
    expect(onConfirm).not.toHaveBeenCalled()
    await user.click(screen.getByRole('button', { name: '确认删除' }))
    expect(onConfirm).toHaveBeenCalledOnce()
  })
})
