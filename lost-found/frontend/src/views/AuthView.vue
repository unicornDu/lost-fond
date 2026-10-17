<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { assetUrl } from '../utils/desktop'   // 桌面端 /uploads 转绝对地址

/**
 * 登录 / 注册页（复刻 21st.dev sign-in：左表单 + 右大图评价卡，交错入场动画）
 * /auth?tab=register 直达注册；登录成功后进入主应用 /home
 */
const route = useRoute()
const router = useRouter()
const user = useUserStore()

const tab = ref(route.query.tab === 'register' ? 'register' : 'login')
const remember = ref(true)
const loading = ref(false)
const form = reactive({ account: '', password: '', password2: '', nickname: '' })
// E-05：被封禁踢出后的专属提示（stores/user.refresh 写入 sessionStorage，登录成功即清除）
const bannedMsg = ref(sessionStorage.getItem('shiguang_banned_msg') || '')
onMounted(() => { if (bannedMsg.value) sessionStorage.removeItem('shiguang_banned_msg') })

function switchTab(t) {
  tab.value = t
  router.replace({ query: { ...route.query, tab: t } })
}

async function submit() {
  if (!/^\d{6,12}$/.test(form.account.trim())) return ElMessage.warning('请输入 6-12 位数字学号')
  if (form.password.length < 6) return ElMessage.warning('密码至少 6 位')
  if (tab.value === 'register' && form.password !== form.password2) {
    return ElMessage.warning('两次输入的密码不一致')
  }
  loading.value = true
  try {
    if (tab.value === 'register') {
      await user.register({
        account: form.account.trim(),
        password: form.password,
        nickname: form.nickname.trim(),
      })
      ElMessage.success('注册成功，已自动登录')
    } else {
      await user.login({ account: form.account.trim(), password: form.password })
      ElMessage.success(`欢迎回来，${user.info.nickname || user.info.account}！`)
    }
    router.push(route.query.redirect || '/home')
  } catch (e) {
    ElMessage.error(e.message || '操作失败，请重试')
  } finally {
    loading.value = false
  }
}

// E-06：忘记密码给出明确指引（后端无自助重置通道，属既定范围，不再用"演示"占位）
function forgotPwd() {
  ElMessageBox.alert('平台暂未接入自助找回：请联系管理员重置密码（管理后台可处理）。', '找回密码', {
    confirmButtonText: '知道了',
  })
}

</script>

<template>
  <div class="auth-page">
    <RouterLink to="/" class="mini-brand fade-up" style="--i:1">
      <span class="mini-logo">拾</span>
      <span>返回拾光官网</span>
    </RouterLink>

    <!-- E-05：封禁提示条（常驻顶部，直到下次登录成功） -->
    <div v-if="bannedMsg" class="banned-banner">⛔ {{ bannedMsg }}</div>

    <div class="split">
      <!-- 左：表单 -->
      <section class="form-side">
        <div class="form-box">
          <div class="seg fade-up" style="--i:2">
            <button :class="{ on: tab === 'login' }" @click="switchTab('login')">登 录</button>
            <button :class="{ on: tab === 'register' }" @click="switchTab('register')">注 册</button>
          </div>

          <h1 class="title fade-up" style="--i:3">{{ tab === 'login' ? '欢迎回来' : '加入拾光' }}</h1>
          <p class="desc fade-up" style="--i:4">
            {{ tab === 'login' ? '登录你的账号，继续找回失物的旅程' : '注册只需 30 秒，全校失物招领即刻为你所用' }}
          </p>

          <form @submit.prevent="submit" class="fields">
            <div v-if="tab === 'register'" class="field fade-up" style="--i:5">
              <label>昵称</label>
              <div class="glass">
                <el-input v-model="form.nickname" placeholder="大家怎么称呼你" maxlength="12" />
              </div>
            </div>

            <div class="field fade-up" style="--i:6">
              <label>学号</label>
              <div class="glass">
                <el-input v-model="form.account" placeholder="请输入你的学号" maxlength="12" />
              </div>
            </div>

            <div class="field fade-up" style="--i:7">
              <label>密码</label>
              <div class="glass">
                <el-input v-model="form.password" type="password" show-password placeholder="请输入密码（至少 6 位）" />
              </div>
            </div>

            <div v-if="tab === 'register'" class="field fade-up" style="--i:8">
              <label>确认密码</label>
              <div class="glass">
                <el-input v-model="form.password2" type="password" show-password placeholder="再输入一次密码" />
              </div>
            </div>

            <div v-if="tab === 'login'" class="row fade-up" style="--i:8">
              <el-checkbox v-model="remember">记住我</el-checkbox>
              <a class="link" @click.prevent="forgotPwd">忘记密码？</a>
            </div>

            <button type="submit" class="submit fade-up" :disabled="loading" :style="{ '--i': tab === 'login' ? 9 : 10 }">
              {{ loading ? '处理中…' : (tab === 'login' ? '登 录' : '注册并登录') }}
            </button>
          </form>

          <!-- E-06：微信登录占位按钮已删除（无接入计划的占位比明确没有更差） -->

          <p class="swap fade-up" :style="{ '--i': tab === 'login' ? 11 : 12 }">
            {{ tab === 'login' ? '还没有账号？' : '已经有账号了？' }}
            <a @click.prevent="switchTab(tab === 'login' ? 'register' : 'login')">
              {{ tab === 'login' ? '立即注册' : '直接登录' }}
            </a>
          </p>

          <div class="demo-tip fade-up" :style="{ '--i': tab === 'login' ? 13 : 14 }">
            🧪 已接入真实后端：注册需 6-12 位数字学号 + ≥6 位密码；登录使用 JWT 令牌。
          </div>
        </div>
      </section>

      <!-- 右：大图 + 悬浮评价卡 -->
      <section class="pic-side">
        <div class="pic fade-slide-right" style="--i:4">
          <img :src="assetUrl('/uploads/seed/umbrella-1.jpg')" alt="" />
          <div class="pic-overlay"></div>
          <div class="pic-cap">
            <b>拾光 · LOST &amp; FOUND</b>
            <span>让每一件失物都能回家</span>
          </div>
          <!-- E-06：右侧编造的用户评价卡已删除（WelcomeView 同类内容按 F-08 清理，此处为漏网残留） -->
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  position: relative;
  padding: 20px;
}
.mini-brand {
  position: absolute; top: 22px; left: 26px; z-index: 10;
  display: inline-flex; align-items: center; gap: 9px;
  color: var(--ink-soft); font-size: 14px; font-weight: 600;
  transition: color .16s;
}
.mini-brand:hover { color: var(--orange-deep); }
.mini-logo {
  display: grid; place-items: center;
  width: 34px; height: 34px; border-radius: 10px;
  background: linear-gradient(135deg, var(--orange), var(--orange-deep));
  color: #FFF7EC; font-family: var(--font-display); font-size: 18px;
  transform: rotate(-4deg);
  box-shadow: 0 5px 12px rgba(255, 125, 46, .35);
}

.split { display: flex; min-height: calc(100vh - 40px); }

/* E-05：封禁提示条 */
.banned-banner {
  position: sticky; top: 10px; z-index: 20;
  max-width: 560px; margin: 0 auto 4px;
  padding: 11px 18px; border-radius: 14px;
  background: #FDECEC; border: 1.5px solid #F5B7B1;
  color: #B03A2E; font-size: 14px; font-weight: 600; text-align: center;
}

/* ---- 左侧表单 ---- */
.form-side { flex: 1; display: grid; place-items: center; padding: 60px 20px; }
.form-box { width: min(430px, 100%); }

.seg {
  display: inline-flex; gap: 4px; padding: 5px;
  background: var(--cream-deep); border-radius: 999px; margin-bottom: 26px;
}
.seg button {
  border: 0; cursor: pointer;
  padding: 9px 30px; border-radius: 999px;
  background: transparent; color: var(--ink-soft);
  font-size: 14.5px; font-weight: 600; font-family: var(--font-body);
  transition: all .2s;
}
.seg button.on {
  background: var(--paper); color: var(--orange-deep);
  box-shadow: 0 4px 12px rgba(160, 100, 40, .18);
}

.title { font-size: 40px; line-height: 1.2; }
.desc { color: var(--ink-soft); font-size: 14.5px; margin: 12px 0 28px; }

.fields { display: flex; flex-direction: column; gap: 16px; }
.field label {
  display: block; font-size: 13px; font-weight: 600;
  color: var(--ink-soft); margin-bottom: 8px;
}
/* 玻璃质感输入壳（复刻 GlassInputWrapper） */
.glass {
  border-radius: 16px;
  border: 1.5px solid var(--line);
  background: rgba(255, 253, 248, .6);
  backdrop-filter: blur(6px);
  padding: 3px 6px;
  transition: border-color .2s, background .2s, box-shadow .2s;
}
.glass:focus-within {
  border-color: var(--orange);
  background: var(--orange-soft);
  box-shadow: 0 0 0 4px rgba(255, 125, 46, .12);
}
.glass :deep(.el-input__wrapper) {
  box-shadow: none !important; background: transparent;
  padding: 10px 12px;
}
.glass :deep(.el-input__inner) { font-size: 14.5px; }

.row { display: flex; justify-content: space-between; align-items: center; }
.link { color: var(--orange-deep); font-size: 13.5px; cursor: pointer; }
.link:hover { text-decoration: underline; }

.submit {
  width: 100%; border: 0; cursor: pointer;
  margin-top: 6px; padding: 15px;
  border-radius: 16px;
  background: linear-gradient(120deg, var(--orange), var(--orange-deep));
  color: #FFF7EC; font-size: 15.5px; font-weight: 700; font-family: var(--font-body);
  letter-spacing: 2px;
  box-shadow: 0 12px 26px rgba(255, 125, 46, .38);
  transition: transform .16s, box-shadow .16s;
}
.submit:hover { transform: translateY(-2px); box-shadow: 0 16px 32px rgba(255, 125, 46, .45); }

.swap { text-align: center; margin: 26px 0 14px; color: var(--ink-soft); font-size: 13.5px; }
.swap a { color: var(--orange-deep); font-weight: 700; cursor: pointer; }
.swap a:hover { text-decoration: underline; }

/* ---- 右侧大图 ---- */
.pic-side { flex: 1; padding: 18px 18px 18px 0; display: block; }
.pic {
  position: relative; height: calc(100vh - 76px); min-height: 560px;
  border-radius: 26px; overflow: hidden;
  box-shadow: var(--shadow-lift);
}
.pic img { width: 100%; height: 100%; object-fit: cover; display: block; }
.pic-overlay {
  position: absolute; inset: 0;
  background: linear-gradient(200deg, rgba(51, 41, 31, 0) 40%, rgba(51, 41, 31, .55));
}
.pic-cap {
  position: absolute; top: 26px; left: 28px;
  color: #FFF7EC; display: flex; flex-direction: column; gap: 4px;
}
.pic-cap b { font-family: var(--font-display); font-size: 22px; letter-spacing: 1px; }
.pic-cap span { font-size: 12.5px; opacity: .9; }

/* ---- 动画 ---- */
.fade-up { opacity: 0; animation: fadeUp .65s cubic-bezier(.22, 1, .36, 1) both; animation-delay: calc(var(--i) * 90ms); }
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(20px); filter: blur(4px); }
  to { opacity: 1; transform: translateY(0); filter: blur(0); }
}
.fade-slide-right { opacity: 0; animation: slideRightIn .8s cubic-bezier(.22, 1, .36, 1) both; animation-delay: calc(var(--i) * 90ms); }
@keyframes slideRightIn {
  from { opacity: 0; transform: translateX(46px); filter: blur(6px); }
  to { opacity: 1; transform: translateX(0); filter: blur(0); }
}
@media (prefers-reduced-motion: reduce) {
  .fade-up, .fade-slide-right { animation: none; opacity: 1; }
}

@media (max-width: 960px) {
  .pic-side { display: none; }
  .auth-page { padding: 16px; }
}
</style>
