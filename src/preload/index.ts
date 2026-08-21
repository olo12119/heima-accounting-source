import { contextBridge, ipcRenderer } from 'electron'
import type { HeimaApi } from '../shared/types'

const api: HeimaApi = {
  getStatus: () => ipcRenderer.invoke('system:get-status'),
  getCategories: () => ipcRenderer.invoke('categories:list'),
  getFrequentCategories: (entryType) => ipcRenderer.invoke('categories:frequent', entryType),
  listExpenses: (preset, entryType = 'all') => ipcRenderer.invoke('expenses:list', preset, entryType),
  createExpense: (input) => ipcRenderer.invoke('expenses:create', input),
  updateExpense: (id, input) => ipcRenderer.invoke('expenses:update', id, input),
  deleteExpense: (id) => ipcRenderer.invoke('expenses:delete', id),
  getDashboard: () => ipcRenderer.invoke('dashboard:get'),
  getStatistics: (preset, entryType) => ipcRenderer.invoke('statistics:get', preset, entryType),
  getSettings: () => ipcRenderer.invoke('settings:get'),
  setTheme: (theme) => ipcRenderer.invoke('settings:set-theme', theme),
  exportCsv: () => ipcRenderer.invoke('data:export-csv'),
  exportBackup: () => ipcRenderer.invoke('data:export-backup'),
  restoreBackup: () => ipcRenderer.invoke('data:restore-backup')
}

contextBridge.exposeInMainWorld('heima', api)
