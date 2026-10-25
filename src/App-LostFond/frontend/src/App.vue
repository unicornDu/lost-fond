<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import zhTw from 'element-plus/es/locale/lang/zh-tw'
import en from 'element-plus/es/locale/lang/en'
import AppShell from './components/AppShell.vue'
import DesktopTitleBar from './components/DesktopTitleBar.vue'
import { locale, t } from './i18n'
import { useUserStore } from './stores/user'

/**
 * 应用根：登录后页面统一进 AppShell（Digg 三栏骨架），
 * 公开页（落地页 / 登录注册 / 404）自带头部与页脚，不渲染应用导航。
 */
const route = useRoute()
const user = useUserStore()

/* Element Plus 内置文案（分页 / 空态 / 日期选择）跟随界面语言 */
const epLocale = computed(() => (
  locale.value === 'en' ? en : locale.value === 'zh-TW' ? zhTw : zhCn
))

/* 切语言后同步浏览器标题（路由 afterEach 只在导航时执行） */
watch(locale, () => {
  const key = route.meta.titleKey
  if (key) document.title = `拾光 · ${t(key)}`
})

// 审查 2.39：启动时刷新用户态（封禁/降权/改昵称后不显示旧身份）；refresh 失败会自动 logout
onMounted(() => {
  if (user.token) user.refresh()
})
</script>

<template>
  <el-config-provider :locale="epLocale">
  <DesktopTitleBar />

  <AppShell v-if="!route.meta.public">
    <router-view v-slot="{ Component }">
      <transition name="page" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </AppShell>

  <main v-else class="app-main" :class="{ 'safe-top': route.name !== 'welcome' }">
    <router-view v-slot="{ Component }">
      <transition name="page" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </main>
  </el-config-provider>
</template>

<style scoped>
.app-main { min-height: 100vh; }

.page-enter-active, .page-leave-active { transition: opacity .22s ease, transform .22s ease; }
.page-enter-from { opacity: 0; transform: translateY(10px); }
.page-leave-to { opacity: 0; transform: translateY(-6px); }
</style>
