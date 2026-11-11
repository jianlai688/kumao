<template>
  <div class="player-wrap" v-if="player.hasCurrent">
    <div class="player-bar">
      <!-- 顶部细进度条 -->
      <div class="top-progress" @click="seekByEvent">
        <div class="top-progress-fill" :style="{ width: percent + '%' }"></div>
      </div>

      <!-- 左侧：封面 + 信息 -->
      <div class="left">
        <div class="cover-ring">
          <img :src="player.current.coverUrl || fallback" class="mini-cover" @error="onImgError" />
          <div class="cover-spin" :class="{ paused: !player.playing }"></div>
        </div>
        <div class="meta">
          <div class="song-name">{{ player.current.name }}</div>
          <div class="singer">{{ player.current.singerName }}</div>
        </div>
        <button class="like-btn" :class="{ liked }" aria-label="收藏当前歌曲" @click="like">
          <el-icon><Star /></el-icon>
        </button>
      </div>

      <!-- 中间：控制 -->
      <div class="center">
        <div class="controls">
          <button class="ctrl sm" title="上一首" aria-label="上一首" @click="player.prev()"><el-icon><DArrowLeft /></el-icon></button>
          <button class="ctrl main" :aria-label="player.playing ? '暂停播放' : '继续播放'" @click="player.toggle()">
            <el-icon v-if="player.playing"><VideoPause /></el-icon>
            <el-icon v-else><VideoPlay /></el-icon>
          </button>
          <button class="ctrl sm" title="下一首" aria-label="下一首" @click="player.next()"><el-icon><DArrowRight /></el-icon></button>
        </div>
        <div class="progress-row">
          <span class="time">{{ fmt(player.progress) }}</span>
          <el-slider v-model="progressValue" :max="player.duration || 100" :show-tooltip="false" @change="seek" />
          <span class="time">{{ fmt(player.duration) }}</span>
        </div>
      </div>

      <!-- 右侧：音质 / MV / 歌词 / 顺序 / 音量 / 列表 -->
      <div class="right">
        <el-dropdown trigger="click" @command="setQuality">
          <button class="quality-btn" title="音质" aria-label="选择音质">
            {{ qualityLabel }}<el-icon class="caret"><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="q in qualityOptions" :key="q.value" :command="q.value"
                                :class="{ 'is-active': player.quality === q.value }">
                <span>{{ q.label }}</span>
                <span class="q-desc">{{ q.desc }}</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <button v-if="player.mv" class="mv-btn" title="播放 MV" aria-label="播放当前歌曲 MV" @click="openMv">MV</button>

        <button class="ctrl sm" :class="{ on: showLyric }" title="歌词" aria-label="显示歌词" @click="showLyric = !showLyric">
          <el-icon><Document /></el-icon>
        </button>

        <button class="ctrl sm" :title="player.modeLabel" :aria-label="`播放模式：${player.modeLabel}`" @click="player.cycleMode()">
          <el-icon><component :is="modeIcon" /></el-icon>
        </button>

        <div class="volume-box">
          <el-icon class="vol-icon" aria-hidden="true"><Bell /></el-icon>
          <el-slider v-model="volume" :max="1" :step="0.01" class="volume" :show-tooltip="false" aria-label="音量" @change="onVolume" />
        </div>

        <button class="ctrl sm" title="播放列表" aria-label="打开播放列表" @click="showQueue = !showQueue">
          <el-icon><List /></el-icon>
        </button>
      </div>
    </div>

    <!-- 播放列表 -->
    <transition name="queue-fade">
      <div v-if="showQueue" class="queue-pop km-glass">
        <div class="queue-head">
          <span>播放列表</span>
          <span class="queue-count">{{ player.playlist.length }} 首</span>
        </div>
        <div class="queue-list">
          <div v-for="(t, i) in player.playlist" :key="t.id"
               :class="['queue-item', { active: i === player.index }]" @click="playAt(i)">
            <span class="q-index">{{ String(i + 1).padStart(2, '0') }}</span>
            <span class="q-name">{{ t.name }}</span>
            <span class="q-singer">{{ t.singerName }}</span>
          </div>
        </div>
      </div>
    </transition>

    <!-- 歌词浮层 -->
    <transition name="lyric-fade">
      <div v-if="showLyric" class="lyric-float km-glass">
        <div class="lyric-head">
          <span class="lyric-title">歌词</span>
          <el-icon class="close" @click="showLyric = false"><Close /></el-icon>
        </div>
        <div class="lyric-lines" ref="lyricBox">
          <div v-for="(line, i) in player.lyric" :key="i"
               :class="['lyric-line', { active: i === player.lyricIndex }]">{{ line.text || '· · ·' }}</div>
          <el-empty v-if="!player.lyric.length" description="暂无歌词" :image-size="60" />
        </div>
      </div>
    </transition>

    <!-- MV 全屏播放 -->
    <teleport to="body">
      <transition name="mv-fade">
        <div v-if="mvOpen" class="mv-overlay" @click.self="closeMv">
          <div class="mv-shell">
            <button class="mv-close" @click="closeMv"><el-icon><Close /></el-icon></button>
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
      @play="player.playing = true"
      @pause="player.playing = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { usePlayerStore } from '@/stores/player'
import { likeApi, musicApi } from '@/api'

const route = useRoute()
const player = usePlayerStore()
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

const fallback = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120"><rect width="120" height="120" fill="#1a1c21"/><text x="60" y="68" font-size="34" text-anchor="middle" fill="#3a3d45">K</text></svg>'
)

const qualityOptions = [
  { value: 'standard', label: '标准', desc: '128kbps' },
  { value: 'hq', label: '高品', desc: '320kbps' },
  { value: 'lossless', label: '无损', desc: 'FLAC' },
  { value: 'ultra', label: '极高', desc: 'Hi-Res' }
]
const qualityLabel = computed(() => qualityOptions.find((q) => q.value === player.quality)?.label || '极高')
const modeIcon = computed(() => ({
  order: 'Sort',
  loop: 'Refresh',
  single: 'RefreshRight',
  random: 'Switch'
}[player.mode] || 'Refresh'))

const audioSrc = computed(() => {
  const url = player.current?.audioUrl
  if (!url) return ''
  return `${url}${url.includes('?') ? '&' : '?'}quality=${player.quality}`
})

const percent = computed(() => {
  if (!player.duration) return 0
  return Math.min(100, (player.progress / player.duration) * 100)
})

watch(
  () => player.current,
  () => {
    progressValue.value = 0
    liked.value = false
    if (player.current?.id && localStorage.getItem('km_token')) {
      likeApi.status(player.current.id).then((r) => (liked.value = !!r.data)).catch(() => {})
    }
    requestAnimationFrame(() => {
      if (audioRef.value) {
        audioRef.value.volume = volume.value
        // 只有用户主动播放（playing=true）时才启动音频，避免换曲或重挂载时自动播放。
        if (player.playing) audioRef.value.play().catch(() => {})
      }
    })
  }
)

watch(
  () => player.playing,
  (v) => {
    if (!audioRef.value) return
    if (v) audioRef.value.play().catch(() => {})
    else audioRef.value.pause()
  }
)

// 切换音质：保持当前播放进度
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
    // 浏览器会因 src 变化自动加载；这里兜底处理未触发 loadedmetadata 的情况
    setTimeout(() => {
      if (wasPlaying && audio.paused) audio.play().catch(() => {})
    }, 800)
  }
)

// 切换路由时关闭 MV，避免遮罩挡住新页面
watch(
  () => route.fullPath,
  () => {
    if (mvOpen.value) closeMv()
  }
)

// 歌曲页点击「切换 MV」时，等待 MV 信息加载完成后打开
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

// 歌词自动滚动到当前行
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

function onTimeUpdate() {
  if (!audioRef.value) return
  progressValue.value = audioRef.value.currentTime
  player.setProgress(audioRef.value.currentTime)
}

function onLoaded() {
  player.setDuration(audioRef.value.duration || 0)
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

function seekByEvent(e) {
  if (!audioRef.value || !player.duration) return
  const rect = e.currentTarget.getBoundingClientRect()
  const ratio = Math.min(1, Math.max(0, (e.clientX - rect.left) / rect.width))
  audioRef.value.currentTime = ratio * player.duration
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
    /* 全局拦截器已提示（如 VIP 高清 MV 权限不足） */
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
    // 跨布局重挂载时，只有原本处于播放状态才继续；暂停状态不会被自动唤醒。
    if (player.current && player.playing) audioRef.value.play().catch(() => {})
  }
})
</script>

<style scoped>
.player-wrap {
  position: fixed;
  left: var(--km-sidebar-w);
  right: 0;
  bottom: 0;
  z-index: 300;
  padding: 0 18px 14px;
  pointer-events: none;
}

.player-bar {
  pointer-events: auto;
  position: relative;
  height: 70px;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 16px;
  border-radius: 14px;
  background: #0f0d14;
  border: 1px solid rgba(255, 255, 255, .1);
  box-shadow: 0 12px 34px rgba(0, 0, 0, .42);
  overflow: hidden;
}

/* 顶部进度 */
.top-progress {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: rgba(255, 255, 255, .07);
  cursor: pointer;
}
.top-progress-fill {
  height: 100%;
  background: var(--km-accent);
  transition: width .12s linear;
}

/* 左侧 */
.left { display: flex; align-items: center; gap: 10px; width: 236px; flex-shrink: 0; }
.cover-ring { position: relative; width: 44px; height: 44px; flex-shrink: 0; }
.mini-cover {
  width: 44px; height: 44px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
  border: 1px solid rgba(255, 255, 255, .12);
}
.cover-spin {
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  border: 1px solid rgba(216, 180, 254, .32);
  border-left-color: transparent;
  border-bottom-color: transparent;
  animation: km-spin 6s linear infinite;
}
.cover-spin.paused { animation-play-state: paused; }
@keyframes km-spin { to { transform: rotate(360deg); } }

.meta { min-width: 0; }
.song-name {
  font-size: 13px; font-weight: 600;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.singer { font-size: 11px; color: var(--km-text-dim); margin-top: 2px; }

.like-btn {
  width: 30px; height: 30px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: rgba(243, 244, 246, .45);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all .18s;
  flex-shrink: 0;
}
.like-btn:hover { color: var(--km-accent); background: rgba(192, 132, 252, .1); }
.like-btn.liked { color: #ff3e58; }

/* 中间 */
.center { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 2px; min-width: 0; }
.controls { display: flex; align-items: center; gap: 14px; }

.ctrl {
  display: flex; align-items: center; justify-content: center;
  border: none; background: transparent; cursor: pointer;
  color: rgba(243, 244, 246, .8);
  transition: all .18s;
}
.ctrl.sm { width: 32px; height: 32px; font-size: 17px; border-radius: 50%; }
.ctrl.sm:hover { color: var(--km-accent); background: rgba(255, 255, 255, .07); }
.ctrl.sm.on { color: var(--km-accent); }
.ctrl.main {
  width: 40px; height: 40px;
  border-radius: 50%;
  background: var(--km-grad);
  color: #fff;
  font-size: 17px;
}
.ctrl.main:hover { transform: scale(1.06); filter: brightness(1.08); }

.progress-row { display: flex; align-items: center; gap: 10px; width: 100%; max-width: 560px; }
.progress-row :deep(.el-slider) { flex: 1; }
.progress-row :deep(.el-slider__runway) { height: 3px; }
.progress-row :deep(.el-slider__bar) { height: 3px; }
.progress-row :deep(.el-slider__button) { width: 10px; height: 10px; opacity: 0; transition: opacity .18s; }
.progress-row:hover :deep(.el-slider__button) { opacity: 1; }
.time {
  font-size: 11px; color: var(--km-text-faint);
  width: 36px; text-align: center;
  font-variant-numeric: tabular-nums;
}

/* 右侧 */
.right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.quality-btn {
  height: 27px;
  padding: 0 11px;
  border-radius: 999px;
  border: 1px solid rgba(192, 132, 252, .4);
  background: rgba(192, 132, 252, .1);
  color: #e9d5ff;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.quality-btn:hover { background: rgba(192, 132, 252, .2); border-color: rgba(240, 171, 252, .6); }
.quality-btn .caret { font-size: 11px; }
.q-desc { margin-left: 18px; color: var(--km-text-faint); font-size: 11px; }
:deep(.el-dropdown-menu__item.is-active) { color: var(--km-accent); }

.mv-btn {
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, .18);
  background: rgba(255, 255, 255, .06);
  color: var(--km-text);
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}
.mv-btn:hover { border-color: var(--km-accent); color: var(--km-accent); }

.volume-box { display: flex; align-items: center; gap: 6px; }
.vol-icon { color: rgba(243, 244, 246, .4); font-size: 14px; }
.volume { width: 68px; }
.volume :deep(.el-slider__runway) { height: 3px; }
.volume :deep(.el-slider__bar) { height: 3px; }

/* 播放列表 */
.queue-pop {
  pointer-events: auto;
  position: absolute;
  right: 14px;
  bottom: 100px;
  width: 360px;
  max-height: 420px;
  display: flex;
  flex-direction: column;
  border-radius: 20px;
  padding: 16px 18px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, .6);
}
.queue-head { display: flex; justify-content: space-between; align-items: center; font-size: 14px; font-weight: 700; margin-bottom: 10px; }
.queue-count { font-size: 11px; color: var(--km-text-faint); font-weight: 500; }
.queue-list { overflow: auto; }
.queue-item {
  display: flex; align-items: center; gap: 10px;
  padding: 9px 8px; border-radius: 10px; cursor: pointer;
  font-size: 12.5px;
}
.queue-item:hover { background: rgba(255, 255, 255, .06); }
.queue-item.active { background: rgba(192, 132, 252, .1); color: var(--km-accent); }
.q-index { width: 22px; color: var(--km-text-faint); font-size: 11px; font-variant-numeric: tabular-nums; }
.q-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.q-singer { width: 90px; text-align: right; color: var(--km-text-faint); font-size: 11px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* 歌词浮层 */
.lyric-float {
  pointer-events: auto;
  position: absolute;
  right: 14px;
  bottom: 100px;
  width: 340px;
  border-radius: 20px;
  padding: 18px 20px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, .6);
}
.lyric-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.lyric-title { font-weight: 700; font-size: 14px; }
.close { cursor: pointer; color: var(--km-text-dim); }
.close:hover { color: var(--km-accent); }
.lyric-lines { max-height: 300px; overflow-y: auto; }
.lyric-line {
  padding: 7px 0;
  font-size: 13px;
  color: var(--km-text-faint);
  transition: all .25s;
  line-height: 1.5;
}
.lyric-line.active {
  color: var(--km-accent);
  font-weight: 700;
  font-size: 14px;
  transform: scale(1.02);
  transform-origin: left center;
}

/* MV 全屏 */
.mv-overlay {
  position: fixed;
  inset: 0;
  z-index: 1200;
  background: rgba(0, 0, 0, .86);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(18px);
}
.mv-shell { position: relative; width: min(1100px, 92vw); border-radius: 20px; overflow: hidden; background: #000; box-shadow: 0 30px 90px rgba(0, 0, 0, .8); }
.mv-video { width: 100%; max-height: 78vh; display: block; background: #000; }
.mv-caption { padding: 12px 18px; font-size: 13px; color: rgba(243, 244, 246, .7); }
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
.mv-close:hover { background: rgba(192, 132, 252, .9); color: #07080a; }

.lyric-fade-enter-active, .lyric-fade-leave-active,
.queue-fade-enter-active, .queue-fade-leave-active { transition: opacity .2s, transform .2s; }
.lyric-fade-enter-from, .lyric-fade-leave-to,
.queue-fade-enter-from, .queue-fade-leave-to { opacity: 0; transform: translateY(10px); }
.mv-fade-enter-active, .mv-fade-leave-active { transition: opacity .2s; }
.mv-fade-enter-from, .mv-fade-leave-to { opacity: 0; }

@media (max-width: 1180px) {
  .player-wrap { left: 76px; }
  .left { width: 196px; }
  .volume-box { display: none; }
}
@media (max-width: 860px) {
  .player-wrap { left: 0; padding: 0 10px 10px; }
  .player-bar { height: 66px; padding: 0 11px; gap: 10px; }
  .left { width: auto; flex: 1; min-width: 0; }
  .right { gap: 2px; }
  .quality-btn, .mv-btn { display: none; }
  .center { flex: 0 0 auto; }
  .progress-row { display: none; }
  .controls { gap: 8px; }
  .ctrl.sm { width: 30px; height: 30px; }
}
@media (max-width: 520px) {
  .like-btn, .right .ctrl.sm { display: none; }
  .ctrl.main { width: 36px; height: 36px; }
}
</style>
