@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
rem ============================================================
rem  拾光 · 公网临时隧道（Cloudflare Tunnel，免账号 / 免流量费）
rem  用法：先启动后端（:8080），再双击本文件。
rem  启动后窗口里会打印 https://xxxx.trycloudflare.com，这就是公网地址。
rem  关掉窗口 / 电脑休眠 → 地址失效；重新运行 → 换一个新地址。
rem ============================================================

set "PORT=8080"
set "CF="
rem 仓库根目录（本文件在 <仓库>/web-lostfond/scripts/ 下）
set "ROOT=%~dp0..\.."

rem ① 项目统一软件目录（首选）
if exist "%ROOT%\Project_operation_software\cloudflared\cloudflared.exe" set "CF=%ROOT%\Project_operation_software\cloudflared\cloudflared.exe"

rem ② 用户目录下的 bin
if not defined CF if exist "%USERPROFILE%\bin\cloudflared.exe" set "CF=%USERPROFILE%\bin\cloudflared.exe"

rem ③ 常见安装位置
if not defined CF if exist "%ProgramFiles(x86)%\cloudflared\cloudflared.exe" set "CF=%ProgramFiles(x86)%\cloudflared\cloudflared.exe"
if not defined CF if exist "%ProgramFiles%\cloudflared\cloudflared.exe" set "CF=%ProgramFiles%\cloudflared\cloudflared.exe"

rem ④ 最后在 PATH 里搜
if not defined CF set "CF=%~$PATH:cloudflared.exe"

if not defined CF (
  echo [错误] 没找到 cloudflared.exe。
  echo        请先把 cloudflared.exe 放到项目统一软件目录：
  echo          %ROOT%\Project_operation_software\cloudflared\cloudflared.exe
  echo        下载地址（Cloudflare 官方 Releases）：
  echo          https://github.com/cloudflare/cloudflared/releases/latest
  echo        （下载 cloudflared-windows-amd64.exe，改名为 cloudflared.exe 放进去）
  echo.
  pause
  exit /b 1
)

echo 使用 cloudflared: %CF%

echo [1/2] 检查本机后端 http://127.0.0.1:%PORT% ...
curl -s -o nul --max-time 3 "http://127.0.0.1:%PORT%/"
if errorlevel 1 (
  echo       [!] 连不上。请先在 backend 目录执行:
  echo           java -jar target/lost-found-backend.jar --server.port=%PORT%
  echo       继续启动隧道也可以，但打开页面会 502。
  echo.
) else (
  echo       后端正常。
)

echo [2/2] 正在申请公网地址（首次约 5-10 秒）...
echo ------------------------------------------------------------
echo   地址会打印在下面，形如 https://xxx-yyy-zzz.trycloudflare.com
echo   按 Ctrl+C 停止隧道。
echo ------------------------------------------------------------
echo.

"%CF%" tunnel --url "http://127.0.0.1:%PORT%" --no-autoupdate

echo.
echo 隧道已退出。
pause
