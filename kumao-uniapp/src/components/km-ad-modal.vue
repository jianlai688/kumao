<template>
  <view v-if="visible" class="ad-mask">
    <view class="ad-card">
      <view class="ad-head">
        <text class="ad-tag">广告</text>
        <text class="ad-name">{{ ad.name || '精选推荐' }}</text>
        <view class="ad-close" @click="close">✕</view>
      </view>

      <view class="ad-body">
        <video
          v-if="ad.type === 'VIDEO' && ad.fileUrl"
          id="adVideo"
          class="ad-video"
          :src="resolvedFile"
          :poster="resolvedCover"
          :controls="false"
          :autoplay="true"
          object-fit="contain"
          @timeupdate="onTimeUpdate"
          @ended="onEnded"
          @error="onMediaError"
        ></video>
        <image v-else-if="ad.coverUrl" class="ad-image" :src="resolvedCover" mode="aspectFill" />
        <view v-else class="ad-fallback">
          <text class="ad-fallback-t">{{ ad.content || ad.name }}</text>
        </view>

        <view class="ad-overlay">
          <text class="ad-content">{{ ad.content }}</text>
          <view class="ad-clock">{{ remain > 0 ? `还剩 ${remain}s` : '可领取奖励' }}</view>
        </view>
      </view>

      <view class="ad-foot">
        <view class="ad-progress"><view class="ad-bar" :style="{ width: progress + '%' }"></view></view>
        <view :class="['ad-btn', { ready: remain === 0 && !claimed }]" @click="claim">
          {{ claimed ? '今日已领取' : remain > 0 ? `观看 ${remain}s 后可领会员` : '领取 1 天会员' }}
        </view>
        <text class="ad-tip">观看广告可免费获得 1 天酷猫会员，每天限领一次</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { api } from '@/api'
import { fileUrl } from '@/utils/media'

const props = defineProps({
  visible: { type: Boolean, default: false },
  ad: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:visible', 'claimed', 'close'])

const REQUIRED_SECONDS = 15

const remain = ref(REQUIRED_SECONDS)
const progress = ref(0)
const claimed = ref(false)
const watched = ref(0)
let timer = null

const resolvedFile = computed(() => (props.ad && props.ad.fileUrl ? fileUrl(props.ad.fileUrl) : ''))
const resolvedCover = computed(() => (props.ad && props.ad.coverUrl ? fileUrl(props.ad.coverUrl) : ''))

function reset() {
  remain.value = REQUIRED_SECONDS
  progress.value = 0
  watched.value = 0
  claimed.value = false
  if (timer) clearInterval(timer)
  timer = null
}

function startTimer() {
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    watched.value += 1
    remain.value = Math.max(0, REQUIRED_SECONDS - watched.value)
    progress.value = Math.min(100, Math.round((watched.value / REQUIRED_SECONDS) * 100))
    if (remain.value === 0) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

watch(
  () => props.visible,
  (v) => {
    if (v) {
      reset()
      startTimer()
      if (props.ad && props.ad.planId) api.adReport(props.ad.planId, 'exposure').catch(() => {})
    } else if (timer) {
      clearInterval(timer)
      timer = null
    }
  }
)

function onTimeUpdate(e) {
  const t = (e && e.detail && e.detail.currentTime) || 0
  // 视频真实播放时长优先，避免用户暂停也在计时
  if (t > watched.value) {
    watched.value = Math.floor(t)
    remain.value = Math.max(0, REQUIRED_SECONDS - watched.value)
    progress.value = Math.min(100, Math.round((watched.value / REQUIRED_SECONDS) * 100))
  }
}

function onEnded() {
  if (remain.value > 0) {
    watched.value = REQUIRED_SECONDS
    remain.value = 0
    progress.value = 100
  }
}

function onMediaError() {
  // 素材加载失败时用计时兜底，保证仍可完成任务
  if (!timer) startTimer()
}

async function claim() {
  if (claimed.value) {
    uni.showToast({ title: '今日已领取，明天再来', icon: 'none' })
    return
  }
  if (remain.value > 0) {
    uni.showToast({ title: `还需观看 ${remain.value} 秒`, icon: 'none' })
    return
  }
  if (!props.ad || !props.ad.planId) {
    uni.showToast({ title: '广告信息失效，请重新进入', icon: 'none' })
    return
  }
  try {
    const res = await api.claimAdReward({ planId: props.ad.planId, watchedSeconds: watched.value })
    claimed.value = true
    const data = (res && res.data) || {}
    uni.showModal({
      title: '领取成功',
      content: `已获得 ${data.days || 1} 天酷猫会员${data.expireTime ? '，有效期至 ' + String(data.expireTime).slice(0, 10) : ''}`,
      showCancel: false
    })
    emit('claimed', data)
  } catch (e) {
    if (e && e.message) uni.showToast({ title: e.message, icon: 'none' })
  }
}

function close() {
  emit('update:visible', false)
  emit('close')
}
</script>

<style scoped>
.ad-mask {
  position: fixed; inset: 0; z-index: 900; background: rgba(0,0,0,.82);
  display: flex; align-items: center; justify-content: center; padding: 40rpx;
}
.ad-card { width: 100%; max-width: 640rpx; border-radius: 24rpx; overflow: hidden; background: #14131a; border: 1rpx solid rgba(255,255,255,.08); }
.ad-head { display: flex; align-items: center; gap: 12rpx; padding: 18rpx 22rpx; }
.ad-tag { font-size: 17rpx; padding: 3rpx 10rpx; border-radius: 5rpx; background: rgba(255,255,255,.12); color: rgba(255,255,255,.7); }
.ad-name { flex: 1; font-size: 23rpx; color: rgba(247,241,232,.8); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ad-close { width: 48rpx; height: 48rpx; display: flex; align-items: center; justify-content: center; color: rgba(255,255,255,.6); font-size: 24rpx; }
.ad-body { position: relative; width: 100%; height: 380rpx; background: #000; }
.ad-video, .ad-image { width: 100%; height: 100%; }
.ad-fallback { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg,#2a2033,#101018); padding: 30rpx; box-sizing: border-box; }
.ad-fallback-t { font-size: 30rpx; color: #f7f1e8; font-weight: 800; text-align: center; }
.ad-overlay { position: absolute; left: 0; right: 0; bottom: 0; padding: 16rpx 20rpx; display: flex; align-items: flex-end; gap: 10rpx; background: linear-gradient(0deg, rgba(0,0,0,.72), transparent); }
.ad-content { flex: 1; font-size: 21rpx; color: rgba(255,255,255,.86); }
.ad-clock { font-size: 19rpx; color: var(--km-accent); font-weight: 700; }
.ad-foot { padding: 18rpx 22rpx 26rpx; }
.ad-progress { height: 6rpx; border-radius: 999rpx; background: rgba(255,255,255,.12); overflow: hidden; }
.ad-bar { height: 100%; background: linear-gradient(90deg,#fbd08a,#de823c); transition: width .3s ease; }
.ad-btn {
  margin-top: 16rpx; height: 82rpx; line-height: 82rpx; text-align: center; border-radius: 14rpx;
  background: rgba(255,255,255,.1); color: rgba(247,241,232,.6); font-size: 25rpx; font-weight: 800;
}
.ad-btn.ready { background: linear-gradient(135deg,#fbd08a,#de823c); color: #22150a; }
.ad-tip { display: block; margin-top: 12rpx; font-size: 18rpx; color: rgba(247,241,232,.3); text-align: center; }
</style>

