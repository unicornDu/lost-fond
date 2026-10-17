import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig(({ mode }) => ({
  plugins: [
    vue(),
    // M-P2 iOS 方案 C（PWA 保底）：manifest + autoUpdate SW；
    // iPhone Safari「分享 → 添加到主屏幕」即得全屏 App 图标，无需签名不过期
    //
    // 修复 F-12：mobile 模式（Capacitor APK）**不注入 PWA/SW**——WebView 里的资源本来
    // 就在 APK 内，Service Worker 只会在升级安装后继续吐旧缓存，造成"更新了还是白屏"。
    mode !== 'mobile' && VitePWA({
      registerType: 'autoUpdate',
      manifest: {
        name: '拾光 · 校园失物招领平台',
        short_name: '失物招领',
        description: '校园失物招领平台：以图搜图、认领审核、站内通知',
        display: 'standalone',
        theme_color: '#FF7D2E',
        background_color: '#FAF4E9',
        icons: [
          { src: 'pwa-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'pwa-512.png', sizes: '512x512', type: 'image/png', purpose: 'any' },
          { src: 'pwa-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
      },
    }),
  ],
  build: {
    // 修复 F-06：大依赖拆独立 chunk 利于缓存与首屏；阈值放宽到 900kB 消除误报
    chunkSizeWarningLimit: 900,
    rollupOptions: {
      output: {
        // 函数式拆包：echarts 按需后仍 ~520kB 独立成 chunk；element-plus 全量约 1.1MB，
        // 按组件名首字母拆成两个可缓存 chunk，保证"无 >1000kB 单文件"（04 验收）
        // 修复 F-11（生产构建白屏）：element-plus 曾按组件首字母拆成 a/b 两个 chunk，
        // 组件之间存在双向依赖（如 select→input、date-picker→time-picker/scrollbar），
        // 跨 chunk 循环在浏览器里触发 TDZ："Cannot access 'xx' before initialization"，
        // 模块顶层抛错 → Vue 未挂载 → 整站只剩 index.html 的米色背景。
        // dev 模式不拆包所以看不出，桌面端早已用 manualChunks:undefined 绕过（见 vite.desktop.config.js）。
        // 现在 element-plus 整体归一 chunk（约 960kB，仍满足"无 >1000kB 单文件"），
        // echarts 保持独立按需加载，其余依赖进 vendor。
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (id.includes('echarts') || id.includes('zrender')) return 'echarts'
          if (id.includes('element-plus') || id.includes('@element-plus')) return 'element-plus'
          return 'vendor'
        },
      },
    },
  },
  server: {
    port: 5173,
    host: true,
    proxy: {
      // 后端接口（BACKEND_PORT 可覆盖，默认 8080。
      // 注：本机 TCP 动态端口范围从 1024 起，8080 偶尔会被出站连接随机占为源端口——
      // 此时后端用 --server.port=8081 启动，前端用 BACKEND_PORT=8081 npm run dev）
      '/api': { target: `http://localhost:${process.env.BACKEND_PORT || 8080}`, changeOrigin: true },
      // 上传图片静态资源（封面/详情图/以图搜图结果图）
      '/uploads': { target: `http://localhost:${process.env.BACKEND_PORT || 8080}`, changeOrigin: true },
    },
  },
  // vitest：路由守卫用例首次懒加载 AuthView 时 jsdom 要编译整包 Element Plus，
  // 本机 5.5s 左右会撞上默认 5s 超时（偶发红）；放宽到 20s，避免"假失败"。
  test: {
    environment: 'jsdom',
    testTimeout: 20000,
  },
}))
