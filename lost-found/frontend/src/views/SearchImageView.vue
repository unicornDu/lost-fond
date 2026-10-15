<script setup>
import { ref, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

/**
 * 以图搜图（核心亮点页）：上传照片 → POST /api/search/image（multipart）
 * 后端用 JImageHash 计算查询图 pHash，与库内指纹逐条算汉明距离，阈值过滤后返回 Top-N。
 * 前端保留「读取图片→提取哈希→汉明匹配」的阶段动画提升观感，与真实请求并行。
 */
const fileUrl = ref('')
const fileName = ref('')
const fileRaw = ref(null)
const step = ref(0)            // 0 未开始 1-3 处理中 4 完成
const results = ref([])
const totalImgs = ref(0)
const elapsed = ref(0)

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

function onFileChange(file) {
  if (!file.raw.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {   // 审查 2.27③ 前端体积校验
    ElMessage.error('图片不能超过 5MB')
    return
  }
  if (fileUrl.value) URL.revokeObjectURL(fileUrl.value) // 审查 2.31② 释放旧 blob
  fileUrl.value = URL.createObjectURL(file.raw)
  fileName.value = file.raw.name
  fileRaw.value = file.raw
  runPipeline()
}

async function runPipeline() {
  step.value = 1; results.value = []; elapsed.value = 0
  const fd = new FormData()
  fd.append('file', fileRaw.value)
  const t0 = performance.now()
  const req = api.searchByImage(fd)
  const cntReq = api.indexedCount().catch(() => ({ count: 0 }))

  await sleep(450); step.value = 2
  await sleep(550); step.value = 3
  try {
    const [res, cnt] = await Promise.all([req, cntReq])
    totalImgs.value = cnt.count || 0
    elapsed.value = ((performance.now() - t0) / 1000).toFixed(2)
    await sleep(250)
    results.value = res || []
    step.value = 4
  } catch (e) {
    ElMessage.error(e.message || '以图搜图失败')
    reset()
  }
}

function reset() {
  if (fileUrl.value) URL.revokeObjectURL(fileUrl.value)
  fileUrl.value = ''; fileName.value = ''; fileRaw.value = null
  step.value = 0; results.value = []
}

onBeforeUnmount(() => { if (fileUrl.value) URL.revokeObjectURL(fileUrl.value) })

const steps = [
  { n: 1, label: '读取图片', detail: '压缩至 32×32 灰度图' },
  { n: 2, label: '提取感知哈希', detail: 'DCT 变换 → 64 位 pHash 指纹' },
  { n: 3, label: '汉明距离匹配', detail: '与库内启事逐条比对，阈值 ≤ 10' },
]
</script>

<template>
  <div class="page container">
    <h1 class="page-title">📷 以图搜图</h1>
    <p class="page-sub">不上传文字，直接上传物品照片——用「长相」找回你的东西（感知哈希 pHash · 汉明距离匹配）</p>

    <div class="layout">
      <!-- 左：上传与流程 -->
      <div class="left">
        <div v-if="!fileUrl" class="upload-zone paper-card">
          <el-upload drag accept="image/*" :auto-upload="false" :show-file-list="false" :on-change="onFileChange">
            <div class="up-inner">
              <span class="cam">🔍</span>
              <p class="up-t">拖一张照片到这里，或 <em>点击上传</em></p>
              <p class="up-d">支持 JPG / PNG，拍清物品特征效果更好</p>
            </div>
          </el-upload>
        </div>

        <div v-else class="preview paper-card">
          <img :src="fileUrl" alt="查询图片" />
          <p class="fname">🖼️ {{ fileName }}</p>
          <el-button round plain size="small" @click="reset">← 重新上传</el-button>
        </div>

        <!-- 匹配流程 -->
        <div v-if="step > 0 && step < 4" class="pipeline paper-card">
          <div v-for="s in steps" :key="s.n" class="p-step" :class="{ doing: step === s.n, done: step > s.n }">
            <span class="dot">
              <el-icon v-if="step > s.n"><Check /></el-icon>
              <el-icon v-else-if="step === s.n" class="loading"><Loading /></el-icon>
              <i v-else>{{ s.n }}</i>
            </span>
            <div>
              <b>{{ s.label }}</b>
              <small>{{ s.detail }}</small>
            </div>
          </div>
        </div>
      </div>

      <!-- 右：结果 -->
      <div class="right">
        <template v-if="step === 4 && results.length">
          <div class="res-head">
            <h3>匹配到 <em>{{ results.length }}</em> 条可能相关</h3>
            <span class="res-sub">已在 {{ totalImgs }} 张启事图片中完成比对 · 耗时 {{ elapsed }}s</span>
          </div>

          <div v-for="(r, i) in results" :key="r.id" class="res-item paper-card" :class="{ top: i === 0 }">
            <span v-if="i === 0" class="top-badge">TOP1 · 疑似同一物品</span>
            <img :src="r.img" alt="" @error="$event.target.style.opacity = 0" />
            <div class="res-info">
              <RouterLink :to="`/item/${r.id}`" class="rt">{{ r.title }}</RouterLink>
              <p class="rm">📍 {{ r.location }} · ⏰ {{ r.time.slice(5, 16) }}</p>
              <div class="bar"><i :style="{ width: r.similarity + '%' }"></i></div>
            </div>
            <b class="sim">{{ r.similarity }}%</b>
          </div>
        </template>

        <div v-else-if="step === 4 && !results.length" class="res-empty paper-card">
          <span class="cam">🤔</span>
          <p>没有匹配到相似的启事</p>
          <small>库内暂无与该照片足够相似的物品（汉明距离均 &gt; 阈值 10）。<br />试试拍得更清晰、更接近物品主体，或直接去发布一条启事。</small>
        </div>

        <div v-else-if="step > 0" class="res-empty paper-card">
          <el-icon class="spin"><Loading /></el-icon>
          <p>正在匹配中…</p>
        </div>

        <div v-else class="res-empty paper-card">
          <span class="cam">🧡</span>
          <p>上传照片后，匹配结果会显示在这里</p>
          <small>算法：pHash 感知哈希 · 汉明距离 ≤ 10 判定为相似（阈值经实验标定）</small>
        </div>
      </div>
    </div>

    <div class="demo-tip" style="max-width:1180px;margin:26px auto 0">
      ✅ 已接入真实后端：本页调用 <b>POST /api/search/image</b>，由 Spring Boot 端 JImageHash
      计算查询图 64 位 pHash，与库内 <b>item_image.phash</b> 逐条做 <b>BIT_COUNT(a XOR b)</b>
      汉明距离比对，阈值过滤后按相似度降序返回 Top-N。
    </div>
  </div>
</template>

<style scoped>
.layout { display: grid; grid-template-columns: .9fr 1.1fr; gap: 30px; align-items: start; }

.upload-zone :deep(.el-upload-dragger) {
  border: 2.5px dashed var(--orange);
  border-radius: 18px;
  background: radial-gradient(420px 200px at 70% 0%, rgba(255, 201, 60, .22), transparent 60%), var(--honey-soft);
  padding: 46px 30px;
}
.up-inner { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.up-inner .cam { font-size: 52px; }
.up-t { font-size: 16px; font-weight: 600; }
.up-t em { color: var(--orange-deep); font-style: normal; }
.up-d { color: var(--ink-faint); font-size: 12.5px; }

.preview { padding: 18px; display: flex; flex-direction: column; gap: 12px; align-items: center; }
.preview img {
  width: 100%; max-height: 320px; object-fit: contain; border-radius: 14px;
  background: var(--cream-deep);
}
.fname { color: var(--ink-soft); font-size: 13px; }

.pipeline { margin-top: 18px; padding: 20px 24px; display: flex; flex-direction: column; gap: 16px; }
.p-step { display: flex; gap: 14px; align-items: center; opacity: .38; transition: opacity .3s; }
.p-step.doing, .p-step.done { opacity: 1; }
.p-step .dot {
  display: grid; place-items: center;
  width: 30px; height: 30px; border-radius: 50%;
  border: 1.5px solid var(--line); font-size: 13px; flex-shrink: 0;
}
.p-step.done .dot { background: var(--mint); border-color: var(--mint); color: #fff; }
.p-step.doing .dot { border-color: var(--orange); color: var(--orange-deep); }
.p-step b { font-size: 14px; display: block; }
.p-step small { color: var(--ink-faint); font-size: 12px; }
.loading { animation: spin 1s linear infinite; }
.spin { animation: spin 1.4s linear infinite; font-size: 34px; color: var(--orange); }
@keyframes spin { to { transform: rotate(360deg); } }

.res-head { margin-bottom: 14px; }
.res-head h3 { font-size: 21px; }
.res-head h3 em { color: var(--orange); font-style: normal; }
.res-sub { color: var(--ink-faint); font-size: 12.5px; }

.res-item {
  position: relative;
  display: flex; gap: 14px; align-items: center;
  padding: 12px; margin-bottom: 14px;
  transition: transform .18s;
}
.res-item:hover { transform: translateX(4px); }
.res-item.top { border: 2px solid var(--orange); box-shadow: 0 14px 30px rgba(255, 125, 46, .22); }
.top-badge {
  position: absolute; top: -11px; left: 14px;
  padding: 3px 12px; border-radius: 999px;
  background: var(--orange); color: #FFF7EC; font-size: 11.5px; font-weight: 700;
  box-shadow: 0 4px 10px rgba(255, 125, 46, .4);
}
.res-item img {
  width: 92px; height: 72px; object-fit: cover; border-radius: 10px;
  background: var(--cream-deep); flex-shrink: 0;
}
.res-info { flex: 1; min-width: 0; }
.rt { font-weight: 600; font-size: 14.5px; display: block; margin-bottom: 4px; }
.rt:hover { color: var(--orange-deep); }
.rm { color: var(--ink-faint); font-size: 12px; margin-bottom: 7px; }
.bar { height: 7px; border-radius: 99px; background: var(--cream-deep); overflow: hidden; }
.bar i {
  display: block; height: 100%; border-radius: 99px;
  background: linear-gradient(90deg, var(--honey), var(--orange));
  transition: width .8s cubic-bezier(.22, 1, .36, 1);
}
.sim { font-family: var(--font-display); font-size: 22px; color: var(--orange-deep); width: 74px; text-align: right; }

.res-empty {
  padding: 54px 30px; text-align: center;
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  color: var(--ink-soft);
}
.res-empty .cam { font-size: 46px; }
.res-empty small { color: var(--ink-faint); font-size: 12px; line-height: 1.7; }

@media (max-width: 900px) { .layout { grid-template-columns: 1fr; } }
</style>
