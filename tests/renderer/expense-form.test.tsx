// @vitest-environment jsdom
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { CATEGORIES } from '../../src/shared/categories'
import { ExpenseFormDialog } from '../../src/renderer/src/components/ExpenseFormDialog'
import type { Expense, HeimaApi } from '../../src/shared/types'

const renderDialog = (createExpense = vi.fn()): void => {
  const api = {
    getCategories: vi.fn().mockResolvedValue(CATEGORIES),
    getFrequentCategories: vi.fn().mockResolvedValue([]),
    createExpense: createExpense.mockImplementation(async (input) => ({
      ...input,
      id: '2ef99495-7651-493c-a0d6-aaa0cc968523',
      primaryCategoryName: '餐饮', secondaryCategoryName: '正餐',
      createdAt: new Date().toISOString(), updatedAt: new Date().toISOString()
    } as Expense))
  } as unknown as HeimaApi
  Object.defineProperty(window, 'heima', { configurable: true, value: api })
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  render(<QueryClientProvider client={client}><ExpenseFormDialog expense={null} onClose={vi.fn()} /></QueryClientProvider>)
}

describe('快速记账表单', () => {
  it('拒绝超过两位小数的金额', async () => {
    const createExpense = vi.fn()
    renderDialog(createExpense)
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('金额'), '12.345')
    await user.click(screen.getByRole('button', { name: '保存支出' }))
    expect(await screen.findByText('请输入正确金额，最多保留两位小数')).toBeInTheDocument()
    expect(createExpense).not.toHaveBeenCalled()
  })

  it('将 12.50 元保存为 1250 分并带上两级分类', async () => {
    const createExpense = vi.fn()
    renderDialog(createExpense)
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('金额'), '12.50')
    await user.click(screen.getByRole('button', { name: '保存支出' }))
    await waitFor(() => expect(createExpense).toHaveBeenCalled())
    expect(createExpense.mock.calls[0]![0]).toMatchObject({
      entryType: 'expense',
      amountCents: 1250,
      primaryCategoryId: 'food',
      secondaryCategoryId: 'food.meal'
    })
  })

  it('切换为收入后使用收入分类保存', async () => {
    const createExpense = vi.fn()
    renderDialog(createExpense)
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: '收入' }))
    await user.type(screen.getByLabelText('金额'), '5000')
    await user.click(screen.getByRole('button', { name: '保存收入' }))
    await waitFor(() => expect(createExpense).toHaveBeenCalled())
    expect(createExpense.mock.calls[0]![0]).toMatchObject({ entryType: 'income', primaryCategoryId: 'salary', secondaryCategoryId: 'salary.base' })
  })
})
