import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const outDir = resolve(here, 'icon-redesign')

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
  .sgi { --ink:#141412; --paper:#f6f1e8; --muted:#a79e91; --orange:#e8823a; --line:rgba(255,255,255,.10);
    box-sizing:border-box; width:100%; color:var(--paper); font-family:inherit; border-radius:20px;
    background:radial-gradient(120% 100% at 50% 0%, #2b231b 0%, #171310 52%, #0d0b09 100%);
    padding:26px; overflow:hidden; position:relative; }
  .sgi *, .sgi *::before, .sgi *::after { box-sizing:border-box; }
  .sgi-head { display:flex; align-items:flex-end; justify-content:space-between; gap:18px; flex-wrap:wrap; margin-bottom:22px; }
  .sgi-eyebrow { color:var(--orange); font-size:11px; letter-spacing:.16em; font-weight:700; margin:0 0 6px; }
  .sgi-title { margin:0; font-size:26px; line-height:1.2; letter-spacing:.02em; }
  .sgi-sub { margin:7px 0 0; color:var(--muted); font-size:13px; line-height:1.6; }
  .sgi-toggle { border:1px solid var(--line); background:rgba(255,255,255,.05); color:var(--paper);
    border-radius:999px; padding:9px 14px; font-size:12px; cursor:pointer; font-family:inherit; }
  .sgi-toggle:hover { border-color:rgba(232,130,58,.55); color:#fff; }
  .sgi-body { display:grid; grid-template-columns:minmax(280px, 360px) minmax(0, 1fr); gap:20px; align-items:start; }
  @media (max-width:760px) { .sgi-body { grid-template-columns:1fr; } }
  .sgi-hero { border:1px solid var(--line); border-radius:22px; padding:22px 20px 18px;
    background:linear-gradient(180deg, rgba(255,255,255,.065), rgba(255,255,255,.018)); text-align:center;
    position:relative; overflow:hidden; }
  .sgi-hero::after { content:""; position:absolute; inset:auto -30% -55% -30%; height:70%;
    background:radial-gradient(closest-side, rgba(232,130,58,.20), transparent); pointer-events:none; }
  .sgi-stage { border-radius:26px; padding:20px; margin:0 auto 18px; width:min(100%, 300px);
    background:radial-gradient(110% 110% at 50% 0%, #211b15, #0e0c0a); transition:background .25s ease;
    display:flex; justify-content:center; }
  .sgi-stage.is-light { background:radial-gradient(110% 110% at 50% 0%, #ffffff, #e9e4da); }
  .sgi-stage img { width:220px; height:220px; border-radius:48px; display:block;
    box-shadow:0 24px 50px rgba(0,0,0,.48); }
  .sgi-num { color:var(--orange); font-size:11px; letter-spacing:.16em; font-weight:700; }
  .sgi-name { margin:6px 0 8px; font-size:21px; letter-spacing:.02em; }
  .sgi-note { margin:0 auto; max-width:290px; color:var(--muted); font-size:12.5px; line-height:1.65; min-height:40px; }
  .sgi-sizes { display:flex; align-items:flex-end; justify-content:center; gap:18px; margin-top:18px;
    padding-top:15px; border-top:1px solid var(--line); }
  .sgi-size { display:flex; flex-direction:column; align-items:center; gap:6px; color:#8f877b; font-size:10px; }
  .sgi-size .box { display:flex; align-items:flex-end; justify-content:center; height:48px; }
  .sgi-size img { display:block; border-radius:5px; }
  .sgi-size img.s48 { width:48px; height:48px; border-radius:11px; }
  .sgi-size img.s32 { width:32px; height:32px; border-radius:7px; }
  .sgi-size img.s16 { width:16px; height:16px; border-radius:4px; }
  .sgi-pick { margin-top:16px; width:100%; border:0; border-radius:12px; padding:12px 14px; cursor:pointer;
    background:linear-gradient(135deg, #f4a261, #d96f2a); color:#1b120a; font-weight:800; font-size:13px;
    font-family:inherit; letter-spacing:.03em; }
  .sgi-pick:hover { filter:brightness(1.06); }
  .sgi-grid { display:grid; grid-template-columns:repeat(3, minmax(0, 1fr)); gap:12px; }
  @media (max-width:900px) { .sgi-grid { grid-template-columns:repeat(2, minmax(0, 1fr)); } }
  .sgi-card { appearance:none; border:1px solid var(--line); background:rgba(255,255,255,.035);
    border-radius:16px; padding:12px 10px 11px; color:var(--paper); font-family:inherit; text-align:center;
    cursor:pointer; transition:border-color .18s ease, background .18s ease, transform .18s ease; position:relative; }
  .sgi-card:hover { border-color:rgba(232,130,58,.45); transform:translateY(-2px); }
  .sgi-card.is-active { border-color:var(--orange); background:rgba(232,130,58,.10); }
  .sgi-card.is-active::after { content:"已选"; position:absolute; top:8px; right:8px; font-size:10px;
    color:#1b120a; background:var(--orange); border-radius:999px; padding:2px 7px; font-weight:800; }
  .sgi-card img { width:112px; height:112px; border-radius:25px; display:block; margin:2px auto 10px;
    box-shadow:0 14px 28px rgba(0,0,0,.38); }
  .sgi-card-name { font-size:12.5px; font-weight:700; line-height:1.35; }
  .sgi-card-num { color:#81786c; font-size:10px; letter-spacing:.12em; margin-top:4px; }
  .sgi-taskbar { margin-top:12px; border:1px solid var(--line); border-radius:16px; padding:12px 14px;
    background:rgba(255,255,255,.035); display:flex; align-items:center; gap:14px; flex-wrap:wrap; }
  .sgi-taskbar-label { color:#8f877b; font-size:10.5px; letter-spacing:.06em; }
  .sgi-taskbar-icons { display:flex; align-items:center; gap:12px; flex-wrap:wrap; }
  .sgi-taskbar-icons img { width:32px; height:32px; border-radius:7px; display:block; }
  .sgi-taskbar-icons img:nth-child(2n) { width:16px; height:16px; border-radius:4px; opacity:.9; }
  .sgi-foot { margin-top:16px; color:#8f877b; font-size:11.5px; line-height:1.7; }
  .sgi-foot strong { color:#e8b184; font-weight:700; }
</style>
<div class="sgi" id="sg-icon-preview">
  <div class="sgi-head">
    <div>
      <p class="sgi-eyebrow">SHIGUANG · ICON REDESIGN</p>
      <h2 class="sgi-title">拾光 · 应用图标重塑</h2>
      <p class="sgi-sub">点击任意方案查看大图与实际小尺寸；当前只是预览，不会改动已安装的 1.0.3。</p>
    </div>
    <button class="sgi-toggle" id="sgi-bg-toggle" type="button">切换浅色底</button>
  </div>
  <div class="sgi-body">
    <section class="sgi-hero" aria-live="polite">
      <div class="sgi-stage" id="sgi-stage"><img id="sgi-hero-img" src="" alt=""/></div>
      <div class="sgi-num" id="sgi-hero-num"></div>
      <h3 class="sgi-name" id="sgi-hero-name"></h3>
      <p class="sgi-note" id="sgi-hero-note"></p>
      <div class="sgi-sizes">
        <div class="sgi-size"><div class="box"><img class="s48" id="sgi-hero-48" src="" alt=""/></div><span>48 px</span></div>
        <div class="sgi-size"><div class="box"><img class="s32" id="sgi-hero-32" src="" alt=""/></div><span>32 px</span></div>
        <div class="sgi-size"><div class="box"><img class="s16" id="sgi-hero-16" src="" alt=""/></div><span>16 px</span></div>
      </div>
      <button class="sgi-pick" id="sgi-pick" type="button">使用这个方案</button>
    </section>
    <section>
      <div class="sgi-grid" id="sgi-grid"></div>
      <div class="sgi-taskbar">
        <span class="sgi-taskbar-label">任务栏尺寸</span>
        <div class="sgi-taskbar-icons" id="sgi-taskbar"></div>
      </div>
      <p class="sgi-foot"><strong>建议：</strong>02 聚焦小尺寸最清晰，也最贴合以图搜物；01 提灯最温暖；06 标签最像失物招领。选定后回复编号即可。</p>
    </section>
  </div>
</div>
<script>
(() => {
  const data = ${JSON.stringify(data).replace(/</g, '\\u003c')};
  const root = document.getElementById('sg-icon-preview');
  if (!root || root.dataset.ready) return;
  root.dataset.ready = '1';
  const $ = (id) => root.querySelector('#' + id);
  const heroImg = $('sgi-hero-img'), heroNum = $('sgi-hero-num'), heroName = $('sgi-hero-name'), heroNote = $('sgi-hero-note');
  const hero48 = $('sgi-hero-48'), hero32 = $('sgi-hero-32'), hero16 = $('sgi-hero-16');
  const grid = $('sgi-grid'), taskbar = $('sgi-taskbar');
  let current = 1;

  const cards = data.map((item, index) => {
    const card = document.createElement('button');
    card.type = 'button';
    card.className = 'sgi-card';
    card.innerHTML = '<img src="' + item.thumb + '" alt="' + item.name + '"/>'
      + '<div class="sgi-card-name">' + item.name + '</div>'
      + '<div class="sgi-card-num">' + item.num + '</div>';
    card.addEventListener('click', () => select(index));
    grid.appendChild(card);
    return card;
  });

  data.forEach((item) => {
    const img = document.createElement('img');
    img.src = item.s32;
    img.alt = item.name;
    img.title = item.name;
    taskbar.appendChild(img);
    const small = document.createElement('img');
    small.src = item.s16;
    small.alt = item.name;
    small.title = item.name;
    taskbar.appendChild(small);
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

  $('sgi-bg-toggle').addEventListener('click', (event) => {
    const light = $('sgi-stage').classList.toggle('is-light');
    event.currentTarget.textContent = light ? '切换深色底' : '切换浅色底';
  });

  $('sgi-pick').addEventListener('click', () => {
    $('sgi-pick').textContent = '已选 ' + data[current].num + ' · 回复编号即可应用';
  });

  select(1);
})();
</script>`

const vizDir = process.env.QODER_VIZ_DIR
if (vizDir) {
  mkdirSync(vizDir, { recursive: true })
  writeFileSync(resolve(vizDir, 'icon-redesign-preview.html'), html)
  console.log('wrote', resolve(vizDir, 'icon-redesign-preview.html'))
} else {
  writeFileSync(resolve(outDir, 'preview.html'), html)
  console.log('wrote', resolve(outDir, 'preview.html'))
}
