<script setup>
import { ref, onMounted } from 'vue'
import SplitText from '../components/SplitText.vue'
import SearchUploadCard from '../components/SearchUploadCard.vue'
import ItemCard from '../components/ItemCard.vue'
import { CATEGORIES } from '../constants/catalog'
import { api } from '../api'

const latestLost = ref([])
const latestFound = ref([])
const stats = ref({ reunited: 0, lostCount: 0, foundCount: 0 })
const error = ref('')

async function load() {
  error.value = ''
  try {
    const [s, lost, found] = await Promise.all([
      api.stats(),
      api.listItems({ type: 'lost', sort: 'new', page: 1, size: 4 }),
      api.listItems({ type: 'found', sort: 'new', page: 1, size: 4 }),
    ])
    stats.value = s
    latestLost.value = lost.records
    latestFound.value = found.records
  } catch (e) {
    error.value = e.message || '加载首页数据失败'
  }
}

onMounted(load)
</script>

<template>
  <div class="page home">
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="container err">
      <el-button size="small" round type="primary" @click="load">重试</el-button>
    </el-alert>

    <!-- ① 海报区：左海报 + 右以图搜图入口卡 -->
    <section class="hero container">
      <div class="hero-left">
        <span class="hero-badge">🧡 软件工程课程设计 · LOST &amp; FOUND</span>
        <h1 class="hero-title">
          <SplitText text="丢了东西？" :stagger="60" /><br />
          <SplitText text="一张照片帮你找回" :stagger="50" :delay="350" class="hero-accent" />
        </h1>
        <p class="hero-sub">
          上传物品照片，感知哈希算法（pHash）秒级匹配全校失物与招领启事——
          不只是关键词搜索，而是「认得长相」的搜索。
        </p>
        <div class="hero-cta">
          <el-button type="primary" size="large" round @click="$router.push('/search-image')">
            📷 试试以图搜图
          </el-button>
          <el-button size="large" round plain @click="$router.push('/wall?type=lost')">
            浏览失物墙
          </el-button>
        </div>
        <div class="hero-stats">
          <div><b>{{ stats.reunited }}</b><span>件失物已找回</span></div>
          <i></i>
          <div><b>{{ stats.lostCount }}</b><span>条寻物启事</span></div>
          <i></i>
          <div><b>{{ stats.foundCount }}</b><span>条招领启事</span></div>
        </div>
      </div>

      <div class="hero-right">
        <SearchUploadCard />
        <span class="float-emoji" style="top:-26px;left:-30px;font-size:40px;--rot:-10deg">🎒</span>
        <span class="float-emoji" style="bottom:-18px;left:-38px;font-size:34px;--rot:8deg;animation-delay:.8s">🔑</span>
        <span class="float-emoji" style="top:-20px;right:-26px;font-size:36px;--rot:12deg;animation-delay:1.6s">🎧</span>
      </div>
    </section>

    <!-- ② 分类快捷入口 -->
    <section class="container cats">
      <span
        v-for="c in CATEGORIES" :key="c.name"
        class="chip"
        @click="$router.push({ path: '/wall', query: { cat: c.name } })"
      >{{ c.emoji }} {{ c.name }}</span>
    </section>

    <!-- ③ 最新寻物 -->
    <section class="container">
      <div class="section-head">
        <h2>最新<em>寻物</em>启事</h2>
        <span class="more" @click="$router.push('/wall?type=lost')">查看全部 →</span>
      </div>
      <div class="grid-cards">
        <ItemCard v-for="it in latestLost" :key="it.id" :item="it" />
      </div>
    </section>

    <!-- ④ 最新招领 -->
    <section class="container">
      <div class="section-head">
        <h2>最新<em>招领</em>启事</h2>
        <span class="more" @click="$router.push('/wall?type=found')">查看全部 →</span>
      </div>
      <div class="grid-cards">
        <ItemCard v-for="it in latestFound" :key="it.id" :item="it" />
      </div>
    </section>
  </div>
</template>

<style scoped>
.hero {
  display: grid; grid-template-columns: 1.15fr .85fr; gap: 48px; align-items: center;
  padding-top: 46px; min-height: 430px;
}
.hero-badge {
  display: inline-block; margin-bottom: 20px;
  padding: 6px 16px; border-radius: 999px;
  background: var(--paper); border: 1.5px solid var(--line);
  color: var(--ink-soft); font-size: 13px; letter-spacing: 1px;
  box-shadow: var(--shadow-card);
}
.hero-title { font-size: clamp(38px, 4.6vw, 56px); line-height: 1.28; }
/* 渐变文字要作用在每个字符上：父级 background-clip:text 会被子字符的
   transform/filter 动画合成层破坏，导致整行透明不可见。
   split-char 在 SplitText 子组件内部，需 :deep() 穿透 scoped 样式 */
.hero-accent :deep(.split-char) {
  background: linear-gradient(100deg, var(--orange), var(--orange-deep) 55%, #E88A1F);
  -webkit-background-clip: text; background-clip: text; color: transparent;
}
.hero-sub {
  margin: 20px 0 26px; max-width: 480px;
  color: var(--ink-soft); font-size: 15.5px; line-height: 1.9;
}
.hero-cta { display: flex; gap: 14px; margin-bottom: 34px; }
.hero-stats { display: flex; align-items: center; gap: 22px; }
.hero-stats div { display: flex; flex-direction: column; }
.hero-stats b { font-size: 26px; font-family: var(--font-display); color: var(--orange-deep); }
.hero-stats span { font-size: 12.5px; color: var(--ink-faint); }
.hero-stats i { width: 1px; height: 34px; background: var(--line); }

.hero-right { position: relative; }

.cats { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 46px; }

@media (max-width: 900px) {
  .hero { grid-template-columns: 1fr; gap: 60px; }
}
</style>
