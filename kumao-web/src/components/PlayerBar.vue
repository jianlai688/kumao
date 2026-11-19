<template>
  <div class="player-wrap" v-if="player.hasCurrent">
    <div class="player-bar">
      <!-- 左侧：封面 + 信息 -->
      <div class="left">
        <router-link class="track-link" :to="songPagePath" :title="`进入 ${player.current.name} 播放页`">
          <img :src="displayCover" class="mini-cover" alt="" @error="onImgError" />
          <div class="meta">
            <div class="song-name">{{ player.current.name }}</div>
            <div class="singer">{{ player.current.singerName }}</div>
          </div>
        </router-link>
        <button class="like-btn" :class="{ liked }" :aria-label="liked ? '取消收藏当前歌曲' : '收藏当前歌曲'" :aria-pressed="liked" @click="like">
          <KmIcon name="heart" :size="18" :filled="liked" />
        </button>
      </div>

      <!-- 中间：播放控制 -->
      <div class="center">
        <div class="controls">
          <span class="ctrl sm ctrl-spacer" aria-hidden="true"></span>
          <button class="ctrl sm" title="上一首" aria-label="上一首" @click="player.prev()">
            <KmIcon name="prev" :size="19" />
          </button>
          <button class="ctrl main" :aria-label="player.playing ? '暂停播放' : player.progress > 0 ? '继续播放' : '播放'" @click="player.toggle()">
            <KmIcon :name="player.playing ? 'pause' : 'play'" :size="19" />
          </button>
          <button class="ctrl sm" title="下一首" aria-label="下一首" @click="player.next()">
            <KmIcon name="next" :size="19" />
          </button>
          <button
            class="ctrl sm mode-ctrl"
            :class="{ on: player.mode !== 'order' }"
            :title="player.modeLabel"
            :aria-label="`播放顺序：${player.modeLabel}，点击切换`"
            @click="cyclePlayMode"
          >
            <KmIcon :name="modeIcon" :size="18" />
          </button>
        </div>
        <div class="progress-row">
          <span class="time">{{ fmt(player.progress) }}</span>
          <el-slider v-model="progressValue" :max="player.duration || 100" :show-tooltip="false" @change="seek" aria-label="播放进度" />
          <span class="time">{{ fmt(player.duration) }}</span>
        </div>
      </div>

      <!-- 右侧：列表 / 歌词 / 音量 / 音质 -->
      <div class="right">
        <button class="ctrl sm" title="播放列表" aria-label="打开播放列表" :aria-pressed="showQueue" @click="showQueue = !showQueue">
          <KmIcon name="queue" :size="18" />
        </button>
        <button class="ctrl sm" :class="{ on: showLyric }" title="歌词" aria-label="显示歌词" :aria-pressed="showLyric" @click="showLyric = !showLyric">
          <KmIcon name="lyrics" :size="18" />
        </button>

        <div class="volume-box">
          <KmIcon class="vol-icon" name="volume" :size="17" />
          <el-slider v-model="volume" :max="1" :step="0.01" class="volume" :show-tooltip="false" aria-label="音量" @change="onVolume" />
        </div>

        <el-dropdown trigger="click" popper-class="km-user-pop" @command="setQuality">
          <button class="quality-btn" title="选择音质" aria-label="选择音质">
            {{ qualityLabel }}
            <KmIcon class="caret" name="chevron-down" :size="11" />
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="q in qualityOptions" :key="q.value" :command="q.value" :class="{ 'is-active': player.quality === q.value }">
                <span>{{ q.label }}</span>
                <span class="q-desc">{{ q.desc }}</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 播放列表 -->
    <transition name="queue-fade">
      <div v-if="showQueue" class="queue-pop">
        <div class="queue-head">
          <span>播放列表</span>
          <span class="queue-count">{{ player.playlist.length }} 首</span>
        </div>
        <div class="queue-list">
          <button v-for="(t, i) in player.playlist" :key="t.id" :class="['queue-item', { active: i === player.index }]" @click="playAt(i)">
            <span class="q-index">{{ String(i + 1).padStart(2, '0') }}</span>
            <span class="q-name">{{ t.name }}</span>
            <span class="q-singer">{{ t.singerName }}</span>
          </button>
          <el-empty v-if="!player.playlist.length" description="播放列表是空的" :image-size="54" />
        </div>
      </div>
    </transition>

    <!-- 歌词浮层 -->
    <transition name="lyric-fade">
      <div v-if="showLyric" class="lyric-float">
        <div class="lyric-head">
          <span class="lyric-title">歌词</span>
          <button class="close" aria-label="关闭歌词" @click="showLyric = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>
        <div class="lyric-lines" ref="lyricBox">
          <div v-for="(line, i) in player.lyric" :key="i" :class="['lyric-line', { active: i === player.lyricIndex }]">{{ line.text || '· · ·' }}</div>
          <el-empty v-if="!player.lyric.length" description="暂无歌词" :image-size="60" />
        </div>
      </div>
    </transition>

    <!-- MV 全屏播放 -->
    <teleport to="body">
      <transition name="mv-fade">
        <div v-if="mvOpen" class="mv-overlay" @click.self="closeMv">
          <div class="mv-shell">
            <button class="mv-close" aria-label="关闭 MV" @click="closeMv"><el-icon><Close /></el-icon></button>
            <video :src="mvUrl" class="mv-video" controls autoplay />
            <div class="mv-caption">{{ player.current.name }} · MV</div>
          </div>
        </div>
      </transition>
    </teleport>

    <audio
      ref="audioRef"
      :src="audioSrc"
      preload="metadata"
      @timeupdate="onTimeUpdate"
      @loadedmetadata="onLoaded"
      @ended="onEnded"
      @error="onAudioError"
      @stalled="onAudioStalled"
      @playing="startProgressLoop"
      @play="player.playing = true"
      @pause="player.playing = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import { usePlayerStore } from '@/stores/player'
import { likeApi, musicApi } from '@/api'
import KmIcon from '@/components/KmIcon.vue'

const route = useRoute()
const player = usePlayerStore()
const songPagePath = computed(() => {
  const id = player.current?.id || player.current?.songId
  return id ? `/song/${id}` : '/'
})
const audioRef = ref(null)
const progressValue = ref(0)
const volume = ref(0.8)
const liked = ref(false)
const showLyric = ref(false)
const showQueue = ref(false)
const lyricBox = ref(null)
const mvOpen = ref(false)
const mvUrl = ref('')
const mvPending = ref(false)
const useLocalAudio = ref(false)
const LOCAL_AUDIO = '/media/kumao-demo.mp3'
let stallTimer = null
let progressRaf = 0

const fallback = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120"><rect width="120" height="120" fill="#f2ecf7"/><text x="60" y="68" font-size="34" text-anchor="middle" fill="#b6a8c5">K</text></svg>'
)

const displayCover = computed(() => {
  if (String(player.current?.id || player.current?.songId) === '3004') return '/media/fuji-cover.svg'
  return player.current?.coverUrl || fallback
})

const qualityOptions = [
  { value: 'standard', label: '标准', desc: '128kbps' },
  { value: 'hq', label: '高品', desc: '320kbps' },
  { value: 'lossless', label: '无损', desc: 'FLAC' },
  { value: 'ultra', label: '极高', desc: 'Hi-Res' }
]
const qualityLabel = computed(() => qualityOptions.find((q) => q.value === player.quality)?.label || '极高')
const modeIcon = computed(() => ({
  order: 'order',
  loop: 'repeat',
  single: 'repeat-one',
  random: 'shuffle'
}[player.mode] || 'repeat'))

const audioSrc = computed(() => {
  if (useLocalAudio.value) return LOCAL_AUDIO
  const url = player.current?.audioUrl
  if (!url) return LOCAL_AUDIO
  return `${url}${url.includes('?') ? '&' : '?'}quality=${player.quality}`
})

function clearStallTimer() {
  if (stallTimer) {
    clearTimeout(stallTimer)
    stallTimer = null
  }
}

function fallbackToLocalAudio() {
  const audio = audioRef.value
  if (!player.current || useLocalAudio.value) return
  useLocalAudio.value = true
  clearStallTimer()
  nextTick(() => {
    const nextAudio = audioRef.value
    if (!nextAudio) return
    nextAudio.load()
    if (player.playing) {
      nextAudio.play().catch(() => {})
    }
  })
}

function armStallTimer() {
  clearStallTimer()
  if (!player.playing) return
  stallTimer = setTimeout(() => {
    const audio = audioRef.value
    if (!audio || !player.playing) return
    if (audio.currentTime < 0.15 && audio.readyState < 3) fallbackToLocalAudio()
  }, 1800)
}

function onAudioError() {
  fallbackToLocalAudio()
}

function onAudioStalled() {
  if (player.playing) fallbackToLocalAudio()
}

watch(
  () => player.current,
  () => {
    progressValue.value = 0
    liked.value = false
    useLocalAudio.value = false
    clearStallTimer()
    if (player.current?.id && localStorage.getItem('km_token')) {
      likeApi.status(player.current.id).then((r) => (liked.value = !!r.data)).catch(() => {})
    }
    requestAnimationFrame(() => {
      if (audioRef.value) {
        audioRef.value.volume = volume.value
        if (player.playing) {
          audioRef.value.play().catch(() => {})
          armStallTimer()
        }
      }
    })
  }
)

watch(
  () => player.playing,
  (v) => {
    if (!audioRef.value) return
    if (v) {
      audioRef.value.play().catch(() => {})
      startProgressLoop()
      armStallTimer()
    } else {
      audioRef.value.pause()
      stopProgressLoop()
      clearStallTimer()
    }
  }
)

watch(
  () => player.quality,
  async () => {
    const audio = audioRef.value
    if (!audio || !player.current) return
    const time = audio.currentTime
    const wasPlaying = !audio.paused
    const restore = () => {
      audio.removeEventListener('loadedmetadata', restore)
      try {
        if (time > 0) audio.currentTime = time
      } catch (e) { /* ignore */ }
      if (wasPlaying) audio.play().catch(() => {})
    }
    audio.addEventListener('loadedmetadata', restore, { once: true })
    await nextTick()
    setTimeout(() => {
      if (wasPlaying && audio.paused) audio.play().catch(() => {})
    }, 800)
  }
)

watch(
  () => route.fullPath,
  () => {
    if (mvOpen.value) closeMv()
  }
)

watch(
  () => player.mvRequest,
  () => {
    if (player.mv) openMv()
    else mvPending.value = true
  }
)
watch(
  () => player.mv,
  (mv) => {
    if (mvPending.value && mv) {
      mvPending.value = false
      openMv()
    }
  }
)

watch(
  () => player.lyricIndex,
  async (i) => {
    if (!showLyric.value) return
    await nextTick()
    const box = lyricBox.value
    if (!box) return
    const el = box.children[i]
    if (el) box.scrollTo({ top: el.offsetTop - box.clientHeight / 2 + 20, behavior: 'smooth' })
  }
)

function setQuality(value) {
  if (value === player.quality) return
  player.setQuality(value)
  ElMessage.success(`已切换音质：${qualityOptions.find((q) => q.value === value)?.label || value}`)
}

function cyclePlayMode() {
  player.cycleMode()
  ElMessage.info(`已切换为${player.modeLabel}`)
}

function onTimeUpdate() {
  if (!audioRef.value) return
  progressValue.value = audioRef.value.currentTime
  player.setProgress(audioRef.value.currentTime)
  if (audioRef.value.currentTime > 0.15) clearStallTimer()
}

/** 用 requestAnimationFrame 同步播放进度，避免 timeupdate（约 4Hz）导致歌词高亮滞后 */
function startProgressLoop() {
  if (progressRaf) return
  const tick = () => {
    const audio = audioRef.value
    if (!audio || audio.paused) {
      progressRaf = 0
      return
    }
    progressValue.value = audio.currentTime
    player.setProgress(audio.currentTime)
    progressRaf = requestAnimationFrame(tick)
  }
  progressRaf = requestAnimationFrame(tick)
}

function stopProgressLoop() {
  if (progressRaf) {
    cancelAnimationFrame(progressRaf)
    progressRaf = 0
  }
}

function onLoaded() {
  player.setDuration(audioRef.value.duration || 0)
  if (player.playing && audioRef.value.paused) audioRef.value.play().catch(() => {})
  if (player.playing) startProgressLoop()
}

function onEnded() {
  if (player.mode === 'single' && audioRef.value) {
    audioRef.value.currentTime = 0
    audioRef.value.play().catch(() => {})
    return
  }
  player.next(true)
}

function seek(val) {
  if (audioRef.value) audioRef.value.currentTime = val
}

function onVolume(val) {
  if (audioRef.value) audioRef.value.volume = val
  player.setVolume(val)
}

async function openMv() {
  if (!player.mv) return ElMessage.info('该歌曲暂无 MV')
  try {
    const res = await musicApi.playMv(player.mv.id)
    const url = res.data?.videoUrl
    if (!url) return ElMessage.error('MV 地址为空')
    mvUrl.value = url
    mvOpen.value = true
    player.playing = false
  } catch (e) {
    /* 全局拦截器已提示 */
  }
}

function closeMv() {
  mvOpen.value = false
  mvUrl.value = ''
  player.playing = true
}

function playAt(i) {
  const track = player.playlist[i]
  if (!track) return
  player.play(track, player.playlist)
  showQueue.value = false
}

function fmt(sec) {
  if (!sec || isNaN(sec)) return '00:00'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

async function like() {
  if (!player.current) return
  if (!localStorage.getItem('km_token')) return ElMessage.info('登录后可收藏歌曲')
  try {
    if (liked.value) {
      await likeApi.unlike(player.current.id)
      liked.value = false
      ElMessage.success('已取消红心')
    } else {
      await likeApi.like(player.current.id)
      liked.value = true
      ElMessage.success('已添加到我喜欢的音乐')
    }
  } catch (e) {
    /* handled */
  }
}

function onImgError(e) {
  e.target.src = fallback
}

onMounted(() => {
  if (audioRef.value) {
    audioRef.value.volume = volume.value
    if (player.current && player.playing) {
      audioRef.value.play().catch(() => {})
      startProgressLoop()
    }
  }
})

onBeforeUnmount(stopProgressLoop)
</script>

<style scoped>
.player-wrap {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 300;
  pointer-events: none;
}

.player-bar {
  pointer-events: auto;
  position: relative;
  height: var(--km-player-h);
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 0 22px;
  border-top: 1px solid #eee9f3;
  background: #fff;
  box-shadow: 0 -8px 28px rgba(57, 36, 80, .035);
}

/* 左侧 */
.left { display: flex; align-items: center; gap: 6px; width: 288px; flex-shrink: 0; min-width: 0; }
.track-link {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  padding: 4px 6px 4px 0;
  border-radius: 8px;
  color: inherit;
  transition: background .16s;
}
.track-link:hover { background: #fbf8fe; }
.mini-cover {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
  display: block;
  background: #f4eef8;
  flex-shrink: 0;
}
.meta { min-width: 0; }
.song-name {
  font-size: 13.5px;
  font-weight: 600;
  color: #231b2d;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.singer { font-size: 11.5px; color: #6f657b; margin-top: 3px; }
.like-btn {
  width: 30px;
  height: 30px;
  margin-left: auto;
  border-radius: 50%;
  border: 0;
  background: transparent;
  color: #776d83;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color .16s, background .16s;
  flex-shrink: 0;
}
.like-btn:hover { color: #c22d68; background: #fff3f8; }
.like-btn.liked { color: #e23b6e; }

/* 中间 */
.center {
  flex: 1;
  min-width: 0;
  max-width: 520px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
}
.controls { display: flex; align-items: center; gap: 13px; height: 40px; }

.ctrl {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: transparent;
  cursor: pointer;
  color: #4d4556;
  transition: color .16s, background .16s, transform .16s;
}
.ctrl.sm { width: 28px; height: 28px; border-radius: 50%; }
.ctrl.sm:hover { color: #8b3fce; background: #f8f3fd; }
.ctrl.sm.on { color: #8b3fce; }
.ctrl.main {
  width: 39px;
  height: 39px;
  border-radius: 50%;
  background: var(--km-grad);
  color: #fff;
  box-shadow: 0 5px 14px rgba(174, 54, 214, .24);
}
.ctrl.main:hover { transform: scale(1.05); }
.ctrl-spacer { visibility: hidden; pointer-events: none; }

.progress-row { display: flex; align-items: center; gap: 10px; width: 100%; max-width: 500px; }
.progress-row :deep(.el-slider) { flex: 1; --el-slider-height: 3px; }
.progress-row :deep(.el-slider__runway) { height: 3px; background: #eee9f3; margin: 0; }
.progress-row :deep(.el-slider__bar) { height: 3px; background: linear-gradient(90deg, #8a2be2 0%, #cf35ba 55%, #f04b9b 100%); }
.progress-row :deep(.el-slider__button-wrapper) { top: -16px; }
.progress-row :deep(.el-slider__button) {
  width: 9px;
  height: 9px;
  border: 2px solid #b33bcf;
  background: #fff;
  opacity: 0;
  transition: opacity .16s;
}
.progress-row:hover :deep(.el-slider__button) { opacity: 1; }
.time {
  width: 34px;
  font-size: 11px;
  color: #6f657b;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

/* 右侧 */
.right { display: flex; align-items: center; justify-content: flex-end; gap: 11px; width: 306px; flex-shrink: 0; }
.volume-box { display: flex; align-items: center; gap: 5px; }
.vol-icon { color: #6f667b; }
.volume { width: 72px; }
.volume :deep(.el-slider__runway) { height: 3px; background: #eee9f3; margin: 0; }
.volume :deep(.el-slider__bar) { height: 3px; background: #5e5569; }
.volume :deep(.el-slider__button) { width: 8px; height: 8px; border: 2px solid #7b7187; background: #fff; }

.quality-btn {
  height: 25px;
  padding: 0 9px;
  border-radius: 7px;
  border: 1px solid #f0c9dc;
  background: #fff5fa;
  color: #bd2d63;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
}
.quality-btn:hover { background: #ffeef7; border-color: #eab2cf; }
.q-desc { margin-left: 18px; color: var(--km-text-faint); font-size: 11px; }
:deep(.el-dropdown-menu__item.is-active) { color: var(--km-brand-violet); }

/* 播放列表 */
.queue-pop {
  pointer-events: auto;
  position: absolute;
  right: 18px;
  bottom: 96px;
  width: 372px;
  max-height: 430px;
  display: flex;
  flex-direction: column;
  border: 1px solid #ece5f3;
  border-radius: 16px;
  padding: 16px 18px;
  background: #fff;
  box-shadow: 0 22px 54px rgba(57, 36, 80, .18);
}
.queue-head { display: flex; justify-content: space-between; align-items: center; font-size: 14px; font-weight: 700; margin-bottom: 10px; }
.queue-count { font-size: 11px; color: var(--km-text-faint); font-weight: 500; }
.queue-list { overflow: auto; }
.queue-item {
  width: 100%;
  border: 0;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 8px;
  border-radius: 9px;
  cursor: pointer;
  font-size: 12.5px;
  color: var(--km-text);
  text-align: left;
}
.queue-item:hover { background: #faf6fe; }
.queue-item.active { background: #f6edff; color: var(--km-brand-violet-dark); }
.q-index { width: 22px; color: #756b81; font-size: 11px; font-variant-numeric: tabular-nums; }
.q-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.q-singer { width: 90px; text-align: right; color: #756b81; font-size: 11px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* 歌词浮层 */
.lyric-float {
  pointer-events: auto;
  position: absolute;
  right: 18px;
  bottom: 96px;
  width: 348px;
  border: 1px solid #ece5f3;
  border-radius: 16px;
  padding: 18px 20px;
  background: #fff;
  box-shadow: 0 22px 54px rgba(57, 36, 80, .18);
}
.lyric-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.lyric-title { font-weight: 700; font-size: 14px; color: var(--km-text); }
.close { border: 0; background: transparent; padding: 2px; cursor: pointer; color: var(--km-text-dim); display: flex; }
.close:hover { color: var(--km-brand-violet); }
.lyric-lines { max-height: 300px; overflow-y: auto; }
.lyric-line {
  padding: 7px 0;
  font-size: 13px;
  color: #756b81;
  transition: color .2s;
  line-height: 1.55;
}
.lyric-line.active { color: var(--km-brand-violet-dark); font-weight: 600; }

/* MV 全屏 */
.mv-overlay {
  position: fixed;
  inset: 0;
  z-index: 1200;
  background: rgba(14, 8, 20, .88);
  display: flex;
  align-items: center;
  justify-content: center;
}
.mv-shell { position: relative; width: min(1100px, 92vw); border-radius: 18px; overflow: hidden; background: #000; box-shadow: 0 30px 90px rgba(0, 0, 0, .7); }
.mv-video { width: 100%; max-height: 78vh; display: block; background: #000; }
.mv-caption { padding: 12px 18px; font-size: 13px; color: rgba(255, 255, 255, .72); }
.mv-close {
  position: absolute;
  right: 12px;
  top: 12px;
  z-index: 2;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, .55);
  color: #fff;
  cursor: pointer;
  display: grid;
  place-items: center;
}
.mv-close:hover { background: rgba(139, 63, 206, .9); }

.lyric-fade-enter-active, .lyric-fade-leave-active,
.queue-fade-enter-active, .queue-fade-leave-active { transition: opacity .18s, transform .18s; }
.lyric-fade-enter-from, .lyric-fade-leave-to,
.queue-fade-enter-from, .queue-fade-leave-to { opacity: 0; transform: translateY(8px); }
.mv-fade-enter-active, .mv-fade-leave-active { transition: opacity .2s; }
.mv-fade-enter-from, .mv-fade-leave-to { opacity: 0; }

@media (max-width: 1180px) {
  .left { width: 238px; }
  .right { width: 238px; gap: 7px; }
  .volume-box { display: none; }
}
@media (max-width: 860px) {
  .player-bar { gap: 10px; padding: 0 12px; }
  .left { width: auto; flex: 1; min-width: 0; }
  .right { width: auto; gap: 2px; }
  .quality-btn, .right .ctrl:first-child { display: none; }
  .center { flex: 0 0 auto; max-width: none; }
  .progress-row { display: none; }
  .controls { gap: 8px; }
  .ctrl.sm { width: 30px; height: 30px; }
}
@media (max-width: 620px) {
  .like-btn, .right .ctrl.sm, .right { display: none; }
  .ctrl.main { width: 36px; height: 36px; }
  .player-bar { height: 74px; }
  .mini-cover { width: 44px; height: 44px; }
  .queue-pop, .lyric-float { right: 10px; left: 10px; width: auto; }
}
</style>
