import { createRequire } from 'node:module'
import { mkdirSync, readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const require = createRequire(resolve(dirname(fileURLToPath(import.meta.url)), '../frontend/package.json'))
const sharp = require('sharp')

const here = dirname(fileURLToPath(import.meta.url))
const outDir = resolve(here, 'icon-concepts')
mkdirSync(outDir, { recursive: true })

const font = "'Microsoft YaHei','Noto Sans SC',sans-serif"
const ORANGE = '#E8823A'
const PAPER = '#FAFAF8'
const INK = '#141412'

const glyph = (fill = PAPER, size = 380, cx = 512, cy = 512) => `
  <text x="${cx}" y="${cy + size * 0.36}" font-family="${font}" font-size="${size}"
        font-weight="700" fill="${fill}" text-anchor="middle">拾</text>`

const frame = (inner, palette = {}) => {
  const { base = '#141412', second = '#0E0D0B' } = palette
  return `
<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">
  <defs>
    <radialGradient id="glow" cx="50%" cy="40%" r="62%">
      <stop offset="0%" stop-color="${ORANGE}" stop-opacity=".20"/>
      <stop offset="52%" stop-color="${ORANGE}" stop-opacity=".06"/>
      <stop offset="100%" stop-color="${ORANGE}" stop-opacity="0"/>
    </radialGradient>
  </defs>
  <rect width="1024" height="1024" rx="224" fill="${base}"/>
  <rect width="1024" height="1024" rx="224" fill="${second}" opacity=".55"/>
  <rect width="1024" height="1024" rx="224" fill="url(#glow)"/>
  <rect x="1.5" y="1.5" width="1021" height="1021" rx="222.5" fill="none"
        stroke="#FFFFFF" stroke-opacity=".07" stroke-width="3"/>
  ${inner}
</svg>`
}

const concepts = [
  {
    id: '01-halo',
    name: '光环 · Halo',
    note: '保留现有光圈识别，收细线条、补高光，最稳妥的升级版。',
    svg: frame(`
      <circle cx="512" cy="500" r="362" fill="none" stroke="${ORANGE}" stroke-opacity=".14" stroke-width="2"/>
      <circle cx="512" cy="500" r="338" fill="none" stroke="${ORANGE}" stroke-width="28" stroke-linecap="round"/>
      <circle cx="796" cy="246" r="26" fill="${ORANGE}"/>
      <circle cx="796" cy="246" r="58" fill="${ORANGE}" opacity=".16"/>
      ${glyph(PAPER, 380, 512, 500)}
    `)
  },
  {
    id: '02-lens',
    name: '镜头 · Lens',
    note: '放大镜 + 拾字，直接表达“以图搜物”，小尺寸剪影最清楚。',
    svg: frame(`
      <circle cx="466" cy="452" r="292" fill="none" stroke="${ORANGE}" stroke-width="52"/>
      <path d="M 676 660 L 824 808" stroke="${ORANGE}" stroke-width="92" stroke-linecap="round"/>
      <path d="M 302 330 A 254 254 0 0 1 424 236" fill="none" stroke="${PAPER}"
            stroke-opacity=".38" stroke-width="18" stroke-linecap="round"/>
      <circle cx="708" cy="286" r="15" fill="${ORANGE}"/>
      ${glyph(PAPER, 300, 466, 452)}
    `)
  },
  {
    id: '03-sunrise',
    name: '日出 · Sunrise',
    note: '拾光＝拾起一束光，橙色半日从底部升起，品牌感最强。',
    svg: frame(`
      <defs>
        <clipPath id="half"><rect x="0" y="0" width="1024" height="704"/></clipPath>
        <linearGradient id="sun" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#F29B55"/>
          <stop offset="100%" stop-color="#D96F2A"/>
        </linearGradient>
        <radialGradient id="sunGlow" cx="50%" cy="70%" r="45%">
          <stop offset="0%" stop-color="${ORANGE}" stop-opacity=".30"/>
          <stop offset="100%" stop-color="${ORANGE}" stop-opacity="0"/>
        </radialGradient>
      </defs>
      <rect width="1024" height="1024" rx="224" fill="url(#sunGlow)"/>
      <circle cx="512" cy="704" r="292" fill="url(#sun)" clip-path="url(#half)"/>
      <path d="M 236 704 H 788" stroke="${ORANGE}" stroke-opacity=".55" stroke-width="5" stroke-linecap="round"/>
      <path d="M 346 604 V 660 M 512 580 V 650 M 678 604 V 660" stroke="${ORANGE}"
            stroke-opacity=".45" stroke-width="8" stroke-linecap="round"/>
      ${glyph(PAPER, 330, 512, 356)}
    `)
  },
  {
    id: '04-notice',
    name: '启事 · Notice',
    note: '一张失物招领卡片，亲和、有校园感，像贴出来的启事。',
    svg: frame(`
      <defs>
        <filter id="cardShadow" x="-30%" y="-30%" width="160%" height="170%">
          <feDropShadow dx="0" dy="26" stdDeviation="30" flood-color="#000000" flood-opacity=".45"/>
        </filter>
      </defs>
      <g transform="rotate(-7 512 512)" filter="url(#cardShadow)">
        <rect x="248" y="228" width="528" height="568" rx="58" fill="${PAPER}"/>
        <path d="M306 228 H718 A58 58 0 0 1 776 286 V356 H248 V286 A58 58 0 0 1 306 228 Z" fill="${ORANGE}"/>
        <circle cx="512" cy="292" r="17" fill="${INK}" opacity=".85"/>
        ${glyph(INK, 300, 512, 530)}
        <rect x="354" y="660" width="316" height="18" rx="9" fill="${ORANGE}" opacity=".8"/>
        <circle cx="716" cy="704" r="13" fill="${ORANGE}" opacity=".9"/>
      </g>
    `)
  },
  {
    id: '05-pin',
    name: '定位 · Pin',
    note: '定位针轮廓，强调“在哪儿丢的”，一眼能懂。',
    svg: frame(`
      <defs>
        <linearGradient id="pin" x1="0" y1="0" x2="0.4" y2="1">
          <stop offset="0%" stop-color="#F29B55"/>
          <stop offset="100%" stop-color="#D96F2A"/>
        </linearGradient>
        <filter id="pinShadow" x="-30%" y="-30%" width="160%" height="170%">
          <feDropShadow dx="0" dy="24" stdDeviation="28" flood-color="#000000" flood-opacity=".42"/>
        </filter>
      </defs>
      <path d="M512 884 C512 884 804 592 804 380 A292 292 0 1 0 220 380 C220 592 512 884 512 884 Z"
            fill="url(#pin)" filter="url(#pinShadow)"/>
      <circle cx="512" cy="380" r="172" fill="${INK}" opacity=".92"/>
      <circle cx="512" cy="380" r="172" fill="none" stroke="${PAPER}" stroke-opacity=".10" stroke-width="4"/>
      ${glyph(PAPER, 238, 512, 380)}
    `)
  },
  {
    id: '06-meet',
    name: '相遇 · Meet',
    note: '失主与拾主的两个圆相交，寓意“在这里相遇”，最品牌化。',
    svg: frame(`
      <circle cx="428" cy="512" r="252" fill="none" stroke="${ORANGE}" stroke-width="34"/>
      <circle cx="596" cy="512" r="252" fill="none" stroke="${PAPER}" stroke-opacity=".92" stroke-width="34"/>
      <circle cx="512" cy="512" r="362" fill="none" stroke="${ORANGE}" stroke-opacity=".10" stroke-width="2"/>
      <circle cx="512" cy="236" r="17" fill="${ORANGE}"/>
      ${glyph(PAPER, 292, 512, 512)}
    `)
  }
]

async function render(svg, size, file) {
  await sharp(Buffer.from(svg)).resize(size, size).png().toFile(file)
}

for (const c of concepts) {
  const master = resolve(outDir, `${c.id}.png`)
  await sharp(Buffer.from(c.svg)).png().toFile(master)
  await render(c.svg, 512, resolve(outDir, `${c.id}-512.png`))
  await render(c.svg, 160, resolve(outDir, `${c.id}-160.png`))
  await render(c.svg, 48, resolve(outDir, `${c.id}-48.png`))
  await render(c.svg, 32, resolve(outDir, `${c.id}-32.png`))
  console.log('wrote', c.id)
}

const b64 = (file) => readFileSync(resolve(outDir, file)).toString('base64')
const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;')

const sheetSvg = `
<svg xmlns="http://www.w3.org/2000/svg" width="2400" height="1780" viewBox="0 0 2400 1780">
  <rect width="2400" height="1780" fill="#F2F0EB"/>
  <text x="120" y="120" font-family="${font}" font-size="66" font-weight="700" fill="#1B1712">拾光 · 桌面图标方案</text>
  <text x="120" y="176" font-family="${font}" font-size="28" fill="#6E675D">暖橙 #E8823A · 近黑 #141412 · 米白 #FAFAF8</text>
  ${concepts.map((c, i) => {
    const col = i % 3
    const row = Math.floor(i / 3)
    const x = 120 + col * 740
    const y = 240 + row * 760
    return `
      <g>
        <rect x="${x}" y="${y}" width="680" height="690" rx="40" fill="#FFFFFF"/>
        <image x="${x + 105}" y="${y + 48}" width="470" height="470"
               href="data:image/png;base64,${b64(`${c.id}-512.png`)}"/>
        <text x="${x + 340}" y="${y + 588}" font-family="${font}" font-size="38" font-weight="700"
              fill="#1B1712" text-anchor="middle">${esc(c.name)}</text>
        <text x="${x + 340}" y="${y + 638}" font-family="${font}" font-size="22"
              fill="#6E675D" text-anchor="middle">${esc(c.note)}</text>
      </g>`
  }).join('')}
</svg>`

await sharp(Buffer.from(sheetSvg)).png().toFile(resolve(outDir, '00-overview.png'))
console.log('wrote 00-overview.png')
