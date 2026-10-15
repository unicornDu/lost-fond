# -*- coding: utf-8 -*-
"""
以图搜图端到端召回评测（03 T-01，第三套协议：真实种子库 + 留出查询图）。

协议（与 docs/11 的 HashExperimentTest 协议不同，互不覆盖）：
  查询图   = backend/uploads/_query/q-<物品>.jpg
  真值     = manifest.json 中图片路径匹配 seed/<物品>- 且 auditStatus==1 的启事数（候选）
  命中判定 = 返回 ItemVO 的封面路径属于同物品族
  指标     = 命中/候选 = 召回率；另记误检（返回了别的物品）与最低相似度

用法：
  python scripts/eval_recall.py                 # 按服务端当前配置评测，写 recall_results.csv
  python scripts/eval_recall.py --threshold 12 --top-n 10   # 临时覆盖（接口参数，无需重启）
  python scripts/eval_recall.py --grid          # 网格扫描 top-n×threshold（T-02 标定用）
  python scripts/eval_recall.py --min-recall 0.75           # 召回不达标退出码非 0（回归门禁）

口令从 backend/application-local.yml 的 seed.* 读取；读不到直接报错退出（不静默 fallback）。
"""
import argparse, csv, json, os, sys, time
import requests

HERE = os.path.dirname(os.path.abspath(__file__))
BACKEND = os.path.dirname(HERE)
BASE = "http://localhost:8080/api"
QUERY_DIR = os.path.join(BACKEND, "uploads", "_query")
MANIFEST = os.path.join(BACKEND, "uploads", "seed", "manifest.json")
CSV_OUT = os.path.join(HERE, "recall_results.csv")


def read_seed_password():
    p = os.path.join(BACKEND, "application-local.yml")
    if not os.path.exists(p):
        sys.exit("错误：缺少 backend/application-local.yml，无法读取演示口令（不允许硬编码 fallback）")
    val = None
    for line in open(p, encoding="utf-8"):
        line = line.strip()
        if line.startswith("admin-password:"):
            val = line.split(":", 1)[1].strip()
    if not val:
        sys.exit("错误：application-local.yml 缺少 seed.admin-password")
    return val


def obj_of(vo):
    url = vo.get("img") or vo.get("thumb") or ""
    base = url.split("/")[-1]
    return base.split("-")[0] if base else "?"


def ground_truth():
    root = json.load(open(MANIFEST, encoding="utf-8"))
    items = root["items"] if isinstance(root, dict) else root
    cand = {}
    for it in items:
        if it.get("auditStatus", 1) != 1:
            continue
        imgs = it.get("images", [])
        if not imgs:
            continue
        obj = imgs[0].split("/")[-1].split("-")[0]
        cand[obj] = cand.get(obj, 0) + 1
    return cand


def evaluate(session, cand, threshold=None, topn=None, label="current"):
    rows, tot_h, tot_c, tot_f, lat = [], 0, 0, 0, []
    print("%-9s %4s %4s %8s %5s %8s %9s" % ("obj", "hit", "cand", "recall", "false", "min_sim", "latency"))
    for q in sorted(os.listdir(QUERY_DIR)):
        if not q.startswith("q-") or not q.endswith(".jpg"):
            continue
        obj = q[2:-4]
        params = {}
        if threshold is not None:
            params["threshold"] = threshold
        if topn is not None:
            params["topN"] = topn
        t0 = time.perf_counter()
        with open(os.path.join(QUERY_DIR, q), "rb") as f:
            r = session.post(BASE + "/search/image", params=params,
                             files={"file": (q, f, "image/jpeg")}, timeout=60)
        ms = (time.perf_counter() - t0) * 1000
        lat.append(ms)
        data = r.json().get("data") or []
        hits = [v for v in data if obj_of(v) == obj]
        false = len(data) - len(hits)
        c = cand.get(obj, 0)
        tot_h += len(hits); tot_c += c; tot_f += false
        sim = min((v.get("similarity", 0) for v in hits), default=0)
        rows.append({"mode": label, "obj": obj, "top_n": topn if topn is not None else "",
                     "threshold": threshold if threshold is not None else "",
                     "hits": len(hits), "candidates": c,
                     "recall": round(len(hits) / c, 4) if c else 0,
                     "false_positives": false, "min_similarity": sim,
                     "latency_ms": round(ms, 1),
                     "hit_ids": "|".join(str(v["id"]) for v in hits)})
        print("%-9s %4d %4d %7.0f%% %5d %8.1f %8.1fms" %
              (obj, len(hits), c, (len(hits) / c * 100 if c else 0), false, sim, ms))
    rec = tot_h / tot_c if tot_c else 0
    print("%-9s %4d %4d %7.1f%% %5d %8s %8.1fms（平均单次）" %
          ("TOTAL", tot_h, tot_c, rec * 100, tot_f, "", sum(lat) / len(lat) if lat else 0))
    return rows, rec, (sum(lat) / len(lat) if lat else 0), tot_f


def write_csv(rows):
    """统一列写 CSV；default 模式保留已有 grid 行，保证两套证据共存。"""
    fields = ["section", "obj", "top_n", "threshold", "hits", "candidates",
              "recall", "false_positives", "min_similarity", "latency_ms", "hit_ids"]
    prev = []
    if os.path.exists(CSV_OUT):
        with open(CSV_OUT, encoding="utf-8") as f:
            for old in csv.DictReader(f):
                if old.get("section") == "grid" and old.get("obj") == "ALL":
                    prev.append(old)
    with open(CSV_OUT, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fields, extrasaction="ignore")
        w.writeheader()
        for g in prev:
            w.writerow(g)
        for r in rows:
            w.writerow(r)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--threshold", type=int, default=None)
    ap.add_argument("--top-n", type=int, default=None)
    ap.add_argument("--grid", action="store_true", help="网格扫描 top-n×threshold")
    ap.add_argument("--min-recall", type=float, default=0.6)
    args = ap.parse_args()

    pwd = read_seed_password()
    s = requests.Session()
    r = s.post(BASE + "/auth/login", json={"account": "10000001", "password": pwd}, timeout=10)
    if r.json().get("code") != 200:
        sys.exit("错误：管理员登录失败（后端是否已启动？口令是否与 application-local.yml 一致？）%s" % r.json().get("msg"))
    s.headers["Authorization"] = "Bearer " + r.json()["data"]["token"]
    try:
        s.get(BASE + "/stats", timeout=5)
    except requests.ConnectionError:
        sys.exit("错误：后端未启动，请先在 backend 目录运行 java -jar target/lost-found-backend.jar")

    cand = ground_truth()

    if args.grid:
        print("===== 网格扫描（top-n × threshold）=====")
        print("%6s %6s %9s %6s %10s %10s" % ("topN", "th", "recall", "false", "avg_ret", "avg_ms"))
        rows = []
        for topn in (6, 10, 20, 50):
            for th in (8, 10, 12, 14, 16):
                detail, rec, avg_ms, tot_f = evaluate(s, cand, threshold=th, topn=topn,
                                                      label="grid-%d-%d" % (topn, th))
                tot_h = sum(d["hits"] for d in detail)
                tot_c = sum(d["candidates"] for d in detail)
                avg_ret = sum(d["hits"] + d["false_positives"] for d in detail) / len(detail)
                rows.append({"section": "grid", "obj": "ALL", "top_n": topn, "threshold": th,
                             "hits": tot_h, "candidates": tot_c, "recall": round(rec, 4),
                             "false_positives": tot_f, "min_similarity": "",
                             "latency_ms": round(avg_ms, 1), "hit_ids": ""})
                print("%6d %6d %8.1f%% %6d %10.2f %9.1f" % (topn, th, rec * 100, tot_f, avg_ret, avg_ms))
        write_csv(rows)
        print("网格结果已写入", CSV_OUT)
        return 0

    rows, rec, avg_ms, tot_f = evaluate(s, cand, args.threshold, args.top_n,
                                        label="override" if (args.threshold or args.top_n) else "current")
    out = [{"section": "current", "obj": r0["obj"], "top_n": args.top_n or "",
            "threshold": args.threshold or "", "hits": r0["hits"], "candidates": r0["candidates"],
            "recall": r0["recall"], "false_positives": r0["false_positives"],
            "min_similarity": r0["min_similarity"], "latency_ms": r0["latency_ms"],
            "hit_ids": r0["hit_ids"]} for r0 in rows]
    tot_h = sum(r0["hits"] for r0 in rows)
    tot_c = sum(r0["candidates"] for r0 in rows)
    out.append({"section": "current", "obj": "ALL", "top_n": args.top_n or "", "threshold": args.threshold or "",
                "hits": tot_h, "candidates": tot_c, "recall": round(rec, 4), "false_positives": tot_f,
                "min_similarity": "", "latency_ms": round(avg_ms, 1), "hit_ids": ""})
    write_csv(out)
    print("明细已写入", CSV_OUT)
    if rec < args.min_recall:
        print("召回 %.1f%% 低于门禁 %.1f%%" % (rec * 100, args.min_recall * 100))
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
