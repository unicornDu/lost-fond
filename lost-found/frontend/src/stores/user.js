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
        this.logout()
      }
    },
    logout() {
      this.token = ''
      this.info = null
      localStorage.removeItem(TK)
      localStorage.removeItem(UK)
    },
  },
})
