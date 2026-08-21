import { join } from 'node:path'
import { app, BrowserWindow, nativeTheme, session } from 'electron'
import { AccountingDatabase } from './database'
import { registerIpcHandlers } from './ipc'
import type { AppStatus } from '../shared/types'

const userDataPath = process.env.HEIMA_TEST_USER_DATA
  ? process.env.HEIMA_TEST_USER_DATA
  : join(app.getPath('appData'), 'HeimaAccounting')
app.setPath('userData', userDataPath)

let mainWindow: BrowserWindow | null = null
let database: AccountingDatabase | null = null
let databaseError: string | undefined

const getStatus = (): AppStatus => ({
  ready: database !== null,
  databasePath: join(userDataPath, 'data', 'heima-accounting.sqlite3'),
  error: databaseError,
  version: app.getVersion()
})

const createWindow = (): void => {
  mainWindow = new BrowserWindow({
    width: 1180,
    height: 760,
    minWidth: 960,
    minHeight: 640,
    show: false,
    backgroundColor: nativeTheme.shouldUseDarkColors ? '#111816' : '#f3f5f2',
    title: '黑马记账',
    webPreferences: {
      preload: join(__dirname, '../preload/index.cjs'),
      sandbox: true,
      contextIsolation: true,
      nodeIntegration: false,
      webSecurity: true
    }
  })

  mainWindow.once('ready-to-show', () => mainWindow?.show())
  mainWindow.webContents.setWindowOpenHandler(() => ({ action: 'deny' }))
  mainWindow.webContents.on('will-navigate', (event, url) => {
    const allowed = process.env.NODE_ENV === 'development'
      ? /^https?:\/\/(?:localhost|127\.0\.0\.1)(?::\d+)?\//.test(url)
      : url.startsWith('file://')
    if (!allowed) event.preventDefault()
  })

  if (process.env.ELECTRON_RENDERER_URL) {
    void mainWindow.loadURL(process.env.ELECTRON_RENDERER_URL)
  } else {
    void mainWindow.loadFile(join(__dirname, '../renderer/index.html'))
  }
}

app.whenReady().then(() => {
  session.defaultSession.setPermissionRequestHandler((_webContents, _permission, callback) => callback(false))
  try {
    database = new AccountingDatabase(getStatus().databasePath)
  } catch (error) {
    databaseError = error instanceof Error ? error.message : '数据库初始化失败'
  }

  registerIpcHandlers({
    getDatabase: () => database,
    getWindow: () => mainWindow,
    getStatus,
    userDataPath
  })
  createWindow()

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})

app.on('before-quit', () => {
  database?.close()
  database = null
})
