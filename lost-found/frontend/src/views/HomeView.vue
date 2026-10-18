<script setup>
import { ref, onMounted } from 'vue'
import SplitText from '../components/SplitText.vue'
import ItemCard from '../components/ItemCard.vue'
import { CATEGORIES } from '../constants/catalog'
import { api } from '../api'
import { assetUrl } from '../utils/asset'

/**
 * 首页（G 版式：非对称编辑 Hero + 计数统计 + 特稿跨列卡）。
 * 左：SplitText 逐字标题 / CTA / rAF 数字计数；右：最新招领"发现现场"卡 + 悬浮签。
 * 最新寻物首卡在桌面端跨两列放大为特稿卡（编辑层级）。
 */
const latestLost = ref([])
const latestFound = ref([])
const stats = ref({ reunited: 0, lostCount: 0, foundCount: 0 })
const error = ref('')
const scene = ref(null)   // 右侧发现现场卡 = 最新一条招领启事

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
    scene.value = found.records?.[0] || null
    runCounters()
  } catch (e) {
    error.value = e.message || '加载首页数据失败'
  }
}

/* rAF 数字计数（数据到位后缓动跑满；reduced-motion 直接显示终值） */
function runCounters() {
  if (matchMedia('(prefers-reduced-motion: reduce)').matches) return
  const t0 = performance.now(), dur = 1400
  const to = { reunited: stats.value.reunited || 0, lostCount: stats.value.lostCount || 0, foundCount: stats.value.foundCount || 0 }
  const tick = (t) => {
    const p = Math.min((t - t0) / dur, 1)
    const ease = 1 - Math.pow(1 - p, 3)
    for (const k of Object.keys(to)) {
      const el = document.getElementById('stat-' + k)
      if (el) el.textContent = Math.round(to[k] * ease)
    }
    if (p < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

onMounted(load)
</script>

<template>
  <div class="page home">
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="container err">
      <el-button size="small" round type="primary" @click="load">重试</el-button>
    </el-alert>

    <!-- ① G 版式 Hero：左编辑式大标题 / 右发现现场卡 -->
    <section class="hero container">
      <div class="hero-left">
        <p class="kicker"><i></i>校园失物招领平台 · LOST &amp; FOUND</p>
        <h1 class="hero-title">
          <SplitText text="丢了东西？" :stagger="60" /><br />
          <SplitText text="一张照片帮你找回" :stagger="50" :delay="350" />
        </h1>
        <p class="hero-sub">
          上传物品照片，感知哈希算法（pHash）为它生成 64 位「长相指纹」，与全校失物与招领启事秒级比对——
          不只是关键词搜索，而是「认得长相」的搜索。
        </p>
        <div class="hero-cta">
          <el-button type="primary" size="large" round @click="$router.push('/search-image')">
            上传照片，搜一搜
          </el-button>
          <el-button size="large" round plain @click="$router.push('/wall?type=lost')">
            浏览失物墙
          </el-button>
        </div>
        <div class="hero-stats">
          <div class="num"><b><span id="stat-reunited">{{ stats.reunited }}</span></b><span>件失物已找回</span></div>
          <div class="num"><b><span id="stat-lostCount">{{ stats.lostCount }}</span></b><span>条寻物启事</span></div>
          <div class="num"><b><span id="stat-foundCount">{{ stats.foundCount }}</span></b><span>条招领启事</span></div>
        </div>
      </div>

      <!-- 右侧：发现现场（最新一条招领启事） -->
      <RouterLink v-if="scene" :to="`/item/${scene.id}`" class="scene">
        <div class="scene-note"><b>{{ scene.views ?? 0 }}</b> 次浏览 · 最新招领</div>
        <div class="scene-card">
          <div class="scene-ph">
            <img v-if="scene.thumb || scene.img" :src="assetUrl(scene.thumb || scene.img)" :alt="scene.title" />
          </div>
          <div class="scene-cap">
            <b>{{ scene.title }}</b>
            <span>{{ scene.location }} · {{ (scene.time || '').slice(5, 16) }}</span>
          </div>
        </div>
        <div class="match-chip">
          <span class="pct">pHash</span>
          <span><b>已加入指纹库</b><small>以图搜图可直接命中这条招领</small></span>
        </div>
      </RouterLink>
    </section>

    <!-- ② 分类快捷入口 -->
    <section class="container cats">
      <span
        v-for="c in CATEGORIES" :key="c.name"
        class="chip"
        @click="$router.push({ path: '/wall', query: { cat: c.name } })"
      >{{ c.emoji }} {{ c.name }}</span>
    </section>

    <!-- ③ 最新寻物（桌面端首卡跨两列特稿） -->
    <section class="container">
      <div class="section-head">
        <h2>最新<em>寻物</em>启事</h2>
        <span class="more" @click="$router.push('/wall?type=lost')">查看全部 →</span>
      </div>
      <div class="grid-cards feat-grid">
        <ItemCard v-for="(it, i) in latestLost" :key="it.id" :item="it" :featured="i === 0" />
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
  display: grid; grid-template-columns: 1.12fr .88fr; gap: 64px; align-items: center;
  padding: 56px 0 40px; min-height: 420px;
}
.kicker {
  display: flex; align-items: center; gap: 12px;
  font-size: 12.5px; font-weight: 700; letter-spacing: 3px; color: var(--ink-faint);
  margin-bottom: 24px;
}
.kicker i { width: 30px; height: 2px; background: var(--orange); }
.hero-title { font-size: clamp(38px, 4.8vw, 60px); line-height: 1.18; letter-spacing: -1px; }
.hero-sub { margin: 24px 0 32px; max-width: 480px; color: var(--ink-soft); font-size: 15.5px; line-height: 1.95; }
.hero-cta { display: flex; gap: 13px; margin-bottom: 44px; flex-wrap: wrap; }
.hero-stats { display: flex; gap: 48px; }
.hero-stats .num b { font-size: 34px; font-weight: 800; letter-spacing: -1px; font-variant-numeric: tabular-nums; }
.hero-stats .num span { display: block; font-size: 12.5px; color: var(--ink-faint); margin-top: 5px; letter-spacing: 1px; }

/* 右侧发现现场卡 */
.scene { position: relative; display: block; }
.scene-note {
  position: absolute; right: -14px; top: -18px; z-index: 2;
  background: var(--paper); border-radius: 12px; padding: 11px 15px;
  box-shadow: 0 14px 34px rgba(27, 26, 23, .14);
  font-size: 12px; color: var(--ink-soft); font-weight: 600;
}
.scene-note b { color: var(--orange-deep); font-variant-numeric: tabular-nums; }
.scene-card {
  background: var(--paper); border-radius: 20px; padding: 13px;
  border: 1px solid var(--line);
  box-shadow: 0 1px 2px rgba(27, 26, 23, .05), 0 26px 60px rgba(27, 26, 23, .11);
}
.scene-ph { border-radius: 12px; aspect-ratio: 4 / 3.2; background: var(--cream-deep); overflow: hidden; }
.scene-ph img { width: 100%; height: 100%; object-fit: cover; transition: transform .5s cubic-bezier(.22, 1, .36, 1); }
.scene:hover .scene-ph img { transform: scale(1.05); }
.scene-cap { display: flex; justify-content: space-between; align-items: baseline; gap: 12px; padding: 13px 6px 4px; }
.scene-cap b { font-size: 14.5px; }
.scene-cap span { font-size: 11.5px; color: var(--ink-faint); font-variant-numeric: tabular-nums; white-space: nowrap; }
.match-chip {
  position: absolute; left: -26px; bottom: 44px;
  background: var(--ink); color: #fff; border-radius: 14px;
  padding: 13px 17px; display: flex; align-items: center; gap: 12px;
  box-shadow: 0 18px 40px rgba(27, 26, 23, .28);
}
.match-chip .pct { font-size: 12px; font-weight: 800; letter-spacing: 1px; color: #7CE8C8; }
.match-chip b { font-size: 13px; display: block; }
.match-chip small { font-size: 11.5px; color: #B8B2A6; line-height: 1.6; display: block; }

.cats { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 44px; }

/* 特稿卡：桌面端首卡跨两列 + 横排（编辑层级） */
@media (min-width: 1021px) {
  .feat-grid { grid-template-columns: repeat(3, 1fr); }
  .feat-grid :deep(.item-card.featured) { grid-column: span 2; display: grid; grid-template-columns: 1.1fr .9fr; }
  .feat-grid :deep(.item-card.featured .cover) { aspect-ratio: auto; height: 100%; min-height: 232px; }
  .feat-grid :deep(.item-card.featured .body) { padding: 22px 24px; justify-content: center; gap: 9px; }
  .feat-grid :deep(.item-card.featured .title) { -webkit-line-clamp: 2; font-size: 17px; }
}

@media (max-width: 900px) {
  .hero { grid-template-columns: 1fr; gap: 52px; padding-top: 36px; }
  .match-chip { left: 6px; }
  .scene-note { right: 6px; }
}
</style>
