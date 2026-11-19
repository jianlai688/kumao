<template>
  <view class="page">
    <km-nav-bar title="我喜欢的音乐" />

    <view class="hero">
      <view class="hero-cover">
        <view class="hero-disc"><view class="hero-hole"></view></view>
      </view>
      <view class="hero-copy">
        <text class="hero-eyebrow">MY FAVORITES</text>
        <text class="hero-title">我喜欢的音乐</text>
        <text class="hero-sub">{{ total }} 首 · 按喜欢时间排序</text>
        <view class="hero-actions">
          <view class="play-all" @click="playAll">
            <km-icon name="play" :size="15" color="#22150a" />
            <text>播放全部</text>
          </view>
          <view class="ghost" @click="shuffleAll">
            <km-icon name="shuffle" :size="15" color="#f5b45b" />
            <text>随机</text>
          </view>
        </view>
      </view>
    </view>

    <view v-if="loading" class="state">
      <view class="spinner"></view>
      <text>正在加载喜欢的歌曲…</text>
    </view>

    <view v-else-if="!songs.length" class="empty">
      <view class="empty-ico"><km-icon name="heart" :size="30" color="rgba(245,180,91,.8)" /></view>
      <text class="empty-title">还没有喜欢的歌</text>
      <text class="empty-sub">在歌曲页点亮红心，喜欢的歌会出现在这里。</text>
      <view class="empty-btn" @click="goDiscover">去发现音乐</view>
    </view>

    <scroll-view v-else scroll-y class="list" @scrolltolower="loadMore">
      <view v-for="(s, i) in songs" :key="s.id" class="song" @click="playAt(i)">
        <image class="cover" :src="coverOf(s.coverUrl)" mode="aspectFill" />
        <view class="meta">
          <text class="name">{{ s.songName || s.name }}</text>
          <text class="sub">{{ s.singerName || '未知歌手' }}</text>
        </view>
        <text class="dur">{{ formatTime(Math.round((s.duration || 0))) }}</text>
        <view class="unlike" @click.stop="unlike(s)">
          <km-icon name="heart-fill" :size="18" color="#fb7185" />
        </view>
      </view>
      <view class="list-foot">{{ hasMore ? '加载中…' : '没有更多了' }}</view>
    </scroll-view>

    <km-mini-player />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { api } from '@/api'
import KmNavBar from '@/components/km-nav-bar.vue'
import KmIcon from '@/components/km-icon.vue'
import KmMiniPlayer from '@/components/km-mini-player.vue'
import { coverOf } from '@/utils/media'
import { play, usePlayer } from '@/utils/player'

const { state: playerState, formatTime } = usePlayer()

const songs = ref([])
const total = ref(0)
const page = ref(1)
const size = 20
const hasMore = ref(true)
const loading = ref(true)
const token = ref(uni.getStorageSync('km_token') || '')

function normalize(record) {
  return {
    ...record,
    id: record.songId || record.id,
    name: record.songName || record.name,
    singer: record.singerName || record.singer,
    audioUrl: record.audioUrl || record.url
  }
}

async function load(reset = false) {
  if (!token.value) {
    loading.value = false
    songs.value = []
    return
  }
  if (reset) {
    page.value = 1
    hasMore.value = true
  }
  if (!hasMore.value) return
  loading.value = page.value === 1
  const res = await api.likes({ pageNum: page.value, pageSize: size }).catch(() => null)
  const data = (res && res.data) || {}
  const records = (data.records || []).map(normalize)
  songs.value = page.value === 1 ? records : songs.value.concat(records)
  total.value = data.total || songs.value.length
  hasMore.value = songs.value.length < total.value && records.length > 0
  page.value += 1
  loading.value = false
}

function loadMore() {
  if (hasMore.value && !loading.value) load()
}

function playAt(i) {
  play(songs.value[i], songs.value)
  uni.navigateTo({ url: `/pages/song/song?id=${songs.value[i].id}` })
}

function playAll() {
  if (!songs.value.length) return
  playAt(0)
}

function shuffleAll() {
  if (!songs.value.length) return
  const idx = Math.floor(Math.random() * songs.value.length)
  playAt(idx)
}

async function unlike(song) {
  try {
    await api.unlike(song.id)
    songs.value = songs.value.filter((s) => s.id !== song.id)
    total.value = Math.max(0, total.value - 1)
    uni.showToast({ title: '已取消喜欢', icon: 'none' })
  } catch (e) { /* toast handled */ }
}

function goDiscover() {
  uni.switchTab({ url: '/pages/index/index' })
}

onLoad(() => {
  token.value = uni.getStorageSync('km_token') || ''
  load(true)
})

onShow(() => {
  if (songs.value.length) load(true)
})
</script>

<style scoped>
.page { min-height: 100vh; background: #08070a; padding-bottom: 180rpx; box-sizing: border-box; }

.hero { display: flex; gap: 22rpx; padding: 20rpx 26rpx 30rpx; align-items: center; }
.hero-cover {
  width: 180rpx; height: 180rpx; border-radius: 22rpx; flex: none;
  background: radial-gradient(circle at 30% 25%, #3a2a1a, #14100c 70%);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 16rpx 40rpx rgba(0,0,0,.55);
}
.hero-disc {
  width: 122rpx; height: 122rpx; border-radius: 50%;
  background: repeating-radial-gradient(circle, #1c1712 0 4rpx, #241d16 4rpx 8rpx);
  display: flex; align-items: center; justify-content: center;
  border: 1rpx solid rgba(245,180,91,.25);
}
.hero-hole { width: 34rpx; height: 34rpx; border-radius: 50%; background: var(--km-accent); opacity: .85; }
.hero-copy { flex: 1; min-width: 0; }
.hero-eyebrow { font-size: 17rpx; letter-spacing: 3rpx; color: var(--km-accent); font-weight: 800; }
.hero-title { display: block; margin-top: 6rpx; font-size: 36rpx; font-weight: 900; color: #f7f1e8; letter-spacing: -1rpx; }
.hero-sub { display: block; margin-top: 6rpx; font-size: 21rpx; color: rgba(247,241,232,.42); }
.hero-actions { display: flex; gap: 12rpx; margin-top: 16rpx; }
.play-all {
  display: flex; align-items: center; gap: 6rpx; height: 60rpx; padding: 0 24rpx;
  border-radius: 999rpx; background: linear-gradient(135deg,#fbd08a,#de823c); color: #22150a;
  font-size: 22rpx; font-weight: 800;
}
.ghost {
  display: flex; align-items: center; gap: 6rpx; height: 60rpx; padding: 0 20rpx;
  border-radius: 999rpx; background: rgba(245,180,91,.1); border: 1rpx solid rgba(245,180,91,.26);
  color: var(--km-accent); font-size: 22rpx; font-weight: 700;
}

.state { padding: 120rpx 0; display: flex; flex-direction: column; align-items: center; gap: 16rpx; color: rgba(247,241,232,.4); font-size: 23rpx; }
.spinner { width: 44rpx; height: 44rpx; border-radius: 50%; border: 4rpx solid rgba(255,255,255,.14); border-top-color: var(--km-accent); animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.empty { padding: 100rpx 60rpx; display: flex; flex-direction: column; align-items: center; gap: 12rpx; }
.empty-ico { width: 108rpx; height: 108rpx; border-radius: 50%; background: rgba(245,180,91,.08); display: flex; align-items: center; justify-content: center; }
.empty-title { font-size: 30rpx; font-weight: 800; color: #f7f1e8; }
.empty-sub { font-size: 22rpx; color: rgba(247,241,232,.38); text-align: center; line-height: 1.6; }
.empty-btn { margin-top: 10rpx; height: 66rpx; line-height: 66rpx; padding: 0 30rpx; border-radius: 999rpx; background: rgba(245,180,91,.14); border: 1rpx solid rgba(245,180,91,.3); color: #f5cf9a; font-size: 23rpx; font-weight: 700; }

.list { padding: 0 16rpx; }
.song { display: flex; align-items: center; gap: 16rpx; padding: 14rpx 10rpx; border-radius: 16rpx; }
.song:active { background: rgba(255,255,255,.04); }
.cover { width: 92rpx; height: 92rpx; border-radius: 12rpx; flex: none; background: #17151b; }
.meta { flex: 1; min-width: 0; }
.name { display: block; font-size: 26rpx; color: #f3f4f6; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sub { display: block; margin-top: 4rpx; font-size: 21rpx; color: rgba(247,241,232,.36); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dur { font-size: 20rpx; color: rgba(247,241,232,.28); }
.unlike { width: 56rpx; height: 56rpx; display: flex; align-items: center; justify-content: center; }
.list-foot { text-align: center; padding: 30rpx 0 40rpx; font-size: 20rpx; color: rgba(247,241,232,.24); }
</style>


