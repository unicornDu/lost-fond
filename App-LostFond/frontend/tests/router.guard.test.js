// @vitest-environment jsdom
import { describe, it, expect, beforeEach } from 'vitest'
import router from '../src/router/index.js'

/**
 * Z-05 冒烟①：路由守卫纯逻辑（不渲染组件，只验证导航落点）。
 * 守卫在每次导航时读 localStorage，因此用例间直接改 localStorage 即可切换登录态。
 */
const UK = 'shiguang_user'
const TK = 'shiguang_token'

function setLogin(user) {
  localStorage.setItem(TK, 'fake-token')
  localStorage.setItem(UK, JSON.stringify(user))
}
function clearLogin() {
  localStorage.removeItem(TK)
  localStorage.removeItem(UK)
}

describe('路由守卫', () => {
  beforeEach(() => {
    clearLogin()
    localStorage.clear()
  })

  it('未登录访问受限页 → 跳 /auth 且带 redirect', async () => {
    await router.push('/profile')
    expect(router.currentRoute.value.path).toBe('/auth')
    expect(router.currentRoute.value.query.redirect).toBe('/profile')
  })

  it('非管理员访问 /admin/* → 回 /home', async () => {
    setLogin({ id: 2, role: 0, nickname: '普通用户' })
    await router.push('/admin/users')
    expect(router.currentRoute.value.path).toBe('/home')
  })

  it('管理员访问 /admin/* → 放行', async () => {
    setLogin({ id: 1, role: 1, nickname: '管理员' })
    await router.push('/admin/logs')
    expect(router.currentRoute.value.path).toBe('/admin/logs')
  })

  it('已登录访问 /auth → 回 /home', async () => {
    setLogin({ id: 2, role: 0 })
    await router.push('/auth')
    expect(router.currentRoute.value.path).toBe('/home')
  })

  it('未匹配路径 → 404 兜底页', async () => {
    clearLogin()
    await router.push('/definitely-not-a-route')
    expect(router.currentRoute.value.name).toBe('notFound')
  })
})
