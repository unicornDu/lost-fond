<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { t } from '../i18n'
import { assetUrl } from '../utils/asset'

/**
 * v2.20 守望订阅（对齐 Creator/过程资料/设计prompt/前端/守望订阅.md）。
 *
 * 设计取用：三档方案卡 + 月付/学期付切换（切换时撒花）+ 热门档抬升并加粗描边 +
 * 侧卡轻微透视收缩；数值权益全部来自 GET /api/watch/plans，文案按 code 走 i18n。
 * 下方「我的守望位」是真实数据：以图搜图页开启守望后出现在这里，可随时结束。
 *
 * 课程演示环境：不接真实支付，开通只落库方案权益（页脚有明示，避免误解为已扣费）。
 */
const router = useRouter()

const plans = ref([])
const overview = ref(null)
const loading = ref(true)
const busy = ref(false)
const cycle = ref('month')

const mineCode = computed(() => overview.value?.plan?.planCode || 'light')
const used = computed(() => overview.value?.used || 0)
const quota = computed(() => overview.value?.quota || 1)
const targets = computed(() => overview.value?.targets || [])
const pct = computed(() => Math.min(100, Math.round((used.value / Math.max(1, quota.value)) * 100)))

const mineIdx = computed(() => plans.value.findIndex((p) => p.code === mineCode.value))
const minePlan = computed(() => plans.value.find((p) => p.code === mineCode.value))
const savePct = computed(() => {
  const p = plans.value.find((x) => x.price > 0)
  if (!p || !p.price) return 0
  // 一学期按 120 天 ≈ 4 个月折算，仅用于展示「省 x%」
  return Math.max(0, Math.round((1 - p.termPrice / (p.price * 4)) * 100))
})

const planName = (p) => t(`watch.plan.${p?.code || 'light'}.name`)
const planTag = (p) => t(`watch.plan.${p.code}.tag`)

function money(p) {
  if (p.price === 0) return t('watch.free')
  return '¥' + (cycle.value === 'month' ? p.price : p.termPrice)
}

function feats(p) {
  return [
    t('watch.f.quota', { n: p.quota }),
    t('watch.f.threshold', { n: p.threshold }),
    t('watch.f.retention', { n: p.retentionDays }),
    t('watch.f.notify'),
    t('watch.f.flexible'),
  ]
}

function ctaText(p) {
  if (p.code === mineCode.value) return t('watch.current')
  const idx = plans.value.findIndex((x) => x.code === p.code)
  return idx > mineIdx.value
    ? t('watch.upgrade', { name: planName(p) })
    : t('watch.downgrade', { name: planName(p) })
}

function remainLabel(item) {
  if (item.remainingDays === null || item.remainingDays === undefined) return t('watch.targets.forever')
  return t('watch.targets.days', { n: item.remainingDays })
}

async function load() {
  loading.value = true
  try {
    const [ps, ov] = await Promise.all([api.watchPlans(), api.watchOverview()])
    plans.value = ps || []
    overview.value = ov
    if (ov?.plan?.cycle === 'semester') cycle.value = 'semester'
  } catch (e) {
    ElMessage.error(e.message || t('watch.loadFail'))
  } finally {
    loading.value = false
  }
}
onMounted(load)

function setCycle(next, ev) {
  if (cycle.value === next) return
  cycle.value = next
  const rect = ev?.currentTarget?.getBoundingClientRect?.()
  if (rect) burst(rect.left + rect.width / 2, rect.top + rect.height / 2)
}

async function choose(p, ev) {
  if (p.code === mineCode.value || busy.value) return
  busy.value = true
  try {
    await api.watchSubscribe({ planCode: p.code, cycle: cycle.value })
    overview.value = await api.watchOverview()
    ElMessage.success(t('watch.msg.ok', { name: planName(p) }))
    const rect = ev?.currentTarget?.getBoundingClientRect?.()
    if (rect) burst(rect.left + rect.width / 2, rect.top + rect.height / 2)
  } catch (e) {
    ElMessage.error(e.message || t('watch.msg.fail'))
  } finally {
    busy.value = false
  }
}

async function stop(item) {
  try {
    await ElMessageBox.confirm(
      t('watch.targets.confirm', { name: item.label }),
      t('watch.targets.remove'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning',
      },
    )
  } catch (e) {
    return
  }
  try {
    await api.watchRemoveTarget(item.id)
    overview.value = await api.watchOverview()
    ElMessage.success(t('watch.targets.removeOk'))
  } catch (e) {
    ElMessage.error(e.message || t('watch.msg.fail'))
  }
}

function goSearch() {
  router.push('/search-image')
}

/* ---------------- 撒花（切换周期 / 开通成功时的小反馈，零依赖） ---------------- */
let confettiCanvas = null
let confettiRaf = 0

function burst(x, y) {
  if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) return
  if (confettiCanvas) {
    cancelAnimationFrame(confettiRaf)
    confettiCanvas.remove()
    confettiCanvas = null
  }
  const dpr = Math.min(2, window.devicePixelRatio || 1)
  const cv = document.createElement('canvas')
  cv.style.cssText = 'position:fixed;inset:0;width:100%;height:100%;pointer-events:none;z-index:4000'
  cv.width = window.innerWidth * dpr
  cv.height = window.innerHeight * dpr
  document.body.appendChild(cv)
  const ctx = cv.getContext('2d')
  ctx.scale(dpr, dpr)

  const colors = ['#141412', '#8A6A1F', '#1E7A4C', '#C2402F', '#A3A39B']
  const parts = Array.from({ length: 46 }, () => {
    const angle = Math.random() * Math.PI * 2
    const speed = 3 + Math.random() * 6
    return {
      x, y,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed - 3,
      g: 0.16 + Math.random() * 0.12,
      s: 3 + Math.random() * 4,
      rot: Math.random() * Math.PI,
      vr: (Math.random() - 0.5) * 0.36,
      c: colors[(Math.random() * colors.length) | 0],
    }
  })

  const start = performance.now()
  const tick = (now) => {
    const elapsed = now - start
    ctx.clearRect(0, 0, window.innerWidth, window.innerHeight)
    for (const p of parts) {
      p.x += p.vx
      p.y += p.vy
      p.vy += p.g
      p.rot += p.vr
      ctx.save()
      ctx.globalAlpha = Math.max(0, 1 - elapsed / 1100)
      ctx.translate(p.x, p.y)
      ctx.rotate(p.rot)
      ctx.fillStyle = p.c
      ctx.fillRect(-p.s / 2, -p.s / 2, p.s, p.s * 0.62)
      ctx.restore()
    }
    if (elapsed < 1100) confettiRaf = requestAnimationFrame(tick)
    else {
      cv.remove()
      confettiCanvas = null
    }
  }
  confettiCanvas = cv
  confettiRaf = requestAnimationFrame(tick)
}

onBeforeUnmount(() => {
  if (confettiCanvas) {
    cancelAnimationFrame(confettiRaf)
    confettiCanvas.remove()
    confettiCanvas = null
  }
})
</script>

<template>
  <div class="watch">
    <header class="head">
      <div class="head-main">
        <p class="eyebrow">WATCH · {{ $t('nav.watch') }}</p>
        <h1 class="title">{{ $t('watch.title') }}</h1>
        <p class="sub">{{ $t('watch.sub') }}</p>
      </div>

      <div class="head-side">
        <p v-if="overview" class="chip">
          {{ overview.plan.expireTime
            ? $t('watch.chip.expire', { name: planName(minePlan), date: overview.plan.expireTime.slice(0, 10) })
            : $t('watch.chip.free', { name: planName(minePlan) }) }}
        </p>
        <div class="cycle" role="group" :aria-label="$t('watch.cycle.label')">
          <button type="button" :class="{ on: cycle === 'month' }" @click="setCycle('month', $event)">
            {{ $t('watch.cycle.month') }}
          </button>
          <button type="button" :class="{ on: cycle === 'semester' }" @click="setCycle('semester', $event)">
            {{ $t('watch.cycle.semester') }}
            <i v-if="savePct">-{{ savePct }}%</i>
          </button>
        </div>
      </div>
    </header>

    <ol class="how">
      <li><b>01</b><span>{{ $t('watch.how.1') }}</span></li>
      <li><b>02</b><span>{{ $t('watch.how.2') }}</span></li>
      <li><b>03</b><span>{{ $t('watch.how.3') }}</span></li>
    </ol>

    <div v-if="loading" class="skeleton">
      <div v-for="i in 3" :key="i" class="sk-card"></div>
    </div>

    <section v-else class="plans">
      <article
        v-for="(p, i) in plans" :key="p.code"
        class="plan" :class="{ pop: p.popular, mine: p.code === mineCode, left: i === 0, right: i === plans.length - 1 }"
        :style="{ '--i': i }"
      >
        <span v-if="p.popular" class="pop-tag">★ {{ $t('watch.popular') }}</span>
        <span v-if="p.code === mineCode" class="mine-tag">{{ $t('watch.current') }}</span>

        <p class="p-name">{{ planName(p) }}</p>
        <p class="p-tag">{{ planTag(p) }}</p>

        <div class="p-price">
          <Transition name="num" mode="out-in">
            <b :key="cycle + p.code">{{ money(p) }}</b>
          </Transition>
          <small v-if="p.price > 0">{{ cycle === 'month' ? $t('watch.per.month') : $t('watch.per.semester') }}</small>
        </div>

        <ul class="p-feats">
          <li v-for="f in feats(p)" :key="f">
            <span class="tick">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><path d="m5 13 4 4L19 7" /></svg>
            </span>
            {{ f }}
          </li>
        </ul>

        <button
          class="p-cta" type="button"
          :class="{ dark: p.popular }"
          :disabled="p.code === mineCode || busy"
          @click="choose(p, $event)"
        >
          {{ ctaText(p) }}
        </button>
        <p class="p-foot">{{ p.price === 0 ? $t('watch.foot.free') : $t('watch.foot.demo') }}</p>
      </article>
    </section>

    <section class="mine">
      <header class="mine-head">
        <div>
          <h2>{{ $t('watch.targets.title') }}</h2>
          <p class="mine-sub">{{ $t('watch.targets.used', { used, quota }) }}</p>
        </div>
        <button class="ghost" type="button" @click="goSearch">{{ $t('watch.targets.go') }}</button>
      </header>

      <div class="quota"><i :style="{ width: pct + '%' }"></i></div>

      <ul v-if="targets.length" class="targets">
        <li v-for="item in targets" :key="item.id" class="target">
          <img v-if="item.imgUrl" :src="assetUrl(item.imgUrl)" alt="" />
          <span v-else class="t-ph">🖼️</span>
          <div class="t-main">
            <b>{{ item.label }}</b>
            <span v-if="item.lastHitItemId" class="hit">
              {{ $t('watch.targets.hit', { name: item.lastHitTitle || '#' + item.lastHitItemId }) }}
            </span>
            <span v-else>{{ $t('watch.targets.watching') }}</span>
          </div>
          <small class="t-left">{{ remainLabel(item) }}</small>
          <RouterLink v-if="item.lastHitItemId" class="t-open" :to="`/item/${item.lastHitItemId}`">
            {{ $t('common.view') }}
          </RouterLink>
          <button class="t-stop" type="button" @click="stop(item)">{{ $t('watch.targets.remove') }}</button>
        </li>
      </ul>

      <div v-else class="empty">
        <p>{{ $t('watch.targets.empty') }}</p>
        <small>{{ $t('watch.targets.emptyHint') }}</small>
        <button class="ghost" type="button" @click="goSearch">{{ $t('watch.targets.go') }} →</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.watch { max-width: 1080px; margin: 0 auto; }

/* ---- 页头 ---- */
.head {
  display: flex; align-items: flex-end; justify-content: space-between;
  gap: 24px; flex-wrap: wrap; margin-bottom: 22px;
}
.eyebrow {
  font-size: 10.5px; font-weight: 700; letter-spacing: .18em;
  color: var(--ink-faint); text-transform: uppercase; margin-bottom: 8px;
}
.title { font-family: var(--font-display); font-size: 34px; font-weight: 800; letter-spacing: -.02em; line-height: 1.1; }
.sub {
  color: var(--ink-soft); font-size: 13.5px; line-height: 1.7;
  margin-top: 8px; max-width: 620px;
  text-wrap: pretty;   /* 避免末行只剩一两个字 */
}

.head-side { display: flex; flex-direction: column; align-items: flex-end; gap: 10px; }
.chip {
  font-size: 11.5px; font-weight: 600; color: var(--ink-soft);
  background: var(--cream-deep); border-radius: 999px; padding: 5px 11px;
}

/* 周期切换：与左导航同一套「轨道 + 白色药丸」 */
.cycle {
  display: inline-flex; gap: 3px; padding: 4px;
  background: var(--nav-track); border-radius: 999px;
}
.cycle button {
  display: inline-flex; align-items: center; gap: 6px;
  border: 0; background: transparent; color: var(--ink-soft);
  font-family: inherit; font-size: 12.5px; font-weight: 600;
  padding: 7px 14px; border-radius: 999px; cursor: pointer;
  transition: background-color .18s, color .18s, box-shadow .18s;
}
.cycle button:hover { color: var(--ink); }
.cycle button.on {
  background: var(--nav-pill); color: var(--ink);
  box-shadow: var(--nav-pill-shadow);
}
.cycle i { font-style: normal; font-size: 10.5px; font-weight: 800; color: var(--mint); }

/* ---- 三步说明 ---- */
.how {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px;
  list-style: none; margin-bottom: 26px;
}
.how li {
  display: flex; align-items: baseline; gap: 9px;
  font-size: 12.5px; color: var(--ink-soft); line-height: 1.6;
  padding: 12px 14px; border: 1px solid var(--line); border-radius: 14px;
  background: var(--paper);
}
.how b {
  font-family: var(--font-display); font-size: 11px; letter-spacing: .08em;
  color: var(--ink-faint); flex: none;
}

/* ---- 方案卡 ---- */
.plans {
  display: grid; grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px; align-items: stretch; margin-bottom: 34px;
}
.plan {
  position: relative; display: flex; flex-direction: column;
  padding: 26px 22px 20px;
  background: var(--paper); border: 1px solid var(--line); border-radius: 20px;
  box-shadow: var(--shadow-card);
  transition: transform .45s cubic-bezier(.22, 1, .36, 1), box-shadow .3s, border-color .3s;
  animation: card-in .6s cubic-bezier(.22, 1, .36, 1) both;
  animation-delay: calc(var(--i) * 70ms);
}
/* 用独立的 translate 属性做进场，避免与宽屏的 transform（抬升/透视）互相覆盖 */
@keyframes card-in {
  from { opacity: 0; translate: 0 16px; }
  to { opacity: 1; translate: 0 0; }
}
.plan.pop {
  border: 2px solid var(--ink);
  box-shadow: 0 18px 44px rgba(20, 20, 18, .12);
  padding-top: 30px; padding-bottom: 24px;
  z-index: 2;
}
.plan.mine { border-color: var(--ink-soft); }
.plan.mine.pop { border-color: var(--ink); }

.pop-tag {
  position: absolute; top: -12px; left: 50%; transform: translateX(-50%);
  display: inline-flex; align-items: center; gap: 5px; white-space: nowrap;
  background: var(--ink); color: var(--paper);
  font-size: 10.5px; font-weight: 700; letter-spacing: .04em;
  padding: 4px 12px; border-radius: 999px;
}
.mine-tag {
  position: absolute; top: 14px; right: 16px;
  font-size: 10px; font-weight: 700; letter-spacing: .1em;
  color: var(--ink-faint);
}
.p-name { font-family: var(--font-display); font-size: 17px; font-weight: 800; letter-spacing: .01em; }
.p-tag { color: var(--ink-soft); font-size: 12px; line-height: 1.6; margin-top: 5px; min-height: 19px; }

.p-price { display: flex; align-items: baseline; gap: 6px; margin: 18px 0 4px; min-height: 50px; }
.p-price b {
  font-family: var(--font-display); font-size: 42px; font-weight: 800;
  letter-spacing: -.03em; line-height: 1; font-variant-numeric: tabular-nums;
}
.p-price small { color: var(--ink-faint); font-size: 12px; font-weight: 600; }

.num-enter-active, .num-leave-active { transition: opacity .22s ease, transform .22s cubic-bezier(.22, 1, .36, 1); }
.num-enter-from { opacity: 0; transform: translateY(8px); }
.num-leave-to { opacity: 0; transform: translateY(-8px); }

.p-feats { list-style: none; display: flex; flex-direction: column; gap: 9px; margin: 16px 0 20px; flex: 1; }
.p-feats li { display: flex; align-items: flex-start; gap: 9px; font-size: 12.5px; line-height: 1.55; color: var(--ink-soft); }
.tick {
  width: 16px; height: 16px; border-radius: 50%; flex: none; margin-top: 1px;
  display: grid; place-items: center;
  background: var(--cream-deep); color: var(--ink);
}
.tick svg { width: 9px; height: 9px; }

.p-cta {
  width: 100%; height: 42px; border-radius: 999px; cursor: pointer;
  border: 1px solid var(--line-2); background: var(--paper); color: var(--ink);
  font-family: inherit; font-size: 13px; font-weight: 700;
  transition: transform .18s, box-shadow .18s, background-color .18s, color .18s, border-color .18s;
}
.p-cta:hover:not(:disabled) { transform: translateY(-1px); border-color: var(--ink); box-shadow: 0 8px 20px rgba(20, 20, 18, .12); }
.p-cta.dark { background: var(--ink); color: var(--paper); border-color: var(--ink); }
.p-cta.dark:hover:not(:disabled) { box-shadow: 0 10px 24px rgba(20, 20, 18, .22); }
.p-cta:disabled { cursor: default; opacity: .72; }
.p-foot { margin-top: 10px; font-size: 10.5px; line-height: 1.5; color: var(--ink-faint); text-align: center; }

/* ---- 我的守望位 ---- */
.mine {
  background: var(--paper); border: 1px solid var(--line);
  border-radius: 20px; padding: 20px 22px 14px; box-shadow: var(--shadow-card);
}
.mine-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.mine-head h2 { font-family: var(--font-display); font-size: 18px; font-weight: 800; letter-spacing: -.01em; }
.mine-sub { color: var(--ink-faint); font-size: 12px; margin-top: 4px; font-variant-numeric: tabular-nums; }

.quota { height: 6px; border-radius: 99px; background: var(--cream-deep); overflow: hidden; margin: 14px 0 6px; }
.quota i {
  display: block; height: 100%; border-radius: 99px; background: var(--ink);
  transition: width .6s cubic-bezier(.22, 1, .36, 1);
}

.targets { list-style: none; }
.target {
  display: grid; grid-template-columns: 64px minmax(0, 1fr) auto auto auto;
  gap: 12px; align-items: center;
  padding: 12px 0; border-top: 1px solid var(--line);
}
.target img, .t-ph {
  width: 64px; height: 48px; border-radius: 10px; object-fit: cover;
  background: var(--cream-deep); display: grid; place-items: center; font-size: 18px;
}
.t-main { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.t-main b { font-size: 13.5px; font-weight: 700; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.t-main span { font-size: 11.5px; color: var(--ink-faint); }
.t-main .hit { color: var(--mint); font-weight: 600; }
.t-left { font-size: 11.5px; color: var(--ink-faint); white-space: nowrap; }
.t-open {
  font-size: 11.5px; font-weight: 600; color: var(--ink-soft);
  border: 1px solid var(--line-2); border-radius: 999px; padding: 5px 10px; white-space: nowrap;
}
.t-open:hover { color: var(--ink); border-color: var(--ink); }
.t-stop {
  border: 0; background: transparent; color: var(--ink-faint);
  font-family: inherit; font-size: 11.5px; font-weight: 600;
  padding: 6px 4px; cursor: pointer; white-space: nowrap;
}
.t-stop:hover { color: var(--rose); }

.ghost {
  border: 1px solid var(--line-2); background: var(--paper); color: var(--ink-soft);
  border-radius: 999px; padding: 7px 14px;
  font-family: inherit; font-size: 12px; font-weight: 600; cursor: pointer;
  transition: color .16s, border-color .16s, background-color .16s;
}
.ghost:hover { color: var(--ink); border-color: var(--ink); background: var(--cream-deep); }

.empty {
  display: flex; flex-direction: column; align-items: flex-start; gap: 6px;
  padding: 26px 0 30px; border-top: 1px solid var(--line); margin-top: 8px;
}
.empty p { font-size: 14px; font-weight: 600; }
.empty small { color: var(--ink-faint); font-size: 12px; line-height: 1.6; }
.empty .ghost { margin-top: 8px; }

/* ---- 骨架 ---- */
.skeleton { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; margin-bottom: 34px; }
.sk-card {
  height: 360px; border-radius: 20px; border: 1px solid var(--line);
  background: linear-gradient(100deg, var(--paper) 20%, var(--cream-deep) 45%, var(--paper) 70%);
  background-size: 240% 100%; animation: shine 1.4s linear infinite;
}
@keyframes shine { from { background-position: 100% 0; } to { background-position: -100% 0; } }

/* ---- 宽屏：热门卡抬升，侧卡轻微透视（参考 prompt 的三卡舞台感） ---- */
@media (min-width: 1000px) {
  .plans { align-items: center; }
  .plan.pop { transform: translateY(-14px) scale(1.02); }
  .plan.left { transform: perspective(1200px) rotateY(6deg) scale(.97); transform-origin: right center; }
  .plan.right { transform: perspective(1200px) rotateY(-6deg) scale(.97); transform-origin: left center; }
  .plan.left:hover, .plan.right:hover { transform: perspective(1200px) rotateY(0) scale(1); }
  .plan.mine { border-width: 2px; }
}

@media (max-width: 999px) {
  .plans { grid-template-columns: 1fr; gap: 14px; }
  .plan.pop { padding-top: 26px; padding-bottom: 20px; }
  .how { grid-template-columns: 1fr; }
  .head-side { align-items: flex-start; width: 100%; }
  .skeleton { grid-template-columns: 1fr; }
  .sk-card { height: 220px; }
}
@media (max-width: 560px) {
  .title { font-size: 28px; }
  .target { grid-template-columns: 52px minmax(0, 1fr) auto; row-gap: 6px; }
  .target img, .t-ph { width: 52px; height: 40px; }
  .t-open { grid-column: 2; }
  .t-stop { grid-column: 3; }
  .t-left { grid-column: 3; grid-row: 1; }
}

@media (prefers-reduced-motion: reduce) {
  .plan { animation: none; }
  .plan.pop, .plan.left, .plan.right { transform: none; }
  .num-enter-active, .num-leave-active { transition: none; }
}
</style>
