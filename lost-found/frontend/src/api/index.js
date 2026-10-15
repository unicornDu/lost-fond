import axios from 'axios'

/**
 * 接口层（真实后端）。统一响应 {code,msg,data}；拦截器剥壳返回 data。
 * 审查 2.14：401 时清登录态并整页跳回 /auth（整页刷新重建 Pinia，规避 store 残留旧 token）。
 */
export const USE_MOCK = false

export const http = axios.create({
  baseURL: '/api',
  timeout: 20000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('shiguang_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

function clearSession() {
  localStorage.removeItem('shiguang_token')
  localStorage.removeItem('shiguang_user')
  // 审查 2.14：跳转登录页并记录来源（已在登录页则不跳，避免死循环）
  if (!location.pathname.startsWith('/auth')) {
    location.href = '/auth?redirect=' + encodeURIComponent(location.pathname + location.search)
  }
}

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) return body.data
      if (body.code === 401) clearSession()
      return Promise.reject(new Error(body.msg || '请求失败'))
    }
    return body
  },
  (err) => {
    const body = err.response?.data
    if (body?.code === 401 || err.response?.status === 401) clearSession()
    return Promise.reject(new Error(body?.msg || err.message || '网络错误'))
  }
)

export const api = {
  // 认证
  register: (data) => http.post('/auth/register', data),
  login: (data) => http.post('/auth/login', data),
  me: () => http.get('/auth/me'),
  // 启事
  stats: () => http.get('/stats'),
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
  // 管理后台（审查 2.8/2.9：契约补全）
  adminUsers: (params) => http.get('/admin/users', { params }),
  adminSetUserStatus: (id, ban) => http.post(`/admin/users/${id}/status`, null, { params: { ban } }),
  adminItems: (params) => http.get('/admin/items', { params }),
  adminAudit: (id, approve) => http.post(`/admin/items/${id}/audit`, null, { params: { approve } }),
  adminOffline: (id) => http.post(`/admin/items/${id}/offline`),
  adminDelete: (id) => http.delete(`/admin/items/${id}`),
  adminStats: () => http.get('/admin/stats'),
  adminLogs: (params) => http.get('/admin/logs', { params }),
  adminRegenThumbs: () => http.post('/admin/regen-thumbs'),
  adminReindex: () => http.post('/search/reindex'),
}

export default api
