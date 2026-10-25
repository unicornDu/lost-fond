<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CAMPUS_VIEW, formatLatLng, offsetToLngLat, paintTiles, wgs84ToGcj02 } from '../utils/geo'
import { t } from '../i18n'

/**
 * 发布页地图选点（v2.41.1）：点地图落钉，或一键读浏览器定位。
 * - v-model 是 { lat, lng } | null；坐标为 GCJ-02（与高德瓦片同基准）。
 * - 地图始终以当前点为中心，所以图钉永远在正中；点别处等于把该处挪到中心。
 * - 没有坐标时以 CAMPUS_VIEW 开场（常量里改成本校区的中心点）。
 */
const props = defineProps({
  modelValue: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue'])

const ZOOM = 17
const mapEl = ref(null)     // 负责点击与量尺寸
const tilesEl = ref(null)   // 只装瓦片（Vue 不管理它的子节点）
const locating = ref(false)

const center = ref({
  lat: props.modelValue?.lat ?? CAMPUS_VIEW.lat,
  lng: props.modelValue?.lng ?? CAMPUS_VIEW.lng,
})

let rafId = 0
let tries = 0

function paint() {
  cancelAnimationFrame(rafId)
  const n = paintTiles(tilesEl.value, center.value.lat, center.value.lng, ZOOM)
  if (n > 0) { tries = 0; return }
  // 容器还没尺寸（首帧 / 父级还没布局完）：下一帧再试，但必须封顶——
  // 无限制重排会在离开页面后继续空转（paintTiles 拿不到 el 时永远返回 0）。
  if (tries++ < 40) rafId = requestAnimationFrame(paint)
}

function setPoint(lat, lng) {
  center.value = { lat, lng }
  emit('update:modelValue', { lat, lng })
  paint()
}

function onClick(e) {
  const el = mapEl.value
  if (!el) return
  const r = el.getBoundingClientRect()
  const { lat, lng } = offsetToLngLat(e.clientX - r.left, e.clientY - r.top, el, center.value.lat, center.value.lng, ZOOM)
  setPoint(lat, lng)
}

function useCurrent() {
  if (!navigator.geolocation) { ElMessage.warning(t('loc.geoFail')); return }
  locating.value = true
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      // 浏览器给 WGS-84，高德是 GCJ-02，必须转
      const p = wgs84ToGcj02(pos.coords.latitude, pos.coords.longitude)
      setPoint(p.lat, p.lng)
      locating.value = false
      ElMessage.success(t('loc.geoOk'))
    },
    () => {
      locating.value = false
      ElMessage.warning(t('loc.geoFail'))
    },
    { enableHighAccuracy: true, timeout: 8000, maximumAge: 60000 },
  )
}

function clear() {
  emit('update:modelValue', null)
  center.value = { lat: CAMPUS_VIEW.lat, lng: CAMPUS_VIEW.lng }
  paint()
}

/* 外部清空（例如表单重置）时回到默认视野 */
watch(() => props.modelValue, (v) => {
  if (!v) { center.value = { lat: CAMPUS_VIEW.lat, lng: CAMPUS_VIEW.lng }; paint() }
  else if (v.lat !== center.value.lat || v.lng !== center.value.lng) {
    center.value = { lat: v.lat, lng: v.lng }
    paint()
  }
})

let ro = null
onMounted(() => {
  paint()
  if (typeof ResizeObserver !== 'undefined') {
    ro = new ResizeObserver(() => paint())
    if (mapEl.value) ro.observe(mapEl.value)
  }
})
onBeforeUnmount(() => { cancelAnimationFrame(rafId); ro?.disconnect() })
</script>

<template>
  <div class="picker">
    <div ref="mapEl" class="pk-map" :class="{ 'is-set': !!modelValue }" role="button" tabindex="0"
      :aria-label="$t('loc.pickHint')" @click="onClick" @keydown.enter.prevent="useCurrent">
      <!-- 瓦片由 paintTiles 直接写 innerHTML：放在一个「Vue 不管理子节点」的空容器里；
           同理，图钉/提示不用 v-if 增删，只用 class 切透明度，避免结构补丁踩到被 innerHTML 冲掉的锚点。 -->
      <div ref="tilesEl" class="pk-tiles" aria-hidden="true"></div>
      <span class="pk-pin" aria-hidden="true">
        <svg width="30" height="40" viewBox="0 0 34 44">
          <path d="M17 43C17 43 32 26.5 32 16.5A15 15 0 1 0 2 16.5C2 26.5 17 43 17 43Z" fill="var(--ink)" />
          <circle cx="17" cy="16" r="5.2" fill="var(--paper)" />
        </svg>
      </span>
      <span class="pk-credit">© 高德地图</span>
      <span class="pk-tip">{{ $t('loc.pickHint') }}</span>
    </div>

    <div class="pk-bar">
      <span class="pk-coord">{{ modelValue ? formatLatLng(modelValue.lat, modelValue.lng, 5) : $t('loc.noCoord') }}</span>
      <span class="pk-sp"></span>
      <button class="pk-btn" type="button" :disabled="locating" @click.stop="useCurrent">
        {{ locating ? $t('loc.locating') : $t('loc.useCurrent') }}
      </button>
      <button v-if="modelValue" class="pk-btn ghost" type="button" @click.stop="clear">{{ $t('loc.clear') }}</button>
    </div>
  </div>
</template>

<style scoped>
.picker { width: 100%; }
.pk-map {
  position: relative; height: 210px; overflow: hidden;
  border: 1px solid var(--line-2); border-radius: 12px;
  background: var(--cream-deep); cursor: crosshair;
}
.pk-map:focus-visible { outline: 2px solid var(--ink); outline-offset: 2px; }
.pk-tiles { position: absolute; inset: 0; overflow: hidden; }
.pk-tiles :deep(img) { position: absolute; width: 256px; height: 256px; display: block; user-select: none; }
.pk-map::after {
  content: ''; position: absolute; inset: 0; pointer-events: none;
  box-shadow: inset 0 0 0 1px rgba(20, 20, 18, .04);
}
.pk-pin {
  position: absolute; left: 50%; top: 50%; z-index: 2;
  transform: translate(-50%, -100%);
  filter: drop-shadow(0 5px 9px rgba(20, 20, 18, .32));
  pointer-events: none;
  opacity: 0; transition: opacity .2s ease;
}
.pk-map.is-set .pk-pin { opacity: 1; }
.pk-credit {
  position: absolute; right: 8px; bottom: 6px; z-index: 2;
  font-size: 9px; color: rgba(255, 255, 255, .92);
  text-shadow: 0 1px 3px rgba(0, 0, 0, .6); pointer-events: none;
}
.pk-tip {
  position: absolute; left: 50%; top: 50%; z-index: 2;
  transform: translate(-50%, -50%);
  padding: 8px 14px; border-radius: 999px;
  background: color-mix(in srgb, var(--paper) 86%, transparent);
  border: 1px solid var(--line);
  font-size: 12px; font-weight: 600; color: var(--ink-soft);
  pointer-events: none; backdrop-filter: blur(4px);
  opacity: 1; transition: opacity .2s ease;
}
.pk-map.is-set .pk-tip { opacity: 0; }
.pk-bar { display: flex; align-items: center; gap: 8px; margin-top: 9px; flex-wrap: wrap; }
.pk-coord { font-size: 11.5px; color: var(--ink-faint); font-variant-numeric: tabular-nums; }
.pk-sp { flex: 1; }
.pk-btn {
  font-family: inherit; font-size: 11.5px; font-weight: 700;
  padding: 6px 12px; border-radius: 999px; cursor: pointer;
  border: 1px solid var(--line-2); background: var(--paper); color: var(--ink);
  transition: background-color .16s, border-color .16s;
}
.pk-btn:hover { background: var(--cream-deep); border-color: var(--ink-faint); }
.pk-btn.ghost { background: transparent; color: var(--ink-soft); }
.pk-btn:disabled { opacity: .5; cursor: wait; }
</style>
