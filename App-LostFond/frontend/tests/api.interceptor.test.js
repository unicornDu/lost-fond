// @vitest-environment jsdom
import { describe, it, expect, beforeEach, beforeAll } from 'vitest'
import { http, api } from '../src/api/index.js'

/**
 * Z-05 冒烟③：axios 拦截器（Bearer 注入 + 剥壳 + 401 清登录态并跳转）。
 * 用假 adapter 模拟后端响应，不发起真实网络请求。
 */
describe('axios 拦截器', () => {
  const originalAdapter = http.defaults.adapter
  let captured

  beforeAll(() => {
    // 清登录态会 location.href 整页跳转——替换 location 以捕获目标（jest/jsdom 常规手法）
    // eslint-disable-next-line no-restricted-globals
    delete window.location
    window.location = { pathname: '/home', search: '', href: '' }
  })

  beforeEach(() => {
    localStorage.clear()
    captured = null
  })

  it('请求拦截器：带 token 时注入 Authorization: Bearer', async () => {
    localStorage.setItem('shiguang_token', 'tk-42')
    http.defaults.adapter = async (config) => {
      captured = config
      return { status: 200, data: { code: 200, msg: 'success', data: { ok: 1 } }, headers: {}, config }
    }
    const data = await api.stats()
    expect(data).toEqual({ ok: 1 })   // 响应拦截器剥壳
    expect(captured.headers.Authorization).toBe('Bearer tk-42')
  })

  it('响应 401：清空登录态并跳转 /auth?redirect=…', async () => {
    localStorage.setItem('shiguang_token', 'tk-expired')
    localStorage.setItem('shiguang_user', JSON.stringify({ id: 9 }))
    http.defaults.adapter = async () => {
      const err = new Error('Unauthorized')
      err.response = { status: 401, data: { code: 401, msg: '请先登录' }, headers: {} }
      throw err
    }
    await expect(api.me()).rejects.toThrow('请先登录')
    expect(localStorage.getItem('shiguang_token')).toBeNull()
    expect(localStorage.getItem('shiguang_user')).toBeNull()
    expect(window.location.href).toContain('/auth?redirect=%2Fhome')
    http.defaults.adapter = originalAdapter
  })

  it('业务 code≠200：拒绝并携带 msg（不清登录态）', async () => {
    localStorage.setItem('shiguang_token', 'tk-ok')
    http.defaults.adapter = async () => ({
      status: 400,
      data: { code: 400, msg: '该启事当前状态不允许标记完成', data: null },
      headers: {},
    })
    await expect(api.markFound(1)).rejects.toThrow('该启事当前状态不允许标记完成')
    expect(localStorage.getItem('shiguang_token')).toBe('tk-ok')
    http.defaults.adapter = originalAdapter
  })
})
