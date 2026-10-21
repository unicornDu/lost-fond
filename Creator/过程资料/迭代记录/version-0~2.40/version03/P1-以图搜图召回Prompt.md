# Prompt 03 · P1 以图搜图召回：标定 Top-N / 阈值，并补一个可复现的召回评测脚本

> 前置：先读同目录 `索引与执行顺序.md`（项目路径、JAVA_HOME 红线、验收命令）。
> 本 Prompt 的目标不是"改算法"，而是**把演示效果与文档口径变成可复现、可自证的东西**。

---

## 一、背景：实测召回明显低于文档描述

我用项目自带的留出查询图实测（协议：查询图 = `backend/uploads/_query/q-<物品>.jpg`；候选 = 该物品**已过审**的启事；Top-N = 6；阈值 = 10）：

| 物品 | 同族已过审启事 | 命中数 | 召回 |
|---|---|---|---|
| cup | 8 | **2** | 25% |
| keys | 8 | 6 | 75% |
| earbuds | 10 | 6 | 60% |
| card | 10 | 4 | 40% |
| umbrella | 10 | 6 | 60% |
| scarf | 10 | 6 | 60% |
| **合计** | **56** | **30** | **≈54%** |

而文档里的说法是：

- `README.md` §四："上传 `backend/uploads/_query/q-cup.jpg` → 命中该物品的**多条** lost/found 启事，相似度 ~100%"
  → 实测 **cup 只有 2 条**，与"多条"的预期不符。
- `Creator/交付文档/08-测试用例表` TC-M3-02："上传 q-cup.jpg → 命中该物品多条 lost/found ~100%"，标注"自动化 ✅"
  → `scripts/test_api.py` 里并没有这条断言（它只断言"新建的启事能被自己那张图搜到"，而且用的是同一张图必然距离 0，属同图自证）。

**关键观察**：keys / earbuds / umbrella / scarf 的命中数都**正好等于 `top-n: 6`**，而它们各有 8~10 条候选 → 说明瓶颈不是汉明距离阈值，而是 **Top-N 截断**。

---

## 二、⚠️ 必须先说清楚的事：不要动 `Creator/交付文档/11` 的实验数据

`Creator/交付文档/11-以图搜图准确率实验报告` 用的是**另一套协议**（1 张 `ref.jpg` 建索引 vs 72 张变体查询图，检索库里只有 6 个对象），对应 `HashExperimentTest` + `backend/scripts/exp_results.csv` / `exp_per_transform.csv`。

这套数据经核对：**可逐字节复现、内部自洽、没有造假**（`strong,aHash,12` 那行 `P=0.7976 < R=0.8095` 也是合法结果）。因此：

- **禁止修改** `HashExperimentTest.java`、`exp_results.csv`、`exp_per_transform.csv` 的任何数值与结论；
- 本 Prompt 引入的是**第三套协议**（真实种子库 + 留出查询图），请**新增**评测脚本与文档章节，**不要**用它去覆盖或否定 `Creator/交付文档/11`。

---

## 三、任务

### T-01 新增可复现的召回评测脚本

新建 `backend/scripts/eval_recall.py`，要求：

1. 只用标准库 + `requests`（与 `test_api.py` 保持一致，不引入新依赖）。
2. 账号与口令从 `backend/application-local.yml` 读取，**不要硬编码口令**（现有 `test_api.py` 硬编码 `Admin@2026/Demo@2026` 作为 fallback 是个反面教材，见 `05`）。读不到配置时要**明确报错退出**，不要静默用默认值。
3. 评测逻辑：
   - 遍历 `uploads/_query/q-*.jpg`；
   - 对每个物品，从 `uploads/seed/manifest.json` 推导"同族启事集合"（图片路径匹配 `seed/<物品>-`，且 `auditStatus == 1`），作为真值；
   - 调 `POST /api/search/image`，把返回的 `id` 集合与真值比对，输出 `命中/候选/召回率/最低相似度/命中id 列表`；
   - 汇总整体召回，并把结果写入 `backend/scripts/recall_results.csv`（新文件，格式自定但要能直接贴进报告）。
4. 支持命令行参数覆盖 `--threshold` 与 `--top-n`（**注意：这两个参数目前只在服务端配置里，接口不暴露**；所以脚本要么提示"请先改 `application.yml` 或传环境变量重启后端"，要么由你在 T-02 里给接口/配置加上可覆盖能力）。
5. 退出码：召回率低于给定阈值（如 `--min-recall 0.6`）时返回非 0，便于当回归门禁。

### T-02 重新标定 `top-n` 与 `hamming-threshold`

`application.yml` 现状：

```yaml
image-search:
  bit-resolution: 64
  hamming-threshold: 10
  top-n: 6
```

要求：

1. 用 T-01 的脚本做**网格扫描**（`top-n ∈ {6, 10, 20, 50}`，`threshold ∈ {8, 10, 12, 14, 16}），把召回率与结果条数记录下来。
2. 给出标定结论与**理由**，并考虑副作用：
   - `top-n` 调大 → 召回提升，但搜索页会返回更多"噪音"（相似度更低的条目），且 `ItemVO` 列表更长；
   - `threshold` 调大 → 召回提升，但误检增加（`Creator/交付文档/11` 已量化"阈值 12 开始出现跨物体误检"）；
   - 前端的展示方式是否要改（例如"只高亮 Top-3，其余折叠"）。
3. 把**最终选定值**写入 `application.yml`，并在 `backend/scripts/recall_results.csv` 里保留扫描数据作为依据。
4. 更新 `Creator/交付文档/11` 的**补充章节**（新增一节"§2.4 真实种子库端到端召回"，明确写清协议差异），不要改动原有表格。

### T-03 修正与实测不符的文案

1. `README.md` §四的演示说明：把"命中该物品多条 lost/found 启事，相似度 ~100%"改成与标定后实测一致的表述（例如"命中 N 条同物品启事，最高相似度 X%"），并为 6 张查询图各写一句实测结果。
2. `Creator/交付文档/08-测试用例表` TC-M3-02：预期结果改为实测值；"方式"列如果仍无自动化断言，必须改成"手工"或补上断言（配合 `05`）。
3. `frontend/src/views/SearchImageView.vue` 里"汉明距离均 > 阈值 10"的空结果提示文案，要在阈值变更后同步（该文案目前是硬编码的 10）。
4. `frontend/src/views/SearchImageView.vue` 的阶段动画文案"压缩至 32×32 灰度图 / DCT 变换 → 64 位 pHash 指纹"——请与 `JImageHash` 的实际实现核对后修正（`PerceptiveHash(64)` 的实际处理尺寸以库实现为准，不要写不准确的数字）。

### T-04 `SearchService` 细节修正

1. `similarTo(Long itemId, int n)` 里 `limit(Math.max(1, n))` 会把 `n=0` 变成 1 —— 语义不对，`n<=0` 应返回空列表。
2. `searchByImage(MultipartFile file)` 在 `file.getBytes()` 前没有做类型/大小前置校验（目前仅依赖 `spring.servlet.multipart.max-file-size: 5MB` 兜底）。请补：
   - 非图片 MIME/扩展名提前拒绝（复用 `FileStorageService` 的白名单常量，避免两处不一致）；
   - 明确超限时的提示文案（现有 `GlobalExceptionHandler` 已有 `MaxUploadSizeExceededException` → 413）。
3. **注意 B-01**：`similarTo` 必须校验源启事的可见性（见 `01` Prompt），本 Prompt 的改动要与它合并，不要互相覆盖。

### T-05 性能结论的复核（可选，但推荐）

`Creator/交付文档/11` 与 `Creator/交付文档/10` 声称"单次 avg 40ms、20 并发 132ms"。请用 T-01 的脚本顺带复测当前规模（120 张索引图）下的单次延迟，并：

- 如果数字仍成立，在文档里补一句"复测环境 + 时间"，让它可追溯；
- 如果数字有明显变化（比如 `top-n` 调大后变慢），必须更新文档，并说明 `BIT_COUNT(phash ^ q)` 走的是**全表扫描**（`Creator/交付文档/06` 已说明 phash 上不建索引是正确的取舍）——这也是回答"上千/上万张图怎么办"时该讲的点。

---

## 四、验收标准

1. `python scripts/eval_recall.py` 能一条命令跑完并输出表格 + 生成 `recall_results.csv`。
2. 重跑后整体召回率明显高于当前的 54%（目标：在可接受误检下达到 ≥75%，具体以你标定的理由为准），并给出前后对比表。
3. 至少 4 个物品不再被 Top-N 卡死（即命中数 < 候选数，而不是正好等于 `top-n`）。
4. `README.md`、`Creator/交付文档/08`、`Creator/交付文档/11`（新增章节）的表述与实际数字完全一致；**`exp_*.csv` 未被修改**（用 `git diff` 证明）。
5. 前端空结果/提示文案中的阈值数字与 `application.yml` 一致。

---

## 五、交付要求

1. 给出：新增/修改文件清单、`recall_results.csv` 的完整内容、网格扫描表、最终选定参数及理由。
2. 明确写出"本次标定与 `Creator/交付文档/11` 实验的协议差异"，避免答辩时被问出矛盾。
3. 如果最终结论是"Top-N 调大后体验变差、维持 6"，也可以——但必须用数据说明，并**同步修正 README/Creator/交付文档/08 的过乐观表述**（本 Prompt 的核心要求是"文档与实测一致"，不是"必须提高召回"）。
