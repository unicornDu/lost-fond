# Z-07（P1·后端）`regen-thumbs` 丢失递归 → 对 `uploads/yyyy/MM` 完全空操作

> Z 批第 7 条（共 11 条，**P1 真 bug**）。执行前先读 `Deepseek_后端/索引与执行顺序.md`（JAVA_HOME 红线、验收命令）。总览与公共红线见 `索引与执行顺序.md` 及本文末尾。
> 关联：补的单测并入 `Z-04` 的用例名单（FileStorageServiceTest）。

## 现象与证据（实测）

```
调用前: uploads/2026 下文件=2  其中 _thumb=0     （uploads/2026/10/ 两张用户上传原图）
POST /api/admin/regen-thumbs → {"code":200,"data":{"generated":0}}
调用后: uploads/2026 下文件=2  其中 _thumb=0     ← 一张都没补
对照:   uploads/seed 下 _thumb=24（seed 分支是通的，所以启动日志"补生成 24 张"是真的）
```

## 根因（代码级，已定位到行）

`FileStorageService`：

```java
private int ensureThumbs(boolean includeSeed) {
    try (Stream<Path> stream = Files.list(root())) {          // L157：只列 uploads/ 下一层
        stream.filter(Files::isDirectory).forEach(p -> {
            String name = p.getFileName().toString();
            if (name.matches("\\d{4}")) dirs.add(p);           // 收下 2026
            else if (includeSeed && "seed".equals(name)) dirs.add(p);
        });
    }
    for (Path dir : dirs) {
        try (Stream<Path> files = Files.list(dir)) {          // L168：仍然只列一层
            files.filter(p -> …扩展名匹配…).forEach(…);        // uploads/2026 下只有子目录 "10" → 被过滤掉
```

- 原实现是 `Files.walk(root())`（**递归**，git 历史 `git log -L` 可证）；
- 02 批 B-08 为"把写入范围收敛到 `yyyy/MM`、不碰 `_src/_query/_exp`"而改成"先列顶层目录、再 `Files.list(dir)`"，**语义从递归变成了固定两层目录 + 文件在第三层**；
- 用户上传真实位置是 `uploads/2026/10/xxx.jpg`（目录两层、文件第三层），因此**永远扫不到**。

## 影响

- `regen-thumbs` 这个工具的用途（给历史图片补缩略图）**完全失效**；
- 启动时的 `ensureThumbs()` 只对 `seed/` 有效，对用户历史图无效（老库/新克隆都无法自愈）；
- `store()` 上传时仍会生成缩略图 → **新上传的图不受影响**，所以缺陷不容易在日常操作中暴露。

> ⚠️ **02 批的验收证据恰好掩盖了这个 bug**：当时写的是"`POST /api/admin/regen-thumbs` generated=0 且 **seed 目录文件数 55→55 不变**"。它把"seed 没被写"当成通过标准，**却没验证 `yyyy/MM` 有没有被写到**——而 `generated=0` 恰恰是这个 bug 的症状。

## 修复要求

1. 恢复递归，但保持"不碰 `_src/_query/_exp`"的收敛目标：

```java
// 对 yyyy 与 seed 两个子树做递归（深度 2 足够：yyyy/MM/*.jpg、seed/*.jpg）
Files.walk(dir, 2).filter(Files::isRegularFile).filter(p -> 非 _thumb 且扩展名匹配)…
```

   或保留 `Files.list` 但显式下钻一层（`\d{4}` → 再列其子目录 → 再列文件），二选一，写明理由。

2. **同修缩略图缓存 key（2026-10-16 复查新增，与递归恢复强耦合）**：`ensureThumbs` 内 `thumbCache.put("/uploads/" + dir.getFileName() + "/" + …)`（约 L183）只拼了**一层**目录，而真实路径是 `/uploads/yyyy/MM/xxx_thumb.jpg` 两层（对比 `store()` 路径 L79 的 key 是带 `yyyy/MM` 两层的）。当前因递归缺失该分支执行不到，**恢复递归后若不同步修 key，缓存判重会全部错配**——必须一并改为带 `yyyy/MM` 的完整相对路径。

3. **重写验收方式**（这是本条的重点，否则又会"假通过"）：

```
1) 制造缺口：找到一张没有 _thumb 的历史图（如 uploads/2026/10/<uuid>.jpg）
2) 调用 POST /api/admin/regen-thumbs（管理员令牌）
3) 断言：返回的 generated ≥ 1，且 <uuid>_thumb.jpg 出现在同目录
4) 再调一次：generated = 0（幂等，不重复生成）
```

4. 若手工删除了 `_thumb` 导致启动时会重新生成，属预期（幂等自愈），在注释里写明。
5. 补单测（并入 Z-04 的用例名单）：`FileStorageServiceTest` 加一条"临时目录下构造 `yyyy/MM/` 双层结构，调 `regenUserUploadThumbs()` 后缩略图出现在 MM 子目录且 `seed` 未被触碰"。

## 验收标准

```bash
cd backend && mvn -o -B -DskipTests package && java -jar target/lost-found-backend.jar
# 制造缺口后调用接口，验证 generated>=1 且文件出现；再调一次 generated=0
cd backend && mvn -o -B test        # 全绿（含新增的 FileStorage 双层目录用例）
```

## 公共红线（Z 批，违者返工）

1. `JAVA_HOME` 指 JDK21；**中文路径下禁止 `mvn spring-boot:run`**（argfile 编码假错），启动验证一律 `mvn -o -B -DskipTests package` + `java -jar target/lost-found-backend.jar`。
2. `scripts/exp_*.csv`、`Creator/交付文档/11` 数据表零修改（D-01）。
3. 幂等基线：test_api.py 跑后 item=60 / item_image=120 / message 回基线；admin_log 只增不计入。
4. 不新增任何 `_*.txt`/`_*.json` 调试残留入库；git 提交由组员执行。
5. **验证方式必须能证伪**：凡"生成/补全"类修复，先制造缺口、再验证缺口被填上，不能只用"没有副作用"当通过标准。
6. 修完在 `Qwenwork_修改/修复记录-2026-10-16.md` 追加对应小节（逐条修改+证据），格式沿用 01-05 修复记录。
