<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppSidebar from './AppSidebar.vue'
import FeedRail from './FeedRail.vue'
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

/* 右栏折叠：桌面端可收起，状态记在本地；收起后中栏自动变宽 */
const railClosed = ref(false)
onMounted(() => { railClosed.value = localStorage.getItem('shiguang_rail_closed') === '1' })
watch(railClosed, (v) => localStorage.setItem('shiguang_rail_closed', v ? '1' : '0'))
function toggleRail() { railClosed.value = !railClosed.value }

const RAIL_ROUTES = ['home', 'wall', 'detail']
const showRail = computed(() => RAIL_ROUTES.includes(route.name))

/* 切页时关闭移动端抽屉，避免遮住新页面 */
watch(() => route.fullPath, () => { mobileNav.value = false })

const meName = computed(() => user.info?.nickname || user.info?.account || t('me.student'))
</script>

<template>
  <div class="shell" :class="{ 'has-rail': showRail, 'rail-closed': railClosed }">
    <AppSidebar v-model:mobile-open="mobileNav" />

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

    <div v-if="showRail" class="rail-col" :class="{ closed: railClosed }">
      <div class="rail-clip"><FeedRail /></div>
      <button
        class="rail-toggle" type="button"
        :aria-expanded="!railClosed"
        :title="railClosed ? $t('rail.expand') : $t('rail.collapse')"
        :aria-label="railClosed ? $t('rail.expand') : $t('rail.collapse')"
        @click="toggleRail"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round">
          <rect x="3.5" y="4.5" width="17" height="15" rx="3.5" />
          <path d="M15.6 5.2v13.6" />
        </svg>
      </button>
    </div>
  </div>
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
.shell.has-rail { grid-template-columns: 232px minmax(0, 1fr) auto; }

.col { min-width: 0; }
.content {
  padding: 26px 34px 80px; min-width: 0;
  /* 三个内容页的宽度上限：右栏收起时放宽，中栏把省下的 300px 吃掉 */
  --feed-max: 760px;
  --wall-max: 860px;
  --detail-max: 760px;
}
.shell.rail-closed .content { --feed-max: 980px; --wall-max: 1180px; --detail-max: 980px; }

/* 右栏外壳：宽度 300→0 做过渡，内部保持 300 避免内容重排。
   v2.20.2：sticky 从内部的 .rail 挪到外壳上。
   原来 .rail 自己 sticky，但它被 .rail-clip（overflow:hidden）包着——
   overflow 会生成滚动容器，sticky 只能相对这个永不滚动的容器生效，
   于是整列跟着页面滚走、右侧留白。现在外壳 sticky + 100vh，内部只负责滚动。 */
.rail-col {
  position: sticky;
  top: 0;
  height: 100vh;
  width: 300px;
  transition: width .42s cubic-bezier(.22, 1, .36, 1);
}
.rail-col.closed { width: 0; }
.rail-clip { width: 100%; height: 100%; overflow: hidden; }

/* 折叠把手（v2.20.1）：固定在右上角，页面上下滚动时位置不变。
   right 用 max() 跟随居中壳体的右边缘——视口比 1240 宽时不会飘到浏览器最右侧。 */
.rail-toggle {
  position: fixed;
  top: 14px;
  right: calc(max(0px, (100% - 1240px) / 2) + 14px);
  width: 34px; height: 34px; border-radius: 50%;
  display: grid; place-items: center;
  background: var(--paper); color: var(--ink-soft);
  border: 1px solid var(--line);
  box-shadow: 0 2px 10px rgba(20, 20, 18, .1);
  cursor: pointer; z-index: 100;
  transition: color .2s ease, border-color .2s ease, background-color .2s ease, box-shadow .2s ease;
}
.rail-toggle:hover {
  color: var(--ink); border-color: var(--line-2); background: var(--cream);
  box-shadow: 0 4px 14px rgba(20, 20, 18, .14);
}
.rail-toggle svg { width: 15px; height: 15px; transition: transform .35s cubic-bezier(.22, 1, .36, 1); }
.rail-col.closed .rail-toggle svg { transform: rotate(180deg); }

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
  .rail-col, .rail-toggle { display: none; }
  .content { --feed-max: 980px; --wall-max: 1180px; --detail-max: 980px; }
}
@media (max-width: 860px) {
  .shell, .shell.has-rail { grid-template-columns: minmax(0, 1fr); }
  .m-bar { display: flex; }
  .content { padding: 18px 16px calc(56px + var(--safe-bottom)); }
}
</style>
