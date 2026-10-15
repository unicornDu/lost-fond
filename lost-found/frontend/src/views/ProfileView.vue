<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import { api } from '../api'

/**
 * 个人中心。审查 2.5：收到的申请展示申请人/凭证/联系方式（杜绝盲审）。
 * 审查 2.7：新增"消息通知"tab，进入即 readAll 清红点，单条点击 readOne。
 */
const route = useRoute()
const user = useUserStore()
const tab = ref(route.query.tab === 'msg' ? 'msg' : 'pub')

const myPublishes = ref([])
const myClaims = ref([])
const received = ref([])
const messages = ref([])
const loading = ref(false)

const publishCount = computed(() => myPublishes.value.length)
const activeClaims = computed(() => myClaims.value.filter((c) => c.claimStatus === '审核中').length)
const successCount = computed(() => myClaims.value.filter((c) => c.claimStatus === '已通过').length)
const pendingReceived = computed(() => received.value.filter((c) => c.claimStatus === '审核中').length)
const unreadCount = computed(() => messages.value.filter((m) => m.isRead === 0).length)

async function loadAll() {
  if (!user.info) return
  loading.value = true
  try {
    const [pub, mine, recv, msgs] = await Promise.all([
      api.myPublishes(), api.myClaims(), api.receivedClaims(), api.messages(),
    ])
    myPublishes.value = pub
    myClaims.value = mine
    received.value = recv
    messages.value = msgs
    markAllReadIfNeeded()   // 修复 F-02：数据加载完成后再判断"进入即已读"
  } catch (e) {
    ElMessage.error(e.message || '加载个人中心失败')
  } finally {
    loading.value = false
  }
}

// 修复 F-02：进入消息 tab 即全部已读（清红点）。
// 原实现 watch 无 immediate 且早于 loadAll 完成 → 从铃铛直达 /profile?tab=msg 时 watcher 不触发、
// 或触发时 unreadCount 还是 0。现改为：immediate + loadAll 完成后复检，并通知 NavBar 立即刷新铃铛。
async function markAllReadIfNeeded() {
  if (tab.value !== 'msg' || unreadCount.value === 0) return
  try {
    await api.readAll()
    messages.value.forEach((m) => (m.isRead = 1))
    window.dispatchEvent(new Event('shiguang-unread-changed'))
  } catch (e) { /* 非关键路径：标已读失败不阻断浏览 */ }
}
watch(tab, markAllReadIfNeeded, { immediate: true })

async function readOne(m) {
  if (m.isRead === 1) return
  try {
    await api.readOne(m.id); m.isRead = 1
    window.dispatchEvent(new Event('shiguang-unread-changed'))
  } catch (e) { /* 非关键路径：单条已读失败静默 */ }
}

async function markFound(item) {
  try {
    await api.markFound(item.id)
    item.status = item.type === 'lost' ? '已找回' : '已认领'
    ElMessage.success('已标记为「已找回」，感谢你的反馈！')
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

async function audit(claim, approve) {
  try {
    let reply = ''
    if (!approve) {
      const r = await ElMessageBox.prompt('可填写拒绝理由（选填）', '拒绝认领', {
        confirmButtonText: '确定拒绝', cancelButtonText: '取消', inputPlaceholder: '例：特征描述不符',
      })
      reply = r.value || ''
    }
    await api.auditClaim(claim.claimId, { approve, reply })
    ElMessage.success(approve ? '已通过认领申请' : '已拒绝认领申请')
    await loadAll()
  } catch (e) {
    if (e === 'cancel' || e?.message === 'cancel') return
    ElMessage.error(e.message || '操作失败')
  }
}

function logout() { user.logout(); ElMessage.success('已退出登录') }

onMounted(loadAll)
</script>

<template>
  <div class="page container" v-loading="loading">
    <template v-if="user.info">
      <div class="me paper-card">
        <span class="avatar">{{ user.info.avatar || '🦊' }}</span>
        <div class="who">
          <h1>{{ user.info.nickname || user.info.account }}</h1>
          <p>学号 {{ user.info.account }}<span v-if="user.info.role === 1"> · 🛡️ 管理员</span></p>
        </div>
        <div class="my-stats">
          <div><b>{{ publishCount }}</b><span>我的发布</span></div>
          <div><b>{{ activeClaims }}</b><span>认领审核中</span></div>
          <div><b>{{ successCount }}</b><span>成功认领</span></div>
        </div>
        <el-button round plain @click="logout">退出登录</el-button>
      </div>

      <el-tabs class="tabs" v-model="tab">
        <!-- 我的发布 -->
        <el-tab-pane label="我的发布" name="pub">
          <div v-for="it in myPublishes" :key="it.id" class="row paper-card">
            <img :src="it.thumb || it.img" alt="" @error="$event.target.style.opacity = 0" />
            <div class="mid">
              <RouterLink :to="`/item/${it.id}`" class="t">{{ it.title }}</RouterLink>
              <p>📍 {{ it.location }} · ⏰ {{ (it.time || '').slice(5, 16) }} · 👀 {{ it.views }}</p>
            </div>
            <el-tag v-if="it.auditStatus === 0" type="warning" round size="small">待审核</el-tag>
            <el-tag :type="it.status === '已找回' || it.status === '已认领' ? 'info' : it.type === 'lost' ? 'warning' : 'success'" round>
              {{ it.status }}
            </el-tag>
            <el-button v-if="it.status === '寻找中' || it.status === '待认领'" size="small" round type="primary" plain @click="markFound(it)">
              标记已找回
            </el-button>
            <el-button size="small" round plain @click="$router.push(`/item/${it.id}`)">查看</el-button>
          </div>
          <el-empty v-if="!myPublishes.length" description="你还没有发布过启事" />
        </el-tab-pane>

        <!-- 我的认领 -->
        <el-tab-pane label="我的认领" name="claim">
          <div v-for="c in myClaims" :key="'c' + c.claimId" class="row paper-card">
            <img :src="c.img" alt="" @error="$event.target.style.opacity = 0" />
            <div class="mid">
              <RouterLink :to="`/item/${c.id}`" class="t">{{ c.title }}</RouterLink>
              <p>申请时间 {{ (c.time || '').slice(5, 16) }}<span v-if="c.reply"> · 回复：{{ c.reply }}</span></p>
            </div>
            <el-tag :type="c.claimStatus === '已通过' ? 'success' : c.claimStatus === '已拒绝' ? 'danger' : 'warning'" round>
              {{ c.claimStatus }}
            </el-tag>
            <el-button size="small" round plain @click="$router.push(`/item/${c.id}`)">查看</el-button>
          </div>
          <el-empty v-if="!myClaims.length" description="你还没有提交过认领申请" />
        </el-tab-pane>

        <!-- 收到的认领申请（发布者审核，展示凭证杜绝盲审 审查2.5） -->
        <el-tab-pane name="recv">
          <template #label>收到的申请<el-badge v-if="pendingReceived" :value="pendingReceived" class="tab-badge" /></template>
          <div v-for="c in received" :key="'r' + c.claimId" class="row paper-card claim-row">
            <img :src="c.img" alt="" @error="$event.target.style.opacity = 0" />
            <div class="mid">
              <RouterLink :to="`/item/${c.id}`" class="t">{{ c.title }}</RouterLink>
              <p class="who-line">申请人：{{ c.claimantName || '同学' }}（{{ c.claimantAccount }}）<span v-if="c.contact"> · 📞 {{ c.contact }}</span></p>
              <p class="proof">🧾 凭证：{{ c.proof }}</p>
              <p>申请时间 {{ (c.time || '').slice(5, 16) }}<span v-if="c.reply"> · 已回复：{{ c.reply }}</span></p>
            </div>
            <el-tag :type="c.claimStatus === '已通过' ? 'success' : c.claimStatus === '已拒绝' ? 'danger' : 'warning'" round>
              {{ c.claimStatus }}
            </el-tag>
            <template v-if="c.claimStatus === '审核中'">
              <el-button size="small" round type="primary" @click="audit(c, true)">通过</el-button>
              <el-button size="small" round plain @click="audit(c, false)">拒绝</el-button>
            </template>
          </div>
          <el-empty v-if="!received.length" description="暂时没有人申请认领你发布的物品" />
        </el-tab-pane>

        <!-- 消息通知（审查 2.7） -->
        <el-tab-pane name="msg">
          <template #label>消息通知<el-badge v-if="unreadCount" :value="unreadCount" class="tab-badge" /></template>
          <div v-for="m in messages" :key="m.id" class="msg paper-card" :class="{ unread: m.isRead === 0 }" @click="readOne(m)">
            <span class="dot" v-if="m.isRead === 0"></span>
            <div class="msg-body">
              <b>{{ m.title }}</b>
              <p>{{ m.content }}</p>
              <small>{{ (m.createTime || '').replace('T', ' ').slice(0, 16) }}</small>
            </div>
            <el-button v-if="m.refId" size="small" round plain @click.stop="$router.push(`/item/${m.refId}`)">查看启事</el-button>
          </div>
          <el-empty v-if="!messages.length" description="暂无消息" />
        </el-tab-pane>
      </el-tabs>
    </template>

    <el-empty v-else description="登录后查看我的发布与认领">
      <el-button type="primary" round @click="$router.push('/auth')">去登录</el-button>
    </el-empty>
  </div>
</template>

<style scoped>
.me { display: flex; align-items: center; gap: 18px; padding: 26px 30px; margin-bottom: 20px; }
.avatar {
  display: grid; place-items: center; width: 68px; height: 68px; border-radius: 22px;
  background: linear-gradient(135deg, var(--honey-soft), var(--orange-soft));
  border: 1.5px solid var(--line); font-size: 34px;
}
.who h1 { font-size: 24px; }
.who p { color: var(--ink-faint); font-size: 13px; margin-top: 4px; }
.my-stats { margin-left: auto; display: flex; gap: 26px; }
.my-stats div { display: flex; flex-direction: column; align-items: center; }
.my-stats b { font-family: var(--font-display); font-size: 24px; color: var(--orange-deep); }
.my-stats span { font-size: 12px; color: var(--ink-faint); }

.tabs { margin-top: 6px; }
.tab-badge { margin-left: 4px; vertical-align: middle; }
.row { display: flex; align-items: center; gap: 16px; padding: 14px 18px; margin-bottom: 14px; }
.row img { width: 84px; height: 64px; object-fit: cover; border-radius: 10px; background: var(--cream-deep); flex-shrink: 0; }
.row .mid { flex: 1; min-width: 0; }
.row .t { font-weight: 600; font-size: 14.5px; }
.row .t:hover { color: var(--orange-deep); }
.row .mid p { color: var(--ink-faint); font-size: 12.5px; margin-top: 5px; }
.claim-row { align-items: flex-start; }
.claim-row .who-line { color: var(--ink-soft); }
.claim-row .proof { color: var(--orange-deep); background: var(--orange-soft); padding: 4px 8px; border-radius: 8px; display: inline-block; }

.msg { position: relative; display: flex; gap: 12px; align-items: center; padding: 14px 18px; margin-bottom: 12px; cursor: pointer; }
.msg.unread { border-color: var(--orange); background: var(--orange-soft); }
.msg .dot { position: absolute; left: 8px; top: 50%; transform: translateY(-50%); width: 8px; height: 8px; border-radius: 50%; background: var(--orange); }
.msg-body { flex: 1; }
.msg-body b { font-size: 14.5px; }
.msg-body p { color: var(--ink-soft); font-size: 13px; margin-top: 4px; line-height: 1.6; }
.msg-body small { color: var(--ink-faint); font-size: 11.5px; }

@media (max-width: 720px) { .me { flex-wrap: wrap; } .my-stats { margin-left: 0; } }
</style>
