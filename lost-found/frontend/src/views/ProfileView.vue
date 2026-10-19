<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import { api } from '../api'
import { t, claimStatusLabel, itemStatusLabel } from '../i18n'
import { assetUrl } from '../utils/asset'
import { downloadDelivery, DELIVERY_ICONS, fmtSize } from '../utils/delivery'
import ItemCard from '../components/ItemCard.vue'

/**
 * 个人中心。审查 2.5：收到的申请展示申请人/凭证/联系方式（杜绝盲审）。
 * 审查 2.7：新增"消息通知"tab，进入即 readAll 清红点，单条点击 readOne。
 * E-01：新增"账号设置"tab（资料编辑/修改密码）。
 * G 版式升级：消息 tab → 通知中心（认领与审核 / 系统 / 私信 三分栏），
 * 私信按"对方"聚合为会话，线程内直接回复（复用 replyMessage）。
 */
const route = useRoute()
const router = useRouter()
const user = useUserStore()
const tab = ref(route.query.tab === 'msg' ? 'msg' : 'pub')

const myPublishes = ref([])
const myClaims = ref([])
const received = ref([])
const messages = ref([])
const favorites = ref([])   // E-07：我的收藏
const deliveries = ref([])  // P1：我的交付文件（认领证明/报表/匹配报告）
const loading = ref(false)

const publishCount = computed(() => myPublishes.value.length)
const activeClaims = computed(() => myClaims.value.filter((c) => c.claimStatus === '审核中').length)
const successCount = computed(() => myClaims.value.filter((c) => c.claimStatus === '已通过').length)
const pendingReceived = computed(() => received.value.filter((c) => c.claimStatus === '审核中').length)
const unreadCount = computed(() => messages.value.filter((m) => m.isRead === 0).length)

// ---- G 版式：通知中心三分栏（2026-10-18 审查修复：状态提前声明，避免 immediate watcher 访问 TDZ）----
const msgSub = ref(route.query.sub === 'dm' ? 'dm' : 'claim')
const MSG_SUBS = computed(() => [
  { key: 'claim', label: t('profile.sub.claim') },
  { key: 'system', label: t('profile.sub.system') },
  { key: 'dm', label: t('profile.sub.dm') },
])
const claimMsgs = computed(() => messages.value.filter((m) => m.type === 'claim' || m.type === 'audit'))
const systemMsgs = computed(() => messages.value.filter((m) => m.type !== 'claim' && m.type !== 'audit' && m.type !== 'dm'))
const dmMsgs = computed(() => messages.value.filter((m) => m.type === 'dm' && m.fromUserId))

function msgSubUnread(k) {
  const arr = k === 'claim' ? claimMsgs.value : k === 'system' ? systemMsgs.value : dmMsgs.value
  return arr.filter((m) => m.isRead === 0).length
}

async function loadAll() {
  if (!user.info) return
  loading.value = true
  try {
    const [pub, mine, recv, msgs, favs, dels] = await Promise.all([
      api.myPublishes(), api.myClaims(), api.receivedClaims(), api.messages(),
      api.favorites({ page: 1, size: 50 }).catch(() => ({ records: [] })),
      api.deliveries().catch(() => []),
    ])
    myPublishes.value = pub
    myClaims.value = mine
    received.value = recv
    messages.value = msgs
    favorites.value = favs.records || []
    deliveries.value = dels || []
    markSubRead()   // 数据加载完成后，标记"当前分栏"已读
  } catch (e) {
    ElMessage.error(e.message || t('profile.loadFail'))
  } finally {
    loading.value = false
  }
}

// 2026-10-18 审查修复：进入消息页不再"全部已读"——那会立刻清空三分栏/会话的未读点，
// 与通知中心的信息架构冲突。改为：只把"当前选中的分栏"标记已读；切换分栏时补标该栏。
// NavBar 铃铛仍会因未读减少而刷新（shiguang-unread-changed 事件）。
async function markSubRead() {
  if (tab.value !== 'msg') return
  const arr = msgSub.value === 'claim' ? claimMsgs.value : msgSub.value === 'system' ? systemMsgs.value : dmMsgs.value
  const unread = arr.filter((m) => m.isRead === 0)
  if (!unread.length) return
  try {
    await Promise.all(unread.map((m) => api.readOne(m.id).catch(() => {})))
    unread.forEach((m) => (m.isRead = 1))
    window.dispatchEvent(new Event('shiguang-unread-changed'))
  } catch (e) { /* 非关键路径：标已读失败不阻断浏览 */ }
}
watch([tab, msgSub], markSubRead, { immediate: true })

// E-07：切到收藏 tab 时刷新收藏列表（取消收藏后回来能看到最新状态）
async function reloadFavorites() {
  if (tab.value !== 'fav') return
  try {
    const favs = await api.favorites({ page: 1, size: 50 })
    favorites.value = favs.records || []
  } catch { /* 非关键路径 */ }
}
watch(tab, reloadFavorites)

async function readOne(m) {
  if (m.isRead === 1) return
  try {
    await api.readOne(m.id); m.isRead = 1
    window.dispatchEvent(new Event('shiguang-unread-changed'))
  } catch (e) { /* 非关键路径：单条已读失败静默 */ }
}

/* 全部已读（对齐整合版 v1 的"全部标为已读"） */
async function readAllNow() {
  try {
    await api.readAll()
    messages.value.forEach((m) => (m.isRead = 1))
    window.dispatchEvent(new Event('shiguang-unread-changed'))
    ElMessage.success(t('profile.readAllOk'))
  } catch (e) { ElMessage.error(e.message || t('profile.opFail')) }
}

// ---- G 版式：通知中心（分栏状态定义见上方）----
// 导航/外部链接带 sub 参数进入时同步分栏
watch(() => route.query, (q) => {
  if (q.tab) tab.value = q.tab
  if (q.sub) { msgSub.value = q.sub; activeConv.value = null }
})

/* 私信会话：按对方（fromUserId）聚合；线程内按时间升序 */
const conversations = computed(() => {
  const map = new Map()
  for (const m of dmMsgs.value) {
    if (!map.has(m.fromUserId)) {
      map.set(m.fromUserId, { fromUserId: m.fromUserId, name: m.fromNickname || t('profile.msg.me'), msgs: [] })
    }
    map.get(m.fromUserId).msgs.push(m)
  }
  const list = [...map.values()].map((c) => {
    c.msgs.sort((a, b) => (a.createTime || '').localeCompare(b.createTime || ''))
    c.last = c.msgs[c.msgs.length - 1]
    c.unread = c.msgs.some((m) => m.isRead === 0)
    return c
  })
  return list.sort((a, b) => (b.last.createTime || '').localeCompare(a.last.createTime || ''))
})

const activeConv = ref(null)
const threadInput = ref('')
const sendingThread = ref(false)

function openConv(c) { activeConv.value = c }
function closeConv() { activeConv.value = null; threadInput.value = '' }

/* 线程回复：回复该会话最新一条私信（后端 replyMessage 以消息 id 定向） */
async function sendThread() {
  const c = activeConv.value
  const v = threadInput.value.trim()
  if (!c || !v) return
  const last = [...c.msgs].reverse().find((m) => !m.sent)
  if (!last) { ElMessage.warning(t('profile.reply.noMsg')); return }
  sendingThread.value = true
  try {
    await api.replyMessage(last.id, v)
    c.msgs.push({ id: 'local-' + Date.now(), sent: true, content: v, createTime: new Date().toISOString().slice(0, 19) })
    threadInput.value = ''
  } catch (e) {
    ElMessage.error(e.message || t('profile.reply.fail'))
  } finally {
    sendingThread.value = false
  }
}

// P1：交付文件动作（桌面原生打开 / Web 受控下载；渲染层只经手 deliveryId）
async function takeDelivery(d) {
  try { await downloadDelivery(d) }
  catch (e) { ElMessage.error(e.message || t('profile.opFail')) }
}
async function removeDelivery(d) {
  try {
    await ElMessageBox.confirm(t('profile.deliv.confirm', { title: d.title }), t('profile.deliv.title'), { type: 'warning' })
    await api.deleteDelivery(d.id)
    deliveries.value = deliveries.value.filter((x) => x.id !== d.id)
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') ElMessage.error(e.message || t('profile.deliv.fail'))
  }
}

async function markFound(item) {
  try {
    await api.markFound(item.id)
    item.status = item.type === 'lost' ? '已找回' : '已认领'
    ElMessage.success(t('profile.markFoundOk'))
    window.dispatchEvent(new Event('shiguang-items-changed'))
  } catch (e) { ElMessage.error(e.message || t('profile.opFail')) }
}

// ---- E-01 账号设置：资料编辑 + 修改密码 ----
const AVATARS = ['🦊', '🐰', '🐼', '🐯', '🦁', '🐨', '🐷', '🐸']
const profileForm = ref({ nickname: '', phone: '', avatar: '' })
const pwdForm = ref({ oldPassword: '', newPassword: '', confirm: '' })
const profileRef = ref()
const pwdRef = ref()

function initAccountForms() {
  if (!user.info) return
  profileForm.value.nickname = user.info.nickname || ''
  profileForm.value.phone = user.info.phone || ''
  profileForm.value.avatar = user.info.avatar || '🦊'
}
watch(() => user.info, initAccountForms, { immediate: true })

const profileRules = computed(() => ({
  nickname: [
    { required: true, message: t('valid.nicknameRequired'), trigger: 'blur' },
    { max: 12, message: t('valid.nicknameMax'), trigger: 'blur' },
  ],
  phone: [{ max: 20, message: t('valid.phoneMax'), trigger: 'blur' }],
}))
const pwdRules = computed(() => ({
  oldPassword: [{ required: true, message: t('valid.oldPwdRequired'), trigger: 'blur' }],
  newPassword: [
    { required: true, message: t('valid.newPwdRequired'), trigger: 'blur' },
    { min: 6, max: 32, message: t('valid.newPwdLen'), trigger: 'blur' },
  ],
  confirm: [
    { required: true, message: t('valid.confirmRequired'), trigger: 'blur' },
    {
      validator: (_r, v, cb) => (v === pwdForm.value.newPassword ? cb() : cb(new Error(t('valid.confirmMismatch')))),
      trigger: 'blur',
    },
  ],
}))

async function saveProfile() {
  await profileRef.value.validate()
  try {
    const vo = await api.updateProfile({
      nickname: profileForm.value.nickname,
      phone: profileForm.value.phone,
      avatar: profileForm.value.avatar,
    })
    user.updateInfo(vo)
    ElMessage.success(t('profile.account.saveOk'))
  } catch (e) { ElMessage.error(e.message || t('profile.account.saveFail')) }
}

async function changePwd() {
  await pwdRef.value.validate()
  try {
    await api.changePassword({ oldPassword: pwdForm.value.oldPassword, newPassword: pwdForm.value.newPassword })
    user.logout()
    ElMessage.success(t('profile.account.pwdOk'))
    router.push('/auth?redirect=/profile')
  } catch (e) { ElMessage.error(e.message || t('profile.account.pwdFail')) }
}

async function audit(claim, approve) {
  try {
    let reply = ''
    if (!approve) {
      const r = await ElMessageBox.prompt(t('profile.audit.rejectPrompt'), t('profile.audit.rejectTitle'), {
        confirmButtonText: t('profile.audit.rejectOk'), cancelButtonText: t('common.cancel'), inputPlaceholder: t('profile.audit.rejectPh'),
      })
      reply = r.value || ''
    }
    await api.auditClaim(claim.claimId, { approve, reply })
    ElMessage.success(approve ? t('profile.audit.approved') : t('profile.audit.rejected'))
    await loadAll()
  } catch (e) {
    if (e === 'cancel' || e?.message === 'cancel') return
    ElMessage.error(e.message || t('profile.opFail'))
  }
}

function logout() { user.logout(); ElMessage.success(t('profile.logoutOk')) }

onMounted(loadAll)
</script>

<template>
  <div class="page container" v-loading="loading">
    <template v-if="user.info">
      <div class="me paper-card">
        <span class="avatar">{{ user.info.avatar || '🦊' }}</span>
        <div class="who">
          <h1>{{ user.info.nickname || user.info.account }}</h1>
          <p>{{ $t('me.id', { n: user.info.account }) }}<span v-if="user.info.role === 1">{{ $t('profile.adminTag') }}</span></p>
        </div>
        <div class="my-stats">
          <div><b>{{ publishCount }}</b><span>{{ $t('profile.stat.pub') }}</span></div>
          <div><b>{{ activeClaims }}</b><span>{{ $t('profile.stat.claims') }}</span></div>
          <div><b>{{ successCount }}</b><span>{{ $t('profile.stat.success') }}</span></div>
        </div>
        <el-button round plain @click="logout">{{ $t('profile.logout') }}</el-button>
      </div>

      <el-tabs class="tabs" v-model="tab">
        <!-- 我的发布 -->
        <el-tab-pane :label="$t('profile.tab.pub')" name="pub">
          <div v-for="it in myPublishes" :key="it.id" class="row paper-card">
            <img :src="assetUrl(it.thumb || it.img)" alt="" @error="$event.target.style.opacity = 0" />
            <div class="mid">
              <RouterLink :to="`/item/${it.id}`" class="t">{{ it.title }}</RouterLink>
              <p>{{ it.location }} · {{ (it.time || '').slice(5, 16) }} · {{ $t('profile.pub.views', { n: it.views }) }}</p>
            </div>
            <el-tag v-if="it.auditStatus === 0" type="warning" round size="small">{{ $t('profile.pub.pending') }}</el-tag>
            <el-tag :type="it.status === '已找回' || it.status === '已认领' ? 'info' : it.type === 'lost' ? 'warning' : 'success'" round>
              {{ itemStatusLabel(it.status) }}
            </el-tag>
            <el-button v-if="it.status === '寻找中' || it.status === '待认领'" size="small" round type="primary" plain @click="markFound(it)">
              {{ $t('profile.pub.markFound') }}
            </el-button>
            <el-button size="small" round plain @click="$router.push(`/item/${it.id}`)">{{ $t('profile.pub.view') }}</el-button>
          </div>
          <el-empty v-if="!myPublishes.length" :description="$t('profile.pub.empty')" />
        </el-tab-pane>

        <!-- 我的收藏（E-07） -->
        <el-tab-pane name="fav">
          <template #label>{{ $t('profile.tab.fav') }}<el-badge v-if="favorites.length" :value="favorites.length" class="tab-badge" /></template>
          <div v-if="favorites.length" class="grid-cards">
            <ItemCard v-for="it in favorites" :key="'f' + it.id" :item="it" fav-initial />
          </div>
          <el-empty v-else :description="$t('profile.fav.empty')" />
        </el-tab-pane>

        <!-- 我的认领 -->
        <el-tab-pane :label="$t('profile.tab.claim')" name="claim">
          <div v-for="c in myClaims" :key="'c' + c.claimId" class="row paper-card">
            <img :src="assetUrl(c.img)" alt="" @error="$event.target.style.opacity = 0" />
            <div class="mid">
              <RouterLink :to="`/item/${c.id}`" class="t">{{ c.title }}</RouterLink>
              <p>{{ $t('profile.claim.appliedAt', { time: (c.time || '').slice(5, 16) }) }}<span v-if="c.reply">{{ $t('profile.claim.reply', { reply: c.reply }) }}</span></p>
            </div>
            <el-tag :type="c.claimStatus === '已通过' ? 'success' : c.claimStatus === '已拒绝' ? 'danger' : 'warning'" round>
              {{ claimStatusLabel(c.claimStatus) }}
            </el-tag>
            <el-button size="small" round plain @click="$router.push(`/item/${c.id}`)">{{ $t('profile.pub.view') }}</el-button>
          </div>
          <el-empty v-if="!myClaims.length" :description="$t('profile.claim.empty')" />
        </el-tab-pane>

        <!-- 收到的认领申请（发布者审核，展示凭证杜绝盲审 审查2.5） -->
        <el-tab-pane name="recv">
          <template #label>{{ $t('profile.tab.recv') }}<el-badge v-if="pendingReceived" :value="pendingReceived" class="tab-badge" /></template>
          <div v-for="c in received" :key="'r' + c.claimId" class="row paper-card claim-row">
            <img :src="assetUrl(c.img)" alt="" @error="$event.target.style.opacity = 0" />
            <div class="mid">
              <RouterLink :to="`/item/${c.id}`" class="t">{{ c.title }}</RouterLink>
              <p class="who-line">{{ $t('profile.recv.applicant', { name: c.claimantName || $t('me.student'), account: c.claimantAccount }) }}<span v-if="c.contact">{{ $t('profile.recv.contact', { contact: c.contact }) }}</span></p>
              <p class="proof">{{ $t('profile.recv.proof', { proof: c.proof }) }}</p>
              <p>{{ $t('profile.claim.appliedAt', { time: (c.time || '').slice(5, 16) }) }}<span v-if="c.reply">{{ $t('profile.recv.replied', { reply: c.reply }) }}</span></p>
            </div>
            <el-tag :type="c.claimStatus === '已通过' ? 'success' : c.claimStatus === '已拒绝' ? 'danger' : 'warning'" round>
              {{ claimStatusLabel(c.claimStatus) }}
            </el-tag>
            <template v-if="c.claimStatus === '审核中'">
              <el-button size="small" round type="primary" @click="audit(c, true)">{{ $t('profile.recv.approve') }}</el-button>
              <el-button size="small" round plain @click="audit(c, false)">{{ $t('profile.recv.reject') }}</el-button>
            </template>
          </div>
          <el-empty v-if="!received.length" :description="$t('profile.recv.empty')" />
        </el-tab-pane>

        <!-- 消息通知（G 版式：通知中心三分栏 + 私信会话化；P1 顶部挂交付文件卡片） -->
        <el-tab-pane name="msg">
          <template #label>{{ $t('profile.tab.msg') }}<el-badge v-if="unreadCount" :value="unreadCount" class="tab-badge" /></template>

          <!-- P1：交付文件（读 delivery 表渲染，与生成方解耦；桌面=原生打开，Web=受控下载） -->
          <template v-if="deliveries.length">
            <p class="deliv-head">{{ $t('profile.msg.deliv') }}</p>
            <div v-for="d in deliveries" :key="'d' + d.id" class="msg paper-card">
              <span class="d-icon">{{ DELIVERY_ICONS[d.bizType] || '📄' }}</span>
              <div class="msg-body">
                <b>{{ d.title }}</b>
                <p v-if="d.description">{{ d.description }}</p>
                <small>{{ fmtSize(d.fileSize) }} · {{ (d.createTime || '').replace('T', ' ').slice(0, 16) }}</small>
              </div>
              <el-button size="small" round type="primary" plain @click.stop="takeDelivery(d)">{{ $t('profile.msg.download') }}</el-button>
              <el-button size="small" round plain type="danger" @click.stop="removeDelivery(d)">{{ $t('profile.msg.delete') }}</el-button>
            </div>
          </template>

          <!-- 三分栏切换 -->
          <div class="ntf-bar">
            <div class="ntf-tabs">
              <button
                v-for="s in MSG_SUBS" :key="s.key"
                class="ntf-tab" :class="{ on: msgSub === s.key }"
                @click="msgSub = s.key; activeConv = null"
              >{{ s.label }}<i v-if="msgSubUnread(s.key)">{{ msgSubUnread(s.key) }}</i></button>
            </div>
            <button v-if="unreadCount" class="all-read" @click="readAllNow">{{ $t('profile.msg.readAll') }}</button>
          </div>

          <!-- 认领与审核 / 系统通知：卡片列表 -->
          <template v-if="msgSub !== 'dm'">
            <div
              v-for="m in (msgSub === 'claim' ? claimMsgs : systemMsgs)"
              :key="m.id" class="msg paper-card" :class="{ unread: m.isRead === 0 }" @click="readOne(m)"
            >
              <span class="dot" v-if="m.isRead === 0"></span>
              <div class="msg-body">
                <b>{{ m.title }}</b>
                <p>{{ m.content }}</p>
                <small>{{ (m.createTime || '').replace('T', ' ').slice(0, 16) }}</small>
              </div>
              <el-button v-if="m.refId" size="small" round plain @click.stop="$router.push(`/item/${m.refId}`)">{{ $t('profile.msg.viewItem') }}</el-button>
            </div>
            <el-empty
              v-if="!(msgSub === 'claim' ? claimMsgs : systemMsgs).length"
              :description="msgSub === 'claim' ? $t('profile.msg.empty.claim') : $t('profile.msg.empty.system')"
            />
          </template>

          <!-- 私信：会话列表 → 线程 -->
          <template v-else>
            <div v-if="!activeConv">
              <div v-for="c in conversations" :key="c.fromUserId" class="conv paper-card" @click="openConv(c)">
                <span class="c-avatar" :class="{ unread: c.unread }">{{ (c.name || $t('inbox.peer'))[0] }}</span>
                <div class="c-main">
                  <div class="c-top">
                    <b>{{ c.name }}</b>
                    <small>{{ (c.last.createTime || '').replace('T', ' ').slice(5, 16) }}</small>
                  </div>
                  <p class="c-last"><span v-if="c.unread" class="c-dot"></span>{{ c.last.content }}</p>
                </div>
                <span class="c-go">{{ $t('profile.msg.enter') }}</span>
              </div>
              <el-empty v-if="!conversations.length" :description="$t('profile.msg.convEmpty')" />
            </div>

            <div v-else class="thread paper-card">
              <div class="thread-head">
                <button class="thread-back" @click="closeConv">{{ $t('profile.msg.back') }}</button>
                <b>{{ $t('profile.msg.threadWith', { name: activeConv.name }) }}</b>
                <el-button
                  v-if="activeConv.last?.refId" size="small" round plain
                  @click="$router.push(`/item/${activeConv.last.refId}`)"
                >{{ $t('profile.msg.threadItem') }}</el-button>
              </div>
              <div class="thread-msgs">
                <div v-for="m in activeConv.msgs" :key="m.id" class="t-msg" :class="{ me: m.sent }">
                  <p>{{ m.content }}</p>
                  <small>{{ m.sent ? $t('profile.msg.me') : activeConv.name }} · {{ (m.createTime || '').replace('T', ' ').slice(5, 16) }}</small>
                </div>
              </div>
              <div class="thread-input">
                <el-input
                  v-model="threadInput" :placeholder="$t('profile.msg.threadPh')"
                  maxlength="200" @keyup.enter="sendThread"
                />
                <el-button type="primary" round :loading="sendingThread" @click="sendThread">{{ $t('profile.msg.send') }}</el-button>
              </div>
            </div>
          </template>
        </el-tab-pane>

        <!-- 账号设置（E-01） -->
        <el-tab-pane :label="$t('profile.tab.account')" name="account">
          <div class="account-grid">
            <el-form ref="profileRef" :model="profileForm" :rules="profileRules" label-width="72px" class="paper-card form-card">
              <h3>{{ $t('profile.account.profile') }}</h3>
              <el-form-item :label="$t('profile.account.avatar')">
                <div class="avatar-pick">
                  <button
                    v-for="a in AVATARS" :key="a" type="button" class="avatar-opt"
                    :class="{ active: profileForm.avatar === a }" @click="profileForm.avatar = a">{{ a }}</button>
                </div>
              </el-form-item>
              <el-form-item :label="$t('profile.account.nickname')" prop="nickname">
                <el-input v-model="profileForm.nickname" maxlength="12" show-word-limit />
              </el-form-item>
              <el-form-item :label="$t('profile.account.phone')" prop="phone">
                <el-input v-model="profileForm.phone" maxlength="20" :placeholder="$t('profile.account.phonePh')" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" round @click="saveProfile">{{ $t('profile.account.save') }}</el-button>
              </el-form-item>
            </el-form>

            <el-form ref="pwdRef" :model="pwdForm" :rules="pwdRules" label-width="72px" class="paper-card form-card">
              <h3>{{ $t('profile.account.pwd') }}</h3>
              <el-form-item :label="$t('profile.account.old')" prop="oldPassword">
                <el-input v-model="pwdForm.oldPassword" type="password" show-password />
              </el-form-item>
              <el-form-item :label="$t('profile.account.new')" prop="newPassword">
                <el-input v-model="pwdForm.newPassword" type="password" show-password :placeholder="$t('valid.newPwdLen')" />
              </el-form-item>
              <el-form-item :label="$t('profile.account.confirm')" prop="confirm">
                <el-input v-model="pwdForm.confirm" type="password" show-password />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" round @click="changePwd">{{ $t('profile.account.change') }}</el-button>
              </el-form-item>
              <p class="pwd-hint">{{ $t('profile.account.pwdHint') }}</p>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
    </template>

    <el-empty v-else :description="$t('profile.loginRequired')">
      <el-button type="primary" round @click="$router.push('/auth')">{{ $t('profile.goLogin') }}</el-button>
    </el-empty>
  </div>
</template>

<style scoped>
.me { display: flex; align-items: center; gap: 18px; padding: 26px 30px; margin-bottom: 20px; }
.avatar {
  display: grid; place-items: center; width: 68px; height: 68px; border-radius: 20px;
  background: var(--orange-soft);
  border: 1.5px solid var(--line); font-size: 34px;
}
.who h1 { font-size: 24px; }
.who p { color: var(--ink-faint); font-size: 13px; margin-top: 4px; }
.my-stats { margin-left: auto; display: flex; gap: 26px; }
.my-stats div { display: flex; flex-direction: column; align-items: center; }
.my-stats b { font-size: 24px; font-weight: 800; color: var(--orange-deep); font-variant-numeric: tabular-nums; }
.my-stats span { font-size: 12px; color: var(--ink-faint); }

.tabs { margin-top: 6px; }
.tab-badge { margin-left: 4px; vertical-align: middle; }
.row { display: flex; align-items: center; gap: 16px; padding: 14px 18px; margin-bottom: 14px; }
.row img { width: 84px; height: 64px; object-fit: cover; border-radius: 10px; background: var(--cream-deep); flex-shrink: 0; }
.row .mid { flex: 1; min-width: 0; }
.row .t { font-weight: 700; font-size: 14.5px; }
.row .t:hover { color: var(--orange-deep); }
.row .mid p { color: var(--ink-faint); font-size: 12.5px; margin-top: 5px; }
.claim-row { align-items: flex-start; }
.claim-row .who-line { color: var(--ink-soft); }
.claim-row .proof { color: var(--orange-deep); background: var(--orange-soft); padding: 4px 8px; border-radius: 8px; display: inline-block; }

.msg { position: relative; display: flex; gap: 12px; align-items: center; padding: 14px 18px; margin-bottom: 12px; cursor: pointer; }
.msg.unread { border-color: var(--orange); background: var(--orange-soft); box-shadow: inset 3px 0 0 var(--orange); }
.msg .dot { position: absolute; left: 8px; top: 50%; transform: translateY(-50%); width: 8px; height: 8px; border-radius: 50%; background: var(--orange); }
.msg-body { flex: 1; }
.msg-body b { font-size: 14.5px; }
.msg-body p { color: var(--ink-soft); font-size: 13px; margin-top: 4px; line-height: 1.6; }
.msg-body small { color: var(--ink-faint); font-size: 11.5px; }
.deliv-head { color: var(--ink-faint); font-size: 12px; margin: 6px 0 8px 4px; }
.d-icon { display: grid; place-items: center; width: 40px; height: 40px; border-radius: 12px; background: var(--honey-soft); font-size: 20px; flex-shrink: 0; }

/* G 版式：通知分栏 */
.ntf-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: 4px 0 16px; flex-wrap: wrap; }
.ntf-bar .ntf-tabs { margin: 0; }
.all-read {
  border: 0; background: none; cursor: pointer;
  color: var(--orange-deep); font-family: inherit; font-size: 12.5px; font-weight: 700;
  padding: 6px 2px; transition: opacity .18s;
}
.all-read:hover { opacity: .75; text-decoration: underline; }
.ntf-tabs { display: flex; gap: 8px; margin: 4px 0 16px; flex-wrap: wrap; }
.ntf-tab {
  border: 0; cursor: pointer;
  padding: 9px 17px; border-radius: 999px;
  background: var(--cream-deep); color: var(--ink-soft);
  font-size: 13px; font-weight: 700;
  display: inline-flex; align-items: center; gap: 6px;
  transition: all .18s cubic-bezier(.22, 1, .36, 1);
}
.ntf-tab:hover { color: var(--ink); transform: translateY(-1px); }
.ntf-tab.on { background: var(--ink); color: var(--paper); }
.ntf-tab i {
  font-style: normal; background: var(--rose); color: #fff;
  border-radius: 999px; font-size: 10px; font-weight: 800; padding: 1px 7px;
}

/* G 版式：私信会话列表与线程 */
.conv { display: flex; align-items: center; gap: 14px; padding: 15px 18px; margin-bottom: 12px; cursor: pointer; transition: all .18s cubic-bezier(.22, 1, .36, 1); }
.conv:hover { transform: translateX(4px); border-color: var(--orange); }
.c-avatar {
  display: grid; place-items: center; width: 44px; height: 44px; border-radius: 14px;
  background: var(--orange-soft); color: var(--orange-deep);
  font-size: 16px; font-weight: 800; flex-shrink: 0;
}
.c-avatar.unread { box-shadow: 0 0 0 3px rgba(15, 122, 92, .25); }
.c-main { flex: 1; min-width: 0; }
.c-top { display: flex; justify-content: space-between; align-items: baseline; gap: 10px; }
.c-top b { font-size: 14.5px; }
.c-top small { font-size: 11px; color: var(--ink-faint); font-variant-numeric: tabular-nums; }
.c-last {
  color: var(--ink-soft); font-size: 13px; margin-top: 5px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  display: flex; align-items: center; gap: 6px;
}
.c-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--orange); flex-shrink: 0; }
.c-go { font-size: 12.5px; font-weight: 700; color: var(--orange-deep); flex-shrink: 0; }

.thread { padding: 0; overflow: hidden; }
.thread-head {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 18px; border-bottom: 1px solid var(--line);
}
.thread-head b { font-size: 15px; }
.thread-head .thread-back {
  border: 0; background: none; cursor: pointer;
  color: var(--orange-deep); font-weight: 700; font-size: 13px;
}
.thread-msgs {
  display: flex; flex-direction: column; gap: 10px;
  padding: 18px; max-height: 380px; overflow-y: auto;
}
.t-msg { max-width: 78%; }
.t-msg p {
  padding: 10px 14px; border-radius: 14px;
  background: var(--cream-deep); font-size: 13.5px; line-height: 1.7;
}
.t-msg.me { align-self: flex-end; text-align: right; }
.t-msg.me p { background: var(--orange); color: var(--paper); text-align: left; }
.t-msg small { display: block; font-size: 10.5px; color: var(--ink-faint); margin-top: 4px; }
.thread-input { display: flex; gap: 10px; padding: 14px 16px; border-top: 1px solid var(--line); }

/* E-01 账号设置 */
.account-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 18px; }
.form-card { padding: 22px 26px; }
.form-card h3 { margin-bottom: 16px; font-size: 16px; }
.avatar-pick { display: flex; gap: 8px; flex-wrap: wrap; }
.avatar-opt {
  width: 40px; height: 40px; border-radius: 12px; border: 1.5px solid var(--line);
  background: var(--cream-deep); font-size: 20px; cursor: pointer; transition: all 0.15s;
}
.avatar-opt.active { border-color: var(--orange); background: var(--orange-soft); transform: scale(1.08); }
.pwd-hint { color: var(--ink-faint); font-size: 12px; margin: 0 0 0 72px; }

@media (max-width: 720px) { .me { flex-wrap: wrap; } .my-stats { margin-left: 0; } }
</style>
