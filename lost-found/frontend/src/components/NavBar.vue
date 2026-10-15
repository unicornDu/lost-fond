<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { api } from '../api'

/**
 * 应用导航。审查 2.7 消息铃铛（未读红点）；2.9 管理员"后台管理"入口；2.22 窄屏汉堡+抽屉。
 */
const router = useRouter()
const route = useRoute()
const user = useUserStore()

const unread = ref(0)
const drawer = ref(false)
let timer = null
let lastPoll = 0

async function pollUnread() {
  lastPoll = Date.now()
  if (!user.token) { unread.value = 0; return }
  try {
    const r = await api.unread()
    unread.value = r.count || 0
  } catch (e) { /* 静默：轮询失败不打扰 */ }
}

function goMessages() {
  drawer.value = false
  router.push({ path: '/profile', query: { tab: 'msg' } })
}

onMounted(() => {
  pollUnread()
  timer = setInterval(pollUnread, 30000) // 30s 轮询未读
  // 修复 F-02：消息页标已读后主动通知铃铛刷新（不用等 30s 轮询）
  window.addEventListener('shiguang-unread-changed', pollUnread)
})
onBeforeUnmount(() => {
  clearInterval(timer)
  window.removeEventListener('shiguang-unread-changed', pollUnread)
})
// 修复 F-11：切页刷新未读加 5 秒节流，避免频繁路由切换时请求过多
watch(() => route.fullPath, () => {
  if (Date.now() - lastPoll < 5000) return
  pollUnread()
})
</script>

<template>
  <header class="nav">
    <div class="container nav-inner">
      <RouterLink to="/home" class="brand">
        <span class="brand-logo">拾</span>
        <span class="brand-name">拾光<small>校园失物招领平台</small></span>
      </RouterLink>

      <nav class="nav-links">
        <RouterLink to="/home" exact-active-class="on">首页</RouterLink>
        <RouterLink :to="{ path: '/wall', query: { type: 'lost' } }" active-class="on">失物墙</RouterLink>
        <RouterLink :to="{ path: '/wall', query: { type: 'found' } }" active-class="on">招领墙</RouterLink>
        <RouterLink to="/search-image" active-class="on" class="link-hot">📷 以图搜图</RouterLink>
        <RouterLink v-if="user.isAdmin" to="/admin" active-class="on" class="link-admin">🛡️ 后台管理</RouterLink>
      </nav>

      <div class="nav-actions">
        <!-- 消息铃铛（审查 2.7） -->
        <el-badge v-if="user.info" :value="unread" :hidden="unread === 0" class="bell">
          <button class="bell-btn" title="消息通知" @click="goMessages">🔔</button>
        </el-badge>
        <el-button type="primary" round class="pub-btn" @click="router.push('/publish')">✏️ 发布启事</el-button>
        <RouterLink v-if="user.info" to="/profile" class="avatar" :title="user.info.nickname || user.info.account">
          {{ user.info.avatar || '🦊' }}
        </RouterLink>
        <el-button v-else round plain @click="router.push('/auth')">登录</el-button>
        <!-- 窄屏汉堡（审查 2.22） -->
        <button class="burger" title="菜单" @click="drawer = true">☰</button>
      </div>
    </div>

    <!-- 移动端抽屉导航 -->
    <el-drawer v-model="drawer" direction="rtl" size="70%" title="拾光 · 菜单">
      <nav class="drawer-nav">
        <RouterLink to="/home" @click="drawer = false">🏠 首页</RouterLink>
        <RouterLink :to="{ path: '/wall', query: { type: 'lost' } }" @click="drawer = false">🙋 失物墙</RouterLink>
        <RouterLink :to="{ path: '/wall', query: { type: 'found' } }" @click="drawer = false">🎁 招领墙</RouterLink>
        <RouterLink to="/search-image" @click="drawer = false">📷 以图搜图</RouterLink>
        <RouterLink to="/publish" @click="drawer = false">✏️ 发布启事</RouterLink>
        <RouterLink v-if="user.info" to="/profile" @click="drawer = false">👤 个人中心</RouterLink>
        <RouterLink v-if="user.info" to="/profile?tab=msg" @click="drawer = false">🔔 消息通知<em v-if="unread"> ({{ unread }})</em></RouterLink>
        <RouterLink v-if="user.isAdmin" to="/admin" @click="drawer = false">🛡️ 后台管理</RouterLink>
        <RouterLink v-if="!user.info" to="/auth" @click="drawer = false">🔑 登录 / 注册</RouterLink>
      </nav>
    </el-drawer>
  </header>
</template>

<style scoped>
.nav {
  position: sticky; top: 0; z-index: 100;
  background: color-mix(in srgb, var(--paper) 82%, transparent);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--line);
}
.nav-inner { display: flex; align-items: center; gap: 28px; height: 68px; }
.brand { display: flex; align-items: center; gap: 10px; }
.brand-logo {
  display: grid; place-items: center; width: 40px; height: 40px; border-radius: 12px;
  background: linear-gradient(135deg, var(--orange), var(--orange-deep));
  color: #FFF7EC; font-family: var(--font-display); font-size: 20px;
  transform: rotate(-4deg); box-shadow: 0 5px 12px rgba(255,125,46,.35);
}
.brand-name { display: flex; flex-direction: column; font-weight: 800; font-size: 17px; line-height: 1.1; }
.brand-name small { font-weight: 500; font-size: 10.5px; color: var(--ink-faint); letter-spacing: 1px; }

.nav-links { display: flex; gap: 6px; margin-left: auto; }
.nav-links a {
  padding: 8px 14px; border-radius: 999px; color: var(--ink-soft);
  font-size: 14.5px; font-weight: 600; transition: all .18s;
}
.nav-links a:hover { background: var(--cream-deep); color: var(--ink); }
.nav-links a.on { background: var(--orange-soft); color: var(--orange-deep); }
.nav-links a.link-hot { color: var(--orange-deep); }
.nav-links a.link-admin { color: #B4491F; }

.nav-actions { display: flex; align-items: center; gap: 14px; }
.bell { line-height: 1; }
.bell-btn {
  border: 0; background: var(--cream-deep); cursor: pointer;
  width: 38px; height: 38px; border-radius: 50%; font-size: 17px;
  display: grid; place-items: center; transition: transform .15s;
}
.bell-btn:hover { transform: scale(1.08); }
.avatar {
  display: grid; place-items: center; width: 40px; height: 40px; border-radius: 50%;
  background: linear-gradient(135deg, var(--honey-soft), var(--orange-soft));
  border: 1.5px solid var(--line); font-size: 20px;
}
.burger { display: none; border: 0; background: var(--cream-deep); border-radius: 10px; width: 40px; height: 40px; font-size: 18px; cursor: pointer; }

.drawer-nav { display: flex; flex-direction: column; gap: 4px; }
.drawer-nav a {
  padding: 13px 14px; border-radius: 12px; color: var(--ink); font-size: 15px; font-weight: 600;
}
.drawer-nav a:hover { background: var(--cream-deep); }
.drawer-nav em { color: var(--orange-deep); font-style: normal; }

@media (max-width: 860px) {
  .nav-links { display: none; }
  .pub-btn { display: none; }
  .burger { display: block; }
  .nav-inner { gap: 14px; }
}
</style>
