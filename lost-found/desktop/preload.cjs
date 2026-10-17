/**
 * 拾光桌面壳 preload：contextIsolation 下经 contextBridge 白名单暴露最小能力。
 * 不暴露任何 Node 能力（nodeIntegration=false），只给三类只读/受控面：
 *  1) 只读配置（是否桌面端/后端地址/平台）
 *  2) 深链接 open-item 事件订阅（lostfound://item/<id> → 前端路由跳转）
 *  3) 交付文件原生打开/定位（P2：主进程内有 realpath 双保险，路径不落渲染层以外的用途）
 */
const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('shiguangDesktop', {
  isDesktop: true,
  getConfig: () => ipcRenderer.invoke('shiguang:get-config'),
  onOpenItem: (cb) => {
    const handler = (_e, itemId) => cb(itemId);
    ipcRenderer.on('shiguang:open-item', handler);
    return () => ipcRenderer.removeListener('shiguang:open-item', handler);
  },
  /** P2：用系统默认程序打开文件（dsh present.open 的 open 动作）；成功返回 ''，失败返回错误文案 */
  openPath: (p) => ipcRenderer.invoke('shiguang:open-file', p),
  /** P2：在文件管理器中显示文件（dsh present.open 的 reveal 动作） */
  revealPath: (p) => ipcRenderer.invoke('shiguang:reveal-file', p),
});
