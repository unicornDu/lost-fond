import { Capacitor } from '@capacitor/core'

/**
 * M-P1b：原生相机接入（Capacitor 环境检测，浏览器走原有 <input type=file> 不受影响）。
 * CameraSource.Prompt = 弹"拍照 / 从相册选择"；quality+width 直接压缩到 ≤5MB 上限内
 * （1600px 宽 JPEG quality 70 实测 300KB~2MB，覆盖 5MB 校验）。
 */
export const IS_NATIVE = Capacitor.isNativePlatform()

function dataUrlToFile(dataUrl, name) {
  const [meta, b64] = dataUrl.split(',')
  const mime = meta.match(/data:(.*?);/)?.[1] || 'image/jpeg'
  const bin = atob(b64)
  const buf = new Uint8Array(bin.length)
  for (let i = 0; i < bin.length; i++) buf[i] = bin.charCodeAt(i)
  return new File([buf], name, { type: mime })
}

/** 弹出"拍照/相册"选择，返回 File（与 <input type=file> 产物同构，走既有上传管道）。 */
export async function takePhotoNative(name = 'photo.jpg') {
  const { Camera, CameraResultType, CameraSource } = await import('@capacitor/camera')
  const photo = await Camera.getPhoto({
    resultType: CameraResultType.DataUrl,
    source: CameraSource.Prompt,
    quality: 70,
    width: 1600,
  })
  return dataUrlToFile(photo.dataUrl, name.endsWith('.jpg') ? name : name + '.jpg')
}
