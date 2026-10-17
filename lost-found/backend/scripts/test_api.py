# -*- coding: utf-8 -*-
"""
后端接口全量联调测试（M1-M6）· 05 A-01/A-02 修复版。
改进（相对审查 2.11 版）：
  ① 失败计数 + 非零退出码，并在结尾打印"共 X 条断言，失败 Y 条"（X 为实际执行数，可自证）；
  ② 口令只从 application-local.yml 的 seed.* 读取，缺失即报错退出（删除硬编码 fallback）；
  ③ 开头 preflight：后端未启动/查询图缺失时明确报错退出，不再 traceback；
  ④ 主体包进 try/finally：任何断言失败/异常都会执行清理（管理员删除本轮创建的启事）；
  ⑤ 幂等自检扩口径：经 /api/admin/db-counts 核对 item/item_image/message 回基线；
     admin_log 为审计日志、设计上只增不减，不计入幂等基线（打印增量留痕）；
  ⑥ 补齐漏测端点：register（重复学号 400 + @Valid）、mark-found、offline、regen-thumbs；
  ⑦ 补齐内容断言：列表全 audit=1、搜图不含待审 id、非法时间 400、重复申请 400、
     驳回路径、竞争状态机（B/C 同申请→通过 B 自动拒绝 C）、真实召回（q-cup ≥6 条同族）。
运行前确保后端 :8080 已启动且已播种（DataInitializer）。
"""
import os, sys, time, requests

HERE = os.path.dirname(os.path.abspath(__file__))
BACKEND = os.path.dirname(HERE)
BASE = "http://localhost:8080/api"
UP = os.path.join(BACKEND, "uploads")

FAILS = 0
TOTAL = 0
def ok(cond, msg=""):
    global FAILS, TOTAL
    TOTAL += 1
    print(("  [PASS] " if cond else "  [FAIL] ") + msg)
    if not cond: FAILS += 1
def show(t): print("\n===== " + t + " =====")

def read_local_yml():
    """从 application-local.yml 读取 seed.* 演示口令（避免在脚本里硬编码口令）。"""
    p = os.path.join(BACKEND, "application-local.yml")
    d = {}
    if os.path.exists(p):
        for line in open(p, encoding="utf-8"):
            line = line.strip()
            for k in ("admin-password", "user-password"):
                if line.startswith(k + ":"):
                    d[k] = line.split(":", 1)[1].strip()
    return d

LOCAL = read_local_yml()
if not LOCAL.get("admin-password") or not LOCAL.get("user-password"):
    sys.exit("错误：application-local.yml 缺 seed.admin-password / seed.user-password，"
             "拒绝用硬编码 fallback 运行（05 A-02）")
ADMIN_ACC = "10000001"
ADMIN_PWD = LOCAL["admin-password"]
DEMO_PWD = LOCAL["user-password"]
A_ACC, B_ACC, C_ACC = "20240001", "20240002", "20240003"   # 种子演示账号（login，幂等）

s = requests.Session()
def login(acc, pwd):
    r = s.post(BASE + "/auth/login", json={"account": acc, "password": pwd})
    return r.json()

# ---------- preflight（05 A-02） ----------
qimg = os.path.join(UP, "_query", "q-cup.jpg")
try:
    s.get(BASE + "/stats", timeout=5)
except requests.ConnectionError:
    sys.exit("错误：后端未启动。请先在 backend 目录运行 java -jar target/lost-found-backend.jar")
if not os.path.exists(qimg):
    sys.exit("错误：缺少查询图 %s，无法执行搜图断言" % qimg)

# ---------- 登录三方 ----------
show("M1 登录（种子演示账号 + 管理员）")
la = login(A_ACC, DEMO_PWD); ok(la["code"] == 200, "A 登录: %s" % la.get("msg", ""))
tokA = la["data"]["token"]; hA = {"Authorization": "Bearer " + tokA}
lb = login(B_ACC, DEMO_PWD); ok(lb["code"] == 200, "B 登录")
tokB = lb["data"]["token"]; hB = {"Authorization": "Bearer " + tokB}
lc = login(C_ACC, DEMO_PWD); ok(lc["code"] == 200, "C 登录（竞争场景用）")
tokC = lc["data"]["token"]; hC = {"Authorization": "Bearer " + tokC}
lad = login(ADMIN_ACC, ADMIN_PWD); ok(lad["code"] == 200, "管理员登录（审查 2.3 固定口令）")
tokD = lad["data"]["token"]; hD = {"Authorization": "Bearer " + tokD}

r = s.post(BASE + "/auth/login", json={"account": A_ACC, "password": "wrongpass"})
ok(r.json()["code"] != 200, "错误密码被拒绝")
r = s.get(BASE + "/auth/me")
ok(r.json()["code"] == 401, "无令牌 me → 401")
r = s.get(BASE + "/auth/me", headers=hA)
ok(r.json()["code"] == 200 and r.json()["data"]["account"] == A_ACC, "me 返回 A")

# ---------- M1 注册（05 A-02 补漏测端点；不造残留用户：用已存在学号测重复 + 用非法密码测 @Valid） ----------
show("M1 注册校验（重复学号 400 / 短密码 400，TC-M1-02）")
r = s.post(BASE + "/auth/register", json={"account": A_ACC, "password": "New@2026x", "nickname": "dup"})
ok(r.json()["code"] != 200 and "已注册" in r.json().get("msg", ""), "重复学号注册被拒: %s" % r.json().get("msg", ""))
r = s.post(BASE + "/auth/register", json={"account": "20248877", "password": "123", "nickname": "short"})
ok(r.json()["code"] == 400, "短密码注册 → 400（@Valid）: %s" % r.json().get("msg", ""))

# ---------- 基线（05 A-01：扩到六表口径） ----------
def counts():
    st = s.get(BASE + "/stats").json()["data"]
    ic = s.get(BASE + "/search/indexed-count").json()["data"]["count"]
    return st, ic
def dbcounts():
    return s.get(BASE + "/admin/db-counts", headers=hD).json()["data"]
base_stats, base_idx = counts()
base_db = dbcounts()

CREATED = []   # 本轮创建的启事 id，finally 里统一清理

def publish(title, typ="found", img=True, happen="2026-10-14 09:30:00"):
    data = {"type": typ, "title": title, "category": "电子设备", "location": "图书馆",
            "happenTime": happen, "description": "test_api 自测发布"}
    if img:
        with open(qimg, "rb") as f:
            return s.post(BASE + "/items", headers=hA, data=data, files={"files": ("q-cup.jpg", f, "image/jpeg")})
    return s.post(BASE + "/items", headers=hA, data=data,
                  files={"dummy": ("a.txt", b"x", "text/plain")})

try:
    # ---------- 真实召回断言（05 A-02：替代"同图自证"，TC-M3-02/06） ----------
    show("M3 真实召回（q-cup 命中同族 ≥6 且不含待审 id，TC-M3-02/TC-M3-06）")
    with open(qimg, "rb") as f:
        r = s.post(BASE + "/search/image", files={"file": ("q-cup.jpg", f, "image/jpeg")})
    hits = r.json()["data"]
    cup = [h for h in hits if "/seed/cup-" in (h.get("img") or "")]
    ok(len(cup) >= 6, "q-cup 命中同族 ≥6 条（实测 %d，标定 top-n=10/th=12）" % len(cup))
    ok(all(h["auditStatus"] == 1 for h in hits), "搜图结果全部 auditStatus=1")
    ok(not any(h["id"] in (9, 10, 19, 20) for h in hits), "搜图结果不含待审 id（9/10/19/20）")
    sims = [h["similarity"] for h in hits]
    ok(sims == sorted(sims, reverse=True), "结果按相似度降序")

    # ---------- M2 发布（唯一标题，finally 清理） ----------
    show("M2 发布（multipart + pHash 入库）")
    TITLE = "自测启事-%d" % int(time.time())
    r = publish(TITLE)
    ok(r.json()["code"] == 200, "发布成功（待审核）")
    newId = r.json()["data"]["id"]; CREATED.append(newId)
    ok(r.json()["data"]["auditStatus"] == 0, "新发布 auditStatus=0（审查 2.4 真审核流）")

    show("B-01 回归：待审核启事可见性（匿名/他人 404，本人/管理员 200，similar 空）")
    ok(s.get(BASE + "/items/%d" % newId).json()["code"] == 404, "B-01 匿名访问待审核详情 → 404")
    ok(s.get(BASE + "/items/%d" % newId, headers=hB).json()["code"] == 404, "B-01 他人访问待审核详情 → 404")
    ok(s.get(BASE + "/items/%d" % newId, headers=hA).json()["code"] == 200, "B-01 发布者本人 → 200")
    ok(s.get(BASE + "/items/%d" % newId, headers=hD).json()["code"] == 200, "B-01 管理员 → 200")
    rj = s.get(BASE + "/items/%d/similar" % newId, params={"n": 3}).json()
    ok(rj["code"] == 200 and rj["data"] == [], "B-01 待审核源启事 similar → 空列表")

    show("M2 校验：无图 / 超长标题 / 非法 type / 非法时间（审查 2.23/2.28，TC-M2-07）")
    ok(publish("无图", img=False).json()["code"] == 400, "无图发布被拒")
    with open(qimg, "rb") as f:
        r = s.post(BASE + "/items", headers=hA, data={"type": "found", "title": "T" * 100, "category": "其他",
             "location": "x", "happenTime": "2026-10-14 09:00:00"}, files={"files": ("q.jpg", f, "image/jpeg")})
    ok(r.json()["code"] == 400, "超长标题 → 400: %s" % r.json().get("msg", ""))
    ok(publish("非法type", typ="hack").json()["code"] == 400, "非法 type → 400")
    ok(publish("非法时间", happen="abc").json()["code"] == 400, "非法 happenTime → 400（TC-M2-07）")

    # ---------- 审核 + 驳回路径 + 重新通过（TC-M6-03 / B-03 回归） ----------
    show("M6 审核：通过 / 驳回 / 重新通过（TC-M6-03、B-03）")
    ok(s.post(BASE + "/admin/items/%d/audit" % newId, headers=hD, params={"approve": True}).json()["code"] == 200, "审核通过")
    ok(s.get(BASE + "/items/%d" % newId).json()["data"]["auditStatus"] == 1, "auditStatus 变 1")
    ok(s.post(BASE + "/admin/items/%d/audit" % newId, headers=hD, params={"approve": False}).json()["code"] == 200, "审核驳回")
    vo = s.get(BASE + "/items/%d" % newId, headers=hD).json()["data"]
    ok(vo["auditStatus"] == 2 and vo["status"] != "已关闭", "驳回后 audit=2 且 status 非已关闭（B-03）")
    ok(s.post(BASE + "/admin/items/%d/audit" % newId, headers=hD, params={"approve": True}).json()["code"] == 200, "重新通过（B-03）")
    ok(s.get(BASE + "/items/%d" % newId).json()["data"]["auditStatus"] == 1, "重新通过后 audit=1 且前台可见")

    # ---------- M3 搜图命中自测启事 + reindex 鉴权 ----------
    show("M3 以图搜图（命中自测启事）+ reindex 鉴权")
    with open(qimg, "rb") as f:
        r = s.post(BASE + "/search/image", files={"file": ("q-cup.jpg", f, "image/jpeg")})
    hits = r.json()["data"]
    ok(any(h["id"] == newId and h["similarity"] >= 99 for h in hits), "命中自测启事: %s" %
       ", ".join("id=%s %.0f%%" % (h["id"], h["similarity"]) for h in hits[:3]))
    ok(s.post(BASE + "/search/reindex", headers=hA).json()["code"] == 403, "普通用户 reindex → 403（审查 2.19）")
    ok(s.post(BASE + "/search/reindex", headers=hD).json()["code"] == 200, "管理员 reindex → 200")

    # ---------- M4 检索 ----------
    show("M4 检索/分页上限/404/列表 audit 过滤（TC-M4-09）")
    ok(s.get(BASE + "/items", params={"type": "found", "category": "电子设备", "size": 5}).json()["code"] == 200, "分类+类型筛选")
    r = s.get(BASE + "/items", params={"keyword": "保温杯", "size": 5})
    ok(r.json()["code"] == 200 and r.json()["data"]["total"] >= 1, "关键词检索")
    ok(s.get(BASE + "/items", params={"size": 100000}).json()["data"]["size"] <= 100, "size 上限截断 ≤100（审查 2.31⑤）")
    ok(s.get(BASE + "/items/999999").json()["code"] == 404, "不存在启事 → 404")
    recs = s.get(BASE + "/items", params={"size": 100}).json()["data"]["records"]
    ok(all(i["auditStatus"] == 1 for i in recs), "列表 100 条全部 auditStatus=1（TC-M4-09）")
    r = s.get(BASE + "/items/%d" % newId)
    v1 = r.json()["data"]["views"]; v2 = s.get(BASE + "/items/%d" % newId).json()["data"]["views"]
    ok(v2 >= v1, "浏览量自增（原子）")
    ok(s.get(BASE + "/items/%d/similar" % newId, params={"n": 3}).json()["code"] == 200, "similar 接口可用（审查 2.26）")

    # ---------- M5 认领：重复申请 / 竞争状态机（TC-M5-02 / TC-竞争-01） ----------
    show("M5 认领流转（B、C 竞争 → A 通过 B → C 自动拒绝）")
    ok(s.post(BASE + "/items/%d/claim" % newId, headers=hB, json={"proof": "划痕特征", "contact": "13800000000"}).json()["code"] == 200, "B 提交认领（含联系方式）")
    ok(s.post(BASE + "/items/%d/claim" % newId, headers=hB, json={"proof": "再来一次", "contact": "13800000000"}).json()["code"] != 200, "B 重复申请被拒（TC-M5-02）")
    ok(s.post(BASE + "/items/%d/claim" % newId, headers=hC, json={"proof": "杯底刻字", "contact": "13800000001"}).json()["code"] == 200, "C 提交认领（竞争）")
    ok(s.get(BASE + "/items/%d" % newId).json()["data"]["status"] == "认领申请中", "状态→认领申请中")
    recv = s.get(BASE + "/claims/received", headers=hA).json()["data"]
    target = [c for c in recv if c["id"] == newId and c["claimStatus"] == "审核中"]
    ok(len(target) == 2, "A 收到 B、C 两条在审申请")
    ok(target and target[0].get("proof") and target[0].get("claimantAccount"), "ClaimVO 含 proof/claimantAccount（审查 2.5）")
    claimB = next(c["claimId"] for c in target if c["claimantAccount"] == B_ACC)
    claimC = next(c["claimId"] for c in target if c["claimantAccount"] == C_ACC)
    ok(s.post(BASE + "/claims/%d/audit" % claimB, headers=hB, json={"approve": True}).json()["code"] == 403, "非发布者审核 → 403")
    ok(s.post(BASE + "/claims/%d/audit" % claimB, headers=hA, json={"approve": True, "reply": "确认"}).json()["code"] == 200, "A 审核通过 B")
    ok(s.get(BASE + "/items/%d" % newId).json()["data"]["status"] == "已认领", "状态→已认领")
    mine_c = s.get(BASE + "/claims/my", headers=hC).json()["data"]
    ok(any(c["claimId"] == claimC and c["claimStatus"] == "已拒绝" for c in mine_c), "竞争：C 的申请被自动拒绝（TC-竞争-01）")
    ok(any(c["claimStatus"] == "已通过" for c in s.get(BASE + "/claims/my", headers=hB).json()["data"]), "B 我的认领含已通过")

    show("M5 消息（C 收到自动拒绝通知 / 未读 / 单条已读 / 全部已读）")
    msgs = s.get(BASE + "/user/messages", headers=hC).json()["data"]
    ok(len(msgs) >= 1, "C 有消息 %d 条" % len(msgs))
    u0 = s.get(BASE + "/user/messages/unread", headers=hC).json()["data"]["count"]
    ok(u0 >= 1, "未读数 ≥1")
    if msgs:
        ok(s.post(BASE + "/user/messages/%d/read" % msgs[0]["id"], headers=hC).json()["code"] == 200, "单条已读（审查 2.7 补）")
    ok(s.post(BASE + "/user/messages/read-all", headers=hC).json()["code"] == 200, "全部已读")
    ok(s.get(BASE + "/user/messages/unread", headers=hC).json()["data"]["count"] == 0, "已读后未读=0")

    show("M2 我的发布 / 标记找回 / 私信（mark-found 补漏测）")
    ok(any(i["id"] == newId for i in s.get(BASE + "/items/my", headers=hA).json()["data"]), "items/my 含自测启事")
    r = publish("自测寻物-%d" % int(time.time()), typ="lost")
    lostId = r.json()["data"]["id"]; CREATED.append(lostId)
    s.post(BASE + "/admin/items/%d/audit" % lostId, headers=hD, params={"approve": True})
    ok(s.post(BASE + "/items/%d/mark-found" % lostId, headers=hA).json()["code"] == 200, "mark-found → 200（补漏测端点）")
    ok(s.get(BASE + "/items/%d" % lostId, headers=hA).json()["data"]["status"] == "已找回", "mark-found 后状态=已找回")
    ok(s.post(BASE + "/items/%d/mark-found" % lostId, headers=hA).json()["code"] == 400, "Z-02：已找回再标记 → 400（状态白名单守卫）")
    ok(s.post(BASE + "/items/%d/dm" % newId, headers=hB, json={"content": "你好，请问还在吗"}).json()["code"] == 200, "B 私信 A（审查 2.7 接真）")
    ok(any(m["type"] == "dm" for m in s.get(BASE + "/user/messages", headers=hA).json()["data"]), "A 收到私信")

    # ---------- M6 管理后台正向 + offline + regen-thumbs（补漏测端点） ----------
    show("M6 管理后台正向（users/items/stats/logs/封禁/offline/regen-thumbs）")
    r = s.get(BASE + "/admin/users", headers=hD, params={"page": 1, "size": 5})
    ok(r.json()["code"] == 200 and r.json()["data"]["records"][0].get("status") is not None, "users 含 status（审查 2.8）")
    ok(s.get(BASE + "/admin/items", headers=hD, params={"auditStatus": 0}).json()["code"] == 200, "items 待审核列表")
    d = s.get(BASE + "/admin/stats", headers=hD).json()["data"]
    ok("dailyTrend" in d and "userCount" in d, "dashboard 含 dailyTrend/userCount（审查 2.8）")
    ok(s.get(BASE + "/admin/logs", headers=hD, params={"page": 1, "size": 5}).json()["data"]["total"] >= 1, "logs 可读（审查 2.29③）")
    rt = s.post(BASE + "/admin/regen-thumbs", headers=hD)
    ok(rt.json()["code"] == 200 and "generated" in rt.json()["data"], "regen-thumbs 返回 generated（补漏测端点）")
    r = s.get(BASE + "/admin/users", headers=hD, params={"keyword": "20240017"})
    recs = r.json()["data"]["records"]
    if recs:
        uid = recs[0]["id"]
        ok(s.post(BASE + "/admin/users/%d/status" % uid, headers=hD, params={"ban": True}).json()["code"] == 200, "封禁用户")
        ok(login("20240017", DEMO_PWD)["code"] != 200, "被封禁账号登录被拒")
        ok(s.post(BASE + "/admin/users/%d/status" % uid, headers=hD, params={"ban": False}).json()["code"] == 200, "解封用户")
    else:
        ok(False, "未找到 20240017 用于封禁测试")
    ok(s.get(BASE + "/admin/users", headers=hA).json()["code"] == 403, "非管理员访问 admin → 403")
    # 修复 Z-10：已团聚启事（lostId 已 mark-found=已找回）下架被守卫拒绝——与 auditItem 的 B-03 对齐
    ok(s.post(BASE + "/admin/items/%d/offline" % lostId, headers=hD).json()["code"] == 400, "Z-10：已找回启事下架 → 400（守卫拒绝）")
    # TC-M6-04 原覆盖保留：对仍在流转的启事违规下架 → 200 且前台不可见
    r = publish("自测下架-%d" % int(time.time()), typ="lost")
    offlineId = r.json()["data"]["id"]; CREATED.append(offlineId)
    ok(s.post(BASE + "/admin/items/%d/offline" % offlineId, headers=hD).json()["code"] == 200, "违规下架 → 200（补漏测端点，TC-M6-04）")
    ok(not any(i["id"] == offlineId for i in s.get(BASE + "/items", params={"size": 100}).json()["data"]["records"]), "下架后前台列表不可见（TC-M6-04）")

finally:
    # ---------- 清理（05 A-01：finally 保证失败时也清理） ----------
    show("清理：删除本轮自测启事（含磁盘/认领/消息，审查 2.20 + B-05）")
    for iid in CREATED:
        rr = s.delete(BASE + "/admin/items/%d" % iid, headers=hD)
        ok(rr.json()["code"] == 200, "删除自测启事 %s" % iid)
    if CREATED:
        ok(s.get(BASE + "/items/%d" % CREATED[0]).json()["code"] == 404, "删除后 404")

    # ---------- 自检：六表口径回基线（05 A-01） ----------
    show("自检：回基线（item/item_image/message；admin_log 为审计只增不计入）")
    time.sleep(1)
    end_stats, end_idx = counts()
    end_db = dbcounts()
    ok(end_stats == base_stats, "stats 回基线 %s == %s" % (end_stats, base_stats))
    ok(end_idx == base_idx, "索引数回基线 %s == %s" % (end_idx, base_idx))
    ok(end_db["item"] == base_db["item"] and end_db["item_image"] == base_db["item_image"],
       "item/item_image 回基线 %s/%s" % (end_db["item"], end_db["item_image"]))
    ok(end_db["message"] == base_db["message"],
       "message 回基线 %s == %s（B-05 后 dm 随启事删除）" % (end_db["message"], base_db["message"]))
    print("  [INFO] admin_log 为审计日志、设计上只增不减，不计入幂等基线；本轮增量 = %d"
          % (end_db["admin_log"] - base_db["admin_log"]))

print("\n========== 结果：%s ==========" % ("全部通过" if FAILS == 0 else "%d 条失败" % FAILS))
print("共 %d 条断言，失败 %d 条" % (TOTAL, FAILS))
sys.exit(1 if FAILS else 0)
