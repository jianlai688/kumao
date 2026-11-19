const STORAGE_KEY = 'km_theme'

export const THEME_PRESETS = [
  { key: 'violet', name: '星云紫', accent: '#8b3fce' },
  { key: 'amber', name: '琥珀金', accent: '#b45309' },
  { key: 'ocean', name: '深海蓝', accent: '#2563eb' },
  { key: 'mint', name: '薄荷绿', accent: '#0d8a61' },
  { key: 'sakura', name: '樱花粉', accent: '#c0266d' },
  { key: 'sunset', name: '落日橙', accent: '#c2410c' },
  { key: 'rose', name: '玫瑰红', accent: '#be123c' }
]

function clamp(v) {
  return Math.max(0, Math.min(255, Math.round(v)))
}

export function shade(hex, percent) {
  const value = hex.replace('#', '')
  const num = parseInt(value.length === 3 ? value.split('').map((c) => c + c).join('') : value, 16)
  const amt = Math.round(2.55 * percent)
  const r = clamp((num >> 16) + amt)
  const g = clamp(((num >> 8) & 0x00ff) + amt)
  const b = clamp((num & 0x0000ff) + amt)
  return `#${((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)}`
}

export function rgba(hex, alpha) {
  const value = hex.replace('#', '')
  const num = parseInt(value.length === 3 ? value.split('').map((c) => c + c).join('') : value, 16)
  return `rgba(${(num >> 16) & 0xff}, ${(num >> 8) & 0xff}, ${num & 0xff}, ${alpha})`
}

export function applyTheme(theme) {
  const accent = (theme && theme.accent) || THEME_PRESETS[0].accent
  const root = document.documentElement
  const set = (name, value) => root.style.setProperty(name, value)
  set('--km-accent', accent)
  set('--km-accent-2', shade(accent, 16))
  set('--km-accent-dim', shade(accent, -16))
  set('--km-accent-ink', shade(accent, 10))
  set('--km-accent-soft', rgba(accent, 0.12))
  set('--km-accent-line', rgba(accent, 0.3))
  set('--km-grad', `linear-gradient(100deg, ${shade(accent, -26)} 0%, ${accent} 52%, ${shade(accent, 18)} 100%)`)
  set('--km-grad-soft', `linear-gradient(100deg, ${rgba(accent, 0.22)}, ${rgba(accent, 0.08)})`)
  set('--km-glow', rgba(accent, 0.32))
  set('--km-primary', accent)
  root.setAttribute('data-km-theme', theme?.key || 'custom')
}

export function loadTheme() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return { ...THEME_PRESETS[0] }
    const parsed = JSON.parse(raw)
    const preset = parsed?.key && THEME_PRESETS.find((item) => item.key === parsed.key)
    if (preset) return { ...preset }
    return parsed && parsed.accent ? parsed : { ...THEME_PRESETS[0] }
  } catch (e) {
    return { ...THEME_PRESETS[0] }
  }
}

export function saveTheme(theme) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(theme))
  applyTheme(theme)
}

export function initTheme() {
  applyTheme(loadTheme())
}
