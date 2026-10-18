<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { api } from '../api'

/**
 * 应用导航（G 版式：下划线滑入链接 + SVG 图标 + 聊天/通知双入口）。
 * 审查 2.7 消息铃铛（未读红点）；2.9 管理员"后台管理"入口；2.22 窄屏汉堡+抽屉。
 * 聊天入口 → 个人中心私信会话；铃铛入口 → 个人中心通知中心（三分栏）。
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
function goChats() {
  drawer.value = false
  router.push({ path: '/profile', query: { tab: 'msg', sub: 'dm' } })
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
        <RouterLink to="/search-image" active-class="on">以图搜图</RouterLink>
        <RouterLink v-if="user.isAdmin" to="/admin" active-class="on">后台管理</RouterLink>
      </nav>

      <div class="nav-actions">
        <!-- 聊天会话（私信）入口 -->
        <button v-if="user.info" class="nav-icon" title="聊天会话" aria-label="聊天会话" @click="goChats">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a8 8 0 0 1-8 8H5l-2 2V12a8 8 0 0 1 8-8h2a8 8 0 0 1 8 8z"/></svg>
        </button>
        <!-- 消息铃铛（审查 2.7） -->
        <button v-if="user.info" class="nav-icon" title="通知中心" aria-label="通知中心" @click="goMessages">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8a6 6 0 1 0-12 0c0 7-3 8-3 8h18s-3-1-3-8"/><path d="M13.7 20a2 2 0 0 1-3.4 0"/></svg>
          <span v-if="unread" class="nav-dot">{{ unread > 99 ? '99+' : unread }}</span>
        </button>
        <el-button type="primary" round class="pub-btn" @click="router.push('/publish')">
          <svg class="btn-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/></svg>
          发布启事
        </el-button>
        <RouterLink v-if="user.info" to="/profile" class="avatar" :title="user.info.nickname || user.info.account">
          {{ user.info.avatar || '拾' }}
        </RouterLink>
        <el-button v-else round plain @click="router.push('/auth')">登录</el-button>
        <!-- 窄屏汉堡（审查 2.22） -->
        <button class="burger" title="菜单" @click="drawer = true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18M3 12h18M3 18h18"/></svg>
        </button>
      </div>
    </div>

    <!-- 移动端抽屉导航 -->
    <el-drawer v-model="drawer" direction="rtl" size="70%" title="拾光 · 菜单">
      <nav class="drawer-nav">
        <RouterLink to="/home" @click="drawer = false">首页</RouterLink>
        <RouterLink :to="{ path: '/wall', query: { type: 'lost' } }" @click="drawer = false">失物墙</RouterLink>
        <RouterLink :to="{ path: '/wall', query: { type: 'found' } }" @click="drawer = false">招领墙</RouterLink>
        <RouterLink to="/search-image" @click="drawer = false">以图搜图</RouterLink>
        <RouterLink to="/publish" @click="drawer = false">发布启事</RouterLink>
        <RouterLink v-if="user.info" to="/profile" @click="drawer = false">个人中心</RouterLink>
        <RouterLink v-if="user.info" to="/profile?tab=msg&sub=dm" @click="drawer = false">聊天会话</RouterLink>
        <RouterLink v-if="user.info" to="/profile?tab=msg" @click="drawer = false">通知中心<em v-if="unread"> ({{ unread }})</em></RouterLink>
        <RouterLink v-if="user.isAdmin" to="/admin" @click="drawer = false">后台管理</RouterLink>
        <RouterLink v-if="!user.info" to="/auth" @click="drawer = false">登录 / 注册</RouterLink>
      </nav>
    </el-drawer>
  </header>
</template>

<style scoped>
.nav {
  position: sticky; top: 0; z-index: 100;
  /* F-14：与落地页一致，让开状态栏（Android 15 edge-to-edge / iOS 刘海） */
  padding-top: var(--safe-top);
  background: color-mix(in srgb, var(--paper) 88%, transparent);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--line);
}
.nav-inner { display: flex; align-items: center; gap: 30px; height: 68px; }
.brand { display: flex; align-items: center; gap: 11px; }
.brand-logo {
  display: grid; place-items: center; width: 38px; height: 38px; border-radius: 11px;
  background: var(--orange); color: #F4FBF8;
  font-family: var(--font-display); font-weight: 800; font-size: 19px;
  box-shadow: 0 6px 14px rgba(15, 122, 92, .28);
}
.brand-name { display: flex; flex-direction: column; font-weight: 800; font-size: 17px; line-height: 1.15; }
.brand-name small { font-weight: 600; font-size: 10px; color: var(--ink-faint); letter-spacing: 2.5px; }

/* 链接：下划线滑入（G 版式） */
.nav-links { display: flex; gap: 26px; margin-left: auto; }
.nav-links a {
  position: relative; padding: 6px 0;
  color: var(--ink-soft); font-size: 14px; font-weight: 600;
  transition: color .18s;
}
.nav-links a::after {
  content: ''; position: absolute; left: 0; bottom: 0; height: 2px; width: 100%;
  background: var(--orange);
  transform: scaleX(0); transform-origin: left;
  transition: transform .3s cubic-bezier(.22, 1, .36, 1);
}
.nav-links a:hover { color: var(--ink); }
.nav-links a:hover::after { transform: scaleX(1); }
.nav-links a.on { color: var(--orange-deep); }
.nav-links a.on::after { transform: scaleX(1); }

.nav-actions { display: flex; align-items: center; gap: 12px; }
.nav-icon {
  position: relative; border: 0; cursor: pointer;
  width: 40px; height: 40px; border-radius: 12px;
  background: var(--cream-deep); color: var(--ink-soft);
  display: grid; place-items: center;
  transition: all .18s cubic-bezier(.22, 1, .36, 1);
}
.nav-icon:hover { background: var(--orange-soft); color: var(--orange-deep); transform: translateY(-1px); }
.nav-icon svg { width: 17px; height: 17px; }
.nav-dot {
  position: absolute; top: -5px; right: -5px; min-width: 18px; height: 18px; padding: 0 4px;
  border-radius: 999px; background: var(--rose); color: #fff;
  border: 2px solid var(--paper);
  font-size: 10px; font-weight: 800;
  display: grid; place-items: center;
}
.btn-ico { width: 14px; height: 14px; margin-right: 4px; }
.avatar {
  display: grid; place-items: center; width: 40px; height: 40px; border-radius: 12px;
  background: var(--orange-soft); border: 1.5px solid var(--line);
  font-size: 19px;
  transition: all .18s;
}
.avatar:hover { border-color: var(--orange); transform: translateY(-1px); }
.burger {
  display: none; border: 0; background: var(--cream-deep); border-radius: 12px;
  width: 40px; height: 40px; color: var(--ink); cursor: pointer;
  place-items: center;
}
.burger svg { width: 18px; height: 18px; }

.drawer-nav { display: flex; flex-direction: column; gap: 4px; }
.drawer-nav a {
  padding: 13px 14px; border-radius: 12px; color: var(--ink); font-size: 15px; font-weight: 600;
}
.drawer-nav a:hover { background: var(--cream-deep); }
.drawer-nav em { color: var(--orange-deep); font-style: normal; }

@media (max-width: 1024px) {
  .nav-links { display: none; }
  .pub-btn { display: none; }
  .burger { display: grid; }
  .nav-inner { gap: 14px; }
}
</style>
