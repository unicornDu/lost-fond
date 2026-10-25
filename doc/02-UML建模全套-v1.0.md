# UML 建模全套 v1.0

> **交付形态说明（05 B-08，2026-10-25 更新）**：七张图以 Mermaid 源码形式交付，**导出 PNG 见仓库 `model/` 目录（图2-1~图2-7）**；类图为核心域模型，为可读性省略时间戳等次要字段。
> 交付物编号 02（杜林峰）。全部图与真实代码一一对应（类名/方法名/接口路径可检索）。Mermaid 渲染：VSCode 装 Markdown Preview Mermaid 插件或 GitHub 在线预览。

## 1. 用例图

```mermaid
graph TB
  subgraph 用户
    U((注册用户))
  end
  subgraph 管理员
    A((管理员))
  end
  U --> UC1[注册/登录 M1]
  U --> UC2[发布启事 M2]
  U --> UC3[以图搜图 M3]
  U --> UC4[检索/浏览启事 M4]
  U --> UC5[申请认领 M5]
  U --> UC6[审核认领申请 M5]
  U --> UC7[私信/查看消息 M5]
  U --> UC12[收藏启事 E-07]
  U --> UC13[账号设置/资料编辑 E-01]
  U --> UC14[查看他人主页 E-04]
  U --> UC15[下载/打开交付文件 P1]
  U --> UC17[守望订阅照片 M7]
  A --> UC8[审核启事 M6]
  A --> UC9[下架/删除启事 M6]
  A --> UC10[封禁用户 M6]
  A --> UC11[查看看板/日志/运维工具 M6]
  A --> UC16[生成统计报表 P1]
  UC2 -.「上传图片触发」.-> UC3
  UC6 -.「通过后生成」.-> UC15
  UC3 -.「搜图页开启守望」.-> UC17
  UC8 -.「招领通过反查命中」.-> UC17
```

> E/P 批补充：收藏（E-07）、账号设置（E-01）、他人主页（E-04）、交付文件（P1：认领证明/报表/匹配报告）、
> 管理端统计报表（P1）。M 批多端（桌面壳/Android/PWA）不改变业务用例，属同一用例集合的运行形态——该形态已于 2026-10-17 下线，用例本身仍由 Web 端覆盖。
> M7 补充（v2.20）：守望订阅——搜图页上传守望照片占守望位，招领启事通过审核时反查 pHash 命中并写站内通知（时序见 §7）。

## 2. 类图（核心域模型）

```mermaid
classDiagram
  class User {
    +Long id
    +String account
    +String password
    +String nickname
    +Integer role
    +Integer status
  }
  class Item {
    +Long id
    +String type
    +String title
    +String category
    +String location
    +String status
    +Integer auditStatus
    +Long userId
  }
  class ItemImage {
    +Long id
    +Long itemId
    +String imgUrl
    +Long phash
  }
  class ClaimRecord {
    +Long id
    +Long itemId
    +Long claimantId
    +String proof
    +String contact
    +String status
    +String reply
  }
  class Message {
    +Long id
    +Long userId
    +String type
    +String title
    +Integer isRead
  }
  class AdminLog {
    +Long id
    +Long adminId
    +String action
    +Long targetId
  }
  class Favorite {
    +Long id
    +Long userId
    +Long itemId
  }
  class Delivery {
    +Long id
    +Long userId
    +String bizType
    +Long bizId
    +String filePath
    +String mimeType
    +Integer status
  }
  class WatchSubscription {
    +Long id
    +Long userId
    +String planCode
    +String cycle
    +Integer status
    +LocalDateTime expireTime
  }
  class WatchTarget {
    +Long id
    +Long userId
    +Long phash
    +String label
    +String imgUrl
    +Integer status
    +Integer hitCount
    +Long lastHitItemId
  }
  User "1" --> "*" Item : 发布
  Item "1" --> "*" ItemImage : 含图(带pHash)
  Item "1" --> "*" ClaimRecord : 被申请
  User "1" --> "*" ClaimRecord : 发起
  User "1" --> "*" Message : 接收
  User "1" --> "*" AdminLog : 留痕
  User "1" --> "*" Favorite : 收藏
  Item "1" --> "*" Favorite : 被收藏
  User "1" --> "*" Delivery : 接收交付
  Item "1" --> "*" Delivery : 产出交付
  User "1" --> "1" WatchSubscription : 订阅(一人一条)
  User "1" --> "*" WatchTarget : 守望
  note for ItemImage "phash: BIGINT 存 64位指纹\n检索: BIT_COUNT(phash^q)"
  note for Delivery "业务只声明→落 delivery 表→UI 读表\n文件走受控下载/桌面 IPC 原生打开"
  note for WatchTarget "phash 与 item_image 同口径\n招领审核通过时反查命中(v2.20)"
```

服务层关键类（时序图参与者）：`AuthService` `ItemService` `SearchService` `ClaimService` `AdminService` `FileStorageService` `PHashUtil` `JwtUtil`；
E/P 批新增：`FavoriteService` `UserService`（改密/资料）`MessageService`（私信回复）`FileDeliveryService`（交付唯一入口）`PdfService`（认领证明/匹配报告）；
v2.20 新增：`WatchService`（守望订阅 / 审核通过反查命中推送）。

> 说明：E-07 收藏、P1 文件交付（delivery）对应的实体/关系已补入类图；对应的用例图补充见 §1 注记。

## 3. 以图搜图时序图（核心，M3）

```mermaid
sequenceDiagram
  participant FE as 前端 SearchImageView
  participant SC as SearchController
  participant SS as SearchService
  participant PU as PHashUtil(JImageHash)
  participant IM as ItemImageMapper
  participant DB as MySQL

  FE->>SC: POST /api/search/image (multipart file)
  SC->>SS: searchByImage(file)
  SS->>PU: hash(查询图字节)
  PU-->>SS: q (64位 pHash)
  SS->>IM: searchByHash(q, 10, 6)
  IM->>DB: SELECT item_id, MIN(BIT_COUNT(phash^q)) dist<br/>JOIN item(仅audit=1且未下架)<br/>GROUP BY item_id HAVING dist<=10 ORDER BY dist LIMIT 6
  DB-->>IM: [{itemId, dist}]
  IM-->>SS: List~ImageMatch~
  SS->>SS: 转 ItemVO + similarity=(1-dist/64)*100
  SS-->>SC: List~ItemVO~（相似度降序）
  SC-->>FE: Result{data: Top-N}
```

## 4. 认领审核时序图（M5，含竞争申请处理）

```mermaid
sequenceDiagram
  participant B as 认领人B
  participant CC as ClaimController
  participant CS as ClaimService
  participant A as 发布者A

  B->>CC: POST /items/{id}/claim {proof, contact}
  CC->>CS: apply(...)
  CS->>CS: 守卫：仅 寻找中/待认领 可申请；重复拦截
  CS-->>B: 200（物品→认领申请中，通知A）
  A->>CC: GET /claims/received
  CC-->>A: 含 申请人/凭证/联系方式（防盲审）
  A->>CC: POST /claims/{id}/audit {approve}
  CC->>CS: audit(...)
  alt 通过
    CS->>CS: 物品→已找回/已认领
    CS->>CS: 其余"审核中"申请→已拒绝+逐个通知
    CS-->>B: 消息：认领已通过
  else 拒绝
    CS->>CS: 若无其他审核中申请→回退物品状态
    CS-->>B: 消息：未通过(附理由)
  end
```

## 5. 活动图（发布一条启事）

```mermaid
graph TD
  S([开始]) --> F[填写表单+选图]
  F --> V{前端校验<br/>必填/图片≥1/≤5MB}
  V -- 不过 --> F
  V -- 通过 --> SUB[POST /api/items multipart]
  SUB --> AUTH{JWT 鉴权?}
  AUTH -- 401 --> L[跳登录页]
  AUTH -- 通过 --> MAGIC{魔数/扩展名校验}
  MAGIC -- 假图 --> E1[400 文件内容不是图片]
  MAGIC -- 真图 --> SAVE[字节落盘+生成缩略图]
  SAVE --> HASH[计算 pHash 入库]
  HASH --> P[创建 Item audit_status=0 待审核]
  P --> W{管理员审核}
  W -- 通过 --> PUB([前台可见/可搜])
  W -- 驳回 --> C([已关闭])
```

## 6. 物品状态图（M5 核心，与 ClaimService 逐分支对应）

```mermaid
stateDiagram-v2
  [*] --> 待审核: 发布
  待审核 --> 已发布: 管理员通过
  待审核 --> 已关闭: 管理员驳回
  已发布 --> 寻找中: type=lost
  已发布 --> 待认领: type=found
  寻找中 --> 认领申请中: 有人申请
  待认领 --> 认领申请中: 有人申请
  认领申请中 --> 已找回: 通过(lost)
  认领申请中 --> 已认领: 通过(found)
  认领申请中 --> 寻找中: 拒绝且无其他申请(lost)
  认领申请中 --> 待认领: 拒绝且无其他申请(found)
  寻找中 --> 已找回: 失主标记找回
  待认领 --> 已认领: 失主标记
  认领申请中 --> 已找回: 失主标记完成(lost，自动关闭其余在审申请并通知)
  认领申请中 --> 已认领: 失主标记完成(found，自动关闭其余在审申请并通知)
  寻找中 --> 已下架: 管理员下架
  待认领 --> 已下架: 管理员下架
  已找回 --> [*]
  note right of 已找回: P1 交付支路：审核通过即生成认领证明 PDF<br/>交付失主与认领人各一份（随启事删除级联清理）
  已认领 --> [*]
  已下架 --> [*]
  已关闭 --> [*]
  已找回 --> [*]: 不允许再下架/审核(Z-10/B-03守卫)
```

> 注：实现中 `已发布` 与 `寻找中/待认领` 合并为初始业务状态（audit_status 与 status 两维正交：审核维度 0/1/2，业务维度如上）。

## 7. 守望订阅时序图（M7，v2.20）

```mermaid
sequenceDiagram
  participant U as 用户（以图搜图页）
  participant WC as WatchController
  participant WS as WatchService
  participant PU as PHashUtil
  participant A as 管理员
  participant IS as ItemService(审核)
  participant MG as MessageMapper

  U->>WC: POST /api/watch/targets (file, label?)
  WC->>WS: addTarget(userId, file, label)
  WS->>WS: 校验图片 + 守望位配额（方案 quota）
  WS->>PU: hash(守望照片字节)
  PU-->>WS: 64位 pHash（与检索同口径）
  WS->>WS: 落库 watch_target（status=1，到期=保留天数）
  WS-->>U: 200 WatchTargetVO（占用 1 个守望位）

  Note over U,MG: ……等待新招领启事……

  A->>IS: POST /api/admin/items/{id}/audit approve=true（招领）
  IS->>WS: matchOnApprove(item)
  WS->>WS: 遍历生效 watch_target（排除本人/已过期）<br/>dist = min(BIT_COUNT(t.phash ^ 启事各图 phash))
  alt dist ≤ 守望人方案阈值（轻守10/长守12/众守14）
    WS->>MG: INSERT message type=watch（相似度% + 启事ID）
    WS->>WS: hit_count+1，记录 last_hit_item_id
    MG-->>U: 通知页/角标 →「查看启事」
  else dist > 阈值
    WS-->>WS: 跳过该守望位
  end
```

> 与《01-SRS》FR-7 对应；方案数值由 `WatchService.PLANS` 唯一定义，前端经 `/api/watch/plans` 读取，避免两套数字各写一遍。
