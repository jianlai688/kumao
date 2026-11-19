/**
 * 上传通道选择 + 超时估算
 *
 * 背景：公网隧道（cpolar 等）上传带宽很低，几百 MB 的 MV 走公网隧道要按小时算。
 * 如果浏览器和服务其实在同一台机器上（PC 端演示最常见），可以直连本机网关上传，
 * 速度能快上百倍。这里用一次很短的探测判断「本机是否有网关」，有就直连，没有就用相对路径。
 */
const LOCAL_BASE = 'http://127.0.0.1:8080/api'

let cachedBase

function isLoopbackHost(host) {
  return host === 'localhost' || host === '::1' || host === '0.0.0.0' || /^127\./.test(host)
}

/** 返回上传用的 baseURL；空字符串表示沿用当前站点相对路径 */
export async function resolveUploadBase() {
  if (cachedBase !== undefined) return cachedBase
  if (typeof window === 'undefined') {
    cachedBase = ''
    return cachedBase
  }
  const { protocol, hostname } = window.location
  // 本机 / 局域网访问时，相对路径本身就是最快的通道
  if (!/^https?:$/.test(protocol) || isLoopbackHost(hostname)) {
    cachedBase = ''
    return cachedBase
  }
  try {
    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), 1500)
    const res = await fetch(`${LOCAL_BASE}/music/banners`, {
      signal: controller.signal,
      cache: 'no-store'
    })
    clearTimeout(timer)
    cachedBase = res.ok ? LOCAL_BASE : ''
  } catch (e) {
    cachedBase = ''
  }
  return cachedBase
}

/**
 * 上传超时（毫秒）。按 32KB/s 的保守速度估算，最少 2 分钟，最多 3 小时。
 * 真正的「卡死」由上传组件的「无进度 45 秒」看门狗负责，这里只做兜底。
 */
export function uploadTimeoutFor(size) {
  const estimate = (Number(size) || 0) / (32 * 1024)
  return Math.max(120000, Math.min(estimate, 3 * 3600) * 1000)
}
