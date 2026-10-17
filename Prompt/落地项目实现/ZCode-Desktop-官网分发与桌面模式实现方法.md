# ZCode Desktop：官网 exe 分发与桌面模式实现方法

> 学习来源：本机安装的 ZCode Desktop 3.15.3（`D:\Visual_Stdio_Code\AI\Zcode\`，
> Electron 应用，主包 `@zcode/desktop`，app.asar 340MB）。
> 通过解析 asar 目录、抽取主进程/host 进程代码静态归纳。
> 与《DeepSeek-Harness-文件交付与桌面联动实现方法.md》互补：dsh 是"本地 Web 服务器"路线，ZCode 是"Electron 桌面壳"路线。

---

## 1. 官网下载 .exe → 用户桌面可用：分发链路

整条链就是 **electron-builder + electron-updater** 的标准方案，ZCode 没有自造轮子，而是把每一步都做扎实。

### 1.1 安装包：NSIS 单文件安装器

本机程序目录的证据（`D:\Visual_Stdio_Code\AI\Zcode\`）：

```
ZCode.exe                    ← Electron 主程序
Uninstall ZCode.exe          ← NSIS 卸载器（electron-builder nsis 目标产物）
resources/app.asar           ← 340MB 应用代码包（asar 归档）
resources/app.asar.unpacked/ ← 不能打进 asar 的原生模块（node-pty、ssh2）
resources/elevate.exe        ← 需要提权时的辅助程序（electron-builder 自带）
resources/config/            ← 随包只读默认配置（default.json：反馈/社区链接）
d3dcompiler/ffmpeg/vk_*.dll  ← Electron 运行时文件
```

要点：

- **per-user 安装**：默认装到 `%LOCALAPPDATA%\Programs\<App>`，用户可选目录（本机用户选了 D 盘），不需要管理员权限；提权场景才用 `elevate.exe`
- **asar 打包**：全部 JS/HTML/资源打进一个文件，只有带 .node 原生二进制的依赖放 `app.asar.unpacked`（electron-builder 自动处理 `asarUnpack`）
- **程序目录与数据目录彻底分离**（见 1.3）——这让覆盖升级/卸载不碰用户数据

### 1.2 自更新：electron-updater 状态机 + 差量下载

`resources/app-update.yml`（electron-updater 配置）：

```yaml
provider: generic
useMultipleRangeRequest: false
url: http://localhost:8081        # 更新源（本机这台是内网/开发源；官网版指向官方更新服务器）
updaterCacheDirName: '@zcodedesktop-updater'   # 更新缓存目录名
```

更新缓存目录（`%LOCALAPPDATA%\Programs\@zcodedesktop-updater\`）：

```
installer.exe     ← 下载好的新版安装器
current.blockmap  ← 当前版本的块哈希表（差量更新用）
pending/          ← 暂存待装更新
```

主进程里的更新状态机（从 `out/main/index.js` 提取的字符串归纳）：

```
idle → checking → download-progress → update-downloaded → quitAndInstall
```

设计细节值得学：

- **blockmap 差量更新**：electron-updater 对比新旧版块哈希，只下载变化的块，几百 MB 的应用更新只需下载几 MB
- **pending 暂存与恢复**：日志出现 "restage restored pending update"——下载完成后暂存，应用重启时若发现有 pending 更新可以恢复安装状态，不丢更新
- **渠道（channel）管理**：日志有 "refresh manifest channel x -> y"、"channel unchanged"，支持多更新渠道切换
- **防抖**：`quitAndInstall already in flight` / `check already in flight, defer`——安装/检查都加单飞锁
- **进度可见**：按 10% 步进取整上报下载进度（`[force-update] 自动升级下载进度 40%`）
- **未打包环境直接跳过**：`[auto-update] skip: not packaged`——开发模式不会触发更新

### 1.3 数据目录：与程序目录、版本无关

| 目录 | 内容 | 证据 |
|---|---|---|
| `~/.zcode/` | 用户数据总根（cli/、v2/、skills/、plugins/…） | 本机存在 |
| `~/.zcode/cli/` | CLI 与桌面共用的配置/日志（config.json 里注册 MCP 服务器） | 日志导出代码打包 `.zcode/cli/config.json`、`.zcode/cli/log`、`.zcode/cli/rollout` |
| `~/.zcode/v2/` | 桌面应用自己的凭据/配置/日志 | "Clear All Data" 对话框原文："将删除 ~/.zcode/v2（配置、凭据、日志）和浏览器缓存，然后自动重启" |

**深链接协议注册**：主进程 `setAsDefaultProtocolClient("zcode", process.execPath, ["--"])`，
支持 `zcode://workspace/open?path=<encoded>`——官网/网页可以一键把工作区拉进桌面应用。
注册时把可执行文件路径和 `--` 参数都写进注册表（开发模式跑 electron.exe 时也能正确带参）。

### 1.4 单实例锁

`app.requestSingleInstanceLock()`——第二个实例启动时把参数交给已有实例然后退出，
深链接唤起的就是已运行实例。

---

## 2. Desktop 模式：Electron 多进程架构

app.asar 里的 `out/` 结构直接暴露了进程划分：

```
out/main/       index.js 786KB   ← 主进程：窗口/更新/协议/托盘/资源管理
out/host/       index.js 1.68MB  ← Agent 引擎进程（GLM agent 运行时）
out/scheduler/  index.js 1.0MB   ← 定时任务进程（zcode-cron-scheduler）
out/preload/    7 个 .cjs 预加载脚本（按功能分沙箱面）
out/renderer/   React 界面（index.html / login.html / workspace.html / resource-manager.html …）
out/metadata/   插件元数据
out/plugin-sandbox/
```

### 2.1 关键决策：Agent 引擎跑在 utilityProcess，不跑在主进程

主进程代码（提取自 `out/main/index.js`）：

```js
const xd = join(import.meta.dirname, "../host/index.js");
const Lh = join(import.meta.dirname, "../scheduler/index.js");
// ...
const d = utilityProcess.fork(xd, [], {
  serviceName: Ks(t),                    // 每个会话一个具名 host 进程
  execArgv: ["--no-warnings"],
  env: { ...n.hostProcessLocalEnv, ZCODE_PROCESS_LABEL: t, ... }
});
```

为什么用 `utilityProcess.fork` 而不是 `child_process`：

- 是 Electron 官方的**受管子进程**：有 Chromium 级生命周期管理（随应用退出）、可被 `app.getAppMetrics()` 观测、支持 `serviceName` 标识
- **崩溃隔离**：Agent 引擎（跑模型循环、工具执行）崩溃不拖垮窗口
- 资源管理器按角色分组显示进程：`main / gpu / renderer / host / scheduler / cli / utility`，用户能看到每个 Agent 会话占多少 CPU/内存

### 2.2 主进程 ↔ host ↔ renderer 的通信

- 主进程与 host 之间：`MessageChannelMain` 建立端口直连（主代码出现 5 处），`postMessage` 传结构化消息（63 处）
- 主进程与 renderer 之间：`ipcMain.handle` + 类型化通道常量（`k.UpdateStateChanged`、`k.SettingsChanged`、`k.WindowControlsOverlayChanged`…）
- preload 脚本按功能拆成 7 个独立文件（`index.cjs` 主桥、`pluginSandbox.cjs` 插件沙箱、`cuaPermissionPanel.cjs` 权限面板、`embeddedBrowserJavaScriptDialog.cjs`、`codingPlanWebview.cjs` 订阅账户、`rewardsWebview.cjs`、`resourceManager.cjs`），**contextIsolation:true + sandbox:true，nodeIntegration:false**

### 2.3 窗口体系

- **窗口类型**：`index`（主页）、`login`（登录页）、`workspace`（工作区，多开）、`update-status`、Resource Manager——每类一个 renderer HTML，`loadWindow(e, kind, opts)` 统一入口，打包后 `loadFile("../renderer/${kind}.html")`，开发模式走 `ELECTRON_RENDERER_URL`
- **托盘（Tray）**：主代码 38 处引用，驻留后台
- **Windows 专属适配**：Toast 通知 AUMID 图标读取（`createWindowsAumidIconReader`）、CUA 操作悬浮指示、PowerShell 安全参数（`createWindowsPowerShellSecurityArgs`）
- **桌面缩放**：`desktopZoomLevel` 持久化到设置，Ctrl+滚轮缩放广播给所有窗口

### 2.4 内置的执行能力（host 进程里）

依赖清单（asar 内 package.json）说明桌面模式不只是聊天窗：

| 依赖 | 用途 |
|---|---|
| `node-pty`（unpacked） | 真终端（conpty），内嵌交互式 shell |
| `playwright-core` | 内嵌浏览器自动化（browser-use 插件、嵌入式浏览器对话框） |
| `@zcode/zcode-cua` + `cuaPermissionPanel` | Computer Use：接管屏幕操作，带独立权限面板 |
| `@modelcontextprotocol/client` | MCP 客户端——读取 `~/.zcode/cli/config.json` 里注册的 MCP 服务器（本机的 REA 就是这么被桌面会话加载的） |
| `ssh2` | 远程执行（remote executor 场景） |
| `sharp` / `yauzl`/`yazl` | 图像处理 / 压缩打包（日志导出 zip） |
| `@arms/rum-*` | 阿里 ARMS 前端监控（崩溃/性能/回放） |
| `@larksuiteoapi/node-sdk` | 飞书集成 |

**插件体系**：`resources/glm/packages/` 下 17 个随包插件（browser-use、documents、pdf、presentations、spreadsheets、visualize、skill-creator、zcode-cua…），每个是独立 npm 包，经 `out/plugin-sandbox/` + `pluginSandbox.cjs` 预加载脚本在受控环境里运行。

**与 CLI 的关系**：桌面 app 不是 CLI 的壳。它把引擎（`@zcode/client` 等包）打包进自己体内，在 host 进程里运行；但与独立 CLI **共享 `~/.zcode/cli/config.json`**（MCP、技能、插件配置同源），会话数据亦互通（`~/.zcode/cli/rollout`）。

---

## 3. 落地实现清单（给自己的项目）

### 3.1 走通"下载 exe → 桌面可用"的最小方案

1. **electron-builder 一步出包**：`"build": {"win": {"target": "nsis"}, "files": ["out/**"], "asarUnpack": ["**/node_modules/**/node-pty/**"]}` → 产出 `Setup.exe`
2. **NSIS per-user**：`nsis: { oneClick: false, perMachine: false, allowToChangeInstallationDirectory: true }`——免管理员、可选目录
3. **数据目录用 `app.getPath('userData')`**（Windows 落在 `%APPDATA%`），程序目录永远不写
4. **electron-updater 接上**：`app-update.yml` 指向你的静态文件服务器（generic provider 即可，Nginx/OSS 都行），代码里 `autoUpdater.checkForUpdates()` + 四个事件（checking/download-progress/update-available/update-downloaded）渲染 UI，`update-downloaded` 后提示重启安装
5. **协议注册**：`app.setAsDefaultProtocolClient("你的scheme")` + `second-instance` 事件里解析参数唤起窗口
6. **单实例锁**：`requestSingleInstanceLock()`，拿不到锁就把参数转发给已有实例再退出

### 3.2 走通"桌面模式"的最小方案

1. **三进程起点**：main（窗口/更新/系统）+ renderer（React）+ **utilityProcess.fork 出 agent 引擎**——不要把模型循环跑在 main 里，崩溃=白屏
2. **通信三件套**：main↔renderer 用 `ipcMain.handle`（类型化通道名常量）；main↔host 用 `MessageChannelMain` 端口；renderer 不给 nodeIntegration，能力全部经 preload 白名单暴露
3. **崩溃与观测**：`child-process-gone`/`render-process-gone` 事件里重启对应进程；进程列表用 `app.getAppMetrics()` + utilityProcess 的 pid 拼出来（ZCode 的 Resource Manager 就是这么做的）
4. **原生能力按需加**：终端=node-pty（记得 asarUnpack）；浏览器自动化=playwright-core；屏幕操作=_robot 配独立权限确认面板
5. **插件沙箱**：每个插件一个独立 preload/独立 webContents（`pluginSandbox.cjs` 模式），插件崩溃互不影响
6. **随包资源**：技能/插件/默认配置放 `resources/<name>/`（asar 外），启动时按需挂载，升级替换整个目录

### 3.3 两条路线对比（桌面壳 vs dsh 的本地服务器）

| 维度 | ZCode（Electron 壳） | dsh（本地 Web 服务器） |
|---|---|---|
| 分发 | NSIS exe + 自更新，体验最"桌面" | npm 安装 + 浏览器访问，分发最轻 |
| 系统集成 | 托盘/深链/Toast/原生菜单全都有 | 靠 open/reveal 类原生命令补 |
| 进程模型 | main+renderer+utilityProcess 受管 | node 主进程 + 浏览器 |
| 崩溃隔离 | 天然多进程 | 引擎崩=服务崩 |
| 适合场景 | 面向 C 端的完整桌面产品 | 开发者工具、内部平台 |

如果你要做的是"用户下载安装的桌面产品"，照 ZCode 这条路走；如果是"开发者本机跑的 Agent 服务"，dsh 那条路更轻。两者可以组合：Electron 壳里也可以嵌一个本地 HTTP 服务（ZCode 的 coding-plan/rewards webview 就是用独立 session partition 嵌网页）。

---

## 附：本机证据位置

| 内容 | 位置 |
|---|---|
| 应用本体与运行时 | `D:\Visual_Stdio_Code\AI\Zcode\`（ZCode.exe 3.15.3.8237） |
| 更新配置 | `D:\Visual_Stdio_Code\AI\Zcode\resources\app-update.yml` |
| 更新缓存（installer.exe/blockmap/pending） | `%LOCALAPPDATA%\Programs\@zcodedesktop-updater\` |
| 随包插件 | `D:\Visual_Stdio_Code\AI\Zcode\resources\glm\packages\`（17 个） |
| 共享 CLI 配置（MCP 注册） | `~/.zcode/cli/config.json` |
| 桌面应用自身数据 | `~/.zcode/v2/` |
| 分析用抽取文件 | `%TEMP%\zcode-asar\`（main-index.js / host-index.js） |
