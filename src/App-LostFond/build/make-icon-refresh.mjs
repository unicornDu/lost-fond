import { createRequire } from 'node:module'
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const require = createRequire(resolve(dirname(fileURLToPath(import.meta.url)), '../frontend/package.json'))
const sharp = require('sharp')

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const outDir = resolve(root, 'build/icon-refresh')
mkdirSync(outDir, { recursive: true })

const INK = '#211F1C'
const YELLOW = '#F4C84A'
const YELLOW_DEEP = '#E5B334'
const CREAM = '#FFFDF8'
const tile = `
  <rect width="1024" height="1024" rx="224" fill="#FFFFFF"/>
  <rect x="3" y="3" width="1018" height="1018" rx="221" fill="none" stroke="#F0EADF" stroke-width="4"/>
`

const stroke = (d, width = 88, color = INK, extra = '') => `
  <path d="${d}" fill="none" stroke="${color}" stroke-width="${width}" stroke-linecap="round" stroke-linejoin="round" ${extra}/>
`

const heart = (cx, cy, size, fill = YELLOW) => {
  const s = size / 2
  return `<path d="M ${cx} ${cy + s * 0.9}
    C ${cx - s * 0.95} ${cy + s * 0.24} ${cx - s * 1.1} ${cy - s * 0.38} ${cx - s * 0.62} ${cy - s * 0.74}
    C ${cx - s * 0.3} ${cy - s * 0.98} ${cx - s * 0.02} ${cy - s * 0.8} ${cx} ${cy - s * 0.54}
    C ${cx + s * 0.02} ${cy - s * 0.8} ${cx + s * 0.3} ${cy - s * 0.98} ${cx + s * 0.62} ${cy - s * 0.74}
    C ${cx + s * 1.1} ${cy - s * 0.38} ${cx + s * 0.95} ${cy + s * 0.24} ${cx} ${cy + s * 0.9} Z"
    fill="${fill}"/>`
}

const spark = (cx, cy, r, fill = CREAM) => {
  const k = r * 0.12
  return `<path d="M ${cx} ${cy - r}
    C ${cx + k} ${cy - r * 0.36} ${cx + r * 0.36} ${cy - k} ${cx + r} ${cy}
    C ${cx + r * 0.36} ${cy + k} ${cx + k} ${cy + r * 0.36} ${cx} ${cy + r}
    C ${cx - k} ${cy + r * 0.36} ${cx - r * 0.36} ${cy + k} ${cx - r} ${cy}
    C ${cx - r * 0.36} ${cy - k} ${cx - k} ${cy - r * 0.36} ${cx} ${cy - r} Z"
    fill="${fill}"/>`
}

const concepts = [
  {
    slug: '01-homebound',
    name: '归家 · Homebound',
    desc: '暖黄的门从屋里亮着，粗线条房子像张开双臂。',
    svg: `
      ${stroke('M 252 500 L 486 292 Q 512 268 538 292 L 772 500', 86)}
      ${stroke('M 320 500 L 320 726 Q 320 790 384 790 L 640 790 Q 704 790 704 726 L 704 500', 86)}
      <rect x="436" y="576" width="152" height="214" rx="76" fill="${YELLOW}"/>
      <circle cx="550" cy="692" r="13" fill="${INK}"/>
    `,
  },
  {
    slug: '02-reunion',
    name: '相逢 · Reunion',
    desc: '两段圆弧向中间靠拢，暖黄的圆是重逢的那束光。',
    svg: `
      ${stroke('M 392 330 C 300 360 240 430 240 512 C 240 594 300 664 392 694', 84)}
      ${stroke('M 632 330 C 724 360 784 430 784 512 C 784 594 724 664 632 694', 84)}
      <circle cx="512" cy="512" r="94" fill="${YELLOW}"/>
    `,
  },
  {
    slug: '03-glow-smile',
    name: '拾光 · Glow',
    desc: '像朋友一样的笑眼与弯弯嘴角，右侧藏着一颗暖黄的太阳。',
    svg: `
      <circle cx="668" cy="548" r="198" fill="${YELLOW}"/>
      ${stroke('M 304 548 C 320 690 442 762 550 716 C 620 686 654 620 666 552', 94)}
      <rect x="416" y="344" width="58" height="126" rx="29" fill="${INK}"/>
      <rect x="552" y="344" width="58" height="126" rx="29" fill="${INK}"/>
      ${spark(800, 280, 62, YELLOW)}
    `,
  },
  {
    slug: '04-key-home',
    name: '钥匙 · Key Home',
    desc: '一把圆润的钥匙，握柄里放着一颗暖黄的心。',
    svg: `
      <g transform="rotate(-16 512 512)">
        <circle cx="360" cy="512" r="146" fill="none" stroke="${INK}" stroke-width="90"/>
        ${heart(360, 512, 148)}
        ${stroke('M 502 512 L 780 512', 90)}
        ${stroke('M 632 512 L 632 620', 82)}
        ${stroke('M 712 512 L 712 594', 82)}
      </g>
    `,
  },
  {
    slug: '05-search-light',
    name: '寻光 · Search',
    desc: '暖黄镜片里藏着一颗白色星芒，找回遗失的那束光。',
    svg: `
      <circle cx="456" cy="456" r="218" fill="${YELLOW}" stroke="${INK}" stroke-width="88"/>
      ${spark(456, 456, 112, CREAM)}
      ${stroke('M 612 612 L 792 792', 94)}
    `,
  },
  {
    slug: '06-nest',
    name: '归巢 · Nest',
    desc: '墨色的巢托住一颗暖黄的心，失物与牵挂一起回家。',
    svg: `
      ${stroke('M 286 548 C 286 756 392 834 512 834 C 632 834 738 756 738 548', 96)}
      ${stroke('M 258 664 C 306 732 366 776 432 798', 30, INK, 'opacity=".48"')}
      ${stroke('M 766 664 C 718 732 658 776 592 798', 30, INK, 'opacity=".48"')}
      <g transform="rotate(-6 512 646)">${heart(512, 646, 180)}</g>
    `,
  },
].map((c) => ({
  ...c,
  svg: `<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">${tile}${c.svg}</svg>`,
}))

async function render(svg, size, file) {
  await sharp(Buffer.from(svg)).resize(size, size).png().toFile(resolve(outDir, file))
}

for (const c of concepts) {
  await render(c.svg, 1024, `${c.slug}-1024.png`)
  for (const size of [512, 320, 160, 64, 48, 32, 16]) {
    await render(c.svg, size, `${c.slug}-${size}.png`)
  }
  console.log('rendered', c.slug)
}

const cardW = 500
const cardH = 500
const pad = 40
const headerH = 150
const sheetW = pad * 2 + cardW * 3 + 20 * 2
const sheetH = headerH + pad + cardH * 2 + 24 + pad
const imageData = (slug) => `data:image/png;base64,${require('node:fs').readFileSync(resolve(outDir, `${slug}-320.png`)).toString('base64')}`
const smallData = (slug, size) => `data:image/png;base64,${require('node:fs').readFileSync(resolve(outDir, `${slug}-${size}.png`)).toString('base64')}`

const cards = concepts.map((c, i) => {
  const col = i % 3
  const row = Math.floor(i / 3)
  const x = pad + col * (cardW + 20)
  const y = headerH + pad + row * (cardH + 24)
  return `
    <g transform="translate(${x} ${y})">
      <rect width="${cardW}" height="${cardH}" rx="26" fill="#FFFDF8" stroke="#E9E1D4" stroke-width="2"/>
      <text x="34" y="54" font-family="'Segoe UI','Microsoft YaHei',sans-serif" font-size="24" font-weight="700" fill="#211F1C">${String(i + 1).padStart(2, '0')}</text>
      <text x="78" y="54" font-family="'Segoe UI','Microsoft YaHei',sans-serif" font-size="24" font-weight="600" fill="#211F1C">${c.name}</text>
      <image href="${imageData(c.slug)}" x="90" y="78" width="320" height="320"/>
      <text x="34" y="432" font-family="'Segoe UI','Microsoft YaHei',sans-serif" font-size="17" fill="#756F63">${c.desc}</text>
      <g transform="translate(34 455)">
        <text x="0" y="34" font-family="'Segoe UI','Microsoft YaHei',sans-serif" font-size="13" fill="#A29A8B">实际尺寸</text>
        <image href="${smallData(c.slug, 64)}" x="78" y="4" width="64" height="64"/>
        <image href="${smallData(c.slug, 32)}" x="156" y="20" width="32" height="32"/>
        <image href="${smallData(c.slug, 16)}" x="202" y="36" width="16" height="16"/>
      </g>
    </g>`
}).join('')

const sheetSvg = `
<svg xmlns="http://www.w3.org/2000/svg" width="${sheetW}" height="${sheetH}" viewBox="0 0 ${sheetW} ${sheetH}">
  <rect width="${sheetW}" height="${sheetH}" fill="#F5F1E9"/>
  <text x="${pad}" y="64" font-family="'Segoe UI','Microsoft YaHei',sans-serif" font-size="34" font-weight="650" fill="#211F1C">拾光 · 新图标方案</text>
  <text x="${pad}" y="102" font-family="'Segoe UI','Microsoft YaHei',sans-serif" font-size="17" fill="#7C7569">白色圆角底 · 墨色粗线条 · 暖黄点睛 · 参考 Qoder 的友好图形语言</text>
  <text x="${pad}" y="132" font-family="'Segoe UI','Microsoft YaHei',sans-serif" font-size="14" fill="#A39B8C">回复编号即可，我把它落到 desktop/build/icon.png 并重新打包</text>
  ${cards}
</svg>`

await sharp(Buffer.from(sheetSvg)).png().toFile(resolve(outDir, 'contact-sheet.png'))
writeFileSync(resolve(outDir, 'concepts.json'), JSON.stringify(concepts.map(({ slug, name, desc, svg }) => ({ slug, name, desc, svg })), null, 2))
console.log('contact sheet ->', resolve(outDir, 'contact-sheet.png'))
