import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './tests/packaged',
  timeout: 45_000,
  expect: { timeout: 10_000 },
  workers: 1,
  reporter: [['list']]
})
