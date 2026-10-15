<script setup>
import { catEmoji } from '../constants/catalog'

/**
 * 物品卡片：图片 + 贴纸分类标签 + 状态角标 + Spotlight 光晕
 * 光晕效果参考 21st.dev 社区热点组件 Spotlight Card（鼠标跟随径向渐变）
 */
const props = defineProps({
  item: { type: Object, required: true },
  similarity: { type: Number, default: null }, // 以图搜图结果页显示相似度
})

function onMove(e) {
  const el = e.currentTarget
  const r = el.getBoundingClientRect()
  el.style.setProperty('--mx', `${e.clientX - r.left}px`)
  el.style.setProperty('--my', `${e.clientY - r.top}px`)
}
</script>

<template>
  <RouterLink :to="`/item/${item.id}`" class="item-card paper-card" @mousemove="onMove">
    <div class="spot"></div>

    <div class="cover">
      <div class="cover-fallback">{{ catEmoji(item.category) }}</div>
      <!-- 审查 2.17：列表用缩略图，缺失回退原图 -->
      <img :src="item.thumb || item.img" :alt="item.title" loading="lazy" @error="$event.target.style.display = 'none'" />

      <span class="ribbon" :class="item.type">{{ item.type === 'lost' ? '寻物中' : '招领中' }}</span>
      <span v-if="item.status !== '寻找中' && item.status !== '待认领'" class="ribbon done">{{ item.status }}</span>
      <span class="sticker cat">{{ catEmoji(item.category) }} {{ item.category }}</span>
      <span v-if="similarity" class="sim-badge">相似 {{ similarity }}%</span>
    </div>

    <div class="body">
      <p class="title">{{ item.title }}</p>
      <p class="meta">
        <span>📍 {{ item.location }}</span>
        <span>⏰ {{ (item.time || '').slice(5, 16) }}</span>
      </p>
      <div class="foot">
        <span class="views">👀 {{ item.views ?? 0 }}</span>
        <span class="go">查看详情 →</span>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.item-card {
  position: relative; overflow: hidden;
  display: flex; flex-direction: column;
  transition: transform .22s ease, box-shadow .22s ease;
  cursor: pointer;
}
.item-card:hover { transform: translateY(-6px); box-shadow: var(--shadow-lift); }

/* Spotlight：跟随鼠标的暖色光晕 */
.spot {
  position: absolute; inset: 0; z-index: 2; pointer-events: none;
  background: radial-gradient(260px circle at var(--mx, 50%) var(--my, 50%), rgba(255, 158, 74, .18), transparent 65%);
  opacity: 0; transition: opacity .25s;
}
.item-card:hover .spot { opacity: 1; }

.cover { position: relative; aspect-ratio: 4 / 3; background: var(--cream-deep); }
/* 图片加载失败时（img 自隐藏）露出下层的分类 emoji 渐变底 */
.cover-fallback {
  position: absolute; inset: 0; display: grid; place-items: center;
  font-size: 54px;
  background: linear-gradient(135deg, var(--orange-soft), var(--honey-soft));
}
.cover img {
  position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover;
}

.ribbon {
  position: absolute; top: 12px; right: -34px; z-index: 3;
  padding: 5px 40px;
  transform: rotate(38deg);
  font-size: 12px; font-weight: 700; letter-spacing: 1px;
  color: #FFF7EC;
  box-shadow: 0 3px 8px rgba(120, 70, 20, .25);
}
.ribbon.lost { background: linear-gradient(90deg, var(--orange), var(--orange-deep)); }
.ribbon.found { background: linear-gradient(90deg, #4CC092, var(--mint)); }
.ribbon.done { background: linear-gradient(90deg, #A9A08F, #7D7466); top: 40px; }

.sticker.cat { position: absolute; left: 12px; bottom: 12px; z-index: 3; }

.sim-badge {
  position: absolute; right: 10px; bottom: 10px; z-index: 3;
  padding: 4px 10px; border-radius: 999px;
  background: rgba(51, 41, 31, .78); color: #FFE9D6;
  font-size: 12px; font-weight: 700;
  backdrop-filter: blur(4px);
}

.body { padding: 14px 16px 13px; display: flex; flex-direction: column; gap: 9px; flex: 1; }
.title {
  font-size: 15.5px; font-weight: 600; line-height: 1.45;
  display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden;
}
.meta {
  display: flex; gap: 12px; flex-wrap: wrap;
  color: var(--ink-soft); font-size: 12.5px;
}
.foot {
  margin-top: auto; display: flex; justify-content: space-between; align-items: center;
  color: var(--ink-faint); font-size: 12.5px;
}
.go { color: var(--orange); font-weight: 600; opacity: 0; transform: translateX(-6px); transition: all .2s; }
.item-card:hover .go { opacity: 1; transform: translateX(0); }
</style>
