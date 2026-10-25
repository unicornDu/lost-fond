<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { catDot } from '../constants/catalog'
import { formatLatLng, paintTiles } from '../utils/geo'
/**
 * 位置卡（v2.41.1，方案 A）：严格按 `设计prompt/前端/定位显示.md` 的形态做。
 *
 * 240×140 折叠 → 360×280 展开（宽度/高度 + 内容显隐都走 CSS 过渡），
 * 点阵底、LIVE 徽标、坐标行、脉冲图钉、指针 3D 倾斜都在。
 * 与 prompt 的差别只有两处，都是为了"真数据"：
 *   1) 底图是真的高德瓦片（有坐标时），不是手绘 SVG 路网；
 *   2) 没坐标的老启事退回"未标注精确位置"的兜底态，不假装精确。
 */
const props = defineProps({
  location: { type: String, default: '' },
  lat: { type: Number, default: null },
  lng: { type: Number, default: null },
  category: { type: String, default: '' },
  expandLabel: { type: String, default: '点击展开' },
})

const hasCoord = computed(() => props.lat != null && props.lng != null)
const coordText = computed(() => (hasCoord.value ? formatLatLng(props.lat, props.lng, 5) : ''))
const pinColor = computed(() => catDot(props.category))

const expanded = ref(false)
const card = ref(null)
const mapEl = ref(null)
const rx = ref('0deg')
const ry = ref('0deg')
const tiltOk = ref(false)
let paintTimer = null

function paint() {
  if (!expanded.value || !hasCoord.value) return
  paintTiles(mapEl.value, props.lat, props.lng, 17)
}

function schedulePaint() {
  clearTimeout(paintTimer)
  paintTimer = setTimeout(paint, 460)   // 等宽高过渡结束，按最终尺寸铺瓦片
}

function toggle() {
  expanded.value = !expanded.value
  if (expanded.value) schedulePaint()
}

/** 供父组件调用（详情页的「查看位置」文字链）。 */
function expand() {
  if (expanded.value) return
  expanded.value = true
  nextTick(schedulePaint)
}

defineExpose({ expand, toggle })

function onMove(e) {
  if (!tiltOk.value || !card.value) return
  const r = card.value.getBoundingClientRect()
  const dx = (e.clientX - r.left - r.width / 2) / (r.width / 2)
  const dy = (e.clientY - r.top - r.height / 2) / (r.height / 2)
  ry.value = `${(dx * 4).toFixed(2)}deg`
  rx.value = `${(-dy * 3.4).toFixed(2)}deg`
}
function onLeave() { rx.value = '0deg'; ry.value = '0deg' }

onMounted(() => {
  try {
    tiltOk.value = !window.matchMedia('(prefers-reduced-motion: reduce)').matches
      && !window.matchMedia('(pointer: coarse)').matches
  } catch (e) { tiltOk.value = false }
})
onBeforeUnmount(() => clearTimeout(paintTimer))
</script>

<template>
  <div class="lc-stage">
    <div
      ref="card" class="lc" :class="{ open: expanded, 'has-coord': hasCoord, plain: !hasCoord }"
      :style="{ '--pin': pinColor, '--rx': rx, '--ry': ry }"
      role="button" tabindex="0" :aria-expanded="expanded"
      @click="toggle" @keydown.enter.prevent="toggle" @keydown.space.prevent="toggle"
      @pointermove="onMove" @pointerleave="onLeave"
    >
      <div class="lc-grid" aria-hidden="true"></div>

      <div class="lc-top">
        <svg class="lc-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="3 6 9 3 15 6 21 3 21 18 15 21 9 18 3 21" /><line x1="9" x2="9" y1="3" y2="18" /><line x1="15" x2="15" y1="6" y2="21" />
        </svg>
        <span class="lc-live">
          <i></i>{{ hasCoord ? 'LIVE' : $t('loc.guess') }}
        </span>
      </div>

      <!-- 真实底图 / 兜底占位：两者都常驻，靠 class 切透明度。
           瓦片是用 innerHTML 写进去的，结构里不能再用 v-if 增删兄弟节点。 -->
      <div ref="mapEl" class="lc-map" aria-hidden="true"></div>
      <div class="lc-fallback" aria-hidden="true">
        <span class="fb-ring"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 21s-7-5.5-7-11a7 7 0 1 1 14 0c0 5.5-7 11-7 11z" /><circle cx="12" cy="10" r="2.5" /></svg></span>
      </div>

      <span class="lc-pulse" aria-hidden="true"></span>
      <div class="lc-pin" aria-hidden="true">
        <svg width="34" height="44" viewBox="0 0 34 44">
          <path d="M17 43C17 43 32 26.5 32 16.5A15 15 0 1 0 2 16.5C2 26.5 17 43 17 43Z" fill="var(--pin)" />
          <circle cx="17" cy="16" r="5.2" fill="var(--paper)" />
        </svg>
      </div>

      <div class="lc-bottom">
        <h4>{{ location || $t('loc.custom') }}</h4>
        <p class="lc-coord">{{ hasCoord ? coordText : $t('loc.noCoord') }}</p>
        <div class="lc-rule"></div>
      </div>

      <span class="lc-credit">© 高德地图</span>
    </div>
    <p v-if="!expanded" class="lc-hint">{{ expandLabel }}</p>
  </div>
</template>

<style scoped>
.lc-stage { display: flex; flex-direction: column; align-items: flex-start; gap: 8px; perspective: 1200px; flex: none; }

.lc {
  position: relative;
  width: 240px; height: 140px;
  border-radius: 16px;
  border: 1px solid var(--line-2);
  background: var(--paper);
  overflow: hidden;
  cursor: pointer;
  user-select: none;
  transform: perspective(1200px) rotateX(var(--rx, 0deg)) rotateY(var(--ry, 0deg));
  transition: width .52s cubic-bezier(.34, 1.56, .64, 1), height .52s cubic-bezier(.34, 1.56, .64, 1),
              transform .2s ease-out, box-shadow .3s ease;
  box-shadow: 0 1px 2px rgba(20, 20, 18, .05);
}
.lc:hover { box-shadow: 0 14px 34px rgba(20, 20, 18, .10); }
.lc.open { width: 360px; height: 280px; cursor: default; box-shadow: 0 18px 44px rgba(20, 20, 18, .14); }
.lc:focus-visible { outline: 2px solid var(--ink); outline-offset: 3px; }

.lc-grid {
  position: absolute; inset: 0; opacity: .05; pointer-events: none;
  background-image: linear-gradient(var(--ink) 1px, transparent 1px),
                    linear-gradient(90deg, var(--ink) 1px, transparent 1px);
  background-size: 20px 20px;
  transition: opacity .35s ease;
}
.lc.open .lc-grid { opacity: 0; }

.lc-top {
  position: absolute; left: 0; right: 0; top: 0; z-index: 3;
  display: flex; align-items: flex-start; justify-content: space-between;
  padding: 15px 15px 0;
}
.lc-ico { width: 18px; height: 18px; color: var(--c-elec); filter: drop-shadow(0 0 4px rgba(47, 164, 106, .3)); transition: filter .3s ease; }
.lc:hover .lc-ico { filter: drop-shadow(0 0 8px rgba(47, 164, 106, .55)); }
.lc-live {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 9px; border-radius: 999px;
  background: rgba(20, 20, 18, .05);
  font-size: 9.5px; font-weight: 800; letter-spacing: .14em; color: var(--ink-soft);
}
.lc-live i { width: 6px; height: 6px; border-radius: 50%; background: var(--c-elec); box-shadow: 0 0 0 3px rgba(47, 164, 106, .16); }

/* 底图 */
.lc-map {
  position: absolute; inset: 0; z-index: 1;
  background: var(--cream-deep);
  opacity: 0; transition: opacity .45s ease .08s;
}
.lc.open.has-coord .lc-map { opacity: 1; }
.lc-map :deep(img) { position: absolute; width: 256px; height: 256px; display: block; }
.lc-map::after {
  content: ''; position: absolute; inset: 0; pointer-events: none;
  background: linear-gradient(180deg, rgba(20, 20, 18, .26) 0%, transparent 26%, transparent 58%, rgba(20, 20, 18, .46) 100%);
}

/* 兜底 */
.lc-fallback {
  position: absolute; inset: 0; z-index: 1;
  display: grid; place-items: center;
  opacity: 0; transition: opacity .4s ease;
}
.lc.open:not(.has-coord) .lc-fallback { opacity: 1; }
.fb-ring {
  width: 124px; height: 124px; border-radius: 50%;
  border: 1px dashed var(--line-2);
  display: grid; place-items: center;
  color: var(--ink-faint);
}
.fb-ring svg { width: 26px; height: 26px; }

/* 图钉 */
.lc-pin {
  position: absolute; left: 50%; top: 50%; z-index: 2;
  transform: translate(-50%, -100%);
  opacity: 0; transition: opacity .3s ease;
  filter: drop-shadow(0 6px 10px rgba(20, 20, 18, .32));
}
.lc.open.has-coord .lc-pin { opacity: 1; animation: lcDrop .6s cubic-bezier(.34, 1.56, .64, 1) .18s both; }
@keyframes lcDrop {
  from { transform: translate(-50%, -100%) translateY(-16px) scale(.6); }
  to { transform: translate(-50%, -100%); }
}
.lc-pulse {
  position: absolute; left: 50%; top: 50%; z-index: 2;
  width: 11px; height: 11px; border-radius: 50%;
  transform: translate(-50%, -50%);
  background: var(--pin); opacity: 0;
}
.lc.open.has-coord .lc-pulse { opacity: .5; animation: lcPulse 2.6s ease-out infinite; }
@keyframes lcPulse {
  0% { box-shadow: 0 0 0 0 color-mix(in srgb, var(--pin) 45%, transparent); }
  70%, 100% { box-shadow: 0 0 0 26px transparent; }
}

/* 底部信息 */
.lc-bottom {
  position: absolute; left: 0; right: 0; bottom: 0; z-index: 3;
  padding: 0 15px 14px;
}
.lc-bottom h4 {
  margin: 0 0 3px; font-size: 14px; font-weight: 700; letter-spacing: -.01em;
  transition: transform .3s ease;
}
.lc:hover .lc-bottom h4 { transform: translateX(3px); }
.lc-coord { margin: 0 0 7px; font-size: 10px; color: var(--ink-faint); letter-spacing: .02em; }
.lc-rule { height: 1px; width: 30%; background: linear-gradient(90deg, color-mix(in srgb, var(--pin) 70%, transparent), transparent); transition: width .4s ease; }
.lc:hover .lc-rule { width: 100%; }

/* 展开后文字压在地图上 → 转白（兜底态没有底图，保持墨色） */
.lc.open.has-coord .lc-bottom h4 { color: #fff; text-shadow: 0 1px 4px rgba(0, 0, 0, .42); }
.lc.open.has-coord .lc-coord { color: rgba(255, 255, 255, .84); text-shadow: 0 1px 3px rgba(0, 0, 0, .45); }
.lc.open.has-coord .lc-live { background: rgba(16, 16, 14, .42); color: #fff; backdrop-filter: blur(6px); }
.lc.open.has-coord .lc-ico { filter: drop-shadow(0 1px 4px rgba(0, 0, 0, .5)); }

.lc-credit {
  position: absolute; right: 12px; bottom: 11px; z-index: 4;
  font-size: 9px; color: rgba(255, 255, 255, .9);
  text-shadow: 0 1px 3px rgba(0, 0, 0, .55);
  opacity: 0; transition: opacity .3s ease .25s; pointer-events: none;
}
.lc.open.has-coord .lc-credit { opacity: 1; }

.lc-hint { margin: 0; font-size: 10px; color: var(--ink-faint); }

@media (max-width: 560px) {
  /* .lb 是 flex column + align-items:flex-start，.lc-stage 会被收缩成 fit-content，
     里面的 .lc 再 width:100% 只会撑到"文字的宽度"。这里把 stage 显式撑满。 */
  .lc-stage { width: 100%; }
  .lc { width: 100%; }
  .lc.open { width: 100%; height: 300px; }
}

@media (prefers-reduced-motion: reduce) {
  .lc, .lc.open { transition: none; transform: none; }
  .lc.open.has-coord .lc-pin, .lc.open.has-coord .lc-pulse { animation: none; }
}
</style>
