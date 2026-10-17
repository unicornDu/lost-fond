import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  build: {
    // 修复 F-06：大依赖拆独立 chunk 利于缓存与首屏；阈值放宽到 900kB 消除误报
    chunkSizeWarningLimit: 900,
    rollupOptions: {
      output: {
        // 修复 F-11（生产构建白屏）：element-plus 曾按组件首字母拆成 a/b 两个 chunk，
        // 组件之间存在双向依赖（如 select→input、date-picker→time-picker/scrollbar），
        // 跨 chunk 循环在浏览器里触发 TDZ："Cannot access 'xx' before initialization"，
        // 模块顶层抛错 → Vue 未挂载 → 整站只剩 index.html 的米色背景。
        // dev 模式不拆包所以看不出。现在 element-plus 整体归一 chunk（约 960kB，仍满足
        // "无 >1000kB 单文件"），echarts 保持独立按需加载，其余依赖进 vendor。
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
})
