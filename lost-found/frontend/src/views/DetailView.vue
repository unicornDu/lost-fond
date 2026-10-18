<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { catEmoji } from '../constants/catalog'
import { api } from '../api'
import { useUserStore } from '../stores/user'
import ItemCard from '../components/ItemCard.vue'
import PosterDialog from '../components/PosterDialog.vue'
import { assetUrl } from '../utils/asset'

/**
 * 启事详情。审查 2.26：多图缩略图条 + el-image 放大；"相似推荐"改调真接口 /items/{id}/similar。
 * 审查 2.5：认领弹窗加联系方式；审查 2.7：私信接真（发站内消息给发布者）。
 * 审查 2.31：加载失败给出错误态 + 重试；切换启事取消旧响应（reqSeq 竞态守卫）。
 */
const route = useRoute()
const router = useRouter()
const user = useUserStore()   // E-01：认领联系方式可快捷填入个人资料手机号

const item = ref(null)
const similar = ref([])
const loadError = ref('')
const mainImg = ref(0)
let reqSeq = 0

const claimVisible = ref(false)
const claimForm = ref({ proof: '', contact: '' })
const claimStep = ref(0)   // 0 核验凭证 → 1 联系方式（对齐整合版 v1 的分步认领）
const submitting = ref(false)
const posterVisible = ref(false)

const dmVisible = ref(false)
const dmText = ref('')
const dmSending = ref(false)

const typeText = computed(() => item.value?.type === 'lost' ? '寻物启事' : '招领启事')
const previewList = computed(() => (item.value?.images?.length ? item.value.images : (item.value?.img ? [item.value.img] : [])).map(assetUrl))   // assetUrl：桌面端 /uploads 转绝对

/* G 版式：认领进度时间线（启事详情直接带回本人认领状态，避免额外请求） */
const myClaim = computed(() => {
  const it = item.value
  if (!it || !it.myClaimStatus) return null
  return { claimStatus: it.myClaimStatus, reply: it.myClaimReply }
})
const CLAIM_TL_NODES = ['提交申请', '发布者审核', '交付确认', '已找回']
const claimStage = computed(() => {
  const c = myClaim.value
  if (!c) return null
  const done = item.value && (item.value.status === '已找回' || item.value.status === '已认领')
  if (c.claimStatus === '已拒绝') return { fill: 1 / 3, note: `申请已被发布者拒绝${c.reply ? '：' + c.reply : ''}`, states: ['done', 'rej', '', ''] }
  if (done) return { fill: 1, note: '物品已找回，本单闭环完成', states: ['done', 'done', 'done', 'done'] }
  if (c.claimStatus === '已通过') return { fill: 2 / 3, note: '审核已通过，请与发布者约定线下交付（建议公共场所）', states: ['done', 'done', 'now', ''] }
  return { fill: 1 / 3, note: '申请已送达发布者，等待审核', states: ['done', 'now', '', ''] }
})
/* 时间线填充：挂载时先钉在 0，再在下一帧过渡到目标宽度（对齐整合版的连线生长动效） */
const tlReady = ref(false)
const claimFillStyle = computed(() => {
  if (!claimStage.value) return {}
  if (!tlReady.value) return { width: '0%' }
  const f = claimStage.value.fill
  return { width: `calc(${f * 100}% - ${(f * 32).toFixed(1)}px)` }
})

/* 认领弹窗：分步核验（第一步只收集"只有失主才知道"的凭证） */
function openClaim() {
  claimStep.value = 0
  claimVisible.value = true
}
function nextClaimStep() {
  const v = claimForm.value.proof.trim()
  if (v.length < 6) { ElMessage.warning('请至少写 6 个字，描述只有失主才知道的特征'); return }
  claimStep.value = 1
}

async function load() {
  const id = route.params.id
  const seq = ++reqSeq
  loadError.value = ''
  try {
    const [detail, sim] = await Promise.all([api.itemDetail(id), api.similar(id, 3).catch(() => [])])
    if (seq !== reqSeq) return // 竞态守卫：只接受最新请求
    item.value = detail
    similar.value = sim
    mainImg.value = 0
    // 时间线：先归零再驱动过渡，让进度条"长"出来
    tlReady.value = false
    await nextTick()
    tlReady.value = true
  } catch (e) {
    if (seq !== reqSeq) return
    item.value = null
    loadError.value = e.message || '加载失败'
  }
}

async function claim() {
  if (!claimForm.value.proof.trim()) { ElMessage.warning('请描述物品特征作为认领凭证'); return }
  submitting.value = true
  try {
    await api.applyClaim(item.value.id, { proof: claimForm.value.proof.trim(), contact: claimForm.value.contact.trim() || undefined })
    claimVisible.value = false
    claimStep.value = 0
    claimForm.value = { proof: '', contact: '' }
    ElMessage.success('认领申请已提交，等待发布者审核')
    await load()
  } catch (e) { ElMessage.error(e.message || '提交失败') }
  finally { submitting.value = false }
}

async function sendDm() {
  if (!dmText.value.trim()) { ElMessage.warning('请输入私信内容'); return }
  dmSending.value = true
  try {
    await api.dm(item.value.id, dmText.value.trim())
    dmVisible.value = false
    dmText.value = ''
    ElMessage.success('私信已发送，对方可在消息中心查看')
  } catch (e) { ElMessage.error(e.message || '发送失败') }
  finally { dmSending.value = false }
}

onBeforeUnmount(() => { reqSeq++ }) // 卸载后丢弃在途响应
watch(() => route.params.id, load)
onMounted(load)
</script>

<template>
  <div class="page container">
    <!-- 错误态 + 重试（审查 2.31） -->
    <el-empty v-if="loadError" :description="loadError">
      <el-button type="primary" round @click="load">重试</el-button>
      <el-button round plain @click="router.push('/wall')">回失物墙</el-button>
    </el-empty>

    <template v-else-if="item">
      <el-button round plain size="small" @click="router.back()">← 返回</el-button>

      <div class="detail paper-card" v-reveal>
        <div class="left">
          <div class="main-img">
            <div class="img-fallback">{{ catEmoji(item.category) }}</div>
            <el-image v-if="previewList.length" :src="previewList[mainImg]" :preview-src-list="previewList"
              :initial-index="mainImg" fit="cover" class="full" preview-teleported />
          </div>
          <!-- 多图缩略图条（审查 2.26） -->
          <div v-if="previewList.length > 1" class="thumbs">
            <img v-for="(u, i) in previewList" :key="i" :src="u" :class="{ on: i === mainImg }" @click="mainImg = i" />
          </div>
          <div class="desc-block">
            <h3>详细描述</h3>
            <p>{{ item.desc }}</p>
          </div>
        </div>

        <div class="right">
          <div class="status-line">
            <el-tag :type="item.type === 'lost' ? 'warning' : 'success'" size="large" effect="dark" round>{{ typeText }}</el-tag>
            <el-tag type="info" round>{{ item.status }}</el-tag>
          </div>
          <h1 class="d-title">{{ item.title }}</h1>
          <ul class="info-list">
            <li><span>📍 地点</span><b>{{ item.location }}</b></li>
            <li><span>⏰ 时间</span><b>{{ item.time }}</b></li>
            <li><span>🙋 发布人</span><b>
              <!-- E-04：发布人可点击进入其公开主页 -->
              <RouterLink v-if="item.userId" :to="`/user/${item.userId}`" class="owner-link">{{ item.owner }}</RouterLink>
              <template v-else>{{ item.owner }}</template>
            </b></li>
            <li><span>👀 浏览</span><b>{{ item.views }} 次</b></li>
          </ul>
          <div class="actions">
            <el-button type="primary" size="large" round @click="openClaim">
              {{ item.type === 'lost' ? '我捡到了，联系Ta' : '这是我的，申请认领' }}
            </el-button>
            <el-button size="large" round plain @click="dmVisible = true">私信</el-button>
            <el-button size="large" round text @click="posterVisible = true">
              <svg class="act-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 12v7a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-7"/><path d="M12 15V3M7 8l5-5 5 5"/></svg>
              分享海报
            </el-button>
          </div>
          <div class="safe-tip">🛡️ 平台提示：认领需发布者核实凭证，请勿向陌生人转账；线下交接建议在公共场所。</div>
        </div>
      </div>

      <!-- G 版式：认领进度时间线（本人提交过认领时展示） -->
      <div v-if="myClaim && claimStage" class="claim-tl paper-card" v-reveal="{ delay: 120 }">
        <div class="tl-head">
          <h3>认领进度</h3>
          <p>{{ claimStage.note }}</p>
        </div>
        <div class="tl">
          <div class="tl-fill" :style="claimFillStyle"></div>
          <div v-for="(label, i) in CLAIM_TL_NODES" :key="label" class="tl-node" :class="claimStage.states[i]">
            <span class="tl-dot">{{ claimStage.states[i] === 'done' ? '✓' : i + 1 }}</span>
            <span>{{ claimStage.states[i] === 'rej' ? '已拒绝' : label }}</span>
          </div>
        </div>
      </div>

      <div class="section-head">
        <h2>视觉上相似 <em>（pHash 最近邻）</em></h2>
        <span class="more" @click="router.push('/search-image')">上传照片精确匹配 →</span>
      </div>
      <div v-if="similar.length" class="grid-cards">
        <ItemCard v-for="it in similar" :key="it.id" :item="it" />
      </div>
      <el-empty v-else description="暂无视觉相似的启事" :image-size="80" />

      <!-- 认领弹窗：两步核验（审查 2.5 凭证 + G 版分步体验） -->
      <el-dialog v-model="claimVisible" title="申请认领" width="480px" @closed="claimStep = 0">
        <el-steps :active="claimStep" finish-status="success" align-center class="claim-steps">
          <el-step title="核验凭证" description="只有失主知道的细节" />
          <el-step title="联系方式" description="便于发布者联系你" />
        </el-steps>

        <template v-if="claimStep === 0">
          <p class="claim-tip">请描述 <b>只有失主才知道</b> 的特征（如贴纸、划痕、内容物），发布者会据此核实，防止冒领：</p>
          <el-input v-model="claimForm.proof" type="textarea" :rows="4" maxlength="200" show-word-limit
            placeholder="例：杯盖内侧有一道裂纹，杯底贴着我的姓名贴……" />
          <p class="claim-note">这条描述就是发布者审核的依据，写得越具体，通过越快。</p>
        </template>

        <template v-else>
          <p class="claim-tip">联系方式（选填，便于发布者联系你安排交付）：</p>
          <div class="contact-row">
            <el-input v-model="claimForm.contact" maxlength="50" placeholder="手机号 / 微信号 / 宿舍号" />
            <el-button v-if="user.info?.phone" round plain size="small" @click="claimForm.contact = user.info.phone">用我的手机号</el-button>
          </div>
          <div class="claim-recap">
            <span>已填写的凭证</span>
            <p>{{ claimForm.proof }}</p>
          </div>
        </template>

        <template #footer>
          <el-button round @click="claimVisible = false">取消</el-button>
          <el-button v-if="claimStep === 1" round plain @click="claimStep = 0">上一步</el-button>
          <el-button v-if="claimStep === 0" type="primary" round @click="nextClaimStep">下一步</el-button>
          <el-button v-else type="primary" round :loading="submitting" @click="claim">提交申请</el-button>
        </template>
      </el-dialog>

      <!-- 私信弹窗（审查 2.7：接真） -->
      <el-dialog v-model="dmVisible" title="给发布者发私信" width="440px">
        <el-input v-model="dmText" type="textarea" :rows="4" maxlength="200" show-word-limit
          placeholder="想对发布者说的话…" />
        <template #footer>
          <el-button round @click="dmVisible = false">取消</el-button>
          <el-button type="primary" round :loading="dmSending" @click="sendDm">发送</el-button>
        </template>
      </el-dialog>

      <!-- 分享海报（canvas 导出 PNG） -->
      <PosterDialog v-model="posterVisible" :item="item" />
    </template>

    <el-empty v-else description="启事不存在或已被删除">
      <el-button type="primary" round @click="router.push('/')">回到首页</el-button>
    </el-empty>
  </div>
</template>

<style scoped>
.detail { display: grid; grid-template-columns: 1.05fr .95fr; gap: 36px; margin-top: 16px; padding: 30px; }
.main-img { position: relative; border-radius: 16px; overflow: hidden; aspect-ratio: 4 / 3; background: var(--cream-deep); }
.main-img .full { position: absolute; inset: 0; width: 100%; height: 100%; }
.img-fallback { position: absolute; inset: 0; display: grid; place-items: center; font-size: 80px; background: linear-gradient(135deg, var(--orange-soft), var(--honey-soft)); }
.thumbs { display: flex; gap: 8px; margin-top: 10px; }
.thumbs img { width: 64px; height: 48px; object-fit: cover; border-radius: 8px; cursor: pointer; border: 2px solid transparent; opacity: .75; }
.thumbs img.on { border-color: var(--orange); opacity: 1; }
.desc-block { margin-top: 20px; }
.desc-block h3 { font-size: 18px; margin-bottom: 10px; }
.desc-block p { color: var(--ink-soft); line-height: 1.9; font-size: 14.5px; }
.status-line { display: flex; gap: 10px; margin-bottom: 14px; }
.d-title { font-size: 28px; line-height: 1.4; margin-bottom: 18px; }
.info-list { list-style: none; padding: 0; margin: 0 0 24px; }
.info-list li { display: flex; justify-content: space-between; padding: 11px 4px; border-bottom: 1px dashed var(--line); font-size: 14px; }
.info-list li span { color: var(--ink-soft); }
.info-list li b { font-weight: 600; }
.owner-link { color: var(--orange-deep); }
.owner-link:hover { text-decoration: underline; }
.actions { display: flex; gap: 12px; margin-bottom: 18px; flex-wrap: wrap; }
.safe-tip { padding: 10px 14px; border-radius: 12px; background: var(--sky-soft); color: #3D6D99; font-size: 12.5px; line-height: 1.7; }
.claim-tip { color: var(--ink-soft); font-size: 13.5px; line-height: 1.7; margin-bottom: 12px; }
.contact-row { display: flex; gap: 10px; align-items: center; }
@media (max-width: 860px) { .detail { grid-template-columns: 1fr; } }

/* G 版式：认领进度时间线卡 */
.claim-tl { margin-top: 18px; padding: 22px 26px; }
.tl-head { display: flex; align-items: baseline; justify-content: space-between; gap: 14px; margin-bottom: 16px; flex-wrap: wrap; }
.tl-head h3 { font-size: 16px; }
.tl-head p { font-size: 13px; color: var(--ink-soft); }

/* 认领分步 */
.claim-steps { margin-bottom: 20px; }
.claim-note { color: var(--ink-faint); font-size: 12px; line-height: 1.7; margin-top: 10px; }
.claim-recap {
  margin-top: 16px; padding: 12px 14px; border-radius: 10px;
  background: var(--cream-deep);
}
.claim-recap span { font-size: 11.5px; color: var(--ink-faint); font-weight: 700; letter-spacing: .5px; }
.claim-recap p { margin-top: 5px; font-size: 13px; color: var(--ink-soft); line-height: 1.7; }
.act-ico { width: 14px; height: 14px; margin-right: 5px; }
</style>
