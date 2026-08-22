// @vitest-environment jsdom
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { CategoriesPage } from '../../src/renderer/src/pages/CategoriesPage'
import type { CategoryManagementItem, HeimaApi } from '../../src/shared/types'

const systemCategories: CategoryManagementItem[] = [
  { id: 'food', parentId: null, name: '餐饮', icon: 'utensils', color: '#d98257', sortOrder: 0, entryType: 'expense', isSystem: true, isActive: true, usageCount: 0 },
  { id: 'food.meal', parentId: 'food', name: '正餐', icon: 'utensils', color: '#d98257', sortOrder: 0, entryType: 'expense', isSystem: true, isActive: true, usageCount: 0 }
]

const renderPage = (categories: CategoryManagementItem[] = systemCategories): HeimaApi => {
  const api = {
    getCategoriesForManagement: vi.fn().mockResolvedValue(categories),
    createCustomPrimaryCategory: vi.fn().mockResolvedValue({}),
    createCustomSecondaryCategory: vi.fn().mockResolvedValue({}),
    updateCustomCategory: vi.fn().mockResolvedValue({}),
    setCustomCategoryActive: vi.fn().mockResolvedValue([]),
    deleteCustomCategory: vi.fn().mockResolvedValue({ mode: 'deleted' }),
    reorderCustomCategory: vi.fn().mockResolvedValue([])
  } as unknown as HeimaApi
  Object.defineProperty(window, 'heima', { configurable: true, value: api })
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  render(<QueryClientProvider client={client}><CategoriesPage /></QueryClientProvider>)
  return api
}

describe('分类管理页面', () => {
  it('系统分类保持锁定，用户可以新增自己的一级分类', async () => {
    const api = renderPage()
    const user = userEvent.setup()
    expect(await screen.findByText('系统预设')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '编辑餐饮' })).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /新增一级分类/ }))
    await user.type(screen.getByLabelText('一级分类名称'), '家庭生活')
    await user.clear(screen.getByLabelText('第一个二级分类'))
    await user.type(screen.getByLabelText('第一个二级分类'), '家庭日用')
    await user.click(screen.getByRole('button', { name: '保存分类' }))
    await waitFor(() => expect(api.createCustomPrimaryCategory).toHaveBeenCalledWith(expect.objectContaining({
      entryType: 'expense', name: '家庭生活', firstSecondaryName: '家庭日用'
    })))
  })

  it('允许在系统一级分类下面添加自定义二级分类', async () => {
    const api = renderPage()
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: /添加二级分类/ }))
    await user.type(screen.getByLabelText('二级分类名称'), '公司食堂')
    await user.click(screen.getByRole('button', { name: '保存分类' }))
    await waitFor(() => expect(api.createCustomSecondaryCategory).toHaveBeenCalledWith({ parentId: 'food', name: '公司食堂' }))
  })

  it('已有账目的自定义分类使用停用确认而不是直接删除', async () => {
    const custom: CategoryManagementItem = {
      id: 'custom.52a4de74-39f8-4b65-8b44-65991a8f6b31', parentId: 'food', name: '公司食堂',
      icon: 'utensils', color: '#d98257', sortOrder: 20, entryType: 'expense', isSystem: false,
      isActive: true, usageCount: 3
    }
    const api = renderPage([...systemCategories, custom])
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: '删除公司食堂' }))
    expect(screen.getByText('停用这个分类？')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '确认停用' }))
    await waitFor(() => expect(api.deleteCustomCategory).toHaveBeenCalledWith(custom.id))
  })
})
