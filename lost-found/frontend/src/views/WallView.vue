<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ItemCard from '../components/ItemCard.vue'
import FeedCard from '../components/FeedCard.vue'
import GradientSearchBar from '../components/GradientSearchBar.vue'
import { CATEGORIES } from '../constants/catalog'
import { api } from '../api'
import { catLabel, t } from '../i18n'

/**
 * 失物墙 / 招领墙（Digg 网格视图）：
 * 顶部工具条切换墙别与排序、右侧列表-网格切换；搜索条与分类胶囊保留原功能。
 * 列表视图复用首页 FeedCard，网格视图用 ItemCard（1.45:1 封面）。
 */
const route = useRoute()
const router = useRouter()

const type = ref(route.query.type === 'found' ? 'found' : 'lost')
const keyword = ref('')
const category = ref(route.query.cat || '')
const location = ref('')
const sort = ref('new')
const view = ref('grid')

// ---- E-07：搜索历史（纯前端，localStorage，上限 10 条、最近优先去重） ----
const HIST_KEY = 'shiguang_search_history'
const history = ref(readHistory())
const searchFocused = ref(false)
const showHistory = computed(() => searchFocused.value && !keyword.value.trim() && history.value.length > 0)

function readHistory() {
  try { return JSON.parse(localStorage.getItem(HIST_KEY) || '[]') } catch { return [] }
}
function persistHistory() { localStorage.setItem(HIST_KEY, JSON.stringify(history.value)) }
function saveHistory(k) {
  const kw = k.trim()
  if (!kw) return
  history.value = [kw, ...history.value.filter((h) => h !== kw)].slice(0, 10)
  persistHistory()
}
function removeHistory(k) { history.value = history.value.filter((h) => h !== k); persistHistory() }
function clearHistory() { history.value = []; localStorage.removeItem(HIST_KEY) }
function useHistory(k) { keyword.value = k; onSearchNow() }

const list = ref([])
const loading = ref(false)
const error = ref('')
const page = ref(1)
const size = ref(12)
const total = ref(0)
let reqSeq = 0

async function load() {
  const seq = ++reqSeq
  loading.value = true
  error.value = ''
  try {
    const res = await api.listItems({
      type: type.value,
      category: category.value || undefined,
      location: location.value || undefined,
      keyword: keyword.value.trim() || undefined,
      sort: sort.value,
      page: page.value,
      size: size.value,
    })
    if (seq !== reqSeq) return // 竞态守卫
    list.value = res.records
    total.value = res.total
    if (keyword.value.trim()) saveHistory(keyword.value)   // E-07：搜索成功才写入历史
  } catch (e) {
    if (seq !== reqSeq) return
    list.value = []
    error.value = e.message || t('wall.error')
  } finally {
    if (seq === reqSeq) loading.value = false
  }
}

function onPage(p) {
  page.value = p
  load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

let kwTimer = null
watch(keyword, () => { clearTimeout(kwTimer); kwTimer = setTimeout(() => { page.value = 1; load() }, 300) })
watch([type, category, location, sort], () => { page.value = 1; load() })

function onSearchNow() {
  clearTimeout(kwTimer)
  page.value = 1
  load()
}

watch(() => route.query, (q) => {
  if (q.type) type.value = q.type
  category.value = q.cat || ''
})

onMounted(load)

function switchType(t) {
  type.value = t
  router.replace({ query: { ...route.query, type: t } })
}

const title = computed(() => (type.value === 'lost' ? t('wall.lost') : t('wall.found')))
const sub = computed(() => (type.value === 'lost' ? t('wall.lostSub') : t('wall.foundSub')))
</script>

<template>
  <div class="wall">
    <header class="wall-head">
      <div>
        <p class="eyebrow">{{ $t('wall.eyebrow') }}</p>
        <h1 class="wall-title">{{ title }}</h1>
        <p class="wall-sub">{{ sub }}</p>
      </div>
      <p class="wall-count"><span v-html="$t('wall.count', { n: '<b>' + total + '</b>' })"></span></p>
    </header>

    <!-- 工具条 -->
    <div class="toolbar">
      <button class="seg" :class="{ on: type === 'lost' }" @click="switchType('lost')">{{ $t('wall.lost') }}</button>
      <button class="seg" :class="{ on: type === 'found' }" @click="switchType('found')">{{ $t('wall.found') }}</button>
      <span class="divider"></span>
      <button class="seg mut soft" :class="{ on: sort === 'new' }" @click="sort = 'new'">{{ $t('wall.sort.new') }}</button>
      <button class="seg mut soft" :class="{ on: sort === 'hot' }" @click="sort = 'hot'">{{ $t('wall.sort.hot') }}</button>
      <span class="grow"></span>
      <button class="icon-btn" :class="{ on: view === 'list' }" :title="$t('home.view.list')" @click="view = 'list'">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01" /></svg>
      </button>
      <button class="icon-btn" :class="{ on: view === 'grid' }" :title="$t('home.view.grid')" @click="view = 'grid'">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="3" width="7" height="7" rx="1.5" /><rect x="3" y="14" width="7" height="7" rx="1.5" /><rect x="14" y="14" width="7" height="7" rx="1.5" /></svg>
      </button>
    </div>

    <!-- 搜索条 + 历史下拉 -->
    <div class="search-area">
      <GradientSearchBar
        v-model:keyword="keyword"
        v-model:location="location"
        @search="onSearchNow"
        @focus="searchFocused = true"
        @blur="searchFocused = false"
      />
      <div v-if="showHistory" class="history-drop">
        <div class="hist-head">
          <span>{{ $t('wall.history') }}</span>
          <a @click.prevent="clearHistory">{{ $t('wall.history.clear') }}</a>
        </div>
        <div class="hist-list">
          <span v-for="h in history" :key="h" class="hist-item" @mousedown.prevent="useHistory(h)">
            {{ h }}
            <i class="hist-x" @mousedown.prevent.stop="removeHistory(h)">✕</i>
          </span>
        </div>
      </div>
    </div>

    <!-- 分类胶囊 -->
    <div class="cats">
      <span class="chip" :class="{ on: category === '' }" @click="category = ''">{{ $t('common.all') }}</span>
      <span
        v-for="c in CATEGORIES" :key="c.name"
        class="chip" :class="{ on: category === c.name }"
        @click="category = category === c.name ? '' : c.name"
      >{{ catLabel(c.name) }}</span>
    </div>

    <!-- 错误态 -->
    <div v-if="error" class="err">
      <p>{{ error }}</p>
      <button class="seg" @click="load">{{ $t('common.retry') }}</button>
    </div>

    <template v-else>
      <div v-if="list.length && view === 'grid'" class="grid-cards" v-loading="loading">
        <ItemCard v-for="(it, i) in list" :key="it.id" :item="it" v-reveal="{ delay: Math.min(i, 6) * 45 }" />
      </div>
      <div v-else-if="list.length" class="stream" v-loading="loading">
        <FeedCard v-for="(it, i) in list" :key="it.id" :item="it" v-reveal="{ delay: Math.min(i, 6) * 45 }" />
      </div>
      <div v-else-if="!loading" class="empty">
        <p>{{ $t('wall.empty') }}</p>
        <button class="seg" @click="router.push('/publish')">{{ $t('wall.emptyCta') }}</button>
      </div>

      <el-pagination v-if="total > size" class="pager" layout="prev, pager, next, total"
        :total="total" :page-size="size" v-model:current-page="page" @current-change="onPage" />
    </template>
  </div>
</template>

<style scoped>
.wall { max-width: 860px; }

.wall-head {
  display: flex; align-items: flex-end; justify-content: space-between; gap: 18px;
  padding: 2px 0 20px; flex-wrap: wrap;
}
.eyebrow { font-size: 10.5px; font-weight: 700; letter-spacing: .18em; color: var(--ink-faint); margin-bottom: 10px; }
.wall-title { font-size: 27px; line-height: 1.2; letter-spacing: -.02em; }
.wall-sub { margin-top: 8px; font-size: 13px; color: var(--ink-soft); }
.wall-count { font-size: 12px; color: var(--ink-faint); font-variant-numeric: tabular-nums; }
.wall-count b { color: var(--ink); font-weight: 800; }

.toolbar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
.toolbar .divider { width: 1px; height: 20px; background: var(--line); margin: 0 4px; }
.toolbar .grow { flex: 1; }

.search-area { position: relative; margin-bottom: 14px; }
.history-drop {
  position: absolute; z-index: 30; left: 10px; right: 10px; top: calc(100% + 6px);
  padding: 12px 16px;
  background: var(--paper); border: 1px solid var(--line); border-radius: var(--radius-sm);
  box-shadow: var(--shadow-lift);
}
.hist-head { display: flex; justify-content: space-between; font-size: 12px; color: var(--ink-faint); margin-bottom: 8px; }
.hist-head a { cursor: pointer; }
.hist-head a:hover { color: var(--ink); }
.hist-list { display: flex; flex-wrap: wrap; gap: 8px; }
.hist-item {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 5px 10px; border-radius: 999px;
  background: var(--cream); font-size: 12.5px; cursor: pointer;
}
.hist-item:hover { background: var(--cream-deep); color: var(--ink); }
.hist-x { font-style: normal; font-size: 10px; color: var(--ink-faint); padding: 0 2px; }
.hist-x:hover { color: var(--rose); }

.cats { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 20px; }

.stream { display: block; }
.err {
  display: flex; align-items: center; justify-content: space-between; gap: 14px;
  padding: 16px 18px; margin-bottom: 20px;
  border: 1px solid var(--line); border-radius: var(--radius-sm);
  background: var(--paper); font-size: 13px; color: var(--ink-soft);
}
.empty {
  display: flex; flex-direction: column; align-items: center; gap: 14px;
  padding: 64px 0; color: var(--ink-faint); font-size: 13px;
}
.pager { margin-top: 26px; justify-content: center; }

@media (max-width: 560px) {
  .toolbar .icon-btn, .toolbar .divider { display: none; }
}
</style>
