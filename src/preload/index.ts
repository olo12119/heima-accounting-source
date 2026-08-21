import { contextBridge, ipcRenderer } from 'electron'
import type { HeimaApi } from '../shared/types'

const api: HeimaApi = {
  getStatus: () => ipcRenderer.invoke('system:get-status'),
  getCategories: () => ipcRenderer.invoke('categories:list'),
  getCategoriesForManagement: () => ipcRenderer.invoke('categories:manage-list'),
  getFrequentCategories: (entryType) => ipcRenderer.invoke('categories:frequent', entryType),
  createCustomPrimaryCategory: (input) => ipcRenderer.invoke('categories:create-primary', input),
  createCustomSecondaryCategory: (input) => ipcRenderer.invoke('categories:create-secondary', input),
  updateCustomCategory: (id, input) => ipcRenderer.invoke('categories:update', id, input),
  setCustomCategoryActive: (id, active) => ipcRenderer.invoke('categories:set-active', id, active),
  deleteCustomCategory: (id) => ipcRenderer.invoke('categories:delete', id),
  reorderCustomCategory: (id, direction) => ipcRenderer.invoke('categories:reorder', id, direction),
  listExpenses: (preset, entryType = 'all') => ipcRenderer.invoke('expenses:list', preset, entryType),
  createExpense: (input) => ipcRenderer.invoke('expenses:create', input),
  updateExpense: (id, input) => ipcRenderer.invoke('expenses:update', id, input),
  deleteExpense: (id) => ipcRenderer.invoke('expenses:delete', id),
  getDashboard: () => ipcRenderer.invoke('dashboard:get'),
  getStatistics: (preset, entryType) => ipcRenderer.invoke('statistics:get', preset, entryType),
  getSettings: () => ipcRenderer.invoke('settings:get'),
  setTheme: (theme) => ipcRenderer.invoke('settings:set-theme', theme),
  setColorTheme: (colorTheme) => ipcRenderer.invoke('settings:set-color-theme', colorTheme),
  exportCsv: () => ipcRenderer.invoke('data:export-csv'),
  exportBackup: () => ipcRenderer.invoke('data:export-backup'),
  restoreBackup: () => ipcRenderer.invoke('data:restore-backup')
}

contextBridge.exposeInMainWorld('heima', api)
