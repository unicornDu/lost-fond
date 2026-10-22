<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import appIcon from '../assets/img/app-icon.png'

const api = window.shiguangDesktop
const isDesktop = !!api?.isDesktop
const maximized = ref(false)
let offMaximized = null

function minimize() { api?.minimize() }
function toggleMaximize() { api?.toggleMaximize() }
function close() { api?.close() }
function openAppMenu() { api?.openAppMenu() }
function showAbout() { api?.showAbout() }

function onBarDoubleClick(event) {
  if (event.target.closest('button')) return
  toggleMaximize()
}

onMounted(async () => {
  if (!isDesktop) return
  try { maximized.value = await api.isMaximized() } catch (e) {}
  offMaximized = api.onMaximized((value) => { maximized.value = value })
})
onBeforeUnmount(() => { if (offMaximized) offMaximized() })
</script>

<template>
  <header v-if="isDesktop" class="desktop-titlebar" @dblclick="onBarDoubleClick">
    <div class="dtb-brand">
      <img class="dtb-icon" :src="appIcon" alt="">
      <span class="dtb-name">拾光</span>
      <span class="dtb-sub">校园失物招领</span>
    </div>

    <nav class="dtb-menus" aria-label="应用菜单">
      <button type="button" @click="openAppMenu">应用</button>
      <button type="button" @click="showAbout">帮助</button>
    </nav>

    <div class="dtb-spacer"></div>

    <div class="dtb-controls">
      <button class="dtb-btn" type="button" title="最小化" aria-label="最小化" @click="minimize">
        <svg viewBox="0 0 12 12" aria-hidden="true"><path d="M2 6h8" /></svg>
      </button>
      <button class="dtb-btn" type="button" :title="maximized ? '还原' : '最大化'" :aria-label="maximized ? '还原' : '最大化'" @click="toggleMaximize">
        <svg v-if="maximized" viewBox="0 0 12 12" aria-hidden="true">
          <path d="M4 2.5h5.5V8M2.5 4H8v5.5H2.5z" />
        </svg>
        <svg v-else viewBox="0 0 12 12" aria-hidden="true">
          <rect x="2.5" y="2.5" width="7" height="7" rx="0.6" />
        </svg>
      </button>
      <button class="dtb-btn dtb-close" type="button" title="关闭" aria-label="关闭" @click="close">
        <svg viewBox="0 0 12 12" aria-hidden="true"><path d="m2 2 8 8M10 2l-8 8" /></svg>
      </button>
    </div>
  </header>
</template>

<style scoped>
.desktop-titlebar {
  position: fixed;
  inset: 0 0 auto 0;
  z-index: 1000;
  height: 44px;
  display: flex;
  align-items: stretch;
  gap: 10px;
  padding-left: 13px;
  background: var(--cream);
  border-bottom: 1px solid var(--line);
  -webkit-app-region: drag;
  user-select: none;
}
.dtb-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.dtb-icon {
  width: 20px;
  height: 20px;
  border-radius: 6px;
}
.dtb-name {
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 750;
  letter-spacing: .04em;
}
.dtb-sub {
  padding-left: 9px;
  border-left: 1px solid var(--line-2);
  color: var(--ink-faint);
  font-size: 10.5px;
  white-space: nowrap;
}
.dtb-menus {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-left: 8px;
  -webkit-app-region: no-drag;
}
.dtb-menus button {
  height: 28px;
  padding: 0 10px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: var(--ink-soft);
  font-size: 12px;
  cursor: default;
  transition: background-color .16s ease, color .16s ease;
}
.dtb-menus button:hover {
  background: var(--cream-deep);
  color: var(--ink);
}
.dtb-spacer { flex: 1; }
.dtb-controls { display: flex; -webkit-app-region: no-drag; }
.dtb-btn {
  width: 46px;
  height: 44px;
  border: 0;
  background: transparent;
  display: grid;
  place-items: center;
  cursor: default;
  transition: background-color .15s ease;
}
.dtb-btn svg {
  width: 10px;
  height: 10px;
  fill: none;
  stroke: var(--ink-soft);
  stroke-width: 1.2;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.dtb-btn:hover { background: var(--cream-deep); }
.dtb-btn.dtb-close:hover { background: #d93b30; }
.dtb-btn.dtb-close:hover svg { stroke: #fff; }

@media (max-width: 960px) {
  .dtb-sub { display: none; }
  .dtb-menus { margin-left: 0; }
}
</style>
