import { test, expect, _electron as electron } from '@playwright/test'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'

test('Windows 解压版可以启动并打开本地账本', async () => {
  const root = resolve(process.cwd())
  const userData = mkdtempSync(join(tmpdir(), 'heima-packaged-smoke-'))
  const env = Object.fromEntries(
    Object.entries(process.env).filter(([key, value]) => key !== 'ELECTRON_RUN_AS_NODE' && value !== undefined)
  ) as Record<string, string>
  const executablePath = process.env.HEIMA_PACKAGED_EXE ?? join(root, 'release', 'win-unpacked', 'HeimaAccounting.exe')
  const app = await electron.launch({
    executablePath,
    args: ['--disable-gpu', '--no-sandbox'],
    env: { ...env, HEIMA_TEST_USER_DATA: userData, NODE_ENV: 'production' }
  })
  try {
    const page = await app.firstWindow()
    await expect(page).toHaveTitle('黑马记账')
    await expect(page.getByText('今天过得怎么样？')).toBeVisible()
    await expect(page.getByText('还没有账目')).toBeVisible()
  } finally {
    await app.close()
    rmSync(userData, { recursive: true, force: true })
  }
})
