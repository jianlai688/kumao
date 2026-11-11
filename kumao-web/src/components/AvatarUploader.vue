<template>
  <div class="avatar-uploader">
    <button
      type="button"
      class="avatar-preview"
      :class="{ empty: !modelValue || broken }"
      aria-label="从文件夹选择头像"
      @click="pick"
      @dragover.prevent
      @drop.prevent="onDrop"
    >
      <img v-if="modelValue && !broken" :src="modelValue" alt="头像预览" @error="broken = true" />
      <span v-else class="avatar-placeholder">{{ (fallbackText || '头像').slice(0, 1) }}</span>
      <span class="avatar-camera"><el-icon><Camera /></el-icon></span>
    </button>

    <div class="avatar-content">
      <div class="avatar-url-row">
        <el-input
          :model-value="modelValue"
          size="large"
          placeholder="头像图片地址（https://...）"
          clearable
          @update:model-value="onUrl"
        >
          <template #prefix><el-icon><Avatar /></el-icon></template>
        </el-input>
        <button type="button" class="pick-btn" :disabled="uploading" @click="pick">
          <el-icon><UploadFilled /></el-icon>
          {{ uploading ? '上传中…' : '从文件夹选择' }}
        </button>
      </div>
      <div class="avatar-hint">支持 JPG / PNG / WebP / GIF，单张不超过 {{ maxSizeMb }}MB</div>
    </div>

    <input
      ref="inputRef"
      class="avatar-file-input"
      type="file"
      accept="image/jpeg,image/png,image/webp,image/gif"
      @change="onFile"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Avatar, Camera, UploadFilled } from '@element-plus/icons-vue'
import { fileApi } from '@/api'

const props = defineProps({
  modelValue: { type: String, default: '' },
  bizType: { type: String, default: 'avatar' },
  maxSizeMb: { type: Number, default: 10 },
  fallbackText: { type: String, default: '头像' }
})
const emit = defineEmits(['update:modelValue'])

const inputRef = ref(null)
const uploading = ref(false)
const broken = ref(false)

function pick() {
  if (uploading.value) return
  inputRef.value?.click()
}

function onUrl(value) {
  broken.value = false
  emit('update:modelValue', value || '')
}

function onFile(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (file) upload(file)
}

function onDrop(event) {
  const file = event.dataTransfer?.files?.[0]
  if (file) upload(file)
}

async function upload(file) {
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > props.maxSizeMb * 1024 * 1024) {
    ElMessage.warning(`图片不能超过 ${props.maxSizeMb}MB`)
    return
  }
  uploading.value = true
  try {
    const res = await fileApi.upload(file, props.bizType)
    const url = res.data?.url
    if (!url) throw new Error('上传结果缺少图片地址')
    broken.value = false
    emit('update:modelValue', url)
    ElMessage.success('头像上传成功')
  } catch (e) {
    // 全局 axios 拦截器已展示后端错误；这里只兜底空响应
    if (!e?.response) ElMessage.error('头像上传失败，请重试')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.avatar-uploader {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}

.avatar-preview {
  position: relative;
  width: 72px;
  height: 72px;
  flex: none;
  padding: 0;
  border-radius: 50%;
  border: 1px solid rgba(192, 132, 252, .3);
  background: radial-gradient(circle at 35% 25%, rgba(192, 132, 252, .22), rgba(19, 14, 30, .9));
  overflow: hidden;
  cursor: pointer;
  transition: border-color .2s, transform .2s;
}
.avatar-preview:hover { border-color: rgba(240, 171, 252, .72); transform: translateY(-1px); }
.avatar-preview img { width: 100%; height: 100%; object-fit: cover; display: block; }
.avatar-placeholder {
  width: 100%; height: 100%;
  display: grid; place-items: center;
  color: #d8b4fe; font-size: 23px; font-weight: 800;
}
.avatar-camera {
  position: absolute;
  right: 2px; bottom: 2px;
  width: 23px; height: 23px;
  display: grid; place-items: center;
  border-radius: 50%;
  background: var(--km-grad);
  border: 2px solid #130e1e;
  color: #fff;
  font-size: 11px;
  opacity: 0;
  transition: opacity .2s;
}
.avatar-preview:hover .avatar-camera, .avatar-preview:focus-visible .avatar-camera { opacity: 1; }

.avatar-content { flex: 1; min-width: 0; }
.avatar-url-row { display: flex; align-items: center; gap: 8px; }
.avatar-url-row :deep(.el-input) { flex: 1; min-width: 0; }
.avatar-url-row :deep(.el-input__wrapper) { border-radius: 11px !important; }

.pick-btn {
  height: 40px;
  padding: 0 14px;
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  border-radius: 11px;
  border: 1px solid rgba(192, 132, 252, .38);
  background: rgba(192, 132, 252, .12);
  color: #e9d5ff;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition: background .18s, border-color .18s, color .18s;
}
.pick-btn:hover:not(:disabled) {
  background: var(--km-grad);
  border-color: transparent;
  color: #fff;
}
.pick-btn:disabled { opacity: .6; cursor: wait; }

.avatar-hint { margin-top: 7px; font-size: 11px; color: var(--km-text-faint); }
.avatar-file-input { display: none; }

@media (max-width: 640px) {
  .avatar-uploader { align-items: flex-start; gap: 12px; }
  .avatar-preview { width: 58px; height: 58px; }
  .avatar-url-row { flex-wrap: wrap; }
  .avatar-url-row :deep(.el-input) { flex-basis: 100%; }
  .pick-btn { width: 100%; }
}
</style>
