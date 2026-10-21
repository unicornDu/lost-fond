<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { t } from '../i18n'

/**
 * 登录 / 注册页
 * 版式对齐 08-新版设计/登录页设计：左紫色品牌图（四个角色 + 栅格底） / 右白底表单。
 * 四个角色的眼睛跟随鼠标，身体按光标方向做幅度不同的倾斜与压缩。
 * /auth?tab=register 直达注册；成功后回 redirect 或 /home。
 */
const route = useRoute()
const router = useRouter()
const user = useUserStore()

const tab = ref(route.query.tab === 'register' ? 'register' : 'login')
const remember = ref(true)
const loading = ref(false)
const showPwd = ref(false)
const showPwd2 = ref(false)
const form = reactive({ account: '', password: '', password2: '', nickname: '' })
// E-05：被封禁踢出后的专属提示（stores/user.refresh 写入 sessionStorage，登录成功即清除）
const bannedMsg = ref(sessionStorage.getItem('shiguang_banned_msg') || '')
const stage = ref(null)

const isLogin = computed(() => tab.value === 'login')

/* ---------------- 角色跟随鼠标 ---------------- */
let rafLook = 0
let lookX = 0
let lookY = 0

function clamp(v, a, b) { return Math.max(a, Math.min(b, v)) }

function onPointerMove(e) {
  const el = stage.value
  if (!el) return
  if (!window.matchMedia('(pointer: fine)').matches) return
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  const r = el.getBoundingClientRect()
  if (!r.width || !r.height) return
  // 以舞台中心为原点归一化到 -1..1：左右决定倾斜方向，上下决定压缩幅度
  lookX = clamp((e.clientX - (r.left + r.width / 2)) / (r.width * 0.72), -1, 1)
  lookY = clamp((e.clientY - (r.top + r.height / 2)) / (r.height * 0.86), -1, 1)
  if (!rafLook) rafLook = requestAnimationFrame(paintLook)
}
function paintLook() {
  rafLook = 0
  const el = stage.value
  if (!el) return
  el.style.setProperty('--mx', lookX.toFixed(3))
  el.style.setProperty('--my', lookY.toFixed(3))
}
function resetLook() {
  lookX = 0
  lookY = 0
  paintLook()
}

onMounted(() => {
  if (bannedMsg.value) sessionStorage.removeItem('shiguang_banned_msg')
  window.addEventListener('pointermove', onPointerMove, { passive: true })
  window.addEventListener('pointerleave', resetLook)
})
onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerleave', resetLook)
  if (rafLook) cancelAnimationFrame(rafLook)
})

/* ---------------- 表单 ---------------- */
function switchTab(next) {
  tab.value = next
  showPwd.value = false
  showPwd2.value = false
  router.replace({ query: { ...route.query, tab: next } })
}

async function submit() {
  if (!/^\d{6,12}$/.test(form.account.trim())) return ElMessage.warning(t('auth.err.account'))
  if (form.password.length < 6) return ElMessage.warning(t('auth.err.pwd'))
  if (tab.value === 'register' && form.password !== form.password2) {
    return ElMessage.warning(t('auth.err.pwd2'))
  }
  loading.value = true
  try {
    if (tab.value === 'register') {
      await user.register({
        account: form.account.trim(),
        password: form.password,
        nickname: form.nickname.trim(),
      })
      ElMessage.success(t('auth.ok.register'))
    } else {
      await user.login({ account: form.account.trim(), password: form.password, rememberMe: remember.value })
      ElMessage.success(t('auth.ok.login', { name: user.info.nickname || user.info.account }))
    }
    router.push(route.query.redirect || '/home')
  } catch (e) {
    ElMessage.error(e.message || t('auth.err.fail'))
  } finally {
    loading.value = false
  }
}

// E-06：忘记密码给出明确指引（后端无自助重置通道，属既定范围）
function forgotPwd() {
  ElMessageBox.alert(t('auth.forgot.body'), t('auth.forgot.title'), {
    confirmButtonText: t('auth.forgot.ok'),
  })
}

/* 左下角三个说明链接：以弹窗承载，不额外造页面 */
const LEGAL = {
  privacy: ['auth.privacy', 'auth.privacyBody'],
  terms: ['auth.terms', 'auth.termsBody'],
  contact: ['auth.contact', 'auth.contactBody'],
}
function showLegal(kind) {
  const [title, body] = LEGAL[kind]
  ElMessageBox.alert(t(body), t(title), { confirmButtonText: t('auth.legalOk') })
}
</script>

<template>
  <div class="auth">
    <!-- ============ 左：品牌图 + 四个角色 ============ -->
    <aside class="pane-brand">
      <RouterLink to="/" class="brand" aria-label="拾光 · 返回首页">
        <span class="brand-mark">拾</span>
        <span class="brand-name">拾光</span>
      </RouterLink>

      <div class="crew-wrap">
        <div ref="stage" class="crew" aria-hidden="true">
          <!-- 紫：最高的一个，站在最后 -->
          <div class="fig purple">
            <span class="body"></span>
            <span class="eyes">
              <span class="eye e1"><span class="pupil"></span></span>
              <span class="eye e2"><span class="pupil"></span></span>
            </span>
          </div>

          <!-- 黑 -->
          <div class="fig black">
            <span class="body"></span>
            <span class="eyes">
              <span class="eye e1"><span class="pupil"></span></span>
              <span class="eye e2"><span class="pupil"></span></span>
            </span>
          </div>

          <!-- 橙：拱形，圆点眼 -->
          <div class="fig orange">
            <span class="body"></span>
            <span class="eyes">
              <span class="eye e1"><span class="pupil"></span></span>
              <span class="eye e2"><span class="pupil"></span></span>
            </span>
          </div>

          <!-- 黄：拱形，圆点眼 -->
          <div class="fig yellow">
            <span class="body"></span>
            <span class="eyes">
              <span class="eye e1"><span class="pupil"></span></span>
              <span class="eye e2"><span class="pupil"></span></span>
            </span>
          </div>
        </div>
      </div>

      <div class="pane-foot">
        <button type="button" @click="showLegal('privacy')">{{ $t('auth.privacy') }}</button>
        <button type="button" @click="showLegal('terms')">{{ $t('auth.terms') }}</button>
        <button type="button" @click="showLegal('contact')">{{ $t('auth.contact') }}</button>
      </div>
    </aside>

    <!-- ============ 右：表单 ============ -->
    <section class="pane-form">
      <div class="form-wrap">
        <div class="mobile-brand">
          <RouterLink to="/" class="brand">
            <span class="brand-mark">拾</span>
            <span class="brand-name">拾光</span>
          </RouterLink>
          <RouterLink to="/" class="back-link">{{ $t('auth.backHome') }}</RouterLink>
        </div>

        <!-- E-05：封禁提示条 -->
        <div v-if="bannedMsg" class="banned">⛔ {{ bannedMsg }}</div>

        <header class="form-head">
          <h1>{{ isLogin ? $t('auth.welcome') : $t('auth.join') }}</h1>
          <p>{{ isLogin ? $t('auth.loginDesc') : $t('auth.registerDesc') }}</p>
        </header>

        <form class="form" @submit.prevent="submit">
          <div v-if="!isLogin" class="field">
            <label for="af-nickname">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round">
                <circle cx="12" cy="8" r="3.2" />
                <path d="M5.5 19.5c.7-3 3.3-4.6 6.5-4.6s5.8 1.6 6.5 4.6" />
              </svg>
              {{ $t('auth.nickname') }}
            </label>
            <input
              id="af-nickname" v-model="form.nickname" type="text" maxlength="12"
              :placeholder="$t('auth.nicknamePh')" autocomplete="nickname"
            />
          </div>

          <div class="field">
            <label for="af-account">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round">
                <circle cx="12" cy="8" r="3.2" />
                <path d="M5.5 19.5c.7-3 3.3-4.6 6.5-4.6s5.8 1.6 6.5 4.6" />
              </svg>
              {{ $t('auth.account') }}
            </label>
            <input
              id="af-account" v-model="form.account" type="text" maxlength="12" inputmode="numeric"
              :placeholder="$t('auth.accountPh')" autocomplete="username"
            />
          </div>

          <div class="field">
            <label for="af-password">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round">
                <rect x="5" y="10.5" width="14" height="9.5" rx="2.2" />
                <path d="M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5" />
              </svg>
              {{ $t('auth.password') }}
            </label>
            <div class="input-box">
              <input
                id="af-password" v-model="form.password" :type="showPwd ? 'text' : 'password'"
                :placeholder="$t('auth.passwordPh')"
                :autocomplete="isLogin ? 'current-password' : 'new-password'"
              />
              <button class="peek" type="button" :aria-label="showPwd ? $t('auth.hidePwd') : $t('auth.showPwd')" @click="showPwd = !showPwd">
                <svg v-if="showPwd" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round">
                  <path d="M3 3l18 18" />
                  <path d="M10.6 6.2A9.9 9.9 0 0 1 12 6c6.4 0 10 6 10 6a17.6 17.6 0 0 1-3.1 3.7M6.3 7.9A17 17 0 0 0 2 12s3.6 6 10 6a9.8 9.8 0 0 0 4-.85" />
                  <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" />
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round">
                  <path d="M2 12s3.6-6 10-6 10 6 10 6-3.6 6-10 6S2 12 2 12Z" />
                  <circle cx="12" cy="12" r="2.6" />
                </svg>
              </button>
            </div>
          </div>

          <div v-if="!isLogin" class="field">
            <label for="af-password2">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round">
                <rect x="5" y="10.5" width="14" height="9.5" rx="2.2" />
                <path d="M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5" />
              </svg>
              {{ $t('auth.password2') }}
            </label>
            <div class="input-box">
              <input
                id="af-password2" v-model="form.password2" :type="showPwd2 ? 'text' : 'password'"
                :placeholder="$t('auth.password2Ph')" autocomplete="new-password"
              />
              <button class="peek" type="button" :aria-label="showPwd2 ? $t('auth.hidePwd') : $t('auth.showPwd')" @click="showPwd2 = !showPwd2">
                <svg v-if="showPwd2" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round">
                  <path d="M3 3l18 18" />
                  <path d="M10.6 6.2A9.9 9.9 0 0 1 12 6c6.4 0 10 6 10 6a17.6 17.6 0 0 1-3.1 3.7M6.3 7.9A17 17 0 0 0 2 12s3.6 6 10 6a9.8 9.8 0 0 0 4-.85" />
                  <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" />
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round">
                  <path d="M2 12s3.6-6 10-6 10 6 10 6-3.6 6-10 6S2 12 2 12Z" />
                  <circle cx="12" cy="12" r="2.6" />
                </svg>
              </button>
            </div>
          </div>

          <div v-if="isLogin" class="between">
            <label class="remember">
              <input v-model="remember" type="checkbox" />
              <span>{{ $t('auth.remember') }}</span>
            </label>
            <button type="button" class="link" @click="forgotPwd">{{ $t('auth.forgot') }}</button>
          </div>

          <button type="submit" class="primary" :disabled="loading">
            {{ loading ? $t('auth.processing') : (isLogin ? $t('auth.submitLogin') : $t('auth.submitRegister')) }}
          </button>
        </form>

        <p class="swap">
          {{ isLogin ? $t('auth.noAccount') : $t('auth.hasAccount') }}
          <button type="button" @click="switchTab(isLogin ? 'register' : 'login')">
            {{ isLogin ? $t('auth.registerNow') : $t('auth.loginNow') }}
          </button>
        </p>
        <RouterLink to="/" class="back-home">{{ $t('auth.backHome') }}</RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* ============================================================
   令牌：紫色品牌面板 / 白色表单面板
   ============================================================ */
.auth {
  --a-ink: #1D1B28;
  --a-sub: #8B8998;
  --a-line: #E9E8F0;
  --a-soft: #F9FAFB;
  --a-primary: #6C5CE7;
  --a-primary-2: #7B6BF6;
  position: relative;
  display: grid;
  grid-template-columns: 49.4fr 50.6fr;
  height: calc(100svh - var(--safe-top));
  overflow: hidden;
  background: #fff;
  color: var(--a-ink);
  font-family: var(--font-body);
}

/* ---------------- 左：品牌面板 ---------------- */
.pane-brand {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: clamp(34px, 5vw, 76px) clamp(24px, 4vw, 60px) clamp(38px, 7.4vh, 78px);
  color: #fff;
  overflow: hidden;
  background-image:
    linear-gradient(to right, rgba(255, 255, 255, .055) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(255, 255, 255, .055) 1px, transparent 1px),
    radial-gradient(90% 62% at 12% 0%, rgba(255, 255, 255, .16), transparent 64%),
    linear-gradient(158deg, #7173F6 0%, #6365F1 38%, #5650E8 100%);
  background-size: 28px 28px, 28px 28px, 100% 100%, 100% 100%;
}
.brand {
  position: relative;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: #fff;
  width: fit-content;
}
.brand-mark {
  display: grid;
  place-items: center;
  width: 34px; height: 34px;
  border-radius: 9px;
  background: rgba(255, 255, 255, .24);
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0;
}
.brand-name { font-size: 21px; font-weight: 600; letter-spacing: .04em; }

.crew-wrap {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  align-items: flex-end;
  justify-content: flex-start;
  padding-bottom: clamp(52px, 10vh, 104px);
  min-height: 0;
}
.crew {
  --mx: 0;
  --my: 0;
  position: relative;
  width: min(84%, 680px);
  aspect-ratio: 651 / 605;
}

/* 每个角色：底部为支点，按光标方向做不同幅度的平移 / 倾斜 / 压缩 */
.fig {
  position: absolute;
  transform-origin: 50% 100%;
  transition: transform .55s cubic-bezier(.22, 1, .36, 1);
  will-change: transform;
  transform:
    translate3d(calc(var(--mx) * var(--lx, 3%)), calc(var(--my) * var(--ly, 1%)), 0)
    rotate(calc(var(--mx) * var(--rot, 1deg)))
    scaleY(calc(1 - var(--my) * var(--sq, .015)));
}
.fig .body { position: absolute; inset: 0; background: var(--c); }

.purple {
  left: 17.4%; top: 0; width: 39.8%; height: 100%;
  --c: #6C3FF5; --lx: 2.4%; --ly: .7%; --rot: .9deg; --sq: .012;
  z-index: 1;
}
.purple .body { border-radius: 4% 4% 0 0 / 1.8% 1.8% 0 0; }

.black {
  left: 54.7%; top: 23%; width: 25.3%; height: 77%;
  --c: #2D2D2D; --lx: 3.4%; --ly: 1%; --rot: 1.3deg; --sq: .016;
  z-index: 2;
}
.black .body { border-radius: 5% 5% 0 0 / 2% 2% 0 0; }

.orange {
  left: 0; top: 50.6%; width: 52.5%; height: 49.4%;
  --c: #FF9B6B; --lx: 4%; --ly: 1.2%; --rot: 1.5deg; --sq: .02;
  z-index: 3;
}
.orange .body { border-radius: 50% 50% 0 0 / 57% 57% 0 0; }

.yellow {
  left: 70.4%; top: 43.6%; width: 29.6%; height: 56.4%;
  --c: #E8D754; --lx: 4.6%; --ly: 1.2%; --rot: 1.7deg; --sq: .02;
  z-index: 4;
}
.yellow .body { border-radius: 50% 50% 0 0 / 28% 28% 0 0; }

/* 眼睛：白眼球 + 可偏移的瞳孔；圆点眼用透明眼眶放大活动范围 */
.eyes { position: absolute; inset: 0; pointer-events: none; }
.eye {
  position: absolute;
  width: var(--eye, 12.4%);
  aspect-ratio: 1;
  border-radius: 50%;
  background: #fff;
  transform: translate(-50%, -50%);
}
.pupil {
  position: absolute;
  width: 36%;
  aspect-ratio: 1;
  border-radius: 50%;
  background: #1B1A20;
  left: calc(50% + var(--mx) * var(--look, 16%));
  top: calc(50% + var(--my) * var(--look, 16%));
  transform: translate(-50%, -50%);
  transition: left .16s ease-out, top .16s ease-out;
}

.purple .e1 { left: 36.3%; top: 11.9%; }
.purple .e2 { left: 46.4%; top: 11.9%; }

.black .eyes { --eye: 19.4%; }
.black .e1 { left: 29.7%; top: 11.6%; }
.black .e2 { left: 45.5%; top: 11.6%; }

.orange .eyes { --eye: 9.7%; --look: 15%; }
.orange .eye { background: transparent; }
.orange .pupil { width: 45%; }
.orange .e1 { left: 44.4%; top: 40.5%; }
.orange .e2 { left: 50.3%; top: 40.5%; }

.yellow .eyes { --eye: 15%; --look: 15%; }
.yellow .eye { background: transparent; }
.yellow .pupil { width: 52%; }
.yellow .e1 { left: 40.9%; top: 13.2%; }
.yellow .e2 { left: 51.3%; top: 13.2%; }

.pane-foot {
  position: relative;
  z-index: 2;
  display: flex;
  gap: clamp(18px, 2.4vw, 34px);
}
.pane-foot button {
  background: none;
  border: 0;
  padding: 0;
  color: rgba(255, 255, 255, .62);
  font-family: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: color .2s;
}
.pane-foot button:hover { color: #fff; }

/* ---------------- 右：表单 ---------------- */
.pane-form {
  display: flex;
  padding: clamp(26px, 4vh, 56px) clamp(22px, 3.2vw, 56px);
  background: #fff;
  overflow-y: auto;
}
/* margin:auto 在 flex 里居中；内容高于视口时自动退化为可滚动的顶部对齐 */
.form-wrap { width: min(462px, 100%); margin: auto; }

.mobile-brand { display: none; }

.banned {
  margin-bottom: 18px;
  padding: 10px 14px;
  border-radius: 9px;
  background: #FDECEC;
  border: 1px solid #F5C6C0;
  color: #B03A2E;
  font-size: 12.5px;
  text-align: center;
}

.form-head { text-align: center; margin-bottom: clamp(32px, 5.2vh, 54px); }
.form-head h1 {
  font-family: var(--font-display);
  font-size: clamp(26px, 2.2vw, 33px);
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -.01em;
  color: #1B1926;
}
.form-head p { margin-top: 10px; font-size: 13.5px; color: var(--a-sub); }

.form { display: flex; flex-direction: column; gap: 20px; }
.field label {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #2C2939;
}
.field label svg { width: 14px; height: 14px; flex: none; }
.field input {
  width: 100%;
  height: 48px;
  padding: 0 15px;
  border-radius: 9px;
  border: 1px solid var(--a-line);
  background: var(--a-soft);
  color: var(--a-ink);
  font-family: inherit;
  font-size: 14px;
  transition: border-color .18s, box-shadow .18s, background-color .18s;
}
.field input::placeholder { color: #B6B4C1; }
.field input:focus {
  outline: none;
  border-color: #8C7BF3;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(108, 92, 231, .12);
}
.input-box { position: relative; }
.input-box input { padding-right: 46px; }
.peek {
  position: absolute;
  right: 7px;
  top: 50%;
  transform: translateY(-50%);
  width: 30px; height: 30px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 6px;
  background: none;
  color: #A9A7B4;
  cursor: pointer;
  transition: color .18s, background-color .18s;
}
.peek:hover { color: var(--a-primary); background: #F1EFFE; }
.peek svg { width: 17px; height: 17px; }

.between { display: flex; align-items: center; justify-content: space-between; margin-top: 26px; }
.remember {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  font-size: 13px;
  color: #4B4858;
  cursor: pointer;
}
.remember input { width: 16px; height: 16px; margin: 0; accent-color: var(--a-primary); cursor: pointer; }
.link {
  background: none;
  border: 0;
  padding: 0;
  color: var(--a-primary);
  font-family: inherit;
  font-size: 13px;
  cursor: pointer;
}
.link:hover { text-decoration: underline; }

.primary {
  margin-top: 14px;
  width: 100%;
  height: 48px;
  border: 0;
  border-radius: 9px;
  background: linear-gradient(100deg, var(--a-primary-2) 0%, #6159EE 100%);
  color: #fff;
  font-family: inherit;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: .02em;
  cursor: pointer;
  box-shadow: 0 10px 22px rgba(99, 88, 240, .26);
  transition: transform .16s ease, box-shadow .16s ease, filter .16s ease;
}
.primary:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 14px 28px rgba(99, 88, 240, .32); filter: brightness(1.03); }
.primary:active:not(:disabled) { transform: translateY(0); }
.primary:disabled { opacity: .62; cursor: not-allowed; box-shadow: none; }

.swap { margin-top: clamp(30px, 5vh, 46px); text-align: center; font-size: 13px; color: #6F6C7B; }
.swap button {
  margin-left: 4px;
  border: 0;
  background: none;
  padding: 0;
  color: var(--a-primary);
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.swap button:hover { text-decoration: underline; }
.back-home {
  display: block;
  margin-top: 18px;
  text-align: center;
  font-size: 12.5px;
  color: #7C6BF0;
}
.back-home:hover { text-decoration: underline; }

/* ---------------- 响应式 ---------------- */
@media (max-width: 62rem) {
  .auth {
    grid-template-columns: 1fr;
    height: auto;
    min-height: calc(100svh - var(--safe-top));
    overflow: visible;
  }
  .pane-brand { display: none; }
  .pane-form {
    display: block;
    padding: 20px 22px calc(38px + var(--safe-bottom));
    overflow: visible;
  }
  .mobile-brand {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: clamp(26px, 6vh, 48px);
  }
  .mobile-brand .brand { color: var(--a-ink); gap: 10px; }
  .mobile-brand .brand-mark {
    width: 30px; height: 30px;
    border-radius: 8px;
    background: var(--a-primary);
    color: #fff;
    font-size: 16px;
  }
  .mobile-brand .brand-name { font-size: 18px; }
  .back-link { font-size: 12px; color: #7C6BF0; }
  .form-wrap { margin: 0 auto; }
}

@media (prefers-reduced-motion: reduce) {
  .fig { transform: none; transition: none; }
  .pupil { transition: none; }
}
</style>
