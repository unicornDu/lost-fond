import { createRequire } from 'node:module'
import { mkdirSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const require = createRequire(resolve(dirname(fileURLToPath(import.meta.url)), '../frontend/package.json'))
const sharp = require('sharp')

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const out = (p) => resolve(root, p)

const INK = '#211F1C'
const YELLOW = '#F4C84A'

const spark = (cx, cy, r, fill) => `
  <path d="M ${cx} ${cy - r}
           C ${cx + r * 0.12} ${cy - r * 0.36} ${cx + r * 0.36} ${cy - r * 0.12} ${cx + r} ${cy}
           C ${cx + r * 0.36} ${cy + r * 0.12} ${cx + r * 0.12} ${cy + r * 0.36} ${cx} ${cy + r}
           C ${cx - r * 0.12} ${cy + r * 0.36} ${cx - r * 0.36} ${cy + r * 0.12} ${cx - r} ${cy}
           C ${cx - r * 0.36} ${cy - r * 0.12} ${cx - r * 0.12} ${cy - r * 0.36} ${cx} ${cy - r} Z"
        fill="${fill}"/>`

const iconSvg = `
<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">
  <rect width="1024" height="1024" rx="224" fill="#FFFFFF"/>
  <rect x="3" y="3" width="1018" height="1018" rx="221" fill="none" stroke="#F0EADF" stroke-width="4"/>

  <circle cx="668" cy="548" r="198" fill="${YELLOW}"/>
  <path d="M 304 548 C 320 690 442 762 550 716 C 620 686 654 620 666 552"
        fill="none" stroke="${INK}" stroke-width="94" stroke-linecap="round" stroke-linejoin="round"/>
  <rect x="416" y="344" width="58" height="126" rx="29" fill="${INK}"/>
  <rect x="552" y="344" width="58" height="126" rx="29" fill="${INK}"/>
  ${spark(800, 280, 62, YELLOW)}
</svg>`

async function render(svg, size, file) {
  const target = out(file)
  mkdirSync(dirname(target), { recursive: true })
  await sharp(Buffer.from(svg)).resize(size, size).png().toFile(target)
  console.log('wrote', file)
}

await render(iconSvg, 1024, 'desktop/build/icon.png')
await render(iconSvg, 64, 'frontend/src/assets/img/app-icon.png')
