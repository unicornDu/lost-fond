<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api'

/**
 * 修复 F-10：管理员工具入口——重算缺失图片指纹 / 补生成缩略图。
 * 两个接口此前在 api/index.js 定义却全项目无人调用（docs/01 FR6.4 要求）。
 * 后端行为（02 批 B-08 后）：reindex 补算 phash 为 null 的图；regen-thumbs 只写 uploads/yyyy/MM。
 */
const busy = ref('')
const lastResult = ref('')

async function run(key, fn, label) {
  try {
    await ElMessageBox.confirm(`确定执行「${label}」？该操作会扫描上传目录，可能耗时数秒。`, '运维工具', { type: 'warning' })
  } catch { return }   // 用户取消
  busy.value = key
  try {
    const d = await fn()
    lastResult.value = `${label} → ${JSON.stringify(d)}`
    ElMessage.success(`${label} 完成：${Object.entries(d).map(([k, v]) => `${k}=${v}`).join('，')}`)
  } catch (e) {
    ElMessage.error(e.message || `${label} 失败`)
  } finally {
    busy.value = ''
  }
}
</script>

<template>
  <div class="paper-card wrap">
    <h2>🧰 运维工具</h2>
    <p class="tip">仅管理员可用；操作会写入操作日志。</p>
    <div class="tools">
      <div class="tool">
        <b>重算缺失图片指纹</b>
        <p>为 item_image 中 phash 为空的图片补算 pHash（发布时算失败的兜底）。</p>
        <el-button type="primary" plain :loading="busy === 'reindex'"
          @click="run('reindex', () => api.adminReindex(), '重算指纹')">执行</el-button>
      </div>
      <div class="tool">
        <b>补生成缩略图</b>
        <p>为 uploads/yyyy/MM 下缺 `_thumb` 的原图补生成 480px 缩略图（不写 seed 目录）。</p>
        <el-button type="primary" plain :loading="busy === 'thumbs'"
          @click="run('thumbs', () => api.adminRegenThumbs(), '补缩略图')">执行</el-button>
      </div>
    </div>
    <p v-if="lastResult" class="last">上次结果：{{ lastResult }}</p>
  </div>
</template>

<style scoped>
.wrap { padding: 22px; }
.wrap h2 { font-size: 18px; margin-bottom: 6px; }
.tip { color: var(--ink-soft); font-size: 13px; margin-bottom: 16px; }
.tools { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 14px; }
.tool {
  border: 1.5px dashed var(--line); border-radius: 14px; padding: 16px;
  display: flex; flex-direction: column; gap: 8px; align-items: flex-start;
}
.tool b { font-size: 15px; }
.tool p { color: var(--ink-soft); font-size: 13px; line-height: 1.6; flex: 1; }
.last { margin-top: 14px; font-size: 12.5px; color: var(--ink-faint); word-break: break-all; }
</style>
