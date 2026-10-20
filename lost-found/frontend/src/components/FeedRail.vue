<script setup>
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'
import { catDot } from '../constants/catalog'
import { catLabel } from '../i18n'
import { relTime } from '../utils/display'
import { useUserStore } from '../stores/user'

/**
 * 右栏（Digg 三栏骨架）：sticky 固定 + 独立滚动，主内容上下滚动时右栏不动。
 * 四块内容全部接真实接口，不编造数字：
 *   ① 以图搜图推广卡（pHash 指标来自实验报告）
 *   ② 找回战报（/api/stats）
 *   ③ 热门启事（/items?sort=hot，按浏览量）
 *   ④ 为你推荐（收藏品类 → 同类最新启事，未登录/无收藏不渲染）
 */
const route = useRoute()
const user = useUserStore()

const stats = ref({ reunited: 0, lostCount: 0, foundCount: 0 })
const hot = ref([])
const reco = ref([])
let timer = null
let lastLoad = 0

async function loadCore() {
  lastLoad = Date.now()
  try {
    const [s, h] = await Promise.all([
      api.stats(),
      api.listItems({ sort: 'hot', page: 1, size: 5 }),
    ])
    stats.value = s || stats.value
    hot.value = h?.records || []
  } catch (e) { /* 右栏失败不阻断主内容 */ }
}

async function loadReco() {
  if (!user.info) { reco.value = []; return }
  try {
    const favs = await api.favorites({ page: 1, size: 30 })
    const cats = [...new Set((favs.records || []).map((i) => i.category).filter(Boolean))].slice(0, 2)
    if (!cats.length) { reco.value = []; return }
    const lists = await Promise.all(
      cats.map((c) => api.listItems({ category: c, sort: 'new', page: 1, size: 3 }).catch(() => ({ records: [] }))),
    )
    const seen = new Set()
    const out = []
    lists.forEach((res, idx) => {
      for (const it of res.records || []) {
        if (seen.has(it.id)) continue
        seen.add(it.id)
      }
    })
    reco.value = out.slice(0, 3)
  } catch (e) { reco.value = [] }
}

onMounted(() => {
  loadCore()
  loadReco()
  timer = setInterval(() => { loadCore() }, 120000)   // 2 分钟刷新一次榜单
  window.addEventListener('shiguang-fav-changed', loadReco)
  window.addEventListener('shiguang-items-changed', loadCore)
})
onBeforeUnmount(() => {
  clearInterval(timer)
  window.removeEventListener('shiguang-fav-changed', loadReco)
  window.removeEventListener('shiguang-items-changed', loadCore)
})
watch(() => route.fullPath, () => {
  if (Date.now() - lastLoad < 8000) return
  loadCore()
  loadReco()
})

const reunited = computed(() => stats.value.reunited || 0)
const activeTotal = computed(() => (stats.value.lostCount || 0) + (stats.value.foundCount || 0))
</script>

<template>
  <aside class="rail" :aria-label="$t('a11y.rail')">
    <!-- ① 以图搜图推广卡 -->
    <section class="promo">
      <p class="kicker"><i></i><span>{{ $t('rail.promo.kicker') }}</span></p>
      <h3 v-html="$t('rail.promo.title')"></h3>
      <p class="promo-desc">{{ $t('rail.promo.desc') }}</p>
      <RouterLink to="/search-image" class="promo-go">
        {{ $t('rail.promo.go') }}
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><path d="M5 12h14m-6-7 7 7-7 7" /></svg>
      </RouterLink>
      <span class="promo-fp" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="6" width="18" height="14" rx="4" /><circle cx="12" cy="13" r="3.5" /><path d="M14 6l-2-2h-2L8 6" /></svg>
      </span>
    </section>

    <!-- ② 找回战报 -->
    <section class="panel">
      <h4>{{ $t('rail.stats.title') }}</h4>
      <p class="big"><b>{{ reunited }}</b><span>{{ $t('rail.stats.big') }}</span></p>
      <div class="stat-row">
        <div><b>{{ stats.lostCount || 0 }}</b><span>{{ $t('rail.stats.lost') }}</span></div>
        <div><b>{{ stats.foundCount || 0 }}</b><span>{{ $t('rail.stats.found') }}</span></div>
      </div>
      <p class="panel-note">{{ $t('rail.stats.note', { n: activeTotal }) }}</p>
    </section>

    <!-- ③ 热门启事 -->
    <section class="panel">
      <h4>{{ $t('rail.hot.title') }}</h4>
      <RouterLink v-for="it in hot" :key="'h' + it.id" :to="`/item/${it.id}`" class="trend">
        <p class="trend-tag"><i class="dot" :style="{ background: catDot(it.category) }"></i>{{ catLabel(it.category) }}</p>
        <b>{{ it.title }}</b>
        <span>{{ $t('rail.hot.meta', { n: it.views ?? 0, time: relTime(it.time) }) }}</span>
      </RouterLink>
      <p v-if="!hot.length" class="panel-empty">{{ $t('rail.hot.empty') }}</p>
    </section>

    <!-- ④ 为你推荐（规则版，保留既有逻辑） -->
    <section v-if="reco.length" class="panel">
      <h4>{{ $t('rail.reco.title') }}</h4>
      <RouterLink v-for="r in reco" :key="'r' + r.item.id" :to="`/item/${r.item.id}`" class="trend">
        <p class="trend-tag"><i class="dot" :style="{ background: catDot(r.item.category) }"></i>{{ $t('rail.reco.reason', { cat: catLabel(r.cat) }) }}</p>
        <b>{{ r.item.title }}</b>
        <span>{{ $t('rail.meta', { loc: r.item.location, time: relTime(r.item.time) }) }}</span>
      </RouterLink>
    </section>
  </aside>
</template>

<style scoped>
.rail {
  position: sticky; top: 0;
  width: 300px;   /* 外壳折叠时保持内容宽度不变，由 rail-clip 负责裁剪 */
  height: 100vh;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 26px 26px 60px 6px;
  display: flex; flex-direction: column; gap: 20px;
  scrollbar-width: thin;
  scrollbar-color: var(--thumb) transparent;
}
.rail::-webkit-scrollbar { width: 6px; }
.rail::-webkit-scrollbar-thumb { background: var(--thumb); border-radius: 3px; }
.rail::-webkit-scrollbar-track { background: transparent; }

/* 推广卡：近黑底反白，右栏唯一深色块 */
.promo {
  position: relative; overflow: hidden; flex: none;
  background: var(--promo-bg); color: #F5F5F2;
  border-radius: var(--radius);
  padding: 20px 20px 22px;
}
.kicker {
  display: flex; align-items: center; gap: 8px;
  font-size: 10px; font-weight: 700; letter-spacing: .2em; color: #8F8F86;
  margin-bottom: 12px;
}
.kicker i { width: 6px; height: 6px; border-radius: 50%; background: #3ECF8E; }
.promo h3 { font-size: 17px; line-height: 1.4; letter-spacing: -.01em; margin-bottom: 8px; }
.promo-desc { font-size: 12.5px; line-height: 1.7; color: #B9B9B0; margin-bottom: 16px; }
.promo-go {
  position: relative; z-index: 1;
  display: inline-flex; align-items: center; gap: 7px;
  background: #F5F5F2; color: #151513;
  font-size: 12.5px; font-weight: 700;
  padding: 10px 16px; border-radius: 999px;
  transition: transform .18s ease, box-shadow .18s ease;
}
.promo-go:hover { transform: translateY(-1px); box-shadow: 0 8px 18px rgba(0, 0, 0, .35); }
.promo-go svg { width: 13px; height: 13px; }
.promo-fp {
  position: absolute; right: -20px; bottom: -20px;
  width: 104px; height: 104px; border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, #2B2B27, var(--promo-bg));
  border: 1px solid #33332E;
  display: grid; place-items: center;
  color: #4E4E47;
  pointer-events: none;
}
.promo-fp svg { width: 34px; height: 34px; }

/* 面板 */
.panel {
  flex: none;
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 16px 16px 8px;
}
.panel h4 { font-size: 13.5px; font-weight: 800; letter-spacing: -.005em; margin-bottom: 10px; }

.big { display: flex; align-items: baseline; gap: 8px; padding: 4px 0 12px; }
.big b {
  font-family: var(--font-display); font-size: 34px; font-weight: 800;
  letter-spacing: -.03em; line-height: 1; font-variant-numeric: tabular-nums;
}
.big span { font-size: 12px; color: var(--ink-soft); }
.stat-row { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; padding-bottom: 12px; }
.stat-row div {
  background: var(--cream); border-radius: 12px; padding: 10px 12px;
  display: flex; flex-direction: column; gap: 4px;
}
.stat-row b { font-size: 16px; font-weight: 800; font-variant-numeric: tabular-nums; }
.stat-row span { font-size: 11px; color: var(--ink-faint); }
.panel-note { font-size: 10.5px; line-height: 1.6; color: var(--ink-faint); padding-bottom: 10px; }

.trend {
  display: block; padding: 11px 2px;
  border-top: 1px solid var(--line);
}
.trend:first-of-type { border-top: 0; }
.trend-tag {
  display: flex; align-items: center; gap: 6px;
  font-size: 10.5px; font-weight: 600; color: var(--ink-faint);
  margin-bottom: 6px;
}
.trend b {
  display: block; font-size: 12.8px; font-weight: 700; line-height: 1.5;
  color: var(--ink);
}
.trend:hover b { text-decoration: underline; text-underline-offset: 2px; }
.trend > span { display: block; margin-top: 5px; font-size: 11px; color: var(--ink-faint); }
.panel-empty { font-size: 12px; color: var(--ink-faint); padding: 6px 0 12px; }

@media (max-width: 1080px) {
  .rail { display: none; }
}
</style>
