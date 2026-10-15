/**
 * v-reveal 滚动显现指令：元素进入视口时淡入上浮。
 * 用法：v-reveal 或 v-reveal="{ delay: 120 }"（transitionDelay 毫秒）
 */
let observer = null

function getObserver() {
  if (!observer) {
    observer = new IntersectionObserver(
      (entries) => {
        for (const e of entries) {
          if (e.isIntersecting) {
            e.target.classList.add('revealed')
            observer.unobserve(e.target)
          }
        }
      },
      { threshold: 0.15 },
    )
  }
  return observer
}

export const reveal = {
  mounted(el, binding) {
    el.classList.add('reveal')
    const delay = binding.value?.delay
    if (delay) el.style.transitionDelay = `${delay}ms`
    getObserver().observe(el)
  },
  unmounted(el) {
    observer?.unobserve(el)
  },
}
