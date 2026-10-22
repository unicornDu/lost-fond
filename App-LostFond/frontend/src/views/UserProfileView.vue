<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { api } from '../api'
import { t } from '../i18n'
import ItemCard from '../components/ItemCard.vue'

/**
 * E-04：他人公开主页 /user/:id。头像/昵称/加入时间 + TA 的公开启事（复用 ItemCard 网格）。
 * 后端已脱敏（无学号、封禁用户 404）；查看自己时提供"去个人中心"入口。
 */
const route = useRoute()
const router = useRouter()
const user = useUserStore()

const profile = ref(null)
const items = ref([])
const total = ref(0)
const page = ref(1)
const size = 12
const loading = ref(false)
const failed = ref('')

const isSelf = computed(() => profile.value && user.info?.id === profile.value.id)

async function load() {
  const id = route.params.id
  loading.value = true
  failed.value = ''
  try {
    const [p, list] = await Promise.all([
      api.userProfile(id),
      api.userItems(id, { page: page.value, size }),
    ])
    profile.value = p
    items.value = list.records
    total.value = list.total
  } catch (e) {
    profile.value = null
    items.value = []
    failed.value = e.message || t('user.loadFail')
  } finally {
    loading.value = false
  }
}

function changePage(p) { page.value = p; load() }
watch(() => route.params.id, () => { page.value = 1; load() })
onMounted(load)
</script>

<template>
  <div class="page container" v-loading="loading">
    <el-empty v-if="failed" :description="failed">
      <el-button round plain @click="router.back()">{{ $t('user.back') }}</el-button>
    </el-empty>

    <template v-else-if="profile">
      <el-button round plain size="small" @click="router.back()">{{ $t('user.back') }}</el-button>

      <div class="owner paper-card">
        <span class="avatar">{{ profile.avatar || '🦊' }}</span>
        <div class="who">
          <h1>{{ profile.nickname || $t('me.student') }}</h1>
          <p>🗓️ {{ (profile.createTime || '').slice(0, 10) }} {{ $t('user.join') }} · 📦 {{ profile.itemCount }} {{ $t('user.notices') }}</p>
        </div>
        <el-button v-if="isSelf" round plain type="primary" @click="router.push('/profile')">{{ $t('user.self') }}</el-button>
      </div>

      <div class="section-head">
        <h2>{{ $t('user.title') }}</h2>
        <span class="hint">{{ $t('user.hint') }}</span>
      </div>
      <div v-if="items.length" class="grid-cards">
        <ItemCard v-for="it in items" :key="it.id" :item="it" />
      </div>
      <el-empty v-else :description="$t('user.empty')" />
      <el-pagination v-if="total > size" class="pager" layout="prev, pager, next, total"
        :total="total" :page-size="size" :current-page="page" @current-change="changePage" />
    </template>
  </div>
</template>

<style scoped>
.owner { display: flex; align-items: center; gap: 18px; padding: 26px 30px; margin: 16px 0 8px; }
.avatar {
  display: grid; place-items: center; width: 68px; height: 68px; border-radius: 22px;
  background: linear-gradient(135deg, var(--honey-soft), var(--orange-soft));
  border: 1.5px solid var(--line); font-size: 34px; flex-shrink: 0;
}
.who h1 { font-size: 24px; }
.who p { color: var(--ink-faint); font-size: 13px; margin-top: 4px; }
.owner .el-button { margin-left: auto; }
.section-head { display: flex; align-items: baseline; gap: 12px; margin: 22px 0 14px; }
.section-head h2 { font-size: 20px; }
.section-head .hint { color: var(--ink-faint); font-size: 12px; }
.pager { margin: 18px 0; justify-content: center; }
</style>
