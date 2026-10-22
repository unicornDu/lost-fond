import axios from 'axios'
import { apiRoot, isAppTarget } from '../utils/server'

/**
 * 接口层（真实后端）。统一响应 {code,msg,data}；拦截器剥壳返回 data。
 * 审查 2.14：401 时清登录态并整页跳回 /auth（整页刷新重建 Pinia，规避 store 残留旧 token）。
 * 修复 F-09：删除无人引用的 USE_MOCK 开关（mock 时代遗留）。
 */

export const http = axios.create({
  baseURL: '/api',
  timeout: 20000,
})

http.interceptors.request.use((config) => {
  // App 端服务器地址可在运行时修改，所以每次请求都重算 baseURL
  config.baseURL = apiRoot()
  const token = localStorage.getItem('shiguang_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

function clearSession() {
  localStorage.removeItem('shiguang_token')
  localStorage.removeItem('shiguang_user')
  // 桌面端由主进程关闭主窗口并唤回原生登录页
  if (window.shiguangDesktop?.isDesktop) {
    window.shiguangDesktop.logout()
    return
  }
  // 审查 2.14：跳转登录页并记录来源（已在登录页则不跳，避免死循环）
  const atAuth = isAppTarget()
    ? location.hash.startsWith('#/auth')
    : location.pathname.startsWith('/auth')
  if (atAuth) return
  if (isAppTarget()) {
    const from = location.hash.replace(/^#/, '') || '/home'
    location.hash = '#/auth?redirect=' + encodeURIComponent(from)
  } else {
    const from = location.pathname + location.search
    location.href = '/auth?redirect=' + encodeURIComponent(from)
  }
}

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) return body.data
      if (body.code === 401) clearSession()
      // E-05：错误对象附带业务 code，封禁 40301 等专用码靠它识别
      return Promise.reject(errWithCode(body.code, body.msg || '请求失败'))
    }
    return body
  },
  (err) => {
    const body = err.response?.data
    if (body?.code === 401 || err.response?.status === 401) clearSession()
    return Promise.reject(errWithCode(body?.code, body?.msg || err.message || '网络错误'))
  }
)

function errWithCode(code, msg) {
  const e = new Error(msg)
  if (code) e.code = code
  return e
}

export const api = {
  // 认证
  register: (data) => http.post('/auth/register', data),
  login: (data) => http.post('/auth/login', data),
  me: () => http.get('/auth/me'),
  // 账号设置（E-01）
  changePassword: (data) => http.put('/user/password', data),
  updateProfile: (data) => http.put('/user/profile', data),
  // 启事
  stats: () => http.get('/stats'),
  statsDaily: (days = 182) => http.get('/stats/daily', { params: { days } }),   // v2.41.1 活动总览
  listItems: (params) => http.get('/items', { params }),
  itemDetail: (id) => http.get(`/items/${id}`),
  similar: (id, n = 3) => http.get(`/items/${id}/similar`, { params: { n } }),
  myPublishes: () => http.get('/items/my'),
  publishItem: (formData) => http.post('/items', formData),
  markFound: (id) => http.post(`/items/${id}/mark-found`),
  dm: (id, content) => http.post(`/items/${id}/dm`, { content }),
  // 以图搜图
  searchByImage: (formData) => http.post('/search/image', formData),
  indexedCount: () => http.get('/search/indexed-count'),
  // 守望订阅（v2.20）
  watchPlans: () => http.get('/watch/plans'),
  watchOverview: () => http.get('/watch/me'),
  watchSubscribe: (data) => http.post('/watch/subscribe', data),
  watchAddTarget: (formData) => http.post('/watch/targets', formData),
  watchRemoveTarget: (id) => http.delete(`/watch/targets/${id}`),
  // 认领
  applyClaim: (id, data) => http.post(`/items/${id}/claim`, data),
  myClaims: () => http.get('/claims/my'),
  receivedClaims: () => http.get('/claims/received'),
  auditClaim: (claimId, data) => http.post(`/claims/${claimId}/audit`, data),
  // 消息（审查 2.7：补 readAll / 单条 read）
  messages: () => http.get('/user/messages'),
  unread: () => http.get('/user/messages/unread'),
  readAll: () => http.post('/user/messages/read-all'),
  readOne: (id) => http.post(`/user/messages/${id}/read`),
  // 私信回复（E-03）
  replyMessage: (id, content) => http.post(`/user/messages/${id}/reply`, { content }),
  // 他人公开主页（E-04）
  userProfile: (id) => http.get(`/users/${id}/profile`),
  userItems: (id, params) => http.get(`/users/${id}/items`, { params }),
  // 收藏（E-07）
  favorite: (id) => http.post(`/items/${id}/favorite`),
  unfavorite: (id) => http.delete(`/items/${id}/favorite`),
  favorites: (params) => http.get('/user/favorites', { params }),
  // 交付文件（P1）：受控下载 blob，渲染层只经手 deliveryId
  deliveries: () => http.get('/file/list'),
  downloadDelivery: (id) => http.get('/file/download', { params: { id }, responseType: 'blob' }),
  deleteDelivery: (id) => http.delete('/file', { params: { id } }),
  // 管理后台（审查 2.8/2.9：契约补全）
  adminUsers: (params) => http.get('/admin/users', { params }),
  adminSetUserStatus: (id, ban) => http.post(`/admin/users/${id}/status`, null, { params: { ban } }),
  adminItems: (params) => http.get('/admin/items', { params }),
  adminAudit: (id, approve, reason) => http.post(`/admin/items/${id}/audit`, null, { params: { approve, reason: reason || undefined } }),
  adminOffline: (id, reason) => http.post(`/admin/items/${id}/offline`, null, { params: { reason: reason || undefined } }),
  adminDelete: (id) => http.delete(`/admin/items/${id}`),
  adminStats: () => http.get('/admin/stats'),
  adminLogs: (params) => http.get('/admin/logs', { params }),
  adminRegenThumbs: () => http.post('/admin/regen-thumbs'),
  adminReindex: () => http.post('/search/reindex'),
  // P1：启事数据报表（入 delivery 体系，替代原 /admin/export/items.csv）
  createReport: () => http.post('/admin/report'),
  // P2：以图搜图匹配报告 PDF
  matchReport: (formData) => http.post('/search/match-report', formData),
}

export default api
