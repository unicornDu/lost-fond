<script setup>
/**
 * GradientSearchBar —— 失物墙/招领墙搜索栏（2026-10-17 重设计）
 * 视觉移植自 Prompt/前端prompt/搜索框.md 的 GradientChatInput（React/shadcn 版）：
 * 大圆角卡片 + 无边框内嵌输入 + 右侧圆形动作按钮 + 有文字/聚焦时"显影"的渐变光晕。
 * 色板适配本项目暖光手账风（薄荷→蜜黄→暖橙→绯红），聊天气泡/音效等聊天专属行为不适用搜索场景，已剥离。
 * 用法：<GradientSearchBar v-model:keyword="k" v-model:location="l" v-model:sort="s" @search="fn" />
 */
import { computed, ref } from 'vue'
import { LOCATIONS } from '../constants/catalog'

const keyword = defineModel('keyword', { type: String, default: '' })
const location = defineModel('location', { type: String, default: '' })
const sort = defineModel('sort', { type: String, default: 'new' })
const emit = defineEmits(['search', 'focus', 'blur'])   // E-07：focus/blur 供搜索历史下拉显隐

const focused = ref(false)
const hasText = computed(() => keyword.value.trim().length > 0)

function submit() {
  emit('search')
}
</script>

<template>
  <div class="gsb" :class="{ 'has-text': hasText }">
    <!-- 渐变光晕层：闲置态极淡，聚焦/有词时"显影"（原组件 reveal glow 的移植） -->
    <div class="gsb-glow" aria-hidden="true"></div>

    <!-- 输入卡片 -->
    <div class="gsb-card">
      <button type="button" class="gsb-round gsb-lead" aria-label="搜索" @click="submit">
        <el-icon :size="19"><Search /></el-icon>
      </button>

      <input
        v-model="keyword"
        class="gsb-input"
        type="text"
        placeholder="搜索标题 / 描述关键词"
        aria-label="搜索标题 / 描述关键词"
        @keydown.enter.prevent="submit"
        @focus="focused = true; emit('focus')"
        @blur="focused = false; emit('blur')"
      />

      <button
        v-if="hasText"
        type="button"
        class="gsb-round gsb-clear"
        aria-label="清空关键词"
        @click="keyword = ''"
      >
        <el-icon :size="14"><Close /></el-icon>
      </button>

      <span class="gsb-divider" aria-hidden="true"></span>

      <el-select v-model="location" placeholder="地点" clearable class="gsb-sel gsb-loc">
        <el-option v-for="l in LOCATIONS" :key="l" :label="l" :value="l" />
      </el-select>
      <el-select v-model="sort" class="gsb-sel gsb-sort">
        <el-option label="最新发布" value="new" />
        <el-option label="最多浏览" value="hot" />
      </el-select>

      <!-- 圆形动作按钮：有词时点亮（原组件 Send 按钮的 secondary→default 形态切换） -->
      <button type="button" class="gsb-round gsb-go" :class="{ hot: hasText }" aria-label="执行搜索" @click="submit">
        <el-icon :size="19"><Promotion /></el-icon>
      </button>
    </div>
  </div>
</template>

<style scoped>
.gsb {
  position: relative;
  margin-bottom: 18px;
}
/* 光晕：横贯色带 + 大半径模糊，藏在卡片后面 */
.gsb-glow {
  position: absolute;
  inset: 4px 14px -8px;
  border-radius: 999px;
  background: linear-gradient(90deg, #3fae83, #ffc93c 38%, #ff7d2e 72%, #ef6f55);
  filter: blur(16px);
  opacity: .16;
  transition: opacity .45s ease;
  pointer-events: none;
}
.gsb:focus-within .gsb-glow { opacity: .45; }
.gsb.has-text .gsb-glow { opacity: .6; }

.gsb-card {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 7px 8px 7px 10px;
  background: var(--paper);
  border: 1.5px solid var(--line);
  border-radius: 26px;
  box-shadow: var(--shadow-card);
  transition: box-shadow .3s ease, border-color .3s ease;
}
.gsb-card:focus-within {
  border-color: #FFD9BD;
  box-shadow: var(--shadow-lift);
}

.gsb-round {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border: none;
  border-radius: 14px;
  cursor: pointer;
  transition: transform .18s ease, background .25s ease, box-shadow .25s ease;
}
.gsb-round:active { transform: scale(.9); }

/* 左侧引导钮：米色圆片，悬浮微微转一下（贴纸手感） */
.gsb-lead {
  background: var(--cream);
  color: var(--ink-soft);
}
.gsb-lead:hover {
  background: var(--orange-soft);
  color: var(--orange-deep);
  transform: rotate(-8deg);
}

/* 无边框内嵌输入 */
.gsb-input {
  flex: 1;
  min-width: 150px;
  height: 40px;
  padding: 0 4px;
  border: none;
  outline: none;
  background: transparent;
  font-family: var(--font-body);
  font-size: 15px;
  color: var(--ink);
}
.gsb-input::placeholder { color: var(--ink-faint); }

.gsb-clear {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--cream-deep);
  color: var(--ink-soft);
}
.gsb-clear:hover { background: var(--orange-soft); color: var(--orange-deep); }

.gsb-divider {
  width: 1.5px;
  height: 22px;
  background: var(--line);
  border-radius: 2px;
}

/* 两个下拉收进卡片里：去掉 Element 自带的框线 */
.gsb-sel { width: 128px; }
.gsb-sel :deep(.el-select__wrapper) {
  min-height: 38px;
  background: transparent;
  box-shadow: none !important;
  padding: 0 8px;
}
.gsb-sel :deep(.el-select__wrapper.is-hovering) { background: var(--cream); border-radius: 12px; }

/* 右侧动作钮：暖橙渐变圆片，有词时点亮上浮 */
.gsb-go {
  background: var(--cream-deep);
  color: var(--ink-faint);
  border-radius: 14px;
}
.gsb-go.hot {
  background: linear-gradient(135deg, var(--orange), var(--honey));
  color: #FFF7EC;
  box-shadow: 0 8px 18px rgba(255, 125, 46, .42);
  transform: scale(1.06) rotate(-6deg);
}
.gsb-go.hot:active { transform: scale(.92) rotate(-6deg); }

@media (max-width: 640px) {
  .gsb-divider, .gsb-lead { display: none; }
  .gsb-sel { width: auto; flex: 1; }
  .gsb-input { min-width: 100%; order: -1; }
}
</style>
