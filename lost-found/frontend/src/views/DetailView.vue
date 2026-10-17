<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { catEmoji } from '../constants/catalog'
import { api } from '../api'
import { useUserStore } from '../stores/user'
import ItemCard from '../components/ItemCard.vue'
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
const submitting = ref(false)

const dmVisible = ref(false)
const dmText = ref('')
const dmSending = ref(false)

const typeText = computed(() => item.value?.type === 'lost' ? '寻物启事' : '招领启事')
const previewList = computed(() => (item.value?.images?.length ? item.value.images : (item.value?.img ? [item.value.img] : [])).map(assetUrl))   // assetUrl：桌面端 /uploads 转绝对

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

      <div class="detail paper-card">
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
            <el-button type="primary" size="large" round @click="claimVisible = true">
              {{ item.type === 'lost' ? '我捡到了，联系Ta' : '这是我的，申请认领' }}
            </el-button>
            <el-button size="large" round plain @click="dmVisible = true">私信</el-button>
          </div>
          <div class="safe-tip">🛡️ 平台提示：认领需发布者核实凭证，请勿向陌生人转账；线下交接建议在公共场所。</div>
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

      <!-- 认领弹窗（审查 2.5：凭证 + 联系方式） -->
      <el-dialog v-model="claimVisible" title="申请认领" width="460px">
        <p class="claim-tip">请描述 <b>只有失主才知道</b> 的特征（如贴纸、划痕、内容物），供发布者核实，防止冒领：</p>
        <el-input v-model="claimForm.proof" type="textarea" :rows="4" maxlength="200" show-word-limit
          placeholder="例：杯盖内侧有一道裂纹，杯底贴着我的姓名贴……" />
        <p class="claim-tip" style="margin-top:12px">联系方式（选填，便于发布者联系你）：</p>
        <div class="contact-row">
          <el-input v-model="claimForm.contact" maxlength="50" placeholder="手机号 / 微信号 / 宿舍号" />
          <el-button v-if="user.info?.phone" round plain size="small" @click="claimForm.contact = user.info.phone">用我的手机号</el-button>
        </div>
        <template #footer>
          <el-button round @click="claimVisible = false">取消</el-button>
          <el-button type="primary" round :loading="submitting" @click="claim">提交申请</el-button>
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
.actions { display: flex; gap: 12px; margin-bottom: 18px; }
.safe-tip { padding: 10px 14px; border-radius: 12px; background: var(--sky-soft); color: #3D6D99; font-size: 12.5px; line-height: 1.7; }
.claim-tip { color: var(--ink-soft); font-size: 13.5px; line-height: 1.7; margin-bottom: 12px; }
.contact-row { display: flex; gap: 10px; align-items: center; }
@media (max-width: 860px) { .detail { grid-template-columns: 1fr; } }
</style>
