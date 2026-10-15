<script setup>
import { RouterLink, RouterView } from 'vue-router'

/** 管理后台独立布局（审查 2.9）：顶部子导航 + 内容出口。路由守卫已保证仅 admin 可达。 */
const links = [
  { to: '/admin/dashboard', label: '📊 数据看板' },
  { to: '/admin/audit', label: '🛂 启事审核' },
  { to: '/admin/users', label: '👥 用户管理' },
  { to: '/admin/logs', label: '📜 操作日志' },
]
</script>

<template>
  <div class="admin-wrap container">
    <div class="admin-head paper-card">
      <h1>🛡️ 管理后台</h1>
      <nav class="admin-nav">
        <RouterLink v-for="l in links" :key="l.to" :to="l.to" class="a-link">{{ l.label }}</RouterLink>
      </nav>
      <RouterLink to="/home" class="back">← 返回前台</RouterLink>
    </div>
    <RouterView />
  </div>
</template>

<style scoped>
.admin-wrap { padding: 24px 0 60px; }
.admin-head {
  display: flex; align-items: center; gap: 22px;
  padding: 16px 22px; margin-bottom: 18px; flex-wrap: wrap;
}
.admin-head h1 { font-size: 20px; }
.admin-nav { display: flex; gap: 6px; flex-wrap: wrap; }
.a-link {
  padding: 8px 16px; border-radius: 999px;
  color: var(--ink-soft); font-size: 14px; font-weight: 600;
  border: 1.5px solid transparent; transition: all .18s;
}
.a-link:hover { background: var(--cream-deep); }
.a-link.router-link-active {
  background: linear-gradient(120deg, var(--orange), var(--orange-deep));
  color: #FFF7EC; box-shadow: 0 6px 16px rgba(255,125,46,.3);
}
.back { margin-left: auto; color: var(--orange-deep); font-size: 13.5px; font-weight: 600; }
.back:hover { text-decoration: underline; }
</style>
