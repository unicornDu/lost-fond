<script setup>
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { api } from '../api'
import { initialOf, avatarColor } from '../utils/display'
import { LOCALES, locale, setLocale, t } from '../i18n'
import { theme, toggleTheme } from '../composables/useTheme'

/**
 * 左侧竖排导航（Digg 三栏骨架）。
 * - 232px sticky 全高，独立滚动；选中态为浅灰圆角块。
 * - 失物墙 / 招领墙共用 /wall 路由，用 query.type 区分高亮。
 * - 通知 / 聊天都是独立页（/notifications、/messages），不再弹右侧抽屉。
 * - 底部设置区：亮/暗主题 + 界面语言（简/繁/EN），与指南的"正式版并入左导航"一致。
 */
const props = defineProps({
  mobileOpen: { type: Boolean, default: false },
})
const emit = defineEmits(['update:mobileOpen'])

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const unread = ref(0)      // 通知（认领 / 审核 / 系统）
const dmUnread = ref(0)    // 私信
const counts = ref({ lost: 0, found: 0 })
const langOpen = ref(false)
let timer = null
let lastPoll = 0

async function pollUnread() {
  lastPoll = Date.now()
  if (!user.token) { unread.value = 0; dmUnread.value = 0; return }
  try {
    // 通知与私信已经拆成两个页面，角标也按类型分开算
    const list = (await api.messages()) || []
    unread.value = list.filter((m) => m.isRead === 0 && m.type !== 'dm').length
    dmUnread.value = list.filter((m) => m.isRead === 0 && m.type === 'dm').length
  } catch (e) { /* 静默：轮询失败不打扰 */ }
}

async function loadCounts() {
  try {
    const s = await api.stats()
    counts.value = { lost: s.lostCount || 0, found: s.foundCount || 0 }
  } catch (e) { /* 统计失败不影响导航 */ }
}

function closeLang(e) {
  if (!e.target.closest?.('.lang-wrap')) langOpen.value = false
}

onMounted(() => {
  pollUnread()
  loadCounts()
  timer = setInterval(pollUnread, 30000)
  window.addEventListener('shiguang-unread-changed', pollUnread)
  window.addEventListener('shiguang-items-changed', loadCounts)
  document.addEventListener('click', closeLang)
})
onBeforeUnmount(() => {
  clearInterval(timer)
  window.removeEventListener('shiguang-unread-changed', pollUnread)
  window.removeEventListener('shiguang-items-changed', loadCounts)
  document.removeEventListener('click', closeLang)
})
watch(() => route.fullPath, () => {
  if (Date.now() - lastPoll < 5000) return
  pollUnread()
})

/* 高亮判定：wall 需要看 query.type，其余比对 path */
const isOn = (key) => {
  if (key === 'home') return route.path === '/home'
  return route.path.startsWith(key)
}
const wallOn = computed(() => route.path === '/wall')
const homeOn = computed(() => route.path === '/home')

const meName = computed(() => user.info?.nickname || user.info?.account || t('me.student'))
const meSub = computed(() => {
  if (!user.info) return t('me.guest')
  return user.info.account ? t('me.id', { n: user.info.account }) : t('me.campus')
})
const curLang = computed(() => LOCALES.find((l) => l.code === locale.value) || LOCALES[0])

function go(payload) {
  emit('update:mobileOpen', false)
  router.push(payload)
}
function pickLang(code) {
  setLocale(code)
  langOpen.value = false
}
</script>

<template>
  <!-- 桌面/平板：sticky 左栏 -->
  <aside class="side">
    <RouterLink to="/home" class="brand" :aria-label="$t('nav.brandHome')">
      <span class="logo">拾</span>
      <span class="brand-txt"><b>拾光</b><small>LOST &amp; FOUND</small></span>
    </RouterLink>

    <nav class="nav" :aria-label="$t('nav.main')">
      <RouterLink to="/home" class="n" :class="{ on: homeOn }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 10.5 12 3l9 7.5V21H3z" /></svg>
        <span>{{ $t('nav.home') }}</span>
      </RouterLink>

      <RouterLink :to="{ path: '/wall', query: { type: 'lost' } }" class="n" :class="{ on: wallOn && route.query.type !== 'found' }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9" /><path d="M12 8v4l2.5 2.5" /></svg>
        <span>{{ $t('nav.lost') }}</span>
        <em v-if="counts.lost" class="badge">{{ counts.lost > 99 ? '99+' : counts.lost }}</em>
      </RouterLink>

      <RouterLink :to="{ path: '/wall', query: { type: 'found' } }" class="n" :class="{ on: wallOn && route.query.type === 'found' }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 7h-9M14 3l-4 4 4 4" /><path d="M4 17h9M10 13l4 4-4 4" /></svg>
        <span>{{ $t('nav.found') }}</span>
        <em v-if="counts.found" class="badge">{{ counts.found > 99 ? '99+' : counts.found }}</em>
      </RouterLink>

      <RouterLink to="/search-image" class="n" :class="{ on: isOn('/search-image') }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="4" /><circle cx="9" cy="9" r="2" /><path d="m21 15-4.5-4.5L6 21" /></svg>
        <span>{{ $t('nav.search') }}</span>
      </RouterLink>

      <button class="n" type="button" @click="go('/publish')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M5 12h14" /></svg>
        <span>{{ $t('nav.publish') }}</span>
      </button>

      <RouterLink to="/notifications" class="n" :class="{ on: route.path === '/notifications' }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8a6 6 0 1 0-12 0c0 7-3 8-3 8h18s-3-1-3-8" /><path d="M10 21a2 2 0 0 0 4 0" /></svg>
        <span>{{ $t('nav.notify') }}</span>
        <em v-if="unread" class="badge">{{ unread > 99 ? '99+' : unread }}</em>
      </RouterLink>

      <RouterLink to="/messages" class="n" :class="{ on: route.path === '/messages' }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a8 8 0 0 1-8 8H5l-2 2V12a8 8 0 0 1 8-8h2a8 8 0 0 1 8 8z" /></svg>
        <span>{{ $t('nav.chat') }}</span>
        <em v-if="dmUnread" class="badge">{{ dmUnread > 99 ? '99+' : dmUnread }}</em>
      </RouterLink>

      <RouterLink to="/watch" class="n" :class="{ on: route.path === '/watch' }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2.5 12S6 5.8 12 5.8 21.5 12 21.5 12 18 18.2 12 18.2 2.5 12 2.5 12Z" /><circle cx="12" cy="12" r="2.8" /></svg>
        <span>{{ $t('nav.watch') }}</span>
      </RouterLink>

      <RouterLink v-if="user.isAdmin" to="/admin" class="n" :class="{ on: isOn('/admin') }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m12 3 8 3v6c0 5-3.4 8.3-8 9-4.6-.7-8-4-8-9V6z" /></svg>
        <span>{{ $t('nav.admin') }}</span>
      </RouterLink>
    </nav>

    <div class="spacer"></div>

    <!-- 设置区：主题 + 语言 -->
    <div class="prefs">
      <button class="pref" type="button" :title="$t('common.theme')" @click="toggleTheme">
        <svg v-if="theme === 'dark'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="4" /><path d="M12 2v2m0 16v2M4.9 4.9l1.4 1.4m11.4 11.4 1.4 1.4M2 12h2m16 0h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" /></svg>
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.8A9 9 0 1 1 11.2 3 7 7 0 0 0 21 12.8z" /></svg>
        <span>{{ theme === 'dark' ? $t('common.theme.light') : $t('common.theme.dark') }}</span>
      </button>

      <div class="lang-wrap">
        <button class="pref lang" type="button" :title="$t('common.language')" :aria-expanded="langOpen" @click.stop="langOpen = !langOpen">
          <span>{{ curLang.short }}</span>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><path d="m6 9 6 6 6-6" /></svg>
        </button>
        <div v-if="langOpen" class="lang-menu">
          <button
            v-for="l in LOCALES" :key="l.code" type="button"
            :class="{ on: l.code === locale }" @click.stop="pickLang(l.code)"
          >
            {{ l.label }}<i v-if="l.code === locale">✓</i>
          </button>
        </div>
      </div>
    </div>

    <RouterLink to="/profile" class="me">
      <span class="ava" :style="{ background: avatarColor(meName) }">{{ initialOf(meName) }}</span>
      <span class="me-txt">
        <b>{{ meName }}</b>
        <small>{{ meSub }}</small>
      </span>
    </RouterLink>
  </aside>

  <!-- 移动端抽屉：同一份导航 -->
  <el-drawer
    :model-value="mobileOpen" direction="ltr" size="78%"
    :with-header="false" class="nav-drawer"
    @update:model-value="emit('update:mobileOpen', $event)"
  >
    <div class="drawer-body">
      <RouterLink to="/home" class="brand" @click="emit('update:mobileOpen', false)">
        <span class="logo">拾</span>
        <span class="brand-txt"><b>拾光</b><small>LOST &amp; FOUND</small></span>
      </RouterLink>
      <nav class="nav" :aria-label="$t('nav.main')">
        <RouterLink to="/home" class="n" :class="{ on: homeOn }" @click="emit('update:mobileOpen', false)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 10.5 12 3l9 7.5V21H3z" /></svg><span>{{ $t('nav.home') }}</span>
        </RouterLink>
        <RouterLink :to="{ path: '/wall', query: { type: 'lost' } }" class="n" :class="{ on: wallOn && route.query.type !== 'found' }" @click="emit('update:mobileOpen', false)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9" /><path d="M12 8v4l2.5 2.5" /></svg><span>{{ $t('nav.lost') }}</span>
        </RouterLink>
        <RouterLink :to="{ path: '/wall', query: { type: 'found' } }" class="n" :class="{ on: wallOn && route.query.type === 'found' }" @click="emit('update:mobileOpen', false)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 7h-9M14 3l-4 4 4 4" /><path d="M4 17h9M10 13l4 4-4 4" /></svg><span>{{ $t('nav.found') }}</span>
        </RouterLink>
        <RouterLink to="/search-image" class="n" @click="emit('update:mobileOpen', false)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="4" /><circle cx="9" cy="9" r="2" /><path d="m21 15-4.5-4.5L6 21" /></svg><span>{{ $t('nav.search') }}</span>
        </RouterLink>
        <RouterLink to="/publish" class="n" @click="emit('update:mobileOpen', false)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M5 12h14" /></svg><span>{{ $t('nav.publish') }}</span>
        </RouterLink>
        <RouterLink to="/notifications" class="n" :class="{ on: route.path === '/notifications' }" @click="emit('update:mobileOpen', false)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8a6 6 0 1 0-12 0c0 7-3 8-3 8h18s-3-1-3-8" /><path d="M10 21a2 2 0 0 0 4 0" /></svg><span>{{ $t('nav.notify') }}</span><em v-if="unread" class="badge">{{ unread }}</em>
        </RouterLink>
        <RouterLink to="/messages" class="n" :class="{ on: route.path === '/messages' }" @click="emit('update:mobileOpen', false)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a8 8 0 0 1-8 8H5l-2 2V12a8 8 0 0 1 8-8h2a8 8 0 0 1 8 8z" /></svg><span>{{ $t('nav.chat') }}</span><em v-if="dmUnread" class="badge">{{ dmUnread }}</em>
        </RouterLink>
        <RouterLink to="/watch" class="n" :class="{ on: route.path === '/watch' }" @click="emit('update:mobileOpen', false)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2.5 12S6 5.8 12 5.8 21.5 12 21.5 12 18 18.2 12 18.2 2.5 12 2.5 12Z" /><circle cx="12" cy="12" r="2.8" /></svg><span>{{ $t('nav.watch') }}</span>
        </RouterLink>
        <RouterLink v-if="user.isAdmin" to="/admin" class="n" @click="emit('update:mobileOpen', false)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m12 3 8 3v6c0 5-3.4 8.3-8 9-4.6-.7-8-4-8-9V6z" /></svg><span>{{ $t('nav.admin') }}</span>
        </RouterLink>
      </nav>

      <div class="spacer"></div>

      <div class="prefs drawer-prefs">
        <button class="pref" type="button" @click="toggleTheme">
          <svg v-if="theme === 'dark'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="4" /><path d="M12 2v2m0 16v2M4.9 4.9l1.4 1.4m11.4 11.4 1.4 1.4M2 12h2m16 0h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" /></svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.8A9 9 0 1 1 11.2 3 7 7 0 0 0 21 12.8z" /></svg>
          <span>{{ theme === 'dark' ? $t('common.theme.light') : $t('common.theme.dark') }}</span>
        </button>
        <div class="lang-wrap">
          <button class="pref lang" type="button" @click.stop="langOpen = !langOpen">
            <span>{{ curLang.short }}</span>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><path d="m6 9 6 6 6-6" /></svg>
          </button>
          <div v-if="langOpen" class="lang-menu above">
            <button v-for="l in LOCALES" :key="l.code" type="button" :class="{ on: l.code === locale }" @click.stop="pickLang(l.code)">
              {{ l.label }}<i v-if="l.code === locale">✓</i>
            </button>
          </div>
        </div>
      </div>

      <RouterLink to="/profile" class="me" @click="emit('update:mobileOpen', false)">
        <span class="ava" :style="{ background: avatarColor(meName) }">{{ initialOf(meName) }}</span>
        <span class="me-txt"><b>{{ meName }}</b><small>{{ meSub }}</small></span>
      </RouterLink>
    </div>
  </el-drawer>
</template>

<style scoped>
.side {
  position: sticky; top: 0;
  height: 100vh;
  padding: 22px 14px;
  display: flex; flex-direction: column;
  border-right: 1px solid var(--line);
  overflow-y: auto;
  /* v2.41：侧栏滚动条隐藏（滚轮/触摸仍可滚）。滚条不再占位，故无需 scrollbar-gutter，
     否则会在右侧留一条看不见的空白，和 14px 内边距叠成不对称留白。 */
  scrollbar-width: none;
  overscroll-behavior: contain;
}
.side::-webkit-scrollbar { width: 0; height: 0; }

.brand { display: flex; align-items: center; gap: 10px; padding: 4px 10px 18px; }
.logo {
  width: 30px; height: 30px; border-radius: 9px;
  background: var(--ink); color: var(--paper);
  display: grid; place-items: center;
  font-family: var(--font-display); font-size: 15px; font-weight: 800;
  transition: background-color .3s ease, color .3s ease;
}
.brand-txt { display: flex; flex-direction: column; }
.brand-txt b { font-size: 17px; font-weight: 800; letter-spacing: .02em; line-height: 1; }
.brand-txt small { font-size: 9.5px; font-weight: 600; color: var(--ink-faint); letter-spacing: .18em; margin-top: 4px; }

/* 导航做成一条浅灰轨道，选中项是浮起的白色药丸（v2.20 选定的分段式效果） */
.nav {
  display: flex; flex-direction: column; gap: 3px;
  padding: 5px; border-radius: 17px;
  background: var(--nav-track);
  transition: background-color .3s ease;
}
.n {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 12px; border-radius: 12px;
  border: 0; background: transparent; width: 100%;
  color: var(--ink-soft); font-family: inherit; font-size: 13.5px; font-weight: 600;
  cursor: pointer; text-align: left;
  transition: background-color .18s ease, color .18s ease, box-shadow .18s ease;
}
.n svg { width: 19px; height: 19px; flex: none; }
.n:focus { outline: none; }
.n:focus-visible { outline: 2px solid var(--ink-faint); outline-offset: 2px; }
.n:hover { background: var(--nav-hover); color: var(--ink); }
.n.on {
  background: var(--nav-pill); color: var(--ink); font-weight: 700;
  box-shadow: var(--nav-pill-shadow);
}
.n.on:hover { background: var(--nav-pill); }
.badge {
  margin-left: auto; font-style: normal;
  font-size: 10.5px; font-weight: 700;
  background: var(--paper); color: var(--ink-soft);
  border: 1px solid var(--line);
  padding: 3px 7px; border-radius: 999px;
  font-variant-numeric: tabular-nums;
}
.n.on .badge { background: var(--ink); color: var(--paper); border-color: var(--ink); }

.spacer { flex: 1; min-height: 14px; }

/* ---- 设置区 ---- */
.prefs { display: flex; gap: 8px; margin-bottom: 10px; }
.pref {
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  height: 32px; padding: 0 11px;
  border: 1px solid var(--line); border-radius: 999px;
  background: var(--paper); color: var(--ink-soft);
  font-family: inherit; font-size: 12px; font-weight: 600;
  cursor: pointer; transition: background .16s, color .16s, border-color .16s;
}
.pref:hover { background: var(--cream-deep); color: var(--ink); border-color: var(--line-2); }
.pref svg { width: 14px; height: 14px; flex: none; }
.pref.lang { padding: 0 9px; gap: 4px; }
.pref.lang svg { width: 11px; height: 11px; }
.lang-wrap { position: relative; }
.lang-menu {
  position: absolute; bottom: calc(100% + 6px); right: 0; z-index: 20;
  min-width: 132px; padding: 5px;
  background: var(--paper); border: 1px solid var(--line);
  border-radius: 12px; box-shadow: var(--shadow-lift);
  display: flex; flex-direction: column; gap: 2px;
}
.lang-menu button {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  border: 0; background: transparent; color: var(--ink-soft);
  font-family: inherit; font-size: 12.5px; font-weight: 600;
  padding: 8px 10px; border-radius: 8px; cursor: pointer; text-align: left;
}
.lang-menu button:hover { background: var(--cream-deep); color: var(--ink); }
.lang-menu button.on { color: var(--ink); }
.lang-menu i { font-style: normal; font-size: 11px; }

.me { display: flex; align-items: center; gap: 10px; padding: 9px; border-radius: 14px; margin-top: 2px; }
.me:hover { background: var(--cream-deep); }
.ava {
  width: 32px; height: 32px; border-radius: 50%; flex: none;
  display: grid; place-items: center; color: #fff; font-size: 13px; font-weight: 700;
}
.me-txt { display: flex; flex-direction: column; min-width: 0; }
.me-txt b { font-size: 13px; font-weight: 700; line-height: 1.2; }
.me-txt small { font-size: 11px; color: var(--ink-faint); line-height: 1.3; margin-top: 2px; }

.drawer-body { display: flex; flex-direction: column; height: 100%; padding-top: 8px; }
.drawer-prefs { margin-bottom: 12px; }

@media (max-width: 860px) {
  .side { display: none; }
}
</style>

<style>
/* 抽屉挂到 body，需全局样式（scoped 打不到 teleport 内容） */
.nav-drawer .el-drawer__body { padding: 14px; }
</style>
