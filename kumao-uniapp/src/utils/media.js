const coverSvg =
  '<svg xmlns="http://www.w3.org/2000/svg" width="320" height="320">' +
  '<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">' +
  '<stop offset="0" stop-color="#2a2320"/><stop offset="1" stop-color="#121116"/></linearGradient></defs>' +
  '<rect width="320" height="320" fill="url(#g)"/>' +
  '<circle cx="160" cy="160" r="72" fill="none" stroke="#4a3f33" stroke-width="2"/>' +
  '<circle cx="160" cy="160" r="9" fill="#4a3f33"/>' +
  '<text x="160" y="286" font-size="18" text-anchor="middle" fill="#5b4d3d" font-family="sans-serif">KUMAO</text>' +
  '</svg>'

const avatarSvg =
  '<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">' +
  '<defs><linearGradient id="a" x1="0" y1="0" x2="1" y2="1">' +
  '<stop offset="0" stop-color="#3a2f24"/><stop offset="1" stop-color="#17151b"/></linearGradient></defs>' +
  '<rect width="200" height="200" fill="url(#a)"/>' +
  '<circle cx="100" cy="80" r="32" fill="#4a3f33"/>' +
  '<path d="M34 200c0-36 30-66 66-66s66 30 66 66z" fill="#4a3f33"/>' +
  '</svg>'

export const FALLBACK_COVER = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(coverSvg)
export const FALLBACK_AVATAR = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(avatarSvg)

function apiOrigin() {
  const base = import.meta.env.VITE_API_BASE || ''
  if (!/^https?:\/\//i.test(base)) return ''
  try {
    return new URL(base).origin
  } catch (e) {
    return ''
  }
}

// H5 / WebView：API 基址是绝对地址时，静态资源也要走同一主机，否则相对路径会落到本地资源服务器。
const FILE_ORIGIN = apiOrigin() || (typeof location !== 'undefined' && /^https?:/.test(location.protocol) ? location.origin : 'http://127.0.0.1:8080')

export function fileUrl(path) {
  if (!path) return ''
  if (/^https?:\/\//.test(path)) return path
  return FILE_ORIGIN + (path.charAt(0) === '/' ? path : '/' + path)
}

export function coverOf(url) {
  return url ? fileUrl(url) : FALLBACK_COVER
}

export function avatarOf(url) {
  return url ? fileUrl(url) : FALLBACK_AVATAR
}

export function formatPlay(n) {
  const v = Number(n) || 0
  if (!v) return '0'
  if (v >= 100000000) return (v / 100000000).toFixed(1) + '亿'
  if (v >= 10000) return (v / 10000).toFixed(1) + '万'
  return String(v)
}

export function formatCount(n) {
  const v = Number(n) || 0
  return v >= 10000 ? (v / 10000).toFixed(1) + '万' : String(v)
}




