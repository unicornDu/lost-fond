<script setup>
import { t } from '../i18n'
import { computed, watch, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { assetUrl } from '../utils/asset'
import { catEmoji } from '../constants/catalog'
import { saveDataUrl } from '../utils/saveFile'

/**
 * 分享海报弹层（对齐「整合版 v1」）：
 *  - 屏上：飞入式弹层，含启事图、标题、地点时间、品牌区与 21×21 伪二维码；
 *  - 导出：canvas 按同一版式重绘并下载 PNG（不引第三方库，图片同源不受污染）。
 * 遮罩点击 / Esc 关闭由父级或本组件统一处理。
 */
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  item: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue'])

function close() { emit('update:modelValue', false) }

/* 21×21 伪二维码（三个定位角 + 伪随机数据点），与演示稿同一套图案 */
const qrCells = computed(() => {
  const out = []
  for (let y = 0; y < 21; y++) {
    for (let x = 0; x < 21; x++) {
      const finder = (x < 7 && y < 7) || (x > 13 && y < 7) || (x < 7 && y > 13)
      let d
      if (finder) {
        const fx = x > 13 ? x - 14 : x
        const fy = y > 13 ? y - 14 : y
        d = fx === 0 || fx === 6 || fy === 0 || fy === 6 || (fx >= 2 && fx <= 4 && fy >= 2 && fy <= 4)
      } else {
        d = ((x * 7 + y * 13 + x * y) % 5) < 2
      }
      out.push(d)
    }
  }
  return out
})

const cover = computed(() => (props.item ? assetUrl(props.item.thumb || props.item.img) : ''))
const typeText = computed(() => (props.item?.type === 'lost' ? t('poster.lost') : t('poster.found')))

function drawQr(g, x0, y0, size, cell) {
  for (let y = 0; y < 21; y++) {
    for (let x = 0; x < 21; x++) {
      if (!qrCells.value[y * 21 + x]) continue
      g.fillRect(x0 + x * cell, y0 + y * cell, cell, cell)
    }
  }
}

function wrapText(g, text, x, y, maxW, lh) {
  let line = ''
  let yy = y
  for (const ch of String(text || '')) {
    if (g.measureText(line + ch).width > maxW && line) {
      g.fillText(line, x, yy); yy += lh; line = ch
    } else {
      line += ch
    }
  }
  if (line) { g.fillText(line, x, yy); yy += lh }
  return yy
}

function loadImg(src) {
  return new Promise((resolve, reject) => {
    if (!src) { reject(new Error('no src')); return }
    const im = new Image()
    // Android 端图片来自另一台主机；不声明 CORS 会污染 canvas，toDataURL 直接失败
    im.crossOrigin = 'anonymous'
    im.onload = () => resolve(im)
    im.onerror = () => reject(new Error('load fail'))
    im.src = src
  })
}

async function save() {
  const it = props.item
  if (!it) return
  const W = 640
  const H = 940
  const c = document.createElement('canvas')
  c.width = W
  c.height = H
  const g = c.getContext('2d')

  g.fillStyle = '#FAFAF8'
  g.fillRect(0, 0, W, H)

  // 封面（等比裁切填满）
  const phH = 420
  g.fillStyle = '#F4F4F1'
  g.fillRect(0, 0, W, phH)
  try {
    const im = await loadImg(cover.value)
    const r = Math.max(W / im.width, phH / im.height)
    const iw = im.width * r
    const ih = im.height * r
    g.save()
    g.beginPath(); g.rect(0, 0, W, phH); g.clip()
    g.drawImage(im, (W - iw) / 2, (phH - ih) / 2, iw, ih)
    g.restore()
  } catch (e) {
    g.fillStyle = '#161614'
    g.font = '120px system-ui, sans-serif'
    g.textAlign = 'center'
    g.fillText(catEmoji(it.category || ''), W / 2, phH / 2 + 40)
    g.textAlign = 'left'
  }

  // 状态签
  g.fillStyle = it.type === 'lost' ? '#141412' : '#161614'
  g.fillRect(28, 28, 88, 34)
  g.fillStyle = '#FFFFFF'
  g.font = '700 16px system-ui, sans-serif'
  g.fillText(typeText.value, 44, 51)

  // 标题
  g.fillStyle = '#141412'
  g.font = '800 30px system-ui, sans-serif'
  let y = wrapText(g, it.title, 40, phH + 66, W - 80, 42)

  // 元信息
  g.fillStyle = '#6F6F68'
  g.font = '400 17px system-ui, sans-serif'
  y = wrapText(g, `${it.location || ''} · ${(it.time || '').replace('T', ' ').slice(0, 16)}`, 40, y + 12, W - 80, 26)
  if (it.desc) {
    g.fillStyle = '#6F6F68'
    g.font = '400 15px system-ui, sans-serif'
    y = wrapText(g, it.desc, 40, y + 10, W - 80, 24)
  }

  // 页脚品牌 + 二维码
  const footY = H - 120
  g.strokeStyle = '#EBEBE7'
  g.beginPath(); g.moveTo(40, footY - 26); g.lineTo(W - 40, footY - 26); g.stroke()

  g.fillStyle = '#141412'
  g.font = '800 19px system-ui, sans-serif'
  g.fillText(t('poster.brand'), 40, footY + 4)
  g.fillStyle = '#A3A39B'
  g.font = '400 13px system-ui, sans-serif'
  g.fillText(t('poster.scan'), 40, footY + 26)

  g.fillStyle = '#FFFFFF'
  g.fillRect(W - 40 - 74, footY - 30, 74, 74)
  g.fillStyle = '#141412'
  drawQr(g, W - 40 - 74 + 5, footY - 30 + 5, 74, 64 / 21)

  const fileName = t('poster.file', { title: it.title || t('detail.type.lost') })
  const how = await saveDataUrl(c.toDataURL('image/png'), fileName, { title: fileName })
  ElMessage.success(how === 'shared' ? t('poster.share') : t('poster.ok'))
  close()
}

watch(() => props.modelValue, (v) => { if (v) document.body.style.overflow = 'hidden'; else document.body.style.overflow = '' })

function onKey(e) { if (e.key === 'Escape' && props.modelValue) close() }
window.addEventListener('keydown', onKey)
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKey)
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <div class="poster-mask" :class="{ open: modelValue }" @click.self="close">
      <div class="poster-card" v-if="item">
        <div class="poster-ph">
          <img v-if="cover" :src="cover" :alt="item.title" />
          <span v-else style="font-size:72px">{{ catEmoji(item.category) }}</span>
        </div>
        <div class="poster-body">
          <h4>{{ item.type === 'lost' ? $t('poster.lost') : $t('poster.found') }} · {{ item.title }}</h4>
          <p>
            {{ item.location }} · {{ (item.time || '').replace('T', ' ').slice(0, 16) }}<br />
            {{ (item.desc || '').slice(0, 46) }}{{ (item.desc || '').length > 46 ? '…' : '' }}
          </p>
          <div class="poster-foot">
            <div class="poster-brand">
              <b>{{ $t('poster.brand') }}</b>
              <span>{{ $t('poster.scanShort') }}</span>
            </div>
            <div class="poster-qr">
              <i v-for="(d, i) in qrCells" :key="i" :class="{ d }"></i>
            </div>
          </div>
          <div class="poster-actions">
            <el-button type="primary" round style="flex:1" @click="save">{{ $t('poster.save') }}</el-button>
            <el-button round plain @click="close">{{ $t('poster.close') }}</el-button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>
