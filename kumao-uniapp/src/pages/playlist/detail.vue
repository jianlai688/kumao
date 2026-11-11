<template>
  <view class="page" v-if="playlist">
    <!-- 头图 -->
    <view class="hero">
      <view class="hero-bg" :style="{ backgroundImage: `url(${playlist.coverUrl})` }"></view>
      <view class="hero-mask"></view>
      <view class="hero-inner">
        <image :src="playlist.coverUrl" mode="aspectFill" class="cover" />
        <view class="info">
          <text class="badge">{{ playlist.isPublic ? '公开歌单' : '私有歌单' }}</text>
          <text class="name">{{ playlist.name }}</text>
          <text class="meta">{{ playlist.songCount }} 首 · {{ playlist.creatorName }}</text>
          <text class="desc">{{ playlist.description }}</text>
        </view>
      </view>
      <view class="hero-actions">
        <view class="play-all" @click="playAll">
          <text class="pa-ico">▶</text>
          <text>播放全部</text>
        </view>
      </view>
    </view>

    <!-- 歌曲 -->
    <view class="list-card">
      <view v-for="(s, i) in playlist.songs || []" :key="s.id" class="row" @click="play(s)">
        <text class="no">{{ i + 1 }}</text>
        <image :src="s.coverUrl" mode="aspectFill" class="km-cover row-cover" />
        <view class="meta-box">
          <text class="sname">{{ s.songName }}</text>
          <text class="ssub">{{ s.singerName }}</text>
        </view>
        <text class="play-icon">▶</text>
      </view>

      <view v-if="!(playlist.songs || []).length" class="empty">
        <text class="e-ico">🎵</text>
        <text>歌单还是空的</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '@/api'

const playlist = ref(null)

function play(s) {
  uni.setStorageSync('km_current_song', {
    songId: s.songId, name: s.songName, singerName: s.singerName,
    coverUrl: s.coverUrl, audioUrl: s.audioUrl, duration: s.duration
  })
  api.playRecord({ songId: s.songId, device: 'APP' }).catch(() => {})
  uni.navigateTo({ url: `/pages/song/song?id=${s.songId}` })
}

function playAll() {
  if (playlist.value?.songs?.length) play(playlist.value.songs[0])
}

onLoad(async (options) => {
  const res = await api.playlist(options.id)
  playlist.value = res.data
})
</script>

<style scoped>
.page { padding-bottom: 50rpx; }

/* 头图 */
.hero {
  position: relative;
  padding: 32rpx 28rpx 30rpx;
  overflow: hidden;
  background: #0a0b0d;
}
.hero-bg {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  filter: blur(60rpx) brightness(.4) saturate(1.6);
  transform: scale(1.25);
}
.hero-mask { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(5, 5, 6, .5), rgba(5, 5, 6, .95)); }
.hero-inner { position: relative; display: flex; gap: 24rpx; }
.cover {
  width: 200rpx;
  height: 200rpx;
  border-radius: 24rpx;
  border: 1rpx solid rgba(255, 255, 255, .12);
  flex-shrink: 0;
  background: #1a1c21;
}
.info { flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 10rpx; min-width: 0; }
.badge {
  align-self: flex-start;
  font-size: 19rpx;
  padding: 4rpx 14rpx;
  border-radius: 6rpx;
  background: rgba(245, 180, 91, .14);
  border: 1rpx solid rgba(245, 180, 91, .3);
  color: #f5b45b;
  font-weight: 700;
}
.name { font-size: 36rpx; font-weight: 900; letter-spacing: -1rpx; line-height: 1.25; }
.meta { font-size: 22rpx; color: rgba(243, 244, 246, .45); }
.desc {
  font-size: 21rpx;
  color: rgba(243, 244, 246, .32);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hero-actions { position: relative; margin-top: 24rpx; }
.play-all {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  height: 68rpx;
  padding: 0 34rpx;
  border-radius: 999rpx;
  background: #f5b45b;
  color: #07080a;
  font-size: 25rpx;
  font-weight: 800;
  box-shadow: 0 10rpx 30rpx rgba(245, 180, 91, .22);
}
.pa-ico { font-size: 20rpx; }

/* 列表 */
.list-card { margin: 16rpx 24rpx 0; padding: 10rpx 20rpx; border-radius: 26rpx; background: #131418; border: 1rpx solid rgba(255, 255, 255, .06); }
.row { display: flex; align-items: center; padding: 16rpx 0; }
.row + .row { border-top: 1rpx solid rgba(255, 255, 255, .05); }
.no { width: 46rpx; color: rgba(243, 244, 246, .3); font-size: 23rpx; text-align: center; }
.row-cover { width: 84rpx; height: 84rpx; border-radius: 14rpx; }
.meta-box { flex: 1; margin-left: 18rpx; display: flex; flex-direction: column; gap: 5rpx; min-width: 0; }
.sname { font-size: 27rpx; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ssub { font-size: 21rpx; color: rgba(243, 244, 246, .35); }
.play-icon { color: rgba(245, 180, 91, .8); font-size: 22rpx; padding: 0 10rpx; }

.empty { display: flex; flex-direction: column; align-items: center; gap: 14rpx; padding: 80rpx 0; color: rgba(243, 244, 246, .3); font-size: 23rpx; }
.e-ico { font-size: 50rpx; opacity: .5; }
</style>

