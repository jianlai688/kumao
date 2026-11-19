<template>
  <div class="broadcast-page" :style="{ backgroundImage: room?.coverUrl ? `url(${room.coverUrl})` : undefined }">
    <div class="broadcast-scrim"></div>
    <div class="broadcast-glow" aria-hidden="true"></div>

    <header class="broadcast-head">
      <div class="brand">
        <span class="brand-mark">KUMAO</span>
        <span>MUSIC · LIVE</span>
      </div>
      <span class="live-pill"><i></i>{{ isLive ? 'LIVE' : 'OFFLINE' }}<b v-if="isLive && onlineCount">{{ onlineCount }}</b></span>
    </header>

    <main class="broadcast-main">
      <section class="broadcast-copy">
        <p class="eyebrow">正在直播</p>
        <h1>{{ room?.title || '酷猫直播间' }}</h1>
        <p class="anchor-line">
          {{ room?.anchorName || '酷猫主播' }}
          <span v-if="room?.category">· {{ room.category }}</span>
          <span v-if="room?.location">· {{ room.location }}</span>
        </p>

        <div v-if="displayTrack" class="now-playing">
          <div class="now-cover">
            <img :src="displayTrack.coverUrl || fallbackCover" alt="" @error="onImg" />
            <span class="cover-pulse" aria-hidden="true"></span>
          </div>
          <div class="now-copy">
            <span class="now-label">NOW PLAYING</span>
            <strong>{{ displayTrack.name }}</strong>
            <span>{{ displayTrack.singerName || '未知歌手' }}</span>
            <div class="now-progress"><i :style="{ width: `${progress}%` }"></i></div>
          </div>
        </div>
        <div v-else class="waiting-copy">
          <span class="waiting-dot"></span>
          <span>直播进行中 · 等待主播设置歌单</span>
        </div>
      </section>

      <aside class="broadcast-queue">
        <div class="queue-head">
          <span>待唱队列</span>
          <small>{{ queue.length }} 首</small>
        </div>
        <div v-if="queue.length" class="queue-list">
          <div
            v-for="(track, index) in queue.slice(0, 8)"
            :key="track.id"
            :class="['queue-row', { current: index === currentIndex }]"
          >
            <span class="queue-index">{{ String(index + 1).padStart(2, '0') }}</span>
            <span class="queue-title">{{ track.name }}</span>
            <span class="queue-singer">{{ track.singerName }}</span>
            <span v-if="index === currentIndex" class="queue-live">正在唱</span>
          </div>
        </div>
        <p v-else class="queue-empty">主播正在准备歌单</p>
      </aside>
    </main>

    <footer class="broadcast-foot">
      <span>点击进入直播间 · KUMAO MUSIC</span>
      <span class="broadcast-bars" aria-hidden="true">
        <i v-for="n in 5" :key="n" :style="{ animationDelay: `${n * 0.12}s` }"></i>
      </span>
    </footer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { liveApi, musicApi } from '@/api'

const route = useRoute()
const roomId = String(route.params.id || '')
const room = ref(null)
const queue = ref([])
const currentIndex = ref(0)
const progress = ref(12)
const onlineCount = ref(0)
const isLive = ref(true)
const nowPlaying = ref(null)
const fallbackCover = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="600" height="600"><rect width="600" height="600" fill="#1b1026"/><circle cx="300" cy="300" r="110" fill="none" stroke="#c084fc" stroke-opacity=".35" stroke-width="3"/><circle cx="300" cy="300" r="12" fill="#e9b8ff"/></svg>'
)

let timer = null
let socket = null
let retryTimer = null

const currentTrack = computed(() => queue.value[currentIndex.value] || null)
const displayTrack = computed(() => nowPlaying.value || currentTrack.value)

function toTrack(song) {
  return {
    id: song.songId || song.id,
    name: song.songName || song.name,
    singerName: song.singerName,
    coverUrl: song.coverUrl,
    duration: Number(song.duration) || 240
  }
}

function onImg(event) {
  event.target.src = fallbackCover
}

function nextTrack() {
  if (!queue.value.length) return
  currentIndex.value = (currentIndex.value + 1) % queue.value.length
  progress.value = 0
}

function applyNowPlaying(message) {
  nowPlaying.value = {
    id: message.songId,
    name: message.songName || '正在播放',
    singerName: message.singerName,
    coverUrl: message.coverUrl,
    duration: message.duration || 240
  }
  if (Number.isFinite(Number(message.progress))) progress.value = Math.max(0, Math.min(100, Number(message.progress)))
  if (Number.isFinite(Number(message.queueIndex)) && Number(message.queueIndex) >= 0 && Number(message.queueIndex) < queue.value.length) {
    currentIndex.value = Number(message.queueIndex)
  }
}

function connectWs() {
  if (!roomId) return
  const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
  try {
    socket = new WebSocket(`${protocol}://${location.host}/ws/live?roomId=${roomId}`)
  } catch (e) {
    return
  }
  socket.onmessage = (event) => {
    if (event.data === 'pong') return
    try {
      const message = JSON.parse(event.data)
      if (message.type === 'ROOM_METRICS') {
        onlineCount.value = Number(message.onlineCount) || 0
        if (message.roomStatus) isLive.value = message.roomStatus === 'LIVING'
      } else if (message.type === 'STREAM_STATUS') {
        isLive.value = message.content === 'ONLINE'
        if (!isLive.value) onlineCount.value = 0
      } else if (message.type === 'NOW_PLAYING') {
        applyNowPlaying(message)
      }
    } catch (e) { /* ignore malformed frames */ }
  }
  socket.onclose = () => {
    socket = null
    retryTimer = window.setTimeout(connectWs, 3000)
  }
  socket.onerror = () => {
    try { socket?.close() } catch (e) { /* ignore */ }
  }
}

function disconnectWs() {
  if (retryTimer) {
    window.clearTimeout(retryTimer)
    retryTimer = null
  }
  if (socket) {
    try { socket.close() } catch (e) { /* ignore */ }
    socket = null
  }
}

onMounted(async () => {
  try {
    const detail = await liveApi.room(roomId)
    room.value = detail.data
    isLive.value = room.value?.status === 'LIVING'
    onlineCount.value = Number(room.value?.realtimeOnline || room.value?.onlineCount) || 0
    const ids = String(room.value?.playlistIds || '').split(',').filter(Boolean)
    if (ids.length) {
      const songs = await Promise.all(ids.map((id) => musicApi.song(id).then((r) => r.data).catch(() => null)))
      queue.value = songs.filter(Boolean).map(toTrack)
    }
  } catch (e) {
    room.value = null
  }
  timer = setInterval(() => {
    progress.value += 1
    if (progress.value >= 100) nextTrack()
  }, 1000)
  connectWs()
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  disconnectWs()
})
</script>

<style scoped>
.broadcast-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 42px 58px 30px;
  color: #fff;
  background-color: #160d20;
  background-position: center;
  background-size: cover;
  font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
}
.broadcast-scrim {
  position: absolute;
  inset: 0;
  background: linear-gradient(110deg, rgba(12, 7, 20, .94) 0%, rgba(25, 10, 39, .82) 45%, rgba(57, 17, 85, .58) 100%);
}
.broadcast-glow {
  position: absolute;
  width: 760px;
  height: 760px;
  right: -180px;
  top: -260px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(214, 112, 255, .38), transparent 64%);
  filter: blur(5px);
  pointer-events: none;
}
.broadcast-head, .broadcast-main, .broadcast-foot { position: relative; z-index: 1; }
.broadcast-head { display: flex; align-items: center; justify-content: space-between; }
.brand { display: flex; align-items: center; gap: 9px; color: rgba(255, 255, 255, .68); font-size: 13px; font-weight: 700; letter-spacing: 2px; }
.brand-mark { color: #fff; font-size: 18px; letter-spacing: 1px; }
.live-pill { display: inline-flex; align-items: center; gap: 7px; height: 28px; padding: 0 12px; border: 1px solid rgba(255, 255, 255, .2); border-radius: 999px; background: rgba(255, 255, 255, .08); font-size: 11px; font-weight: 800; letter-spacing: 1px; }
.live-pill i { width: 7px; height: 7px; border-radius: 50%; background: #ff4d6d; box-shadow: 0 0 0 4px rgba(255, 77, 109, .16); }
.live-pill b { margin-left: 2px; color: rgba(255, 255, 255, .66); font-size: 10px; font-weight: 500; letter-spacing: 0; }
.broadcast-main { flex: 1; display: flex; align-items: center; justify-content: space-between; gap: 60px; min-height: 0; }
.broadcast-copy { max-width: 760px; }
.eyebrow { margin: 0 0 10px; color: rgba(255, 255, 255, .6); font-size: 16px; font-weight: 600; letter-spacing: 3px; }
.broadcast-copy h1 { margin: 0; font-size: clamp(44px, 5.2vw, 78px); font-weight: 900; line-height: 1.08; letter-spacing: -2px; text-shadow: 0 8px 28px rgba(0, 0, 0, .22); }
.anchor-line { margin: 16px 0 0; color: rgba(255, 255, 255, .72); font-size: 18px; }
.now-playing { display: flex; align-items: center; gap: 18px; margin-top: 38px; max-width: 600px; padding: 16px; border: 1px solid rgba(255, 255, 255, .14); border-radius: 18px; background: rgba(17, 9, 27, .44); backdrop-filter: blur(14px); }
.now-cover { position: relative; width: 92px; height: 92px; flex: none; overflow: hidden; border-radius: 12px; }
.now-cover img { width: 100%; height: 100%; object-fit: cover; }
.cover-pulse { position: absolute; right: 5px; bottom: 5px; width: 9px; height: 9px; border-radius: 50%; background: #ff4d6d; box-shadow: 0 0 0 4px rgba(255, 77, 109, .18); }
.now-copy { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 4px; }
.now-label { color: #d8b4fe; font-size: 10px; font-weight: 800; letter-spacing: 2px; }
.now-copy strong { overflow: hidden; font-size: 21px; font-weight: 800; text-overflow: ellipsis; white-space: nowrap; }
.now-copy > span:not(.now-label) { color: rgba(255, 255, 255, .64); font-size: 13px; }
.now-progress { height: 3px; margin-top: 10px; overflow: hidden; border-radius: 99px; background: rgba(255, 255, 255, .18); }
.now-progress i { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #c084fc, #fb7185); transition: width 1s linear; }
.waiting-copy { display: flex; align-items: center; gap: 9px; margin-top: 34px; color: rgba(255, 255, 255, .64); font-size: 14px; }
.waiting-dot { width: 7px; height: 7px; border-radius: 50%; background: #c084fc; box-shadow: 0 0 0 4px rgba(192, 132, 252, .16); }
.broadcast-queue { width: 320px; flex: none; padding: 19px 20px; border: 1px solid rgba(255, 255, 255, .14); border-radius: 18px; background: rgba(14, 8, 22, .42); backdrop-filter: blur(14px); }
.queue-head { display: flex; align-items: baseline; justify-content: space-between; padding-bottom: 12px; border-bottom: 1px solid rgba(255, 255, 255, .14); color: rgba(255, 255, 255, .9); font-size: 13px; font-weight: 700; }
.queue-head small { color: rgba(255, 255, 255, .5); font-size: 11px; font-weight: 400; }
.queue-list { display: flex; flex-direction: column; gap: 2px; padding-top: 8px; }
.queue-row { display: grid; grid-template-columns: 25px minmax(0, 1fr) auto; align-items: center; gap: 7px; padding: 8px 5px; border-radius: 8px; color: rgba(255, 255, 255, .65); }
.queue-row.current { background: rgba(216, 180, 254, .14); color: #fff; }
.queue-index { color: rgba(255, 255, 255, .35); font-size: 10px; font-variant-numeric: tabular-nums; }
.queue-title { overflow: hidden; font-size: 12px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.queue-singer { overflow: hidden; max-width: 86px; color: rgba(255, 255, 255, .42); font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.queue-live { grid-column: 2 / -1; color: #d8b4fe; font-size: 9px; font-weight: 700; }
.queue-empty { margin: 0; padding: 34px 0 24px; color: rgba(255, 255, 255, .45); font-size: 12px; text-align: center; }
.broadcast-foot { display: flex; align-items: center; justify-content: space-between; padding-top: 15px; border-top: 1px solid rgba(255, 255, 255, .14); color: rgba(255, 255, 255, .5); font-size: 12px; }
.broadcast-bars { display: flex; align-items: flex-end; gap: 4px; height: 22px; }
.broadcast-bars i { display: block; width: 5px; height: 8px; border-radius: 2px; background: rgba(255, 255, 255, .68); animation: bars 1s ease-in-out infinite alternate; }
@keyframes bars { from { height: 7px; opacity: .5; } to { height: 21px; opacity: 1; } }
@media (max-width: 900px) {
  .broadcast-page { padding: 24px 22px 20px; }
  .broadcast-main { gap: 20px; align-items: flex-start; flex-direction: column; justify-content: center; }
  .broadcast-copy h1 { font-size: 34px; }
  .broadcast-queue { width: 100%; }
  .broadcast-queue .queue-row:nth-child(n+6) { display: none; }
}
</style>
