<template>
  <div class="live-preview" :class="{ 'has-controls': controls }">
    <video
      ref="videoRef"
      class="preview-video"
      :poster="poster || undefined"
      :controls="controls"
      autoplay
      muted
      playsinline
      aria-label="直播实时画面"
      @canplay="markReady"
      @loadeddata="markReady"
      @waiting="onWaiting"
      @error="onVideoError"
      @click="onVideoClick"
    ></video>

    <div v-if="active && (loading || error)" class="preview-state" role="status" aria-live="polite">
      <span v-if="loading" class="preview-spinner" aria-hidden="true"></span>
      <template v-if="error">
        <strong>直播画面未接入</strong>
        <span>请确认推流已开始，或稍后重试</span>
        <button type="button" @click.stop="retry">重新连接</button>
      </template>
      <template v-else>
        <strong>正在接入直播画面</strong>
        <span>推流开始后会自动显示</span>
      </template>
    </div>

    <button
      v-if="!controls && !error"
      type="button"
      class="preview-expand"
      aria-label="放大直播画面"
      @click.stop="emit('expand')"
    >
      <el-icon><FullScreen /></el-icon>
    </button>
    <span v-if="active && !controls && !loading && !error" class="preview-live"><i aria-hidden="true"></i>实时画面</span>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import Hls from 'hls.js'
import flvjs from 'flv.js'
import { FullScreen } from '@element-plus/icons-vue'

const props = defineProps({
  src: { type: String, default: '' },
  poster: { type: String, default: '' },
  active: { type: Boolean, default: true },
  controls: { type: Boolean, default: false }
})

const emit = defineEmits(['expand', 'ready', 'offline'])
const videoRef = ref(null)
const loading = ref(false)
const error = ref(false)
let hls = null
let flvPlayer = null
let flvFallbackTimer = null
let mountToken = 0

function destroyHls() {
  if (hls) {
    try { hls.destroy() } catch (e) { /* ignore */ }
    hls = null
  }
}

function destroyFlv() {
  if (flvFallbackTimer) {
    clearTimeout(flvFallbackTimer)
    flvFallbackTimer = null
  }
  if (flvPlayer) {
    try {
      flvPlayer.pause()
      flvPlayer.unload()
      flvPlayer.detachMediaElement()
      flvPlayer.destroy()
    } catch (e) { /* ignore */ }
    flvPlayer = null
  }
}

function destroyPlayers() {
  destroyFlv()
  destroyHls()
}

function normalizeStreamUrl(url) {
  if (!url) return url
  try {
    const parsed = new URL(url, location.origin)
    const localHosts = ['127.0.0.1', 'localhost']
    if (localHosts.includes(parsed.hostname) && !localHosts.includes(location.hostname)) {
      parsed.hostname = location.hostname
    }
    return parsed.toString()
  } catch (e) {
    return url
  }
}

function markReady() {
  if (!props.active) return
  if (flvFallbackTimer) {
    clearTimeout(flvFallbackTimer)
    flvFallbackTimer = null
  }
  loading.value = false
  error.value = false
  videoRef.value?.play?.().catch(() => {})
  emit('ready')
}

function onWaiting() {
  if (props.active && !error.value) loading.value = true
}

function onVideoError() {
  if (!props.active) return
  error.value = true
  loading.value = false
  emit('offline')
}

function fail() {
  if (!props.active) return
  error.value = true
  loading.value = false
  destroyPlayers()
  emit('offline')
}

function attachHls(video, url, run) {
  if (!props.active || run !== mountToken) return
  if (Hls.isSupported()) {
    hls = new Hls({
      enableWorker: true,
      lowLatencyMode: true,
      backBufferLength: 30,
      manifestLoadingMaxRetry: 1,
      levelLoadingMaxRetry: 1,
      fragLoadingMaxRetry: 1
    })
    hls.loadSource(url)
    hls.attachMedia(video)
    hls.on(Hls.Events.MANIFEST_PARSED, () => {
      if (run === mountToken) markReady()
    })
    hls.on(Hls.Events.ERROR, (_event, data) => {
      if (run !== mountToken || !data.fatal || !props.active) return
      if (data.type === Hls.ErrorTypes.MEDIA_ERROR) {
        hls?.recoverMediaError()
        return
      }
      fail()
    })
    return
  }
  // Safari / iOS 原生支持 HLS，其他浏览器使用 hls.js。
  if (video.canPlayType('application/vnd.apple.mpegurl')) {
    video.src = url
    video.addEventListener('loadedmetadata', () => {
      if (run === mountToken) markReady()
    }, { once: true })
    video.play?.().catch(() => {})
    return
  }
  fail()
}

function attach() {
  destroyPlayers()
  const video = videoRef.value
  if (!props.active || !props.src || !video) {
    loading.value = false
    error.value = false
    return
  }

  const run = ++mountToken
  const hlsUrl = normalizeStreamUrl(props.src)
  loading.value = true
  error.value = false
  video.removeAttribute('src')

  // 优先 HTTP-FLV，延迟更低；失败后自动降级 HLS。
  if (flvjs.isSupported()) {
    try {
      const flvUrl = hlsUrl.replace(/\.m3u8(\?.*)?$/i, '.flv$1')
      flvPlayer = flvjs.createPlayer({
        type: 'flv',
        url: flvUrl,
        isLive: true,
        hasAudio: true
      }, {
        enableStashBuffer: false,
        stashInitialSize: 128
      })
      flvPlayer.attachMediaElement(video)
      flvPlayer.on(flvjs.Events.ERROR, () => {
        if (run !== mountToken || !props.active) return
        destroyFlv()
        attachHls(video, hlsUrl, run)
      })
      flvPlayer.load()
      flvPlayer.play?.().catch(() => {})
      // 个别浏览器 / 编码组合下 FLV 会停在 loading 不触发 error，超时后降级 HLS。
      flvFallbackTimer = window.setTimeout(() => {
        if (run !== mountToken || !props.active || !loading.value) return
        destroyFlv()
        attachHls(video, hlsUrl, run)
      }, 2000)
      return
    } catch (e) {
      destroyFlv()
    }
  }
  attachHls(video, hlsUrl, run)
}

function retry() {
  attach()
}

function onVideoClick() {
  if (!props.controls) emit('expand')
}

watch(
  () => [props.src, props.active],
  async () => {
    await nextTick()
    attach()
  }
)

onMounted(() => {
  nextTick(attach)
})

onBeforeUnmount(() => {
  mountToken++
  destroyPlayers()
})
</script>

<style scoped>
.live-preview {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background: #08090b;
}

.preview-video {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  background: #08090b;
}

.live-preview:not(.has-controls) .preview-video {
  cursor: zoom-in;
}

.has-controls .preview-video {
  object-fit: contain;
  background: #000;
}

.preview-state {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 22px;
  text-align: center;
  color: var(--km-text-dim, #a8abb2);
  background: linear-gradient(180deg, rgba(7, 8, 10, .42), rgba(7, 8, 10, .82));
  pointer-events: none;
}

.preview-state strong {
  color: var(--km-text, #f3f4f6);
  font-size: 13px;
  font-weight: 700;
}

.preview-state span {
  font-size: 11px;
}

.preview-state button {
  margin-top: 3px;
  height: 29px;
  padding: 0 13px;
  border: 1px solid rgba(255, 255, 255, .2);
  border-radius: 999px;
  background: rgba(255, 255, 255, .08);
  color: var(--km-text, #f3f4f6);
  font: inherit;
  font-size: 11.5px;
  font-weight: 700;
  cursor: pointer;
  pointer-events: auto;
}

.preview-state button:hover {
  border-color: var(--km-accent, #c084fc);
  color: var(--km-accent, #c084fc);
}

.preview-spinner {
  width: 22px;
  height: 22px;
  border: 2px solid rgba(255, 255, 255, .18);
  border-top-color: var(--km-accent, #c084fc);
  border-radius: 50%;
  animation: preview-spin .8s linear infinite;
}

@keyframes preview-spin {
  to { transform: rotate(360deg); }
}

.preview-expand {
  position: absolute;
  right: 10px;
  bottom: 10px;
  width: 29px;
  height: 29px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, .16);
  border-radius: 9px;
  background: rgba(10, 11, 13, .62);
  color: #fff;
  cursor: pointer;
  backdrop-filter: blur(10px);
  transition: all .18s;
}

.preview-expand:hover {
  background: var(--km-accent, #c084fc);
  border-color: var(--km-accent, #c084fc);
  color: #07080a;
}

.preview-live {
  position: absolute;
  left: 10px;
  bottom: 12px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(10, 11, 13, .6);
  border: 1px solid rgba(255, 255, 255, .12);
  color: rgba(255, 255, 255, .82);
  font-size: 10px;
  font-weight: 700;
  backdrop-filter: blur(10px);
  pointer-events: none;
}

.preview-live i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #fb7185;
  box-shadow: 0 0 0 3px rgba(251, 113, 133, .15);
}
</style>
