import { defineConfig, mergeConfig } from 'vite'
import baseConfig from './vite.config'

/**
 * 桌面端构建配置（desktop/prepare-renderer.js 使用）：
 * 在基础配置上只改两处——base 相对化（file:// 必须）+ 禁用 manualChunks
 * （element-plus 拆 a/b 两个 chunk 在 file:// 的脚本执行顺序下触发循环依赖：
 *  "Cannot access 'x' before initialization"；桌面单 vendor 大 chunk 无此问题）。
 *
 * 注：vite.config.js 已改为回调式（按 mode 决定是否注入 PWA，F-12），
 * 这里先用同样的 ctx 求值成对象再 mergeConfig——mergeConfig 不接受函数。
 */
export default defineConfig(({ mode, command, isSsrBuild, isPreview }) => {
  const base = typeof baseConfig === 'function'
    ? baseConfig({ mode, command, isSsrBuild, isPreview })
    : baseConfig
  return mergeConfig(base, {
    build: {
      rollupOptions: {
        output: {
          manualChunks: undefined,
        },
      },
    },
  })
})
