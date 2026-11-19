<template>
  <view class="page">
    <km-nav-bar title="全部直播间" />

    <view class="search" @click="goSearch">
      <km-icon name="search" :size="16" color="rgba(247,241,232,.35)" />
      <text class="search-ph">搜索主播、直播间</text>
    </view>

    <scroll-view scroll-x class="cats" :show-scrollbar="false">
      <view v-for="c in cats" :key="c" :class="['cat', { on: cat === c }]" @click="pick(c)">{{ c }}</view>
    </scroll-view>

    <view v-if="loading && !list.length" class="state"><view class="spinner"></view><text>正在加载直播间…</text></view>

    <view v-else-if="!list.length" class="empty">
      <text class="empty-title">暂时没有直播</text>
      <text class="empty-sub">主播开播后会出现在这里，先去听听歌吧。</text>
    </view>

    <scroll-view v-else scroll-y class="grid-wrap" @scrolltolower="loadMore">
      <view class="grid">
        <view v-for="r in list" :key="r.id" class="card" @click="open(r)">
          <view class="thumb">
            <image class="cover" :src="coverOf(r.coverUrl)" mode="aspectFill" />
            <view class="badge"><text class="dot"></text>LIVE</view>
            <view class="online"><km-icon name="user" :size="11" color="#fff" /><text>{{ formatCount(r.realtimeOnline) }}</text></view>
          </view>
          <text class="title">{{ r.title }}</text>
          <view class="meta">
            <text class="anchor">{{ r.anchorName }}</text>
            <text class="cat-tag">{{ r.category || '音乐' }}</text>
          </view>
        </view>
      </view>
      <view class="foot">{{ hasMore ? '加载中…' : '没有更多了' }}</view>
    </scroll-view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '@/api'
import KmNavBar from '@/components/km-nav-bar.vue'
import KmIcon from '@/components/km-icon.vue'
import { coverOf, formatCount } from '@/utils/media'

const list = ref([])
const page = ref(1)
const hasMore = ref(true)
const loading = ref(true)
const cat = ref('全部')
const categories = ref([])

const cats = computed(() => ['全部'].concat(categories.value))

async function load(reset = false) {
  if (reset) {
    page.value = 1
    hasMore.value = true
    loading.value = true
  }
  if (!hasMore.value) return
  const res = await api.liveSquare({
    pageNum: page.value,
    pageSize: 20,
    category: cat.value === '全部' ? undefined : cat.value
  }).catch(() => null)
  const data = (res && res.data) || {}
  const records = data.records || []
  list.value = page.value === 1 ? records : list.value.concat(records)
  hasMore.value = records.length === 20
  page.value += 1
  loading.value = false
}

function loadMore() {
  if (hasMore.value && !loading.value) load()
}

function pick(c) {
  if (cat.value === c) return
  cat.value = c
  load(true)
}

function open(room) {
  uni.navigateTo({ url: `/pages/live/room?id=${room.id}` })
}

function goSearch() {
  uni.navigateTo({ url: '/pages/search/search' })
}

onLoad(async () => {
  const res = await api.liveCategories().catch(() => null)
  categories.value = ((res && res.data) || []).map((c) => (typeof c === 'string' ? c : c.name)).filter(Boolean).slice(0, 8)
  load(true)
})
</script>

<style scoped>
.page { min-height: 100vh; background: #08070a; padding-bottom: 40rpx; box-sizing: border-box; }
.search {
  display: flex; align-items: center; gap: 10rpx; height: 76rpx; margin: 6rpx 24rpx 16rpx;
  padding: 0 22rpx; border-radius: 999rpx; background: rgba(255,255,255,.05);
  border: 1rpx solid rgba(255,255,255,.06);
}
.search-ph { font-size: 23rpx; color: rgba(247,241,232,.32); }
.cats { white-space: nowrap; padding: 0 24rpx 10rpx; }
.cat {
  display: inline-block; padding: 12rpx 24rpx; margin-right: 12rpx; border-radius: 999rpx;
  background: rgba(255,255,255,.05); color: rgba(247,241,232,.5); font-size: 22rpx;
}
.cat.on { background: rgba(245,180,91,.18); color: #f5cf9a; font-weight: 800; }
.state { padding: 140rpx 0; display: flex; flex-direction: column; align-items: center; gap: 16rpx; color: rgba(247,241,232,.4); font-size: 23rpx; }
.spinner { width: 44rpx; height: 44rpx; border-radius: 50%; border: 4rpx solid rgba(255,255,255,.14); border-top-color: var(--km-accent); animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.empty { padding: 140rpx 60rpx; display: flex; flex-direction: column; align-items: center; gap: 10rpx; }
.empty-title { font-size: 29rpx; font-weight: 800; color: #f7f1e8; }
.empty-sub { font-size: 22rpx; color: rgba(247,241,232,.36); text-align: center; }
.grid-wrap { padding: 0 20rpx; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18rpx; }
.card { border-radius: 20rpx; overflow: hidden; background: rgba(255,255,255,.035); border: 1rpx solid rgba(255,255,255,.05); padding-bottom: 14rpx; }
.thumb { position: relative; width: 100%; height: 200rpx; }
.cover { width: 100%; height: 100%; background: #17151b; }
.badge {
  position: absolute; top: 10rpx; left: 10rpx; display: flex; align-items: center; gap: 5rpx;
  padding: 4rpx 10rpx; border-radius: 999rpx; background: #fb7185; color: #fff; font-size: 15rpx; font-weight: 800;
}
.dot { width: 6rpx; height: 6rpx; border-radius: 50%; background: #fff; }
.online {
  position: absolute; right: 10rpx; bottom: 10rpx; display: flex; align-items: center; gap: 4rpx;
  padding: 3rpx 10rpx; border-radius: 999rpx; background: rgba(0,0,0,.55); color: #fff; font-size: 16rpx;
}
.title { display: block; padding: 12rpx 14rpx 0; font-size: 24rpx; font-weight: 700; color: #f3f4f6; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.meta { display: flex; align-items: center; gap: 8rpx; padding: 6rpx 14rpx 0; }
.anchor { font-size: 20rpx; color: rgba(247,241,232,.36); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cat-tag { padding: 2rpx 10rpx; border-radius: 5rpx; background: rgba(245,180,91,.14); color: #f5cf9a; font-size: 16rpx; }
.foot { text-align: center; padding: 26rpx 0 40rpx; font-size: 20rpx; color: rgba(247,241,232,.24); }
</style>


