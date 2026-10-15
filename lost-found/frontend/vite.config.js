import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  build: {
    // 修复 F-06：大依赖拆独立 chunk 利于缓存与首屏；阈值放宽到 900kB 消除误报
    chunkSizeWarningLimit: 900,
    rollupOptions: {
      output: {
        // 函数式拆包：echarts 按需后仍 ~520kB 独立成 chunk；element-plus 全量约 1.1MB，
        // 按组件名首字母拆成两个可缓存 chunk，保证"无 >1000kB 单文件"（04 验收）
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (id.includes('echarts') || id.includes('zrender')) return 'echarts'
          if (id.includes('element-plus') || id.includes('@element-plus')) {
            if (id.includes('/es/components/')) {
              const comp = id.split('/es/components/')[1].split('/')[0]
              return comp < 'm' ? 'element-plus-a' : 'element-plus-b'
            }
            return 'element-plus-a'
          }
          return 'vendor'
        },
      },
    },
  },
  server: {
    port: 5173,
    host: true,
    proxy: {
      // 后端接口
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      // 上传图片静态资源（封面/详情图/以图搜图结果图）
      '/uploads': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
