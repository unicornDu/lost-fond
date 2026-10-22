# MarkDone · 公网访问开启与关闭

> 适用项目：拾光 · 校园失物招领（`web-lostfond`）
> 记录日期：2026-10-21
> 状态：已完成并实测（隧道开启返回 200，关闭后旧地址返回 530）
> 相关脚本：[tunnel.bat](./tunnel.bat)

---

## 一、三种访问方式

| 场景 | 地址 | 需要隧道 | 谁能访问 |
|---|---|---|---|
| 本机自测 | `http://localhost:8080/` | 否 | 只有本机 |
| 同一 Wi-Fi / 热点 | `http://<电脑局域网 IP>:8080/` | 否 | 同一网络内的设备 |
| 任意网络（手机流量） | `https://xxx.trycloudflare.com` | 是 | 拿到链接的任何人 |

后端 `:8080` 同时托管打包好的前端（`frontend/dist`），所以给别人看只需要这一个端口，不必另开 Vite。

---

## 二、启动前检查

1. **后端已启动**：浏览器打开 `http://localhost:8080/` 能看到首页，或命令行执行：
   ```bat
   curl -s -o nul -w "%{http_code}" http://127.0.0.1:8080/
   ```
   返回 `200` 即正常。
2. **前端已打包**：`web-lostfond/frontend/dist/` 存在。若改过前端代码，先在 `frontend` 目录执行：
   ```bat
   npm run build
   ```
3. **cloudflared 已就位**（仅公网方式需要）：
   `Project_operation_software\cloudflared\cloudflared.exe`

### 本机启动后端（实测命令）

```bat
cd /d <仓库>\web-lostfond\backend
java -jar target/lost-found-backend.jar --spring.config.additional-location=file:./application-local.yml
```

> 必须在 `backend` 目录下启动，否则相对路径 `../frontend/dist` 找不到前端页面。
> `application-local.yml` 含数据库密码和 JWT 密钥，已被 gitignore，不要提交、不要外发。

### 本机前端开发模式（可选）

```bat
cd /d <仓库>\web-lostfond\frontend
npm run dev
```

默认地址 `http://localhost:5173/`，通过 Vite 代理访问后端；只给自己调试用。

---

## 三、方式 A：同一 Wi-Fi / 热点给手机看

### 开启

1. 电脑和手机连接**同一个** Wi-Fi 或热点；
2. 电脑上执行 `ipconfig`，找到当前网卡的 IPv4 地址；
   - 本次实测为 `172.20.10.2`（手机热点网段），换网络后会变；
   - `172.26.16.1` 是 WSL 虚拟网卡，`169.254.x.x` 是未连接网卡，都不要用。
3. 手机浏览器打开：
   ```
   http://172.20.10.2:8080/
   ```
   （把 IP 换成你当前查到的值）

### 打不开时检查

- 后端窗口是否还在运行、地址是否为 `0.0.0.0:8080`；
- Windows 防火墙是否放行 Java / 8080 入站：
  ```bat
  netsh advfirewall firewall add rule name="拾光 8080" dir=in action=allow protocol=TCP localport=8080
  ```
  需要管理员权限；
- 热点/路由器是否开启了「AP 隔离 / 访客隔离」，开启后设备之间不能互访；
- 电脑是否休眠。

### 关闭

- **只关闭手机访问**：停止后端进程（关闭运行后端的窗口，或 `Ctrl+C`）；
- **保留本机访问、禁止局域网访问**：启动时加参数，只监听本机：
  ```bat
  java -jar target/lost-found-backend.jar --server.address=127.0.0.1 --spring.config.additional-location=file:./application-local.yml
  ```
- 如果启动过 `npm run dev -- --host 0.0.0.0`，同一网络的手机也能访问 `http://<IP>:5173/`，关闭 Vite 窗口即可。

> 局域网走的是 HTTP，不是安全上下文，「使用当前位置」定位和剪贴板复制可能不可用；需要这些功能请用方式 B 的 HTTPS 地址。

---

## 四、方式 B：公网临时隧道（Cloudflare）

### 开启

1. 确认后端 `:8080` 正在运行；
2. 双击 [tunnel.bat](./tunnel.bat)；
3. 黑色窗口不要关闭，等待 **5–10 秒**，出现类似下面的地址：
   ```
   https://xxx-yyy-zzz.trycloudflare.com
   ```
4. 把这个 HTTPS 地址发给对方，手机流量也能打开。

首次运行如果弹出 Windows SmartScreen，点「更多信息 → 仍要运行」。

### 关闭

任选一种：

- 在隧道窗口按 `Ctrl+C`；
- 直接关闭该黑色窗口；
- 任务管理器中结束 `cloudflared.exe`。

只最小化窗口不算关闭，进程仍在运行。

关闭后验证：

```bat
tasklist | findstr cloudflared
```

没有输出即已退出；旧地址再打开会返回 **530**（隧道不存在）。

### 注意事项

- 地址是**临时**的：关窗口、关机、断网都会失效，重新启动会生成新地址；
- 电脑必须保持开机、不休眠，后端和隧道窗口都要保留；
- 链接**任何拿到的人都能打开**，包括注册和登录，只发给需要演示的人；
- trycloudflare 在国内偶尔不稳定，打不开时关掉重开一次；
- 快速隧道没有可用性保证，不适合长期正式使用。

---

## 五、常见问题

### 1. 双击后窗口一闪而过

已定位并修复：批处理必须是 **GBK/ANSI 编码 + CRLF 换行**。
如果之后用 VS Code、记事本等重新编辑过 `tunnel.bat`：

- 换行必须保持 `CRLF`（VS Code 右下角可切换）；
- 编码选择 `GBK` / `ANSI`（记事本「另存为 → 编码：ANSI」）；
- 不要保存成 UTF-8 或 LF，否则 cmd 解析中文行会报错并直接退出。

仍闪退时，在 cmd 中运行 `tunnel.bat` 查看具体报错。

### 2. 隧道开了但页面 502

后端没启动，或端口不是 8080。先确认 `http://127.0.0.1:8080/` 能打开，再重开隧道。

### 3. 没有打印地址

等待时间不够（首次 5–10 秒）；或网络无法访问 `api.trycloudflare.com`，重开窗口再试。

### 4. 手机打不开局域网地址

见「方式 A → 打不开时检查」：同一网络、防火墙、AP 隔离、电脑休眠。

---

## 六、安全提醒

- 公网链接一旦发出，任何人可访问、可注册、可登录，演示结束请立即关闭隧道；
- 仓库内的演示账号密码只用于课程演示；长期部署前必须更换数据库密码、JWT 密钥和种子账号密码；
- `backend/application-local.yml` 含敏感配置，不要写进文档、截图或提交到 Git；
- 不在公共场合长期开着 `cloudflared` 窗口。

---

## 七、本次验证记录（2026-10-21）

| 项目 | 结果 |
|---|---|
| 本机后端 | `http://127.0.0.1:8080/` → 200 |
| 公网隧道首页 | `https://...trycloudflare.com/` → 200 |
| 公网隧道接口 | `/api/items?page=1` → 200 |
| 关闭隧道后 | 旧地址 → 530，无 `cloudflared` 进程残留 |
| 批处理解析 | GBK + CRLF 重写后，完整走到启动隧道步骤，中文提示正常 |

关闭动作已执行，当前无公网隧道在运行；需要再次演示时重新双击 `tunnel.bat` 即可。
