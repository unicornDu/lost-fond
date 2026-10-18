<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { catEmoji } from '../constants/catalog'
import { api } from '../api'
import { assetUrl } from '../utils/asset'

/**
 * 物品卡片（G 版式：扁平白卡 + 左上状态方块签 + 悬停上浮/封面缩放）。
 * E-07：右上角收藏星标（未登录点击引导去登录；星标状态由本组件自管，可选用 fav-initial 传入）。
 * similarity：以图搜图结果页显示相似度徽标。
 */
const props = defineProps({
  item: { type: Object, required: true },
  similarity: { type: Number, default: null }, // 以图搜图结果页显示相似度
  favInitial: { type: Boolean, default: false }, // 收藏列表页初始为已收藏
  featured: { type: Boolean, default: false }, // 2026-10-18：特稿卡（跨列布局由父级样式负责）
})

const fav = ref(props.favInitial)
onMounted(() => { fav.value = props.favInitial })

async function toggleFav() {
  if (!localStorage.getItem('shiguang_token')) {
    ElMessage.warning('请先登录后收藏')
    return
  }
  try {
    if (fav.value) {
      await api.unfavorite(props.item.id)
      fav.value = false
      ElMessage.success('已取消收藏')
    } else {
      await api.favorite(props.item.id)
      fav.value = true
      ElMessage.success('已收藏')
    }
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}
</script>

<template>
  <RouterLink :to="`/item/${item.id}`" class="item-card" :class="{ featured }">
    <div class="cover">
      <div class="cover-fallback">{{ catEmoji(item.category) }}</div>
      <!-- 审查 2.17：列表用缩略图，缺失回退原图 -->
      <img :src="assetUrl(item.thumb || item.img)" :alt="item.title" loading="lazy" @error="$event.target.style.display = 'none'" />

      <div class="status-tags">
        <span class="status" :class="item.type">{{ item.type === 'lost' ? '寻物中' : '招领中' }}</span>
        <span v-if="item.status !== '寻找中' && item.status !== '待认领'" class="status done">{{ item.status }}</span>
      </div>
      <span v-if="similarity" class="sim-badge">相似 {{ similarity }}%</span>
      <!-- E-07：收藏星标（阻止冒泡，不触发卡片跳转） -->
      <button class="fav-btn" :class="{ on: fav }" type="button" title="收藏" @click.prevent.stop="toggleFav">
        {{ fav ? '★' : '☆' }}
      </button>
    </div>

    <div class="body">
      <div class="cat-line">{{ catEmoji(item.category) }} {{ item.category }}</div>
      <p class="title">{{ item.title }}</p>
      <p class="meta">
        <span class="m-item">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 21s7-5.1 7-11a7 7 0 1 0-14 0c0 5.9 7 11 7 11z"/><circle cx="12" cy="10" r="2.5"/></svg>
          {{ item.location }}
        </span>
        <span class="m-item">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>
          {{ (item.time || '').slice(5, 16) }}
        </span>
      </p>
      <div class="foot">
        <span class="views">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7z"/><circle cx="12" cy="12" r="3"/></svg>
          {{ item.views ?? 0 }}
        </span>
        <span class="go">查看详情 →</span>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.item-card {
  display: flex; flex-direction: column;
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--shadow-card);
  transition: transform .22s cubic-bezier(.22, 1, .36, 1), box-shadow .25s ease;
  cursor: pointer;
}
.item-card:hover { transform: translateY(-5px); box-shadow: var(--shadow-lift); }

.cover { position: relative; aspect-ratio: 4 / 3; background: var(--cream-deep); overflow: hidden; }
/* 图片加载失败时（img 自隐藏）露出下层的分类 emoji 底 */
.cover-fallback {
  position: absolute; inset: 0; display: grid; place-items: center;
  font-size: 54px;
  background: var(--orange-soft);
}
.cover img {
  position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover;
  transition: transform .5s cubic-bezier(.22, 1, .36, 1);
}
.item-card:hover .cover img { transform: scale(1.06); }

/* 状态方块签（G 版式：左上角、小圆角、实色） */
.status-tags { position: absolute; top: 12px; left: 12px; z-index: 3; display: flex; gap: 7px; }
.status {
  padding: 5px 11px; border-radius: 6px;
  font-size: 11px; font-weight: 800; letter-spacing: 1.5px;
  color: #fff;
  box-shadow: 0 3px 8px rgba(27, 26, 23, .18);
}
.status.lost { background: var(--ink); }
.status.found { background: var(--orange); }
.status.done { background: var(--ink-faint); }

.sim-badge {
  position: absolute; right: 10px; bottom: 10px; z-index: 3;
  padding: 5px 12px; border-radius: 8px;
  background: var(--orange); color: #fff;
  font-size: 12px; font-weight: 800;
  font-variant-numeric: tabular-nums;
  box-shadow: 0 4px 10px rgba(15, 122, 92, .35);
}

/* E-07：收藏星标 */
.fav-btn {
  position: absolute; top: 10px; right: 10px; z-index: 4;
  width: 32px; height: 32px; border-radius: 9px;
  border: 0;
  background: rgba(255, 255, 255, .94); color: var(--ink-faint);
  font-size: 15px; line-height: 1; cursor: pointer;
  box-shadow: 0 2px 6px rgba(27, 26, 23, .14);
  transition: transform .18s cubic-bezier(.22, 1, .36, 1), color .18s, background .18s;
}
.fav-btn:hover { transform: scale(1.12); color: var(--orange-deep); }
.fav-btn.on { color: var(--orange); }

.body { padding: 15px 17px 16px; display: flex; flex-direction: column; gap: 7px; flex: 1; }
.cat-line { font-size: 11.5px; color: var(--ink-faint); letter-spacing: 1px; font-weight: 600; }
.title {
  font-size: 15.5px; font-weight: 700; line-height: 1.45;
  display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden;
}
.meta {
  display: flex; gap: 12px; flex-wrap: wrap;
  color: var(--ink-faint); font-size: 12px;
  font-variant-numeric: tabular-nums;
}
.m-item { display: inline-flex; align-items: center; gap: 4px; }
.m-item svg { width: 12px; height: 12px; flex: none; }
.foot {
  margin-top: auto; padding-top: 10px; display: flex; justify-content: space-between; align-items: center;
  color: var(--ink-faint); font-size: 12px;
}
.views { display: inline-flex; align-items: center; gap: 4px; }
.views svg { width: 13px; height: 13px; }
.go { color: var(--orange); font-weight: 700; opacity: 0; transform: translateX(-6px); transition: all .2s; }
.item-card:hover .go { opacity: 1; transform: translateX(0); }
</style>
