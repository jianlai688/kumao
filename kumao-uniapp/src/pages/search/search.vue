<template>
  <view class="page">
    <!-- 搜索框 -->
    <view class="search-bar">
      <view class="input-wrap">
        <text class="s-ico">🔍</text>
        <input v-model="keyword" placeholder="搜索歌曲、歌手、歌单" placeholder-class="ph" class="input" confirm-type="search" @confirm="doSearch" />
        <text v-if="keyword" class="clear" @click="keyword = ''">✕</text>
      </view>
      <view class="search-btn" @click="doSearch">搜索</view>
    </view>

    <!-- 热搜 -->
    <view v-if="!searched" class="hot">
      <text class="sec-title">热门搜索</text>
      <view class="chips">
        <text v-for="(w, i) in hotWords" :key="w" :class="['hot-chip', { top: i < 3 }]" @click="quick(w)">{{ w }}</text>
      </view>

      <text class="sec-title" style="margin-top: 40rpx">大家都在听</text>
      <view class="tips">
        <view class="tip-row"><text class="tip-ico">🎧</text><text>试试搜索「晴天」，感受经典旋律</text></view>
        <view class="tip-row"><text class="tip-ico">🎤</text><text>搜索歌手名，直达歌手主页</text></view>
        <view class="tip-row"><text class="tip-ico">📻</text><text>搜索「弹唱」，发现正在直播的房间</text></view>
      </view>
    </view>

    <!-- 结果 -->
    <view v-else class="results">
      <view class="result-head">
        <text class="rh-title">搜索结果</text>
        <text class="rh-count">{{ total }} 条</text>
      </view>

      <view v-for="item in items" :key="item.type + item.bizId" class="item" @click="open(item)">
        <image :src="item.coverUrl" mode="aspectFill" class="cover" />
        <view class="meta">
          <text class="title">{{ item.title }}</text>
          <text class="sub">{{ item.subtitle }}</text>
        </view>
        <text class="type">{{ typeName(item.type) }}</text>
      </view>

      <view v-if="!items.length" class="empty">
        <text class="e-ico">🔍</text>
        <text>没有找到相关内容，换个关键词试试</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '@/api'

const keyword = ref('')
const hotWords = ref(['周杰伦', '晴天', '原创', '弹唱', '电台', '翻唱'])
const searched = ref(false)
const items = ref([])
const total = ref(0)

const typeName = (t) => ({ SONG: '歌曲', SINGER: '歌手', PLAYLIST: '歌单', LIVE_ROOM: '直播间', MUSICIAN: '音乐人' }[t] || t)

async function doSearch() {
  if (!keyword.value.trim()) return
  const res = await api.search(keyword.value.trim())
  items.value = res.data?.items || []
  total.value = res.data?.total || 0
  searched.value = true
}

function quick(w) {
  keyword.value = w
  doSearch()
}

onLoad((options) => {
  if (options && options.keyword) {
    keyword.value = decodeURIComponent(options.keyword)
    doSearch()
  }
})

function open(item) {
  if (item.type === 'SONG') uni.navigateTo({ url: `/pages/song/song?id=${item.bizId}` })
  else if (item.type === 'PLAYLIST') uni.navigateTo({ url: `/pages/playlist/detail?id=${item.bizId}` })
  else if (item.type === 'LIVE_ROOM') uni.navigateTo({ url: `/pages/live/room?id=${item.bizId}` })
}
</script>

<style scoped>
.page { padding-bottom: 50rpx; }

.search-bar { display: flex; align-items: center; gap: 16rpx; padding: 24rpx 24rpx 8rpx; }
.input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12rpx;
  height: 76rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, .06);
  border: 1rpx solid rgba(255, 255, 255, .08);
}
.s-ico { font-size: 24rpx; opacity: .5; }
.input { flex: 1; font-size: 26rpx; color: #f3f4f6; }
.ph { color: rgba(243, 244, 246, .3); }
.clear { font-size: 22rpx; color: rgba(243, 244, 246, .4); padding: 0 6rpx; }
.search-btn {
  height: 76rpx;
  line-height: 76rpx;
  padding: 0 32rpx;
  border-radius: 999rpx;
  background: #f5b45b;
  color: #07080a;
  font-size: 26rpx;
  font-weight: 800;
}

.sec-title { display: block; font-size: 30rpx; font-weight: 800; padding: 30rpx 24rpx 16rpx; }
.chips { display: flex; flex-wrap: wrap; padding: 0 24rpx; }
.hot-chip {
  padding: 14rpx 26rpx;
  margin: 0 14rpx 16rpx 0;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, .045);
  border: 1rpx solid rgba(255, 255, 255, .07);
  font-size: 24rpx;
  color: rgba(243, 244, 246, .7);
}
.hot-chip.top { background: rgba(245, 180, 91, .1); border-color: rgba(245, 180, 91, .22); color: #f5b45b; font-weight: 700; }

.tips { padding: 0 24rpx; display: flex; flex-direction: column; gap: 18rpx; }
.tip-row { display: flex; align-items: center; gap: 14rpx; font-size: 24rpx; color: rgba(243, 244, 246, .45); }
.tip-ico { font-size: 28rpx; }

.results { padding-top: 12rpx; }
.result-head { display: flex; align-items: baseline; gap: 12rpx; padding: 20rpx 28rpx 16rpx; }
.rh-title { font-size: 30rpx; font-weight: 800; }
.rh-count { font-size: 22rpx; color: rgba(243, 244, 246, .35); }

.item {
  display: flex;
  align-items: center;
  margin: 0 24rpx 14rpx;
  padding: 16rpx 18rpx;
  border-radius: 22rpx;
  background: #131418;
  border: 1rpx solid rgba(255, 255, 255, .05);
}
.cover { width: 96rpx; height: 96rpx; border-radius: 16rpx; background: #1a1c21; }
.meta { flex: 1; margin-left: 18rpx; display: flex; flex-direction: column; gap: 6rpx; min-width: 0; }
.title { font-size: 27rpx; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sub { font-size: 22rpx; color: rgba(243, 244, 246, .38); }
.type {
  font-size: 19rpx;
  padding: 5rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(245, 180, 91, .1);
  color: #f5b45b;
  border: 1rpx solid rgba(245, 180, 91, .2);
}

.empty { display: flex; flex-direction: column; align-items: center; gap: 16rpx; padding: 120rpx 40rpx; color: rgba(243, 244, 246, .3); font-size: 24rpx; }
.e-ico { font-size: 60rpx; opacity: .5; }
</style>


