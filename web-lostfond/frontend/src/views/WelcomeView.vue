<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { useUserStore } from '../stores/user'
// 轮播图使用 1600px 的优化版（ffmpeg scale=1600 + q:v 3，约 0.75 MB / 4 张）；
// 原始 01–04.jpg 保留在同目录作为母版，不参与打包。
import img01 from '../assets/img/01-1600.jpg'
import img02 from '../assets/img/02-1600.jpg'
import img03 from '../assets/img/03-1600.jpg'
import img04 from '../assets/img/04-1600.jpg'

/**
 * 公开首页（复刻 dsgninterior.se：北欧极简 editorial / 图像主导）
 *
 * 交互模型与目标站点一致：
 * - 页面本身不滚动（html.w-landing 锁 overflow），4 屏图片是一个固定舞台；
 * - 滚轮 / 触摸滑动 / 方向键一次只翻一屏，翻到第 4 屏就停住，不能继续向下；
 * - 关于 / 流程 / 能力 / 数据不再是页面向下滚动的内容，而是点击导航后弹出的整页面板。
 */
const router = useRouter()
const user = useUserStore()

const stats = ref({ reunited: 0, lostCount: 0, foundCount: 0 })
const statsAt = ref('')
const active = ref(0)
const menuOpen = ref(false)
const infoOpen = ref(false)
const infoTab = ref('about')
const infoBody = ref(null)

const slides = [
  {
    key: 'lost', to: { path: '/wall', query: { type: 'lost' } }, img: img02,
    idx: '02', label: '失物墙 · LOST WALL',
    title: '丢了东西，<br>先别急着找。',
  },
  {
    key: 'found', to: { path: '/wall', query: { type: 'found' } }, img: img03,
    idx: '03', label: '招领墙 · FOUND WALL',
    title: '捡到的东西，<br>在这里等主人。',
  },
  {
    key: 'search', to: '/search-image', img: img04,
    idx: '04', label: '以图搜图 · PHASH',
    title: '认不出它？<br>交给算法。',
  },
]

const INFO_TABS = [
  { key: 'about', label: '关于' },
  { key: 'flow', label: '流程' },
  { key: 'features', label: '能力' },
  { key: 'stats', label: '数据' },
]

const total = computed(() => slides.length + 1) // 4 屏
const totalActive = computed(() => (stats.value.lostCount || 0) + (stats.value.foundCount || 0))

function enter() {
  closeInfo()
  router.push(user.info ? '/home' : '/auth')
}

/** 每次进入 / 打开「数据」都重新统计：数字跟着库实时走，并把更新时间显示在说明里 */
async function refreshStats() {
  try {
    stats.value = await api.stats()
    statsAt.value = new Date().toLocaleTimeString('zh-CN', {
      hour: '2-digit', minute: '2-digit', hour12: false,
    })
  } catch (e) { /* 统计失败保留上一次的数字，不阻断页面 */ }
}

/* ---------------- 全屏翻页 ---------------- */
const SLIDE_LOCK = 1050 // 与 CSS 过渡时长一致：一次手势只消化一屏
let locked = false
let lockTimer = null
let acc = 0
let lastWheel = 0

function go(i) {
  const next = Math.max(0, Math.min(total.value - 1, i))
  if (next === active.value) return
  active.value = next
  locked = true
  clearTimeout(lockTimer)
  lockTimer = setTimeout(() => { locked = false }, SLIDE_LOCK)
}

function onWheel(e) {
  if (infoOpen.value || menuOpen.value) return // 面板内部自己滚动
  if (e.ctrlKey) return // 触控板缩放
  e.preventDefault()
  if (locked) return
  // 两次手势间隔超过 220ms 就重新累计，避免触控板残余增量攒到下一次
  const now = performance.now()
  if (now - lastWheel > 220) acc = 0
  lastWheel = now
  acc += e.deltaY
  if (Math.abs(acc) < 18) return
  const dir = acc > 0 ? 1 : -1
  acc = 0
  go(active.value + dir)
}

let touchY = 0
let touchX = 0
function onTouchStart(e) {
  if (infoOpen.value || menuOpen.value) return
  touchY = e.touches[0].clientY
  touchX = e.touches[0].clientX
}
function onTouchEnd(e) {
  if (infoOpen.value || menuOpen.value) return
  const dy = e.changedTouches[0].clientY - touchY
  const dx = e.changedTouches[0].clientX - touchX
  if (Math.abs(dy) < 46 || Math.abs(dy) < Math.abs(dx)) return
  go(active.value + (dy < 0 ? 1 : -1))
}

function onKeydown(e) {
  if (e.key === 'Escape') {
    if (infoOpen.value) closeInfo()
    else if (menuOpen.value) menuOpen.value = false
    return
  }
  if (infoOpen.value || menuOpen.value) return
  const tag = (e.target?.tagName || '').toLowerCase()
  if (tag === 'input' || tag === 'textarea') return
  if (['ArrowDown', 'PageDown', ' '].includes(e.key)) { e.preventDefault(); go(active.value + 1) }
  else if (['ArrowUp', 'PageUp'].includes(e.key)) { e.preventDefault(); go(active.value - 1) }
  else if (e.key === 'Home') { e.preventDefault(); go(0) }
  else if (e.key === 'End') { e.preventDefault(); go(total.value - 1) }
}

/* ---------------- 信息弹层（关于 / 流程 / 能力 / 数据） ---------------- */
let rafInfo = 0
function openInfo(key = 'about') {
  infoTab.value = key
  infoOpen.value = true
  menuOpen.value = false
  if (key === 'stats') refreshStats()
  nextTick(() => {
    scrollInfoTo(key, 'auto')
    infoBody.value?.focus?.()
  })
}
function closeInfo() {
  infoOpen.value = false
}
function scrollInfoTo(key, behavior = 'smooth') {
  const body = infoBody.value
  const el = document.getElementById(`info-${key}`)
  if (!body || !el) return
  body.scrollTo({ top: Math.max(0, el.offsetTop - 4), behavior })
}
function jumpInfo(key) {
  infoTab.value = key
  if (key === 'stats') refreshStats()
  scrollInfoTo(key)
}
function onInfoScroll() {
  if (rafInfo) return
  rafInfo = requestAnimationFrame(() => {
    rafInfo = 0
    const body = infoBody.value
    if (!body) return
    const y = body.scrollTop + Math.min(160, body.clientHeight * 0.25)
    let current = INFO_TABS[0].key
    for (const t of INFO_TABS) {
      const el = document.getElementById(`info-${t.key}`)
      if (el && el.offsetTop <= y) current = t.key
    }
    infoTab.value = current
  })
}

/* ---------------- 生命周期 ---------------- */
onMounted(async () => {
  document.documentElement.classList.add('w-landing')
  window.addEventListener('wheel', onWheel, { passive: false })
  window.addEventListener('keydown', onKeydown)
  await refreshStats()
})

onBeforeUnmount(() => {
  document.documentElement.classList.remove('w-landing')
  window.removeEventListener('wheel', onWheel)
  window.removeEventListener('keydown', onKeydown)
  clearTimeout(lockTimer)
  if (rafInfo) cancelAnimationFrame(rafInfo)
})
</script>

<template>
  <div class="w">
    <!-- 固定页头：始终压在图片上，反白 -->
    <header class="w-head light" :class="{ open: menuOpen, hidden: infoOpen }">
      <RouterLink to="/" class="wordmark">
        <strong>拾光</strong>
        <span>LOST &amp; FOUND</span>
      </RouterLink>

      <nav class="w-nav" aria-label="主导航">
        <button type="button" @click="openInfo('about')">关于</button>
        <button type="button" @click="openInfo('flow')">流程</button>
        <button type="button" @click="openInfo('features')">能力</button>
        <button type="button" @click="openInfo('stats')">数据</button>
        <button class="nav-cta" type="button" @click="enter">进入平台</button>
      </nav>

      <button class="menu-btn" type="button" :aria-expanded="menuOpen" @click="menuOpen = !menuOpen">
        <span class="t-open">菜单</span>
        <span class="t-close">关闭</span>
      </button>
    </header>

    <!-- 移动端全屏菜单 -->
    <nav class="menu-overlay" aria-label="移动端导航">
      <button type="button" @click="openInfo('about')">关于拾光</button>
      <button type="button" @click="openInfo('flow')">使用流程</button>
      <button type="button" @click="openInfo('features')">平台能力</button>
      <button type="button" @click="openInfo('stats')">平台数据</button>
      <div class="menu-actions">
        <button class="nav-cta" type="button" @click="menuOpen = false; enter()">进入平台</button>
      </div>
    </nav>

    <!-- 固定舞台：4 屏，竖向翻页，滚到底即止 -->
    <div
      class="deck"
      :style="{ '--cur': active }"
      @touchstart.passive="onTouchStart"
      @touchend.passive="onTouchEnd"
    >
      <!-- 01 介绍屏 -->
      <section class="slide intro" :class="{ on: active === 0 }" :style="{ '--i': 0 }">
        <div class="media"><img :src="img01" alt="毛毡质感的黑猫趴在蓝色的水面上" /></div>
        <span class="scrim"></span>
        <div class="slide-in">
          <h1 class="giant">让每一件失物<br />都能回家</h1>
          <p class="lede">拍一张照片，平台在全校启事里认出「长得一样」的东西。<br />不用编关键词，也不用碰运气。</p>
        </div>
        <div class="slide-foot">
          <span class="idx">01 — 介绍</span>
          <span class="foot-note">已找回 <b>{{ stats.reunited || 0 }}</b> 件 · 在流转 <b>{{ totalActive }}</b> 条</span>
          <span class="hint">向下滚动<i class="vline"></i></span>
        </div>
      </section>

      <!-- 02-04 三屏入口（整屏可点） -->
      <RouterLink
        v-for="(s, i) in slides"
        :key="s.key"
        :to="s.to"
        class="slide link"
        :class="{ on: active === i + 1 }"
        :style="{ '--i': i + 1 }"
      >
        <div class="media"><img :src="s.img" alt="" /></div>
        <span class="scrim"></span>
        <div class="slide-in">
          <h2 class="giant" v-html="s.title"></h2>
        </div>
        <div class="slide-foot">
          <span class="idx">{{ s.idx }} — {{ s.label.split(' · ')[0] }}</span>
          <span class="arrow">
            进入
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M5 12h14m-6-7 7 7-7 7" /></svg>
          </span>
        </div>
      </RouterLink>
    </div>

    <!-- 右侧页码 -->
    <div class="ticks" :class="{ hidden: infoOpen }" aria-hidden="true">
      <button v-for="n in total" :key="n" type="button" :class="{ on: active === n - 1 }" @click="go(n - 1)">
        <span>0{{ n }}</span><i></i>
      </button>
    </div>

    <!-- ============ 信息弹层：关于 / 流程 / 能力 / 数据 ============ -->
    <Teleport to="body">
      <div v-if="infoOpen" class="info" role="dialog" aria-modal="true" aria-label="拾光介绍" @click.self="closeInfo">
        <div class="info-panel">
          <div class="info-bar">
            <span class="info-brand"><b>拾光</b><i>LOST &amp; FOUND</i></span>
            <nav class="info-nav" aria-label="介绍导航">
              <button
                v-for="t in INFO_TABS"
                :key="t.key"
                type="button"
                :class="{ on: infoTab === t.key }"
                @click="jumpInfo(t.key)"
              >{{ t.label }}</button>
            </nav>
            <button class="nav-cta info-enter" type="button" @click="enter">进入平台</button>
            <button class="info-close" type="button" aria-label="关闭" @click="closeInfo">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M6 6l12 12M18 6L6 18" /></svg>
            </button>
          </div>

          <div ref="infoBody" class="info-body" tabindex="-1" @scroll.passive="onInfoScroll">
            <!-- 01 关于 -->
            <section id="info-about" class="sec about">
              <div class="sec-head">
                <p class="label">01 — 关于拾光</p>
                <h2 class="h2">不是另一张公告栏，<br />而是一条回家的路。</h2>
              </div>

              <div class="about-grid">
                <div class="about-copy">
                  <p class="lead">
                    拾光把「以图搜图」带进校园失物招领。丢了东西，不必回忆上周三到底把杯子放在哪一排；
                    捡到东西，也不必用文字去描述它的样子——照片本身就是最好的线索。
                  </p>
                  <p>
                    每条启事都要经过管理员审核才会出现在墙上；认领需要说出只有失主才知道的细节，
                    由发布者确认。找回不是一次碰运气，而是一条有据可查的流程。
                  </p>

                  <ul class="index-list">
                    <li><span>01</span><div><b>校园身份</b><em>学号注册，实名发布，减少无效信息</em></div></li>
                    <li><span>02</span><div><b>审核上墙</b><em>启事经管理员审核后才对外可见</em></div></li>
                    <li><span>03</span><div><b>凭证认领</b><em>描述只有失主知道的特征，双方确认后交接</em></div></li>
                  </ul>
                </div>

                <figure class="about-fig">
                  <img :src="img03" alt="插画风格的山谷与小猫" loading="lazy" />
                  <figcaption>演示素材 · 校园里的一次归还</figcaption>
                </figure>
              </div>
            </section>

            <!-- 02 流程 -->
            <section id="info-flow" class="sec flow">
              <div class="sec-head">
                <p class="label">02 — 使用流程</p>
                <h2 class="h2">三步，<br />让东西回到主人手里。</h2>
              </div>

              <div class="flow-grid">
                <article class="flow-item">
                  <span class="no">01</span>
                  <h3>拍照发布</h3>
                  <p>丢了或捡到东西，拍张照片、写清地点，30 秒生成一条启事，等待审核上墙。</p>
                </article>
                <article class="flow-item">
                  <span class="no">02</span>
                  <h3>以图搜图</h3>
                  <p>上传照片，系统为它生成 64 位感知哈希指纹，在全校启事里找出「长得一样」的那几张。</p>
                </article>
                <article class="flow-item">
                  <span class="no">03</span>
                  <h3>凭证交接</h3>
                  <p>凭只有失主知道的细节发起认领，发布者确认后约定地点，线下交接并标记「已找回」。</p>
                </article>
              </div>
            </section>

            <!-- 03 能力 -->
            <section id="info-features" class="sec features">
              <div class="sec-head">
                <p class="label">03 — 平台能力</p>
                <h2 class="h2">认得长相的搜索，<br />也管得住流程。</h2>
              </div>

              <div class="feat-grid">
                <article class="feat">
                  <span class="no">01</span>
                  <h3>以图搜图</h3>
                  <p>pHash 感知哈希：64 位指纹 + 汉明距离比对，实测召回率 85.7%，误检为 0，单张约 54 ms。</p>
                </article>
                <article class="feat">
                  <span class="no">02</span>
                  <h3>全流程状态</h3>
                  <p>寻找中 → 待认领 → 已认领 → 已找回，每一步都有记录，认领进度像快递一样可查。</p>
                </article>
                <article class="feat">
                  <span class="no">03</span>
                  <h3>审核与凭证</h3>
                  <p>启事审核后才上墙；认领必须提交凭证并由发布者确认，全程站内消息留痕。</p>
                </article>
                <article class="feat">
                  <span class="no">04</span>
                  <h3>校园身份</h3>
                  <p>学号注册、角色分权，管理员可审核启事、封禁账号，操作写入日志可追溯。</p>
                </article>
              </div>
            </section>

            <!-- 04 数据 -->
            <section id="info-stats" class="sec stats">
              <div class="sec-head">
                <p class="label">04 — 平台数据</p>
                <h2 class="h2">数字会说话。</h2>
              </div>

              <div class="stat-grid">
                <div class="stat"><b>{{ stats.reunited || 0 }}</b><span>件失物已找到主人</span></div>
                <div class="stat"><b>{{ stats.lostCount || 0 }}</b><span>条寻物启事在流转</span></div>
                <div class="stat"><b>{{ stats.foundCount || 0 }}</b><span>条招领启事待认领</span></div>
                <div class="stat"><b>85.7<em>%</em></b><span>pHash 实测召回率</span></div>
              </div>
              <p class="stat-note">
                数据来自平台实时统计<span v-if="statsAt">（更新于 {{ statsAt }}）</span>；召回率取自 56 组查询变体的离线实验（阈值 12 / Top-10）。
              </p>
            </section>

            <!-- CTA -->
            <section class="cta">
              <div class="cta-in">
                <p class="label light">现在开始</p>
                <h2>让丢掉的东西<br />回家。</h2>
                <p class="cta-sub">注册只需 30 秒。全校的失物与招领启事，都在等你。</p>
                <div class="btns">
                  <button class="pill paper" type="button" @click="enter">
                    {{ user.info ? '进入拾光' : '免费注册' }}
                  </button>
                  <RouterLink to="/wall?type=lost" class="text-link" @click="closeInfo">先看看失物墙 →</RouterLink>
                </div>
              </div>
            </section>

            <!-- 页脚 -->
            <footer class="w-foot visible">
              <div class="foot-grid">
                <div class="foot-brand">
                  <strong>拾光</strong>
                  <span>LOST &amp; FOUND</span>
                  <p>校园失物招领平台 · 以图搜图</p>
                </div>
                <div class="foot-col">
                  <span class="fh">快速入口</span>
                  <RouterLink to="/wall?type=lost" @click="closeInfo">失物墙</RouterLink>
                  <RouterLink to="/wall?type=found" @click="closeInfo">招领墙</RouterLink>
                  <RouterLink to="/search-image" @click="closeInfo">以图搜图</RouterLink>
                </div>
                <div class="foot-col">
                  <span class="fh">说明</span>
                  <p>软件工程课程设计 · 小组项目</p>
                  <p>演示数据，仅用于教学与答辩</p>
                  <p>Spring Boot · Vue 3 · MySQL</p>
                </div>
                <div class="foot-top">
                  <button type="button" @click="jumpInfo('about')">回到顶部<i class="vline"></i></button>
                </div>
              </div>
              <div class="foot-base">
                <span>© 2026 拾光 LOST &amp; FOUND</span>
                <span>以图搜图基于 pHash 感知哈希</span>
              </div>
            </footer>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
/* ============================================================
   令牌：暖米白 / 深棕 / 陶土橘（仅用于焦点描边）
   字体：标题思源宋体，正文思源黑体，拉丁标签 Century Gothic
   ============================================================ */
.w,
.info {
  --w-text: #594037;
  --w-bg: #F6F5F4;
  --w-accent: #E68762;
  --w-display: 'Noto Serif SC', 'Noto Serif', 'Songti SC', 'STSong', Georgia, serif;
  --w-sans: 'Noto Sans SC', 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  --w-latin: 'Century Gothic', 'Noto Sans', 'Segoe UI', sans-serif;
  --w-gap: clamp(.75rem, .66rem + .43vw, 1.125rem);
  --w-pad: clamp(1.125rem, .93rem + .99vw, 2rem);
  --w-head: clamp(3.5rem, 3.41rem + .43vw, 3.875rem);
  --w-expo-out: cubic-bezier(.16, 1, .3, 1);
  --w-expo-in-out: cubic-bezier(.9, 0, .1, 1);
  --w-slide-ease: cubic-bezier(.76, 0, .24, 1);
  --w-slide-dur: 1050ms;
}
.w {
  background: var(--w-bg);
  color: var(--w-text);
  font-family: var(--w-sans);
  font-size: 1.0625rem;
  line-height: 1.5;
  -webkit-font-smoothing: antialiased;
  height: 100svh;
  overflow: hidden;
}
.w img, .info img { display: block; max-width: 100%; }
.w :focus-visible, .info :focus-visible { outline: max(2px, .08rem) solid var(--w-accent); outline-offset: 2px; }

/* ---------------- 页头 ---------------- */
.w-head {
  position: fixed; inset: 0 0 auto 0; z-index: 100;
  height: var(--w-head);
  display: grid; grid-template-columns: 1fr auto;
  color: var(--w-bg);
  text-shadow: 0 1px 16px hsl(23 26% 10% / .55);
  border-bottom: 1px solid transparent;
  padding-top: var(--safe-top);
  transition: opacity .35s ease, color .3s ease;
}
.w-head.hidden { opacity: 0; pointer-events: none; }
/* 菜单展开时页头压在深棕覆盖层上 */
.w-head.open { color: var(--w-bg); }
.wordmark {
  display: flex; align-items: baseline; gap: .55em;
  padding: 0 var(--w-pad);
  line-height: var(--w-head);
  white-space: nowrap;
}
.wordmark strong { font-family: var(--w-display); font-size: 1.375rem; font-weight: 600; letter-spacing: .04em; }
.wordmark span { font-family: var(--w-latin); font-size: .625rem; letter-spacing: .22em; opacity: .72; }

.w-nav { display: flex; align-items: center; gap: clamp(.9rem, .55rem + 1.1vw, 1.9rem); padding-right: var(--w-pad); }
.w-nav > button:not(.nav-cta) {
  font-size: .9375rem; font-weight: 400; line-height: var(--w-head);
  background: none; border: 0; color: inherit; font-family: inherit;
  cursor: pointer; padding: 0;
  transition: opacity .3s ease;
}
.w-nav > button:not(.nav-cta):hover { opacity: .55; }
.nav-cta {
  font-family: inherit; font-size: .875rem; font-weight: 500;
  padding: .6em 1.35em; border-radius: 999px;
  border: 1px solid currentColor; background: transparent; color: inherit;
  cursor: pointer;
  transition: background-color .3s ease, color .3s ease, opacity .3s ease;
}
.nav-cta:hover { background: var(--w-bg); color: var(--w-text); }

.menu-btn {
  display: none;
  background: none; border: 0; color: inherit;
  font-family: inherit; font-size: .9375rem;
  padding: 0 var(--w-pad); cursor: pointer;
}
.menu-btn span { transition: opacity .3s ease, transform .3s var(--w-expo-out); }
.menu-btn .t-close { display: none; }
.w-head.open .t-open { display: none; }
.w-head.open .t-close { display: inline; }

.menu-overlay {
  position: fixed; inset: 0; z-index: 90;
  background: var(--w-text); color: var(--w-bg);
  display: grid; align-content: center; gap: .35em;
  padding: var(--w-pad); padding-top: var(--w-head);
  transform: translateY(-100%); visibility: hidden;
  transition: transform .6s var(--w-expo-in-out), visibility .6s;
}
.w-head.open + .menu-overlay { transform: none; visibility: visible; }
.menu-overlay > button {
  background: none; border: 0; color: inherit; text-align: left; padding: 0;
  font-family: var(--w-display); font-size: clamp(1.9rem, 1.4rem + 2.2vw, 2.75rem);
  font-weight: 500; line-height: 1.2;
  opacity: 0; transform: translateY(.5em);
  transition: opacity .4s var(--w-expo-out), transform .4s var(--w-expo-out);
}
.w-head.open + .menu-overlay > button { opacity: 1; transform: none; }
.w-head.open + .menu-overlay > button:nth-child(1) { transition-delay: .14s; }
.w-head.open + .menu-overlay > button:nth-child(2) { transition-delay: .2s; }
.w-head.open + .menu-overlay > button:nth-child(3) { transition-delay: .26s; }
.w-head.open + .menu-overlay > button:nth-child(4) { transition-delay: .32s; }
.menu-actions { display: flex; gap: 1rem; align-items: center; margin-top: 2rem; }
.menu-actions .nav-cta { color: var(--w-bg); }

/* ---------------- 固定舞台：4 屏竖向翻页 ---------------- */
.deck {
  position: fixed; inset: 0; z-index: 1;
  overflow: hidden;
  background: hsl(23 24% 12%);
}
.slide {
  position: absolute; inset: 0;
  display: block;
  height: 100svh;
  overflow: hidden;
  background: var(--w-text);
  color: var(--w-bg);
  transform: translate3d(0, calc((var(--i) - var(--cur)) * 100%), 0);
  transition: transform var(--w-slide-dur) var(--w-slide-ease);
  will-change: transform;
}
.slide:not(.on) { pointer-events: none; }
.slide .media { position: absolute; inset: 0; }
.slide .media img {
  width: 100%; height: 100%; object-fit: cover;
  transform: scale(1.1);
  transition: transform 1.7s var(--w-expo-out);
}
.slide.on .media img { transform: scale(1); }
.scrim {
  position: absolute; inset: 0;
  /* 只压暗顶部（保证页头反白可读），画面主体保持通透 */
  background: linear-gradient(180deg, hsl(23 26% 12% / .74) 0%, hsl(23 26% 14% / .34) 26%, transparent 50%);
}
.slide-in {
  position: absolute; inset: 0;
  display: grid; align-content: end; justify-items: center;
  text-align: center;
  padding: calc(var(--w-head) + 1rem) var(--w-pad) clamp(4.5rem, 10vh, 7rem);
  /* 文字区局部压暗：一块柔和的暗池，不牺牲整张照片的亮度 */
  background: radial-gradient(80% 66% at 50% 94%,
    hsl(23 26% 12% / .78) 0%, hsl(23 26% 14% / .48) 48%, hsl(23 26% 14% / 0) 78%);
}
.giant {
  font-family: var(--w-display);
  font-size: clamp(2.85rem, 1.9rem + 5.1vw, 7.75rem);
  font-weight: 500;
  line-height: 1.06;
  letter-spacing: -.01em;
  margin-left: -.05em;   /* 中文标点/字形视觉补偿 */
  max-width: 16em;
  text-shadow: 0 2px 40px hsl(23 26% 12% / .28);
  opacity: 0;
  transform: translateY(3.5vh);
  transition: opacity .9s var(--w-expo-out) .16s, transform 1.1s var(--w-expo-out) .16s;
}
.slide.on .giant { opacity: 1; transform: none; }
.lede {
  margin-top: clamp(1.2rem, .8rem + 1vw, 2rem);
  font-size: clamp(1rem, .94rem + .3vw, 1.1875rem);
  line-height: 1.9;
  max-width: 32em;
  opacity: 0;
  transform: translateY(2.4vh);
  text-shadow: 0 1px 24px hsl(23 26% 10% / .45);
  transition: opacity .9s var(--w-expo-out) .3s, transform 1.1s var(--w-expo-out) .3s;
}
.slide.on .lede { opacity: .94; transform: none; }

.btns { display: flex; gap: .9rem; flex-wrap: wrap; justify-content: center; margin-top: clamp(1.6rem, 1rem + 1.6vw, 2.6rem); }
.pill {
  display: inline-flex; align-items: center; justify-content: center;
  font-family: inherit; font-size: .9375rem; font-weight: 500;
  padding: .85em 1.7em; border-radius: 999px; cursor: pointer;
  border: 1px solid transparent;
  transition: background-color .3s ease, color .3s ease, border-color .3s ease, transform .3s var(--w-expo-out);
}
.pill.paper { background: var(--w-bg); color: var(--w-text); }
.pill.paper:hover { transform: translateY(-2px); }

.slide-foot {
  position: absolute; left: 0; right: 0; bottom: 0;
  display: flex; align-items: center; gap: 1.5rem;
  padding: 0 var(--w-pad) clamp(1.1rem, .7rem + 1vw, 1.75rem);
  font-family: var(--w-latin);
  font-size: .6875rem; letter-spacing: .16em;
  color: rgba(246, 245, 244, .82);
  pointer-events: none;
}
.idx { text-transform: uppercase; }
.foot-note { margin-left: auto; letter-spacing: .06em; font-family: var(--w-sans); font-size: .75rem; opacity: .92; }
.foot-note b { font-weight: 600; }
.hint { display: inline-flex; align-items: center; gap: .7em; margin-left: auto; letter-spacing: .12em; }
.vline {
  display: inline-block; width: 1px; height: 2.2em;
  background: currentColor;
  animation: wScroll 2.2s var(--w-expo-in-out) infinite;
  transform-origin: top;
}
@keyframes wScroll {
  0% { transform: translateY(-30%) scaleY(.55); opacity: 0; }
  33% { transform: translateY(-8%) scaleY(1); opacity: 1; }
  66% { opacity: 1; }
  100% { transform: translateY(22%) scaleY(.9); opacity: 0; }
}
.slide.link .slide-in {
  /* 02–04 入口屏只有一句标题：垂直居中，避免压在下三分之一 */
  align-content: center;
  padding-bottom: calc(var(--w-head) + 1rem);
  background:
    radial-gradient(80% 62% at 50% 50%,
      hsl(23 26% 12% / .74) 0%, hsl(23 26% 14% / .44) 48%, hsl(23 26% 14% / 0) 78%),
    /* 底部留一层薄暗，压住 02 屏那种浅色桌面，保证页脚反白可读 */
    linear-gradient(180deg, transparent 82%, hsl(23 26% 10% / .46) 100%);
  transition: transform .8s var(--w-expo-out);
}
.slide.link:hover .slide-in { transform: scale(1.012); }
.arrow {
  margin-left: auto;
  display: inline-flex; align-items: center; gap: .6em;
  font-family: var(--w-sans); letter-spacing: .08em;
}
.arrow svg { width: 1.35em; height: 1.35em; transition: transform .4s var(--w-expo-out); }
.slide.link:hover .arrow svg { transform: translateX(.28em); }

/* 右侧页码 */
.ticks {
  position: fixed; right: clamp(1rem, .4rem + 1.2vw, 2rem); top: 50%;
  transform: translateY(-50%); z-index: 80;
  display: flex; flex-direction: column; gap: .85rem;
  padding: .8rem .6rem; border-radius: 999px;
  background: hsl(23 26% 12% / .34);
  backdrop-filter: blur(10px);
  border: 1px solid hsl(23 26% 80% / .14);
  color: rgba(246, 245, 244, .55);
  transition: opacity .4s ease;
}
.ticks.hidden { opacity: 0; pointer-events: none; }
.ticks button {
  display: flex; align-items: center; gap: .55em;
  background: none; border: 0; color: inherit; cursor: pointer;
  font-family: var(--w-latin); font-size: .5625rem; letter-spacing: .14em;
  padding: 0;
}
.ticks i { display: block; width: 1.5rem; height: 1px; background: currentColor; opacity: .5; transition: width .4s var(--w-expo-out), opacity .3s; }
.ticks button.on { color: #F6F5F4; }
.ticks button:hover { color: rgba(246, 245, 244, .9); }
.ticks button.on i { width: 2.6rem; opacity: 1; }

/* ---------------- 信息弹层 ---------------- */
.info {
  position: fixed; inset: 0; z-index: 200;
  display: grid; place-items: center;
  padding: clamp(0rem, -1rem + 3vw, 2rem);
  background: hsl(23 24% 10% / .58);
  backdrop-filter: blur(16px) saturate(1.05);
  color: var(--w-text);
  font-family: var(--w-sans);
  animation: infoFade .35s ease both;
}
@keyframes infoFade { from { opacity: 0; } to { opacity: 1; } }
.info-panel {
  width: min(74rem, 100%);
  height: min(90svh, 58rem);
  display: flex; flex-direction: column;
  background: var(--w-bg);
  box-shadow: 0 2rem 6rem hsl(23 24% 6% / .45);
  animation: panelIn .62s var(--w-expo-out) both;
  overflow: hidden;
}
@keyframes panelIn {
  from { opacity: 0; transform: translateY(3.5rem) scale(.985); }
  to { opacity: 1; transform: none; }
}
.info-bar {
  flex: none;
  display: flex; align-items: center; gap: clamp(1rem, .6rem + 1.2vw, 2rem);
  height: var(--w-head);
  padding: 0 clamp(1rem, .6rem + 1.2vw, 1.75rem);
  border-bottom: 1px solid rgba(89, 64, 55, .14);
  background: var(--w-bg);
}
.info-brand { display: flex; align-items: baseline; gap: .5em; white-space: nowrap; }
.info-brand b { font-family: var(--w-display); font-size: 1.25rem; font-weight: 600; letter-spacing: .04em; }
.info-brand i { font-style: normal; font-family: var(--w-latin); font-size: .5rem; letter-spacing: .2em; color: rgba(89, 64, 55, .55); }
.info-nav { display: flex; align-items: center; gap: clamp(.9rem, .5rem + 1.1vw, 1.8rem); margin-left: auto; }
.info-nav button {
  position: relative;
  background: none; border: 0; padding: .35em 0;
  font-family: inherit; font-size: .9375rem; color: var(--w-text);
  cursor: pointer; opacity: .62;
  transition: opacity .3s ease;
}
.info-nav button::after {
  content: ''; position: absolute; left: 0; right: 0; bottom: 0;
  height: 1px; background: currentColor;
  transform: scaleX(0); transform-origin: left;
  transition: transform .45s var(--w-expo-out);
}
.info-nav button:hover { opacity: 1; }
.info-nav button.on { opacity: 1; }
.info-nav button.on::after { transform: none; }
.info-enter { color: var(--w-text); }
.info-enter:hover { background: var(--w-text); color: var(--w-bg); }
.info-close {
  flex: none;
  width: 2.4rem; height: 2.4rem;
  display: grid; place-items: center;
  background: none; border: 1px solid rgba(89, 64, 55, .25); border-radius: 999px;
  color: var(--w-text); cursor: pointer;
  transition: background-color .3s ease, color .3s ease, border-color .3s ease;
}
.info-close svg { width: 1rem; height: 1rem; }
.info-close:hover { background: var(--w-text); color: var(--w-bg); border-color: var(--w-text); }

.info-body {
  flex: 1; min-height: 0;
  position: relative;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
}
/* 面板滚动区会被程序聚焦，焦点环在这里没有意义，去掉（否则顶部出现一条橙线） */
.info-body:focus,
.info-body:focus-visible { outline: none; }
.info-body::-webkit-scrollbar { width: 8px; }
.info-body::-webkit-scrollbar-track { background: transparent; }
.info-body::-webkit-scrollbar-thumb { background: rgba(89, 64, 55, .22); border-radius: 8px; }

/* ---------------- 通用版式 ---------------- */
.sec {
  padding: clamp(3rem, 2rem + 3.4vw, 5.5rem) clamp(1.25rem, .8rem + 1.8vw, 3rem);
  max-width: 90rem; margin: 0 auto;
  /* 编辑网格：左侧 1 栏留给章节标签，正文占右侧 2 栏（subgrid 保证头图与内容严格对齐） */
  display: grid;
  grid-template-columns: minmax(0, .55fr) minmax(0, 1.2fr) minmax(0, 1.45fr);
  --w-col-gap: clamp(1rem, .4rem + 2.4vw, 3.5rem);
  column-gap: var(--w-col-gap);
}
.sec-head {
  grid-column: 1 / -1;
  display: grid; grid-template-columns: subgrid;
  align-items: start;
  padding-top: clamp(1.2rem, .8rem + 1.2vw, 2rem);
  border-top: 1px solid rgba(89, 64, 55, .18);
}
.sec-head .label { grid-column: 1; }
.sec-head .h2 { grid-column: 2 / 4; }
.label {
  font-family: var(--w-latin);
  font-size: .6875rem; letter-spacing: .2em; text-transform: uppercase;
  color: rgba(89, 64, 55, .62);
  padding-top: .55em;
}
.label.light { color: rgba(246, 245, 244, .6); }
.h2 {
  font-family: var(--w-display);
  font-size: clamp(2.1rem, 1.4rem + 3vw, 4.1rem);
  font-weight: 500; line-height: 1.16; letter-spacing: -.012em;
  max-width: 20em;
}

/* 关于 */
.about-grid {
  grid-column: 1 / -1;
  display: grid; grid-template-columns: subgrid;
  gap: clamp(2rem, 1rem + 4vw, 5rem) var(--w-col-gap, 2rem);
  align-items: start;
  margin-top: clamp(2.2rem, 1.4rem + 3vw, 4.5rem);
}
.about-copy { grid-column: 2; max-width: 34em; }
.about-fig { grid-column: 3; }
.about-copy .lead {
  font-family: var(--w-display);
  font-size: clamp(1.15rem, 1rem + .55vw, 1.55rem);
  line-height: 1.75;
  margin-bottom: 1.4em;
}
.about-copy > p { font-size: .975rem; line-height: 1.9; color: rgba(89, 64, 55, .78); margin-bottom: 1.2em; }

.index-list { list-style: none; margin: 2.2em 0 0; padding: 0; border-top: 1px solid rgba(89, 64, 55, .16); }
.index-list li {
  display: grid; grid-template-columns: 2.2rem 1fr;
  gap: .9rem; align-items: baseline;
  padding: .95em 0;
  border-bottom: 1px solid rgba(89, 64, 55, .16);
}
.index-list span { font-family: var(--w-latin); font-size: .625rem; letter-spacing: .14em; color: rgba(89, 64, 55, .5); }
.index-list b { display: block; font-size: .975rem; font-weight: 600; margin-bottom: .28em; }
.index-list em { display: block; font-style: normal; font-size: .8125rem; line-height: 1.7; color: rgba(89, 64, 55, .62); }

.about-fig { margin: 0; }
.about-fig img { width: 100%; aspect-ratio: 4 / 5; object-fit: cover; }
.about-fig figcaption {
  margin-top: .8em; font-size: .75rem; letter-spacing: .04em;
  color: rgba(89, 64, 55, .55);
}

/* 流程 */
.flow-grid {
  grid-column: 2 / 4;
  display: grid; grid-template-columns: repeat(3, 1fr);
  gap: clamp(1.2rem, .6rem + 2vw, 3rem);
  margin-top: clamp(2.2rem, 1.4rem + 3vw, 4.5rem);
}
.flow-item { padding-top: 1.4em; border-top: 1px solid rgba(89, 64, 55, .35); }
.flow-item .no {
  display: block;
  font-family: var(--w-display);
  font-size: clamp(2.4rem, 1.8rem + 2.4vw, 3.75rem);
  font-weight: 400; line-height: 1;
  color: rgba(89, 64, 55, .35);
  margin-bottom: .55em;
}
.flow-item h3 {
  font-family: var(--w-display);
  font-size: clamp(1.25rem, 1.1rem + .45vw, 1.5rem);
  font-weight: 600; margin-bottom: .5em;
}
.flow-item p { font-size: .9rem; line-height: 1.85; color: rgba(89, 64, 55, .72); max-width: 26em; }

/* 能力：2×2 细线网格（无卡片无阴影） */
.feat-grid {
  grid-column: 2 / 4;
  display: grid; grid-template-columns: 1fr 1fr;
  margin-top: clamp(2.2rem, 1.4rem + 3vw, 4.5rem);
  border-top: 1px solid rgba(89, 64, 55, .16);
}
.feat {
  padding: clamp(1.6rem, 1rem + 2vw, 3rem) clamp(1rem, .4rem + 2vw, 3rem) clamp(1.6rem, 1rem + 2vw, 3rem) 0;
  border-bottom: 1px solid rgba(89, 64, 55, .16);
}
.feat:nth-child(even) { padding-left: clamp(1.4rem, .8rem + 2vw, 3rem); border-left: 1px solid rgba(89, 64, 55, .16); }
.feat .no {
  font-family: var(--w-latin); font-size: .625rem; letter-spacing: .16em;
  color: rgba(89, 64, 55, .5);
}
.feat h3 {
  font-family: var(--w-display);
  font-size: clamp(1.3rem, 1.1rem + .7vw, 1.75rem);
  font-weight: 600; margin: .5em 0 .55em;
}
.feat p { font-size: .9125rem; line-height: 1.85; color: rgba(89, 64, 55, .72); max-width: 30em; }

/* 数据 */
.stat-grid {
  grid-column: 2 / 4;
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: clamp(1rem, .4rem + 1.6vw, 2rem);
  margin-top: clamp(2.2rem, 1.4rem + 3vw, 4.5rem);
}
.stat { padding-top: 1.1em; border-top: 1px solid rgba(89, 64, 55, .35); }
.stat b {
  display: block;
  font-family: var(--w-display);
  font-size: clamp(2.5rem, 1.6rem + 4vw, 5rem);
  font-weight: 400; line-height: 1; letter-spacing: -.03em;
  font-variant-numeric: tabular-nums;
}
.stat b em { font-style: normal; font-size: .38em; letter-spacing: 0; margin-left: .1em; }
.stat span { display: block; margin-top: .9em; font-size: .8125rem; color: rgba(89, 64, 55, .62); }
.stat-note {
  grid-column: 2 / 4;
  margin-top: 2.2em; font-size: .75rem; line-height: 1.8;
  color: rgba(89, 64, 55, .5); max-width: 60em;
}

/* CTA：深棕整幅 */
.cta {
  background: var(--w-text); color: var(--w-bg);
  padding: clamp(3rem, 2rem + 5vw, 7rem) clamp(1.25rem, .8rem + 1.8vw, 3rem);
}
.cta-in {
  max-width: 90rem; margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, .55fr) minmax(0, 1.2fr) minmax(0, 1.45fr);
  --w-col-gap: clamp(1rem, .4rem + 2.4vw, 3.5rem);
  column-gap: var(--w-col-gap);
}
.cta .label { grid-column: 1; }
.cta h2 {
  grid-column: 2 / 4;
  font-family: var(--w-display);
  font-size: clamp(2.6rem, 1.6rem + 5.2vw, 7rem);
  font-weight: 500; line-height: 1.06; letter-spacing: -.015em;
  margin: 1.2rem 0 0;
}
.cta-sub { grid-column: 2 / 4; margin-top: 1.4em; font-size: clamp(.9375rem, .9rem + .2vw, 1.0625rem); opacity: .72; }
.cta .btns { grid-column: 2 / 4; justify-content: flex-start; }
.text-link {
  display: inline-flex; align-items: center;
  font-size: .9375rem; color: inherit; opacity: .78;
  border-bottom: 1px solid currentColor; padding-bottom: .15em;
  transition: opacity .3s ease;
}
.text-link:hover { opacity: 1; }

/* ---------------- 页脚（弹层内） ---------------- */
.w-foot {
  background: var(--w-text); color: var(--w-bg);
  padding: clamp(2rem, 1.4rem + 2vw, 3rem) clamp(1.25rem, .8rem + 1.8vw, 3rem) clamp(1.2rem, 1rem + .6vw, 1.6rem);
  display: grid; align-content: end;
}
.foot-grid {
  max-width: 90rem; margin: 0 auto; width: 100%;
  display: grid; grid-template-columns: 1.4fr 1fr 1.2fr auto;
  gap: 2rem clamp(1.5rem, .8rem + 3vw, 4rem);
  align-items: end;
  padding-top: 2rem;
  border-top: 1px solid rgba(246, 245, 244, .22);
}
.foot-brand strong { display: block; font-family: var(--w-display); font-size: 1.5rem; font-weight: 600; letter-spacing: .04em; }
.foot-brand span { display: block; margin-top: .35em; font-family: var(--w-latin); font-size: .5625rem; letter-spacing: .22em; opacity: .6; }
.foot-brand p { margin-top: 1.1em; font-size: .8125rem; opacity: .62; line-height: 1.7; }
.foot-col { display: flex; flex-direction: column; gap: .5em; }
.fh { font-family: var(--w-latin); font-size: .5625rem; letter-spacing: .2em; text-transform: uppercase; opacity: .5; margin-bottom: .4em; }
.foot-col a, .foot-col p { font-size: .8125rem; opacity: .78; line-height: 1.7; }
.foot-col a:hover { opacity: 1; text-decoration: underline; text-underline-offset: 3px; }
.foot-top { justify-self: end; text-align: right; }
.foot-top button {
  background: none; border: 0; color: inherit; cursor: pointer;
  font-family: inherit; font-size: .75rem; opacity: .7;
  display: inline-grid; justify-items: end; gap: .7em;
}
.foot-top button:hover { opacity: 1; }
.foot-base {
  max-width: 90rem; margin: 2.5rem auto 0; width: 100%;
  display: flex; justify-content: space-between; gap: 1rem; flex-wrap: wrap;
  font-size: .6875rem; opacity: .45; letter-spacing: .04em;
}

/* ---------------- 响应式 ---------------- */
@media (max-width: 64rem) {
  .w-nav { display: none; }
  .menu-btn { display: grid; place-items: center; }
  .w-head { grid-template-columns: 1fr auto; }
  .sec { display: block; }
  .sec-head { display: block; }
  .sec-head .h2 { margin-top: .9rem; }
  .about-grid { display: block; }
  .about-fig { margin-top: 2rem; }
  .about-fig img { aspect-ratio: 16 / 10; }
  .flow-grid { grid-template-columns: 1fr; gap: 1.6rem; }
  .feat-grid { grid-template-columns: 1fr; }
  .feat:nth-child(even) { padding-left: 0; border-left: 0; }
  .stat-grid { grid-template-columns: 1fr 1fr; gap: 1.6rem; }
  .foot-grid { grid-template-columns: 1fr 1fr; align-items: start; }
  .cta-in { display: block; }
  .foot-top { justify-self: start; text-align: left; }
  .foot-top button { justify-items: start; }
  .ticks { display: none; }
  .info { padding: 0; }
  .info-panel { width: 100%; height: 100svh; }
  .info-bar { gap: .75rem; padding: 0 .9rem; }
  .info-nav { gap: .9rem; overflow-x: auto; scrollbar-width: none; }
  .info-nav::-webkit-scrollbar { display: none; }
  .info-nav button { white-space: nowrap; font-size: .875rem; }
  .info-enter { display: none; }
}
@media (max-width: 40rem) {
  .slide-in {
    background: linear-gradient(180deg, transparent 26%,
      hsl(23 26% 12% / .52) 56%, hsl(23 26% 10% / .84) 100%);
  }
  .lede br { display: none; }
  .giant { font-size: clamp(2.5rem, 2rem + 3vw, 3.4rem); }
  .slide-foot { flex-wrap: wrap; gap: .6rem 1rem; }
  .foot-note { order: 3; margin-left: 0; width: 100%; }
  .hint { margin-left: auto; }
  .about-fig img { aspect-ratio: 4 / 3; }
  .index-list li { grid-template-columns: 2rem 1fr; }
  .stat-grid { grid-template-columns: 1fr 1fr; }
  .foot-grid { grid-template-columns: 1fr; gap: 1.6rem; }
  .foot-base { margin-top: 2rem; }
  .info-brand i { display: none; }
  .info-brand b { font-size: 1.1rem; }
}
@media (prefers-reduced-motion: reduce) {
  .slide, .slide .media img, .giant, .lede { transition: none; }
  .giant, .lede { opacity: 1; transform: none; }
  .vline { animation: none; }
  .info, .info-panel { animation: none; }
}
</style>

<style>
/* 落地页锁滚动：整个首页是固定舞台，不产生页面滚动条（滚到第 4 屏即止） */
html.w-landing,
html.w-landing body {
  height: 100%;
  overflow: hidden;
  overscroll-behavior: none;
}
html.w-landing { background: #594037; }
html.w-landing body { background: #F6F5F4; }
</style>
