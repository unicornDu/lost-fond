# 协作与提交规范

> 适用于本仓库全部内容。目的是让每次提交**可读、可回滚、不污染仓库**。
> 2026-10-17 曾因 `git add .` 一次性暂存 11869 个文件（含 712 MB Android SDK、350 MB 安装包、签名密钥），
> 仓库 `.git` 膨胀到 697 MB。本规范就是为了不再发生第二次。

---

## 一、提交前检查单（每次必做）

```bash
git status                    # ① 看清单：有没有不该入库的东西
git diff --stat               # ② 看规模：多少文件、多少行
```

逐条确认：

- [ ] 清单里**没有** `_android-sdk/`、`release*/`、`node_modules/`、`dist/`、`target/`
- [ ] 清单里**没有** `*.keystore`、`*.jks`、`application-local.yml`、`.env.local`
- [ ] 单个文件 < 10 MB（接近就先问，别硬提交）
- [ ] 待提交文件数与这次改动**相符**（改 5 个文件却显示 5000 个 → 一定有问题）
- [ ] 提交信息说清了"改了什么、为什么"

> **禁止裸用 `git add .` 或 `git add -A`。** 请明确到目录：
> `git add src/web-lostfond/backend/src` / `git add "doc"`
> 确实需要全量时，先 `git status` 审一遍再执行。

---

## 二、什么不该入库

| 类别 | 路径 | 原因 |
|---|---|---|
| 本地工具链 | `src/web-lostfond/_android-sdk/` | 712 MB，各人本机自备（设 `ANDROID_HOME` / `sdk.dir`）。**2026-10-17 单 Web 收敛后 Android 工程已下线，目录不再需要** |
| 打包产物 | `src/web-lostfond/desktop/release*/`、`target/*.jar` | 安装包 / Electron 运行时（约 350 MB）与可执行 jar 均可由源码重建 |
| 运行期软件 | `other/Project_operation_software/*/*.exe`、`*/*.zip` | 仓库只留 README 台账，二进制走 GitHub Releases |
| 构建缓存 | `node_modules/`、`dist/`、`target/`、`*/build/` | 可再生成 |
| 签名密钥 | `*.keystore`、`*.jks`、`*.p12`、`*.pfx` | 公开仓库泄露 = 他人可冒名签名打包 |
| 本地配置 | `src/web-lostfond/backend/application-local.yml` | 含数据库口令 / JWT secret / 演示口令 |
| 运行时数据 | `src/web-lostfond/backend/uploads/20*/` | 用户上传图，由 `.gitignore` 排除 |
| 系统/IDE | `.DS_Store`、`Thumbs.db`、`.idea/`、`.vscode/` | 噪音 |

以上规则已写入根 `.gitignore`，正常情况下 `git status` 不会列出它们。**若列出来了，说明规则被绕过，先修规则再提交。**

历史例外（已失效，保留说明以免后人困惑）：`src/web-lostfond/backend/uploads/downloads/app-release.apk`（约 5.9 MB）曾允许入库，供
`/api/file/apk` 分发 Android 安装包。**2026-10-17 答辩改为浏览器演示后，APK 与 `/api/file/apk`、桌面端一并下线**，该例外不再成立，
`downloads/` 下不再有任何入库产物。

---

## 三、提交信息规范

沿用本项目既有习惯：**一个版本/一个批次 = 一笔提交**，提交信息为 `versionNN_简述`。

| 已有提交 | 含义 |
|---|---|
| `verison-01` | 初版（历史遗留拼写，无需改） |
| `version-02` | 第二版 |
| `version-03_文件夹的规划` | 第三版：目录规划 |
| `version04_第一次漏洞修改` | 第四版：首轮缺陷修复 |
| `version05_debug02` | 第五版：第二轮调试 |
| `version06-software01` | 第六版：软件化收尾 |
| `version07_文档整理` | 第七版：文档体系整理 |

写法要求：

- **版本号递增，不复用**。当前到 `version07`，下一版写 `version08_简述`。
- **分隔符 `_` 或 `-` 均可**（历史两种都用过），**后缀用简短中文说明这一版做了什么**，如 `_文档整理`、`_后端加固`、`_多端适配`。
- **一次提交装一个完整批次**，不要把两个不相关的批次塞进同一笔。
- 细节写在**提交正文**里（改了什么、为什么、涉及哪些缺陷编号），标题保持一行可读。

```
version07_文档整理

- 过程资料按用途重组：Prompt/ → other/Creator/过程资料/{选题,设计prompt,迭代记录,参考项目}
- version_01~06 改为语义目录名，6 份废弃版移入 archive/
- 交付文档提层至 doc/（12 篇 + 运行说明 6 篇），同步修正 55 个文件路径引用
- 新增 README.md、CONTRIBUTING.md 与两级索引
- 删除 65 个 .class、13 个 .DS_Store 与 target/ 构建目录
```

> 若某一批改动较大，允许在同一版本号下拆成两三笔，但仍用 `versionNN_` 前缀并加区分后缀，
> 例如 `version08_后端加固1-参数校验` / `version08_后端加固2-限流`。

---

## 四、分支与合并

- 本项目实际以 `main` 单线开发，**一次提交对应一个批次**，与版本号一一对应。
- 需要多人并行时再开分支，分支名沿用版本号，如 `version08_后端加固`；合并后删除。
- 合并/推送前：本机跑通 `python scripts/test_api.py` 与 `mvn test`（涉及前端再跑 `npx vitest run`）。
- 大规模目录调整（如本次文档重组）单独占一个版本号，不要和功能改动混在一起。

---

## 五、误操作补救

### 已经 `git add` 了不该入库的东西

```bash
git restore --staged <路径>          # 撤出暂存区，工作区文件不动
git rm -r --cached <路径>            # 已跟踪过的话用这个取消跟踪
```

补好 `.gitignore` 后回收对象（撤销后这些 blob 已不可达，安全）：

```bash
git gc --prune=now
```

### 已经 commit 但没 push

```bash
git reset --soft HEAD~1              # 保留改动，回到暂存区
# 调整后重新提交
```

### 已经 push 了敏感信息（如 keystore）

立即按以下顺序处理，**不要只是删文件再提交**（历史里仍可检出）：

1. 作废并重新生成密钥，同步更新分发渠道的签名
2. 用 `git filter-repo` 清理历史（需组员统一重新 clone）
3. 清理完成后通知全体组员 `git clone` 新仓库，不要 `pull`

---

## 六、仓库体积红线

| 指标 | 建议值 | 说明 |
|---|---|---|
| 单文件 | < 10 MB | GitHub 硬限制 100 MB，超了直接拒收 |
| 仓库总量 | < 100 MB | 超过后 clone 明显变慢 |
| 单次提交文件数 | < 100 | 超过请检查是否误 add |

当前基线（2026-10-17 整理后）：`git count-objects -vH` 可查，约 30 MB。

---

## 七、常用检查命令

```bash
git status                       # 改动清单
git diff --cached --stat         # 即将提交的规模
git log --oneline -10            # 最近提交
git count-objects -vH            # 仓库体积
git check-ignore -v <路径>        # 查某文件被哪条规则忽略
```
