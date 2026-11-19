/**
 * 服务器地址（API 基址）运行时解析。
 *
 * 打包时可用 VITE_API_BASE 写入默认值；App 内「设置 → 服务器地址」可覆盖。
 * 这样 cpolar 免费版换随机域名后，只要在手机上改一次地址即可，不必重新打包 APK。
 */
const STORAGE_KEY = 'km_api_base'

function normalize(value) {
  const text = String(value || '').trim()
  if (!text) return ''
  return text.replace(/\/+$/, '')
}

/** 打包时注入的默认基址（H5 未指定时为相对路径 /api）。 */
export function buildApiBase() {
  return normalize(import.meta.env.VITE_API_BASE) || '/api'
}

/** 当前生效的 API 基址：本地覆盖优先，其次打包默认值。 */
export function getApiBase() {
  try {
    const saved = normalize(uni.getStorageSync(STORAGE_KEY))
    if (saved) return saved
  } catch (e) { /* ignore */ }
  return buildApiBase()
}

/** 保存覆盖地址；传空则清除覆盖，回到打包默认值。 */
export function setApiBase(value) {
  const next = normalize(value)
  try {
    if (next) uni.setStorageSync(STORAGE_KEY, next)
    else uni.removeStorageSync(STORAGE_KEY)
  } catch (e) { /* ignore */ }
  return getApiBase()
}

/** 是否处于「手动覆盖」状态。 */
export function hasApiBaseOverride() {
  try {
    return !!normalize(uni.getStorageSync(STORAGE_KEY))
  } catch (e) {
    return false
  }
}

/** API 基址对应的站点源（绝对地址）；相对基址则取当前页面源。 */
export function getApiOrigin() {
  const base = getApiBase()
  if (/^https?:\/\//i.test(base)) {
    try {
      return new URL(base).origin
    } catch (e) {
      return ''
    }
  }
  if (typeof location !== 'undefined' && /^https?:/.test(location.protocol)) {
    return location.origin
  }
  return ''
}
