<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import { api } from '../api'
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
const MSG_SUBS = [
  { key: 'claim', label: '认领与审核' },
  { key: 'system', label: '系统通知' },
  { key: 'dm', label: '私信' },
]
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
    ElMessage.error(e.message || '加载个人中心失败')
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
    ElMessage.success('通知已全部标为已读')
  } catch (e) { ElMessage.error(e.message || '操作失败') }
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
      map.set(m.fromUserId, { fromUserId: m.fromUserId, name: m.fromNickname || '对方', msgs: [] })
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
  if (!last) { ElMessage.warning('找不到可回复的消息'); return }
  sendingThread.value = true
  try {
    await api.replyMessage(last.id, v)
    c.msgs.push({ id: 'local-' + Date.now(), sent: true, content: v, createTime: new Date().toISOString().slice(0, 19) })
    threadInput.value = ''
  } catch (e) {
    ElMessage.error(e.message || '发送失败')
  } finally {
    sendingThread.value = false
  }
}

// P1：交付文件动作（桌面原生打开 / Web 受控下载；渲染层只经手 deliveryId）
async function takeDelivery(d) {
  try { await downloadDelivery(d) }
  catch (e) { ElMessage.error(e.message || '操作失败') }
}
async function removeDelivery(d) {
  try {
    await ElMessageBox.confirm(`删除「${d.title}」？对应文件将从磁盘一并移除。`, '删除交付文件', { type: 'warning' })
    await api.deleteDelivery(d.id)
    deliveries.value = deliveries.value.filter((x) => x.id !== d.id)
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

async function markFound(item) {
  try {
    await api.markFound(item.id)
    item.status = item.type === 'lost' ? '已找回' : '已认领'
    ElMessage.success('已标记为「已找回」，感谢你的反馈！')
  } catch (e) { ElMessage.error(e.message || '操作失败') }
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

const profileRules = {
  nickname: [
    { required: true, message: '昵称不能为空', trigger: 'blur' },
    { max: 12, message: '昵称最多 12 个字', trigger: 'blur' },
  ],
  phone: [{ max: 20, message: '手机号最多 20 位', trigger: 'blur' }],
}
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '新密码 6-32 位', trigger: 'blur' },
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_r, v, cb) => (v === pwdForm.value.newPassword ? cb() : cb(new Error('两次输入的新密码不一致'))),
      trigger: 'blur',
    },
  ],
}

async function saveProfile() {
  await profileRef.value.validate()
  try {
    const vo = await api.updateProfile({
      nickname: profileForm.value.nickname,
      phone: profileForm.value.phone,
      avatar: profileForm.value.avatar,
    })
    user.updateInfo(vo)
    ElMessage.success('资料已更新')
  } catch (e) { ElMessage.error(e.message || '保存失败') }
}

async function changePwd() {
  await pwdRef.value.validate()
  try {
    await api.changePassword({ oldPassword: pwdForm.value.oldPassword, newPassword: pwdForm.value.newPassword })
    user.logout()
    ElMessage.success('密码已修改，请使用新密码重新登录')
    router.push('/auth?redirect=/profile')
  } catch (e) { ElMessage.error(e.message || '修改失败') }
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
          <p>学号 {{ user.info.account }}<span v-if="user.info.role === 1"> · 管理员</span></p>
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
            <img :src="assetUrl(it.thumb || it.img)" alt="" @error="$event.target.style.opacity = 0" />
            <div class="mid">
              <RouterLink :to="`/item/${it.id}`" class="t">{{ it.title }}</RouterLink>
              <p>{{ it.location }} · {{ (it.time || '').slice(5, 16) }} · {{ it.views }} 次浏览</p>
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

        <!-- 我的收藏（E-07） -->
        <el-tab-pane name="fav">
          <template #label>我的收藏<el-badge v-if="favorites.length" :value="favorites.length" class="tab-badge" /></template>
          <div v-if="favorites.length" class="grid-cards">
            <ItemCard v-for="it in favorites" :key="'f' + it.id" :item="it" fav-initial />
          </div>
          <el-empty v-else description="还没有收藏任何启事——去失物墙点亮星标吧" />
        </el-tab-pane>

        <!-- 我的认领 -->
        <el-tab-pane label="我的认领" name="claim">
          <div v-for="c in myClaims" :key="'c' + c.claimId" class="row paper-card">
            <img :src="assetUrl(c.img)" alt="" @error="$event.target.style.opacity = 0" />
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
            <img :src="assetUrl(c.img)" alt="" @error="$event.target.style.opacity = 0" />
            <div class="mid">
              <RouterLink :to="`/item/${c.id}`" class="t">{{ c.title }}</RouterLink>
              <p class="who-line">申请人：{{ c.claimantName || '同学' }}（{{ c.claimantAccount }}）<span v-if="c.contact"> · {{ c.contact }}</span></p>
              <p class="proof">凭证：{{ c.proof }}</p>
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

        <!-- 消息通知（G 版式：通知中心三分栏 + 私信会话化；P1 顶部挂交付文件卡片） -->
        <el-tab-pane name="msg">
          <template #label>消息通知<el-badge v-if="unreadCount" :value="unreadCount" class="tab-badge" /></template>

          <!-- P1：交付文件（读 delivery 表渲染，与生成方解耦；桌面=原生打开，Web=受控下载） -->
          <template v-if="deliveries.length">
            <p class="deliv-head">交付文件</p>
            <div v-for="d in deliveries" :key="'d' + d.id" class="msg paper-card">
              <span class="d-icon">{{ DELIVERY_ICONS[d.bizType] || '📄' }}</span>
              <div class="msg-body">
                <b>{{ d.title }}</b>
                <p v-if="d.description">{{ d.description }}</p>
                <small>{{ fmtSize(d.fileSize) }} · {{ (d.createTime || '').replace('T', ' ').slice(0, 16) }}</small>
              </div>
              <el-button size="small" round type="primary" plain @click.stop="takeDelivery(d)">下载</el-button>
              <el-button size="small" round plain type="danger" @click.stop="removeDelivery(d)">删除</el-button>
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
            <button v-if="unreadCount" class="all-read" @click="readAllNow">全部标为已读</button>
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
              <el-button v-if="m.refId" size="small" round plain @click.stop="$router.push(`/item/${m.refId}`)">查看启事</el-button>
            </div>
            <el-empty
              v-if="!(msgSub === 'claim' ? claimMsgs : systemMsgs).length"
              :description="msgSub === 'claim' ? '暂无认领与审核消息' : '暂无系统通知'"
            />
          </template>

          <!-- 私信：会话列表 → 线程 -->
          <template v-else>
            <div v-if="!activeConv">
              <div v-for="c in conversations" :key="c.fromUserId" class="conv paper-card" @click="openConv(c)">
                <span class="c-avatar" :class="{ unread: c.unread }">{{ (c.name || '对')[0] }}</span>
                <div class="c-main">
                  <div class="c-top">
                    <b>{{ c.name }}</b>
                    <small>{{ (c.last.createTime || '').replace('T', ' ').slice(5, 16) }}</small>
                  </div>
                  <p class="c-last"><span v-if="c.unread" class="c-dot"></span>{{ c.last.content }}</p>
                </div>
                <span class="c-go">进入会话 →</span>
              </div>
              <el-empty v-if="!conversations.length" description="暂无私信会话——去启事详情页给对方发条私信吧" />
            </div>

            <div v-else class="thread paper-card">
              <div class="thread-head">
                <button class="thread-back" @click="closeConv">← 返回</button>
                <b>与 {{ activeConv.name }} 的会话</b>
                <el-button
                  v-if="activeConv.last?.refId" size="small" round plain
                  @click="$router.push(`/item/${activeConv.last.refId}`)"
                >查看关联启事</el-button>
              </div>
              <div class="thread-msgs">
                <div v-for="m in activeConv.msgs" :key="m.id" class="t-msg" :class="{ me: m.sent }">
                  <p>{{ m.content }}</p>
                  <small>{{ m.sent ? '我' : activeConv.name }} · {{ (m.createTime || '').replace('T', ' ').slice(5, 16) }}</small>
                </div>
              </div>
              <div class="thread-input">
                <el-input
                  v-model="threadInput" placeholder="输入回复…（回车发送）"
                  maxlength="200" @keyup.enter="sendThread"
                />
                <el-button type="primary" round :loading="sendingThread" @click="sendThread">发送</el-button>
              </div>
            </div>
          </template>
        </el-tab-pane>

        <!-- 账号设置（E-01） -->
        <el-tab-pane label="账号设置" name="account">
          <div class="account-grid">
            <el-form ref="profileRef" :model="profileForm" :rules="profileRules" label-width="72px" class="paper-card form-card">
              <h3>个人资料</h3>
              <el-form-item label="头像">
                <div class="avatar-pick">
                  <button
                    v-for="a in AVATARS" :key="a" type="button" class="avatar-opt"
                    :class="{ active: profileForm.avatar === a }" @click="profileForm.avatar = a">{{ a }}</button>
                </div>
              </el-form-item>
              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="profileForm.nickname" maxlength="12" show-word-limit />
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="profileForm.phone" maxlength="20" placeholder="选填，可用于认领联系" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" round @click="saveProfile">保存资料</el-button>
              </el-form-item>
            </el-form>

            <el-form ref="pwdRef" :model="pwdForm" :rules="pwdRules" label-width="72px" class="paper-card form-card">
              <h3>修改密码</h3>
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="pwdForm.oldPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-32 位" />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirm">
                <el-input v-model="pwdForm.confirm" type="password" show-password />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" round @click="changePwd">修改密码</el-button>
              </el-form-item>
              <p class="pwd-hint">修改成功后需要用新密码重新登录。</p>
            </el-form>
          </div>
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
.ntf-tab.on { background: var(--ink); color: #fff; }
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
.t-msg.me p { background: var(--orange); color: #fff; text-align: left; }
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
