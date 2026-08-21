import { test, expect, _electron as electron, type ElectronApplication, type Page } from '@playwright/test'
import { mkdtempSync, readFileSync, readdirSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'

const root = resolve(process.cwd())
const executablePath = join(root, 'node_modules', 'electron', 'dist', 'electron.exe')
const cleanEnvironment = (): Record<string, string> => Object.fromEntries(
  Object.entries(process.env).filter(([key, value]) => key !== 'ELECTRON_RUN_AS_NODE' && value !== undefined)
) as Record<string, string>

const launchApp = async (userData: string): Promise<{ app: ElectronApplication; page: Page }> => {
  const exportDirectory = join(userData, 'test-exports')
  const app = await electron.launch({
    executablePath,
    args: ['.'],
    cwd: root,
    env: {
      ...cleanEnvironment(),
      HEIMA_TEST_USER_DATA: userData,
      HEIMA_TEST_EXPORT_DIR: exportDirectory,
      NODE_ENV: 'production'
    }
  })
  return { app, page: await app.firstWindow() }
}

test('新增、编辑、筛选、删除确认、主题和重启持久化', async () => {
  const userData = mkdtempSync(join(tmpdir(), 'heima-e2e-'))
  let running: ElectronApplication | undefined
  try {
    let launched = await launchApp(userData)
    running = launched.app
    let page = launched.page
    await expect(page).toHaveTitle('黑马记账')
    await expect(page.getByText('还没有账目')).toBeVisible()

    await page.getByRole('button', { name: '记一笔' }).first().click()
    await page.getByLabel('金额').fill('12.50')
    await page.getByPlaceholder('例如：午餐、超市采购…').fill('测试午餐')
    await page.getByRole('button', { name: '保存支出' }).click()
    await expect(page.getByText('测试午餐')).toBeVisible()
    await expect(page.locator('.recent-panel .expense-amount')).toHaveText('−¥12.50')
    await page.screenshot({ path: join(root, 'test-results', 'dashboard-populated.png'), fullPage: true })

    await page.getByRole('link', { name: '账单' }).click()
    await expect(page.getByText('测试午餐')).toBeVisible()
    await page.getByRole('button', { name: '编辑正餐' }).click()
    await page.getByLabel('金额').fill('20.88')
    await page.getByRole('button', { name: '保存支出' }).click()
    await expect(page.locator('.records-panel .expense-amount')).toHaveText('−¥20.88')

    await running.close()
    launched = await launchApp(userData)
    running = launched.app
    page = launched.page
    await page.getByRole('link', { name: '账单' }).click()
    await expect(page.locator('.records-panel .expense-amount')).toHaveText('−¥20.88')

    await page.getByRole('link', { name: '数据与设置' }).click()
    await page.getByRole('button', { name: '导出' }).click()
    await expect(page.getByText(/CSV 已导出/)).toBeVisible()
    await page.getByRole('button', { name: '备份' }).click()
    await expect(page.getByText(/完整备份已保存/)).toBeVisible()
    const exportDirectory = join(userData, 'test-exports')
    const exportedFiles = readdirSync(exportDirectory)
    const csvPath = join(exportDirectory, exportedFiles.find((name) => name.endsWith('.csv'))!)
    const backupPath = join(exportDirectory, exportedFiles.find((name) => name.endsWith('.heima-backup.json'))!)
    expect(readFileSync(csvPath, 'utf8')).toContain('20.88')

    await page.getByRole('link', { name: '账单' }).click()

    await page.getByRole('button', { name: '删除正餐' }).click()
    await page.getByRole('button', { name: '取消' }).click()
    await expect(page.locator('.records-panel .expense-amount')).toHaveText('−¥20.88')
    await page.getByRole('button', { name: '删除正餐' }).click()
    await page.getByRole('button', { name: '确认删除' }).click()
    await expect(page.getByText('这个时间段还没有账目')).toBeVisible()

    await page.getByRole('link', { name: '数据与设置' }).click()
    await running.evaluate((_electron, path) => { process.env.HEIMA_TEST_RESTORE_PATH = path }, backupPath)
    await page.getByRole('button', { name: '恢复' }).click()
    await page.getByRole('button', { name: '选择备份并恢复' }).click()
    await expect(page.getByText(/已恢复 1 笔账目/)).toBeVisible()
    expect(readdirSync(join(userData, 'backups')).some((name) => name.startsWith('恢复前自动备份'))).toBe(true)

    await page.getByRole('button', { name: /深色/ }).click()
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')

    await page.getByRole('link', { name: '首页' }).click()
    await page.getByRole('button', { name: '记一笔' }).first().click()
    await page.getByRole('button', { name: '收入' }).click()
    await page.getByLabel('金额').fill('5000')
    await page.getByPlaceholder('例如：八月工资、差旅报销…').fill('八月工资')
    await page.getByRole('button', { name: '保存收入' }).click()
    await expect(page.getByText('+¥5,000.00')).toBeVisible()
    await expect(page.getByText('本月结余')).toBeVisible()

    await page.getByRole('link', { name: '账单' }).click()
    await page.getByRole('button', { name: '仅收入' }).click()
    await expect(page.getByText('八月工资')).toBeVisible()
    await expect(page.getByText('测试午餐')).not.toBeVisible()
    await page.getByRole('link', { name: '统计' }).click()
    await page.getByRole('button', { name: '收入分析' }).click()
    await expect(page.getByText('这一阶段共收入')).toBeVisible()
    await expect(page.locator('.breakdown-list strong').filter({ hasText: '工资薪酬' })).toBeVisible()
  } finally {
    await running?.close().catch(() => undefined)
    rmSync(userData, { recursive: true, force: true })
  }
})
