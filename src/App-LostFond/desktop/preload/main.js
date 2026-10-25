const { contextBridge, ipcRenderer } = require('electron')

try {
  const session = ipcRenderer.sendSync('session:get')
  if (session?.token && session?.user) {
    localStorage.setItem('shiguang_token', session.token)
    localStorage.setItem('shiguang_user', JSON.stringify(session.user))
  } else {
    localStorage.removeItem('shiguang_token')
    localStorage.removeItem('shiguang_user')
  }
} catch (e) {
  // 登录态注入失败时由页面 401 处理，不阻塞窗口加载
}

contextBridge.exposeInMainWorld('shiguangDesktop', {
  isDesktop: true,
  platform: process.platform,
  minimize: () => ipcRenderer.send('window:minimize'),
  toggleMaximize: () => ipcRenderer.send('window:toggle-maximize'),
  close: () => ipcRenderer.send('window:close'),
  isMaximized: () => ipcRenderer.invoke('window:is-maximized'),
  onMaximized: (callback) => {
    const listener = (_event, value) => callback(!!value)
    ipcRenderer.on('window:maximized', listener)
    return () => ipcRenderer.removeListener('window:maximized', listener)
  },
  logout: () => ipcRenderer.send('desktop:logout'),
  openAppMenu: () => ipcRenderer.send('desktop:open-app-menu'),
  showAbout: () => ipcRenderer.send('desktop:about'),
})
