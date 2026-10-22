const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('shiguangLogin', {
  isDesktop: true,
  getState: () => ipcRenderer.invoke('backend:get-state'),
  onState: (callback) => {
    const listener = (_event, value) => callback(value)
    ipcRenderer.on('backend:state', listener)
    return () => ipcRenderer.removeListener('backend:state', listener)
  },
  login: (payload) => ipcRenderer.invoke('auth:login', payload),
  minimize: () => ipcRenderer.send('window:minimize'),
  close: () => ipcRenderer.send('window:close'),
  showServiceInfo: () => ipcRenderer.send('desktop:service-info'),
})
