<template>
  <view class="page">
    <km-nav-bar title="我的下载" />

    <view class="head">
      <view class="head-copy">
        <text class="eyebrow">OFFLINE</text>
        <text class="title">离线音乐</text>
        <text class="sub">{{ list.length }} 首 · 共 {{ totalSize }}</text>
      </view>
      <view v-if="list.length" class="clear" @click="clearAll">全部删除</view>
    </view>

    <view v-if="loading" class="state"><view class="spinner"></view><text>正在读取本地文件…</text></view>

    <view v-else-if="!list.length" class="empty">
      <view class="empty-ico"><km-icon name="download" :size="28" color="rgba(245,180,91,.8)" /></view>
      <text class="empty-title">还没有下载歌曲</text>
      <text class="empty-sub">会员在歌曲页点击“下载”，即可离线收听。</text>
    </view>

    <scroll-view v-else scroll-y class="list">
      <view v-for="s in list" :key="s.id" class="song" @click="playLocal(s)">
        <image class="cover" :src="coverOf(s.coverUrl)" mode="aspectFill" />
        <view class="meta">
          <text class="name">{{ s.name }}</text>
          <view class="subline">
            <text class="tag">已下载</text>
            <text class="sub">{{ s.singer }} · {{ formatSize(s.size) }}</text>
          </view>
        </view>
        <view class="del" @click.stop="remove(s)">
          <km-icon name="close" :size="15" color="rgba(247,241,232,.45)" />
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import KmNavBar from '@/components/km-nav-bar.vue'
import KmIcon from '@/components/km-icon.vue'
import { coverOf } from '@/utils/media'
import { play } from '@/utils/player'
import { clearDownloads, formatSize, getLocalUrl, listDownloads, removeDownload } from '@/utils/download'

const list = ref([])
const loading = ref(true)

const totalSize = computed(() => {
  const sum = list.value.reduce((acc, s) => acc + (s.size || 0), 0)
  return formatSize(sum)
})

async function load() {
  loading.value = true
  try {
    list.value = await listDownloads()
  } catch (e) {
    list.value = []
  }
  loading.value = false
}

async function playLocal(song) {
  const url = await getLocalUrl(song.id)
  if (!url) {
    uni.showToast({ title: '本地文件已失效，请重新下载', icon: 'none' })
    return
  }
  play({ ...song, audioUrl: url }, list.value.map((s) => ({ ...s })))
}

function remove(song) {
  uni.showModal({
    title: '删除下载',
    content: `确定删除《${song.name}》的本地文件？`,
    success: async (res) => {
      if (!res.confirm) return
      await removeDownload(song.id)
      list.value = list.value.filter((s) => s.id !== song.id)
      uni.showToast({ title: '已删除', icon: 'none' })
    }
  })
}

function clearAll() {
  uni.showModal({
    title: '清空下载',
    content: '将删除全部离线歌曲，确定继续？',
    success: async (res) => {
      if (!res.confirm) return
      await clearDownloads()
      list.value = []
      uni.showToast({ title: '已清空', icon: 'none' })
    }
  })
}

onShow(load)
</script>

<style scoped>
.page { min-height: 100vh; background: #08070a; padding-bottom: 60rpx; box-sizing: border-box; }
.head { display: flex; align-items: flex-end; justify-content: space-between; padding: 10rpx 26rpx 24rpx; }
.head-copy { display: flex; flex-direction: column; gap: 4rpx; }
.eyebrow { font-size: 17rpx; letter-spacing: 3rpx; color: #f5b45b; font-weight: 800; }
.title { font-size: 36rpx; font-weight: 900; color: #f7f1e8; letter-spacing: -1rpx; }
.sub { font-size: 21rpx; color: rgba(247,241,232,.4); }
.clear { font-size: 21rpx; color: #ff9fb0; padding: 10rpx 18rpx; border-radius: 999rpx; background: rgba(251,113,133,.1); }
.state { padding: 130rpx 0; display: flex; flex-direction: column; align-items: center; gap: 16rpx; color: rgba(247,241,232,.4); font-size: 23rpx; }
.spinner { width: 44rpx; height: 44rpx; border-radius: 50%; border: 4rpx solid rgba(255,255,255,.14); border-top-color: #f5b45b; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.empty { padding: 120rpx 60rpx; display: flex; flex-direction: column; align-items: center; gap: 12rpx; }
.empty-ico { width: 108rpx; height: 108rpx; border-radius: 50%; background: rgba(245,180,91,.08); display: flex; align-items: center; justify-content: center; }
.empty-title { font-size: 30rpx; font-weight: 800; color: #f7f1e8; }
.empty-sub { font-size: 22rpx; color: rgba(247,241,232,.38); text-align: center; line-height: 1.6; }
.list { padding: 0 16rpx; }
.song { display: flex; align-items: center; gap: 16rpx; padding: 14rpx 10rpx; border-radius: 16rpx; }
.song:active { background: rgba(255,255,255,.04); }
.cover { width: 92rpx; height: 92rpx; border-radius: 12rpx; flex: none; background: #17151b; }
.meta { flex: 1; min-width: 0; }
.name { display: block; font-size: 26rpx; color: #f3f4f6; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.subline { display: flex; align-items: center; gap: 8rpx; margin-top: 6rpx; }
.tag { padding: 2rpx 8rpx; border-radius: 5rpx; background: rgba(74,222,128,.14); color: #86efac; font-size: 16rpx; }
.sub { font-size: 20rpx; color: rgba(247,241,232,.34); }
.del { width: 52rpx; height: 52rpx; display: flex; align-items: center; justify-content: center; }
</style>
