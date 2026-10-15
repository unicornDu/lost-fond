import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 修复 F-03：首页搜图卡拖入的图片暂存在这里，带到 /search-image 直接开搜。
 * File 对象不能走 router state / localStorage，Pinia 内存暂存是最小改动方案。
 */
export const useTransferStore = defineStore('transfer', () => {
  const pendingFile = ref(null)
  function setFile(f) { pendingFile.value = f }
  function takeFile() {
    const f = pendingFile.value
    pendingFile.value = null
    return f
  }
  return { pendingFile, setFile, takeFile }
})
