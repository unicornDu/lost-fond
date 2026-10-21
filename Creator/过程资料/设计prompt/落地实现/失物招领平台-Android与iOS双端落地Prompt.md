# 失物招领平台 · Android 与 iPhone 双端落地 Prompt

> 用途：把本文件整体交给 AI 编码代理（ZCode / Cursor / Codex 等）作为实施指令。
> 前置文档：同目录《失物招领平台-文件交付能力落地流程.md》（本 Prompt 的第 5 节依赖其中的下载路由）。
> 项目基线：Spring Boot 3.x + MySQL 8 + Vue 3 前端 + JImageHash 以图搜图；5 人课程小组。

---

## 角色设定

你是一名移动端交付工程师，任务是把这个已有的 Vue 3 Web 应用打包为
**Android APK（必须交付）** 和 **iPhone 可安装应用（按条件降级交付）**。
你不得重写任何前端业务逻辑，不得引入新的大型框架，所有工作围绕"一套代码、多端分发"展开。

## 总原则（每一步都要遵守）

1. **一套代码三端运行**：Web、Android、iOS 共用同一个 Vue 工程，差异只出现在
   构建配置和环境变量里，禁止复制出第二份前端代码目录。
2. **不重写、不换框架**：禁止 React Native / Flutter / uni-app 迁移建议；
   走 Capacitor 套壳路线。
3. **原生能力只加不炫**：只接入相机（拍照搜图需要）和网络存储，别的插件一律不加。
4. **数据与程序分离**：登录态（JWT）存 Capacitor Preferences（原生存储），
   禁止存 localStorage；API 地址走环境变量，不硬编码。
5. 每个阶段结束时跑一遍该阶段的验收清单，全部通过才进入下一阶段。

---

## P0 · 环境与移动端适配（成员D，第 1 周，2 天）

### 任务

1. 全员统一安装：Node.js 20 LTS、Android Studio（含 SDK 34 + Platform Tools）、JDK 17（已有）。
   Windows 组员无需 Mac——**Android 打包全程可在 Windows 完成**。
2. Vue 工程做移动端适配：
   - 视口：`<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">`
   - 核心五页（发布失物 / 拍照搜图 / 物品详情 / 认领消息 / 我的）按 375px 宽度走查，
     触控目标 ≥ 44px，底部安全区用 `env(safe-area-inset-bottom)` 预留
   - 图片上传组件检查：accept/capture 属性在移动浏览器可用的降级路径
3. API 地址改为环境变量：`.env.development` 指向本机，`.env.production` 指向服务器域名。

### 验收清单

- [ ] 手机浏览器访问 Web 端，五个核心页面无横向滚动、无按钮重叠
- [ ] `npm run build` 产物在手机浏览器完整可用（登录→拍照搜图→发布）
- [ ] API 地址在 dev/prod 两套环境变量中切换正确

---

## P1 · Android APK 打包（成员D 主责，成员C 配合，第 2 周，2~3 天）

### 任务

1. 在 Vue 工程根目录集成 Capacitor：

```bash
npm i @capacitor/core @capacitor/cli
npm i @capacitor/android @capacitor/camera @capacitor/preferences
npx cap add android
```

2. `capacitor.config.ts`（关键字段）：

```ts
const config: CapacitorConfig = {
  appId: 'cn.edu.xxx.lostfound',          // 反域名格式，全班不重复即可
  appName: '校园失物招领',
  webDir: 'dist',
  server: {
    // 开发期热更新：指向本机 vite，真机调试用；发布前必须删除
    // url: 'http://192.168.x.x:5173', cleartext: true
  },
  android: { allowMixedContent: false }
};
```

3. **拍照搜图接原生相机**：上传组件改造为 Capacitor 环境检测——
   `Capacitor.isNativePlatform()` 为真时调 `Camera.getPhoto({ source: CameraSource.Prompt, quality: 70, width: 1600 })`
   （Prompt = 弹出"拍照/相册"选择，quality/width 直接压缩到 ≤5MB）；
   浏览器环境走原有 `<input type="file">`。拍完的 base64 转 Blob 走既有上传接口。
4. **登录态迁移**：封装 `tokenStorage`，Web 用 localStorage、App 用
   `Preferences.set({ key:'jwt', value })`，全工程只允许通过它读写 token。
5. **服务端配合（成员C）**：
   - 服务器启用 HTTPS（自签证书演示机手动信任，或用内网证书）
   - CORS 放行 `capacitor://localhost`（iOS）与 `https://localhost`（Android WebView 源）
   - Spring Boot 若无 HTTPS：临时方案为 APK 的
     `android:usesCleartextTraffic="true"` 仅限开发构建，发布构建必须关掉
6. **打包签名**：

```bash
keytool -genkey -v -keystore lostfound.keystore -alias lostfound -keyalg RSA -validity 10000
# android/app/build.gradle 的 signingConfigs 填入 keystore 路径与密码
cd android && ./gradlew assembleRelease   # 产物 app-release.apk
```

7. **分发**：把 APK 放到 Spring Boot 静态目录 `/downloads/app-release.apk`；
   Web 官网加"下载 Android 版"按钮。下载接口补安全响应头（防 APK 被当网页执行）：

```
Content-Type: application/vnd.android.package-archive
Content-Disposition: attachment; filename="lostfound.apk"
X-Content-Type-Options: nosniff
```

### 验收清单

- [ ] 全新 Android 手机（Android 10+）：允许"未知来源安装"后 APK 可安装、可打开
- [ ] App 内登录 → 拍照 → 以图搜图 → 发布，全流程走通
- [ ] 拍摄 8MB 原图上传后，服务器收到的文件 ≤5MB（压缩生效）
- [ ] 杀掉 App 重开，登录态还在（Preferences 生效）
- [ ] 从官网点"下载 Android 版"，下载的 APK 可直接安装（响应头正确）
- [ ] release 构建 cleartext 已关闭，HTTP 明文请求被拒绝

---

## P2 · iPhone 端（成员D + 成员A，第 3~4 周，按硬件条件三选一）

> iOS 的约束先讲清楚：打包 IPA 必须有 **Mac + Xcode**；免费 Apple ID 签名的应用
> **7 天过期**，99 美元/年的开发者账号才能长期分发（TestFlight/App Store）。
> 课程小组按手头资源选下面的方案，**方案 C 是人人可完成的保底**。

### 方案 A（有 Mac + 有开发者账号）· Capacitor iOS 真打包

1. `npm i @capacitor/ios && npx cap add ios`，Xcode 打开 `ios/App` 工程改 Bundle ID 与签名团队
2. iPhone 用数据线连 Mac，Xcode 直接 Run 到真机（免费账号可用，7 天重签一次）
3. 有付费账号则走 TestFlight：上传 IPA → 邀请组员与答辩评委安装（这是体验最好的路径）

### 方案 B（有 Mac、无付费账号）· 免费签名真机安装

- 同方案 A 前 2 步；接受"7 天过期、最多 3 台设备"的限制，
  答辩当天 morning 用 Xcode 重签一次即可，现场演示完全够用

### 方案 C（没有 Mac）· PWA 保底，iPhone 也"像装了 App"

1. Vue 工程加 PWA 支持（`vite-plugin-pwa`）：

```ts
VitePWA({
  registerType: 'autoUpdate',
  manifest: {
    name: '校园失物招领', short_name: '失物招领',
    display: 'standalone',           // 全屏运行，看起来就是 App
    theme_color: '#你们的主色',
    icons: [ { src: 'icon-192.png', sizes: '192x192' },
             { src: 'icon-512.png', sizes: '512x512' } ]
  }
})
```

2. iPhone 用 Safari 打开站点 → 分享按钮 → **"添加到主屏幕"**——得到独立图标、
   全屏启动、离线壳的"App"，**无需签名、无需 Mac、不会过期**
3. 首次访问给出安装引导浮层（检测 `navigator.standalone === false` 且 iOS UA 时显示）
4. 注意：iOS 的 PWA 不弹自定义安装横幅，必须靠引导图说明"分享→添加到主屏幕"

### 验收清单

- [ ] 选定方案在 iPhone 上完成安装/添加，桌面出现独立图标，启动无浏览器地址栏
- [ ] 登录 → 拍照搜图 → 发布 全流程可用（iOS 相机权限弹窗已处理拒绝场景）
- [ ] （方案 C）断网时已缓存页面可打开，联网后数据恢复
- [ ] 官网/文档写清 iPhone 安装指引截图（答辩演示用）

---

## P3 · 收尾（成员E + 成员A，随主排期第 4 周）

1. **测试用例补充（成员E）**：双端矩阵——Android 10/12/14 各一台模拟器或真机、
   iPhone（真机或方案 C 的 Safari）；用例含弱网发布、 token 过期重登、
   相机权限拒绝后的降级路径、APK 覆盖安装
2. **文档（成员B）**：需求说明书补"移动端"非功能需求（安装包 ≤30MB、
   冷启动 ≤3s）；部署手册补 APK 分发与 HTTPS 配置章节
3. **答辩准备（成员A）**：演示机预装 App + 官网下载页投屏；
   话术要点——"一套 Vue 代码三端运行；iOS 受签名机制约束采用 PWA 方案，
   这是成本与合规的工程权衡；了解上架 TestFlight 的完整路径，评估后不引入"

---

## 明确不做的事（防止范围膨胀）

- 不上架应用商店（审核周期与企业资质都不具备）
- 不做推送通知（国内厂商通道各自为政，成本远超课设收益；站内消息已有）
- 不做 APK 自动更新管道（覆盖安装即可；若前端是 WebView 加载远程页，改前端无需重发包）
- 不引入原生页面混合开发（Kotlin/Swift 一行都不写）
