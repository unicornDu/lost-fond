import { createRequire } from 'node:module'
import { mkdirSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const require = createRequire(resolve(dirname(fileURLToPath(import.meta.url)), '../frontend/package.json'))
const sharp = require('sharp')

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const out = (p) => resolve(root, p)
const font = "'Microsoft YaHei','Noto Sans SC',sans-serif"

const glyph = (fill = '#FAFAF8', size = 470) => `
  <text x="512" y="${512 + size * 0.36}" font-family="${font}" font-size="${size}"
        font-weight="700" fill="${fill}" text-anchor="middle">拾</text>`

const iconSvg = `
<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">
  <defs>
    <radialGradient id="glow" cx="50%" cy="42%" r="52%">
      <stop offset="0%" stop-color="#E8823A" stop-opacity=".22"/>
      <stop offset="60%" stop-color="#E8823A" stop-opacity=".07"/>
      <stop offset="100%" stop-color="#E8823A" stop-opacity="0"/>
    </radialGradient>
  </defs>
  <rect width="1024" height="1024" fill="#161614"/>
  <rect width="1024" height="1024" fill="url(#glow)"/>
  <circle cx="512" cy="492" r="392" fill="none" stroke="#E8823A" stroke-opacity=".4" stroke-width="5"/>
  <circle cx="836" cy="220" r="22" fill="#E8823A"/>
  ${glyph('#FAFAF8', 380)}
</svg>`

async function render(svg, size, file) {
  const target = out(file)
  mkdirSync(dirname(target), { recursive: true })
  await sharp(Buffer.from(svg)).resize(size, size).png().toFile(target)
  console.log('wrote', file)
}

await render(iconSvg, 1024, 'desktop/build/icon.png')
