<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ItemCard from '../components/ItemCard.vue'
import GradientSearchBar from '../components/GradientSearchBar.vue'
import { CATEGORIES } from '../constants/catalog'
import { api } from '../api'

const route = useRoute()
const router = useRouter()

const type = ref(route.query.type === 'found' ? 'found' : 'lost')
const keyword = ref('')
const category = ref(route.query.cat || '')
const location = ref('')
const sort = ref('new')

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
  } catch (e) {
    if (seq !== reqSeq) return
    list.value = []
    error.value = e.message || '加载失败'
  } finally {
    if (seq === reqSeq) loading.value = false
  }
}

// 修复 F-11：翻页后回到页顶，避免用户停在页尾以为没翻页
function onPage(p) {
  page.value = p
  load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

let kwTimer = null
watch(keyword, () => { clearTimeout(kwTimer); kwTimer = setTimeout(() => { page.value = 1; load() }, 300) })
watch([type, category, location, sort], () => { page.value = 1; load() })

// 渐变搜索栏的立即搜索（回车/点按钮）：跳过 300ms 防抖直接查
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
</script>

<template>
  <div class="page container">
    <h1 class="page-title">{{ type === 'lost' ? '🙋 失物墙' : '🎁 招领墙' }}</h1>
    <p class="page-sub">
      {{ type === 'lost' ? '同学们发布的寻物启事，看看有没有你捡到的东西' : '好心人捡到的物品都在这里，快看看有没有你的' }}
    </p>

    <!-- 类型切换大按钮 -->
    <div class="type-switch">
      <button :class="{ on: type === 'lost' }" @click="switchType('lost')">🙋 我丢了东西</button>
      <button :class="{ on: type === 'found', mint: true }" @click="switchType('found')">🎁 我捡到东西</button>
    </div>

    <!-- 搜索栏（2026-10-17 重设计：渐变光晕胶囊搜索条，见 Prompt/前端prompt/搜索框.md） -->
    <GradientSearchBar
      v-model:keyword="keyword"
      v-model:location="location"
      v-model:sort="sort"
      @search="onSearchNow"
    />

    <!-- 分类 chips -->
    <div class="cats">
      <span class="chip" :class="{ on: category === '' }" @click="category = ''">全部</span>
      <span
        v-for="c in CATEGORIES" :key="c.name"
        class="chip" :class="{ on: category === c.name }"
        @click="category = category === c.name ? '' : c.name"
      >{{ c.emoji }} {{ c.name }}</span>
    </div>

    <p class="count">共 <b>{{ total }}</b> 条启事</p>

    <!-- 错误态 + 重试（审查 2.31） -->
    <el-empty v-if="error" :description="error">
      <el-button type="primary" round @click="load">重试</el-button>
    </el-empty>

    <template v-else>
      <div v-if="list.length" class="grid-cards" v-loading="loading">
        <ItemCard v-for="it in list" :key="it.id" :item="it" />
      </div>
      <el-empty v-else description="没有匹配的启事，换个条件试试～">
        <el-button type="primary" round @click="router.push('/publish')">发布一条启事</el-button>
      </el-empty>

      <!-- 分页（审查 2.25） -->
      <el-pagination v-if="total > size" class="pager" layout="prev, pager, next, total"
        :total="total" :page-size="size" v-model:current-page="page" @current-change="onPage" />
    </template>
  </div>
</template>

<style scoped>
.type-switch { display: flex; gap: 14px; margin: 8px 0 20px; }
.type-switch button {
  padding: 13px 30px; border-radius: 16px;
  border: 1.5px solid var(--line); background: var(--paper);
  font-size: 15px; color: var(--ink-soft);
  cursor: pointer; transition: all .2s ease;
  font-family: var(--font-body);
}
.type-switch button:hover { transform: translateY(-2px); border-color: var(--orange); }
.type-switch button.on {
  background: linear-gradient(120deg, var(--orange), var(--orange-deep));
  border-color: transparent; color: #FFF7EC; font-weight: 700;
  box-shadow: 0 10px 22px rgba(255, 125, 46, .35);
  transform: rotate(-1deg);
}
.type-switch button.on.mint {
  background: linear-gradient(120deg, #4CC092, var(--mint));
  box-shadow: 0 10px 22px rgba(63, 174, 131, .35);
  transform: rotate(1deg);
}

.cats { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 18px; }
.count { color: var(--ink-faint); font-size: 13px; margin-bottom: 14px; }
.count b { color: var(--orange-deep); }
.pager { margin-top: 26px; justify-content: center; }
</style>
