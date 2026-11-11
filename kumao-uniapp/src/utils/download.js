/**
 * 离线下载：把音频以 Blob 存进 IndexedDB，播放时用 object URL。
 * 纯前端实现，WebView 内也能真正落盘（受浏览器存储配额限制）。
 */
import { API_BASE_URL } from '@/api'
import { fileUrl } from '@/utils/media'

const DB_NAME = 'km_offline'
const DB_VERSION = 1
const STORE = 'songs'
const urlCache = new Map()

function openDB() {
  return new Promise((resolve, reject) => {
    if (typeof indexedDB === 'undefined') {
      reject(new Error('当前环境不支持离线存储'))
      return
    }
    const req = indexedDB.open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = () => {
      const db = req.result
      if (!db.objectStoreNames.contains(STORE)) {
        db.createObjectStore(STORE, { keyPath: 'id' })
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error || new Error('打开本地库失败'))
  })
}

function tx(db, mode) {
  return db.transaction(STORE, mode).objectStore(STORE)
}

function requestToPromise(request) {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

export async function listDownloads() {
  const db = await openDB()
  const all = await requestToPromise(tx(db, 'readonly').getAll())
  db.close()
  return (all || []).sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0))
}

export async function isDownloaded(id) {
  const db = await openDB()
  const row = await requestToPromise(tx(db, 'readonly').get(String(id)))
  db.close()
  return !!row
}

export async function removeDownload(id) {
  const db = await openDB()
  await requestToPromise(tx(db, 'readwrite').delete(String(id)))
  db.close()
  const cached = urlCache.get(String(id))
  if (cached) {
    URL.revokeObjectURL(cached)
    urlCache.delete(String(id))
  }
}

export async function clearDownloads() {
  const db = await openDB()
  await requestToPromise(tx(db, 'readwrite').clear())
  db.close()
  urlCache.forEach((url) => URL.revokeObjectURL(url))
  urlCache.clear()
}

/**
 * 下载歌曲并落库。onProgress(0~100)
 */
export async function downloadSong(song, onProgress) {
  if (!song || !song.audioUrl) throw new Error('该歌曲暂无音源')
  const id = String(song.songId || song.id)

  const target = resolveTarget(song.audioUrl)
  const resp = await fetch(target.url, { mode: 'cors', headers: target.headers })
  if (!resp.ok) throw new Error('下载失败（' + resp.status + '）')

  const total = Number(resp.headers.get('content-length')) || 0
  let received = 0
  let blob

  if (resp.body && resp.body.getReader) {
    const reader = resp.body.getReader()
    const chunks = []
    for (;;) {
      const { done, value } = await reader.read()
      if (done) break
      chunks.push(value)
      received += value.length
      if (onProgress) {
        onProgress(total ? Math.min(99, Math.round((received / total) * 100)) : 50)
      }
    }
    blob = new Blob(chunks, { type: resp.headers.get('content-type') || 'audio/mpeg' })
  } else {
    blob = await resp.blob()
  }

  const record = {
    id,
    name: song.name || song.songName || '未知歌曲',
    singer: song.singer || song.singerName || '',
    coverUrl: song.coverUrl ? fileUrl(song.coverUrl) : '',
    duration: song.duration || 0,
    size: blob.size,
    createdAt: Date.now(),
    blob
  }

  const db = await openDB()
  await requestToPromise(tx(db, 'readwrite').put(record))
  db.close()
  if (onProgress) onProgress(100)
  return record
}

/** 取本地可播放地址（object URL），未下载返回 null */
export async function getLocalUrl(id) {
  const key = String(id)
  if (urlCache.has(key)) return urlCache.get(key)
  const db = await openDB()
  const row = await requestToPromise(tx(db, 'readonly').get(key))
  db.close()
  if (!row || !row.blob) return null
  const url = URL.createObjectURL(row.blob)
  urlCache.set(key, url)
  return url
}

/** 第三方音源没有 CORS 头，跨域时改走后端同源代理并带上登录态。 */
function resolveTarget(audioUrl) {
  const token = uni.getStorageSync('km_token') || ''
  const headers = token ? { Authorization: `Bearer ${token}` } : {}
  const absolute = fileUrl(audioUrl)
  let targetOrigin = ''
  let apiOrigin = ''
  try {
    targetOrigin = new URL(absolute).origin
  } catch (e) {
    targetOrigin = ''
  }
  try {
    apiOrigin = new URL(API_BASE_URL, typeof location !== 'undefined' ? location.href : undefined).origin
  } catch (e) {
    apiOrigin = ''
  }
  // 本站静态资源（/static/...）直接下载，只对第三方音源走代理绕开 CORS。
  if (targetOrigin && apiOrigin && targetOrigin === apiOrigin) {
    return { url: absolute, headers }
  }
  const base = String(API_BASE_URL || '').replace(/\/$/, '')
  return { url: `${base}/music/proxy/media?url=${encodeURIComponent(absolute)}`, headers }
}

export function formatSize(bytes) {
  const v = Number(bytes) || 0
  if (v < 1024) return v + ' B'
  if (v < 1024 * 1024) return (v / 1024).toFixed(1) + ' KB'
  return (v / 1024 / 1024).toFixed(1) + ' MB'
}




