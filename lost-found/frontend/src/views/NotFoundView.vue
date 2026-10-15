<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template>
  <div class="nf">
    <span class="code">404</span>
    <p class="t">页面走丢了…</p>
    <p class="d">你要找的页面不存在或已被移除。也许它正等着被人「以图搜图」找回来？</p>
    <div class="acts">
      <el-button type="primary" round @click="router.push('/home')">返回首页</el-button>
      <el-button round plain @click="router.push('/search-image')">去以图搜图</el-button>
    </div>
  </div>
</template>

<style scoped>
.nf {
  min-height: calc(100vh - 150px);
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 10px; text-align: center; padding: 40px 20px;
}
.code {
  font-family: var(--font-display); font-size: 96px; line-height: 1;
  background: linear-gradient(120deg, var(--orange), var(--orange-deep));
  -webkit-background-clip: text; background-clip: text; color: transparent;
}
.t { font-size: 22px; font-weight: 700; }
.d { color: var(--ink-soft); font-size: 14px; max-width: 420px; line-height: 1.8; }
.acts { margin-top: 14px; display: flex; gap: 12px; }
</style>
