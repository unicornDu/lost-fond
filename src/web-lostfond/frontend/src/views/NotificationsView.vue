<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { t } from '../i18n'

/**
 * 通知中心（独立页）：认领与审核 / 系统通知。
 * 私信已独立到 /messages，这里只处理通知类消息。
 * 进入某个分栏只把该栏标记已读，避免一进页面就把所有未读清空。
 */
const messages = ref([])
const loading = ref(false)
const sub = ref('claim')

const SUBS = computed(() => [
  { key: 'claim', label: t('profile.sub.claim') },
  { key: 'system', label: t('profile.sub.system') },
])

const claimMsgs = computed(() => messages.value.filter((m) => m.type === 'claim' || m.type === 'audit'))
const systemMsgs = computed(() => messages.value.filter((m) => m.type !== 'claim' && m.type !== 'audit' && m.type !== 'dm'))
const unreadCount = computed(() => messages.value.filter((m) => m.isRead === 0 && m.type !== 'dm').length)

function subUnread(k) {
  const arr = k === 'claim' ? claimMsgs.value : systemMsgs.value
  return arr.filter((m) => m.isRead === 0).length
}

async function markSubRead() {
  const arr = sub.value === 'claim' ? claimMsgs.value : systemMsgs.value
  const unread = arr.filter((m) => m.isRead === 0)
  if (!unread.length) return
  try {
    await Promise.all(unread.map((m) => api.readOne(m.id).catch(() => {})))
    unread.forEach((m) => (m.isRead = 1))
    window.dispatchEvent(new Event('shiguang-unread-changed'))
  } catch (e) { /* 非关键路径 */ }
}

async function load() {
  loading.value = true
  try {
    messages.value = await api.messages()
    markSubRead()
  } catch (e) {
    ElMessage.error(e.message || t('profile.loadFail'))
  } finally {
    loading.value = false
  }
}
onMounted(load)
watch(sub, markSubRead)

async function readOne(m) {
  if (m.isRead === 1) return
  try {
    await api.readOne(m.id)
    m.isRead = 1
    window.dispatchEvent(new Event('shiguang-unread-changed'))
  } catch (e) { /* 非关键路径 */ }
}

async function readAll() {
  try {
    await api.readAll()
    messages.value.forEach((m) => (m.isRead = 1))
    window.dispatchEvent(new Event('shiguang-unread-changed'))
    ElMessage.success(t('profile.readAllOk'))
  } catch (e) { ElMessage.error(e.message || t('profile.opFail')) }
}
</script>

<template>
  <div class="notify">
    <header class="head">
      <div class="head-main">
        <p class="eyebrow">INBOX · {{ $t('nav.notify') }}</p>
        <h1 class="title">{{ $t('nav.notify') }}</h1>
        <p class="sub">{{ $t('notify.sub') }}</p>
      </div>
      <div class="head-actions">
        <button v-if="unreadCount" class="all-read" type="button" @click="readAll">
          {{ $t('profile.msg.readAll') }}
        </button>
        <RouterLink to="/messages" class="to-chat">{{ $t('nav.chat') }} →</RouterLink>
      </div>
    </header>

    <div class="tabs" role="tablist">
      <button
        v-for="s in SUBS" :key="s.key" type="button" role="tab"
        class="tab" :class="{ on: sub === s.key }" :aria-selected="sub === s.key"
        @click="sub = s.key"
      >
        {{ s.label }}<i v-if="subUnread(s.key)">{{ subUnread(s.key) }}</i>
      </button>
    </div>

    <div class="body" v-loading="loading">
      <div
        v-for="m in (sub === 'claim' ? claimMsgs : systemMsgs)"
        :key="m.id"
        class="msg paper-card" :class="{ unread: m.isRead === 0 }"
        @click="readOne(m)"
      >
        <span v-if="m.isRead === 0" class="dot"></span>
        <div class="msg-body">
          <b>{{ m.title }}</b>
          <p>{{ m.content }}</p>
          <small>{{ (m.createTime || '').replace('T', ' ').slice(0, 16) }}</small>
        </div>
        <el-button
          v-if="m.refId" size="small" round plain
          @click.stop="$router.push(`/item/${m.refId}`)"
        >{{ $t('profile.msg.viewItem') }}</el-button>
      </div>
      <el-empty
        v-if="!(sub === 'claim' ? claimMsgs : systemMsgs).length"
        :description="sub === 'claim' ? $t('profile.msg.empty.claim') : $t('profile.msg.empty.system')"
      />
    </div>
  </div>
</template>

<style scoped>
/* 与首页信息流同宽：右栏收起时自动变宽 */
.notify { max-width: var(--feed-max, 760px); }

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

.head-actions { display: flex; align-items: center; gap: 16px; }
.all-read {
  border: 0; background: none; cursor: pointer;
  color: var(--ink); font-family: inherit; font-size: 12.5px; font-weight: 700;
  padding: 6px 0; border-bottom: 1px solid currentColor;
  transition: opacity .18s;
}
.all-read:hover { opacity: .6; }
.to-chat {
  font-size: 12.5px; font-weight: 700; color: var(--orange-deep);
  transition: opacity .18s;
}
.to-chat:hover { opacity: .7; }

.tabs { display: flex; gap: 8px; margin: 0 0 18px; flex-wrap: wrap; }
.tab {
  border: 0; cursor: pointer;
  padding: 9px 17px; border-radius: 999px;
  background: var(--cream-deep); color: var(--ink-soft);
  font-family: inherit; font-size: 13px; font-weight: 700;
  display: inline-flex; align-items: center; gap: 6px;
  transition: background-color .18s ease, color .18s ease, transform .18s ease;
}
.tab:hover { color: var(--ink); transform: translateY(-1px); }
.tab.on { background: var(--ink); color: var(--paper); }
.tab i {
  font-style: normal; background: var(--rose); color: #fff;
  border-radius: 999px; font-size: 10px; font-weight: 800; padding: 1px 7px;
}

.msg {
  position: relative; display: flex; gap: 12px; align-items: center;
  padding: 14px 18px; margin-bottom: 12px; cursor: pointer;
}
.msg.unread { border-color: var(--orange); background: var(--orange-soft); box-shadow: inset 3px 0 0 var(--orange); }
.msg .dot {
  position: absolute; left: 8px; top: 50%; transform: translateY(-50%);
  width: 8px; height: 8px; border-radius: 50%; background: var(--orange);
}
.msg-body { flex: 1; min-width: 0; }
.msg-body b { font-size: 14.5px; }
.msg-body p { color: var(--ink-soft); font-size: 13px; margin-top: 4px; line-height: 1.6; }
.msg-body small { color: var(--ink-faint); font-size: 11.5px; }
</style>
