<script setup>
/**
 * 搜索条（v3 Digg 版式）：一条胶囊里装下「关键词 + 地点 + 执行」，
 * 去掉旧版的渐变光晕与旋转贴纸手感——信息流风格里 UI 不抢戏。
 * 用法：<GradientSearchBar v-model:keyword="k" v-model:location="l" @search="fn" />
 */
import { computed, ref } from 'vue'
import { LOCATIONS } from '../constants/catalog'

const keyword = defineModel('keyword', { type: String, default: '' })
const location = defineModel('location', { type: String, default: '' })
const emit = defineEmits(['search', 'focus', 'blur'])

const focused = ref(false)
const hasText = computed(() => keyword.value.trim().length > 0)

function submit() {
  emit('search')
}
</script>

<template>
  <div class="sbar" :class="{ 'is-focus': focused, 'has-text': hasText }">
    <svg class="lead" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></svg>

    <input
      v-model="keyword"
      class="input"
      type="text"
      :placeholder="$t('wall.search.placeholder')"
      :aria-label="$t('wall.search.aria')"
      @keydown.enter.prevent="submit"
      @focus="focused = true; emit('focus')"
      @blur="focused = false; emit('blur')"
    />

    <button v-if="hasText" type="button" class="clear" :aria-label="$t('wall.search.clear')" @click="keyword = ''">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6 6 18M6 6l12 12" /></svg>
    </button>

    <span class="divider" aria-hidden="true"></span>

    <el-select v-model="location" :placeholder="$t('wall.loc.all')" clearable class="loc">
      <el-option v-for="l in LOCATIONS" :key="l" :label="l" :value="l" />
    </el-select>

    <button type="button" class="go" :class="{ hot: hasText || location }" :aria-label="$t('wall.search.go')" @click="submit">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><path d="M5 12h14m-6-7 7 7-7 7" /></svg>
    </button>
  </div>
</template>

<style scoped>
.sbar {
  display: flex; align-items: center; gap: 6px;
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 6px 7px 6px 16px;
  transition: border-color .2s ease, box-shadow .2s ease;
}
.sbar.is-focus { border-color: var(--ink); box-shadow: 0 6px 18px rgba(20, 20, 18, .06); }

.lead { width: 17px; height: 17px; color: var(--ink-faint); flex: none; }

.input {
  flex: 1; min-width: 120px;
  height: 32px; padding: 0 4px;
  border: 0; outline: none; background: transparent;
  font-family: inherit; font-size: 13.5px; color: var(--ink);
}
.input::placeholder { color: var(--ink-faint); }

.clear {
  width: 24px; height: 24px; border: 0; border-radius: 50%;
  background: var(--cream-deep); color: var(--ink-soft);
  display: grid; place-items: center; cursor: pointer; flex: none;
}
.clear svg { width: 12px; height: 12px; }

.divider { width: 1px; height: 18px; background: var(--line); margin: 0 2px; flex: none; }

.loc { width: 108px; flex: none; }
.loc :deep(.el-select__wrapper) {
  min-height: 32px;
  background: transparent;
  box-shadow: none !important;
  padding: 0 6px;
  font-size: 12.5px;
}
.loc :deep(.el-select__wrapper.is-hovering) { background: var(--cream-deep); border-radius: 999px; }

.go {
  width: 34px; height: 34px; flex: none;
  border: 0; border-radius: 50%;
  background: var(--cream-deep); color: var(--ink-faint);
  display: grid; place-items: center; cursor: pointer;
  transition: background .18s ease, color .18s ease, transform .18s ease;
}
.go svg { width: 15px; height: 15px; }
.go.hot { background: var(--ink); color: var(--paper); }
.go:hover { transform: translateX(1px); }
.go:active { transform: scale(.94); }

@media (max-width: 560px) {
  .sbar { padding-left: 12px; }
  .divider, .loc { display: none; }
}
</style>
