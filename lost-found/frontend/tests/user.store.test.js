// @vitest-environment jsdom
import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useUserStore } from '../src/stores/user.js'

/**
 * Z-05 冒烟②：user store（登录态写入/清除、role 解析）。
 */
const TK = 'shiguang_token'
const UK = 'shiguang_user'

describe('user store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('初始为未登录', () => {
    const s = useUserStore()
    expect(s.logged).toBe(false)
    expect(s.isAdmin).toBe(false)
  })

  it('setSession 写入 token/用户并持久化 localStorage', () => {
    const s = useUserStore()
    s.setSession('t-123', { id: 9, role: 0, nickname: '张三' })
    expect(s.logged).toBe(true)
    expect(s.isAdmin).toBe(false)
    expect(localStorage.getItem(TK)).toBe('t-123')
    expect(JSON.parse(localStorage.getItem(UK)).nickname).toBe('张三')
  })

  it('role=1 时 isAdmin 为真', () => {
    const s = useUserStore()
    s.setSession('t-1', { id: 1, role: 1 })
    expect(s.isAdmin).toBe(true)
  })

  it('logout 清空内存与 localStorage', () => {
    const s = useUserStore()
    s.setSession('t-123', { id: 9, role: 0 })
    s.logout()
    expect(s.logged).toBe(false)
    expect(localStorage.getItem(TK)).toBeNull()
    expect(localStorage.getItem(UK)).toBeNull()
  })
})
