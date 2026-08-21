export const getErrorMessage = (error: unknown): string => {
  if (!(error instanceof Error)) return '操作失败，请稍后再试'
  return error.message
    .replace(/^Error invoking remote method '[^']+': Error: /, '')
    .replace(/^Error: /, '')
}
