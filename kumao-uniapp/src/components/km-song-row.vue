<template>
  <view class="srow" @click="onTap">
    <text v-if="showRank" :class="['no', { top: index < 3 }]">{{ index + 1 }}</text>
    <image class="cv" :src="coverOf(song.coverUrl)" mode="aspectFill" />
    <view class="meta">
      <view class="l1">
        <text class="name">{{ song.name }}</text>
        <text v-if="song.vipOnly" class="vip">VIP</text>
      </view>
      <text class="sub">{{ song.singerName }}<text v-if="showPlays" class="dot"> · </text><text v-if="showPlays">{{ formatPlay(song.playCount) }} 次播放</text></text>
    </view>
    <view class="heart" @click.stop="onLike">
      <km-icon :name="liked ? 'heart-fill' : 'heart'" :size="18" :color="liked ? '#F5B45B' : 'rgba(247,241,232,.3)'" />
    </view>
  </view>
</template>

<script setup>
import KmIcon from '@/components/km-icon.vue'
import { coverOf, formatPlay } from '@/utils/media'

const props = defineProps({
  song: { type: Object, required: true },
  index: { type: Number, default: 0 },
  showRank: { type: Boolean, default: false },
  showPlays: { type: Boolean, default: true },
  liked: { type: Boolean, default: false }
})

const emit = defineEmits(['select', 'like'])

function onTap() {
  emit('select', props.song)
}

function onLike() {
  emit('like', props.song)
}
</script>

<style scoped>
.srow {
  display: flex;
  align-items: center;
  gap: 18rpx;
  height: 84rpx;
  padding: 0 8rpx;
}
.no {
  width: 28rpx;
  text-align: center;
  font-size: 24rpx;
  font-weight: 800;
  font-style: italic;
  color: rgba(247, 241, 232, .22);
}
.no.top { color: #f5b45b; }
.cv {
  width: 68rpx;
  height: 68rpx;
  border-radius: 20rpx;
  flex-shrink: 0;
  border: 1rpx solid rgba(255, 255, 255, .07);
  background: #1a1c21;
}
.meta { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4rpx; }
.l1 { display: flex; align-items: center; gap: 10rpx; }
.name {
  font-size: 26rpx;
  font-weight: 600;
  color: #f7f1e8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 330rpx;
}
.vip {
  font-size: 16rpx;
  padding: 2rpx 8rpx;
  border-radius: 6rpx;
  background: rgba(245, 180, 91, .16);
  color: #f5b45b;
  font-weight: 700;
}
.sub {
  font-size: 20rpx;
  color: rgba(247, 241, 232, .38);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dot { color: rgba(247, 241, 232, .2); }
.heart { width: 56rpx; height: 56rpx; display: flex; align-items: center; justify-content: center; }
</style>


