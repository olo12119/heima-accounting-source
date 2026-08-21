export type RangePreset = 'today' | 'week' | 'month' | 'all'
export type ThemeMode = 'light' | 'dark' | 'system'
export type EntryType = 'expense' | 'income'
export type EntryTypeFilter = EntryType | 'all'

export interface Category {
  id: string
  parentId: string | null
  name: string
  icon: string
  sortOrder: number
  entryType: EntryType
}

export interface ExpenseInput {
  entryType: EntryType
  amountCents: number
  primaryCategoryId: string
  secondaryCategoryId: string
  spentDate: string
  spentTime: string
  note: string
}

export interface Expense extends ExpenseInput {
  id: string
  primaryCategoryName: string
  secondaryCategoryName: string
  createdAt: string
  updatedAt: string
}

export interface CategoryTotal {
  categoryId: string
  categoryName: string
  amountCents: number
  color: string
}

export interface DailyTotal {
  date: string
  amountCents: number
}

export interface DashboardSummary {
  todayExpenseCents: number
  todayIncomeCents: number
  monthExpenseCents: number
  monthIncomeCents: number
  monthBalanceCents: number
  recentEntries: Expense[]
  categoryTotals: CategoryTotal[]
}

export interface StatisticsSnapshot {
  preset: Exclude<RangePreset, 'all'>
  startDate: string
  endDate: string
  entryType: EntryType
  totalCents: number
  categoryTotals: CategoryTotal[]
  dailyTotals: DailyTotal[]
}

export interface FrequentCategory {
  primaryCategoryId: string
  secondaryCategoryId: string
  primaryCategoryName: string
  secondaryCategoryName: string
  count: number
}

export interface AppSettings {
  theme: ThemeMode
}

export interface AppStatus {
  ready: boolean
  databasePath: string
  error?: string
  version: string
}

export interface FileOperationResult {
  canceled: boolean
  path?: string
  count?: number
}

export interface RestoreResult extends FileOperationResult {
  safetyBackupPath?: string
}

export interface BackupExpense extends ExpenseInput {
  id: string
  createdAt: string
  updatedAt: string
}

export interface BackupPayload {
  expenses: BackupExpense[]
  settings: AppSettings
}

export interface BackupDocument {
  magic: 'heima-accounting-backup'
  schemaVersion: 1 | 2
  appVersion: string
  exportedAt: string
  checksum: string
  payload: BackupPayload
}

export interface HeimaApi {
  getStatus: () => Promise<AppStatus>
  getCategories: () => Promise<Category[]>
  getFrequentCategories: (entryType: EntryType) => Promise<FrequentCategory[]>
  listExpenses: (preset: RangePreset, entryType?: EntryTypeFilter) => Promise<Expense[]>
  createExpense: (input: ExpenseInput) => Promise<Expense>
  updateExpense: (id: string, input: ExpenseInput) => Promise<Expense>
  deleteExpense: (id: string) => Promise<void>
  getDashboard: () => Promise<DashboardSummary>
  getStatistics: (preset: Exclude<RangePreset, 'all'>, entryType: EntryType) => Promise<StatisticsSnapshot>
  getSettings: () => Promise<AppSettings>
  setTheme: (theme: ThemeMode) => Promise<AppSettings>
  exportCsv: () => Promise<FileOperationResult>
  exportBackup: () => Promise<FileOperationResult>
  restoreBackup: () => Promise<RestoreResult>
}
