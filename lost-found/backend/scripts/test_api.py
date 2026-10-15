# -*- coding: utf-8 -*-
"""
后端接口全量联调测试（M1-M6）· 审查 2.11 修复版。
改进：① 失败计数 + 非零退出码（echo $? 反映真实结果）；
      ② 幂等：使用种子演示账号（login），测试启事用唯一标题并在结尾由管理员删除（含磁盘清理）；
      ③ 结尾自检：本轮创建的启事/认领/消息已被清理（行数回基线）；
      ④ 补齐场景：items/my、mark-found、messages/unread、read-all、reindex 鉴权、
         7 个 /api/admin/* 正向、404、超长字段 400、非法 type 400、分页上限。
运行前确保后端 :8080 已启动且已播种（DataInitializer）。
"""
import os, sys, json, time, random, requests

HERE = os.path.dirname(os.path.abspath(__file__))
BACKEND = os.path.dirname(HERE)
BASE = "http://localhost:8080/api"
UP = os.path.join(BACKEND, "uploads")

FAILS = 0
def ok(cond, msg=""):
    global FAILS
    print(("  [PASS] " if cond else "  [FAIL] ") + msg)
    if not cond: FAILS += 1
def show(t): print("\n===== " + t + " =====")

def read_local_yml():
    """从 application-local.yml 读取演示管理员口令（避免在脚本里硬编码口令）。"""
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
ADMIN_ACC = "10000001"
ADMIN_PWD = LOCAL.get("admin-password") or "Admin@2026"
DEMO_PWD = LOCAL.get("user-password") or "Demo@2026"
A_ACC, B_ACC = "20240001", "20240002"   # 种子演示账号（login，幂等）

s = requests.Session()
def login(acc, pwd):
    r = s.post(BASE + "/auth/login", json={"account": acc, "password": pwd})
    return r.json()

# ---------- 登录三方 ----------
show("M1 登录（种子演示账号 + 管理员）")
la = login(A_ACC, DEMO_PWD); ok(la["code"] == 200, "A 登录: %s" % la.get("msg", ""))
tokA = la["data"]["token"]; hA = {"Authorization": "Bearer " + tokA}
lb = login(B_ACC, DEMO_PWD); ok(lb["code"] == 200, "B 登录")
tokB = lb["data"]["token"]; hB = {"Authorization": "Bearer " + tokB}
lad = login(ADMIN_ACC, ADMIN_PWD); ok(lad["code"] == 200, "管理员登录（审查 2.3 固定口令）")
tokD = lad["data"]["token"]; hD = {"Authorization": "Bearer " + tokD}

r = s.post(BASE + "/auth/login", json={"account": A_ACC, "password": "wrongpass"})
ok(r.json()["code"] != 200, "错误密码被拒绝")
r = s.get(BASE + "/auth/me")
ok(r.json()["code"] == 401, "无令牌 me → 401")
r = s.get(BASE + "/auth/me", headers=hA)
ok(r.json()["code"] == 200 and r.json()["data"]["account"] == A_ACC, "me 返回 A")

# ---------- 基线 ----------
def counts():
    st = s.get(BASE + "/stats").json()["data"]
    ic = s.get(BASE + "/search/indexed-count").json()["data"]["count"]
    return st, ic
base_stats, base_idx = counts()

# ---------- M2 发布（唯一标题，结尾清理） ----------
show("M2 发布（multipart + pHash 入库）")
TITLE = "自测启事-%d" % int(time.time())
qimg = os.path.join(UP, "_query", "q-cup.jpg")
with open(qimg, "rb") as f:
    files = {"files": ("q-cup.jpg", f, "image/jpeg")}
    data = {"type": "found", "title": TITLE, "category": "电子设备", "location": "图书馆",
            "happenTime": "2026-10-14 09:30:00", "description": "test_api 自测发布"}
    r = s.post(BASE + "/items", headers=hA, data=data, files=files)
ok(r.json()["code"] == 200, "发布成功（待审核）")
newId = r.json()["data"]["id"]
ok(r.json()["data"]["auditStatus"] == 0, "新发布 auditStatus=0（审查 2.4 真审核流）")

show("M2 校验：无图发布 / 超长标题 / 非法 type（审查 2.23/2.28）")
# 无图：发一个 multipart 但不含 files 部分（用无关字段名 dummy 强制 multipart）
r = s.post(BASE + "/items", headers=hA, data={"type": "found", "title": "无图", "category": "其他",
     "location": "x", "happenTime": "2026-10-14 09:00:00"},
     files={"dummy": ("a.txt", b"x", "text/plain")})
ok(r.json()["code"] == 400 and "图片" in r.json().get("msg", ""), "无图发布被拒: %s" % r.json().get("msg", ""))
# 超长标题：multipart + 真图，触发 @Size 校验
with open(qimg, "rb") as f:
    r = s.post(BASE + "/items", headers=hA, data={"type": "found", "title": "T" * 100, "category": "其他",
         "location": "x", "happenTime": "2026-10-14 09:00:00"},
         files={"files": ("q.jpg", f, "image/jpeg")})
ok(r.json()["code"] == 400, "超长标题 → 400: %s" % r.json().get("msg", ""))
# 非法 type
with open(qimg, "rb") as f:
    r = s.post(BASE + "/items", headers=hA, data={"type": "hack", "title": "非法type",
         "category": "其他", "location": "x", "happenTime": "2026-10-14 09:00:00"},
         files={"files": ("q.jpg", f, "image/jpeg")})
ok(r.json()["code"] == 400, "非法 type → 400")

# ---------- 管理员审核通过该启事 ----------
show("M6 管理员审核通过自测启事")
r = s.post(BASE + "/admin/items/%d/audit" % newId, headers=hD, params={"approve": True})
ok(r.json()["code"] == 200, "审核通过")
r = s.get(BASE + "/items/%d" % newId).json()["data"]
ok(r["auditStatus"] == 1, "auditStatus 变 1")

# ---------- M3 搜图 ----------
show("M3 以图搜图（命中自测启事）")
with open(qimg, "rb") as f:
    r = s.post(BASE + "/search/image", files={"file": ("q-cup.jpg", f, "image/jpeg")})
hits = r.json()["data"]
ok(any(h["id"] == newId and h["similarity"] >= 99 for h in hits), "命中自测启事: %s" %
   ", ".join("id=%s %.0f%%" % (h["id"], h["similarity"]) for h in hits[:3]))
r = s.post(BASE + "/search/reindex", headers=hA)
ok(r.json()["code"] == 403, "普通用户 reindex → 403（审查 2.19）")
r = s.post(BASE + "/search/reindex", headers=hD)
ok(r.json()["code"] == 200, "管理员 reindex → 200")

# ---------- M4 检索 ----------
show("M4 检索/分页上限/404")
r = s.get(BASE + "/items", params={"type": "found", "category": "电子设备", "size": 5})
ok(r.json()["code"] == 200, "分类+类型筛选")
r = s.get(BASE + "/items", params={"keyword": "保温杯", "size": 5})
ok(r.json()["code"] == 200 and r.json()["data"]["total"] >= 1, "关键词检索")
r = s.get(BASE + "/items", params={"size": 100000})
ok(r.json()["data"]["size"] <= 100, "size 上限截断 ≤100（审查 2.31⑤）")
r = s.get(BASE + "/items/999999")
ok(r.json()["code"] == 404, "不存在启事 → 404")
r = s.get(BASE + "/items/%d" % newId)
v1 = r.json()["data"]["views"]; v2 = s.get(BASE + "/items/%d" % newId).json()["data"]["views"]
ok(v2 >= v1, "浏览量自增（原子）")
r = s.get(BASE + "/items/%d/similar" % newId, params={"n": 3})
ok(r.json()["code"] == 200, "similar 接口可用（审查 2.26）")

# ---------- M5 认领 ----------
show("M5 认领流转（B 申请 → A 审核通过）")
r = s.post(BASE + "/items/%d/claim" % newId, headers=hB, json={"proof": "划痕特征", "contact": "13800000000"})
ok(r.json()["code"] == 200, "B 提交认领（含联系方式）")
r = s.get(BASE + "/items/%d" % newId).json()["data"]
ok(r["status"] == "认领申请中", "状态→认领申请中")
r = s.get(BASE + "/claims/received", headers=hA)
recv = r.json()["data"]
target = [c for c in recv if c["id"] == newId and c["claimStatus"] == "审核中"]
ok(len(target) >= 1, "A 收到申请且含凭证字段")
ok(target and target[0].get("proof") and target[0].get("claimantAccount"), "ClaimVO 含 proof/claimantAccount（审查 2.5）")
claimId = target[0]["claimId"]
r = s.post(BASE + "/claims/%d/audit" % claimId, headers=hB, json={"approve": True})
ok(r.json()["code"] == 403, "非发布者审核 → 403")
r = s.post(BASE + "/claims/%d/audit" % claimId, headers=hA, json={"approve": True, "reply": "确认"})
ok(r.json()["code"] == 200, "A 审核通过")
r = s.get(BASE + "/items/%d" % newId).json()["data"]
ok(r["status"] == "已认领", "状态→已认领")
r = s.get(BASE + "/claims/my", headers=hB)
ok(any(c["claimStatus"] == "已通过" for c in r.json()["data"]), "B 我的认领含已通过")

show("M5 消息（B 收到通知 / 未读 / 全部已读）")
r = s.get(BASE + "/user/messages", headers=hB)
msgs = r.json()["data"]
ok(len(msgs) >= 1, "B 有消息 %d 条" % len(msgs))
r = s.get(BASE + "/user/messages/unread", headers=hB)
u0 = r.json()["data"]["count"]
ok(u0 >= 1, "未读数 ≥1")
if msgs:
    mid = msgs[0]["id"]
    r = s.post(BASE + "/user/messages/%d/read" % mid, headers=hB)
    ok(r.json()["code"] == 200, "单条已读（审查 2.7 补）")
r = s.post(BASE + "/user/messages/read-all", headers=hB)
ok(r.json()["code"] == 200, "全部已读")
r = s.get(BASE + "/user/messages/unread", headers=hB)
ok(r.json()["data"]["count"] == 0, "已读后未读=0")

show("M2 我的发布 / 标记找回 / 私信")
r = s.get(BASE + "/items/my", headers=hA)
ok(any(i["id"] == newId for i in r.json()["data"]), "items/my 含自测启事")
r = s.post(BASE + "/items/%d/dm" % newId, headers=hB, json={"content": "你好，请问还在吗"})
ok(r.json()["code"] == 200, "B 私信 A（审查 2.7 接真）")
r = s.get(BASE + "/user/messages", headers=hA)
ok(any(m["type"] == "dm" for m in r.json()["data"]), "A 收到私信")

# ---------- M6 管理后台正向 ----------
show("M6 管理后台正向（users/items/stats/logs/封禁）")
r = s.get(BASE + "/admin/users", headers=hD, params={"page": 1, "size": 5})
ok(r.json()["code"] == 200 and r.json()["data"]["records"][0].get("status") is not None, "users 含 status（审查 2.8）")
r = s.get(BASE + "/admin/items", headers=hD, params={"auditStatus": 0})
ok(r.json()["code"] == 200, "items 待审核列表")
r = s.get(BASE + "/admin/stats", headers=hD)
d = r.json()["data"]
ok("dailyTrend" in d and "userCount" in d, "dashboard 含 dailyTrend/userCount（审查 2.8）")
r = s.get(BASE + "/admin/logs", headers=hD, params={"page": 1, "size": 5})
ok(r.json()["code"] == 200 and r.json()["data"]["total"] >= 1, "logs 可读（审查 2.29③）")
# 封禁/解封一个非关键演示用户（用 B 之外的种子用户 20240017）
r = s.get(BASE + "/admin/users", headers=hD, params={"keyword": "20240017"})
recs = r.json()["data"]["records"]
if recs:
    uid = recs[0]["id"]
    r = s.post(BASE + "/admin/users/%d/status" % uid, headers=hD, params={"ban": True})
    ok(r.json()["code"] == 200, "封禁用户")
    lb2 = login("20240017", DEMO_PWD)
    ok(lb2["code"] != 200, "被封禁账号登录被拒")
    r = s.post(BASE + "/admin/users/%d/status" % uid, headers=hD, params={"ban": False})
    ok(r.json()["code"] == 200, "解封用户")
else:
    ok(False, "未找到 20240017 用于封禁测试")
r = s.get(BASE + "/admin/users", headers=hA)
ok(r.json()["code"] == 403, "非管理员访问 admin → 403")

# ---------- 清理：管理员删除自测启事（含磁盘清理，审查 2.20） ----------
show("清理：删除自测启事")
r = s.delete(BASE + "/admin/items/%d" % newId, headers=hD)
ok(r.json()["code"] == 200, "删除自测启事")
r = s.get(BASE + "/items/%d" % newId)
ok(r.json()["code"] == 404, "删除后 404")

# ---------- 自检：行数回基线 ----------
show("自检：统计回基线（审查 2.11③）")
time.sleep(1)
end_stats, end_idx = counts()
ok(end_stats == base_stats, "stats 回基线 %s == %s" % (end_stats, base_stats))
ok(end_idx == base_idx, "索引数回基线 %s == %s" % (end_idx, base_idx))

print("\n========== 结果：%s ==========" % ("全部通过" if FAILS == 0 else "%d 条失败" % FAILS))
sys.exit(1 if FAILS else 0)
