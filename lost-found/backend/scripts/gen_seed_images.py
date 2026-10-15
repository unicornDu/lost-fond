# -*- coding: utf-8 -*-
"""
生成演示种子图片 + manifest.json（v2，审查 2.12 / 2.10）。
- 路径全部相对本脚本（Path(__file__)），换机器可跑。
- 底图来自 uploads/_src/（已入库的真实物品图：杯/钥匙/耳机/学生证/雨伞/围巾），**无外网依赖**。
- 每个物品生成多个「同物异拍」变体（缩放/裁剪/亮度/对比度/灰度，均为 pHash 鲁棒变换，不用旋转），
  同一物品的多条启事共用这些变体 → 以图搜图可跨 lost/found 强命中。
- 每物品另存一张留出查询图 v4 到 uploads/_query/（冒烟测试/演示用，不入库）。
- 产出约 60 条启事（满足分工方案 100+ 的量级要求之半，含图），并标记 4 条 auditStatus=0 供后台审核演示。
manifest 仅描述业务字段与图片路径；口令由后端初始化时按配置/随机生成（不含任何密码字面量）。
"""
import os, io, json, random
from pathlib import Path
from PIL import Image, ImageEnhance, ImageFilter

BASE = Path(__file__).resolve().parents[1] / "uploads"
SRC = BASE / "_src"
SEED = BASE / "seed"
QUERY = BASE / "_query"
for d in (SEED, QUERY):
    d.mkdir(parents=True, exist_ok=True)

random.seed(42)  # 可复现

def center_crop(img, ratio):
    w, h = img.size
    cw, ch = int(w*ratio), int(h*ratio)
    l, t = (w-cw)//2, (h-ch)//2
    return img.crop((l, t, l+cw, t+ch))

def v0(img): return img.resize((640, 480))
def v1(img): return ImageEnhance.Brightness(center_crop(img, 0.85).resize((640, 480))).enhance(1.12)
def v2(img): return ImageEnhance.Contrast(img.resize((360, 270))).enhance(1.10)
def v3(img): return img.convert("L").convert("RGB").filter(ImageFilter.GaussianBlur(0.8)).resize((640, 480))
def v4(img): return ImageEnhance.Brightness(center_crop(img, 0.90).resize((640, 480))).enhance(1.05)  # 留出查询（轻变换，保证 pHash 可命中）
def v5(img): return ImageEnhance.Contrast(center_crop(img, 0.90).resize((640, 480))).enhance(0.92)

def save(img, path): img.save(path, "JPEG", quality=90)

# 六个物品（与 _src 文件名对应）
OBJECTS = {
    "cup":     dict(cat="其他",     titles=["蓝色保温杯，杯身有小熊贴纸","捡到蓝色保温杯（小熊贴纸）","丢失米色盖保温杯","蓝色小熊贴纸保温杯","保温杯（杯盖米色）遗落","捡到蓝色保温杯一只","寻：蓝色保温杯","蓝色保温杯待认领","丢失蓝色小熊保温杯","招领：蓝色保温杯"]),
    "keys":    dict(cat="钥匙",     titles=["一串钥匙带小熊挂件","丢失一串钥匙（小熊挂件）","捡到四把钥匙+挂件","钥匙串（小熊挂件）待认领","寻：宿舍楼下丢的钥匙","捡到钥匙一串","丢失钥匙带小熊挂件","钥匙串待失主认领","丢了一串钥匙急寻","招领：钥匙串带挂件"]),
    "earbuds": dict(cat="电子设备", titles=["白色无线耳机一副","捡到白色无线耳机","丢失白色耳机+充电盒","白色耳机充电盒有划痕","寻：白色无线耳机","捡到耳机及充电盒","丢失白色耳机一副","白色无线耳机待认领","耳机充电盒遗落操场","招领：白色无线耳机"]),
    "card":    dict(cat="证件卡类", titles=["学生证一张（透明卡套）","捡到学生证（带挂绳）","丢失学生证急寻","学生证待失主认领","寻：透明卡套学生证","捡到学生证已交班委","丢失学生证一张","学生证（挂绳款）待认领","校车站捡到学生证","招领：学生证带卡套"]),
    "umbrella":dict(cat="其他",     titles=["粉色雨伞，伞柄有蝴蝶结","捡到粉色蝴蝶结雨伞","丢失粉色雨伞一把","粉色雨伞待认领","寻：蝴蝶结柄粉伞","捡到粉色雨伞","丢失粉色蝴蝶结雨伞","粉伞（伞骨微弯）待认领","图书馆遗落粉色雨伞","招领：粉色雨伞"]),
    "scarf":   dict(cat="衣物饰品", titles=["灰色格纹流苏围巾","捡到灰色格纹围巾","丢失灰色围巾一条","灰围巾（流苏款）待认领","寻：灰色格纹围巾","捡到灰色围巾","丢失灰色流苏围巾","灰色围巾待失主","操场主席台捡到灰围巾","招领：灰色格纹围巾"]),
}
LOCATIONS = ["第一教学楼","图书馆","第二食堂","操场","宿舍楼","实验楼","校车站","体育馆"]
OWNERS = [("202400%02d" % i, n) for i, n in enumerate(
    ["林同学","陈同学","王同学","赵同学","刘同学","孙同学","周同学","吴同学","郑同学","钱同学",
     "冯同学","褚同学","卫同学","蒋同学","沈同学","韩同学","杨同学","朱同学","秦同学","许同学"], start=1)]

DESC = "物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）"

items = []
print("生成种子图片（离线，读取 _src）...", flush=True)
for oi, (obj, meta) in enumerate(OBJECTS.items()):
    base = Image.open(SRC / (obj + ".jpg")).convert("RGB")
    variants = [v0(base), v1(base), v2(base), v3(base), v5(base)]
    fnames = []
    for vi, vim in enumerate(variants):
        fn = f"{obj}-{vi}.jpg"
        save(vim, SEED / fn)
        fnames.append(fn)
    save(v4(base), QUERY / f"q-{obj}.jpg")   # 留出查询图

    for j in range(10):
        cover = fnames[j % 5]
        second = fnames[(j + 1) % 5]
        typ = "lost" if j % 2 == 0 else "found"
        # 多数保持"在找/待认领"，仅 j=8,9 置已完成，保证墙与首页有内容
        status = ("已找回" if typ == "lost" else "已认领") if j in (8, 9) else ("寻找中" if typ == "lost" else "待认领")
        owner_acc, owner_nick = OWNERS[(oi * 3 + j) % len(OWNERS)]
        # 审查 2.4：auditStatus 0=待审核。仅 cup/keys 的 j=8,9 置待审核供后台演示（注意别与 0/1 语义搞反）
        audit = 0 if (obj in ("cup", "keys") and j >= 8) else 1
        items.append(dict(
            account=owner_acc, nickname=owner_nick, type=typ,
            title=meta["titles"][j], category=meta["cat"],
            location=LOCATIONS[(oi + j) % len(LOCATIONS)],
            happenTime=f"2026-09-{20 + (j % 8):02d} {8 + (j % 10):02d}:{(j*7)%60:02d}",
            status=status, views=40 + j * 17 + oi * 5, desc=DESC,
            auditStatus=audit,
            images=[f"/uploads/seed/{cover}", f"/uploads/seed/{second}"],
        ))
    print(f"  完成物品 {obj}（10 条启事 + 查询图）", flush=True)

manifest = dict(admin=dict(account="10000001", nickname="管理员"), items=items)
with io.open(SEED / "manifest.json", "w", encoding="utf-8") as f:
    json.dump(manifest, f, ensure_ascii=False, indent=2)
print(f"共 {len(items)} 条启事，manifest 已写入 seed 目录", flush=True)
print("查询图:", sorted(p.name for p in QUERY.glob("*.jpg")), flush=True)
