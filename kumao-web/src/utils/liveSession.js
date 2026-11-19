const STORAGE_KEY = 'km_live_reports_v1'

export function createSessionId(roomId) {
  return `${String(roomId)}-${Date.now()}`
}

function readStore() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}') || {}
  } catch (e) {
    return {}
  }
}

export function saveLiveReport(report) {
  if (!report?.sessionId) return
  const all = readStore()
  all[report.sessionId] = report
  // 只保留最近 30 场，避免 localStorage 无限增长
  const entries = Object.entries(all).sort((a, b) => (b[1]?.endTime || '').localeCompare(a[1]?.endTime || ''))
  localStorage.setItem(STORAGE_KEY, JSON.stringify(Object.fromEntries(entries.slice(0, 30))))
}

export function loadLiveReport(sessionId) {
  return readStore()[sessionId] || null
}

function hashString(value) {
  let hash = 2166136261
  for (let i = 0; i < String(value).length; i += 1) {
    hash ^= String(value).charCodeAt(i)
    hash = Math.imul(hash, 16777619)
  }
  return hash >>> 0
}

function seededRandom(seed) {
  let state = seed >>> 0
  return () => {
    state = (state * 1664525 + 1013904223) >>> 0
    return state / 4294967296
  }
}

function clock(date) {
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function axisClock(date, durationSeconds) {
  return durationSeconds <= 600
    ? `${clock(date)}:${String(date.getSeconds()).padStart(2, '0')}`
    : clock(date)
}

function buildOnlineSeries(startMs, durationSeconds, peakOnline, seed) {
  const random = seededRandom(seed)
  const count = 12
  const values = []
  const peakIndex = 4 + Math.floor(random() * 4)
  for (let i = 0; i < count; i += 1) {
    const progress = i / (count - 1)
    const shape = 0.34 + Math.sin(progress * Math.PI) * 0.48
    const spike = i === peakIndex ? 0.2 : 0
    const noise = (random() - 0.5) * 0.12
    values.push(Math.max(1, Math.round(peakOnline * Math.min(1, shape + spike + noise))))
  }
  values[peakIndex] = Math.max(values[peakIndex], peakOnline)
  return values.map((value, index) => {
    const at = new Date(startMs + (durationSeconds * 1000 * index) / (count - 1))
    return { label: axisClock(at, durationSeconds), value }
  })
}

function buildGiftMinutes(giftIncome, seed, startMs, durationSeconds) {
  const random = seededRandom(seed ^ 0x9e3779b9)
  const count = 12
  const weights = Array.from({ length: count }, (_, index) => 0.2 + random() * (1 + Math.sin(index / 2) * 0.25))
  const totalWeight = weights.reduce((sum, value) => sum + value, 0)
  let remaining = Math.max(0, Math.round(giftIncome || 0))
  return weights.map((weight, index) => {
    const value = index === count - 1 ? remaining : Math.min(remaining, Math.round((giftIncome * weight) / totalWeight))
    remaining -= value
    const at = new Date(startMs + (durationSeconds * 1000 * index) / (count - 1))
    return { label: axisClock(at, durationSeconds), value: Math.max(0, value) }
  })
}

function normalizeDistribution(items) {
  const total = items.reduce((sum, item) => sum + item.value, 0) || 1
  let remaining = 100
  return items.map((item, index) => {
    const value = index === items.length - 1 ? remaining : Math.round((item.value / total) * 100)
    remaining -= value
    return { ...item, value: Math.max(0, value) }
  })
}

function buildAudience(seed) {
  const random = seededRandom(seed ^ 0x85ebca6b)
  const gender = normalizeDistribution([
    { label: '女性', value: 48 + Math.round(random() * 18) },
    { label: '男性', value: 30 + Math.round(random() * 14) },
    { label: '其他', value: 4 + Math.round(random() * 5) }
  ])
  const age = normalizeDistribution([
    { label: '18-24', value: 24 + Math.round(random() * 12) },
    { label: '25-30', value: 30 + Math.round(random() * 12) },
    { label: '31-40', value: 20 + Math.round(random() * 9) },
    { label: '40+', value: 8 + Math.round(random() * 6) }
  ])
  const region = normalizeDistribution([
    { label: '华东', value: 28 + Math.round(random() * 8) },
    { label: '华南', value: 20 + Math.round(random() * 7) },
    { label: '华北', value: 16 + Math.round(random() * 6) },
    { label: '西南', value: 11 + Math.round(random() * 5) },
    { label: '海外', value: 5 + Math.round(random() * 4) }
  ])
  return { gender, age, region }
}

export function buildLiveReport({ room, sessionId, startTime, endTime, metrics, audience = null }) {
  const startMs = new Date(startTime).getTime()
  const endMs = new Date(endTime).getTime()
  const durationSeconds = Math.max(1, Math.round((endMs - startMs) / 1000))
  const peakOnline = Math.max(1, Math.round(metrics?.peakOnline || metrics?.online || 1))
  const giftIncome = Math.max(0, Math.round(metrics?.giftIncome || 0))
  const seed = hashString(sessionId)
  const random = seededRandom(seed ^ 0xc2b2ae35)
  const avgStaySeconds = 8 * 60 + Math.round(random() * 10 * 60)
  const interactionRate = Number((12 + random() * 14).toFixed(1))

  return {
    sessionId,
    roomId: String(room.id),
    title: room.title || '直播复盘',
    category: room.category || '音乐现场',
    coverUrl: room.coverUrl || '',
    startTime: new Date(startMs).toISOString(),
    endTime: new Date(endMs).toISOString(),
    durationSeconds,
    peakOnline,
    avgStaySeconds,
    interactionRate,
    giftIncome,
    changes: {
      peakOnline: Number((6 + random() * 15).toFixed(1)),
      avgStay: Number((random() * 9 - 3).toFixed(1)),
      interactionRate: Number((random() * 10 - 3).toFixed(1)),
      giftIncome: Number((random() * 16 - 2).toFixed(1))
    },
    onlineSeries: buildOnlineSeries(startMs, durationSeconds, peakOnline, seed),
    giftMinutes: buildGiftMinutes(giftIncome, seed, startMs, durationSeconds),
    audience: audience || buildAudience(seed),
    settlement: {
      status: 'PENDING',
      label: '待结算（T+1）',
      note: '本场礼物收益将在直播结束后的次日（T+1）结算，结算完成后可在礼物收益中提现。'
    }
  }
}

export function fallbackLiveReport(sessionId) {
  const roomId = String(sessionId || '11001').split('-')[0]
  const end = new Date()
  const start = new Date(end.getTime() - (2 * 60 * 60 + 14 * 60) * 1000)
  const report = buildLiveReport({
    room: { id: roomId, title: `直播复盘 · 房间 ${roomId}`, category: '音乐现场' },
    sessionId: String(sessionId || `${roomId}-${Date.now()}`),
    startTime: start.toISOString(),
    endTime: end.toISOString(),
    metrics: { peakOnline: 356, giftIncome: 7851 }
  })
  report.settlement = {
    status: 'SETTLED',
    label: '已结算',
    note: '本场收益已完成结算，可在礼物收益中查看明细并申请提现。'
  }
  return report
}

export function formatDuration(totalSeconds) {
  const seconds = Math.max(0, Math.round(Number(totalSeconds) || 0))
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const rest = seconds % 60
  if (hours) return `${hours}小时${String(minutes).padStart(2, '0')}分`
  return `${minutes}分${String(rest).padStart(2, '0')}秒`
}

export function formatClockTime(totalSeconds) {
  const seconds = Math.max(0, Math.round(Number(totalSeconds) || 0))
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return [h, m, s].map((value) => String(value).padStart(2, '0')).join(':')
}

export function formatDateTime(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
