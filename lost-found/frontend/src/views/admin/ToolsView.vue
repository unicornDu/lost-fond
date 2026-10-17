<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api'
import { openOrDownload } from '../../utils/delivery'

/**
 * 修复 F-10：管理员工具入口——重算缺失图片指纹 / 补生成缩略图。
 * 两个接口此前在 api/index.js 定义却全项目无人调用（docs/01 FR6.4 要求）。
 * 后端行为（02 批 B-08 后）：reindex 补算 phash 为 null 的图；regen-thumbs 只写 uploads/yyyy/MM。
 * E-08B：新增启事数据 CSV 导出（blob 下载，带鉴权头）。
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

// P1：报表生成 → delivery 卡片 → 统一受控下载/原生打开（不再有独立下载端点）
async function createReport() {
  busy.value = 'report'
  try {
    const d = await api.createReport()
    ElMessage.success('报表已生成，开始下载')
    await openOrDownload({ ...d, fileName: d.title + '.csv' })
    lastResult.value = `报表已生成：${d.title}（${(d.fileSize / 1024).toFixed(1)} KB）`
  } catch (e) {
    ElMessage.error(e.message || '生成失败')
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
      <div class="tool">
        <b>导出启事数据报表</b>
        <p>生成 item 全量 CSV（含状态/审核/发布人，公式注入转义），交付到消息中心卡片，走统一受控下载。</p>
        <el-button type="primary" plain :loading="busy === 'report'" @click="createReport">生成报表</el-button>
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
