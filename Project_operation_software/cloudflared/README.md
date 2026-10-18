# cloudflared

公网内网穿透客户端（Cloudflare Tunnel）。把本机后端 `http://127.0.0.1:8080` 暴露成一个
公网可访问的 HTTPS 域名，替代已停用的花生壳免费版。

## 版本与文件

| 文件 | 说明 |
|---|---|
| `cloudflared.exe` | v2026.10.1，Windows amd64，约 55MB |
| `config.yml` | 内容为 `{}` 的占位文件，quick tunnel 用不到，可删 |

## 获取方式（新电脑要装时）

**官方 Releases**（认准 Cloudflare 官方仓库）：

<https://github.com/cloudflare/cloudflared/releases/latest>
→ 下载 `cloudflared-windows-amd64.exe` → 改名 `cloudflared.exe` → 放到本目录。

国内直连 GitHub 较慢，断了可续传：

```bash
curl -L -C - -o cloudflared.exe ^
  "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe"
```

**macOS / Linux**：`brew install cloudflared`（macOS）或 apt/yum/官方二进制（Linux）。
命令与 Windows 完全一致，但本仓库的 `tunnel.bat` 仅适用于 Windows。

## 怎么用：临时隧道（免账号，项目默认方式）

> 前提：后端已在 8080 运行。没跑的话先执行
> `cd lost-found/backend && java -jar target/lost-found-backend.jar`

**方式 A（推荐）：双击 `lost-found/scripts/tunnel.bat`**，脚本会自动：

1. 按「本目录 → `%USERPROFILE%\bin` → Program Files → PATH」四级找 cloudflared.exe；
2. 探测 `127.0.0.1:8080` 是否存活（不拦着启动，但页面会 502）；
3. 申请隧道并打印公网地址。

**方式 B：手动命令（与 A 等价）**

```bash
cd Project_operation_software/cloudflared
cloudflared.exe tunnel --url http://127.0.0.1:8080 --no-autoupdate
```

**拿到地址后**：窗口里形如 `https://xxx-yyy-zzz.trycloudflare.com` 的那行就是公网地址，
发给任何人，浏览器打开即完整站点（后端同源托管前端）。

**怎么停**：关掉窗口，或在窗口里按 `Ctrl+C`。

## 使用须知

- **域名每次重启都会变**：重新运行就是新地址，旧地址立即作废 → 地址要"现开现发"，不要写死进任何文档；
- **你的电脑就是服务器**：电脑关机 / 休眠 / 关窗口 = 服务下线，演示期间把电源计划睡眠设为"从不"；
- 免费、无流量上限、自带 HTTPS、支持 WebSocket；后端 20MB 上传限制远低于 Cloudflare 侧限制；
- **公网即公开**：拿到网址的任何人都能访问并注册账号、看到演示数据，演示完就关。

## 进阶：固定域名（named tunnel，依然免费）

临时隧道的域名会变；要固定网址需 Cloudflare 账号 + 一个托管在 CF 的域名：

```bash
cloudflared tunnel login                 # 浏览器授权
cloudflared tunnel create shiguang       # 建命名隧道
cloudflared tunnel route dns shiguang demo.你的域名.com
cloudflared tunnel run shiguang          # 运行（域名固定）
```
