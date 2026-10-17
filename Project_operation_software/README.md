# 项目运行软件目录

本项目运行 / 演示 / 打包时需要用的第三方软件。

**可执行文件不进仓库**，统一作为 GitHub Releases 的附件分发；这里只保留台账和每个软件各自的说明。

- Releases 下载页：<https://github.com/unicornDu/lost-fond/releases>
- 克隆仓库后：照下面「台账」把缺的软件下到对应子目录（路径要对得上，见各子目录的 README）

## 目录约定

- 一个软件一个子目录，目录名用小写英文，与可执行文件同名；
- 子目录里只放**可执行文件 + `README.md`**，摆好后 `lost-found/scripts/` 下的启动脚本就能自动找到；
- 二进制已被根 `.gitignore` 排除（`Project_operation_software/*/*.exe`），不要试图提交。

```
Project_operation_software/
├── README.md              ← 本文件（总台账，入库）
└── cloudflared/
    ├── cloudflared.exe    ← 不入库，从 Releases 下载后放这里
    └── README.md          ← 入库
```

## 台账

| 软件 | 放这里 | 版本 | 用途 | 在哪下 |
|---|---|---|---|---|
| cloudflared | `cloudflared/cloudflared.exe` | 2026.10.1 | 公网内网穿透：把本机 `:8080` 映射成 `https://*.trycloudflare.com`，替代已停用的花生壳 | 本仓库 Releases → `cloudflared-windows-amd64.exe` |

## 新增一个软件的流程

1. 在本目录下建 `<软件名>/`；
2. 写一份 `<软件名>/README.md`：版本、用途、下载地址、使用方式；
3. 回到本文件，在「台账」表里补一行；
4. 把可执行文件传到该次 GitHub Release 的附件里，**不要 commit**；
5. 若启动脚本要引用它，脚本里用**相对仓库根的路径**，不要写死 `C:\Users\...`。

## 为什么二进制走 Release 而不是仓库

- 体积：cloudflared 单个 55MB，放进仓库等于每个 clone 的人都要多下 55MB；
- 历史：二进制改一次，git 里就多存一整份（无法增量），历史会迅速膨胀；
- 分发：Releases 支持单文件 2GB、可单独下载、可标版本号、可写说明，正好适合放这类工具。

仓库里保留台账的好处是：别人 clone 后能知道**缺什么、去哪下、放哪个路径**，
不至于启动脚本报“找不到 cloudflared.exe”时无从下手。
