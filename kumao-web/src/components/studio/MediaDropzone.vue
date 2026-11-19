<template>
  <div class="media-drop">
    <div
      class="dropzone"
      :class="{ dragging, uploading, disabled, 'has-file': !!modelValue }"
      role="button"
      :tabindex="disabled ? -1 : 0"
      :aria-label="`${title}，点击或拖拽上传`"
      :aria-disabled="disabled || uploading"
      :aria-describedby="hintId"
      @click="pick"
      @keydown.enter.prevent="pick"
      @keydown.space.prevent="pick"
      @dragenter.prevent="onDragEnter"
      @dragover.prevent="onDragEnter"
      @dragleave.prevent="onDragLeave"
      @drop.prevent="onDrop"
    >
      <input
        ref="inputRef"
        class="file-input"
        type="file"
        :accept="acceptAttr"
        :disabled="disabled || uploading"
        :aria-label="title"
        @change="onFileChange"
      />

      <div v-if="kind === 'cover' && modelValue && !coverBroken" class="cover-preview">
        <img :src="modelValue" :alt="`${title}预览`" @error="coverBroken = true" />
      </div>
      <div v-else class="file-icon" aria-hidden="true">
        <el-icon><component :is="icon" /></el-icon>
      </div>

      <div class="drop-copy">
        <strong>{{ uploading ? `正在上传 ${progress}%` : modelValue ? replaceTitle : title }}</strong>
        <span :id="hintId">{{ hint }}</span>
        <span v-if="uploading && uploadStat" class="file-name">{{ uploadStat }}</span>
        <span v-if="fileName" class="file-name">{{ fileName }}</span>
        <span v-if="error" class="upload-error" role="alert">{{ error }}</span>
      </div>

      <span v-if="uploading" class="pick-label cancel" role="button" @click.stop="cancelUpload">取消</span>
      <span v-else class="pick-label">{{ modelValue ? '替换' : (pickLabel || '选择文件') }}</span>

      <div
        v-if="uploading"
        class="upload-progress"
        role="progressbar"
        aria-label="上传进度"
        aria-valuemin="0"
        aria-valuemax="100"
        :aria-valuenow="progress"
      >
        <span :style="{ width: `${progress}%` }"></span>
      </div>
    </div>

    <div v-if="modelValue && !uploading" class="uploaded-line">
      <span class="ready-dot" aria-hidden="true"></span>
      <span>{{ readyText || (kind === 'cover' ? '封面已就绪' : '文件已上传') }}</span>
      <a :href="modelValue" target="_blank" rel="noopener">预览</a>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Document, Headset, Picture, VideoCamera } from '@element-plus/icons-vue'
import { fileApi } from '@/api'

const props = defineProps({
  modelValue: { type: String, default: '' },
  kind: { type: String, default: 'audio' },
  bizType: { type: String, default: 'audio' },
  title: { type: String, required: true },
  hint: { type: String, default: '' },
  pickLabel: { type: String, default: '' },
  maxSizeMb: { type: Number, default: 100 },
  disabled: { type: Boolean, default: false },
  doneTitle: { type: String, default: '' },
  readyText: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue', 'uploaded'])

const inputRef = ref(null)
const dragging = ref(false)
const uploading = ref(false)
const progress = ref(0)
const speed = ref(0)
const eta = ref(0)
const error = ref('')
const fileName = ref('')
const coverBroken = ref(false)
const hintId = `media-hint-${Math.random().toString(36).slice(2, 9)}`

let controller = null
let stallTimer = null
let stalled = false

const uploadStat = computed(() => {
  if (!uploading.value) return ''
  const parts = []
  if (speed.value > 0) parts.push(`${formatSpeed(speed.value)}/s`)
  if (eta.value > 0 && eta.value < 86400) parts.push(`剩余 ${formatEta(eta.value)}`)
  if (!parts.length) return progress.value > 0 ? '' : '正在连接上传通道…'
  return parts.join(' · ')
})

const acceptMap = {
  audio: '.mp3,.wav,.flac,.m4a,.aac,audio/*',
  video: '.mp4,.mov,.flv,.mkv,video/*',
  cover: '.jpg,.jpeg,.png,.webp,.gif,.bmp,image/*',
  lyric: '.lrc,.txt,text/plain'
}
const acceptAttr = computed(() => acceptMap[props.kind] || '*/*')
const icon = computed(() => ({ audio: Headset, video: VideoCamera, cover: Picture, lyric: Document }[props.kind] || Document))
const replaceTitle = computed(() => props.doneTitle || ({ audio: '音频已就绪', video: '视频已就绪', cover: '封面已就绪', lyric: '歌词文件已就绪' }[props.kind]))

watch(
  () => props.modelValue,
  (value) => {
    coverBroken.value = false
    if (value && !fileName.value) {
      try {
        fileName.value = decodeURIComponent(value.split('/').pop()?.split('?')[0] || '')
      } catch {
        fileName.value = ''
      }
    }
    if (!value) fileName.value = ''
  }
)

function pick() {
  if (props.disabled || uploading.value) return
  inputRef.value?.click()
}

function onDragEnter() {
  if (props.disabled || uploading.value) return
  dragging.value = true
}

function onDragLeave() {
  dragging.value = false
}

function onDrop(event) {
  dragging.value = false
  if (props.disabled || uploading.value) return
  const file = event.dataTransfer?.files?.[0]
  if (file) upload(file)
}

function onFileChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (file) upload(file)
}

function validate(file) {
  const allowed = {
    audio: ['mp3', 'wav', 'flac', 'm4a', 'aac'],
    video: ['mp4', 'mov', 'flv', 'mkv'],
    cover: ['jpg', 'jpeg', 'png', 'webp', 'gif', 'bmp'],
    lyric: ['lrc', 'txt']
  }[props.kind] || []
  const ext = file.name.split('.').pop()?.toLowerCase() || ''
  if (!allowed.includes(ext)) {
    error.value = `格式不支持，仅接受 ${allowed.map((v) => v.toUpperCase()).join(' / ')}`
    return false
  }
  if (file.size > props.maxSizeMb * 1024 * 1024) {
    error.value = `文件超过 ${props.maxSizeMb}MB 限制，当前为 ${formatBytes(file.size)}`
    return false
  }
  return true
}

function formatBytes(bytes) {
  if (bytes >= 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)}MB`
  return `${Math.ceil(bytes / 1024)}KB`
}

function formatSpeed(bps) {
  if (bps >= 1024 * 1024) return `${(bps / 1024 / 1024).toFixed(1)}MB`
  return `${Math.max(1, Math.round(bps / 1024))}KB`
}

function formatEta(seconds) {
  const s = Math.max(1, Math.round(seconds))
  if (s < 60) return `${s} 秒`
  if (s < 3600) return `${Math.floor(s / 60)} 分 ${s % 60} 秒`
  return `${Math.floor(s / 3600)} 小时 ${Math.round((s % 3600) / 60)} 分`
}

function armStallTimer() {
  window.clearTimeout(stallTimer)
  stallTimer = window.setTimeout(() => {
    stalled = true
    controller?.abort()
  }, 45000)
}

function clearStallTimer() {
  window.clearTimeout(stallTimer)
  stallTimer = null
}

function cancelUpload() {
  stalled = false
  controller?.abort()
}

async function upload(file) {
  error.value = ''
  if (!validate(file)) return
  uploading.value = true
  progress.value = 0
  speed.value = 0
  eta.value = 0
  fileName.value = file.name
  stalled = false
  controller = new AbortController()
  armStallTimer()

  let lastLoaded = 0
  let lastAt = Date.now()
  try {
    const res = await fileApi.upload(file, props.bizType, {
      signal: controller.signal,
      onUploadProgress: (event) => {
        if (!event.total) return
        const now = Date.now()
        const dt = (now - lastAt) / 1000
        if (dt >= 0.5) {
          const instant = (event.loaded - lastLoaded) / dt
          speed.value = speed.value ? speed.value * 0.65 + instant * 0.35 : instant
          lastLoaded = event.loaded
          lastAt = now
          const remain = event.total - event.loaded
          eta.value = speed.value > 0 ? remain / speed.value : 0
        }
        progress.value = Math.max(1, Math.min(99, Math.round((event.loaded / event.total) * 100)))
        if (event.loaded < event.total) armStallTimer()
        else clearStallTimer()
      }
    })
    const url = res.data?.url
    if (!url) throw new Error('上传结果缺少文件地址')
    progress.value = 100
    coverBroken.value = false
    emit('update:modelValue', url)
    emit('uploaded', { file, data: res.data })
  } catch (e) {
    const code = e?.code || ''
    if (code === 'ERR_CANCELED') {
      error.value = stalled ? '网络中断，上传已停止（45 秒无进度），请重试' : '已取消上传'
    } else if (code === 'ECONNABORTED' || /timeout/i.test(e?.message || '')) {
      error.value = '上传超时，请检查网络后重试'
    } else if (e?.response?.data?.message) {
      error.value = e.response.data.message
    } else {
      error.value = e?.message || '上传失败，请检查网络后重试'
    }
  } finally {
    clearStallTimer()
    uploading.value = false
    controller = null
  }
}
</script>

<style scoped>
.media-drop { width: 100%; }
.dropzone {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 82px;
  padding: 14px;
  overflow: hidden;
  border: 1px dashed #d9cbe8;
  border-radius: 12px;
  background: #fbf9fd;
  color: var(--km-text);
  cursor: pointer;
  transition: border-color .16s, background-color .16s;
}
.dropzone:hover,
.dropzone:focus-visible,
.dropzone.dragging {
  border-color: #b766e8;
  background: #f7f0ff;
  outline: none;
}
.dropzone.disabled { opacity: .55; cursor: not-allowed; }
.dropzone.has-file { border-style: solid; border-color: #e1d5ee; }
.file-input { display: none; }
.cover-preview {
  width: 56px;
  height: 56px;
  flex: none;
  overflow: hidden;
  border-radius: 8px;
  background: #f4eef8;
}
.cover-preview img { width: 100%; height: 100%; object-fit: cover; display: block; }
.file-icon {
  width: 44px;
  height: 44px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: #f4eafd;
  color: var(--km-brand-violet);
  font-size: 20px;
}
.drop-copy { min-width: 0; flex: 1; display: flex; flex-direction: column; gap: 3px; }
.drop-copy strong { font-size: 13px; font-weight: 600; line-height: 1.4; }
.drop-copy span { font-size: 11.5px; line-height: 1.5; color: var(--km-text-faint); }
.drop-copy .file-name {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--km-text-dim);
}
.drop-copy .upload-error { color: #c4242a; }
.pick-label {
  flex: none;
  min-width: 62px;
  height: 28px;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #dfd3eb;
  border-radius: 7px;
  color: var(--km-brand-violet-dark);
  background: #fff;
  font-size: 12px;
  font-weight: 600;
}
.pick-label.cancel {
  color: #c4242a;
  border-color: #f0c9cb;
  background: #fff7f7;
}
.pick-label.cancel:hover { background: #ffecec; }
.upload-progress {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 3px;
  background: #f0eaf5;
}
.upload-progress span {
  display: block;
  height: 100%;
  background: var(--km-brand-violet);
  transition: width .18s ease;
}
.uploaded-line {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  font-size: 11.5px;
  color: var(--km-text-faint);
}
.ready-dot { width: 5px; height: 5px; border-radius: 50%; background: #16a34a; }
.uploaded-line a { color: var(--km-brand-violet); margin-left: auto; text-decoration: underline; text-underline-offset: 2px; }
</style>
