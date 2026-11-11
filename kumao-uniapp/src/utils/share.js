import { copyText } from '@/utils/clipboard'

/**
 * 统一分享：支持系统分享面板时直接唤起，否则复制链接并提示。
 */
const SHARE_ORIGIN = 'http://192.168.20.103:5173'

function shareUrl(path) {
  if (/^https?:\/\//i.test(path)) return path
  const base = typeof location !== 'undefined' && location.origin && !/^file:/.test(location.origin)
    ? location.origin
    : SHARE_ORIGIN
  return base + (path.charAt(0) === '/' ? path : '/' + path)
}

export async function share(title, path, desc) {
  const url = shareUrl(path)
  const text = desc ? `${title} - ${desc}` : title

  // #ifdef H5
  if (typeof navigator !== 'undefined' && typeof navigator.share === 'function') {
    try {
      await navigator.share({ title, text, url })
      return { ok: true, method: 'system' }
    } catch (e) {
      if (e && e.name === 'AbortError') return { ok: false, method: 'cancel' }
    }
  }
  // #endif

  const ok = await copyText(`${text} ${url}`)
  uni.showToast({
    title: ok ? '链接已复制，去分享吧' : '复制失败，请手动分享',
    icon: 'none'
  })
  return { ok, method: ok ? 'clipboard' : 'fail', url }
}

export function songSharePath(id) {
  return `/#/pages/song/song?id=${id}`
}

export function mvSharePath(id) {
  return `/#/pages/mv/play?id=${id}`
}

export function roomSharePath(id) {
  return `/#/pages/live/room?id=${id}`
}
