import { getApiBase } from '@/utils/server'

function apiUrlObject() {
  const base = getApiBase()
  if (!/^https?:\/\//i.test(base)) return null
  try {
    return new URL(base)
  } catch (e) {
    return null
  }
}

function mediaHost() {
  const parsed = apiUrlObject()
  if (parsed) return parsed.hostname
  if (typeof location !== 'undefined' && location.hostname) return location.hostname
  return ''
}

/** 直播间 WebSocket 地址：APP 走 API 主机，H5 走当前站点（Vite/nginx 代理 /ws）。 */
export function liveWsBase() {
  const parsed = apiUrlObject()
  if (parsed) {
    const scheme = parsed.protocol === 'https:' ? 'wss' : 'ws'
    return `${scheme}://${parsed.host}`
  }
  if (typeof location !== 'undefined') {
    const scheme = location.protocol === 'https:' ? 'wss' : 'ws'
    return `${scheme}://${location.host}`
  }
  return ''
}

/** 后端返回的 127.0.0.1 只适用于电脑本机，真机/模拟器需要替换成 API 所在主机。 */
export function liveMediaUrl(url) {
  if (!url) return ''
  const host = mediaHost()
  if (!host) return url
  return String(url).replace(
    /\/\/(?:127\.0\.0\.1|localhost)(?=:\d+|\/|$)/i,
    `//${host}`
  )
}

export function liveFlvUrl(url) {
  return liveMediaUrl(url).replace(/\.m3u8(\?.*)?$/i, '.flv$1')
}
