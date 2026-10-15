<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

/**
 * 以图搜图入口卡：首页 C 位入口，贴纸质感 + 虚线上传区。
 * 点击/拖入图片后跳转 /search-image，真正的上传与匹配在该页完成。
 */
const router = useRouter()
const dragging = ref(false)

function go() {
  router.push('/search-image')
}
</script>

<template>
  <div class="search-card paper-card" :class="{ dragging }" @click="go"
    @dragover.prevent="dragging = true" @dragleave="dragging = false"
    @drop.prevent="dragging = false; go()">
    <div class="inner">
      <div class="cam">📷</div>
      <p class="t1">一张照片，全校帮你找</p>
      <p class="t2">上传物品照片，感知哈希秒级匹配失物招领启事</p>
      <span class="try-btn">拖入图片 或 点击开始搜图 →</span>
    </div>
    <span class="sticker corner">AI 以图搜图 · 核心功能</span>
  </div>
</template>

<style scoped>
.search-card {
  position: relative;
  padding: 14px;
  transform: rotate(2deg);
  transition: transform .25s cubic-bezier(.34, 1.56, .64, 1), box-shadow .25s;
  cursor: pointer;
}
.search-card:hover { transform: rotate(0) scale(1.02); box-shadow: var(--shadow-lift); }
.search-card.dragging { transform: rotate(0) scale(1.03); border-color: var(--orange); }

.inner {
  border: 2.5px dashed var(--orange);
  border-radius: 16px;
  background:
    radial-gradient(420px 220px at 70% 0%, rgba(255, 201, 60, .25), transparent 60%),
    var(--honey-soft);
  padding: 40px 30px 34px;
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  text-align: center;
}
.cam {
  font-size: 52px;
  animation: camBob 3.2s ease-in-out infinite;
}
@keyframes camBob {
  0%, 100% { transform: rotate(-5deg) scale(1); }
  50% { transform: rotate(4deg) scale(1.08); }
}
.t1 { font-family: var(--font-display); font-size: 23px; color: var(--ink); }
.t2 { font-size: 13px; color: var(--ink-soft); max-width: 260px; line-height: 1.6; }
.try-btn {
  margin-top: 6px;
  padding: 9px 20px; border-radius: 999px;
  background: var(--orange); color: #FFF7EC;
  font-size: 13.5px; font-weight: 700;
  box-shadow: 0 6px 16px rgba(255, 125, 46, .4);
  transition: transform .16s;
}
.search-card:hover .try-btn { transform: scale(1.05); }

.sticker.corner { position: absolute; top: -12px; right: 14px; }
@media (prefers-reduced-motion: reduce) { .cam { animation: none; } }
</style>
