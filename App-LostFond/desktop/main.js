const { app, BrowserWindow, Menu, dialog, shell, ipcMain, safeStorage, screen } = require('electron')
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
const LOGIN_PAGE = path.join(__dirname, 'login', 'index.html')
const LOGIN_PRELOAD = path.join(__dirname, 'preload', 'login.js')
const MAIN_PRELOAD = path.join(__dirname, 'preload', 'main.js')

const DATA_ROOT = path.join(app.getPath('userData'), 'data')
const UPLOAD_DIR = path.join(DATA_ROOT, 'uploads')
const LOG_DIR = path.join(app.getPath('userData'), 'logs')
const LOG_FILE = path.join(LOG_DIR, 'backend.log')
const SECRET_FILE = path.join(app.getPath('userData'), 'jwt.key')
const SESSION_FILE = path.join(app.getPath('userData'), 'session.bin')
const FIRST_RUN_FILE = path.join(app.getPath('userData'), 'first-run-done')
const DEMO_ADMIN = 'Admin@2026'
const DEMO_USER = 'Demo@2026'

let child = null
let loginWindow = null
let mainWindow = null
let stopping = false
let switching = false
let loginPending = false
let servicePort = 18080
let backendState = 'starting'
let session = null

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

function apiRequest(port, method, urlPath, { body, token, timeout = 30000 } = {}) {
  return new Promise((resolve) => {
    const payload = body ? Buffer.from(JSON.stringify(body)) : null
    const headers = { Accept: 'application/json' }
    if (payload) {
      headers['Content-Type'] = 'application/json'
      headers['Content-Length'] = payload.length
    }
    if (token) headers.Authorization = `Bearer ${token}`
    const req = http.request({ host: '127.0.0.1', port, path: urlPath, method, headers }, (res) => {
      let raw = ''
      res.setEncoding('utf8')
      res.on('data', (chunk) => { raw += chunk })
      res.on('end', () => {
        let data = null
        try { data = JSON.parse(raw) } catch (e) {}
        resolve({ status: res.statusCode || 0, body: data })
      })
    })
    req.setTimeout(timeout, () => req.destroy(new Error('timeout')))
    req.on('error', (e) => resolve({ status: 0, body: null, error: e.message }))
    if (payload) req.write(payload)
    req.end()
  })
}

function loadSessionFromDisk() {
  try {
    if (!fs.existsSync(SESSION_FILE) || !safeStorage.isEncryptionAvailable()) return null
    const data = JSON.parse(safeStorage.decryptString(fs.readFileSync(SESSION_FILE)))
    return data?.token && data?.user ? data : null
  } catch (e) {
    log(`read session failed: ${e.message}`)
    return null
  }
}

function saveSessionToDisk() {
  try {
    if (!session || !safeStorage.isEncryptionAvailable()) return
    fs.writeFileSync(SESSION_FILE, safeStorage.encryptString(JSON.stringify({ token: session.token, user: session.user })))
  } catch (e) {
    log(`save session failed: ${e.message}`)
  }
}

function clearSessionFile() {
  try {
    if (fs.existsSync(SESSION_FILE)) fs.unlinkSync(SESSION_FILE)
  } catch (e) {
    log(`clear session failed: ${e.message}`)
  }
}

async function restoreSession(port) {
  const stored = loadSessionFromDisk()
  if (!stored) return false
  const res = await apiRequest(port, 'GET', '/api/auth/me', { token: stored.token })
  if (res.status === 200 && res.body?.code === 200 && res.body.data) {
    session = { token: stored.token, user: res.body.data, rememberMe: true }
    saveSessionToDisk()
    return true
  }
  clearSessionFile()
  return false
}

function backendSnapshot() {
  return {
    state: backendState,
    port: backendState === 'ready' || backendState === 'restoring' ? servicePort : null,
    version: app.getVersion(),
  }
}

function sendBackendState() {
  if (loginWindow && !loginWindow.isDestroyed()) {
    loginWindow.webContents.send('backend:state', backendSnapshot())
  }
}

function setBackendState(state) {
  backendState = state
  sendBackendState()
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

function showServiceInfo(parent) {
  dialog.showMessageBox(parent || undefined, {
    type: 'info',
    title: '服务信息',
    message: `拾光服务运行中 · 端口 ${servicePort}`,
    detail: serviceInfo(),
    buttons: ['知道了'],
    noLink: true,
  })
}

function showAbout(parent) {
  dialog.showMessageBox(parent || undefined, {
    type: 'info',
    title: '关于拾光',
    message: `拾光 · 校园失物招领平台`,
    detail: [
      `Windows 桌面版 ${app.getVersion()}`,
      `Electron + 内置 Java 运行环境 + Spring Boot + H2`,
      ``,
      `数据保存在本机：${DATA_ROOT}`,
      `不需要安装 MySQL / JDK，也不需要云服务器。`,
    ].join('\n'),
    buttons: ['知道了'],
    noLink: true,
  })
}

function openAppMenu(win) {
  if (!win || win.isDestroyed()) return
  const menu = Menu.buildFromTemplate([
    { label: '服务信息 / 局域网地址', click: () => showServiceInfo(win) },
    { label: '打开数据目录', click: () => shell.openPath(DATA_ROOT) },
    { label: '打开日志', click: () => shell.openPath(LOG_DIR) },
    { type: 'separator' },
    { label: '退出登录', click: () => switchToLogin() },
    { label: '退出拾光', click: () => app.quit() },
  ])
  menu.popup({ window: win })
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

function windowSize(preferred, min, margin = 40) {
  const { width, height } = screen.getPrimaryDisplay().workAreaSize
  return {
    width: Math.max(min.width, Math.min(preferred.width, width - margin)),
    height: Math.max(min.height, Math.min(preferred.height, height - margin)),
  }
}

function createLoginWindow() {
  if (loginWindow && !loginWindow.isDestroyed()) {
    if (loginWindow.isMinimized()) loginWindow.restore()
    loginWindow.show()
    loginWindow.focus()
    return loginWindow
  }
  // 与 Qoder 登录页截图保持同一窗口尺寸（1042×673）
  const size = windowSize({ width: 1042, height: 673 }, { width: 860, height: 560 })
  loginWindow = new BrowserWindow({
    ...size,
    minWidth: 860,
    minHeight: 560,
    frame: false,
    show: false,
    center: true,
    backgroundColor: '#F6F4EF',
    icon: ICON,
    title: '拾光 · 登录',
    webPreferences: {
      preload: LOGIN_PRELOAD,
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
    },
  })
  loginWindow.loadFile(LOGIN_PAGE)
  loginWindow.once('ready-to-show', () => {
    if (loginWindow && !loginWindow.isDestroyed()) loginWindow.show()
  })
  loginWindow.on('closed', () => { loginWindow = null })
  loginWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https?:/i.test(url)) shell.openExternal(url)
    return { action: 'deny' }
  })
  loginWindow.webContents.on('will-navigate', (e) => e.preventDefault())
  loginWindow.webContents.on('did-finish-load', sendBackendState)
  loginWindow.webContents.on('did-fail-load', (e, code, desc) => {
    log(`login page load failed: ${code} ${desc}`)
  })
  return loginWindow
}

function createMainWindow(port) {
  if (mainWindow && !mainWindow.isDestroyed()) {
    if (mainWindow.isMinimized()) mainWindow.restore()
    mainWindow.show()
    mainWindow.focus()
    return mainWindow
  }
  const size = windowSize({ width: 1360, height: 900 }, { width: 1100, height: 720 })
  mainWindow = new BrowserWindow({
    ...size,
    minWidth: 1100,
    minHeight: 720,
    frame: false,
    show: false,
    center: true,
    backgroundColor: '#FAFAF8',
    icon: ICON,
    title: '拾光 · 校园失物招领平台',
    webPreferences: {
      preload: MAIN_PRELOAD,
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
    },
  })
  mainWindow.loadURL(`http://127.0.0.1:${port}/#/home`)
  mainWindow.once('ready-to-show', () => {
    if (!mainWindow || mainWindow.isDestroyed()) return
    mainWindow.show()
    if (loginWindow && !loginWindow.isDestroyed()) loginWindow.close()
    switching = false
    if (!fs.existsSync(FIRST_RUN_FILE)) {
      fs.writeFileSync(FIRST_RUN_FILE, new Date().toISOString())
      setTimeout(() => showServiceInfo(mainWindow), 1200)
    }
  })
  mainWindow.on('maximize', () => {
    if (mainWindow && !mainWindow.isDestroyed()) mainWindow.webContents.send('window:maximized', true)
  })
  mainWindow.on('unmaximize', () => {
    if (mainWindow && !mainWindow.isDestroyed()) mainWindow.webContents.send('window:maximized', false)
  })
  mainWindow.on('closed', () => { mainWindow = null })
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
  log(`main window ready on port ${port}`)
  return mainWindow
}

function switchToLogin() {
  if (switching) return
  switching = true
  session = null
  clearSessionFile()
  backendState = 'ready'
  const currentMain = mainWindow
  if (!loginWindow || loginWindow.isDestroyed()) createLoginWindow()
  else {
    if (loginWindow.isMinimized()) loginWindow.restore()
    loginWindow.show()
    loginWindow.focus()
    sendBackendState()
  }
  if (currentMain && !currentMain.isDestroyed()) {
    currentMain.webContents.executeJavaScript(
      "try{localStorage.removeItem('shiguang_token');localStorage.removeItem('shiguang_user')}catch(e){}"
    ).catch(() => {})
    setTimeout(() => {
      if (currentMain && !currentMain.isDestroyed()) currentMain.close()
      switching = false
    }, 120)
  } else {
    switching = false
  }
}

async function doLogin(payload) {
  if (backendState !== 'ready') return { ok: false, message: '本地服务正在启动，请稍候…' }
  if (loginPending) return { ok: false, message: '正在登录，请稍候…' }
  if (mainWindow && !mainWindow.isDestroyed()) return { ok: false, message: '已登录' }
  const account = String(payload?.account || '').trim()
  const password = String(payload?.password || '')
  const rememberMe = payload?.rememberMe !== false
  if (!/^\d{6,12}$/.test(account)) return { ok: false, message: '请输入 6–12 位学号' }
  if (password.length < 6) return { ok: false, message: '密码至少 6 位' }
  loginPending = true
  try {
    const res = await apiRequest(servicePort, 'POST', '/api/auth/login', {
      body: { account, password, rememberMe },
    })
    if (res.status === 200 && res.body?.code === 200 && res.body.data?.token) {
      session = { token: res.body.data.token, user: res.body.data.user, rememberMe }
      if (rememberMe) saveSessionToDisk()
      else clearSessionFile()
      log(`login ok: ${account}`)
      switching = true
      setTimeout(() => {
        if (!mainWindow || mainWindow.isDestroyed()) createMainWindow(servicePort)
      }, 350)
      return { ok: true, user: session.user }
    }
    const message = res.body?.msg
      || (res.status === 0 ? '无法连接本地服务，请稍候重试' : '登录失败，请重试')
    log(`login failed: ${account} ${res.status} ${message}`)
    return { ok: false, message }
  } catch (e) {
    log(`login error: ${e.message}`)
    return { ok: false, message: '登录失败，请重试' }
  } finally {
    loginPending = false
  }
}

ipcMain.on('session:get', (event) => {
  event.returnValue = session
    ? { token: session.token, user: session.user }
    : null
})
ipcMain.handle('backend:get-state', () => backendSnapshot())
ipcMain.handle('auth:login', (event, payload) => doLogin(payload))
ipcMain.handle('window:is-maximized', (event) => {
  const win = BrowserWindow.fromWebContents(event.sender)
  return !!win && win.isMaximized()
})
ipcMain.on('window:minimize', (event) => {
  const win = BrowserWindow.fromWebContents(event.sender)
  if (win) win.minimize()
})
ipcMain.on('window:toggle-maximize', (event) => {
  const win = BrowserWindow.fromWebContents(event.sender)
  if (!win) return
  if (win.isMaximized()) win.unmaximize()
  else win.maximize()
})
ipcMain.on('window:close', (event) => {
  const win = BrowserWindow.fromWebContents(event.sender)
  if (win) win.close()
})
ipcMain.on('desktop:logout', () => switchToLogin())
ipcMain.on('desktop:service-info', (event) => {
  showServiceInfo(BrowserWindow.fromWebContents(event.sender))
})
ipcMain.on('desktop:about', (event) => {
  showAbout(BrowserWindow.fromWebContents(event.sender))
})
ipcMain.on('desktop:open-app-menu', (event) => {
  openAppMenu(BrowserWindow.fromWebContents(event.sender))
})

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
  const win = mainWindow || loginWindow
  if (win && !win.isDestroyed()) {
    if (win.isMinimized()) win.restore()
    win.show()
    win.focus()
  }
})

app.whenReady().then(async () => {
  Menu.setApplicationMenu(null)
  createLoginWindow()
  try {
    const port = await startBackend()
    backendState = 'restoring'
    sendBackendState()
    const restored = await restoreSession(port)
    if (restored) {
      backendState = 'ready'
      sendBackendState()
      switching = true
      createMainWindow(port)
      log(`session restored, app ready on port ${port}`)
    } else {
      backendState = 'ready'
      sendBackendState()
      log(`login window ready on port ${port}`)
    }
  } catch (e) {
    log(`startup failed: ${e.message}`)
    backendState = 'error'
    sendBackendState()
    if (loginWindow && !loginWindow.isDestroyed()) loginWindow.close()
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
  if (!switching) app.quit()
})
