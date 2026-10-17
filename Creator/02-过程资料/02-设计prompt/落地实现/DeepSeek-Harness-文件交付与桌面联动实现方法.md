# DeepSeek Harness（dsh）文件交付与桌面联动实现方法

> 学习来源：`@deepseek-ai/dsh 0.1.5-rc.1`（本机安装位置
> `C:\Users\emmmmm\AppData\Roaming\npm\node_modules\@deepseek-ai\dsh`），
> 通过静态阅读其编译产物与各子包 README 归纳。目标是把这套方法复刻到自己的项目里。

---

## 0. 整体架构：本地优先（Local-first）

dsh 的核心选择是：**Agent 跑在用户自己的电脑上，界面是一个本地起的小型 Web 服务器，浏览器打开 localhost 访问**，而不是做一个 Electron 壳。

由此带来的关键推论：

- Agent 生成的文件**本来就在用户磁盘的 workspace 里**，"下载文件"被重新定义为 **"预览 + 原生打开"**，而不是网络传输。
- 界面与 Agent 进程完全解耦：界面读的是**持久化的会话日志**，Agent 死活不影响 UI 渲染历史。
- 每个"功能"是一对可插拔插件：**host 半边**（本地能力探测与执行）+ **client 半边**（浏览器 UI），通过 YAML 组合挂载。

技术栈参考：Node.js 单二进制 CLI（NSIS 安装包分发），内置 Python 3.12 运行时
（python-docx / python-pptx / openpyxl / Pillow / lxml 等），让 Agent 能直接产出
docx、pptx、xlsx 等真实文件。运行时装在 `~/.dsh/dsh-runtimes/`。

---

## 1. 文件交付链路：present → 会话事件 → UI → 打开

### 1.1 Agent 只"声明"，不"搬运"

模型侧工具 `present`（包 `dsh-tool-present`）：

```
调用参数：files: [{ path: string, description?: string }]
约束：单次最多 8 个文件（maxFiles 可配）
```

设计要点：

- **只记路径和说明，不复制文件内容**——工具"记录路径和可选说明，不复制文件内容"。
- 服务端校验（任一失败即调用失败，把错误反馈给模型重试）：
  - 文件必须存在、是普通文件（目录不行）
  - 最终路径不能是符号链接
  - 文件必须对 Session 文件系统可访问（沙箱私有 /tmp 里的文件要先挪出来）
  - 相对路径按 Session 工作目录解析；绝对路径允许指向工作区外（如 Downloads）

### 1.2 声明变成持久事件（解耦的关键）

校验通过后，往会话日志（JSONL）写一条**持久事件**：

```
事件类型：deliverables/presented
载荷：{ turn: number, callId: string, files: [{ path, description? }] }
```

UI 渲染交付物卡片时**读的是会话日志**，不是问 Agent 要。效果：

- 会话重启、翻历史、换入口打开，卡片都还在
- 交付物与某一次工具调用（callId）、某一轮对话（turn）精确绑定

### 1.3 预览走一条受控文件路由 `/api/file`

浏览器预览文件内容的唯一入口（`dsh-api-session-controller`，`serveFile`）：

```
GET/HEAD /api/file?path=<绝对路径>
```

安全约束逐条学走：

| 约束 | 实现 | 目的 |
|---|---|---|
| 必须绝对路径 | 拒绝空 path、含 `\0`、非绝对路径 | 防路径注入 |
| 经 fs provider 解析 | `fs.resolve()` + 沙箱策略 | 越界访问返回 `FS_SANDBOX_DENIED` → 403 |
| 字节上限 | 复用附件图片大小限制 maxBytes，超限 413 | 防大文件拖垮服务器 |
| MIME 嗅探 | 按扩展名 lookup，默认 `application/octet-stream` | 正确渲染 |
| 响应头 | `Cache-Control: private, no-store`<br>`X-Content-Type-Options: nosniff`<br>`Content-Security-Policy: sandbox; default-src 'none'` | 防缓存泄露、防 MIME 混淆、防下载内容在站点上下文执行 |
| 错误码映射 | `FS_NOT_FOUND→404、FS_NOT_REGULAR_FILE/FS_PERMISSION_DENIED/FS_SANDBOX_DENIED→403、FS_TOO_LARGE→413、FS_ABORTED→499` | 精确、不泄露多余信息 |

注意它的注释原话："Paths and MIME types do not restrict access; the connection
service authenticates requests before this handler" —— **路径白名单不在这层做，
认证在更前置的连接层统一做**（见第 3 节）。

浏览器侧用法（`dsh-client-ui-chat`）：把 Markdown 里的本地媒体路径映射为同源 URL：

```js
function localPathMediaUrl(protocol, origin, value) {
  if (protocol !== "http:" && protocol !== "https:") return undefined; // Electron file:// 场景直接放弃
  if (!value.startsWith("/") || value.startsWith("//")) return undefined;
  return `${origin}/api/file?path=${encodeURIComponent(value)}`;
}
```

### 1.4 "下载"动作 = 原生打开

交付物卡片（`dsh-client-ui-deliverables`）只提供两个动作：

```
POST /api/present.open?action=open|reveal&sessionId=<id>&seq=<n>&index=<n>
```

- `open`：用系统默认应用打开该文件
- `reveal`：在文件管理器中显示该文件
- 服务端按 `sessionId/seq/index` 从会话日志**重新读出**文件路径（不信任浏览器传路径！），
  校验主机桌面能力（`workspaceDesktop().available`，不可用返回 409），再执行原生打开
- 请求在 Connection 的认证围栏内；进行中的任务用 Set 跟踪，插件卸载时统一 abort

**用户视角的"文件下载到了我电脑上"**：文件在生成那一刻就已经在用户磁盘上了，
UI 只是告诉他"这个文件在哪"并帮他打开。远程部署场景才需要真正的 HTTP 下载
（dsh 里对应 `session.export` 打 zip，响应头 `content-disposition: attachment; filename=...`）。

---

## 2. "继续到桌面"：open-in-app 与原生打开

### 2.1 跨平台原生打开（`dsh-native-command`）

一句话：**每平台一条 shell-free 命令，不经过 shell 解释**。

```js
// Windows
powershell.exe -NoProfile -Command "Invoke-Item -LiteralPath <path>"
// macOS
open <path>            // 文本文件意图用 open -t
// Linux
xdg-open <path>
// WSL：先翻译路径再交给 Windows
wslpath -w <path>  →  Invoke-Item
```

配套能力探测 `canReachDesktop()`：

- macOS / Windows 恒为 true
- Linux：WSL（Windows 接管）或有显示服务器才 true
- 无头/容器 Linux 返回 false → **UI 不渲染按钮**，把路径显示为文本

原则：**探测不到桌面就不给按钮**，而不是让用户点了才报错。

### 2.2 "Open In..." 分体按钮（host + client 成对插件）

`dsh-host-open-in-app`（主机半边）+ `dsh-client-ui-open-in-app`（浏览器半边）：

**主机端做三件事，各一条路由：**

1. **可用性**：探测本机已安装的编辑器 / Git GUI / 终端 / 文件管理器（固定应用目录 + 逐个验证启动器存在）；探测不到任何一个 → 整个按钮不渲染
2. **图标**：提取应用真实图标（Windows 读 exe 图标、macOS 读 bundle 图标、Linux 读主题图标），失败给通用占位
3. **启动**：POST 启动对应应用打开 workspace 目录；不把继承的凭据传给被启动的应用；探测与启动都有可配置超时

**浏览器端交互细节：**

- 主按钮显示"记住的应用"图标，一键启动；下拉列出全部可用应用
- 用户选择持久化在 localStorage（`dsh.open-in-app.choice`）；选的应用被卸载则回退到第一个可用项
- 飞行中的启动用 ref 守卫：重复点击/选择整体忽略（防止持久化一个没打开成的选择）
- 等待态只在超过 250ms 后出现（快速启动不闪烁）；失败显示红色描边两秒 + tooltip
- 应用重启后才出现新装的应用（探测按页缓存）

### 2.3 所有 /api 路由的统一认证围栏（`dsh-client-connection`）

这是整个设计里最严谨、最值得照抄的部分：

```
1. 进程启动时生成随机令牌
2. dsh web 打开带 ?token=... 的根 URL
3. 首页 GET / 才接受令牌 → 写入签名 cookie → 302 重定向到干净的 /
4. 之后所有请求凭 cookie 认证；query token / Authorization 头一律不再接受
```

Cookie 规格：

- 签名密钥存 `$DSH_HOME/.credentials.yaml`（凭据 grant 记录），服务留在内存
- 绑定规范化 hostname + port；携带绝对签发/过期区间；默认 30 天
- `HttpOnly`、`SameSite=Strict`、`Path=/`、host-only；loopback 场景刻意不设 `Secure`

请求信任检查（认证之外、每个请求都过，防 DNS rebinding 与跨站）：

- `Host` 必须是 loopback，或精确匹配 `trustedHosts` 条目（WHATWG 归一化）
- 若带 `Origin`，必须等于 Host
- `sec-fetch-site: cross-site` 一律拒绝
- 结果：信任检查失败 → 403；Host 可信但未认证 → 401；静态资源保持公开

**所有文件路由（/api/file、/api/present.open 等）都在这道围栏之后注册**——
认证是单点的、前置的，各功能包不重复造认证。

---

## 3. 落地实现清单（给自己的项目）

按优先级排列的最小可行方案：

1. **本地 Web 服务器 + 浏览器 UI**（FastAPI / Express / Hono 均可），
   `启动命令 → 打开 http://127.0.0.1:<port>/?token=...`。不要 Electron 壳。
2. **Agent 工具 `present(path, description)`**：
   - 校验存在 / 普通文件 / 非符号链接 / 在允许目录内
   - 通过后写一条持久事件到会话存储（JSONL 或 SQLite）
   - UI 卡片从会话存储渲染，与 Agent 进程解耦
3. **预览路由 `GET /api/file?path=`**：
   - 绝对路径校验 → resolve 到允许根目录内 → stat 检查 → 字节上限
   - 响应头加 `no-store` + `nosniff` + `CSP: sandbox`
   - 错误码按"未找到 / 非普通文件 / 越界 / 超限"细分
4. **打开/显示动作 `POST /api/present.open?action=open|reveal`**：
   - 浏览器只传 sessionId/seq/index，**路径由服务端从会话存储读回**
   - 原生打开：Windows `Invoke-Item -LiteralPath`、macOS `open`、Linux `xdg-open`
   - 用 `subprocess` 直调（`shell=False`），不拼 shell 字符串
5. **单点认证围栏**：
   - 启动随机 token → `GET /?token=` 换签名 HttpOnly SameSite=Strict cookie
   - 每请求过 Host/Origin/sec-fetch-site 信任检查
   - 所有业务路由注册在围栏之后
6. **能力探测决定渲染**：
   - `canReachDesktop()`（无头环境隐藏按钮）、应用探测（无可用应用隐藏 Open In）
   - 插件成对：host 半边探测执行 + client 半边渲染
7. **文件生成能力**：捆绑或引导安装 Python + python-docx/python-pptx/openpyxl，
   Agent 用 shell 调用脚本产出真文件，再 present 之。

### 常见坑（dsh 已经替你踩过）

- 浏览器传路径 = 任意文件读取漏洞，必须服务端按事件坐标读回路径
- 符号链接绕过目录白名单，present 校验要 resolve 后再判
- `Content-Type` 不对 + 没有 `nosniff` = 下载的 HTML 在你域下执行
- 无头服务器上按钮点了报错，不如探测后直接不渲染
- 启动令牌留在 URL 里被历史记录/引用头泄露 → 换一次就作废，立即重定向到干净 URL
- WSL 场景路径要 `wslpath -w` 翻译后再交给 Windows

---

## 附：关键源码位置（本机）

| 功能 | 位置 |
|---|---|
| present 工具 | `node_modules/@deepseek-ai/dsh-tool-present/`（README 有完整行为说明） |
| 交付物事件与 open 路由 | `node_modules/@deepseek-ai/dsh-client-ui-deliverables/lib/index.js` |
| `/api/file` 受控读取 | `node_modules/@deepseek-ai/dsh-api-session-controller/lib/index.js`（media-references 段） |
| 原生打开（跨平台） | `node_modules/@deepseek-ai/dsh-native-command/lib/index.js` |
| Open In 主机半边 | `node_modules/@deepseek-ai/dsh-host-open-in-app/`（README 含配置示例） |
| 认证围栏说明 | `node_modules/@deepseek-ai/dsh-client-connection/README.zh.md`（"浏览器认证与请求信任"节） |
| 运行时（文档生成包） | `~/.dsh/dsh-runtimes/dsh-primary-runtime/runtime.json` |
