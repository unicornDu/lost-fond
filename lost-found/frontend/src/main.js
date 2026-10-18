import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import { Search, Close, Promotion, Plus, Check, Loading } from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { reveal } from './directives/reveal'
import './styles/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.directive('reveal', reveal)

// 2026-10-18 审查修复：只注册模板实际用到的 6 个图标。
// 原实现遍历注册全部图标（ElementPlusIconsVue），把整套图标都打进首包。
for (const [key, component] of Object.entries({ Search, Close, Promotion, Plus, Check, Loading })) {
  app.component(key, component)
}

app.mount('#app')
