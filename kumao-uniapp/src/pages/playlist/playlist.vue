<template>
  <view class="page">
    <view class="head">
      <text class="page-title">歌单广场</text>
      <text class="page-sub">发现与你同频的精选歌单</text>
    </view>

    <view class="grid">
      <view v-for="p in list" :key="p.id" class="pl" @click="open(p)">
        <view class="cover-box">
          <image :src="p.coverUrl" mode="aspectFill" class="cover" />
          <view class="mask"></view>
          <view class="count">▶ {{ p.songCount }} 首</view>
        </view>
        <text class="name">{{ p.name }}</text>
        <text class="creator">{{ p.creatorName || '酷猫音乐' }}</text>
      </view>
    </view>

    <view v-if="!list.length" class="empty">
      <text class="e-ico">📁</text>
      <text>暂无歌单</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '@/api'

const list = ref([])

function open(p) {
  uni.navigateTo({ url: `/pages/playlist/detail?id=${p.id}` })
}

onMounted(async () => {
  const res = await api.playlists({ pageNum: 1, pageSize: 30 })
  list.value = res.data?.records || []
})
</script>

<style scoped>
.page { padding-bottom: 50rpx; }
.head { padding: 28rpx 28rpx 14rpx; display: flex; flex-direction: column; gap: 8rpx; }
.page-title { font-size: 44rpx; font-weight: 900; letter-spacing: -1.4rpx; }
.page-sub { font-size: 22rpx; color: rgba(243, 244, 246, .4); }

.grid { display: flex; flex-wrap: wrap; padding: 8rpx 24rpx 0; }
.pl { width: 48%; margin-right: 4%; margin-bottom: 26rpx; }
.pl:nth-child(2n) { margin-right: 0; }
.cover-box { position: relative; width: 100%; height: 300rpx; border-radius: 24rpx; overflow: hidden; }
.cover { width: 100%; height: 300rpx; background: #1a1c21; }
.mask { position: absolute; inset: 0; background: linear-gradient(180deg, transparent 55%, rgba(0, 0, 0, .6)); }
.count {
  position: absolute;
  right: 12rpx;
  top: 12rpx;
  font-size: 19rpx;
  color: rgba(255, 255, 255, .92);
  background: rgba(5, 5, 6, .55);
  padding: 5rpx 14rpx;
  border-radius: 999rpx;
}
.name {
  display: block;
  font-size: 26rpx;
  font-weight: 600;
  margin-top: 12rpx;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.creator { display: block; font-size: 21rpx; color: rgba(243, 244, 246, .35); margin-top: 4rpx; }

.empty { display: flex; flex-direction: column; align-items: center; gap: 16rpx; padding: 120rpx 0; color: rgba(243, 244, 246, .3); font-size: 24rpx; }
.e-ico { font-size: 60rpx; opacity: .5; }
</style>
