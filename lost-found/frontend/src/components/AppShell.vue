<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppSidebar from './AppSidebar.vue'
import FeedRail from './FeedRail.vue'
import InboxDrawer from './InboxDrawer.vue'
import { useUserStore } from '../stores/user'
import { initialOf, avatarColor } from '../utils/display'
import { t } from '../i18n'

/**
 * 登录后应用骨架（Digg 三栏）：左竖导航 232px / 中内容 / 右信息栏 300px。
 * - 左栏与右栏各自 sticky + 独立滚动，只有中栏跟随页面滚动。
 * - 右栏只在信息流类页面出现（首页 / 失物墙 / 详情），表单类页面让出宽度。
 * - ≤1080px 收右栏；≤860px 收左栏，改用顶栏 + 抽屉。
 */
const route = useRoute()
const user = useUserStore()

const mobileNav = ref(false)
const inboxOpen = ref(false)
const inboxMode = ref('notify')

const RAIL_ROUTES = ['home', 'wall', 'detail']
const showRail = computed(() => RAIL_ROUTES.includes(route.name))

function openInbox(mode) {
  inboxMode.value = mode
  inboxOpen.value = true
}

/* 切页时关闭移动端抽屉，避免遮住新页面 */
watch(() => route.fullPath, () => { mobileNav.value = false })

const meName = computed(() => user.info?.nickname || user.info?.account || t('me.student'))
</script>

<template>
  <div class="shell" :class="{ 'has-rail': showRail }">
    <AppSidebar v-model:mobile-open="mobileNav" @inbox="openInbox" />

    <div class="col">
      <!-- 移动端顶栏（桌面隐藏） -->
      <header class="m-bar">
        <button class="m-burger" type="button" :aria-label="$t('nav.open')" @click="mobileNav = true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18M3 12h18M3 18h18" /></svg>
        </button>
        <RouterLink to="/home" class="m-brand">
          <span class="m-logo">拾</span>
          <b>拾光</b>
        </RouterLink>
        <div class="m-actions">
          <RouterLink to="/publish" class="m-pub">{{ $t('nav.publishShort') }}</RouterLink>
          <RouterLink to="/profile" class="m-ava" :style="{ background: avatarColor(meName) }">
            {{ initialOf(meName) }}
          </RouterLink>
        </div>
      </header>

      <main class="content">
        <slot />
      </main>
    </div>

    <FeedRail v-if="showRail" />
  </div>

  <InboxDrawer v-model="inboxOpen" v-model:mode="inboxMode" />
</template>

<style scoped>
.shell {
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr);
  max-width: 1240px;
  margin: 0 auto;
  min-height: 100vh;
  align-items: start;
}
.shell.has-rail { grid-template-columns: 232px minmax(0, 1fr) 300px; }

.col { min-width: 0; }
.content { padding: 26px 34px 80px; min-width: 0; }

/* 视图里普遍以 .container 包一层（旧版式是整页容器），进壳后让出宽度交给中栏 */
.content :deep(.container) { width: 100%; margin: 0; }
.content :deep(.page) { padding: 0; }
.content :deep(.page-title) { margin-bottom: 8px; }
.content :deep(.admin-wrap) { padding-top: 0; }

/* 移动端顶栏 */
.m-bar {
  display: none;
  position: sticky; top: 0; z-index: 90;
  height: 58px;
  padding: 0 14px;
  padding-top: var(--safe-top);
  align-items: center; gap: 10px;
  background: color-mix(in srgb, var(--cream) 88%, transparent);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--line);
}
.m-burger {
  width: 36px; height: 36px; border: 0; border-radius: 10px;
  background: var(--cream-deep); color: var(--ink);
  display: grid; place-items: center; cursor: pointer;
}
.m-burger svg { width: 18px; height: 18px; }
.m-brand { display: flex; align-items: center; gap: 8px; }
.m-logo {
  width: 26px; height: 26px; border-radius: 8px;
  background: var(--ink); color: var(--paper);
  display: grid; place-items: center;
  font-family: var(--font-display); font-size: 13px; font-weight: 800;
}
.m-brand b { font-size: 15px; font-weight: 800; }
.m-actions { margin-left: auto; display: flex; align-items: center; gap: 8px; }
.m-pub {
  padding: 8px 14px; border-radius: 999px;
  background: var(--ink); color: #fff; font-size: 12.5px; font-weight: 700;
}
.m-ava {
  width: 32px; height: 32px; border-radius: 50%;
  display: grid; place-items: center; color: #fff; font-size: 13px; font-weight: 700;
}

@media (max-width: 1080px) {
  .shell.has-rail { grid-template-columns: 232px minmax(0, 1fr); }
}
@media (max-width: 860px) {
  .shell, .shell.has-rail { grid-template-columns: minmax(0, 1fr); }
  .m-bar { display: flex; }
  .content { padding: 18px 16px calc(56px + var(--safe-bottom)); }
}
</style>
