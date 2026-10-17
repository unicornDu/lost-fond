import { defineStore } from 'pinia'
import { api } from '../api'

const TK = 'shiguang_token'
const UK = 'shiguang_user'

/**
 * 用户状态：登录态存 localStorage（token + 用户信息），与后端 JWT 对接。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TK) || '',
    info: JSON.parse(localStorage.getItem(UK) || 'null'),
  }),
  getters: {
    logged: (s) => !!s.token && !!s.info,
    isAdmin: (s) => s.info?.role === 1,
  },
  actions: {
    setSession(token, user) {
      this.token = token
      this.info = user
      localStorage.setItem(TK, token)
      localStorage.setItem(UK, JSON.stringify(user))
    },
    async login(form) {
      const data = await api.login(form)
      this.setSession(data.token, data.user)
      return data.user
    },
    async register(form) {
      const data = await api.register(form)
      this.setSession(data.token, data.user)
      return data.user
    },
    async refresh() {
      try {
        const u = await api.me()
        this.info = u
        localStorage.setItem(UK, JSON.stringify(u))
      } catch (e) {
        // E-05：封禁（40301）与普通过期分流——封禁要留下提示并回登录页展示，过期静默清态
        if (e?.code === 40301) {
          sessionStorage.setItem('shiguang_banned_msg', e.message || '账号已被封禁，如有疑问请联系管理员')
          this.logout()
          if (!location.pathname.startsWith('/auth'))
            location.href = '/auth'
          return
        }
        this.logout()
      }
    },
    // E-01：资料编辑后局部刷新 store 与导航栏（不动 token）
    updateInfo(partial) {
      if (!this.info) return
      this.info = { ...this.info, ...partial }
      localStorage.setItem(UK, JSON.stringify(this.info))
    },
    logout() {
      this.token = ''
      this.info = null
      localStorage.removeItem(TK)
      localStorage.removeItem(UK)
    },
  },
})
