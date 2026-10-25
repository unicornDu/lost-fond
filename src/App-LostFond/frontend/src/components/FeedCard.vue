<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { catDot, catEmoji } from '../constants/catalog'
import { catLabel, t } from '../i18n'
import { assetUrl } from '../utils/asset'
import { avatarColor, initialOf, relTime, statusOf } from '../utils/display'

/**
 * 启事卡（Digg 信息流主卡）：
 * 分类圆点 + 状态淡彩签 → 19px 标题 → 两行摘要 → 2.35:1 大图（左下地点 pill）
 * → meta 行（头像/昵称/相对时间 + 收藏/帮转幽灵按钮）。
 * 收藏与帮转沿用旧卡片的真实接口与本地助力态，不虚构计数。
 */
const props = defineProps({
  item: { type: Object, required: true },
  similarity: { type: Number, default: null },
  favInitial: { type: Boolean, default: false },
})

const fav = ref(props.favInitial)
const boosted = ref(false)

const dot = computed(() => catDot(props.item.category))
const emoji = computed(() => catEmoji(props.item.category))
const status = computed(() => statusOf(props.item))
const owner = computed(() => props.item.owner || t('me.student'))
const img = computed(() => assetUrl(props.item.thumb || props.item.img))

const BOOST_KEY = 'shiguang_boosted'
function readBoosted() {
  try { return JSON.parse(localStorage.getItem(BOOST_KEY) || '[]') } catch { return [] }
}
onMounted(() => { boosted.value = readBoosted().includes(props.item.id) })

function boost() {
  if (boosted.value) return
  const set = readBoosted()
  if (!set.includes(props.item.id)) set.push(props.item.id)
  try { localStorage.setItem(BOOST_KEY, JSON.stringify(set.slice(-300))) } catch (e) { /* 隐私模式忽略 */ }
  boosted.value = true
  ElMessage.success(t('card.boostOk'))
}

async function toggleFav() {
  if (!localStorage.getItem('shiguang_token')) {
    ElMessage.warning(t('card.needLogin'))
    return
  }
  try {
    if (fav.value) {
      await api.unfavorite(props.item.id)
      fav.value = false
      ElMessage.success(t('card.unfavOk'))
    } else {
      await api.favorite(props.item.id)
      fav.value = true
      ElMessage.success(t('card.favOk'))
    }
    window.dispatchEvent(new Event('shiguang-fav-changed'))
  } catch (e) { ElMessage.error(e.message || t('card.opFail')) }
}
</script>

<template>
  <article class="fcard">
    <div class="fcat">
      <i class="dot" :style="{ background: dot }"></i>
      <span class="fcat-name">{{ catLabel(item.category) }}</span>
      <span class="fstatus" :class="status.cls">{{ $t(status.key) }}</span>
    </div>

    <RouterLink :to="`/item/${item.id}`" class="ftitle">{{ item.title }}</RouterLink>
    <p v-if="item.desc" class="fdesc">{{ item.desc }}</p>

    <RouterLink :to="`/item/${item.id}`" class="fphoto" :style="{ background: `linear-gradient(135deg, color-mix(in srgb, ${dot} 12%, #F4F4F1), color-mix(in srgb, ${dot} 26%, #E9E9E4))` }">
      <span class="femoji">{{ emoji }}</span>
      <img v-if="img" :src="img" :alt="item.title" loading="lazy" @error="$event.target.style.display = 'none'" />
      <span class="floc">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><path d="M12 21s-7-5.5-7-11a7 7 0 1 1 14 0c0 5.5-7 11-7 11z" /><circle cx="12" cy="10" r="2.5" /></svg>
        {{ item.location }}
      </span>
      <span v-if="similarity" class="fsim">{{ $t('card.similar', { n: similarity }) }}</span>
    </RouterLink>

    <div class="fmeta">
      <span class="ava" :style="{ background: avatarColor(owner) }">{{ initialOf(owner) }}</span>
      <b class="who">{{ owner }}</b>
      <span class="when">{{ relTime(item.time) }}</span>
      <div class="acts">
        <span class="views">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7z" /><circle cx="12" cy="12" r="3" /></svg>
          {{ item.views ?? 0 }}
        </span>
        <button class="act" :class="{ on: fav }" type="button" @click="toggleFav">
          <svg viewBox="0 0 24 24" :fill="fav ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2"><path d="M19 14c1.5-1.5 3-3.2 3-5.5A5.5 5.5 0 0 0 12 5 5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4 3 5.5l7 7z" /></svg>
          {{ fav ? $t('card.faved') : $t('card.fav') }}
        </button>
        <button class="act" :class="{ on: boosted }" type="button" @click="boost">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m22 2-7 20-4-9-9-4z" /><path d="M22 2 11 13" /></svg>
          {{ boosted ? $t('card.boosted') : $t('card.boost') }}
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.fcard {
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 20px 22px 14px;
  margin-bottom: 26px;
  transition: border-color .18s ease, box-shadow .18s ease;
}
.fcard:hover { border-color: var(--line-2); box-shadow: 0 10px 30px rgba(20, 20, 18, .05); }

.fcat { display: flex; align-items: center; gap: 7px; margin-bottom: 10px; }
.fcat-name { font-size: 12px; font-weight: 700; color: var(--ink-soft); }
.fstatus {
  margin-left: auto;
  font-size: 11px; font-weight: 700;
  padding: 5px 10px; border-radius: 999px;
}

.ftitle {
  display: block;
  font-family: var(--font-display);
  font-size: 19px; font-weight: 800; line-height: 1.42;
  letter-spacing: -.01em;
  color: var(--ink);
  margin-bottom: 8px;
}
.ftitle:hover { text-decoration: underline; text-underline-offset: 3px; text-decoration-thickness: 2px; }

.fdesc {
  font-size: 13.5px; line-height: 1.65; color: var(--ink-soft);
  margin-bottom: 16px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}

.fphoto {
  position: relative; display: grid; place-items: center;
  aspect-ratio: 2.35 / 1;
  border-radius: var(--radius-sm);
  overflow: hidden;
  margin-bottom: 14px;
}
.fphoto img {
  position: absolute; inset: 0;
  width: 100%; height: 100%; object-fit: cover;
  transition: transform .5s cubic-bezier(.22, 1, .36, 1);
}
.fphoto:hover img { transform: scale(1.03); }
.femoji { font-size: 54px; filter: drop-shadow(0 10px 18px rgba(0, 0, 0, .14)); }
.floc {
  position: absolute; left: 12px; bottom: 12px; z-index: 1;
  display: inline-flex; align-items: center; gap: 5px;
  padding: 7px 11px; border-radius: 999px;
  background: rgba(16, 16, 14, .55);
  backdrop-filter: blur(6px);
  color: #fff; font-size: 11.5px; font-weight: 600;
}
.floc svg { width: 11px; height: 11px; }
.fsim {
  position: absolute; right: 12px; bottom: 12px; z-index: 1;
  padding: 6px 11px; border-radius: 999px;
  background: var(--ink); color: var(--paper);
  font-size: 11.5px; font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.fmeta {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  padding-top: 13px; border-top: 1px solid var(--line);
}
.ava {
  width: 24px; height: 24px; border-radius: 50%; flex: none;
  display: grid; place-items: center; color: #fff;
  font-size: 11px; font-weight: 700;
}
.who { font-size: 12.5px; font-weight: 600; }
.when { font-size: 12px; color: var(--ink-faint); }
.acts { margin-left: auto; display: flex; align-items: center; gap: 2px; }
.views {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 11.5px; color: var(--ink-faint);
  padding-right: 6px;
  font-variant-numeric: tabular-nums;
}
.views svg { width: 13px; height: 13px; }
.act {
  display: inline-flex; align-items: center; gap: 5px;
  border: 0; background: transparent; cursor: pointer;
  font-family: inherit; font-size: 12px; font-weight: 600;
  color: var(--ink-faint);
  padding: 6px 9px; border-radius: 999px;
  transition: background .16s, color .16s;
}
.act:hover { background: var(--cream-deep); color: var(--ink); }
.act.on { color: var(--ink); }
.act svg { width: 14px; height: 14px; }

@media (max-width: 560px) {
  .fcard { padding: 16px 16px 12px; }
  .ftitle { font-size: 17px; }
  .fphoto { aspect-ratio: 1.7 / 1; }
}
</style>
