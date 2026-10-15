<script setup>
import { computed } from 'vue'

/**
 * SplitText 逐字入场动画（复刻 reactbits.dev/text-animations/split-text 思路，纯 CSS 实现）
 * 每个字符从下方带轻微旋转 + 模糊浮入，逐字错峰延迟。
 */
const props = defineProps({
  text: { type: String, required: true },
  stagger: { type: Number, default: 45 },   // 每字符延迟 ms
  delay: { type: Number, default: 0 },      // 起始延迟 ms
})

const chars = computed(() => [...props.text])
</script>

<template>
  <span class="split-line" aria-label="text">
    <span
      v-for="(ch, i) in chars"
      :key="i"
      class="split-char"
      :style="{ '--d': delay + i * stagger }"
      aria-hidden="true"
    >{{ ch }}</span>
  </span>
</template>
