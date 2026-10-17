<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import { api } from '../api'
import { IS_DESKTOP, assetUrl } from '../utils/desktop'   // 桌面端 /uploads 转绝对地址
import { openOrDownload, revealInFolder, DELIVERY_ICONS, fmtSize } from '../utils/delivery'
import ItemCard from '../components/ItemCard.vue'

/**
 * 个人中心。审查 2.5：收到的申请展示申请人/凭证/联系方式（杜绝盲审）。
 * 审查 2.7：新增"消息通知"tab，进入即 readAll 清红点，单条点击 readOne。
 * E-01：新增"账号设置"tab（资料编辑/修改密码）；E-03：消息支持回复私信。
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

// P1：交付文件动作（桌面原生打开 / Web 受控下载；渲染层只经手 deliveryId）
async function takeDelivery(d) {
  try { await openOrDownload(d) }
  catch (e) { ElMessage.error(e.message || '操作失败') }
}
async function revealDelivery(d) {
  try { await revealInFolder(d) }
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

// E-03：消息中心回复私信
async function replyDm(m) {
  try {
    const r = await ElMessageBox.prompt('回复对方（最多 200 字）', `回复 ${m.fromNickname || '对方'}`, {
      confirmButtonText: '发送回复', cancelButtonText: '取消', inputPlaceholder: '输入回复内容',
      inputValidator: (v) => (v && v.trim() ? true : '回复内容不能为空'),
    })
    await api.replyMessage(m.id, r.value.trim())
    ElMessage.success('回复已发送')
    await loadAll()
  } catch (e) {
    if (e === 'cancel' || e?.message === 'cancel') return
    ElMessage.error(e.message || '回复失败')
  }
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
            <img :src="assetUrl(it.thumb || it.img)" alt="" @error="$event.target.style.opacity = 0" />
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

        <!-- 消息通知（审查 2.7；E-03 支持 dm 回复；P1 顶部挂交付文件卡片） -->
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
              <el-button size="small" round type="primary" plain @click.stop="takeDelivery(d)">{{ IS_DESKTOP ? '打开' : '下载' }}</el-button>
              <el-button v-if="IS_DESKTOP" size="small" round plain @click.stop="revealDelivery(d)">所在位置</el-button>
              <el-button size="small" round plain type="danger" @click.stop="removeDelivery(d)">删除</el-button>
            </div>
          </template>
          <div v-for="m in messages" :key="m.id" class="msg paper-card" :class="{ unread: m.isRead === 0 }" @click="readOne(m)">
            <span class="dot" v-if="m.isRead === 0"></span>
            <div class="msg-body">
              <b>{{ m.title }}</b>
              <p v-if="m.type === 'dm' && m.fromNickname" class="dm-from">来自：{{ m.fromNickname }}</p>
              <p>{{ m.content }}</p>
              <small>{{ (m.createTime || '').replace('T', ' ').slice(0, 16) }}</small>
            </div>
            <el-button v-if="m.type === 'dm' && m.fromNickname" size="small" round plain type="primary" @click.stop="replyDm(m)">回复</el-button>
            <el-button v-if="m.refId" size="small" round plain @click.stop="$router.push(`/item/${m.refId}`)">查看启事</el-button>
          </div>
          <el-empty v-if="!messages.length" description="暂无消息" />
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
.dm-from { color: var(--orange-deep); font-size: 12px !important; margin-top: 2px !important; }
.deliv-head { color: var(--ink-faint); font-size: 12px; margin: 0 0 8px 4px; }
.d-icon { display: grid; place-items: center; width: 40px; height: 40px; border-radius: 12px; background: var(--honey-soft); font-size: 20px; flex-shrink: 0; }

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
