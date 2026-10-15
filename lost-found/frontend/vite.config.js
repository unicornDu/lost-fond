import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
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
