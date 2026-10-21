<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { catEmoji, catDot } from '../constants/catalog'
import { api } from '../api'
import { useUserStore } from '../stores/user'
import ItemCard from '../components/ItemCard.vue'
import PosterDialog from '../components/PosterDialog.vue'
import LocationBlock from '../components/LocationBlock.vue'
import { assetUrl } from '../utils/asset'
import { t, catLabel } from '../i18n'
import { avatarColor, initialOf, statusOf } from '../utils/display'

/**
 * 启事详情（v3 Digg 详情版式）：返回 pill → 白卡（分类行 + 大标题 + 描述 + 2.1:1 大图
 * + 三格事实卡 + 发布者行 + 操作行）→ 认领进度时间线 → 认领流程步骤卡 → 相似启事。
 * 审查 2.26：多图缩略图条 + el-image 放大；"相似推荐"改调真接口 /items/{id}/similar。
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
const locRef = ref(null)   // v2.41.1：位置区块（方案 A 展开卡）

const dmVisible = ref(false)
const dmText = ref('')
const dmSending = ref(false)

const typeText = computed(() => item.value?.type === 'lost' ? t('detail.type.lost') : t('detail.type.found'))
const previewList = computed(() => (item.value?.images?.length ? item.value.images : (item.value?.img ? [item.value.img] : [])).map(assetUrl))   // assetUrl：桌面端 /uploads 转绝对
const dot = computed(() => catDot(item.value?.category))
const status = computed(() => statusOf(item.value))

/* 认领进度时间线（启事详情直接带回本人认领状态，避免额外请求） */
const myClaim = computed(() => {
  const it = item.value
  if (!it || !it.myClaimStatus) return null
  return { claimStatus: it.myClaimStatus, reply: it.myClaimReply }
})
const CLAIM_TL_NODES = computed(() => [
  t('detail.tl.1'), t('detail.tl.2'), t('detail.tl.3'), t('detail.tl.4'),
])
const claimStage = computed(() => {
  const c = myClaim.value
  if (!c) return null
  const done = item.value && (item.value.status === '已找回' || item.value.status === '已认领')
  if (c.claimStatus === '已拒绝') return { fill: 1 / 3, note: t('detail.progress.rejected', { reply: c.reply ? t('detail.progress.reply', { reply: c.reply }) : '' }), states: ['done', 'rej', '', ''] }
  if (done) return { fill: 1, note: t('detail.progress.done'), states: ['done', 'done', 'done', 'done'] }
  if (c.claimStatus === '已通过') return { fill: 2 / 3, note: t('detail.progress.approved'), states: ['done', 'done', 'now', ''] }
  return { fill: 1 / 3, note: t('detail.progress.pending'), states: ['done', 'now', '', ''] }
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
  if (v.length < 6) { ElMessage.warning(t('detail.claim.warn')); return }
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
    loadError.value = e.message || t('posts.loadFail')
  }
}

async function claim() {
  if (!claimForm.value.proof.trim()) { ElMessage.warning(t('detail.claim.needProof')); return }
  submitting.value = true
  try {
    await api.applyClaim(item.value.id, { proof: claimForm.value.proof.trim(), contact: claimForm.value.contact.trim() || undefined })
    claimVisible.value = false
    claimStep.value = 0
    claimForm.value = { proof: '', contact: '' }
    ElMessage.success(t('detail.claim.ok'))
    await load()
  } catch (e) { ElMessage.error(e.message || t('detail.claim.fail')) }
  finally { submitting.value = false }
}

async function sendDm() {
  if (!dmText.value.trim()) { ElMessage.warning(t('detail.dm.warn')); return }
  dmSending.value = true
  try {
    await api.dm(item.value.id, dmText.value.trim())
    dmVisible.value = false
    dmText.value = ''
    ElMessage.success(t('detail.dm.ok'))
  } catch (e) { ElMessage.error(e.message || t('detail.dm.fail')) }
  finally { dmSending.value = false }
}

onBeforeUnmount(() => { reqSeq++ }) // 卸载后丢弃在途响应
watch(() => route.params.id, load)
onMounted(load)
</script>

<template>
  <div class="detail-page">
    <!-- 错误态 + 重试（审查 2.31） -->
    <div v-if="loadError" class="state">
      <p>{{ loadError }}</p>
      <div class="state-btns">
        <button class="seg" @click="load">{{ $t('common.retry') }}</button>
        <button class="seg mut" @click="router.push('/wall')">{{ $t('detail.backWall') }}</button>
      </div>
    </div>

    <template v-else-if="item">
      <button class="back" type="button" @click="router.back()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><path d="M19 12H5m6-7-7 7 7 7" /></svg>
        {{ $t('detail.back') }}
      </button>

      <article class="d-card" v-reveal>
        <div class="cat-row">
          <i class="dot" :style="{ background: dot }"></i>
          <span class="cat">{{ catLabel(item.category) }}</span>
          <span class="cat-type">{{ typeText }}</span>
          <span class="d-status" :class="status.cls">{{ $t(status.key) }}</span>
        </div>

        <h1 class="d-title">{{ item.title }}</h1>
        <p v-if="item.desc" class="d-desc">{{ item.desc }}</p>

        <!-- 大图（2.1:1）+ 多图缩略条 -->
        <div class="d-photo">
          <div class="ph-fallback">{{ catEmoji(item.category) }}</div>
          <el-image v-if="previewList.length" :src="previewList[mainImg]" :preview-src-list="previewList"
            :initial-index="mainImg" fit="cover" class="ph-img" preview-teleported />
          <button v-if="item.location" class="ph-loc" type="button"
            :title="$t('detail.locLink')" @click="locRef?.open()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><path d="M12 21s-7-5.5-7-11a7 7 0 1 1 14 0c0 5.5-7 11-7 11z" /><circle cx="12" cy="10" r="2.5" /></svg>
            {{ item.location }}
          </button>
        </div>
        <div v-if="previewList.length > 1" class="thumbs">
          <img v-for="(u, i) in previewList" :key="i" :src="u" :class="{ on: i === mainImg }" alt="" @click="mainImg = i" />
        </div>

        <!-- 三格事实卡 -->
        <div class="facts">
          <div class="fact"><span>{{ $t('detail.fact.loc') }}</span><b>{{ item.location }}</b></div>
          <div class="fact"><span>{{ $t('detail.fact.time') }}</span><b>{{ item.time }}</b></div>
          <div class="fact"><span>{{ $t('detail.fact.views') }}</span><b>{{ $t('detail.fact.times', { n: item.views ?? 0 }) }}</b></div>
        </div>

        <!-- 位置（v2.41.1）：prompt 同款展开卡 + 真实坐标 -->
        <LocationBlock ref="locRef" :item="item" />

        <!-- 发布者行 -->
        <div class="d-meta">
          <span class="ava" :style="{ background: avatarColor(item.owner) }">{{ initialOf(item.owner) }}</span>
          <b>{{ item.owner }}</b>
          <RouterLink v-if="item.userId" :to="`/user/${item.userId}`" class="owner-link">{{ $t('detail.owner.home') }}</RouterLink>
          <span class="meta-type">{{ item.type === 'lost' ? $t('detail.owner.lost') : $t('detail.owner.found') }}</span>
        </div>

        <!-- 操作行 -->
        <div class="d-actions">
          <button class="btn-primary" type="button" @click="openClaim">
            {{ item.type === 'lost' ? $t('detail.claimLost') : $t('detail.claimFound') }}
          </button>
          <button class="btn-ghost" type="button" @click="dmVisible = true">{{ $t('detail.dm') }}</button>
          <button class="btn-ghost" type="button" @click="posterVisible = true">{{ $t('detail.poster') }}</button>
        </div>

        <p class="safe-tip">{{ $t('detail.safe') }}</p>
      </article>

      <!-- 认领进度时间线（本人提交过认领时展示） -->
      <div v-if="myClaim && claimStage" class="claim-tl" v-reveal="{ delay: 80 }">
        <div class="tl-head">
          <h3>{{ $t('detail.progress') }}</h3>
          <p>{{ claimStage.note }}</p>
        </div>
        <div class="tl">
          <div class="tl-fill" :style="claimFillStyle"></div>
          <div v-for="(label, i) in CLAIM_TL_NODES" :key="label" class="tl-node" :class="claimStage.states[i]">
            <span class="tl-dot">{{ claimStage.states[i] === 'done' ? '✓' : i + 1 }}</span>
            <span>{{ claimStage.states[i] === 'rej' ? $t('claim.rejected') : label }}</span>
          </div>
        </div>
      </div>

      <!-- 认领流程（静态说明，Digg 详情页第四块） -->
      <section class="steps" v-reveal="{ delay: 100 }">
        <h4>{{ $t('detail.steps.title') }}</h4>
        <div class="step">
          <i>1</i>
          <div><b>{{ $t('detail.steps.1.t') }}</b><span>{{ $t('detail.steps.1.d') }}</span></div>
        </div>
        <div class="step">
          <i>2</i>
          <div><b>{{ $t('detail.steps.2.t') }}</b><span>{{ $t('detail.steps.2.d') }}</span></div>
        </div>
        <div class="step">
          <i>3</i>
          <div><b>{{ $t('detail.steps.3.t') }}</b><span>{{ $t('detail.steps.3.d') }}</span></div>
        </div>
      </section>

      <!-- 相似启事 -->
      <div class="section-head">
        <h2>{{ $t('detail.similar.title') }} <em>{{ $t('detail.similar.tag') }}</em></h2>
        <span class="more" @click="router.push('/search-image')">{{ $t('detail.similar.more') }}</span>
      </div>
      <div v-if="similar.length" class="grid-cards">
        <ItemCard v-for="it in similar" :key="it.id" :item="it" />
      </div>
      <p v-else class="empty-line">{{ $t('detail.similar.empty') }}</p>

      <!-- 认领弹窗：两步核验（审查 2.5 凭证 + 分步体验） -->
      <el-dialog v-model="claimVisible" :title="$t('detail.claim.title')" width="480px" @closed="claimStep = 0">
        <el-steps :active="claimStep" finish-status="success" align-center class="claim-steps">
          <el-step :title="$t('detail.claim.step1')" :description="$t('detail.claim.step1d')" />
          <el-step :title="$t('detail.claim.step2')" :description="$t('detail.claim.step2d')" />
        </el-steps>

        <template v-if="claimStep === 0">
          <p class="claim-tip" v-html="$t('detail.claim.tip')"></p>
          <el-input v-model="claimForm.proof" type="textarea" :rows="4" maxlength="200" show-word-limit
            :placeholder="$t('detail.claim.ph')" />
          <p class="claim-note">{{ $t('detail.claim.note') }}</p>
        </template>

        <template v-else>
          <p class="claim-tip">{{ $t('detail.claim.contact') }}</p>
          <div class="contact-row">
            <el-input v-model="claimForm.contact" maxlength="50" :placeholder="$t('detail.claim.contactPh')" />
            <el-button v-if="user.info?.phone" round plain size="small" @click="claimForm.contact = user.info.phone">{{ $t('detail.claim.phone') }}</el-button>
          </div>
          <div class="claim-recap">
            <span>{{ $t('detail.claim.recap') }}</span>
            <p>{{ claimForm.proof }}</p>
          </div>
        </template>

        <template #footer>
          <el-button round @click="claimVisible = false">{{ $t('common.cancel') }}</el-button>
          <el-button v-if="claimStep === 1" round plain @click="claimStep = 0">{{ $t('detail.claim.prev') }}</el-button>
          <el-button v-if="claimStep === 0" type="primary" round @click="nextClaimStep">{{ $t('detail.claim.next') }}</el-button>
          <el-button v-else type="primary" round :loading="submitting" @click="claim">{{ $t('detail.claim.submit') }}</el-button>
        </template>
      </el-dialog>

      <!-- 私信弹窗（审查 2.7：接真） -->
      <el-dialog v-model="dmVisible" :title="$t('detail.dm.title')" width="440px">
        <el-input v-model="dmText" type="textarea" :rows="4" maxlength="200" show-word-limit
          :placeholder="$t('detail.dm.ph')" />
        <template #footer>
          <el-button round @click="dmVisible = false">{{ $t('common.cancel') }}</el-button>
          <el-button type="primary" round :loading="dmSending" @click="sendDm">{{ $t('detail.dm.send') }}</el-button>
        </template>
      </el-dialog>

      <!-- 分享海报（canvas 导出 PNG） -->
      <PosterDialog v-model="posterVisible" :item="item" />
    </template>

    <div v-else class="state">
      <p>{{ $t('detail.missing') }}</p>
      <button class="seg" @click="router.push('/home')">{{ $t('detail.home') }}</button>
    </div>
  </div>
</template>

<style scoped>
.detail-page { max-width: var(--detail-max, 760px); }

.back {
  display: inline-flex; align-items: center; gap: 7px;
  border: 0; background: transparent; cursor: pointer;
  font-family: inherit; font-size: 13px; font-weight: 600; color: var(--ink-soft);
  padding: 9px 14px 9px 10px; border-radius: 999px;
  margin-bottom: 16px;
  transition: background .16s, color .16s;
}
.back:hover { background: var(--cream-deep); color: var(--ink); }
.back svg { width: 15px; height: 15px; }

.d-card {
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 24px 26px 22px;
}
.cat-row { display: flex; align-items: center; gap: 7px; margin-bottom: 10px; }
.cat { font-size: 12px; font-weight: 700; color: var(--ink-soft); }
.cat-type { font-size: 11.5px; color: var(--ink-faint); }
.d-status { margin-left: auto; font-size: 11px; font-weight: 700; padding: 5px 10px; border-radius: 999px; }

.d-title { font-size: 26px; line-height: 1.35; letter-spacing: -.015em; margin-bottom: 12px; }
.d-desc { font-size: 14px; line-height: 1.8; color: var(--ink-soft); margin-bottom: 18px; white-space: pre-wrap; }

.d-photo {
  position: relative; overflow: hidden;
  aspect-ratio: 2.1 / 1;
  border-radius: var(--radius-sm);
  background: var(--cream-deep);
  display: grid; place-items: center;
}
.ph-fallback { font-size: 64px; }
.ph-img { position: absolute; inset: 0; width: 100%; height: 100%; }
.ph-loc {
  position: absolute; left: 12px; bottom: 12px; z-index: 2;
  display: inline-flex; align-items: center; gap: 5px;
  padding: 7px 11px; border-radius: 999px;
  background: rgba(16, 16, 14, .55); backdrop-filter: blur(6px);
  border: 1px solid rgba(255, 255, 255, .16);
  color: #fff; font-family: inherit; font-size: 11.5px; font-weight: 600;
  cursor: pointer;
  transition: background-color .18s ease, border-color .18s ease, transform .18s ease;
}
.ph-loc:hover { background: rgba(16, 16, 14, .74); border-color: rgba(255, 255, 255, .3); transform: translateY(-1px); }
.ph-loc:focus-visible { outline: 2px solid rgba(255, 255, 255, .8); outline-offset: 2px; }
.ph-loc svg { width: 11px; height: 11px; }
.thumbs { display: flex; gap: 8px; margin-top: 10px; flex-wrap: wrap; }
.thumbs img {
  width: 62px; height: 46px; object-fit: cover; border-radius: 9px;
  cursor: pointer; border: 1.5px solid transparent; opacity: .65;
  transition: opacity .16s, border-color .16s;
}
.thumbs img.on { border-color: var(--ink); opacity: 1; }

.facts { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin: 20px 0 18px; }
.fact { background: var(--cream); border: 1px solid var(--line); border-radius: 12px; padding: 12px 14px; }
.fact span { display: block; font-size: 10.5px; font-weight: 600; color: var(--ink-faint); margin-bottom: 6px; }
.fact b { font-size: 13px; font-weight: 700; line-height: 1.4; }

.d-meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 18px; }
.ava {
  width: 26px; height: 26px; border-radius: 50%;
  display: grid; place-items: center; color: #fff;
  font-size: 11px; font-weight: 700;
}
.d-meta b { font-size: 13px; font-weight: 700; }
.owner-link { font-size: 12px; color: var(--ink-soft); text-decoration: underline; text-underline-offset: 2px; }
.meta-type { font-size: 11.5px; color: var(--ink-faint); }

.d-actions { display: flex; gap: 10px; flex-wrap: wrap; padding-top: 18px; border-top: 1px solid var(--line); }
.btn-primary {
  background: var(--ink); color: var(--paper); border: 0;
  font-family: inherit; font-size: 13.5px; font-weight: 700;
  padding: 13px 22px; border-radius: 999px; cursor: pointer;
  transition: transform .18s, box-shadow .18s;
}
.btn-primary:hover { transform: translateY(-1px); box-shadow: 0 8px 20px rgba(20, 20, 18, .2); }
.btn-ghost {
  background: var(--paper); color: var(--ink);
  border: 1px solid var(--line-2);
  font-family: inherit; font-size: 13.5px; font-weight: 700;
  padding: 13px 20px; border-radius: 999px; cursor: pointer;
  transition: border-color .18s, background .18s;
}
.btn-ghost:hover { border-color: var(--ink-faint); background: var(--cream); }

.safe-tip { margin-top: 14px; font-size: 11.5px; line-height: 1.7; color: var(--ink-faint); }

/* 认领进度 */
.claim-tl {
  margin-top: 18px; padding: 20px 24px;
  background: var(--paper); border: 1px solid var(--line); border-radius: var(--radius);
}
.tl-head { display: flex; align-items: baseline; justify-content: space-between; gap: 14px; margin-bottom: 16px; flex-wrap: wrap; }
.tl-head h3 { font-size: 15px; }
.tl-head p { font-size: 12.5px; color: var(--ink-soft); }

/* 认领流程 */
.steps {
  margin-top: 18px; padding: 20px 24px;
  background: var(--paper); border: 1px solid var(--line); border-radius: var(--radius);
}
.steps h4 { font-size: 13.5px; font-weight: 800; margin-bottom: 16px; }
.step { display: flex; gap: 13px; padding-bottom: 18px; position: relative; }
.step:last-child { padding-bottom: 2px; }
.step::before { content: ''; position: absolute; left: 10px; top: 24px; bottom: 2px; width: 2px; background: var(--line); }
.step:last-child::before { display: none; }
.step i {
  width: 22px; height: 22px; border-radius: 50%; flex: none;
  display: grid; place-items: center;
  font-style: normal; font-size: 11px; font-weight: 800;
  background: var(--cream-deep); color: var(--ink-soft);
  position: relative; z-index: 1;
}
.step b { display: block; font-size: 13px; font-weight: 700; line-height: 1.4; }
.step span { display: block; margin-top: 3px; font-size: 12px; line-height: 1.6; color: var(--ink-faint); }

.state { padding: 70px 0; text-align: center; color: var(--ink-faint); font-size: 13px; }
.state-btns { display: flex; gap: 10px; justify-content: center; margin-top: 14px; }
.empty-line { padding: 24px 0 8px; text-align: center; font-size: 12.5px; color: var(--ink-faint); }

/* 弹窗内小件 */
.claim-steps { margin-bottom: 20px; }
.claim-tip { color: var(--ink-soft); font-size: 13.5px; line-height: 1.7; margin-bottom: 12px; }
.claim-note { color: var(--ink-faint); font-size: 12px; line-height: 1.7; margin-top: 10px; }
.contact-row { display: flex; gap: 10px; align-items: center; }
.claim-recap { margin-top: 16px; padding: 12px 14px; border-radius: 10px; background: var(--cream-deep); }
.claim-recap span { font-size: 11px; color: var(--ink-faint); font-weight: 700; letter-spacing: .5px; }
.claim-recap p { margin-top: 5px; font-size: 13px; color: var(--ink-soft); line-height: 1.7; }

@media (max-width: 560px) {
  .d-card { padding: 18px 16px; }
  .d-title { font-size: 21px; }
  .facts { grid-template-columns: 1fr; }
  .d-photo { aspect-ratio: 1.6 / 1; }
}
</style>
