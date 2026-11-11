import { defineStore } from 'pinia'
import { recordApi, musicApi } from '@/api'

const MODES = ['order', 'loop', 'single', 'random']

export const usePlayerStore = defineStore('player', {
  state: () => ({
    current: null,
    playing: false,
    playlist: [],
    index: -1,
    volume: 0.8,
    progress: 0,
    duration: 0,
    lyric: [],
    lyricIndex: 0,
    mode: localStorage.getItem('km_play_mode') || 'loop',
    quality: localStorage.getItem('km_quality') || 'ultra',
    mv: null,
    mvRequest: 0
  }),
  getters: {
    hasCurrent: (state) => !!state.current,
    modeLabel: (state) => ({
      order: '顺序播放',
      loop: '列表循环',
      single: '单曲循环',
      random: '随机播放'
    }[state.mode] || '列表循环')
  },
  actions: {
    play(song, list) {
      if (!song) return
      if (list && list.length) {
        this.playlist = list
        this.index = list.findIndex((s) => String(s.id) === String(song.id))
      } else if (!this.playlist.length) {
        this.playlist = [song]
        this.index = 0
      }
      this.current = song
      this.playing = true
      this.progress = 0
      this.loadLyric(song.id)
      this.loadMv(song.id)
      // 投递播放记录（RabbitMQ 异步落库），未登录时跳过避免触发 401 跳登录
      if (localStorage.getItem('km_token')) {
        recordApi
          .submit({ songId: song.id, device: 'PC', playedSeconds: 0 })
          .catch(() => {})
      }
    },
    loadLyric(songId) {
      this.lyric = []
      this.lyricIndex = 0
      musicApi
        .lyric(songId)
        .then((res) => {
          this.lyric = res.data || []
        })
        .catch(() => {})
    },
    loadMv(songId) {
      this.mv = null
      if (!songId) return
      musicApi
        .songMv(songId)
        .then((res) => {
          if (this.current && String(this.current.id) === String(songId)) {
            this.mv = res.data || null
          }
        })
        .catch(() => {})
    },
    requestMv() {
      this.mvRequest += 1
    },
    toggle() {
      if (!this.current) return
      this.playing = !this.playing
    },
    next(auto = false) {
      if (!this.playlist.length) return
      if (auto && this.mode === 'single') {
        this.progress = 0
        this.playing = true
        return
      }
      if (this.mode === 'random') {
        const len = this.playlist.length
        let nextIndex = this.index
        if (len > 1) {
          while (nextIndex === this.index) {
            nextIndex = Math.floor(Math.random() * len)
          }
        } else {
          nextIndex = 0
        }
        this.index = nextIndex
        this.play(this.playlist[nextIndex], this.playlist)
        return
      }
      if (this.mode === 'order' && this.index >= this.playlist.length - 1) {
        this.playing = false
        this.progress = 0
        return
      }
      this.index = (this.index + 1) % this.playlist.length
      this.play(this.playlist[this.index], this.playlist)
    },
    prev() {
      if (!this.playlist.length) return
      if (this.mode === 'random') {
        this.next(false)
        return
      }
      this.index = (this.index - 1 + this.playlist.length) % this.playlist.length
      this.play(this.playlist[this.index], this.playlist)
    },
    cycleMode() {
      const index = MODES.indexOf(this.mode)
      this.mode = MODES[(index + 1) % MODES.length]
      localStorage.setItem('km_play_mode', this.mode)
    },
    setQuality(quality) {
      this.quality = quality
      localStorage.setItem('km_quality', quality)
    },
    setProgress(p) {
      this.progress = p
      if (!this.lyric.length) return
      let idx = 0
      for (let i = 0; i < this.lyric.length; i++) {
        if (this.lyric[i].time <= p * 1000) idx = i
        else break
      }
      this.lyricIndex = idx
    },
    setDuration(d) {
      this.duration = d
    },
    setVolume(v) {
      this.volume = v
    }
  }
})
