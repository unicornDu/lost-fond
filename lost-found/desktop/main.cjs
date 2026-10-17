/**
 * 拾光 · 校园失物招领平台 桌面壳（Electron 主进程）。
 *
 * 设计原则（照搬 Prompt/落地项目实现/ZCode-Desktop 文档，裁剪到课程项目体量）：
 *  - 单实例锁 requestSingleInstanceLock：深链接/二次启动唤起已有窗口
 *  - 深链接 lostfound://item/<id> 一键拉起指定启事
 *  - 程序与数据分离：壳不写任何业务数据；后端地址可经 userData/config.json 覆盖
 *  - 外部链接一律交系统浏览器（setWindowOpenHandler + will-navigate 守卫）
 *  - 不做自更新（课程项目明确取舍，见《失物招领平台-桌面端完整实施方案.md》P2/P3）
 *
 * P2 桌面联动增强：
 *  - IPC 原生打开/定位交付文件（dsh present.open 的 open/reveal 转译）：
 *    服务端 /api/file/open 按 deliveryId 读回路径 → 壳做 realpath 双保险校验 → shell.openPath
 *  - 托盘驻留：关窗隐藏到托盘，菜单"退出"真退出（退出时随杀自动拉起的 jar）
 *  - 窗口状态记忆：位置/尺寸持久化 userData/window-state.json（自写约 20 行，不引依赖）
 *
 * 运行模式：
 *  - 开发：ELECTRON_RENDERER_URL=http://localhost:5173 electron .   （API 走 Vite 代理）
 *  - 打包：loadFile renderer/index.html?apiBase=<后端地址>，前端以 hash 路由 + 绝对地址运行
 */
const { app, BrowserWindow, Menu, Tray, shell, ipcMain, dialog, nativeImage } = require('electron');
const path = require('node:path');
const fs = require('node:fs');
const http = require('node:http');

const DEV_URL = process.env.ELECTRON_RENDERER_URL;               // 开发模式由启动方注入
const PROTOCOL = 'lostfound';

// ---------- 配置优先级（高→低）：
//   userData/config.json（用户手改） > 环境变量 > 包内 default-config.json（发行版打包时烙入）
//   > localhost:8080（开发默认）
// 发行流程：编辑 desktop/default-config.json 的 apiBase 为公网/实验室服务器地址 → npm run dist，
// 产出的 Setup.exe 装到任何机器即"开箱即连"，数据全员同步（同一后端同一库）。
let cfg = { apiBase: process.env.SHIGUANG_API_BASE || 'http://localhost:8080', backendJar: process.env.SHIGUANG_BACKEND_JAR || '' };
let child = null;   // 可选：自动拉起的后端进程
let quitting = false;
let win = null;
let tray = null;

function loadBundledDefault() {
  try {
    const p = path.join(__dirname, 'default-config.json');
    if (fs.existsSync(p)) {
      const d = JSON.parse(fs.readFileSync(p, 'utf-8'));
      if (d.apiBase) cfg.apiBase = String(d.apiBase).replace(/\/+$/, '');
      if (d.backendJar) cfg.backendJar = String(d.backendJar);
    }
  } catch { /* 包内无发行配置属正常 */ }
}

function loadConfig() {
  loadBundledDefault();                          // ③ 包内发行默认（最低优先）
  if (process.env.SHIGUANG_API_BASE) cfg.apiBase = String(process.env.SHIGUANG_API_BASE).replace(/\/+$/, '');
  if (process.env.SHIGUANG_BACKEND_JAR) cfg.backendJar = String(process.env.SHIGUANG_BACKEND_JAR);
  try {
    const p = path.join(app.getPath('userData'), 'config.json');   // ① 用户手改，最高优先
    const user = JSON.parse(fs.readFileSync(p, 'utf-8'));
    if (user.apiBase) cfg.apiBase = String(user.apiBase).replace(/\/+$/, '');
    if (user.backendJar) cfg.backendJar = String(user.backendJar);
  } catch { /* 无用户配置属正常 */ }
}

function health(base, timeoutMs = 2500) {
  return new Promise((resolve) => {
    try {
      // 发行 apiBase 可能是 https（穿透/云服务器）——按协议选模块，异常一律按不可达
      const mod = base.startsWith('https:') ? require('node:https') : http;
      const req = mod.get(base + '/api/stats', { timeout: timeoutMs }, (res) => resolve(res.statusCode === 200));
      req.on('error', () => resolve(false));
      req.on('timeout', () => { req.destroy(); resolve(false); });
    } catch { resolve(false); }
  });
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

/** 可选能力：配置了 backendJar 且后端未启动时，自动拉起（"双击即用"；MySQL 需已在本机运行）。 */
async function ensureBackend() {
  if (DEV_URL) return true;                       // 开发模式走 Vite 代理，不干预
  if (await health(cfg.apiBase)) return true;
  if (!cfg.backendJar || !fs.existsSync(cfg.backendJar)) return false;
  const { spawn } = require('node:child_process');
  child = spawn('java', ['-jar', cfg.backendJar], { cwd: path.dirname(cfg.backendJar), detached: false, stdio: 'ignore' });
  child.on('exit', () => { child = null; });
  for (let i = 0; i < 20; i++) {                  // 最多等 40s（Spring Boot 冷启动）
    if (await health(cfg.apiBase)) return true;
    await sleep(2000);
  }
  return false;
}

// ---------- 窗口状态记忆（userData/window-state.json，程序与数据分离：只存壳自身状态） ----------
function stateFile() { return path.join(app.getPath('userData'), 'window-state.json'); }
function loadWindowState() {
  try {
    const s = JSON.parse(fs.readFileSync(stateFile(), 'utf-8'));
    // 简单防呆：宽高必须落在屏幕范围内才采用
    const { screen } = require('electron');
    const area = screen.getPrimaryDisplay().workAreaSize;
    if (s.width >= 800 && s.height >= 600 && s.width <= area.width + 80 && s.height <= area.height + 80) return s;
  } catch { /* 首次启动无文件 */ }
  return null;
}
let saveTimer = null;
function scheduleSaveState() {
  if (!win || win.isDestroyed() || win.isMinimized()) return;
  clearTimeout(saveTimer);
  saveTimer = setTimeout(() => {
    try { fs.writeFileSync(stateFile(), JSON.stringify(win.getBounds())); } catch { }
  }, 600);
}

function createWindow() {
  const saved = loadWindowState();
  win = new BrowserWindow({
    width: saved?.width || 1280,
    height: saved?.height || 820,
    x: saved?.x, y: saved?.y,
    minWidth: 960,
    minHeight: 640,
    title: '拾光 · 校园失物招领平台',
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });
  Menu.setApplicationMenu(null);   // 去掉默认菜单，更像桌面应用；F12 开发者工具在 dev 模式仍可用

  // 外部链接（新窗口/导航到 http(s)）一律交系统浏览器，壳内只跑本应用
  const isOurOrigin = (u) => {
    try { return new URL(u).origin === new URL(cfg.apiBase).origin; } catch { return false; }
  };
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https?:/i.test(url)) shell.openExternal(url);
    return { action: 'deny' };
  });
  win.webContents.on('will-navigate', (e, url) => {
    if (DEV_URL && url.startsWith(DEV_URL)) return;
    if (url.startsWith('file:')) return;          // 打包模式内部路由
    if (isOurOrigin(url)) return;                 // 后端同源（如受控下载）
    e.preventDefault();
    if (/^https?:/i.test(url)) shell.openExternal(url);
  });

  // 托盘驻留：关窗=隐藏（防误关），托盘菜单"退出"才真退出
  win.on('close', (e) => {
    if (!quitting) { e.preventDefault(); win.hide(); }
  });
  ['resize', 'move'].forEach((ev) => win.on(ev, scheduleSaveState));

  if (DEV_URL) {
    win.loadURL(DEV_URL);
    win.webContents.openDevTools({ mode: 'detach' });
  } else {
    // apiBase 经 query 注入 renderer（见 frontend/src/utils/desktop.js）
    win.loadFile(path.join(__dirname, 'renderer/index.html'), {
      search: 'apiBase=' + encodeURIComponent(cfg.apiBase),
    });
  }
  win.on('closed', () => { win = null; });
}

// ---------- 托盘 ----------
function createTray() {
  const iconPath = path.join(__dirname, 'build/icon.png');
  tray = new Tray(fs.existsSync(iconPath) ? nativeImage.createFromPath(iconPath) : nativeImage.createEmpty());
  tray.setToolTip('拾光 · 校园失物招领平台');
  tray.setContextMenu(Menu.buildFromTemplate([
    { label: '打开主窗口', click: () => { if (win && !win.isDestroyed()) { win.show(); win.focus(); } } },
    { type: 'separator' },
    { label: '退出', click: () => { quitting = true; app.quit(); } },
  ]));
  tray.on('double-click', () => { if (win && !win.isDestroyed()) { win.show(); win.focus(); } });
}

// ---------- P2：交付文件原生打开/定位（dsh present.open 转译） ----------
/**
 * 双保险校验：后端 /api/file/open 已按 deliveryId 读回路径并做过 uploads 根前缀校验；
 * 壳侧不信任渲染层传来的字符串，再独立验证：
 *  ① 绝对路径 ② realpath 存在且是普通文件 ③ 路径含 /uploads/ 段
 *  ④ 若配置了 backendJar，realpath 必须位于其后端目录内（更强约束）
 */
function pathSafeToOpen(p) {
  if (typeof p !== 'string' || !p || !path.isAbsolute(p)) return false;
  if (!p.replaceAll('\\', '/').includes('/uploads/')) return false;
  try {
    const real = fs.realpathSync(p);
    if (!fs.statSync(real).isFile()) return false;
    if (cfg.backendJar) {
      const backendDir = fs.realpathSync(path.dirname(cfg.backendJar));
      if (!real.startsWith(backendDir)) return false;
    }
    return true;
  } catch { return false; }
}

function registerIpc() {
  ipcMain.handle('shiguang:get-config', () => ({ isDesktop: true, apiBase: cfg.apiBase, platform: process.platform }));
  ipcMain.handle('shiguang:open-file', async (_e, p) => {
    if (!pathSafeToOpen(p)) return '路径校验未通过，已拒绝打开';
    return shell.openPath(p);   // 成功返回 ''，失败返回系统错误文案
  });
  ipcMain.handle('shiguang:reveal-file', (_e, p) => {
    if (!pathSafeToOpen(p)) return false;
    shell.showItemInFolder(p);
    return true;
  });
}

// ---------- 深链接 lostfound://item/<id> ----------
function handleDeepLink(rawUrl) {
  if (!rawUrl || !rawUrl.startsWith(PROTOCOL + '://')) return;
  const m = rawUrl.match(/^lostfound:\/\/item\/(\d+)/i);
  if (m && win && !win.isDestroyed()) {
    win.show(); win.focus();
    win.webContents.send('shiguang:open-item', m[1]);
  } else if (win) {
    win.show(); win.focus();
  }
}

// ---------- 单实例锁（ZCode 1.4 同款） ----------
const gotLock = app.requestSingleInstanceLock();
if (!gotLock) {
  app.quit();
} else {
  app.on('second-instance', (_e, argv) => {
    const url = argv.find((a) => String(a).startsWith(PROTOCOL + '://'));
    if (url) handleDeepLink(url);
    else if (win) { win.show(); win.focus(); }
  });

  app.whenReady().then(async () => {
    loadConfig();
    registerIpc();
    // 深链接注册：开发模式跑 electron.exe 需带自身路径与 --（ZCode 1.3 的做法）
    if (process.defaultApp) {
      app.setAsDefaultProtocolClient(PROTOCOL, process.execPath, [path.resolve(process.argv[1])]);
    } else {
      app.setAsDefaultProtocolClient(PROTOCOL);
    }

    const ok = await ensureBackend();
    if (!ok) {
      const r = await dialog.showMessageBox({
        type: 'warning',
        title: '后端未启动',
        message: `未检测到后端服务（${cfg.apiBase}）。\n\n` +
          '请先启动后端：java -jar backend/target/lost-found-backend.jar（需本机 MySQL 已运行）。\n' +
          '或在桌面端配置文件 userData/config.json 中填写 apiBase / backendJar 以支持自动拉起。',
        buttons: ['仍然打开', '退出'],
        defaultId: 0,
      });
      if (r.response === 1) { app.quit(); return; }
    }
    createWindow();
    createTray();
  });

  app.on('before-quit', () => {
    quitting = true;
    if (child) child.kill();      // 壳拉起的后端随壳退出，不留孤儿进程
  });

  // 托盘驻留模式下窗口只会 hide；真正 quit 时 window-all-closed 才会触发
  app.on('window-all-closed', () => { app.quit(); });
}
