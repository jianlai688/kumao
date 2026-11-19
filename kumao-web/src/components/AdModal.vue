<template>
  <transition name="ad-fade">
    <div v-if="visible" class="ad-modal" @click.self="close">
      <div class="ad-card">
        <header class="ad-card__head">
          <span class="ad-card__tag">广告</span>
          <span class="ad-card__name">{{ ad?.name || '精选推荐' }}</span>
          <button class="ad-card__close" aria-label="关闭" @click="close">✕</button>
        </header>

        <div class="ad-card__body">
          <video
            v-if="ad?.type === 'VIDEO' && mediaUrl"
            ref="videoRef"
            class="ad-card__video"
            :src="mediaUrl"
            :poster="coverUrl"
            autoplay
            playsinline
            @timeupdate="onTimeUpdate"
            @ended="onEnded"
          ></video>
          <img v-else-if="coverUrl" class="ad-card__image" :src="coverUrl" :alt="ad?.name" />
          <div v-else class="ad-card__fallback">{{ ad?.content || ad?.name }}</div>

          <div class="ad-card__overlay">
            <p>{{ ad?.content }}</p>
            <span class="ad-card__clock">{{ remain > 0 ? `还剩 ${remain}s` : '可领取奖励' }}</span>
          </div>
        </div>

        <footer class="ad-card__foot">
          <div class="ad-card__progress"><i :style="{ width: progress + '%' }"></i></div>
          <button class="ad-card__btn" :class="{ ready: remain === 0 }" @click="claim">
            {{ claimed ? '今日已领取' : remain > 0 ? `观看 ${remain}s 后可领会员` : '领取 1 天会员' }}
          </button>
          <p class="ad-card__tip">观看广告可免费获得 1 天酷猫会员，每天限领一次</p>
        </footer>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { adApi, vipApi } from '@/api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  ad: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'claimed'])

const REQUIRED = 15
const remain = ref(REQUIRED)
const progress = ref(0)
const claimed = ref(false)
const watched = ref(0)
const videoRef = ref(null)
let timer = null

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const mediaUrl = computed(() => props.ad?.fileUrl || '')
const coverUrl = computed(() => props.ad?.coverUrl || '')

function reset() {
  remain.value = REQUIRED
  progress.value = 0
  watched.value = 0
  claimed.value = false
  clearInterval(timer)
  timer = null
}

function startTimer() {
  clearInterval(timer)
  timer = setInterval(() => {
    watched.value += 1
    remain.value = Math.max(0, REQUIRED - watched.value)
    progress.value = Math.min(100, Math.round((watched.value / REQUIRED) * 100))
    if (remain.value === 0) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      clearInterval(timer)
      timer = null
      return
    }
    reset()
    startTimer()
    if (props.ad?.planId) adApi.report(props.ad.planId, 'exposure').catch(() => {})
  }
)

function onTimeUpdate(e) {
  const t = Math.floor(e.target?.currentTime || 0)
  if (t > watched.value) {
    watched.value = t
    remain.value = Math.max(0, REQUIRED - t)
    progress.value = Math.min(100, Math.round((t / REQUIRED) * 100))
  }
}

function onEnded() {
  watched.value = REQUIRED
  remain.value = 0
  progress.value = 100
}

async function claim() {
  if (claimed.value) return ElMessage.info('今日已领取，明天再来')
  if (remain.value > 0) return ElMessage.warning(`还需观看 ${remain.value} 秒`)
  if (!props.ad?.planId) return ElMessage.warning('广告信息失效，请刷新重试')
  try {
    const res = await vipApi.claimAdReward(props.ad.planId, watched.value)
    claimed.value = true
    ElMessage.success(`已获得 ${res.data?.days || 1} 天会员`)
    emit('claimed', res.data || {})
  } catch (e) {
    ElMessage.error(e?.message || '领取失败，请稍后重试')
  }
}

function close() {
  visible.value = false
}

onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.ad-modal {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  place-items: center;
  background: rgba(4, 3, 8, 0.82);
  backdrop-filter: blur(6px);
  padding: 24px;
}
.ad-card {
  width: min(560px, 100%);
  border-radius: 20px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #ece7f2;
  box-shadow: 0 30px 80px rgba(40, 22, 63, 0.2);
}
.ad-card__head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
}
.ad-card__tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f3eef8;
  color: #6f657b;
}
.ad-card__name {
  flex: 1;
  font-size: 13px;
  color: #2b2334;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ad-card__close {
  border: 0;
  background: transparent;
  color: #8b8195;
  cursor: pointer;
  font-size: 15px;
}
.ad-card__close:hover { color: #4f465a; }
.ad-card__body {
  position: relative;
  height: 300px;
  background: #000;
}
.ad-card__video,
.ad-card__image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}
.ad-card__fallback {
  height: 100%;
  display: grid;
  place-items: center;
  padding: 24px;
  color: #5f5769;
  font-size: 18px;
  text-align: center;
  background: linear-gradient(135deg, #faf6ff, #f3eef8);
}
.ad-card__overlay {
  position: absolute;
  inset: auto 0 0 0;
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 14px 18px;
  background: linear-gradient(0deg, rgba(0, 0, 0, 0.72), transparent);
}
.ad-card__overlay p {
  margin: 0;
  flex: 1;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.86);
}
.ad-card__clock {
  font-size: 12px;
  font-weight: 700;
  color: var(--km-accent);
}
.ad-card__foot {
  padding: 16px 18px 20px;
}
.ad-card__progress {
  height: 3px;
  border-radius: 999px;
  background: #eee8f4;
  overflow: hidden;
}
.ad-card__progress i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #fbd08a, #de823c);
  transition: width 0.3s ease;
}
.ad-card__btn {
  margin-top: 14px;
  width: 100%;
  height: 44px;
  border: 0;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  background: #f4f0f8;
  color: #9a90a6;
}
.ad-card__btn.ready {
  background: linear-gradient(135deg, #fbd08a, #de823c);
  color: #22150a;
}
.ad-card__tip {
  margin: 10px 0 0;
  text-align: center;
  font-size: 11px;
  color: #8a8096;
}
.ad-fade-enter-active,
.ad-fade-leave-active {
  transition: opacity 0.2s ease;
}
.ad-fade-enter-from,
.ad-fade-leave-to {
  opacity: 0;
}
</style>
