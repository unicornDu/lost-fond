<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import FeedCard from '../components/FeedCard.vue'
import ItemCard from '../components/ItemCard.vue'
import { api } from '../api'
import { t } from '../i18n'

/**
 * 首页 = 信息流（Digg 版式）：
 * 顶部工具条（排序 / 类型 / 列表-网格切换）+ 单列启事流，去掉旧版 Hero、计数墙与特稿卡。
 * 数据：寻物 12 条 + 招领 12 条合并后按发生时间排序，前端筛选/排序不再请求；
 * 「为你推荐」已移到右栏（FeedRail），首页只保留主信息流。
 */
const router = useRouter()

const all = ref([])
const stats = ref({ reunited: 0, lostCount: 0, foundCount: 0 })
const error = ref('')
const loading = ref(true)

const sortBy = ref('new')      // new | hot
const typeFilter = ref('all')  // all | lost | found
const view = ref('list')       // list | grid
const shown = ref(6)

async function load() {
  error.value = ''
  loading.value = true
  try {
    const [s, lost, found] = await Promise.all([
      api.stats(),
      api.listItems({ type: 'lost', sort: 'new', page: 1, size: 12 }),
      api.listItems({ type: 'found', sort: 'new', page: 1, size: 12 }),
    ])
    stats.value = s || stats.value
    all.value = [...(lost.records || []), ...(found.records || [])]
      .sort((a, b) => String(b.time || '').localeCompare(String(a.time || '')))
  } catch (e) {
    error.value = e.message || t('home.error')
  } finally {
    loading.value = false
  }
}

const list = computed(() => {
  let out = all.value
  if (typeFilter.value !== 'all') out = out.filter((i) => i.type === typeFilter.value)
  if (sortBy.value === 'hot') out = [...out].sort((a, b) => (b.views || 0) - (a.views || 0))
  return out
})
const visible = computed(() => list.value.slice(0, shown.value))
const hasMore = computed(() => shown.value < list.value.length)

function pickSort(v) { sortBy.value = v; shown.value = 6 }
function pickType(v) { typeFilter.value = v; shown.value = 6 }

const activeCount = computed(() => (stats.value.lostCount || 0) + (stats.value.foundCount || 0))

onMounted(load)
</script>

<template>
  <div class="feed">
    <h1 class="sr-only">{{ $t('home.sr') }}</h1>

    <!-- 信息流抬头（不是 Hero，只做定位与统计） -->
    <header class="mast">
      <div>
        <p class="eyebrow">{{ $t('home.eyebrow') }}</p>
        <h2 class="mast-title">{{ $t('home.title') }}</h2>
        <p class="mast-sub">{{ $t('home.sub') }}</p>
      </div>
      <div class="mast-stats">
        <div><b>{{ stats.reunited || 0 }}</b><span>{{ $t('home.stat.reunited') }}</span></div>
        <div><b>{{ activeCount }}</b><span>{{ $t('home.stat.active') }}</span></div>
      </div>
    </header>

    <!-- 工具条：排序 + 类型 + 视图切换 -->
    <div class="feed-head">
      <button class="seg" :class="{ on: sortBy === 'new' }" @click="pickSort('new')">{{ $t('home.sort.new') }}</button>
      <button class="seg" :class="{ on: sortBy === 'hot' }" @click="pickSort('hot')">{{ $t('home.sort.hot') }}</button>
      <span class="divider"></span>
      <button class="seg mut soft" :class="{ on: typeFilter === 'all' }" @click="pickType('all')">{{ $t('home.type.all') }}</button>
      <button class="seg mut soft" :class="{ on: typeFilter === 'lost' }" @click="pickType('lost')">{{ $t('home.type.lost') }}</button>
      <button class="seg mut soft" :class="{ on: typeFilter === 'found' }" @click="pickType('found')">{{ $t('home.type.found') }}</button>
      <span class="grow"></span>
      <button class="icon-btn" :class="{ on: view === 'list' }" :title="$t('home.view.list')" @click="view = 'list'">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01" /></svg>
      </button>
      <button class="icon-btn" :class="{ on: view === 'grid' }" :title="$t('home.view.grid')" @click="view = 'grid'">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="3" width="7" height="7" rx="1.5" /><rect x="3" y="14" width="7" height="7" rx="1.5" /><rect x="14" y="14" width="7" height="7" rx="1.5" /></svg>
      </button>
    </div>

    <!-- 错误态 -->
    <div v-if="error" class="err">
      <p>{{ error }}</p>
      <button class="seg" @click="load">{{ $t('common.retry') }}</button>
    </div>

    <!-- 骨架屏 -->
    <template v-else-if="loading">
      <div v-for="n in 3" :key="'sk' + n" class="skeleton">
        <span class="sk-line w40"></span>
        <span class="sk-line w85"></span>
        <span class="sk-line w60"></span>
        <span class="sk-photo"></span>
      </div>
    </template>

    <template v-else>
      <!-- 列表视图 -->
      <div v-if="view === 'list'" class="stream">
        <FeedCard v-for="(it, i) in visible" :key="it.id" :item="it" v-reveal="{ delay: Math.min(i, 5) * 60 }" />
      </div>

      <!-- 网格视图 -->
      <div v-else class="grid-cards">
        <ItemCard v-for="(it, i) in visible" :key="it.id" :item="it" v-reveal="{ delay: Math.min(i, 5) * 50 }" />
      </div>

      <div v-if="!visible.length" class="empty">
        <p>{{ $t('home.empty') }}</p>
        <button class="seg" @click="router.push('/publish')">{{ $t('home.emptyCta') }}</button>
      </div>

      <div v-if="hasMore" class="more-wrap">
        <button class="more-btn" @click="shown += 6">{{ $t('home.more') }}</button>
      </div>
      <p v-else-if="visible.length" class="stream-end">{{ $t('home.end', { n: list.length }) }}</p>
    </template>
  </div>
</template>

<style scoped>
.feed { max-width: 760px; }

.mast {
  display: flex; align-items: flex-end; justify-content: space-between; gap: 20px;
  padding: 2px 0 22px;
  flex-wrap: wrap;
}
.eyebrow {
  font-size: 10.5px; font-weight: 700; letter-spacing: .18em;
  color: var(--ink-faint); margin-bottom: 10px;
}
.mast-title { font-size: 27px; line-height: 1.2; letter-spacing: -.02em; }
.mast-sub { margin-top: 8px; font-size: 13px; color: var(--ink-soft); }
.mast-stats { display: flex; gap: 24px; }
.mast-stats div { display: flex; flex-direction: column; align-items: flex-end; gap: 3px; }
.mast-stats b { font-family: var(--font-display); font-size: 22px; font-weight: 800; letter-spacing: -.02em; font-variant-numeric: tabular-nums; }
.mast-stats span { font-size: 11px; color: var(--ink-faint); }

.feed-head {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  padding-bottom: 20px;
}
.feed-head .divider { width: 1px; height: 20px; background: var(--line); margin: 0 4px; }
.feed-head .grow { flex: 1; }

.stream { display: block; }
.err {
  display: flex; align-items: center; justify-content: space-between; gap: 14px;
  padding: 16px 18px; margin-bottom: 20px;
  border: 1px solid var(--line); border-radius: var(--radius-sm);
  background: var(--paper); font-size: 13px; color: var(--ink-soft);
}

/* 骨架屏 */
.skeleton {
  background: var(--paper); border: 1px solid var(--line); border-radius: var(--radius);
  padding: 20px 22px; margin-bottom: 26px;
}
.sk-line { display: block; height: 11px; border-radius: 6px; background: var(--cream-deep); margin-bottom: 10px; }
.sk-line.w40 { width: 40%; height: 9px; }
.sk-line.w85 { width: 85%; height: 17px; }
.sk-line.w60 { width: 60%; }
.sk-photo { display: block; height: 190px; border-radius: var(--radius-sm); background: var(--cream-deep); margin-top: 14px; }
.skeleton { animation: skPulse 1.6s ease-in-out infinite; }
@keyframes skPulse { 0%, 100% { opacity: 1; } 50% { opacity: .62; } }

.empty {
  display: flex; flex-direction: column; align-items: center; gap: 14px;
  padding: 60px 0; color: var(--ink-faint); font-size: 13px;
}
.more-wrap { display: flex; justify-content: center; padding: 4px 0 20px; }
.more-btn {
  border: 1px solid var(--line); background: var(--paper);
  padding: 11px 26px; border-radius: 999px;
  font-family: inherit; font-size: 13px; font-weight: 700; color: var(--ink);
  cursor: pointer; transition: background .16s, border-color .16s;
}
.more-btn:hover { background: var(--cream-deep); border-color: var(--line-2); }
.stream-end { text-align: center; font-size: 11.5px; color: var(--ink-faint); padding: 10px 0 26px; }

@media (max-width: 560px) {
  .mast-title { font-size: 23px; }
  .mast-stats { display: none; }
  .feed-head .divider { display: none; }
  .feed-head .icon-btn { display: none; }
}
</style>
