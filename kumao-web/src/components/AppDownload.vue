<template>
  <el-dialog
    :model-value="modelValue"
    width="372px"
    align-center
    append-to-body
    class="app-dl-dialog"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <template #header>
      <div class="adl-head">
        <span class="adl-logo">酷</span>
        <div class="adl-title">
          <b>手机 App 下载</b>
          <span>扫码安装 · 曲库与电脑端同步</span>
        </div>
      </div>
    </template>

    <div class="adl-body">
      <div class="adl-qr">
        <img v-if="qr" :src="qr" alt="酷猫音乐安卓客户端下载二维码" />
        <div v-else class="adl-qr-ph">
          <span class="adl-spin"></span>
          二维码生成中…
        </div>
      </div>

      <p class="adl-tip">手机扫码打开下载页，点「下载」即可安装</p>

      <div class="adl-meta">
        <span>v1.5.2</span>
        <i></i>
        <span>425.4 KB</span>
        <i></i>
        <span>Android 6.0+</span>
      </div>

      <div class="adl-actions">
        <a class="adl-btn solid" :href="apkUrl">电脑直接下载 APK</a>
        <button class="adl-btn plain" type="button" @click="copyLink">{{ copied ? '已复制下载链接' : '复制下载链接' }}</button>
      </div>

      <p class="adl-link">{{ downloadUrl }}</p>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({ modelValue: Boolean })
defineEmits(['update:modelValue'])

/** 本机调试（localhost）时二维码指向公网地址，手机才能扫得开 */
const PUBLIC_BASE = 'https://7c4869c1.r60.cpolar.top'
const APK_NAME = 'KuMao-Android-v1.5.2.apk'

const qr = ref('')
const copied = ref(false)

function baseUrl() {
  if (typeof window === 'undefined') return PUBLIC_BASE
  const host = window.location.hostname
  const isLocal = !host || host === 'localhost' || host === '::1' || host === '0.0.0.0' || /^127\./.test(host)
  return isLocal ? PUBLIC_BASE : window.location.origin
}

const downloadUrl = computed(() => `${baseUrl()}/download.html`)
const apkUrl = computed(() => `${baseUrl()}/downloads/${APK_NAME}`)

function loadLib() {
  if (window.qrcode) return Promise.resolve()
  if (!window.__kmQrLoading) {
    window.__kmQrLoading = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = '/vendor/qrcode-generator.js'
      script.async = true
      script.onload = resolve
      script.onerror = () => {
        window.__kmQrLoading = null
        reject(new Error('qrcode lib load failed'))
      }
      document.head.appendChild(script)
    })
  }
  return window.__kmQrLoading
}

async function renderQr() {
  if (qr.value) return
  try {
    await loadLib()
    const q = window.qrcode(0, 'M')
    q.addData(downloadUrl.value)
    q.make()
    qr.value = q.createDataURL(10, 2)
  } catch (e) {
    qr.value = ''
  }
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) renderQr()
  },
  { immediate: true }
)

async function copyLink() {
  const text = downloadUrl.value
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
    } else {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
    }
    copied.value = true
    setTimeout(() => { copied.value = false }, 1800)
  } catch (e) {
    ElMessage.warning('复制失败，请手动复制下方链接')
  }
}
</script>

<style scoped>
.adl-head { display: flex; align-items: center; gap: 12px; }
.adl-logo {
  width: 42px; height: 42px; border-radius: 12px; flex: none;
  display: grid; place-items: center;
  background: var(--km-grad, linear-gradient(135deg, #a855f7, #7c3aed));
  color: #fff; font-size: 20px; font-weight: 800;
  box-shadow: 0 8px 20px rgba(153, 45, 190, .22);
}
.adl-title { display: flex; flex-direction: column; gap: 2px; }
.adl-title b { font-size: 16px; font-weight: 700; color: var(--km-text, #1d1726); }
.adl-title span { font-size: 12px; color: var(--km-text-faint, #8b8496); }

.adl-body { display: flex; flex-direction: column; align-items: center; }
.adl-qr {
  width: 208px; height: 208px; padding: 10px;
  border-radius: 16px; border: 1px solid #ece4f6;
  background: #fff;
  box-shadow: 0 10px 26px rgba(76, 40, 120, .08);
  display: grid; place-items: center;
}
.adl-qr img { width: 100%; height: 100%; border-radius: 8px; display: block; }
.adl-qr-ph {
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  font-size: 12px; color: var(--km-text-faint, #8b8496);
}
.adl-spin {
  width: 26px; height: 26px; border-radius: 50%;
  border: 2px solid rgba(139, 63, 206, .15); border-top-color: #8b3fce;
  animation: adl-spin .9s linear infinite;
}
@keyframes adl-spin { to { transform: rotate(360deg); } }

.adl-tip { margin: 14px 0 0; font-size: 12.5px; color: #6f6779; text-align: center; }
.adl-meta {
  display: flex; align-items: center; gap: 8px; margin-top: 10px;
  font-size: 11.5px; color: #9a92a6;
}
.adl-meta i { width: 3px; height: 3px; border-radius: 50%; background: #d9d1e4; }

.adl-actions { display: flex; gap: 8px; width: 100%; margin-top: 18px; }
.adl-btn {
  flex: 1; height: 42px; border-radius: 11px; border: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 600; text-decoration: none; cursor: pointer;
  transition: all .18s;
}
.adl-btn.solid {
  background: var(--km-grad, linear-gradient(135deg, #a855f7, #7c3aed));
  color: #fff; box-shadow: 0 8px 20px rgba(153, 45, 190, .2);
}
.adl-btn.solid:hover { filter: brightness(1.05); }
.adl-btn.plain {
  background: #f7f2fd; color: #7a3fae; border: 1px solid #e6d8f6;
}
.adl-btn.plain:hover { background: #f1e8fb; }

.adl-link {
  margin: 12px 0 0; font-size: 11px; color: #b0a8bb;
  word-break: break-all; text-align: center;
}
</style>
