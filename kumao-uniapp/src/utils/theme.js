const STORAGE_KEY = 'km_theme'

export const THEME_PRESETS = [
  { key: 'amber', name: '琥珀金', accent: '#f5b45b' },
  { key: 'violet', name: '星云紫', accent: '#c084fc' },
  { key: 'ocean', name: '深海蓝', accent: '#60a5fa' },
  { key: 'mint', name: '薄荷绿', accent: '#34d399' },
  { key: 'sakura', name: '樱花粉', accent: '#f472b6' },
  { key: 'sunset', name: '落日橙', accent: '#fb923c' },
  { key: 'rose', name: '玫瑰红', accent: '#fb7185' }
]

export const CUSTOM_COLORS = [
  '#f5b45b', '#fbbf24', '#fb923c', '#fb7185',
  '#f472b6', '#c084fc', '#a78bfa', '#60a5fa',
  '#38bdf8', '#34d399', '#4ade80', '#a3e635'
]

function clamp(v) {
  return Math.max(0, Math.min(255, Math.round(v)))
}

export function shade(hex, percent) {
  const value = String(hex || '#f5b45b').replace('#', '')
  const num = parseInt(value.length === 3 ? value.split('').map((c) => c + c).join('') : value, 16)
  const amt = Math.round(2.55 * percent)
  const r = clamp((num >> 16) + amt)
  const g = clamp(((num >> 8) & 0x00ff) + amt)
  const b = clamp((num & 0x0000ff) + amt)
  return `#${((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)}`
}

export function rgba(hex, alpha) {
  const value = String(hex || '#f5b45b').replace('#', '')
  const num = parseInt(value.length === 3 ? value.split('').map((c) => c + c).join('') : value, 16)
  return `rgba(${(num >> 16) & 0xff}, ${(num >> 8) & 0xff}, ${num & 0xff}, ${alpha})`
}

export function applyTheme(theme) {
  if (typeof document === 'undefined') return
  const accent = (theme && theme.accent) || THEME_PRESETS[0].accent
  const css = `page, uni-page-body, .km-screen {
    --km-accent: ${accent};
    --km-accent-deep: ${shade(accent, -14)};
    --km-accent-soft: ${rgba(accent, 0.13)};
    --km-accent-line: ${rgba(accent, 0.28)};
    --km-grad: linear-gradient(140deg, ${shade(accent, 22)} 0%, ${shade(accent, 2)} 48%, ${shade(accent, -16)} 100%);
  }`
  let el = document.getElementById('km-theme-style')
  if (!el) {
    el = document.createElement('style')
    el.id = 'km-theme-style'
    document.head.appendChild(el)
  }
  el.textContent = css
}

export function loadTheme() {
  try {
    const raw = uni.getStorageSync(STORAGE_KEY)
    if (!raw) return { ...THEME_PRESETS[0] }
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    return parsed && parsed.accent ? parsed : { ...THEME_PRESETS[0] }
  } catch (e) {
    return { ...THEME_PRESETS[0] }
  }
}

export function saveTheme(theme) {
  uni.setStorageSync(STORAGE_KEY, JSON.stringify(theme))
  applyTheme(theme)
}
