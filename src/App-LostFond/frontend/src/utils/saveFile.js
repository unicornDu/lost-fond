/**
 * 文件保存：桌面版与浏览器一致，走浏览器下载。
 * 交付文件以 blob 返回，海报以 dataURL 返回。
 */
function downloadInBrowser(url, fileName, revoke) {
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  a.click()
  if (revoke) setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export async function saveBlob(blob, fileName) {
  const url = URL.createObjectURL(blob)
  downloadInBrowser(url, fileName, true)
  return 'downloaded'
}

export async function saveDataUrl(dataUrl, fileName) {
  downloadInBrowser(dataUrl, fileName, false)
  return 'downloaded'
}
