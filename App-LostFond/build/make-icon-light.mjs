import { createRequire } from 'node:module'
import { mkdirSync, readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const require = createRequire(resolve(dirname(fileURLToPath(import.meta.url)), '../frontend/package.json'))
const sharp = require('sharp')

const here = dirname(fileURLToPath(import.meta.url))
const outDir = resolve(here, 'icon-light')
mkdirSync(outDir, { recursive: true })

const font = "'Microsoft YaHei','Noto Sans SC',sans-serif"
const ORANGE = '#E8823A'
const ORANGE_LIGHT = '#F6B77C'
const ORANGE_DEEP = '#C95C22'
const INK = '#241C15'
const TERRA = '#8A4A22'
const PAPER = '#FFFFFF'

const spark = (cx, cy, r, fill, opacity = 1) => `
  <path d="M ${cx} ${cy - r}
           C ${cx + r * 0.12} ${cy - r * 0.36} ${cx + r * 0.36} ${cy - r * 0.12} ${cx + r} ${cy}
           C ${cx + r * 0.36} ${cy + r * 0.12} ${cx + r * 0.12} ${cy + r * 0.36} ${cx} ${cy + r}
           C ${cx - r * 0.12} ${cy + r * 0.36} ${cx - r * 0.36} ${cy + r * 0.12} ${cx - r} ${cy}
           C ${cx - r * 0.36} ${cy - r * 0.12} ${cx - r * 0.12} ${cy - r * 0.36} ${cx} ${cy - r} Z"
        fill="${fill}" opacity="${opacity}"/>`

const frame = (inner) => `
<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">
  <defs>
    <linearGradient id="tile" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="#FFFFFF"/>
      <stop offset="58%" stop-color="#FDFBF7"/>
      <stop offset="100%" stop-color="#F4EEE4"/>
    </linearGradient>
    <radialGradient id="warm" cx="50%" cy="28%" r="72%">
      <stop offset="0%" stop-color="#F8E8D8" stop-opacity=".72"/>
      <stop offset="100%" stop-color="#F8E8D8" stop-opacity="0"/>
    </radialGradient>
    <linearGradient id="rim" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="#FFFFFF"/>
      <stop offset="55%" stop-color="#E9E0D3"/>
      <stop offset="100%" stop-color="#DCCFBE"/>
    </linearGradient>
  </defs>
  <rect width="1024" height="1024" rx="224" fill="url(#tile)"/>
  <rect width="1024" height="1024" rx="224" fill="url(#warm)"/>
  <rect x="2" y="2" width="1020" height="1020" rx="222" fill="none"
        stroke="url(#rim)" stroke-width="4"/>
  ${inner}
</svg>`

const concepts = [
  {
    id: '01-lantern',
    name: '提灯 · Lantern',
    note: '一盏暖灯，取“拾起一束光”的意象，温暖且有记忆点。',
    svg: frame(`
      <defs>
        <radialGradient id="lampGlow" cx="50%" cy="52%" r="52%">
          <stop offset="0%" stop-color="${ORANGE}" stop-opacity=".24"/>
          <stop offset="55%" stop-color="${ORANGE}" stop-opacity=".08"/>
          <stop offset="100%" stop-color="${ORANGE}" stop-opacity="0"/>
        </radialGradient>
        <linearGradient id="lampBody" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stop-color="#F8BE86"/>
          <stop offset="42%" stop-color="#E8823A"/>
          <stop offset="100%" stop-color="#C4551F"/>
        </linearGradient>
        <linearGradient id="lampCap" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#4A3322"/>
          <stop offset="100%" stop-color="#2A1C12"/>
        </linearGradient>
        <filter id="lampShadow" x="-40%" y="-30%" width="180%" height="180%">
          <feDropShadow dx="0" dy="24" stdDeviation="28" flood-color="${TERRA}" flood-opacity=".20"/>
        </filter>
      </defs>
      <circle cx="512" cy="540" r="344" fill="url(#lampGlow)"/>
      <path d="M 466 266 C 466 204 558 204 558 266" fill="none" stroke="${ORANGE}"
            stroke-width="18" stroke-linecap="round"/>
      <rect x="398" y="248" width="228" height="60" rx="30" fill="url(#lampCap)"/>
      <rect x="370" y="300" width="284" height="472" rx="142" fill="url(#lampBody)"
            filter="url(#lampShadow)"/>
      <path d="M 430 334 C 400 424 400 648 430 738" fill="none" stroke="${TERRA}"
            stroke-width="10" opacity=".18"/>
      <path d="M 512 312 V 760" fill="none" stroke="${TERRA}" stroke-width="10" opacity=".15"/>
      <path d="M 594 334 C 624 424 624 648 594 738" fill="none" stroke="${TERRA}"
            stroke-width="10" opacity=".18"/>
      ${spark(512, 530, 76, PAPER, 0.94)}
      <rect x="398" y="748" width="228" height="60" rx="30" fill="url(#lampCap)"/>
      <path d="M 512 808 V 842" stroke="${ORANGE}" stroke-width="14" stroke-linecap="round"/>
      <circle cx="512" cy="860" r="16" fill="${ORANGE}"/>
    `)
  },
  {
    id: '02-focus',
    name: '聚焦 · Focus',
    note: '取景框对准暖橙光点，对应以图搜物的“锁定”。',
    svg: frame(`
      <defs>
        <radialGradient id="orb" cx="38%" cy="30%" r="72%">
          <stop offset="0%" stop-color="#F7B076"/>
          <stop offset="58%" stop-color="#E8823A"/>
          <stop offset="100%" stop-color="#C95C22"/>
        </radialGradient>
      </defs>
      <g fill="none" stroke="${INK}" stroke-width="44" stroke-linecap="round" opacity=".9">
        <path d="M 240 390 V 240 H 390"/>
        <path d="M 634 240 H 784 V 390"/>
        <path d="M 784 634 V 784 H 634"/>
        <path d="M 390 784 H 240 V 634"/>
      </g>
      <circle cx="512" cy="512" r="176" fill="url(#orb)"/>
      <circle cx="512" cy="512" r="176" fill="none" stroke="${TERRA}" stroke-opacity=".16" stroke-width="6"/>
      ${spark(512, 512, 94, PAPER, 0.98)}
    `)
  },
  {
    id: '03-fold-s',
    name: '折光 S · Fold',
    note: '一条光带写成 S，远看是字母，近看是一束光。',
    svg: frame(`
      <defs>
        <linearGradient id="sRibbon" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="#F8BE86"/>
          <stop offset="48%" stop-color="#E8823A"/>
          <stop offset="100%" stop-color="#C4551F"/>
        </linearGradient>
        <filter id="sShadow" x="-30%" y="-30%" width="160%" height="170%">
          <feDropShadow dx="0" dy="18" stdDeviation="22" flood-color="${TERRA}" flood-opacity=".20"/>
        </filter>
      </defs>
      <path d="M 716 320
               C 716 246 636 208 548 208
               C 448 208 348 254 348 356
               C 348 452 436 486 520 512
               C 620 544 700 590 700 700
               C 700 806 606 858 504 858
               C 412 858 328 820 300 744"
            fill="none" stroke="url(#sRibbon)" stroke-width="132"
            stroke-linecap="round" filter="url(#sShadow)"/>
      ${spark(778, 252, 46, TERRA, 0.9)}
    `)
  },
  {
    id: '04-mark',
    name: '拾字 · Mark',
    note: '几何笔画重绘「拾」，品牌名最直接。',
    svg: frame(`
      <g fill="none" stroke-linecap="round" stroke-linejoin="round"
         transform="translate(512 512) scale(1.08) translate(-512 -512)">
        <path d="M 336 252 V 690 Q 336 768 268 750" stroke="${ORANGE}" stroke-width="64"/>
        <path d="M 214 424 H 456" stroke="${ORANGE}" stroke-width="64"/>
        <path d="M 300 700 L 380 622" stroke="${ORANGE}" stroke-width="52"/>
        <path d="M 500 424 L 614 304 L 728 424" stroke="${INK}" stroke-width="64"/>
        <path d="M 474 484 H 754" stroke="${INK}" stroke-width="64"/>
        <path d="M 530 572 H 698 V 762 H 530 Z" stroke="${INK}" stroke-width="58"/>
      </g>
    `)
  },
  {
    id: '05-spark',
    name: '星芒 · Spark',
    note: '四角星芒极简高对比，16px 仍清晰。',
    svg: frame(`
      <defs>
        <linearGradient id="star" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="#F8C08C"/>
          <stop offset="46%" stop-color="#E8823A"/>
          <stop offset="100%" stop-color="#C95C22"/>
        </linearGradient>
        <filter id="starShadow" x="-30%" y="-30%" width="160%" height="170%">
          <feDropShadow dx="0" dy="18" stdDeviation="24" flood-color="${TERRA}" flood-opacity=".16"/>
        </filter>
      </defs>
      <circle cx="512" cy="548" r="360" fill="none" stroke="${ORANGE}" stroke-opacity=".16" stroke-width="4"/>
      ${spark(512, 560, 342, 'url(#star)')}
      ${spark(768, 274, 82, TERRA, 0.9)}
    `)
  },
  {
    id: '06-tag',
    name: '标签 · Tag',
    note: '失物标签加一点光，功能语义最明确。',
    svg: frame(`
      <defs>
        <filter id="tagShadow" x="-30%" y="-30%" width="170%" height="180%">
          <feDropShadow dx="0" dy="26" stdDeviation="28" flood-color="${TERRA}" flood-opacity=".22"/>
        </filter>
        <linearGradient id="tagBody" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="#F6A15E"/>
          <stop offset="100%" stop-color="#D96F2A"/>
        </linearGradient>
      </defs>
      <g transform="rotate(-14 512 512)">
        <path d="M 380 292 H 700 Q 758 292 758 350 V 690 Q 758 748 700 748 H 380
                 L 224 560 Q 206 540 224 520 Z"
              fill="url(#tagBody)" filter="url(#tagShadow)"/>
        <circle cx="360" cy="540" r="36" fill="${INK}"/>
        <path d="M 360 540 C 270 478 224 408 270 340 C 306 288 360 262 414 258"
              fill="none" stroke="${TERRA}" stroke-width="12" stroke-linecap="round" opacity=".9"/>
        ${spark(590, 512, 104, PAPER, 0.98)}
      </g>
    `)
  }
]

async function render(svg, size, file) {
  await sharp(Buffer.from(svg)).resize(size, size).png().toFile(file)
}

for (const c of concepts) {
  await sharp(Buffer.from(c.svg)).png().toFile(resolve(outDir, `${c.id}.png`))
  await render(c.svg, 512, resolve(outDir, `${c.id}-512.png`))
  await render(c.svg, 320, resolve(outDir, `${c.id}-320.png`))
  await render(c.svg, 160, resolve(outDir, `${c.id}-160.png`))
  await render(c.svg, 48, resolve(outDir, `${c.id}-48.png`))
  await render(c.svg, 32, resolve(outDir, `${c.id}-32.png`))
  await render(c.svg, 16, resolve(outDir, `${c.id}-16.png`))
  console.log('wrote', c.id)
}

const b64 = (file) => readFileSync(resolve(outDir, file)).toString('base64')
const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;')

const sheetSvg = `
<svg xmlns="http://www.w3.org/2000/svg" width="2400" height="1840" viewBox="0 0 2400 1840">
  <rect width="2400" height="1840" fill="#E9E2D7"/>
  <text x="120" y="118" font-family="${font}" font-size="66" font-weight="700" fill="#241C15">拾光 · 白底图标方案</text>
  <text x="120" y="174" font-family="${font}" font-size="28" fill="#75695B">暖白底 · 暖橙 #E8823A · 暖黑 #241C15 ｜ 6 个方向，均按 1024×1024 输出</text>
  ${concepts.map((c, i) => {
    const col = i % 3
    const row = Math.floor(i / 3)
    const x = 90 + col * 745
    const y = 220 + row * 780
    return `
      <g>
        <rect x="${x}" y="${y}" width="700" height="720" rx="40" fill="#FFFFFF"
              stroke="#DED4C6" stroke-width="2"/>
        <image x="${x + 120}" y="${y + 40}" width="460" height="460"
               href="data:image/png;base64,${b64(`${c.id}-512.png`)}"/>
        <text x="${x + 350}" y="${y + 574}" font-family="${font}" font-size="38" font-weight="700"
              fill="#241C15" text-anchor="middle">${esc(c.name)}</text>
        <text x="${x + 350}" y="${y + 622}" font-family="${font}" font-size="22"
              fill="#75695B" text-anchor="middle">${esc(c.note)}</text>
        <g transform="translate(${x + 168} ${y + 660})">
          <text font-family="${font}" font-size="19" fill="#8B7E6F" y="30">48</text>
          <image x="38" y="0" width="40" height="40" href="data:image/png;base64,${b64(`${c.id}-48.png`)}"/>
          <text font-family="${font}" font-size="19" fill="#8B7E6F" x="102" y="30">32</text>
          <image x="140" y="4" width="32" height="32" href="data:image/png;base64,${b64(`${c.id}-32.png`)}"/>
          <text font-family="${font}" font-size="19" fill="#8B7E6F" x="188" y="30">16</text>
          <image x="226" y="12" width="16" height="16" href="data:image/png;base64,${b64(`${c.id}-16.png`)}"/>
        </g>
      </g>`
  }).join('')}
</svg>`

await sharp(Buffer.from(sheetSvg)).png().toFile(resolve(outDir, '00-overview.png'))
console.log('wrote 00-overview.png')
