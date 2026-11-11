<template>
  <view class="lcard" @click="$emit('select', room)">
    <view class="ph">
      <image class="img" :src="coverOf(room.coverUrl)" mode="aspectFill" />
      <view class="mask"></view>
      <view class="badge"><view class="pip"></view>LIVE</view>
      <view class="views">
        <km-icon name="user" :size="10" color="rgba(255,255,255,.8)" />
        <text>{{ formatCount(room.realtimeOnline) }}</text>
      </view>
    </view>
    <text class="t">{{ room.title }}</text>
    <view class="a">
      <image class="ava" :src="avatarOf(room.anchorAvatar || room.anchorAvatarUrl)" mode="aspectFill" />
      <text class="an">{{ room.anchorName || '主播' }}</text>
      <text v-if="room.category" class="cat">{{ room.category }}</text>
    </view>
  </view>
</template>

<script setup>
import KmIcon from '@/components/km-icon.vue'
import { coverOf, avatarOf, formatCount } from '@/utils/media'

defineProps({ room: { type: Object, required: true } })
defineEmits(['select'])
</script>

<style scoped>
.lcard { width: 100%; }
.ph {
  position: relative;
  width: 100%;
  height: 198rpx;
  border-radius: 26rpx;
  overflow: hidden;
  background: #1a1c21;
  border: 1rpx solid rgba(255, 255, 255, .07);
}
.img { width: 100%; height: 100%; }
.mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(8, 7, 10, .32), transparent 44%, rgba(8, 7, 10, .62));
}
.badge {
  position: absolute;
  left: 12rpx;
  top: 12rpx;
  height: 30rpx;
  padding: 0 8rpx;
  border-radius: 6rpx;
  background: linear-gradient(96deg, #ff8a6b, #e94c55);
  display: flex;
  align-items: center;
  gap: 5rpx;
  font-size: 14rpx;
  font-weight: 800;
  letter-spacing: 1rpx;
  color: #fff;
}
.pip { width: 6rpx; height: 6rpx; border-radius: 50%; background: #fff; }
.views {
  position: absolute;
  right: 12rpx;
  bottom: 10rpx;
  display: flex;
  align-items: center;
  gap: 4rpx;
  font-size: 16rpx;
  color: rgba(255, 255, 255, .82);
}
.t {
  display: block;
  margin-top: 14rpx;
  font-size: 24rpx;
  font-weight: 600;
  color: #f7f1e8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.a { display: flex; align-items: center; gap: 8rpx; margin-top: 8rpx; }
.ava { width: 26rpx; height: 26rpx; border-radius: 50%; background: #2a2320; }
.an { font-size: 18rpx; color: rgba(247, 241, 232, .36); }
.cat {
  margin-left: auto;
  font-size: 15rpx;
  color: rgba(245, 180, 91, .8);
  border: 1rpx solid rgba(245, 180, 91, .25);
  border-radius: 6rpx;
  padding: 1rpx 6rpx;
}
</style>



