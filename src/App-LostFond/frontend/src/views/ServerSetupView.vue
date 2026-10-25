<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { t } from '../i18n'
import { getServerBase, normalizeServer, setServerBase } from '../utils/server'

const route = useRoute()
const router = useRouter()
const input = ref('')
const current = ref('')
const testing = ref(false)

onMounted(() => {
  current.value = getServerBase()
  input.value = current.value
})

async function connect() {
  const base = normalizeServer(input.value)
  if (!base || !/^https?:\/\/[^\s/]+/i.test(base)) {
    ElMessage.warning(t('server.invalid'))
    return
  }
  testing.value = true
  try {
    const ctrl = new AbortController()
    const timer = setTimeout(() => ctrl.abort(), 8000)
    let res
    try {
      res = await fetch(base + '/api/stats', { signal: ctrl.signal })
    } finally {
      clearTimeout(timer)
    }
    if (!res.ok) throw new Error('HTTP ' + res.status)
    const body = await res.json()
    if (body?.code !== 200) throw new Error(body?.msg || 'bad response')
    setServerBase(base)
    current.value = base
    ElMessage.success(t('server.ok'))
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    router.replace(redirect && !redirect.startsWith('/server') ? redirect : '/auth')
  } catch (e) {
    ElMessage.error(t('server.fail'))
  } finally {
    testing.value = false
  }
}
</script>

<template>
  <main class="server-setup">
    <section class="server-card">
      <p class="eyebrow">{{ t('server.eyebrow') }}</p>
      <h1>{{ t('server.title') }}</h1>
      <p class="sub">{{ t('server.sub') }}</p>

      <form @submit.prevent="connect">
        <label for="server-url">{{ t('server.label') }}</label>
        <input
          id="server-url"
          v-model="input"
          type="url"
          inputmode="url"
          autocapitalize="off"
          autocorrect="off"
          spellcheck="false"
          :placeholder="t('server.ph')"
        />
        <p class="tip">{{ t('server.tip') }}</p>
        <button type="submit" :disabled="testing">
          {{ testing ? t('server.connecting') : t('server.connect') }}
        </button>
      </form>

      <p v-if="current" class="current">{{ t('server.current', { url: current }) }}</p>
      <RouterLink to="/auth" class="back">{{ t('server.back') }}</RouterLink>
    </section>
  </main>
</template>

<style scoped>
.server-setup {
  min-height: 100svh;
  display: grid;
  place-items: center;
  padding: calc(24px + env(safe-area-inset-top)) 20px calc(24px + env(safe-area-inset-bottom));
  background:
    radial-gradient(120% 80% at 50% -10%, #F1E9DC 0%, transparent 60%),
    #F7F4EF;
  color: #1c1a17;
}
.server-card {
  width: min(440px, 100%);
  padding: 30px 26px 24px;
  border: 1px solid #E7E1D8;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 22px 70px rgba(70, 52, 34, .09);
}
.eyebrow {
  margin: 0 0 10px;
  font-size: 11px;
  letter-spacing: .18em;
  text-transform: uppercase;
  color: #9B8F80;
}
h1 {
  margin: 0;
  font-family: var(--font-serif, serif);
  font-size: clamp(26px, 7vw, 32px);
  font-weight: 500;
  letter-spacing: -.02em;
}
.sub {
  margin: 12px 0 24px;
  font-size: 13.5px;
  line-height: 1.75;
  color: #6E675E;
}
label {
  display: block;
  margin-bottom: 8px;
  font-size: 12.5px;
  font-weight: 600;
  color: #4D463D;
}
input {
  width: 100%;
  box-sizing: border-box;
  padding: 14px 14px;
  border: 1px solid #DED5C8;
  border-radius: 12px;
  background: #FCFBF8;
  color: #1c1a17;
  font: inherit;
  font-size: 16px;
  outline: none;
  transition: border-color .18s, box-shadow .18s;
}
input:focus {
  border-color: #1c1a17;
  box-shadow: 0 0 0 3px rgba(28, 26, 23, .08);
}
.tip {
  margin: 9px 0 18px;
  font-size: 11.5px;
  line-height: 1.65;
  color: #9B8F80;
}
button {
  width: 100%;
  padding: 14px;
  border: 0;
  border-radius: 12px;
  background: #1c1a17;
  color: #F7F4EF;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: .04em;
  cursor: pointer;
}
button:disabled { opacity: .6; cursor: wait; }
.current {
  margin: 18px 0 0;
  font-size: 11.5px;
  color: #8A8073;
  word-break: break-all;
}
.back {
  display: block;
  margin-top: 14px;
  text-align: center;
  font-size: 12.5px;
  color: #7C6BF0;
}
</style>
