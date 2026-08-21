import { useState } from 'react'
import { LockKeyhole, ShieldCheck } from 'lucide-react'
import { getErrorMessage } from '../lib/errors'

export function LockScreen({ onUnlocked }: { onUnlocked: () => Promise<void> }): React.JSX.Element {
  const [pin, setPin] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const unlock = async (event: React.FormEvent): Promise<void> => {
    event.preventDefault()
    setBusy(true); setError('')
    try { await window.heima.unlock(pin); await onUnlocked() } catch (reason) { setError(getErrorMessage(reason)) } finally { setBusy(false) }
  }
  return <main className="lock-screen">
    <div className="lock-ambient" />
    <form className="lock-card" onSubmit={unlock}>
      <img src="./logo.svg" alt="" />
      <span className="lock-badge"><ShieldCheck size={14} />本地隐私保护</span>
      <h1>欢迎回来</h1><p>输入隐私密码，打开你的本地账本。</p>
      <label><span><LockKeyhole size={15} />隐私密码</span><input autoFocus type="password" inputMode="numeric" pattern="[0-9]*" minLength={4} maxLength={12} value={pin} onChange={(event) => setPin(event.target.value.replace(/\D/g, ''))} placeholder="4 至 12 位数字" aria-label="隐私密码" /></label>
      {error && <div className="form-error" role="alert">{error}</div>}
      <button className="button primary" disabled={busy || pin.length < 4}>{busy ? '正在验证…' : '打开账本'}</button>
      <small>密码只在本机校验，不会上传网络。</small>
    </form>
  </main>
}
