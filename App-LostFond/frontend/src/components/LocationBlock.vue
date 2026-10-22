<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { findPlace } from '../constants/campus'
import { formatLatLng } from '../utils/geo'
import { t } from '../i18n'
import LocationCard from './LocationCard.vue'

/**
 * 详情页「位置」区块（v2.41.1）：左边是 prompt 同款展开卡，右边是坐标/该地点启事数/操作。
 *
 * 数据取舍：
 * - 有经纬度 → 真高德底图 + 图钉；没有 → 卡片走"未标注精确位置"的兜底，不画假地图。
 * - "该地点 N 条启事"与跳转都用 location + type 两个条件，保证按钮数字 = 落地页条数。
 */
const props = defineProps({
  item: { type: Object, default: null },
})

const router = useRouter()
const cardRef = ref(null)
const rootEl = ref(null)

const hasCoord = computed(() => props.item?.latitude != null && props.item?.longitude != null)
const coordText = computed(() => (hasCoord.value ? formatLatLng(props.item.latitude, props.item.longitude, 5) : ''))
const zoneText = computed(() => {
  const p = findPlace(props.item?.location)
  return p ? t(p.zoneKey) : t('loc.zone.unknown')
})

const count = ref(-1)
async function loadCount() {
  const loc = props.item?.location
  count.value = -1
  if (!loc) { count.value = 0; return }
  try {
    const res = await api.listItems({ location: loc, type: props.item?.type, page: 1, size: 1 })
    count.value = res?.total ?? 0
  } catch (e) { count.value = -1 }
}
loadCount()
watch(() => props.item?.id, loadCount)

async function copyLoc() {
  try {
    await navigator.clipboard.writeText(props.item?.location || '')
    ElMessage.success(t('loc.copied'))
  } catch (e) { ElMessage.warning(t('loc.copyFail')) }
}

function goWall() {
  router.push({ path: '/wall', query: { type: props.item.type, location: props.item.location } })
}

/** 供详情页「查看位置」文字链调用：展开卡片并滚到它。 */
async function open() {
  await nextTick()
  cardRef.value?.expand()
  rootEl.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

defineExpose({ open })
</script>

<template>
  <section ref="rootEl" class="lb" v-if="item?.location">
    <LocationCard
      ref="cardRef"
      :location="item.location"
      :lat="item.latitude"
      :lng="item.longitude"
      :category="item.category"
      :expand-label="$t('loc.expand')"
    />

    <div class="lb-side">
      <p class="lb-kicker">{{ $t('loc.sec') }} · {{ zoneText }}</p>
      <p class="lb-coord" :class="{ muted: !hasCoord }">
        {{ hasCoord ? coordText : $t('loc.noCoord') }}
      </p>
      <p v-if="count > 0" class="lb-count">{{ $t('loc.othersCount', { n: count }) }}</p>
      <div class="lb-acts">
        <button class="lb-btn" type="button" @click="copyLoc">{{ $t('loc.copy') }}</button>
        <button class="lb-btn ghost" type="button" :disabled="count === 0" @click="goWall">
          {{ $t('loc.others') }}
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.lb {
  display: flex; align-items: flex-start; gap: 26px;
  margin: 20px 0 4px; padding: 20px;
  background: var(--cream);
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
}
.lb-side { display: flex; flex-direction: column; gap: 7px; min-width: 0; flex: 1; padding-top: 4px; }
.lb-kicker { font-size: 10.5px; font-weight: 700; letter-spacing: .16em; color: var(--ink-faint); margin: 0; }
.lb-coord { font-size: 12.5px; color: var(--ink-soft); margin: 0; font-variant-numeric: tabular-nums; }
.lb-coord.muted { color: var(--ink-faint); }
.lb-count { font-size: 12px; color: var(--ink-soft); margin: 0; }
.lb-acts { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 4px; }
.lb-btn {
  font-family: inherit; font-size: 12px; font-weight: 700;
  padding: 8px 14px; border-radius: 999px; cursor: pointer;
  background: var(--paper); color: var(--ink); border: 1px solid var(--line-2);
  transition: border-color .16s, background-color .16s, transform .16s;
}
.lb-btn:hover { border-color: var(--ink-faint); background: var(--cream-deep); }
.lb-btn.ghost { background: transparent; }
.lb-btn:disabled { opacity: .45; cursor: not-allowed; }

@media (max-width: 720px) {
  .lb { flex-direction: column; gap: 14px; padding: 16px; }
}
</style>
