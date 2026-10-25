<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { t } from '../i18n'

/**
 * 聊天（独立页）：私信会话列表 → 会话线程回复。
 * 与通知页同一套数据源（/user/messages），进入某个会话时只把该会话标为已读。
 */
const messages = ref([])
const loading = ref(false)
const activeConv = ref(null)
const input = ref('')
const sending = ref(false)

const dmMsgs = computed(() => messages.value.filter((m) => m.type === 'dm' && m.fromUserId))

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

const unreadDm = computed(() => dmMsgs.value.filter((m) => m.isRead === 0).length)

async function load() {
  loading.value = true
  try {
    messages.value = (await api.messages()) || []
  } catch (e) {
    ElMessage.error(e.message || t('profile.loadFail'))
  } finally {
    loading.value = false
  }
}
onMounted(load)

async function openConv(c) {
  activeConv.value = c
  const unread = c.msgs.filter((m) => m.isRead === 0)
  if (!unread.length) return
  try {
    await Promise.all(unread.map((m) => api.readOne(m.id).catch(() => {})))
    unread.forEach((m) => (m.isRead = 1))
    c.unread = false
    window.dispatchEvent(new Event('shiguang-unread-changed'))
  } catch (e) { /* 非关键路径 */ }
}

function closeConv() {
  activeConv.value = null
  input.value = ''
}

async function send() {
  const c = activeConv.value
  const v = input.value.trim()
  if (!c || !v) return
  const last = [...c.msgs].reverse().find((m) => !m.sent)
  if (!last) { ElMessage.warning(t('profile.reply.noMsg')); return }
  sending.value = true
  try {
    await api.replyMessage(last.id, v)
    c.msgs.push({ id: 'local-' + Date.now(), sent: true, content: v, createTime: new Date().toISOString().slice(0, 19) })
    input.value = ''
  } catch (e) {
    ElMessage.error(e.message || t('profile.reply.fail'))
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <div class="chat">
    <header class="head">
      <div>
        <p class="eyebrow">MESSAGES · {{ $t('nav.chat') }}</p>
        <h1 class="title">{{ $t('nav.chat') }}</h1>
        <p class="sub">{{ $t('messages.sub') }}</p>
      </div>
      <span v-if="unreadDm" class="count">{{ $t('profile.sub.dm') }} · {{ unreadDm }}</span>
    </header>

    <div v-loading="loading">
      <!-- 会话列表 -->
      <template v-if="!activeConv">
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
      </template>

      <!-- 会话线程 -->
      <div v-else class="thread paper-card">
        <div class="thread-head">
          <button class="thread-back" type="button" @click="closeConv">{{ $t('profile.msg.back') }}</button>
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
          <el-input v-model="input" :placeholder="$t('profile.msg.threadPh')" maxlength="200" @keyup.enter="send" />
          <el-button type="primary" round :loading="sending" @click="send">{{ $t('profile.msg.send') }}</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 与首页信息流同宽：右栏收起时自动变宽 */
.chat { max-width: var(--feed-max, 760px); }

.head {
  display: flex; align-items: flex-end; justify-content: space-between; gap: 18px;
  padding: 2px 0 20px; flex-wrap: wrap;
}
.eyebrow {
  font-size: 10.5px; font-weight: 700; letter-spacing: .18em;
  color: var(--ink-faint); margin-bottom: 10px;
}
.title { font-size: 27px; line-height: 1.2; letter-spacing: -.02em; }
.sub { margin-top: 8px; font-size: 13px; color: var(--ink-soft); }
.count { font-size: 12px; color: var(--ink-faint); font-variant-numeric: tabular-nums; }

.conv {
  display: flex; align-items: center; gap: 14px; padding: 15px 18px; margin-bottom: 12px;
  cursor: pointer; transition: transform .18s cubic-bezier(.22, 1, .36, 1), border-color .18s ease;
}
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
.thread-head { display: flex; align-items: center; gap: 14px; padding: 14px 18px; border-bottom: 1px solid var(--line); }
.thread-head b { font-size: 15px; }
.thread-back { border: 0; background: none; cursor: pointer; color: var(--orange-deep); font-weight: 700; font-size: 13px; }
.thread-msgs { display: flex; flex-direction: column; gap: 10px; padding: 18px; max-height: 460px; overflow-y: auto; }
.t-msg { max-width: 78%; }
.t-msg p { padding: 10px 14px; border-radius: 14px; background: var(--cream-deep); font-size: 13.5px; line-height: 1.7; }
.t-msg.me { align-self: flex-end; text-align: right; }
.t-msg.me p { background: var(--orange); color: var(--paper); text-align: left; }
.t-msg small { display: block; font-size: 10.5px; color: var(--ink-faint); margin-top: 4px; }
.thread-input { display: flex; gap: 10px; padding: 14px 16px; border-top: 1px solid var(--line); }
</style>
