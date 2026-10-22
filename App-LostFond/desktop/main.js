const { app, BrowserWindow, Menu, dialog, shell } = require('electron')
const path = require('path')
const fs = require('fs')
const os = require('os')
const net = require('net')
const http = require('http')
const crypto = require('crypto')
const { spawn } = require('child_process')

app.setName('Shiguang')
app.setAppUserModelId('com.shiguang.lostfound')

const gotLock = app.requestSingleInstanceLock()
if (!gotLock) {
  app.quit()
  return
}

const JAR = app.isPackaged
  ? path.join(process.resourcesPath, 'backend', 'lost-found-backend.jar')
  : path.resolve(__dirname, '..', 'backend', 'target', 'lost-found-backend.jar')
const JAVA = app.isPackaged
  ? path.join(process.resourcesPath, 'runtime', 'bin', 'java.exe')
  : path.resolve(__dirname, 'runtime', 'bin', 'java.exe')
const DIST = app.isPackaged
  ? path.join(process.resourcesPath, 'frontend-dist')
  : path.resolve(__dirname, '..', 'frontend', 'dist')
const SEED = app.isPackaged
  ? path.join(process.resourcesPath, 'seed')
  : path.resolve(__dirname, '..', 'backend', 'uploads', 'seed')
const ICON = path.join(__dirname, 'build', 'icon.png')

const DATA_ROOT = path.join(app.getPath('userData'), 'data')
const UPLOAD_DIR = path.join(DATA_ROOT, 'uploads')
const LOG_DIR = path.join(app.getPath('userData'), 'logs')
const LOG_FILE = path.join(LOG_DIR, 'backend.log')
const SECRET_FILE = path.join(app.getPath('userData'), 'jwt.key')
const DEMO_ADMIN = 'Admin@2026'
const DEMO_USER = 'Demo@2026'

let child = null
let mainWindow = null
let splash = null
let stopping = false
let servicePort = 18080

function log(line) {
  const text = `[${new Date().toISOString()}] ${line}\n`
  try {
    fs.mkdirSync(LOG_DIR, { recursive: true })
    fs.appendFileSync(LOG_FILE, text)
  } catch (e) {}
  if (!app.isPackaged) process.stdout.write(text)
}

function ensureRuntimeFiles() {
  fs.mkdirSync(UPLOAD_DIR, { recursive: true })
  fs.mkdirSync(LOG_DIR, { recursive: true })
  const seedDest = path.join(UPLOAD_DIR, 'seed')
  if (!fs.existsSync(seedDest) && fs.existsSync(SEED)) {
    fs.cpSync(SEED, seedDest, { recursive: true })
    log(`seeded uploads -> ${seedDest}`)
  }
  if (!fs.existsSync(SECRET_FILE)) {
    fs.writeFileSync(SECRET_FILE, crypto.randomBytes(48).toString('base64'), 'utf8')
  }
  return fs.readFileSync(SECRET_FILE, 'utf8').trim()
}

function isPortFree(port) {
  return new Promise((resolve) => {
    const srv = net.createServer()
    srv.once('error', () => resolve(false))
    srv.once('listening', () => srv.close(() => resolve(true)))
    srv.listen(port, '0.0.0.0')
  })
}

async function pickPort(start = 18080) {
  for (let p = start; p < start + 20; p++) {
    if (await isPortFree(p)) return p
  }
  throw new Error('18080-18099 端口都被占用')
}

function startBackend() {
  const secret = ensureRuntimeFiles()
  return pickPort().then((port) => {
    servicePort = port
    const args = [
      '-Xms64m', '-Xmx512m', '-Dfile.encoding=UTF-8',
      '-jar', JAR,
      '--spring.profiles.active=app',
    ]
    log(`starting backend: ${JAVA} ${args.join(' ')}`)
    child = spawn(JAVA, args, {
      cwd: path.dirname(JAR),
      windowsHide: true,
      env: {
        ...process.env,
        APP_DATA_DIR: DATA_ROOT.replace(/\\/g, '/'),
        SERVER_PORT: String(port),
        SERVER_ADDRESS: '0.0.0.0',
        FRONTEND_DIST_DIR: DIST,
        JWT_SECRET: secret,
        SEED_ADMIN_PASSWORD: DEMO_ADMIN,
        SEED_USER_PASSWORD: DEMO_USER,
        SPRING_PROFILES_ACTIVE: 'app',
      },
    })
    child.stdout.on('data', (d) => log(`[backend] ${d.toString().trimEnd()}`))
    child.stderr.on('data', (d) => log(`[backend:err] ${d.toString().trimEnd()}`))
    child.on('exit', (code) => {
      log(`backend exited with code ${code}`)
      child = null
      if (!stopping) {
        dialog.showErrorBox(
          '拾光服务已停止',
          `后端进程退出（code=${code}）。\n日志：${LOG_FILE}`
        )
        app.quit()
      }
    })
    return waitForServer(port)
  })
}

function waitForServer(port, timeoutMs = 90000) {
  const deadline = Date.now() + timeoutMs
  return new Promise((resolve, reject) => {
    const tick = () => {
      if (stopping) return reject(new Error('stopped'))
      if (child && child.exitCode !== null) return reject(new Error('backend exited'))
      if (Date.now() > deadline) return reject(new Error('backend timeout'))
      const req = http.get(`http://127.0.0.1:${port}/api/stats`, (res) => {
        res.resume()
        if (res.statusCode === 200) resolve(port)
        else setTimeout(tick, 400)
      })
      req.on('error', () => setTimeout(tick, 400))
      req.setTimeout(3000, () => req.destroy())
    }
    tick()
  })
}

function splashHtml() {
  return `<!doctype html><html><head><meta charset="utf-8"><style>
    *{box-sizing:border-box}html,body{height:100%;margin:0}
    body{display:grid;place-items:center;background:#161614;color:#FAFAF8;
      font-family:"Microsoft YaHei","PingFang SC",system-ui,sans-serif}
    .wrap{text-align:center}.mark{width:86px;height:86px;margin:0 auto 22px;border-radius:24px;
      background:#FAFAF8;color:#161614;display:grid;place-items:center;font-size:44px;font-weight:700}
    h1{margin:0;font-size:22px;font-weight:600;letter-spacing:.08em}
    p{margin:10px 0 0;font-size:12px;color:#9A9A93;letter-spacing:.06em}
    .bar{width:180px;height:2px;margin:26px auto 0;background:#2C2C29;overflow:hidden}
    .bar i{display:block;width:60px;height:100%;background:#E8823A;animation:run 1.1s infinite ease-in-out}
    @keyframes run{0%{transform:translateX(-60px)}100%{transform:translateX(180px)}}
  </style></head><body><div class="wrap"><div class="mark">拾</div>
  <h1>拾光</h1><p>正在启动本地服务…</p><div class="bar"><i></i></div></div></body></html>`
}

function openSplash() {
  splash = new BrowserWindow({
    width: 420, height: 300, frame: false, resizable: false,
    alwaysOnTop: true, backgroundColor: '#161614',
    webPreferences: { contextIsolation: true, nodeIntegration: false, sandbox: true },
  })
  splash.loadURL('data:text/html;charset=utf-8,' + encodeURIComponent(splashHtml()))
  splash.on('closed', () => { splash = null })
}

function createWindow(port) {
  mainWindow = new BrowserWindow({
    width: 1280, height: 860, minWidth: 980, minHeight: 680,
    show: false, backgroundColor: '#FAFAF8', icon: ICON,
    title: '拾光 · 校园失物招领平台',
    webPreferences: { contextIsolation: true, nodeIntegration: false, sandbox: true },
  })
  mainWindow.loadURL(`http://127.0.0.1:${port}/`)
  mainWindow.once('ready-to-show', () => {
    if (splash) splash.close()
    mainWindow.show()
  })
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https?:/i.test(url)) shell.openExternal(url)
    return { action: 'deny' }
  })
  mainWindow.webContents.on('will-navigate', (e, url) => {
    if (!url.startsWith(`http://127.0.0.1:${port}`)) {
      e.preventDefault()
      shell.openExternal(url)
    }
  })
  mainWindow.webContents.on('did-fail-load', (e, code, desc) => {
    log(`page load failed: ${code} ${desc}`)
  })
  mainWindow.on('closed', () => { mainWindow = null })
}

function lanAddresses() {
  const out = []
  for (const [name, list] of Object.entries(os.networkInterfaces())) {
    for (const item of list || []) {
      if (item.family === 'IPv4' && !item.internal) {
        out.push({ name, address: item.address })
      }
    }
  }
  return out
}

function serviceInfo() {
  const addrs = lanAddresses()
  const same = addrs.length
    ? addrs.map((a) => `  http://${a.address}:${servicePort}  (${a.name})`).join('\n')
    : '  未检测到可用的局域网地址'
  return [
    `同一局域网内的其他设备可用浏览器访问：`,
    ``,
    same,
    ``,
    `本机访问：http://127.0.0.1:${servicePort}/`,
    ``,
    `演示账号：`,
    `  管理员  10000001 / ${DEMO_ADMIN}`,
    `  普通用户 20240001 ~ 20240020 / ${DEMO_USER}`,
    ``,
    `数据目录：${DATA_ROOT}`,
    `日志：${LOG_FILE}`,
  ].join('\n')
}

function showServiceInfo() {
  dialog.showMessageBox(mainWindow || undefined, {
    type: 'info',
    title: '服务信息',
    message: `拾光服务运行中 · 端口 ${servicePort}`,
    detail: serviceInfo(),
    buttons: ['知道了'],
    noLink: true,
  })
}

function buildMenu() {
  const template = [
    {
      label: '应用',
      submenu: [
        { label: '服务信息 / 局域网地址', click: showServiceInfo },
        { label: '打开数据目录', click: () => shell.openPath(DATA_ROOT) },
        { type: 'separator' },
        { label: '退出', role: 'quit' },
      ],
    },
    {
      label: '编辑',
      submenu: [
        { label: '撤销', role: 'undo' },
        { label: '重做', role: 'redo' },
        { type: 'separator' },
        { label: '剪切', role: 'cut' },
        { label: '复制', role: 'copy' },
        { label: '粘贴', role: 'paste' },
        { label: '全选', role: 'selectAll' },
      ],
    },
    {
      label: '视图',
      submenu: [
        { label: '重新加载', role: 'reload' },
        { label: '开发者工具', role: 'toggleDevTools' },
        { type: 'separator' },
        { label: '实际大小', role: 'resetZoom' },
        { label: '放大', role: 'zoomIn' },
        { label: '缩小', role: 'zoomOut' },
        { type: 'separator' },
        { label: '全屏', role: 'togglefullscreen' },
      ],
    },
    {
      label: '帮助',
      submenu: [
        {
          label: '使用说明',
          click: () => dialog.showMessageBox(mainWindow || undefined, {
            type: 'info',
            title: '拾光 Windows 版',
            message: '拾光 · 校园失物招领平台',
            detail: '内置后端与 H2 数据库，无需安装 MySQL / Java。\n首次启动会播种演示数据，约需 10 秒。\n同一局域网内的其他设备可用浏览器访问「应用 → 服务信息」中的地址。',
            buttons: ['知道了'],
            noLink: true,
          }),
        },
        { label: '打开日志', click: () => shell.openPath(LOG_DIR) },
      ],
    },
  ]
  Menu.setApplicationMenu(Menu.buildFromTemplate(template))
}

function shutdownBackend() {
  if (!child) return
  const pid = child.pid
  child = null
  try {
    spawn('taskkill', ['/PID', String(pid), '/T', '/F'], { windowsHide: true })
  } catch (e) {
    log(`taskkill failed: ${e.message}`)
  }
}

app.on('second-instance', () => {
  if (mainWindow) {
    if (mainWindow.isMinimized()) mainWindow.restore()
    mainWindow.focus()
  }
})

app.whenReady().then(async () => {
  buildMenu()
  openSplash()
  try {
    const port = await startBackend()
    createWindow(port)
    log(`app ready on port ${port}`)
    const flag = path.join(app.getPath('userData'), 'first-run-done')
    if (!fs.existsSync(flag)) {
      fs.writeFileSync(flag, new Date().toISOString())
      setTimeout(showServiceInfo, 1200)
    }
  } catch (e) {
    log(`startup failed: ${e.message}`)
    if (splash) splash.close()
    dialog.showErrorBox(
      '拾光启动失败',
      `${e.message}\n\n请查看日志：${LOG_FILE}`
    )
    app.quit()
  }
})

app.on('before-quit', () => {
  stopping = true
  shutdownBackend()
})

app.on('window-all-closed', () => {
  app.quit()
})
