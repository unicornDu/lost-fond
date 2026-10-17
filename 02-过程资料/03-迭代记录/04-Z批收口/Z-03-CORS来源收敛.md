# Z-03（P3·后端）CORS 全放开：任意来源 + 携带凭证（配置卫生问题，预防性收敛）

> Z 批第 3 条（共 11 条）。执行前先读 `Deepseek_后端/00-索引与执行顺序.md`（JAVA_HOME 红线、验收命令）与 `Qwenwork_修改/05-修复记录-2026-10-15.md`。总览与公共红线见 `00-索引与执行顺序.md` 及本文末尾。

## 现象与证据

`config/CorsConfig.java`：

```java
config.addAllowedOriginPattern("*");
config.addAllowedHeader("*");
config.addAllowedMethod("*");
config.setAllowCredentials(true);   // 与通配 Origin 组合（修正 B：见下，当前无可利用路径，属配置卫生问题）
```

## 说明

- 部署形态是 **Nginx 同源反代**（01-交付文档/12），开发期前端走 Vite 代理（`vite.config.js` 的 `/api`、`/uploads` → 8080），两种形态下浏览器请求都是同源，**运行时并不依赖跨域放行**。
- 鉴权走 `Authorization: Bearer` 头而非 Cookie：跨站页面读不到 `localStorage` 里的 token，因此 `allowCredentials(true)` **当前没有可被利用的凭证泄露路径**——本条定性为"**无收益 + 配置卫生问题 + 答辩追问面**"；但一旦将来改成 Cookie 会话会立刻变成真实漏洞，故现在预防性收敛（修正 B）。
- NFR-4 声称"安全加固"，答辩可能被追问此配置。

## 修复要求

1. 来源列表**外置到配置**：`application.yml` 增加 `web.cors.allowed-origins`（local 默认 `http://localhost:5173`；`application-prod.yml` 留示例注释，同源部署可留空）。
2. `CorsConfig` 改为读该配置：列表为空 → 不注册跨域放行（或只放行同源）；非空 → 仅放行显式来源。
3. **去掉 `setAllowCredentials(true)`**（本项目无 Cookie 鉴权场景）；通配 `*` 不允许再与凭证共存。
4. `01-交付文档/12-部署手册` 的 Nginx 段补一句：同源反代下 CORS 不生效/无需配置。

## 验收标准

```bash
# 预检：伪造来源应拿不到 ACAO 头
curl -si -X OPTIONS http://localhost:8080/api/items -H "Origin: http://evil.com" -H "Access-Control-Request-Method: GET" | grep -i access-control-allow-origin
# → 无输出（或不含 evil.com）
# 前端 npm run dev 全流程不受影响（同源代理，本就不走 CORS）
```

## 公共红线（Z 批，违者返工）

1. `JAVA_HOME` 指 JDK21；**中文路径下禁止 `mvn spring-boot:run`**（argfile 编码假错），启动验证一律 `mvn -o -B -DskipTests package` + `java -jar target/lost-found-backend.jar`。
2. `scripts/exp_*.csv`、`01-交付文档/11` 数据表零修改（D-01）。
3. 所有文档数字改动必须与实测输出一致（宁可改小）。
4. 幂等基线：test_api.py 跑后 item=60 / item_image=120 / message 回基线；admin_log 只增不计入。
5. 不新增任何 `_*.txt`/`_*.json` 调试残留入库；git 提交由组员执行。
6. 修完在 `Qwenwork_修改/06-修复记录-2026-10-16.md` 追加对应小节（逐条修改+证据），格式沿用 01-05 修复记录。
