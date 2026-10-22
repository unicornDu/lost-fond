/**
 * 地图工具（v2.41.1）：Web 墨卡托换算 + 高德栅格瓦片 + WGS-84→GCJ-02。
 *
 * 为什么自己算而不用 Leaflet：只需要"铺瓦片 + 一个点"，引一个 40KB 的库不划算，
 * 也让构建产物多一个依赖。这里只保留必要公式，几何部分有单测兜底。
 *
 * 坐标系：高德瓦片是 GCJ-02。发布页在地图上点选得到的点本身就是 GCJ-02，直接入库；
 * 浏览器 geolocation 给的是 WGS-84，必须转一次（wgs84ToGcj02）才能对齐到瓦片上。
 */

export const TILE_SIZE = 256
export const TILE_STYLE_URL =
  'https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&x={x}&y={y}&z={z}'

/** 经纬度 → 世界像素坐标（原点在左上，z 决定比例）。 */
export function lngLatToWorld(lng, lat, z) {
  const scale = TILE_SIZE * Math.pow(2, z)
  const x = ((Number(lng) + 180) / 360) * scale
  const s = Math.sin((Number(lat) * Math.PI) / 180)
  const y = (0.5 - Math.log((1 + s) / (1 - s)) / (4 * Math.PI)) * scale
  return { x, y }
}

/** 世界像素坐标 → 经纬度（逆运算，点选地图时用）。 */
export function worldToLngLat(x, y, z) {
  const scale = TILE_SIZE * Math.pow(2, z)
  const lng = (x / scale) * 360 - 180
  const n = Math.PI - (2 * Math.PI * y) / scale
  const lat = (180 / Math.PI) * Math.atan(0.5 * (Math.exp(n) - Math.exp(-n)))
  return { lng, lat }
}

/** 单张瓦片地址（子域 1–4 轮询，避免单域名并发被限）。 */
export function tileUrl(x, y, z) {
  const s = 1 + (Math.abs(x + y) % 4)
  return TILE_STYLE_URL.replace('{s}', s).replace('{x}', x).replace('{y}', y).replace('{z}', z)
}

/**
 * 把瓦片铺进容器，并让 (lat,lng) 落在容器正中。
 * 返回本次写入的瓦片数（0 表示容器还没尺寸，调用方可在 nextTick 后重试）。
 */
export function paintTiles(el, lat, lng, zoom) {
  if (!el) return 0
  const W = el.clientWidth
  const H = el.clientHeight
  if (!W || !H) return 0
  const p = lngLatToWorld(lng, lat, zoom)
  const left = p.x - W / 2
  const top = p.y - H / 2
  const x0 = Math.floor(left / TILE_SIZE)
  const x1 = Math.floor((left + W) / TILE_SIZE)
  const y0 = Math.floor(top / TILE_SIZE)
  const y1 = Math.floor((top + H) / TILE_SIZE)
  const max = Math.pow(2, zoom) - 1
  let html = ''
  let n = 0
  for (let ty = y0; ty <= y1; ty++) {
    if (ty < 0 || ty > max) continue
    for (let tx = x0; tx <= x1; tx++) {
      const wrapped = ((tx % (max + 1)) + max + 1) % (max + 1)
      html += `<img src="${tileUrl(wrapped, ty, zoom)}" alt="" loading="lazy" draggable="false" ` +
        `style="left:${tx * TILE_SIZE - left}px;top:${ty * TILE_SIZE - top}px" />`
      n++
    }
  }
  el.innerHTML = html
  return n
}

/** 光标在容器内的偏移 → 经纬度（容器中心对应当前中心点）。 */
export function offsetToLngLat(px, py, el, lat, lng, zoom) {
  const W = el.clientWidth
  const H = el.clientHeight
  const p = lngLatToWorld(lng, lat, zoom)
  return worldToLngLat(p.x + (px - W / 2), p.y + (py - H / 2), zoom)
}

/** 格式化：34.86349° N, 113.64764° E（南纬/西经自动换成 S/W）。 */
export function formatLatLng(lat, lng, digits = 5) {
  if (lat == null || lng == null) return ''
  const la = Math.abs(Number(lat)).toFixed(digits)
  const ln = Math.abs(Number(lng)).toFixed(digits)
  return `${la}° ${Number(lat) >= 0 ? 'N' : 'S'}, ${ln}° ${Number(lng) >= 0 ? 'E' : 'W'}`
}

/* ---------- WGS-84 → GCJ-02（国测局偏移） ---------- */
const A = 6378245.0
const EE = 0.00669342162296594323

function outOfChina(lat, lng) {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
}

function transformLat(x, y) {
  let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
  ret += ((20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin((y / 3.0) * Math.PI)) * 2.0) / 3.0
  ret += ((160.0 * Math.sin((y / 12.0) * Math.PI) + 320 * Math.sin((y * Math.PI) / 30.0)) * 2.0) / 3.0
  return ret
}

function transformLng(x, y) {
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
  ret += ((20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin((x / 3.0) * Math.PI)) * 2.0) / 3.0
  ret += ((150.0 * Math.sin((x / 12.0) * Math.PI) + 300.0 * Math.sin((x / 30.0) * Math.PI)) * 2.0) / 3.0
  return ret
}

/** WGS-84（GPS / 浏览器定位）→ GCJ-02（高德/腾讯）。境外原样返回。 */
export function wgs84ToGcj02(lat, lng) {
  const la = Number(lat)
  const ln = Number(lng)
  if (!Number.isFinite(la) || !Number.isFinite(ln) || outOfChina(la, ln)) return { lat: la, lng: ln }
  let dLat = transformLat(ln - 105.0, la - 35.0)
  let dLng = transformLng(ln - 105.0, la - 35.0)
  const radLat = (la / 180.0) * Math.PI
  let magic = Math.sin(radLat)
  magic = 1 - EE * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  dLat = (dLat * 180.0) / (((A * (1 - EE)) / (magic * sqrtMagic)) * Math.PI)
  dLng = (dLng * 180.0) / ((A / sqrtMagic) * Math.cos(radLat) * Math.PI)
  return { lat: la + dLat, lng: ln + dLng }
}

/**
 * 默认视野：**郑州师范学院 东校区（主校区，惠济区英才街 16 号）**，GCJ-02。
 * 取点：OSM relation 10414002（郑州师范学院(东区)）中心 34.8643146, 113.6508687（WGS-84），
 * 再用本文件的 wgs84ToGcj02() 转到 GCJ-02，与高德瓦片同基准。
 * 西校区（同在北大学城，图书馆/特教楼/怡馨公寓一带）中心约 34.863491, 113.647637；
 * 要默认看西区就把下面两行换成它。
 */
export const CAMPUS_VIEW = { lat: 34.863292, lng: 113.65696, zoom: 17 }
