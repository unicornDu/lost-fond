<script setup>
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import NavBar from './components/NavBar.vue'
import { useUserStore } from './stores/user'

// 公开页（落地页 / 登录注册）自带头部与页脚，不渲染应用导航
const route = useRoute()
const user = useUserStore()

// 审查 2.39：启动时刷新用户态（封禁/降权/改昵称后不显示旧身份）；refresh 失败会自动 logout
onMounted(() => {
  if (user.token) user.refresh()
})
</script>

<template>
  <NavBar v-if="!route.meta.public" />
  <main class="app-main" :class="{ 'safe-top': route.meta.public && route.name !== 'welcome' }">
    <router-view v-slot="{ Component }">
      <transition name="page" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </main>
  <footer v-if="!route.meta.public" class="app-footer">
    <div class="container">
      <span class="foot-logo">拾光</span>
      <span>校园失物招领平台 · 软件工程课程设计小组项目</span>
      <span class="foot-tip">Spring Boot + Vue3 + MySQL · 以图搜图采用感知哈希 pHash</span>
    </div>
  </footer>
</template>

<style scoped>
.app-main { min-height: calc(100vh - 150px); }
.app-footer {
  border-top: 1px dashed var(--line);
  background: var(--paper);
  padding: 22px 0;
}
.app-footer .container {
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  color: var(--ink-soft); font-size: 13px; text-align: center;
}
.foot-logo { font-family: var(--font-display); font-size: 20px; color: var(--orange); }
.foot-tip { color: var(--ink-faint); font-size: 12px; }

.page-enter-active, .page-leave-active { transition: opacity .22s ease, transform .22s ease; }
.page-enter-from { opacity: 0; transform: translateY(10px); }
.page-leave-to { opacity: 0; transform: translateY(-6px); }
</style>
