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
        <span v-if="fileName" class="file-name">{{ fileName }}</span>
        <span v-if="error" class="upload-error" role="alert">{{ error }}</span>
      </div>

      <span class="pick-label">{{ uploading ? `${progress}%` : modelValue ? '替换' : (pickLabel || '选择文件') }}</span>

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
      <span>{{ kind === 'cover' ? '封面已就绪' : '文件已上传' }}</span>
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
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'uploaded'])

const inputRef = ref(null)
const dragging = ref(false)
const uploading = ref(false)
const progress = ref(0)
const error = ref('')
const fileName = ref('')
const coverBroken = ref(false)
const hintId = `media-hint-${Math.random().toString(36).slice(2, 9)}`

const acceptMap = {
  audio: '.mp3,.wav,.flac,.m4a,.aac,audio/*',
  video: '.mp4,.mov,.flv,.mkv,video/*',
  cover: '.jpg,.jpeg,.png,.webp,.gif,.bmp,image/*',
  lyric: '.lrc,.txt,text/plain'
}
const acceptAttr = computed(() => acceptMap[props.kind] || '*/*')
const icon = computed(() => ({ audio: Headset, video: VideoCamera, cover: Picture, lyric: Document }[props.kind] || Document))
const replaceTitle = computed(() => ({ audio: '音频已就绪', video: '视频已就绪', cover: '封面已就绪', lyric: '歌词文件已就绪' }[props.kind]))

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

async function upload(file) {
  error.value = ''
  if (!validate(file)) return
  uploading.value = true
  progress.value = 0
  fileName.value = file.name
  try {
    const res = await fileApi.upload(file, props.bizType, {
      onUploadProgress: (event) => {
        if (!event.total) return
        progress.value = Math.max(1, Math.min(99, Math.round((event.loaded / event.total) * 100)))
      }
    })
    const url = res.data?.url
    if (!url) throw new Error('上传结果缺少文件地址')
    progress.value = 100
    coverBroken.value = false
    emit('update:modelValue', url)
    emit('uploaded', { file, data: res.data })
  } catch (e) {
    error.value = e?.message && !e?.response ? e.message : '上传失败，请检查网络后重试'
  } finally {
    uploading.value = false
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
  border: 1px dashed #403947;
  border-radius: 12px;
  background: #0c0b10;
  color: var(--km-text);
  cursor: pointer;
  transition: border-color .16s, background-color .16s;
}
.dropzone:hover,
.dropzone:focus-visible,
.dropzone.dragging {
  border-color: rgba(232, 121, 249, .62);
  background: #121018;
  outline: none;
}
.dropzone.disabled { opacity: .55; cursor: not-allowed; }
.dropzone.has-file { border-style: solid; border-color: #322d3a; }
.file-input { display: none; }
.cover-preview {
  width: 56px;
  height: 56px;
  flex: none;
  overflow: hidden;
  border-radius: 8px;
  background: #1b1722;
}
.cover-preview img { width: 100%; height: 100%; object-fit: cover; display: block; }
.file-icon {
  width: 44px;
  height: 44px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: #1b1722;
  color: #e9b8ff;
  font-size: 20px;
}
.drop-copy { min-width: 0; flex: 1; display: flex; flex-direction: column; gap: 3px; }
.drop-copy strong { font-size: 13px; font-weight: 600; line-height: 1.4; }
.drop-copy span { font-size: 11.5px; line-height: 1.5; color: #98909f; }
.drop-copy .file-name {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #c9c2d0;
}
.drop-copy .upload-error { color: #ff8fa2; }
.pick-label {
  flex: none;
  min-width: 62px;
  height: 28px;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #433b4b;
  border-radius: 7px;
  color: #e8e1ee;
  font-size: 12px;
  font-weight: 600;
}
.upload-progress {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 3px;
  background: #221d29;
}
.upload-progress span {
  display: block;
  height: 100%;
  background: #e879f9;
  transition: width .18s ease;
}
.uploaded-line {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  font-size: 11.5px;
  color: #98909f;
}
.ready-dot { width: 5px; height: 5px; border-radius: 50%; background: #4ade80; }
.uploaded-line a { color: #e9b8ff; margin-left: auto; text-decoration: underline; text-underline-offset: 2px; }
</style>
