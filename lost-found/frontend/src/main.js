import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { Capacitor } from '@capacitor/core'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { reveal } from './directives/reveal'
import { hydrateTokenStorage } from './utils/token-storage'
import './styles/main.css'

// 修复 F-12 迁移：1.0.0 版 APK 在 WebView 里注册过 PWA Service Worker，
// 覆盖安装后 SW 仍会回放旧缓存（旧 index.html + 已删除的哈希资源）→ 依旧白屏。
// 原生端冷启动时清一次注册与缓存（幂等；Web/PWA 完全不受影响）。
;(function purgeLegacyServiceWorker() {
  if (!Capacitor.isNativePlatform() || !('serviceWorker' in navigator)) return
  navigator.serviceWorker.getRegistrations()
    .then((regs) => Promise.all(regs.map((r) => r.unregister())))
    .then(() => (window.caches ? caches.keys() : []))
    .then((keys) => Promise.all((keys || []).map((k) => caches.delete(k))))
    .catch(() => { /* 清理失败不阻断启动 */ })
})()

// M-P1：Capacitor 原生端先水合登录态（Preferences→镜像）再挂载，避免闪登录页
hydrateTokenStorage().finally(() => {
  const app = createApp(App)

  app.use(createPinia())
  app.use(router)
  app.use(ElementPlus, { locale: zhCn })
  app.directive('reveal', reveal)

  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }

  app.mount('#app')

  // 桌面端深链接：lostfound://item/<id> → 拉起应用并跳到对应启事（Electron 壳经 preload 转发）
  if (window.shiguangDesktop?.onOpenItem) {
    window.shiguangDesktop.onOpenItem((itemId) => {
      if (itemId) router.push(`/item/${itemId}`)
    })
  }
})
