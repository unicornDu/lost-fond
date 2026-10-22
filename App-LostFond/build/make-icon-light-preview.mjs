import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const outDir = resolve(here, 'icon-light')

const concepts = [
  { id: '01-lantern', name: '提灯 · Lantern', note: '一盏暖灯，取“拾起一束光”的意象，温暖且有记忆点。' },
  { id: '02-focus', name: '聚焦 · Focus', note: '取景框对准暖橙光点，对应以图搜物的“锁定”。' },
  { id: '03-fold-s', name: '折光 S · Fold', note: '一条光带写成 S，远看是字母，近看是一束光。' },
  { id: '04-mark', name: '拾字 · Mark', note: '几何笔画重绘「拾」，品牌名最直接。' },
  { id: '05-spark', name: '星芒 · Spark', note: '四角星芒极简高对比，16px 仍清晰。' },
  { id: '06-tag', name: '标签 · Tag', note: '失物标签加一点光，功能语义最明确。' }
]

const b64 = (id, size) => readFileSync(resolve(outDir, `${id}${size ? `-${size}` : ''}.png`)).toString('base64')

const data = concepts.map((c, i) => ({
  num: String(i + 1).padStart(2, '0'),
  name: c.name,
  note: c.note,
  big: `data:image/png;base64,${b64(c.id, 320)}`,
  thumb: `data:image/png;base64,${b64(c.id, 160)}`,
  s48: `data:image/png;base64,${b64(c.id, 48)}`,
  s32: `data:image/png;base64,${b64(c.id, 32)}`,
  s16: `data:image/png;base64,${b64(c.id, 16)}`
}))

const html = `
<style>
  .sgl { --ink:#241c15; --muted:#7a6d5e; --orange:#e8823a; --line:#e3d9cb; --card:#ffffff;
    box-sizing:border-box; width:100%; color:var(--ink); font-family:inherit; border-radius:20px;
    background:linear-gradient(135deg, #f8f3eb 0%, #eee6da 58%, #f5efe6 100%);
    padding:26px; overflow:hidden; position:relative; }
  .sgl *, .sgl *::before, .sgl *::after { box-sizing:border-box; }
  .sgl-head { display:flex; align-items:flex-end; justify-content:space-between; gap:18px; flex-wrap:wrap; margin-bottom:22px; }
  .sgl-eyebrow { color:#b65a1f; font-size:11px; letter-spacing:.16em; font-weight:800; margin:0 0 6px; }
  .sgl-title { margin:0; font-size:26px; line-height:1.2; letter-spacing:.02em; color:var(--ink); }
  .sgl-sub { margin:7px 0 0; color:var(--muted); font-size:13px; line-height:1.6; }
  .sgl-toggle { border:1px solid var(--line); background:#fff; color:var(--ink);
    border-radius:999px; padding:9px 14px; font-size:12px; cursor:pointer; font-family:inherit;
    box-shadow:0 2px 8px rgba(80,55,30,.06); }
  .sgl-toggle:hover { border-color:rgba(232,130,58,.6); color:#b65a1f; }
  .sgl-body { display:grid; grid-template-columns:minmax(280px, 360px) minmax(0, 1fr); gap:20px; align-items:start; }
  @media (max-width:760px) { .sgl-body { grid-template-columns:1fr; } }
  .sgl-hero { border:1px solid var(--line); border-radius:22px; padding:22px 20px 18px;
    background:var(--card); text-align:center; position:relative; overflow:hidden;
    box-shadow:0 18px 40px rgba(80,55,30,.08); }
  .sgl-stage { border-radius:26px; padding:20px; margin:0 auto 18px; width:min(100%, 300px);
    background:radial-gradient(110% 110% at 50% 0%, #f2ece2, #e5dbcd); transition:background .25s ease;
    display:flex; justify-content:center; }
  .sgl-stage.is-dark { background:radial-gradient(110% 110% at 50% 0%, #221b15, #0d0b09); }
  .sgl-stage img { width:220px; height:220px; border-radius:48px; display:block;
    box-shadow:0 20px 38px rgba(74,48,24,.22); }
  .sgl-num { color:#b65a1f; font-size:11px; letter-spacing:.16em; font-weight:800; }
  .sgl-name { margin:6px 0 8px; font-size:21px; letter-spacing:.02em; color:var(--ink); }
  .sgl-note { margin:0 auto; max-width:290px; color:var(--muted); font-size:12.5px; line-height:1.65; min-height:40px; }
  .sgl-sizes { display:flex; align-items:flex-end; justify-content:center; gap:18px; margin-top:18px;
    padding-top:15px; border-top:1px solid var(--line); }
  .sgl-size { display:flex; flex-direction:column; align-items:center; gap:6px; color:#8b7e6f; font-size:10px; }
  .sgl-size .box { display:flex; align-items:flex-end; justify-content:center; height:48px; }
  .sgl-size img { display:block; border-radius:5px; }
  .sgl-size img.s48 { width:48px; height:48px; border-radius:11px; }
  .sgl-size img.s32 { width:32px; height:32px; border-radius:7px; }
  .sgl-size img.s16 { width:16px; height:16px; border-radius:4px; }
  .sgl-pick { margin-top:16px; width:100%; border:0; border-radius:12px; padding:12px 14px; cursor:pointer;
    background:linear-gradient(135deg, #f4a261, #d96f2a); color:#1b120a; font-weight:800; font-size:13px;
    font-family:inherit; letter-spacing:.03em; box-shadow:0 8px 20px rgba(217,111,42,.22); }
  .sgl-pick:hover { filter:brightness(1.05); }
  .sgl-grid { display:grid; grid-template-columns:repeat(3, minmax(0, 1fr)); gap:12px; }
  @media (max-width:900px) { .sgl-grid { grid-template-columns:repeat(2, minmax(0, 1fr)); } }
  .sgl-card { appearance:none; border:1px solid var(--line); background:var(--card);
    border-radius:16px; padding:12px 10px 11px; color:var(--ink); font-family:inherit; text-align:center;
    cursor:pointer; transition:border-color .18s ease, background .18s ease, transform .18s ease;
    position:relative; box-shadow:0 8px 20px rgba(80,55,30,.05); }
  .sgl-card:hover { border-color:rgba(232,130,58,.6); transform:translateY(-2px); }
  .sgl-card.is-active { border-color:var(--orange); background:#fff8f0; box-shadow:0 10px 24px rgba(217,111,42,.14); }
  .sgl-card.is-active::after { content:"已选"; position:absolute; top:8px; right:8px; font-size:10px;
    color:#1b120a; background:var(--orange); border-radius:999px; padding:2px 7px; font-weight:800; }
  .sgl-card img { width:112px; height:112px; border-radius:25px; display:block; margin:2px auto 10px;
    box-shadow:0 12px 24px rgba(80,55,30,.14); }
  .sgl-card-name { font-size:12.5px; font-weight:700; line-height:1.35; }
  .sgl-card-num { color:#948676; font-size:10px; letter-spacing:.12em; margin-top:4px; }
  .sgl-taskbar { margin-top:12px; border-radius:16px; padding:12px 14px;
    background:linear-gradient(180deg, #241d16, #14110d); display:flex; align-items:center; gap:14px;
    flex-wrap:wrap; box-shadow:0 12px 26px rgba(36,28,21,.16); }
  .sgl-taskbar-label { color:#b9aea1; font-size:10.5px; letter-spacing:.06em; }
  .sgl-taskbar-icons { display:flex; align-items:center; gap:12px; flex-wrap:wrap; }
  .sgl-taskbar-icons img { width:32px; height:32px; border-radius:7px; display:block; }
  .sgl-taskbar-icons img:nth-child(2n) { width:16px; height:16px; border-radius:4px; opacity:.92; }
  .sgl-foot { margin-top:16px; color:var(--muted); font-size:11.5px; line-height:1.7; }
  .sgl-foot strong { color:#b65a1f; font-weight:800; }
</style>
<div class="sgl" id="sg-icon-light-preview">
  <div class="sgl-head">
    <div>
      <p class="sgl-eyebrow">SHIGUANG · LIGHT ICON</p>
      <h2 class="sgl-title">拾光 · 白底图标方案</h2>
      <p class="sgl-sub">暖白底 + 暖橙 + 暖黑。点击任意方案查看大图、深浅任务栏和实际小尺寸。</p>
    </div>
    <button class="sgl-toggle" id="sgl-bg-toggle" type="button">切换深色任务栏</button>
  </div>
  <div class="sgl-body">
    <section class="sgl-hero" aria-live="polite">
      <div class="sgl-stage" id="sgl-stage"><img id="sgl-hero-img" src="" alt=""/></div>
      <div class="sgl-num" id="sgl-hero-num"></div>
      <h3 class="sgl-name" id="sgl-hero-name"></h3>
      <p class="sgl-note" id="sgl-hero-note"></p>
      <div class="sgl-sizes">
        <div class="sgl-size"><div class="box"><img class="s48" id="sgl-hero-48" src="" alt=""/></div><span>48 px</span></div>
        <div class="sgl-size"><div class="box"><img class="s32" id="sgl-hero-32" src="" alt=""/></div><span>32 px</span></div>
        <div class="sgl-size"><div class="box"><img class="s16" id="sgl-hero-16" src="" alt=""/></div><span>16 px</span></div>
      </div>
      <button class="sgl-pick" id="sgl-pick" type="button">使用这个方案</button>
    </section>
    <section>
      <div class="sgl-grid" id="sgl-grid"></div>
      <div class="sgl-taskbar">
        <span class="sgl-taskbar-label">深色任务栏 · 32 / 16px</span>
        <div class="sgl-taskbar-icons" id="sgl-taskbar"></div>
      </div>
      <p class="sgl-foot"><strong>建议：</strong>02 聚焦最清晰，也最贴合以图搜物；01 提灯最温暖；06 标签最像失物招领。选定后回复编号即可。</p>
    </section>
  </div>
</div>
<script>
(() => {
  const data = ${JSON.stringify(data).replace(/</g, '\\u003c')};
  const root = document.getElementById('sg-icon-light-preview');
  if (!root || root.dataset.ready) return;
  root.dataset.ready = '1';
  const $ = (id) => root.querySelector('#' + id);
  const heroImg = $('sgl-hero-img'), heroNum = $('sgl-hero-num'), heroName = $('sgl-hero-name'), heroNote = $('sgl-hero-note');
  const hero48 = $('sgl-hero-48'), hero32 = $('sgl-hero-32'), hero16 = $('sgl-hero-16');
  const grid = $('sgl-grid'), taskbar = $('sgl-taskbar');
  let current = 1;

  const cards = data.map((item, index) => {
    const card = document.createElement('button');
    card.type = 'button';
    card.className = 'sgl-card';
    card.innerHTML = '<img src="' + item.thumb + '" alt="' + item.name + '"/>'
      + '<div class="sgl-card-name">' + item.name + '</div>'
      + '<div class="sgl-card-num">' + item.num + '</div>';
    card.addEventListener('click', () => select(index));
    grid.appendChild(card);
    return card;
  });

  data.forEach((item) => {
    [item.s32, item.s16].forEach((src) => {
      const img = document.createElement('img');
      img.src = src;
      img.alt = item.name;
      img.title = item.name;
      taskbar.appendChild(img);
    });
  });

  function select(index) {
    current = index;
    const item = data[index];
    heroImg.src = item.big;
    heroImg.alt = item.name;
    hero48.src = item.s48;
    hero32.src = item.s32;
    hero16.src = item.s16;
    heroNum.textContent = item.num;
    heroName.textContent = item.name;
    heroNote.textContent = item.note;
    cards.forEach((card, i) => card.classList.toggle('is-active', i === index));
  }

  $('sgl-bg-toggle').addEventListener('click', (event) => {
    const dark = $('sgl-stage').classList.toggle('is-dark');
    event.currentTarget.textContent = dark ? '切换浅色底' : '切换深色任务栏';
  });

  $('sgl-pick').addEventListener('click', () => {
    $('sgl-pick').textContent = '已选 ' + data[current].num + ' · 回复编号即可应用';
  });

  select(1);
})();
</script>`

const vizDir = process.env.QODER_VIZ_DIR
if (vizDir) {
  mkdirSync(vizDir, { recursive: true })
  writeFileSync(resolve(vizDir, 'icon-light-preview.html'), html)
  console.log('wrote', resolve(vizDir, 'icon-light-preview.html'))
} else {
  writeFileSync(resolve(outDir, 'preview.html'), html)
  console.log('wrote', resolve(outDir, 'preview.html'))
}
