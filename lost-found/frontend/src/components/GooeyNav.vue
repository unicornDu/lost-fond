<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

/**
 * GooeyNav · 果冻导航（React Bits 同名组件的 Vue 移植）
 * 点击导航项：橙色药丸滑入目标项并与气泡粒子发生"果冻融合"。
 *
 * 移植说明：原版用 blur+contrast+黑底+mix-blend-mode:lighten 实现融合，只适用于深色背景；
 * 本项目是米白底，改用 SVG alpha 通道滤镜（feGaussianBlur + feColorMatrix 阈值化）
 * 在透明图层上做果冻融合，任何背景色都成立。
 */
const props = defineProps({
  items: { type: Array, required: true },          // [{ label, href }]
  animationTime: { type: Number, default: 600 },
  particleCount: { type: Number, default: 15 },
  particleDistances: { type: Array, default: () => [90, 10] },
  particleR: { type: Number, default: 100 },
  timeVariance: { type: Number, default: 300 },
  colors: { type: Array, default: () => [1, 2, 3, 1, 2, 3, 1, 4] }, // 色号 1-4，映射到 --goo-c1..4
  initialActiveIndex: { type: Number, default: 0 }, // -1 = 初始都不选中
})

const emit = defineEmits(['change'])

const containerRef = ref(null)
const navRef = ref(null)
const gooRef = ref(null)
const pillRef = ref(null)
const activeIndex = ref(props.initialActiveIndex)

let resizeObserver = null
let prefersReducedMotion = false

const noise = (n = 1) => n / 2 - Math.random() * n

const getXY = (distance, pointIndex, totalPoints) => {
  const angle = ((360 + noise(8)) / totalPoints) * pointIndex * (Math.PI / 180)
  return [distance * Math.cos(angle), distance * Math.sin(angle)]
}

/** 把药丸层对齐到某个导航项 */
function positionEffect(el) {
  const containerRect = containerRef.value.getBoundingClientRect()
  const pos = el.getBoundingClientRect()
  const s = gooRef.value.style
  s.left = `${pos.x - containerRect.x}px`
  s.top = `${pos.y - containerRect.y}px`
  s.width = `${pos.width}px`
  s.height = `${pos.height}px`
}

/** 生成一圈气泡粒子（与药丸发生果冻融合后消散） */
function makeParticles(el) {
  if (prefersReducedMotion) return
  const d = props.particleDistances
  const r = props.particleR
  const bubbleTime = props.animationTime * 2 + props.timeVariance
  el.style.setProperty('--time', `${bubbleTime}ms`)

  for (let i = 0; i < props.particleCount; i++) {
    const t = props.animationTime * 2 + noise(props.timeVariance * 2)
    const rot0 = noise(r / 10)
    const start = getXY(d[0], props.particleCount - i, props.particleCount)
    const end = getXY(d[1] + noise(7), props.particleCount - i, props.particleCount)
    const scale = 1 + noise(0.2)
    const color = props.colors[Math.floor(Math.random() * props.colors.length)]
    const rotate = rot0 > 0 ? (rot0 + r / 20) * 10 : (rot0 - r / 20) * 10

    setTimeout(() => {
      if (!alive) return // 审查 2.31②：卸载后不再追加 DOM
      const particle = document.createElement('span')
      const point = document.createElement('span')
      particle.className = 'particle'
      particle.style.setProperty('--start-x', `${start[0]}px`)
      particle.style.setProperty('--start-y', `${start[1]}px`)
      particle.style.setProperty('--end-x', `${end[0]}px`)
      particle.style.setProperty('--end-y', `${end[1]}px`)
      particle.style.setProperty('--time', `${t}ms`)
      particle.style.setProperty('--scale', `${scale}`)
      particle.style.setProperty('--color', `var(--goo-c${color}, var(--orange))`)
      particle.style.setProperty('--rotate', `${rotate}deg`)
      point.className = 'point'
      particle.appendChild(point)
      el.appendChild(particle)
      requestAnimationFrame(() => el.classList.add('active'))
      setTimeout(() => {
        try { el.removeChild(particle) } catch { /* 已被清理 */ }
      }, t)
    }, 30)
  }
}

function handleClick(e, index) {
  if (activeIndex.value === index) return
  activeIndex.value = index

  const li = e.currentTarget.parentElement
  positionEffect(li)

  // 清掉旧粒子，重放药丸入场
  gooRef.value.classList.remove('active')
  gooRef.value.querySelectorAll('.particle').forEach((p) => p.remove())
  const pill = pillRef.value
  pill.classList.remove('show')
  void pill.offsetWidth
  pill.classList.add('show')
  makeParticles(gooRef.value)

  const item = props.items[index]
  if (item?.href?.startsWith('#')) {
    document.querySelector(item.href)?.scrollIntoView({ behavior: 'smooth' })
  }
  emit('change', item, index)
}

function reposition() {
  const li = navRef.value?.querySelectorAll('li')[activeIndex.value]
  if (li && activeIndex.value >= 0) positionEffect(li)
}

let alive = true // 审查 2.31②：组件存活标志，阻止卸载后的定时器操作 DOM

onMounted(() => {
  prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (activeIndex.value >= 0) {
    const li = navRef.value.querySelectorAll('li')[activeIndex.value]
    if (li) {
      positionEffect(li)
      // 首个选中项直接显示药丸，不播放动画
      pillRef.value.style.transition = 'none'
      pillRef.value.classList.add('show')
      void pillRef.value.offsetWidth
      pillRef.value.style.transition = ''
    }
  }
  resizeObserver = new ResizeObserver(reposition)
  resizeObserver.observe(containerRef.value)
})

onBeforeUnmount(() => {
  alive = false
  resizeObserver?.disconnect()
  gooRef.value?.querySelectorAll('.particle').forEach((p) => p.remove())
})
</script>

<template>
  <div ref="containerRef" class="gooey-nav">
    <nav ref="navRef">
      <ul>
        <li v-for="(item, i) in items" :key="item.label" :class="{ active: activeIndex === i }">
          <a :href="item.href" @click.prevent="handleClick($event, i)">{{ item.label }}</a>
        </li>
      </ul>
    </nav>
    <span ref="gooRef" class="goo-layer" aria-hidden="true">
      <span ref="pillRef" class="pill"></span>
    </span>

    <!-- alpha 果冻滤镜：模糊后按透明度阈值化，让药丸与粒子融合 -->
    <svg class="goo-defs" width="0" height="0" aria-hidden="true" focusable="false">
      <defs>
        <filter id="goo-nav-filter">
          <feGaussianBlur in="SourceGraphic" stdDeviation="7" result="blur" />
          <feColorMatrix in="blur" mode="matrix"
            values="1 0 0 0 0  0 1 0 0 0  0 0 1 0 0  0 0 0 19 -8" />
        </filter>
      </defs>
    </svg>
  </div>
</template>

<style scoped>
.gooey-nav {
  /* 暖橙系气泡色板：colors 里的色号 1-4 对应以下四个变量 */
  --goo-c1: var(--orange);
  --goo-c2: var(--honey);
  --goo-c3: var(--orange-deep);
  --goo-c4: #FFB37A;
  position: relative;
}
.gooey-nav nav {
  position: relative;
  z-index: 2;
  transform: translate3d(0, 0, 0.01px); /* 生成堆叠上下文，保证文字在药丸之上 */
}
.gooey-nav ul {
  display: flex;
  gap: 0.5em;
  list-style: none;
  padding: 0;
  margin: 0;
}
.gooey-nav li { position: relative; }
.gooey-nav li a {
  display: inline-block;
  padding: 0.62em 1.15em;
  border-radius: 100vw;
  color: var(--ink-soft);
  font-size: 14.5px;
  transition: color 0.3s ease;
  user-select: none;
}
.gooey-nav li a:hover { color: var(--orange-deep); }
.gooey-nav li.active a { color: #FFF7EC; }
.gooey-nav li.active a:hover { color: #FFF7EC; }

/* 药丸 + 粒子所在层：整体套 alpha 果冻滤镜，透明背景不影响页面 */
.goo-layer {
  position: absolute;
  left: 0; top: 0;
  width: 0; height: 0;
  z-index: 1;
  pointer-events: none;
  filter: url(#goo-nav-filter);
}
.pill {
  position: absolute;
  inset: 0;
  border-radius: 100vw;
  background: linear-gradient(120deg, var(--orange), var(--orange-deep));
  box-shadow: 0 8px 20px rgba(255, 125, 46, .38);
  transform: scale(0);
}
.pill.show {
  transform: scale(1);
  transition: transform .38s cubic-bezier(.34, 1.56, .64, 1);
}

.particle, .point {
  display: block;
  opacity: 0;
  width: 18px;
  height: 18px;
  border-radius: 100%;
  transform-origin: center;
}
.particle {
  --time: 5s;
  position: absolute;
  top: calc(50% - 8px);
  left: calc(50% - 8px);
  animation: goo-particle calc(var(--time)) ease 1 -350ms;
}
.point {
  background: var(--color);
  opacity: 1;
  animation: goo-point calc(var(--time)) ease 1 -350ms;
}
@keyframes goo-particle {
  0% {
    transform: rotate(0deg) translate(var(--start-x), var(--start-y));
    opacity: 1;
    animation-timing-function: cubic-bezier(0.55, 0, 1, 0.45);
  }
  70% {
    transform: rotate(calc(var(--rotate) * 0.5)) translate(calc(var(--end-x) * 1.2), calc(var(--end-y) * 1.2));
    opacity: 1;
    animation-timing-function: ease;
  }
  85% {
    transform: rotate(calc(var(--rotate) * 0.66)) translate(var(--end-x), var(--end-y));
    opacity: 1;
  }
  100% {
    transform: rotate(calc(var(--rotate) * 1.2)) translate(calc(var(--end-x) * 0.5), calc(var(--end-y) * 0.5));
    opacity: 1;
  }
}
@keyframes goo-point {
  0% { transform: scale(0); opacity: 0; animation-timing-function: cubic-bezier(0.55, 0, 1, 0.45); }
  25% { transform: scale(calc(var(--scale) * 0.25)); }
  38% { opacity: 1; }
  65% { transform: scale(var(--scale)); opacity: 1; animation-timing-function: ease; }
  85% { transform: scale(var(--scale)); opacity: 1; }
  100% { transform: scale(0); opacity: 0; }
}

.goo-defs { position: absolute; }

@media (prefers-reduced-motion: reduce) {
  .pill { transition: none; }
  .particle { display: none; }
}
</style>
