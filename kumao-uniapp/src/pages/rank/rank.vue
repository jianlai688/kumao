<template>
  <view class="km-screen rank-page">
    <view class="head">
      <view class="tabs">
        <view
          v-for="t in tabs"
          :key="t.key"
          :class="['tab', { on: type === t.key }]"
          @click="switchTab(t.key)"
        >
          <text class="tn">{{ t.name }}</text>
          <text class="td">{{ t.desc }}</text>
        </view>
      </view>
      <text class="date">{{ today }} · 每天更新</text>
    </view>

    <view class="list">
      <km-song-row
        v-for="(s, i) in list"
        :key="s.songId || s.id"
        :song="s"
        :index="i"
        :show-rank="true"
        :show-plays="true"
        :liked="likedIds.indexOf(s.songId) >= 0"
        @select="open"
        @like="toggleLike"
      />
      <view v-if="!list.length" class="km-empty">
        <km-icon name="rank" :size="34" color="rgba(247,241,232,.22)" />
        <text>{{ loading ? '正在加载榜单…' : '暂无榜单数据' }}</text>
      </view>
    </view>

    <view class="tip">榜单每小时更新 · 数据来自全站播放行为</view>

    <km-mini-player />
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import KmSongRow from '@/components/km-song-row.vue'
import KmMiniPlayer from '@/components/km-mini-player.vue'
import KmIcon from '@/components/km-icon.vue'
import { api } from '@/api'

const tabs = [
  { key: 'hot', name: '热门榜', desc: '全站最热' },
  { key: 'new', name: '新歌榜', desc: '每周上新' },
  { key: 'rising', name: '飙升榜', desc: '热度飙升' }
]
const type = ref('hot')
const list = ref([])
const loading = ref(true)
const likedIds = ref([])
const today = new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })

async function load() {
  loading.value = true
  const fn = type.value === 'hot' ? api.hot : type.value === 'new' ? api.newSongs : api.rising
  const res = await fn(50).catch(() => null)
  list.value = (res && res.data) || []
  loading.value = false
}

function switchTab(k) {
  if (type.value === k) return
  type.value = k
  list.value = []
  load()
}

function open(s) {
  const id = s.songId || s.id
  uni.setStorageSync('km_current_song', s)
  uni.setStorageSync('km_queue', list.value)
  api.playRecord({ songId: id, device: 'APP' }).catch(() => {})
  uni.navigateTo({ url: `/pages/song/song?id=${id}` })
}

function toggleLike(s) {
  const id = s.songId || s.id
  const idx = likedIds.value.indexOf(id)
  if (idx >= 0) { likedIds.value.splice(idx, 1); api.unlike(id).catch(() => {}) }
  else { likedIds.value.push(id); api.like(id).catch(() => {}) }
}

onMounted(load)
</script>

<style scoped>
.rank-page { padding-bottom: calc(var(--window-bottom, 0px) + 190rpx); }
.head { padding: 22rpx 36rpx 4rpx; }
.tabs { display: flex; }
.tab {
  flex: 1;
  margin-right: 14rpx;
  padding: 20rpx 18rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, .035);
  border: 1rpx solid rgba(255, 255, 255, .06);
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.tab:last-child { margin-right: 0; }
.tab.on {
  background: linear-gradient(150deg, rgba(245, 180, 91, .16), rgba(245, 180, 91, .02) 70%);
  border-color: rgba(245, 180, 91, .3);
}
.tn { font-size: 25rpx; font-weight: 800; color: rgba(247, 241, 232, .7); }
.tab.on .tn { color: var(--km-accent); }
.td { font-size: 17rpx; color: rgba(247, 241, 232, .3); }
.date { display: block; margin-top: 18rpx; font-size: 19rpx; color: rgba(247, 241, 232, .28); }
.list { padding: 10rpx 28rpx 0; }
.tip { text-align: center; font-size: 18rpx; color: rgba(247, 241, 232, .2); margin-top: 20rpx; }
</style>



