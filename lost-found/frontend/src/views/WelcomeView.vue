<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import SplitText from '../components/SplitText.vue'
import GooeyNav from '../components/GooeyNav.vue'
import { api } from '../api'
import { assetUrl } from '../utils/asset'

/**
 * 简介落地页（复刻 21st.dev hero-01：导航 + Hero + 社交证明 + 品牌跑马灯）
 * 未登录访客入口；注册/登录跳转 /auth
 * 导航链接使用 GooeyNav（React Bits 果冻导航的 Vue 移植版）
 * 审查 2.30：社交证明数字接真实 /api/stats，不再写死假数据
 */
const router = useRouter()
const stats = ref({ reunited: 0, lostCount: 0, foundCount: 0 })
onMounted(async () => {
  try { stats.value = await api.stats() } catch (e) { /* 落地页统计失败不阻断 */ }
})

const navItems = [
  { label: '功能特性', href: '#features' },
  { label: '使用流程', href: '#flow' },
  { label: '加入我们', href: '#join' },
]

const toAuth = (tab) => router.push({ path: '/auth', query: { tab } })

const scrollTo = (sel) => document.querySelector(sel)?.scrollIntoView({ behavior: 'smooth' })

const brands = [
  { emoji: '💻', name: '计算机学院' },
  { emoji: '⚙️', name: '机械工程学院' },
  { emoji: '🌍', name: '外国语学院' },
  { emoji: '🎓', name: '校学生会' },
  { emoji: '📚', name: '校图书馆' },
  { emoji: '🤝', name: '青年志愿者协会' },
  { emoji: '🏺', name: '校团委' },
]

const features = [
  { emoji: '📷', title: '以图搜图', desc: '基于 pHash 感知哈希算法，上传一张照片就能匹配全校"长得一样"的启事——不用绞尽脑汁想关键词。', tag: '核心亮点' },
  { emoji: '🔄', title: '全流程跟踪', desc: '发布 → 智能匹配 → 认领审核 → 线下交接，每一步状态清晰可见，再也不用翻几十条聊天记录。', tag: '' },
  { emoji: '🛡️', title: '审核与凭证', desc: '校园学号注册，启事经管理员审核后才上墙；认领要填只有失主知道的凭证并由发布者确认，全程站内消息留痕。', tag: '' },
]

const steps = [
  { no: '01', title: '发布 / 上传照片', desc: '丢了东西或捡到东西，拍照发布，30 秒生成一条启事' },
  { no: '02', title: '系统自动匹配', desc: '感知哈希比对 + 关键词检索，相似的失物与招领自动关联' },
  { no: '03', title: '审核交接找回', desc: '凭"只有失主知道"的特征申请认领，线下交接完成闭环' },
]

</script>

<template>
  <div class="welcome">
    <!-- ① 导航头（落地页专属） -->
    <header class="w-nav">
      <div class="container w-nav-inner">
        <RouterLink to="/" class="brand">
          <span class="brand-logo">拾</span>
          <span class="brand-name">拾光<small>校园失物招领平台</small></span>
        </RouterLink>
        <GooeyNav class="w-goo" :items="navItems" :initial-active-index="-1" />
        <div class="w-actions">
          <el-button round plain @click="toAuth('login')">登录</el-button>
          <el-button type="primary" round @click="toAuth('register')">免费注册</el-button>
        </div>
      </div>
    </header>

    <!-- ② Hero：左文案 + 右拍立得照片墙 -->
    <section class="hero container">
      <div class="hero-left">
        <span class="w-badge fade-up" style="--i:1">🧡 软件工程课程设计 · LOST &amp; FOUND</span>
        <h1 class="hero-h1">
          <SplitText text="让每一件失物" :stagger="55" /><br />
          <SplitText text="都能回家" :stagger="65" :delay="350" class="hero-accent" />
        </h1>
        <p class="hero-sub fade-up" style="--i:3">
          拾光把「以图搜图」带进校园失物招领：拍一张照片，感知哈希算法帮你在全校启事里找到那件
          "长得一模一样"的东西——找回失物，像发朋友圈一样简单。
        </p>
        <div class="hero-cta fade-up" style="--i:4">
          <el-button type="primary" size="large" round @click="toAuth('register')">免费注册</el-button>
          <el-button size="large" round plain @click="scrollTo('#features')">看看它怎么工作 ↓</el-button>
        </div>
        <div class="social fade-up" style="--i:5">
          <div class="avatars">
            <span>🦊</span><span>🐰</span><span>🐻</span><span>🐱</span><span>🦉</span>
          </div>
          <div class="social-txt">
            <!-- 修复 F-08：删除编造的五星好评，只保留 /api/stats 真实指标 -->
            <b>{{ stats.reunited }}</b> 件失物已找到主人
            <small>在找 {{ stats.lostCount + stats.foundCount }} 件 · 学号注册 + 凭证认领 + 站内通知</small>
          </div>
        </div>
      </div>

      <div class="hero-right fade-up" style="--i:2">
        <!-- 修复 F-07：外网 picsum 图换成本项目自有演示素材（:src 运行时绑定，构建期不解析；/uploads 由 vite 代理/生产 Nginx alias 提供，内网不破图） -->
        <div class="polaroid p1"><img :src="assetUrl('/uploads/seed/cup-1.jpg')" alt="" /><i>_lost & found_</i></div>
        <div class="polaroid p2"><img :src="assetUrl('/uploads/seed/scarf-1.jpg')" alt="" /><i>reunited ♥</i></div>
        <div class="polaroid p3"><img :src="assetUrl('/uploads/seed/earbuds-1.jpg')" alt="" /><i>thank you!</i></div>
        <div class="stat-card paper-card">
          <b>64 位</b>
          <span>pHash 感知指纹</span>
        </div>
        <span class="float-emoji" style="top:-24px;right:8%;font-size:38px;--rot:10deg">🎒</span>
        <span class="float-emoji" style="bottom:-14px;left:-20px;font-size:32px;--rot:-8deg;animation-delay:.9s">🔑</span>
      </div>
    </section>

    <!-- ③ 品牌跑马灯（BrandSlider） -->
    <section class="brands" id="join">
      <p class="brands-label">面向全校院系与组织（演示数据，非合作背书）</p>
      <div class="marquee">
        <div class="marquee-track">
          <span v-for="(b, i) in [...brands, ...brands]" :key="i" class="brand-chip paper-card">
            {{ b.emoji }} {{ b.name }}
          </span>
        </div>
      </div>
    </section>

    <!-- ④ 功能特性 -->
    <section id="features" class="container sec">
      <h2 v-reveal class="sec-title">为什么是<em>拾光</em>？</h2>
      <p v-reveal="{ delay: 80 }" class="sec-sub">三个理由，让失物招领不再靠缘分</p>
      <div class="feat-grid">
        <div v-for="(f, i) in features" :key="f.title" v-reveal="{ delay: i * 110 }" class="feat paper-card">
          <span class="feat-emoji">{{ f.emoji }}</span>
          <h3>{{ f.title }}</h3>
          <p>{{ f.desc }}</p>
          <span v-if="f.tag" class="sticker">✨ {{ f.tag }}</span>
        </div>
      </div>
    </section>

    <!-- ⑤ 使用流程 -->
    <section id="flow" class="container sec">
      <h2 v-reveal class="sec-title">三步，<em>找回</em>你的东西</h2>
      <div class="flow-grid">
        <div v-for="(s, i) in steps" :key="s.no" v-reveal="{ delay: i * 130 }" class="flow-step">
          <span class="flow-no display">{{ s.no }}</span>
          <h3>{{ s.title }}</h3>
          <p>{{ s.desc }}</p>
        </div>
      </div>
    </section>

    <!-- ⑥ 底部注册引导 -->
    <section class="container">
      <div v-reveal class="cta-band">
        <h2>现在注册，让丢掉的东西回家</h2>
        <p>注册只需 30 秒，全校的失物与招领启事都在等你</p>
        <el-button size="large" round @click="toAuth('register')" class="cta-btn">免费注册 →</el-button>
      </div>
    </section>

    <footer class="w-footer">
      <span class="foot-logo">拾光</span>
      <p>校园失物招领平台 · 软件工程课程设计小组项目</p>
      <small>© 2026 拾光 LOST &amp; FOUND</small>
    </footer>
  </div>
</template>

<style scoped>
.welcome { overflow-x: hidden; }

/* ---- 导航 ---- */
.w-nav {
  position: sticky; top: 0; z-index: 100;
  /* F-14：Android 15 强制 edge-to-edge 后，导航会压在状态栏（时间/电量）下面，
     这里让背景延伸进状态栏、内容整体下移一个安全区高度 */
  padding-top: var(--safe-top);
  background: color-mix(in srgb, var(--paper) 78%, transparent);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--line);
}
.w-nav-inner { display: flex; align-items: center; gap: 30px; height: 68px; }
.brand { display: flex; align-items: center; gap: 10px; }
.brand-logo {
  display: grid; place-items: center;
  width: 42px; height: 42px; border-radius: 13px;
  background: linear-gradient(135deg, var(--orange), var(--orange-deep));
  color: #FFF7EC; font-family: var(--font-display); font-size: 22px;
  transform: rotate(-4deg);
  box-shadow: 0 6px 16px rgba(255, 125, 46, .4);
}
.brand-name {
  font-family: var(--font-display); font-size: 23px; line-height: 1;
  display: flex; flex-direction: column; gap: 2px;
}
.brand-name small {
  font-family: var(--font-body); font-size: 10px; font-weight: 400;
  color: var(--ink-faint); letter-spacing: 2px;
}
.w-goo { margin-left: auto; }
.w-actions { display: flex; gap: 10px; }

/* ---- Hero ---- */
.hero {
  display: grid; grid-template-columns: 1.1fr .9fr; gap: 40px; align-items: center;
  padding: 56px 0 30px; min-height: 520px;
}
.w-badge {
  display: inline-block; margin-bottom: 22px;
  padding: 6px 16px; border-radius: 999px;
  background: var(--paper); border: 1.5px solid var(--line);
  color: var(--ink-soft); font-size: 13px; letter-spacing: 1px;
  box-shadow: var(--shadow-card);
}
.hero-h1 { font-size: clamp(40px, 5vw, 62px); line-height: 1.25; }
.hero-accent :deep(.split-char) {
  background: linear-gradient(100deg, var(--orange), var(--orange-deep) 55%, #E88A1F);
  -webkit-background-clip: text; background-clip: text; color: transparent;
}
.hero-sub {
  margin: 22px 0 28px; max-width: 500px;
  color: var(--ink-soft); font-size: 15.5px; line-height: 1.95;
}
.hero-cta { display: flex; gap: 14px; margin-bottom: 32px; }
.social { display: flex; align-items: center; gap: 14px; }
.avatars { display: flex; }
.avatars span {
  display: grid; place-items: center;
  width: 42px; height: 42px; border-radius: 50%;
  background: var(--paper); border: 2px solid var(--cream);
  font-size: 20px; margin-left: -10px;
  box-shadow: var(--shadow-card);
}
.avatars span:first-child { margin-left: 0; }
.social-txt { font-size: 13.5px; color: var(--ink-soft); line-height: 1.55; }
.social-txt b { color: var(--ink); }
.stars { color: var(--orange); letter-spacing: 2px; }
.social-txt small { display: block; color: var(--ink-faint); }

/* 拍立得照片墙 */
.hero-right { position: relative; min-height: 440px; }
.polaroid {
  position: absolute;
  background: #FFFDF8; padding: 12px 12px 34px;
  border-radius: 8px;
  box-shadow: 0 16px 38px rgba(160, 100, 40, .25);
}
.polaroid img {
  width: 100%; display: block; border-radius: 4px; object-fit: cover;
  aspect-ratio: 4 / 3; background: var(--cream-deep);
}
.polaroid i {
  position: absolute; left: 0; right: 0; bottom: 8px;
  text-align: center; font-size: 12px; color: var(--ink-faint); font-style: italic;
}
.polaroid::before {
  content: ''; position: absolute; top: -10px; left: 50%;
  width: 74px; height: 22px; transform: translateX(-50%) rotate(-3deg);
  background: rgba(255, 214, 120, .65); border: 1px solid rgba(200, 150, 40, .2);
  border-radius: 2px;
}
.p1 { width: 46%; left: 0; top: 30px; transform: rotate(-7deg); z-index: 1; }
.p2 { width: 48%; right: 4%; top: 0; transform: rotate(4deg); z-index: 2; }
.p3 { width: 44%; left: 26%; bottom: 10px; transform: rotate(-2deg); z-index: 3; }
.polaroid { transition: transform .3s cubic-bezier(.34, 1.56, .64, 1); }
.polaroid:hover { transform: rotate(0) scale(1.05); z-index: 5; }
.stat-card {
  position: absolute; right: -6px; bottom: 44px; z-index: 4;
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  padding: 14px 20px; transform: rotate(3deg);
}
.stat-card b { font-family: var(--font-display); font-size: 26px; color: var(--orange-deep); }
.stat-card span { font-size: 11.5px; color: var(--ink-soft); }

/* ---- 跑马灯 ---- */
.brands { padding: 44px 0 8px; }
.brands-label {
  text-align: center; color: var(--ink-faint); font-size: 13px;
  letter-spacing: 3px; margin-bottom: 18px;
}
.marquee { overflow: hidden; position: relative; }
.marquee::before, .marquee::after {
  content: ''; position: absolute; top: 0; bottom: 0; width: 90px; z-index: 2;
  pointer-events: none;
}
.marquee::before { left: 0; background: linear-gradient(90deg, var(--cream), transparent); }
.marquee::after { right: 0; background: linear-gradient(-90deg, var(--cream), transparent); }
.marquee-track {
  display: flex; gap: 16px; width: max-content;
  animation: marquee 26s linear infinite;
}
.marquee:hover .marquee-track { animation-play-state: paused; }
.brand-chip {
  padding: 12px 26px; border-radius: 999px; white-space: nowrap;
  color: var(--ink-soft); font-size: 14.5px; font-weight: 600;
}
@keyframes marquee { to { transform: translateX(-50%); } }

/* ---- 区块通用 ---- */
.sec { padding: 64px 0 8px; }
.sec-title { font-size: 34px; text-align: center; }
.sec-title em { font-style: normal; color: var(--orange); }
.sec-sub { text-align: center; color: var(--ink-soft); margin: 10px 0 34px; }

.feat-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 22px; }
.feat {
  position: relative; padding: 30px 26px;
  display: flex; flex-direction: column; gap: 12px;
  transition: transform .22s, box-shadow .22s;
}
.feat:hover { transform: translateY(-6px); box-shadow: var(--shadow-lift); }
.feat-emoji { font-size: 40px; }
.feat h3 { font-size: 20px; }
.feat p { color: var(--ink-soft); font-size: 13.8px; line-height: 1.85; }
.feat .sticker { position: absolute; top: -12px; right: 16px; }

.flow-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 22px; }
.flow-step {
  position: relative; padding: 26px;
  border-radius: var(--radius);
  border: 1.5px dashed var(--line); background: transparent;
}
.flow-no {
  font-size: 46px; color: var(--orange);
  opacity: .9; display: block; margin-bottom: 8px;
}
.flow-step h3 { font-size: 18px; margin-bottom: 8px; }
.flow-step p { color: var(--ink-soft); font-size: 13.5px; line-height: 1.8; }

/* ---- CTA ---- */
.cta-band {
  margin-top: 60px; padding: 52px 40px;
  border-radius: 28px; text-align: center;
  background:
    radial-gradient(500px 200px at 20% 0%, rgba(255, 255, 255, .25), transparent 60%),
    linear-gradient(120deg, var(--orange), var(--orange-deep));
  color: #FFF7EC;
  box-shadow: 0 24px 54px rgba(234, 91, 12, .35);
}
.cta-band h2 { font-size: 32px; }
.cta-band p { margin: 10px 0 24px; opacity: .92; font-size: 14.5px; }
.cta-btn { background: #FFF7EC !important; border-color: #FFF7EC !important; color: var(--orange-deep) !important; font-weight: 700; }

/* ---- 页脚 ---- */
.w-footer {
  margin-top: 64px; padding: 30px 0 36px;
  border-top: 1px dashed var(--line);
  display: flex; flex-direction: column; align-items: center; gap: 5px;
  color: var(--ink-soft); font-size: 13px; text-align: center;
}
.foot-logo { font-family: var(--font-display); font-size: 22px; color: var(--orange); }
.w-footer small { color: var(--ink-faint); }

/* ---- 入场动画 ---- */
.fade-up { opacity: 0; animation: fadeUp .7s cubic-bezier(.22, 1, .36, 1) both; animation-delay: calc(var(--i) * 110ms); }
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(22px); filter: blur(4px); }
  to { opacity: 1; transform: translateY(0); filter: blur(0); }
}

/* F-14：断点从 900 提到 1024——平板/折叠屏（约 980~1080 CSS px）此前仍套桌面双栏布局，
   看起来"移动端没适配"；1024 以内一律单栏 + 收起中部导航 */
@media (max-width: 1024px) {
  .hero { grid-template-columns: 1fr; padding-top: 36px; min-height: 0; }
  .hero-right { min-height: 400px; width: min(560px, 100%); margin: 0 auto; }
  .w-goo { display: none; }
  .feat-grid, .flow-grid { grid-template-columns: 1fr; }
  .w-nav-inner { justify-content: space-between; gap: 12px; }
}
/* 触屏平板横屏（CSS 宽度可能到 1200+，但仍是"手机/平板"）：单栏而不是桌面双栏。
   用 pointer:coarse 区分——同宽度的笔记本窗口仍保留双栏。 */
@media (pointer: coarse) and (max-width: 1366px) {
  .hero { grid-template-columns: 1fr; padding-top: 36px; min-height: 0; }
  .hero-right { min-height: 420px; width: min(560px, 100%); margin: 0 auto; }
}
@media (max-width: 560px) {
  .w-nav-inner { height: 60px; }
  .brand-name small { display: none; }
  .hero { padding: 26px 0 18px; }
  .hero-h1 { font-size: clamp(30px, 9vw, 40px); }
  .w-actions :deep(.el-button) { padding-left: 14px; padding-right: 14px; }
}
@media (prefers-reduced-motion: reduce) {
  .fade-up { animation: none; opacity: 1; }
  .marquee-track { animation: none; }
}
</style>
