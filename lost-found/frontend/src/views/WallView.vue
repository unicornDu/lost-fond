<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ItemCard from '../components/ItemCard.vue'
import { CATEGORIES, LOCATIONS } from '../mock/data'
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

function onPage(p) { page.value = p; load() }

let kwTimer = null
watch(keyword, () => { clearTimeout(kwTimer); kwTimer = setTimeout(() => { page.value = 1; load() }, 300) })
watch([type, category, location, sort], () => { page.value = 1; load() })

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

    <!-- 工具栏 -->
    <div class="toolbar paper-card">
      <el-input v-model="keyword" placeholder="搜索标题 / 描述关键词" clearable class="kw">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="location" placeholder="地点" clearable class="loc">
        <el-option v-for="l in LOCATIONS" :key="l" :label="l" :value="l" />
      </el-select>
      <el-select v-model="sort" class="sort">
        <el-option label="最新发布" value="new" />
        <el-option label="最多浏览" value="hot" />
      </el-select>
    </div>

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

.toolbar {
  display: flex; gap: 12px; padding: 12px;
  margin-bottom: 16px; align-items: center;
}
.toolbar .kw { flex: 1; }
.toolbar .loc { width: 150px; }
.toolbar .sort { width: 130px; }

.cats { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 18px; }
.count { color: var(--ink-faint); font-size: 13px; margin-bottom: 14px; }
.count b { color: var(--orange-deep); }
.pager { margin-top: 26px; justify-content: center; }
</style>
