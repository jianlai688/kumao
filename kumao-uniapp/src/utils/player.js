import { reactive } from 'vue'
import { fileUrl } from '@/utils/media'

const state = reactive({
  song: null,
  queue: [],
  index: -1,
  playing: false,
  current: 0,
  duration: 0,
  shuffle: uni.getStorageSync('km_shuffle') === '1',
  rate: Number(uni.getStorageSync('km_rate')) || 1,
  sleepAt: 0
})

let audio = null

function songId(s) {
  return s ? s.songId || s.id : null
}

function ctx() {
  if (audio) return audio
  audio = uni.createInnerAudioContext()
  audio.obeyMuteSwitch = false
  audio.onCanplay(() => {
    if (audio.duration) state.duration = audio.duration
  })
  audio.onTimeUpdate(() => {
    state.current = audio.currentTime || 0
    if (audio.duration) state.duration = audio.duration
  })
  audio.onPlay(() => { state.playing = true })
  audio.onPause(() => { state.playing = false })
  audio.onStop(() => { state.playing = false })
  audio.onEnded(() => next())
  audio.onError(() => { state.playing = false })
  return audio
}

export function play(song, list) {
  if (!song) return
  // 单曲付费：未购买时先跳转详情页购买，避免从列表直接播放
  if (Number(song.price) > 0) {
    const owned = (uni.getStorageSync('km_owned_songs') || []).map(String)
    if (!owned.includes(String(songId(song)))) {
      uni.showToast({ title: '付费单曲，请先购买', icon: 'none' })
      uni.navigateTo({ url: `/pages/song/song?id=${songId(song)}` })
      return
    }
  }
  if (Array.isArray(list) && list.length) {
    state.queue = list
    const idx = list.findIndex((s) => songId(s) === songId(song))
    state.index = idx >= 0 ? idx : 0
  } else if (!state.queue.length) {
    state.queue = [song]
    state.index = 0
  }
  if (!song.audioUrl) {
    uni.showToast({ title: '暂无音源，试试其他歌曲', icon: 'none' })
    state.song = song
    state.playing = false
    return
  }
  state.song = song
  state.current = 0
  state.duration = song.duration || 0
  const a = ctx()
  a.src = fileUrl(song.audioUrl)
  applyRate()
  a.play()
  state.playing = true
  uni.setStorageSync('km_current_song', song)
}

export function toggle() {
  if (!state.song) return
  const a = ctx()
  if (state.playing) {
    a.pause()
    state.playing = false
  } else {
    if (!a.src) return play(state.song)
    a.play()
    state.playing = true
  }
}

export function seek(seconds) {
  if (!audio || !state.song) return
  audio.seek(Math.max(0, seconds))
  state.current = seconds
}

export function next() {
  if (!state.queue.length) return
  const idx = state.shuffle && state.queue.length > 1 ? randomIndex() : (state.index + 1) % state.queue.length
  state.index = idx
  play(state.queue[idx])
}

export function prev() {
  if (!state.queue.length) return
  const idx = (state.index - 1 + state.queue.length) % state.queue.length
  state.index = idx
  play(state.queue[idx])
}

export function clear() {
  if (audio) {
    audio.stop()
    audio.src = ''
  }
  state.song = null
  state.queue = []
  state.index = -1
  state.playing = false
  state.current = 0
  state.duration = 0
  uni.removeStorageSync('km_current_song')
}

export function playAt(index) {
  if (!state.queue.length) return
  const i = ((index % state.queue.length) + state.queue.length) % state.queue.length
  state.index = i
  play(state.queue[i])
}

function randomIndex() {
  if (state.queue.length < 2) return 0
  let i = state.index
  while (i === state.index) i = Math.floor(Math.random() * state.queue.length)
  return i
}

function applyRate() {
  if (!audio) return
  try {
    audio.playbackRate = state.rate
  } catch (e) { /* 某些内核不支持变速 */ }
}

export function toggleShuffle() {
  state.shuffle = !state.shuffle
  uni.setStorageSync('km_shuffle', state.shuffle ? '1' : '0')
  return state.shuffle
}

export function setRate(rate) {
  state.rate = Number(rate) || 1
  uni.setStorageSync('km_rate', String(state.rate))
  applyRate()
  return state.rate
}

export function setSleep(minutes) {
  state.sleepAt = minutes > 0 ? Date.now() + minutes * 60 * 1000 : 0
  return state.sleepAt
}

export function playNext(song) {
  if (!song) return
  const idx = state.index >= 0 ? state.index : 0
  state.queue.splice(idx + 1, 0, song)
}

export function stopForSleep() {
  if (audio) audio.pause()
  state.playing = false
  state.sleepAt = 0
}

export function formatTime(sec) {
  const s = Math.max(0, Math.floor(Number(sec) || 0))
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
}

export function usePlayer() {
  return {
    state, play, toggle, seek, next, prev, clear, playAt, formatTime,
    toggleShuffle, setRate, setSleep, playNext
  }
}



