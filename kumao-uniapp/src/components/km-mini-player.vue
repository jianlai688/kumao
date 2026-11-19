<template>
  <view v-if="state.song" class="mini" @click="open">
    <image class="mcover" :src="coverOf(state.song.coverUrl)" mode="aspectFill" />
    <view class="mmeta">
      <text class="mname">{{ state.song.name }}</text>
      <text class="msinger">{{ state.song.singerName }}</text>
    </view>
    <view class="mbtn" @click.stop="toggle">
      <km-icon :name="state.playing ? 'pause' : 'play'" :size="13" color="#2B1A08" />
    </view>
    <view class="mlist" @click.stop="openList">
      <km-icon name="list" :size="18" color="rgba(247,241,232,.55)" />
    </view>
    <view class="prog">
      <view class="prog-i" :style="{ width: pct + '%' }"></view>
    </view>

    <km-queue-sheet v-model:visible="queueOpen" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import KmIcon from '@/components/km-icon.vue'
import KmQueueSheet from '@/components/km-queue-sheet.vue'
import { usePlayer } from '@/utils/player'
import { coverOf } from '@/utils/media'

const { state, toggle } = usePlayer()
const queueOpen = ref(false)

const pct = computed(() => {
  if (!state.duration) return 0
  return Math.min(100, Math.max(0, (state.current / state.duration) * 100))
})

function open() {
  const id = state.song.songId || state.song.id
  if (id) uni.navigateTo({ url: `/pages/song/song?id=${id}` })
}

function openList() {
  queueOpen.value = true
}
</script>

<style scoped>
.mini {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: calc(var(--window-bottom, 0px) + 16rpx);
  z-index: 60;
  height: 112rpx;
  border-radius: 36rpx;
  background: linear-gradient(180deg, rgba(38, 32, 34, .94), rgba(20, 17, 20, .96));
  border: 1rpx solid rgba(255, 255, 255, .1);
  box-shadow: 0 18rpx 40rpx rgba(0, 0, 0, .55), inset 0 1rpx 0 rgba(255, 255, 255, .07);
  backdrop-filter: blur(24rpx);
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 0 20rpx 0 16rpx;
}
.mcover {
  width: 80rpx;
  height: 80rpx;
  border-radius: 24rpx;
  flex-shrink: 0;
  border: 1rpx solid rgba(255, 255, 255, .1);
  background: #1a1c21;
}
.mmeta { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4rpx; }
.mname {
  font-size: 25rpx;
  font-weight: 600;
  color: #f7f1e8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.msinger { font-size: 20rpx; color: rgba(247, 241, 232, .42); }
.mbtn {
  width: 62rpx;
  height: 62rpx;
  border-radius: 50%;
  background: linear-gradient(160deg, #f9c878, #e58f46);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 20rpx rgba(224, 139, 62, .34);
  flex-shrink: 0;
}
.mlist { width: 44rpx; height: 44rpx; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.prog {
  position: absolute;
  left: 34rpx;
  right: 34rpx;
  bottom: 10rpx;
  height: 4rpx;
  border-radius: 4rpx;
  background: rgba(255, 255, 255, .1);
  overflow: hidden;
}
.prog-i {
  height: 100%;
  border-radius: 4rpx;
  background: linear-gradient(90deg, var(--km-accent), #ee7f93);
}
</style>



