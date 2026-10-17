import { Capacitor } from '@capacitor/core'

/**
 * 登录态存储统一入口（M-P1 红线 4：数据与程序分离）。
 * - Web / 桌面：沿用 localStorage（shiguang_token / shiguang_user），同步读写路径不变；
 * - Capacitor 原生（Android/iOS）：**Preferences 是持久真源**，App 冷启动时 hydrate()
 *   把 Preferences 水合进 localStorage 镜像，之后读写照旧走同步 localStorage，
 *   每次写入镜像时同步写回 Preferences（write-through）。
 *   —— 这样全工程业务代码无需改成异步，同时杀掉 App 重开登录态仍在。
 */
const NATIVE = Capacitor.isNativePlatform()
const TK = 'shiguang_token'
const UK = 'shiguang_user'

async function prefsSet(key, value) {
  const { Preferences } = await import('@capacitor/preferences')
  await Preferences.set({ key, value })
}

async function prefsGet(key) {
  const { Preferences } = await import('@capacitor/preferences')
  const { value } = await Preferences.get({ key })
  return value
}

/** 原生端冷启动水合：Preferences → localStorage 镜像。Web/桌面为 no-op。 */
export async function hydrateTokenStorage() {
  if (!NATIVE) return
  try {
    const token = await prefsGet(TK)
    const user = await prefsGet(UK)
    if (token) localStorage.setItem(TK, token)
    if (user) localStorage.setItem(UK, user)
  } catch (e) {
    console.warn('[token-storage] 水合失败，按未登录继续', e)
  }
}

/** 登录/资料更新后调用：镜像已由调用方写好 localStorage，这里同步落 Preferences（fire-and-forget）。 */
export function persistTokenStorage() {
  if (!NATIVE) return
  const token = localStorage.getItem(TK)
  const user = localStorage.getItem(UK)
  prefsSet(TK, token ?? '').catch(() => {})
  prefsSet(UK, user ?? '').catch(() => {})
}

/** 退出登录：清 Preferences 真源（镜像由调用方清）。 */
export function clearTokenStorage() {
  if (!NATIVE) return
  prefsSet(TK, '').catch(() => {})
  prefsSet(UK, '').catch(() => {})
}
