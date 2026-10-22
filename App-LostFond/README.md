# 拾光 · Windows 桌面版（App-LostFond）

本目录把 `web-lostfond` 的 Web 项目落地为 **Windows 独立安装包**，`web-lostfond` 源码保持原样、未做任何改动。

| 产物 | 平台 | 文件 |
|---|---|---|
| Windows 独立安装包 | Windows 10 / 11 x64 | `Softeare/Shiguang-Windows-Setup-1.0.2.exe` |

面向使用者的安装步骤、演示账号、常见问题见 [`Softeare/使用说明.md`](Softeare/使用说明.md)。

> 安装包约 162 MB，超过 GitHub 单文件 100 MB 限制，**不提交到 Git 仓库**；发布时上传到 [GitHub Releases](https://github.com/unicornDu/lost-fond/releases)。仓库内的 `Softeare/` 只保留 `SHA256SUMS.txt` 与 `使用说明.md`。

> 2026-10-22：按使用需求，Android 工程、Android SDK 与 APK 已删除；本工程只维护 Windows 桌面版。

---

## 一、架构

**Windows 版（单进程安装、双击即用）**

```
Electron 主进程 (desktop/main.js)
  ├─ 启动 内置 JRE 21 (desktop/runtime)
  ├─ 运行 Spring Boot jar，profile=app
  │    ├─ H2 文件数据库（MySQL 兼容模式）
  │    └─ 同源托管 frontend/dist
  └─ BrowserWindow 打开 http://127.0.0.1:<port>
```

- 端口默认 `18080`，被占用时自动在 `18080–18099` 内顺延。
- 首次启动自动建库并播种演示数据；JWT 密钥首次运行随机生成。
- 监听 `0.0.0.0`，同一局域网内的其他设备可用浏览器访问，不需要额外安装客户端。
- 不依赖云服务器；每个用户的数据保存在本机 `%APPDATA%\Shiguang`。

---

## 二、目录结构

```
App-LostFond/
├─ backend/                  # 由 web-lostfond/backend 复制并增加 H2 支持
│  └─ src/main/resources/
│     ├─ application-app.yml # 独立版 profile：H2、存储路径、CORS、演示种子
│     └─ db/schema-h2.sql
├─ frontend/                 # 由 web-lostfond/frontend 复制，Windows 桌面版共用
│  └─ vite.config.js         # electron 模式使用相对 base
├─ desktop/                  # Electron 壳 + 打包配置
│  ├─ main.js
│  ├─ package.json           # electron-builder 配置
│  └─ runtime/               # jlink 生成的内置 JRE 21
├─ build/
│  ├─ make-icons.mjs         # 生成桌面图标
│  └─ release/               # electron-builder 中间产物
└─ Softeare/                 # 最终交付物 + 校验和 + 使用说明
```

---

## 三、构建环境

| 工具 | 版本 | 说明 |
|---|---|---|
| JDK | 21 | 编译后端、生成内置 runtime |
| Maven | 3.9+ | 构建后端 jar |
| Node.js | 20+ | 构建前端、Electron |

Windows 构建不需要 MySQL：独立版使用 H2 文件数据库。

---

## 四、构建 Windows 安装包

### 1. 后端 jar

```bash
cd App-LostFond/backend
mvn -DskipTests clean package
# 产物：target/lost-found-backend.jar
```

### 2. 前端（Electron 模式）

```bash
cd App-LostFond/frontend
npm install
npm run build:electron
# 产物：dist/（相对资源路径）
```

> 注意：`npm run dist` 会自动重新执行这一步。**不要**直接运行 `electron-builder`，否则可能把旧的 `dist` 打进安装包。

### 3. 内置 JRE（已生成，可跳过）

`desktop/runtime` 由 jlink 生成，包含后端及 H2 所需模块。需要重建时：

```bash
jlink --add-modules java.base,java.compiler,java.datatransfer,java.xml,java.prefs,java.desktop,java.instrument,java.logging,java.management,java.security.sasl,java.naming,java.net.http,java.rmi,java.scripting,java.security.jgss,java.transaction.xa,java.sql,jdk.charsets,jdk.crypto.ec,jdk.localedata,jdk.unsupported,jdk.zipfs \
      --strip-debug --no-header-files --no-man-pages --compress=zip-6 \
      --output App-LostFond/desktop/runtime
```

### 4. 打包

```bash
cd App-LostFond/desktop
npm install
npm run dist
```

- `npm run dist` 执行 `build-dist.mjs`：先以 Electron 模式构建前端，再用 electron-builder 打包到 `build/release`，最后把安装包复制到 `Softeare/Shiguang-Windows-Setup-1.0.2.exe`。
- NSIS 采用 **oneClick + per-user**：安装到 `%LOCALAPPDATA%\Programs\shiguang-desktop`，不需要管理员权限，也不提供路径选择，避免用户装到中文路径或被联想电脑管家/火绒等安全软件保护的目录（这类目录中的未签名程序会被拦截，表现为双击无反应）。可执行文件为 `shiguang.exe`，快捷方式仍显示「拾光」。
- `package.json` 中的 `build.electronDist` 指向本地 `node_modules/electron/dist`，避免打包阶段再次联网下载 Electron。
- 若 `npm install` 时 Electron 二进制下载失败，可把 `%LOCALAPPDATA%\electron\Cache` 中对应版本的 zip 解压到 `desktop/node_modules/electron/dist`，并写入 `path.txt`（内容 `electron.exe`）。
- 如果 `build/release` 或 `Softeare/win-unpacked` 被安全软件短暂占用导致打包报 `EBUSY`，可稍后重试，或在 `desktop/package.json` 与 `build-dist.mjs` 中换一个新的输出目录（如 `build/release` → `build/release2`）；构建输出与交付目录已分开。

开发调试（不打包）：

```bash
cd App-LostFond/desktop
npm start
```

---

## 五、运行数据

| 内容 | 路径 |
|---|---|
| H2 数据库 + 上传图片 | `%APPDATA%\Shiguang\data` |
| JWT 密钥 | `%APPDATA%\Shiguang\jwt.key` |
| 后端日志 | `%APPDATA%\Shiguang\logs\backend.log` |

- 删除 `%APPDATA%\Shiguang\data` 后重启即可重置演示数据。
- 卸载程序不会删除上述数据目录。
- 手动运行后端（不使用 Electron）时必须提供 `JWT_SECRET` 与 `APP_DATA_DIR`，例如：

```bash
APP_DATA_DIR=./app-data JWT_SECRET=<随机字符串> SERVER_PORT=18080 \
  java -jar backend/target/lost-found-backend.jar --spring.profiles.active=app
```

---

## 六、验收结果与限制

已验证：

- Windows 1.0.2：一键安装到当前用户目录 → 启动 → 建库播种（20 用户 / 60 启事 / 120 条图片记录）→ 接口 200 → 管理员与普通用户登录 → 单实例（重复启动不重复起服务）→ 退出后端口释放。
- 1.0.2 在 1.0.1 基础上移除了移动端菜单措辞与 Capacitor 代码，重新打包；1.0.1 的既有修复保持不变。
- 已修复 1.0.0 的启动失败：旧包允许选择安装目录并可选“所有用户”，当安装到 `D:\新建文件夹\...` 这类被联想电脑管家（火绒引擎）保护的目录时，未签名 exe 会被拦截，双击无反应且没有日志。1.0.1 固定安装到当前用户目录，并在打包前强制以 Electron 模式重建前端。
- 前端 31 个单元测试全部通过；Electron `dist` 可构建。

限制：

- 安装包未做商业代码签名，Windows SmartScreen 可能提示风险。
- Windows 版同一时间只允许运行一个实例。
- 每个用户的数据独立保存在本机；需要多人共用数据时，使用同一台电脑的局域网地址访问。
- 不要把 Windows 版程序目录手动移动到中文/受安全软件保护的文件夹；需要迁移时重新运行安装包。安全软件（联想电脑管家/火绒等）的“文件夹保护”可能拦截未签名程序。

---

## 七、与 Web 版的关系

- 代码基线来自 `web-lostfond`，本目录内的改动不会回流，**Web 答辩版本不受影响**。
- Web 版继续使用 MySQL + `web-lostfond/backend/application-local.yml` 的本地配置；App 版使用 H2 + `application-app.yml`。
- Web 版部署仍以根目录 `README.md` 为准。
