<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { catDot, catEmoji } from '../constants/catalog'
import { catLabel, t } from '../i18n'
import { assetUrl } from '../utils/asset'
import { avatarColor, initialOf, statusOf } from '../utils/display'

/**
 * 物品卡片（Digg 网格卡，用于失物墙 / 收藏 / 相似推荐）：
 * 1.45:1 封面 + 分类圆点 + 14.5px 标题 + 地点 + 底栏头像与状态淡彩签。
 * E-07 收藏星标保留（右上角白色圆钮）；similarity 用于以图搜图结果页。
 * featured 参数保留仅为兼容旧调用（首页已改用 FeedCard，不再有跨列特稿）。
 */
const props = defineProps({
  item: { type: Object, required: true },
  similarity: { type: Number, default: null },
  favInitial: { type: Boolean, default: false },
  featured: { type: Boolean, default: false },
})

const fav = ref(props.favInitial)
onMounted(() => { fav.value = props.favInitial })

const dot = computed(() => catDot(props.item.category))
const emoji = computed(() => catEmoji(props.item.category))
const status = computed(() => statusOf(props.item))
const owner = computed(() => props.item.owner || t('me.student'))

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
  <RouterLink :to="`/item/${item.id}`" class="g-card">
    <div class="g-photo">
      <span class="g-emoji">{{ emoji }}</span>
      <img :src="assetUrl(item.thumb || item.img)" :alt="item.title" loading="lazy" @error="$event.target.style.display = 'none'" />
      <button class="g-fav" :class="{ on: fav }" type="button" :title="fav ? $t('card.faved') : $t('card.fav')" @click.prevent.stop="toggleFav">
        {{ fav ? '★' : '☆' }}
      </button>
      <span v-if="similarity" class="g-sim">{{ $t('card.similar', { n: similarity }) }}</span>
    </div>

    <div class="g-body">
      <div class="cat-row">
        <i class="dot" :style="{ background: dot }"></i>
        <span>{{ catLabel(item.category) }}</span>
      </div>
      <h3 class="g-title">{{ item.title }}</h3>
      <p class="g-where">{{ item.location }}</p>
    </div>

    <div class="g-foot">
      <span class="g-ava" :style="{ background: avatarColor(owner) }">{{ initialOf(owner) }}</span>
      <span class="g-owner">{{ owner }}</span>
      <span class="g-status" :class="status.cls">{{ $t(status.key) }}</span>
    </div>
  </RouterLink>
</template>

<style scoped>
.g-card {
  display: flex; flex-direction: column;
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  overflow: hidden;
  transition: border-color .18s ease, box-shadow .18s ease, transform .18s ease;
}
.g-card:hover {
  border-color: var(--line-2);
  box-shadow: 0 10px 26px rgba(20, 20, 18, .07);
  transform: translateY(-2px);
}

.g-photo {
  position: relative;
  aspect-ratio: 1.45 / 1;
  display: grid; place-items: center;
  overflow: hidden;
  background: var(--cream-deep);
}
.g-emoji { font-size: 44px; }
.g-photo img {
  position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover;
  transition: transform .45s cubic-bezier(.22, 1, .36, 1);
}
.g-card:hover .g-photo img { transform: scale(1.04); }

.g-fav {
  position: absolute; top: 10px; right: 10px; z-index: 2;
  width: 30px; height: 30px; border-radius: 50%;
  border: 0; background: var(--paper);
  color: var(--ink-faint); font-size: 14px; line-height: 1;
  cursor: pointer;
  display: grid; place-items: center;
  transition: transform .18s ease, color .18s ease;
}
.g-fav:hover { transform: scale(1.1); color: var(--ink); }
.g-fav.on { color: var(--ink); }

.g-sim {
  position: absolute; left: 10px; bottom: 10px; z-index: 2;
  padding: 5px 10px; border-radius: 999px;
  background: var(--ink); color: var(--paper);
  font-size: 11px; font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.g-body { padding: 14px 15px 12px; display: flex; flex-direction: column; gap: 6px; }
.cat-row { display: flex; align-items: center; gap: 6px; font-size: 11px; font-weight: 700; color: var(--ink-soft); }
.g-title {
  font-size: 14.5px; font-weight: 750; line-height: 1.45; letter-spacing: -.005em;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  min-height: 2.9em;
}
.g-where { font-size: 12px; color: var(--ink-faint); line-height: 1.5; }

.g-foot {
  display: flex; align-items: center; gap: 7px;
  padding: 10px 15px; margin-top: auto;
  border-top: 1px solid var(--line);
}
.g-ava {
  width: 20px; height: 20px; border-radius: 50%;
  display: grid; place-items: center; color: #fff;
  font-size: 10px; font-weight: 700; flex: none;
}
.g-owner { font-size: 11.5px; color: var(--ink-faint); }
.g-status {
  margin-left: auto;
  font-size: 10.5px; font-weight: 700;
  padding: 4px 9px; border-radius: 999px;
}
</style>
