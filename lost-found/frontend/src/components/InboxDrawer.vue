<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { t } from '../i18n'

/**
 * 收件抽屉（对齐「整合版 v1」的右侧滑出交互）：
 *  - mode="notify"：通知中心三栏（认领与审核 / 系统 / 私信），分栏未读徽标、点击已读、全部已读；
 *  - mode="chat"  ：私信会话列表 → 线程（上下文卡 + 气泡 + 回车发送 + 关联启事）。
 * 数据全部走真实接口（messages / readOne / readAll / replyMessage），与个人中心同一套数据源。
 */
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: 'notify' }, // notify | chat
})
const emit = defineEmits(['update:modelValue', 'update:mode'])

const messages = ref([])
const loading = ref(false)
const sub = ref('claim')
const activeConv = ref(null)
const input = ref('')
const sending = ref(false)

const SUBS = computed(() => [
  { key: 'claim', label: t('inbox.sub.claim') },
  { key: 'system', label: t('inbox.sub.system') },
  { key: 'dm', label: t('inbox.sub.dm') },
])

const claimMsgs = computed(() => messages.value.filter((m) => m.type === 'claim' || m.type === 'audit'))
const systemMsgs = computed(() => messages.value.filter((m) => !['claim', 'audit', 'dm'].includes(m.type)))
const dmMsgs = computed(() => messages.value.filter((m) => m.type === 'dm' && m.fromUserId))

function listOf(k) {
  return k === 'claim' ? claimMsgs.value : k === 'system' ? systemMsgs.value : dmMsgs.value
}
function unreadOf(k) {
  return listOf(k).filter((m) => m.isRead === 0).length
}

/* 私信会话：按对方聚合，线程内按时间升序 */
const conversations = computed(() => {
  const map = new Map()
  for (const m of dmMsgs.value) {
    if (!map.has(m.fromUserId)) {
      map.set(m.fromUserId, { fromUserId: m.fromUserId, name: m.fromNickname || t('inbox.peer'), msgs: [] })
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

const threadMsgs = computed(() => (activeConv.value ? activeConv.value.msgs : []))

function fmt(t) {
  return (t || '').replace('T', ' ').slice(5, 16)
}

async function load() {
  loading.value = true
  try {
    messages.value = (await api.messages()) || []
  } catch (e) {
    messages.value = []
  } finally {
    loading.value = false
  }
}

function close() { emit('update:modelValue', false) }

async function readOne(m) {
  if (m.isRead === 0) {
    try {
      await api.readOne(m.id)
      m.isRead = 1
      window.dispatchEvent(new Event('shiguang-unread-changed'))
    } catch (e) { /* 非关键路径 */ }
  }
  if (m.type === 'dm' && m.fromUserId) {
    const c = conversations.value.find((x) => x.fromUserId === m.fromUserId)
    // 先切模式（触发 mode watcher 复位），下一帧再落到目标会话，避免被清空
    if (c) { emit('update:mode', 'chat'); nextTick(() => { activeConv.value = c }) }
  }
}

async function readAll() {
  try {
    await api.readAll()
    messages.value.forEach((m) => (m.isRead = 1))
    window.dispatchEvent(new Event('shiguang-unread-changed'))
    ElMessage.success(t('inbox.readAllOk'))
  } catch (e) { ElMessage.error(e.message || t('inbox.opFail')) }
}

function openConv(c) { activeConv.value = c }
function backToList() { activeConv.value = null; input.value = '' }

async function send() {
  const c = activeConv.value
  const v = input.value.trim()
  if (!c || !v) return
  const last = [...c.msgs].reverse().find((m) => !m.sent)
  if (!last) { ElMessage.warning(t('inbox.noReply')); return }
  sending.value = true
  try {
    await api.replyMessage(last.id, v)
    c.msgs.push({ id: 'local-' + Date.now(), sent: true, content: v, createTime: new Date().toISOString().slice(0, 19) })
    input.value = ''
  } catch (e) {
    ElMessage.error(e.message || t('inbox.sendFail'))
  } finally {
    sending.value = false
  }
}

function onKey(e) {
  if (e.key === 'Escape' && props.modelValue) close()
}

watch(() => props.modelValue, (v) => {
  if (v) {
    load()
    if (props.mode === 'chat') activeConv.value = null
    else sub.value = 'claim'
  }
})
watch(() => props.mode, () => { activeConv.value = null; input.value = '' })

if (typeof window !== 'undefined') window.addEventListener('keydown', onKey)
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <Teleport to="body">
    <div class="drawer-mask" :class="{ open: modelValue }" @click="close"></div>
    <aside class="inbox-drawer" :class="{ open: modelValue }" :aria-hidden="!modelValue">
      <div class="inbox-head">
        <h3>{{ mode === 'chat' ? (activeConv ? activeConv.name : $t('inbox.title.chat')) : $t('inbox.title.notify') }}</h3>
        <button class="inbox-x" :aria-label="$t('inbox.close')" @click="close">✕</button>
      </div>

      <div class="inbox-body">
        <!-- ============ 通知中心 ============ -->
        <template v-if="mode === 'notify'">
          <div class="inbox-tabs">
            <button
              v-for="s in SUBS" :key="s.key"
              class="inbox-tab" :class="{ on: sub === s.key }"
              @click="sub = s.key"
            >{{ s.label }}<i v-if="unreadOf(s.key)">{{ unreadOf(s.key) }}</i></button>
          </div>

          <div class="inbox-list">
            <div
              v-for="m in listOf(sub)" :key="m.id"
              class="inbox-ntf" :class="{ unread: m.isRead === 0 }"
              @click="readOne(m)"
            >
              <div>
                <b>{{ m.title }}</b>
                <p>{{ m.content }}</p>
                <small>{{ fmt(m.createTime) }}</small>
              </div>
            </div>
            <p v-if="!listOf(sub).length" class="inbox-empty">
              {{ loading ? $t('common.loading') : sub === 'dm' ? $t('inbox.empty.dm') : $t('inbox.empty.other') }}
            </p>
          </div>

          <div class="inbox-foot">
            <el-button round plain style="width:100%" @click="readAll">{{ $t('inbox.readAll') }}</el-button>
          </div>
        </template>

        <!-- ============ 聊天会话 ============ -->
        <template v-else>
          <!-- 会话列表 -->
          <div v-if="!activeConv" class="inbox-convs">
            <div v-for="c in conversations" :key="c.fromUserId" class="inbox-conv" @click="openConv(c)">
              <span class="inbox-ava">{{ (c.name || $t('inbox.peer'))[0] }}</span>
              <div class="inbox-conv-main">
                <div class="inbox-conv-top">
                  <b>{{ c.name }}</b>
                  <span>{{ fmt(c.last.createTime) }}</span>
                </div>
                <p class="inbox-conv-last">{{ c.last.content }}</p>
              </div>
            </div>
            <p v-if="!conversations.length" class="inbox-empty">
              {{ loading ? $t('common.loading') : $t('inbox.empty.conv') }}
            </p>
          </div>

          <!-- 会话线程 -->
          <div v-else class="inbox-thread">
            <div class="inbox-thread-head">
              <button class="inbox-back" @click="backToList">{{ $t('inbox.back') }}</button>
            </div>

            <!-- 启事上下文卡：防止"你说的是哪件？" -->
            <RouterLink
              v-if="activeConv.last?.refId" :to="`/item/${activeConv.last.refId}`"
              class="inbox-item-card" @click="close"
            >
              <div>
                <b>{{ $t('inbox.context.title') }}</b>
                <span>{{ $t('inbox.context.desc') }}</span>
              </div>
              <span style="margin-left:auto;color:var(--orange-deep);font-weight:700">{{ $t('inbox.view') }}</span>
            </RouterLink>

            <div class="inbox-msgs">
              <div v-for="m in threadMsgs" :key="m.id" class="inbox-msg" :class="m.sent ? 'me' : 'them'">
                {{ m.content }}
                <small>{{ m.sent ? $t('inbox.me') : activeConv.name }} · {{ m.sent ? $t('inbox.now') : fmt(m.createTime) }}</small>
              </div>
            </div>

            <div class="inbox-input">
              <input
                v-model="input" :placeholder="$t('inbox.inputPh')"
                maxlength="200" @keyup.enter="send"
              />
              <button :disabled="sending" @click="send">{{ $t('inbox.send') }}</button>
            </div>
          </div>
        </template>
      </div>
    </aside>
  </Teleport>
</template>
