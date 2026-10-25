import { createRequire } from 'node:module'
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const require = createRequire(resolve(dirname(fileURLToPath(import.meta.url)), '../frontend/package.json'))
const here = dirname(fileURLToPath(import.meta.url))
const root = resolve(here, '..')
const iconDir = resolve(root, 'build/icon-refresh')
const outDir = process.env.QODER_VIZ_DIR
  ? resolve(process.env.QODER_VIZ_DIR)
  : iconDir
mkdirSync(outDir, { recursive: true })

const concepts = JSON.parse(readFileSync(resolve(iconDir, 'concepts.json'), 'utf8'))
const b64 = (file) => readFileSync(resolve(iconDir, file)).toString('base64')
const img = (slug, size) => `data:image/png;base64,${b64(`${slug}-${size}.png`)}`

const cards = concepts.map((c, i) => `
  <article class="card">
    <header class="card-head">
      <span class="num">${String(i + 1).padStart(2, '0')}</span>
      <h2>${c.name}</h2>
    </header>
    <div class="stage"><img src="${img(c.slug, 320)}" alt="${c.name}" width="320" height="320"></div>
    <p class="desc">${c.desc}</p>
    <div class="sizes">
      <span>实际尺寸</span>
      <figure><img src="${img(c.slug, 64)}" alt="" width="64" height="64"><figcaption>64</figcaption></figure>
      <figure><img src="${img(c.slug, 32)}" alt="" width="32" height="32"><figcaption>32</figcaption></figure>
      <figure><img src="${img(c.slug, 16)}" alt="" width="16" height="16"><figcaption>16</figcaption></figure>
    </div>
  </article>`).join('')

const html = `<style>
  .ip { --paper:#fffdf8; --bg:#f5f1e9; --line:#e9e1d4; --ink:#211f1c; --muted:#7c7569; --faint:#a39b8c;
    margin:0; padding:34px 30px 50px; background:var(--bg); color:var(--ink);
    font-family:"Segoe UI Variable Text","Segoe UI","Microsoft YaHei","PingFang SC",system-ui,sans-serif; }
  .ip * { box-sizing:border-box; }
  .ip .head { max-width:1180px; margin:0 auto 22px; }
  .ip .eyebrow { margin:0 0 8px; color:#a8842d; font-size:11px; font-weight:700; letter-spacing:.2em; }
  .ip h1 { margin:0; font-family:"Segoe UI Variable Display","Segoe UI","Microsoft YaHei",sans-serif; font-size:30px; font-weight:650; letter-spacing:-.02em; }
  .ip .sub { margin:9px 0 0; color:var(--muted); font-size:13px; line-height:1.7; }
  .ip .hint { display:inline-flex; margin-top:14px; padding:7px 11px; border:1px solid var(--line); border-radius:999px; background:var(--paper); color:var(--muted); font-size:12px; }
  .ip .grid { max-width:1180px; margin:0 auto; display:grid; grid-template-columns:repeat(3, minmax(0,1fr)); gap:18px; }
  .ip .card { min-width:0; padding:19px 19px 17px; border:1px solid var(--line); border-radius:20px; background:var(--paper); box-shadow:0 1px 2px rgba(40,32,20,.03); }
  .ip .card-head { display:flex; align-items:center; gap:9px; }
  .ip .num { color:#b08a36; font-size:12px; font-weight:750; letter-spacing:.08em; }
  .ip h2 { margin:0; font-size:15px; font-weight:650; letter-spacing:.01em; }
  .ip .stage { display:grid; place-items:center; margin-top:14px; padding:12px; border-radius:16px; background:#f7f2e9; }
  .ip .stage img { display:block; width:min(100%, 300px); height:auto; }
  .ip .desc { min-height:40px; margin:13px 1px 0; color:var(--muted); font-size:12.5px; line-height:1.65; }
  .ip .sizes { display:flex; align-items:flex-end; gap:12px; margin-top:12px; padding-top:12px; border-top:1px solid var(--line); color:var(--faint); font-size:11px; }
  .ip .sizes > span { margin-right:2px; }
  .ip figure { margin:0; display:grid; justify-items:center; gap:3px; }
  .ip figure img { display:block; }
  .ip figcaption { color:var(--faint); font-size:9px; line-height:1; }
  .ip .foot { max-width:1180px; margin:22px auto 0; color:var(--faint); font-size:11.5px; line-height:1.7; }
  @media (max-width:1000px) { .ip .grid { grid-template-columns:repeat(2, minmax(0,1fr)); } }
  @media (max-width:680px) { .ip { padding:22px 16px 36px; } .ip .grid { grid-template-columns:1fr; } .ip h1 { font-size:25px; } }
</style>
<div class="ip">
  <header class="head">
    <p class="eyebrow">SHIGUANG · ICON DIRECTIONS</p>
    <h1>拾光 · 新图标方案</h1>
    <p class="sub">白色圆角底 · 墨色粗线条 · 暖黄点睛。参考 Qoder 图标的友好、圆润和留白感，同时把“失物回家 / 拾光 / 寻回”放进入口符号里。</p>
    <div class="hint">回复编号 01–06 即可，我把它落到桌面图标并重新打包</div>
  </header>
  <main class="grid">${cards}</main>
  <p class="foot">当前为方案预览，尚未替换正式图标。底色为白色圆角，64 / 32 / 16 px 为实际显示尺寸。</p>
</div>`

const out = resolve(outDir, 'icon-refresh.html')
writeFileSync(out, html, 'utf8')
console.log(out)
