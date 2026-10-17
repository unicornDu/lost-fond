# cloudflared

公网内网穿透客户端。用来把本机后端 `http://127.0.0.1:8080` 暴露成一个公网可访问的 HTTPS 域名，
替代已停用的花生壳免费版。

## 版本

- `cloudflared 2026.10.1 (built 2026-10-15T09:15 UTC)`
- 文件：`cloudflared.exe`（Windows amd64，约 55MB）

## 获取方式

**首选：本仓库 Releases**（已传好，直接下）

<https://github.com/unicornDu/lost-fond/releases> → 附件 `cloudflared-windows-amd64.exe`

下载后放到本目录：`Project_operation_software/cloudflared/cloudflared.exe`

**备用：Cloudflare 官方**（国内直连约 70–80 KB/s，55MB 要 10 分钟以上）

```bash
curl -L -C - -o cloudflared.exe \
  "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe"
```

> 中途断了用 `-C -` 续传，别从头下。

**macOS / Linux 用户**：本项目自带的 `tunnel.bat` 只适用于 Windows。换平台请按官方文档装
（macOS `brew install cloudflared`，Linux 下 apt/yum 或直接下对应架构的二进制），
命令本身不变：`cloudflared tunnel --url http://127.0.0.1:8080`。

## 怎么用

项目里已经封装好，**双击 `lost-found/scripts/tunnel.bat`** 就会：

1. 按优先级找 cloudflared（本目录 → `%USERPROFILE%\bin` → Program Files → PATH）；
2. 先探测 `127.0.0.1:8080` 是否活着；
3. 申请临时隧道并打印公网地址，形如 `https://xxx-yyy-zzz.trycloudflare.com`。

手动等价命令：

```bash
cloudflared.exe tunnel --url http://127.0.0.1:8080 --no-autoupdate
```

## 注意

- **免账号临时隧道每次重启都会换域名**，关窗口 / 休眠即失效；
- 免费、无流量上限、无请求数限制，支持 WebSocket 与大文件上传（后端 20MB 上限远低于 CF 限制）；
- 域名一变，桌面端改 `%APPDATA%\ShiGuang\config.json` 的 `apiBase` 即可（无需重打包），
  但 **APK 必须改 `frontend/.env.mobile` 的 `VITE_API_BASE` 后重新打包**；
- 想要固定域名：注册 Cloudflare 账号 + 一个托管在 CF 的域名，用
  `cloudflared tunnel login` / `create` / `route dns` 建命名隧道，依然免费、无流量限制。
