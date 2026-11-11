/**
 * 跨端复制文本。
 * H5 / WebView：优先 Clipboard API，其次 textarea + execCommand（部分 WebView 不支持 uni.setClipboardData）。
 * 原生端：回退到 uni.setClipboardData。
 */
export async function copyText(text) {
  const value = String(text == null ? '' : text)
  if (!value) return false

  // #ifdef H5
  try {
    if (typeof navigator !== 'undefined' && navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(value)
      return true
    }
  } catch (e) { /* fall through */ }

  try {
    const ta = document.createElement('textarea')
    ta.value = value
    ta.setAttribute('readonly', '')
    ta.style.position = 'fixed'
    ta.style.top = '-1000px'
    ta.style.opacity = '0'
    document.body.appendChild(ta)
    ta.select()
    ta.setSelectionRange(0, value.length)
    const ok = document.execCommand('copy')
    document.body.removeChild(ta)
    if (ok) return true
  } catch (e) { /* fall through */ }
  // #endif

  return new Promise((resolve) => {
    if (typeof uni === 'undefined' || typeof uni.setClipboardData !== 'function') {
      resolve(false)
      return
    }
    uni.setClipboardData({
      data: value,
      success: () => resolve(true),
      fail: () => resolve(false)
    })
  })
}
