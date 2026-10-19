import { createRouter, createWebHistory } from 'vue-router'
import { t } from '../i18n'

/**
 * 路由架构：
 *   /            简介落地页（公开）
 *   /auth        登录 / 注册（公开）
 *   /home /wall /publish /item/:id /search-image /profile  需登录
 *   /admin/*     需登录且 role=1（审查 2.9 角色守卫）
 *   /:pathMatch  404 兜底（审查 2.21）
 */
const routes = [
  {
    path: '/',
    name: 'welcome',
    component: () => import('../views/WelcomeView.vue'),
    meta: { title: '让每一件失物都能回家', public: true },
  },
  {
    path: '/auth',
    name: 'auth',
    component: () => import('../views/AuthView.vue'),
    meta: { title: '登录 / 注册', titleKey: 'title.auth', public: true },
  },
  { path: '/login', redirect: '/auth' },

  { path: '/home', name: 'home', component: () => import('../views/HomeView.vue'), meta: { title: '首页', titleKey: 'title.home' } },
  { path: '/wall', name: 'wall', component: () => import('../views/WallView.vue'), meta: { title: '失物墙', titleKey: 'title.wall' } },
  { path: '/publish', name: 'publish', component: () => import('../views/PublishView.vue'), meta: { title: '发布启事', titleKey: 'title.publish' } },
  { path: '/item/:id', name: 'detail', component: () => import('../views/DetailView.vue'), meta: { title: '启事详情', titleKey: 'title.detail' } },
  { path: '/search-image', name: 'searchImage', component: () => import('../views/SearchImageView.vue'), meta: { title: '以图搜图', titleKey: 'title.search' } },
  { path: '/profile', name: 'profile', component: () => import('../views/ProfileView.vue'), meta: { title: '个人中心', titleKey: 'title.profile' } },
  { path: '/user/:id', name: 'userProfile', component: () => import('../views/UserProfileView.vue'), meta: { title: '用户主页', titleKey: 'title.user' } },   // E-04

  // 管理后台（审查 2.9）：独立布局，角色守卫
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { title: '管理后台', titleKey: 'title.admin', roles: ['admin'] },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'adminDashboard', component: () => import('../views/admin/DashboardView.vue'), meta: { title: '数据看板', titleKey: 'title.admin', roles: ['admin'] } },
      { path: 'audit', name: 'adminAudit', component: () => import('../views/admin/AuditView.vue'), meta: { title: '启事审核', titleKey: 'title.admin', roles: ['admin'] } },
      { path: 'users', name: 'adminUsers', component: () => import('../views/admin/UsersView.vue'), meta: { title: '用户管理', titleKey: 'title.admin', roles: ['admin'] } },
      { path: 'logs', name: 'adminLogs', component: () => import('../views/admin/LogsView.vue'), meta: { title: '操作日志', titleKey: 'title.admin', roles: ['admin'] } },
      { path: 'tools', name: 'adminTools', component: () => import('../views/admin/ToolsView.vue'), meta: { title: '运维工具', titleKey: 'title.admin', roles: ['admin'] } },   // 修复 F-10
    ],
  },

  // 404 兜底（审查 2.21）
  {
    path: '/:pathMatch(.*)*',
    name: 'notFound',
    component: () => import('../views/NotFoundView.vue'),
    meta: { title: '页面不存在', titleKey: 'title.notFound', public: true },
  },
]

const router = createRouter({
  // Web 单端：history 模式（后端 FrontendController 已做 SPA 回退，刷新深链不 404）
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

const logged = () => !!localStorage.getItem('shiguang_user')
const isAdmin = () => {
  try { return JSON.parse(localStorage.getItem('shiguang_user') || 'null')?.role === 1 }
  catch { return false }
}

router.beforeEach((to) => {
  const needAdmin = to.matched.some((r) => r.meta?.roles?.includes('admin'))
  if (needAdmin) {
    if (!logged()) return { path: '/auth', query: { redirect: to.fullPath } }
    if (!isAdmin()) return { path: '/home' }   // 非管理员访问后台 → 回首页
    return true
  }
  if (!to.meta.public && !logged()) {
    return { path: '/auth', query: { redirect: to.fullPath } }
  }
  if (to.name === 'auth' && logged()) {
    return { path: '/home' }
  }
})

router.afterEach((to) => {
  const label = to.meta.titleKey ? t(to.meta.titleKey) : to.meta.title
  document.title = label ? `拾光 · ${label}` : '拾光 · 校园失物招领平台'
})

export default router
