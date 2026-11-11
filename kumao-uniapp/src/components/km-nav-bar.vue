<template>
  <view class="nav" :style="{ paddingTop: statusBar + 'px' }">
    <view class="inner">
      <view v-if="back" class="btn" @click="goBack">
        <km-icon name="chevron-left" :size="16" color="#F7F1E8" />
      </view>
      <text class="title">{{ title }}</text>
      <view class="right"><slot></slot></view>
    </view>
  </view>
</template>

<script setup>
import KmIcon from '@/components/km-icon.vue'

defineProps({
  title: { type: String, default: '' },
  back: { type: Boolean, default: true }
})

const statusBar = uni.getSystemInfoSync().statusBarHeight || 0

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) uni.navigateBack()
  else uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style scoped>
.nav { width: 100%; }
.inner {
  height: 88rpx;
  padding: 0 36rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.btn {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, .055);
  border: 1rpx solid rgba(255, 255, 255, .08);
  display: flex;
  align-items: center;
  justify-content: center;
}
.title { font-size: 34rpx; font-weight: 800; letter-spacing: .5rpx; color: #f7f1e8; }
.right { margin-left: auto; display: flex; align-items: center; gap: 14rpx; }
</style>
