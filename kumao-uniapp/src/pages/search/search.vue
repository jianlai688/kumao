<template>
  <view class="page">
    <!-- 搜索框 -->
    <view class="search-bar">
      <view class="input-wrap">
        <text class="s-ico">🔍</text>
        <input
          v-model="keyword"
          placeholder="搜索歌曲、歌手、歌单、直播间"
          placeholder-class="ph"
          class="input"
          confirm-type="search"
          @confirm="doSearch"
        />
        <text v-if="keyword" class="clear" @click="keyword = ''">✕</text>
      </view>
      <view class="search-btn" @click="doSearch">搜索</view>
    </view>

    <!-- 未搜索：发现区 -->
    <view v-if="!searched" class="discover">
      <!-- 搜索历史 -->
      <view v-if="history.length" class="block">
        <view class="block-head">
          <text class="sec-title">搜索历史</text>
          <text class="head-action" @click="clearHistory">清空</text>
        </view>
        <view class="chips">
          <view v-for="w in history" :key="w" class="chip" @click="quick(w)">
            <text class="chip-text">{{ w }}</text>
          </view>
        </view>
      </view>

      <!-- 热门搜索 -->
      <view class="block">
        <view class="block-head">
          <text class="sec-title">热门搜索</text>
          <text class="head-note">实时热度</text>
        </view>
        <view class="hot-grid">
          <view
            v-for="(w, i) in hotWords"
            :key="w"
            :class="['hot-cell', { top: i < 3 }]"
            @click="quick(w)"
          >
            <text class="hot-rank">{{ i + 1 }}</text>
            <text class="hot-word">{{ w }}</text>
            <text v-if="i === 0" class="hot-fire">🔥</text>
          </view>
        </view>
      </view>

      <!-- 快捷入口 -->
      <view class="block">
        <view class="block-head">
          <text class="sec-title">快捷发现</text>
        </view>
        <view class="quick-grid">
          <view v-for="q in quickEntries" :key="q.key" class="quick-cell" @click="goQuick(q)">
            <text class="quick-ico">{{ q.icon }}</text>
            <text class="quick-label">{{ q.label }}</text>
            <text class="quick-desc">{{ q.desc }}</text>
          </view>
        </view>
      </view>

      <!-- 猜你喜欢 -->
      <view v-if="hotSongs.length" class="block">
        <view class="block-head">
          <text class="sec-title">猜你喜欢</text>
          <text class="head-action" @click="goRank">更多 ›</text>
        </view>
        <view class="song-grid">
          <view v-for="s in hotSongs" :key="s.songId || s.id" class="song-card" @click="playSong(s)">
            <image v-if="s.coverUrl" :src="coverOf(s.coverUrl)" mode="aspectFill" class="song-cover" />
            <view v-else class="song-cover ph-cover"><text>♫</text></view>
            <text class="song-name">{{ s.name }}</text>
            <text class="song-singer">{{ s.singerName }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 结果 -->
    <view v-else class="results">
      <view class="tabs">
        <text
          v-for="t in tabs"
          :key="t.key"
          :class="['tab', { on: filter === t.key }]"
          @click="filter = t.key"
        >{{ t.label }}<text v-if="t.key !== 'ALL' && countOf(t.key)" class="tab-num">{{ countOf(t.key) }}</text></text>
      </view>

      <view v-for="item in filteredItems" :key="item.type + item.bizId" class="item" @click="open(item)">
        <image v-if="item.coverUrl" :src="coverOf(item.coverUrl)" mode="aspectFill" class="cover" />
        <view v-else class="cover ph-cover"><text>♫</text></view>
        <view class="meta">
          <text class="title">{{ plain(item.title) }}</text>
          <text class="sub">{{ plain(item.subtitle) }}</text>
        </view>
        <text :class="['type', item.type.toLowerCase()]">{{ typeName(item.type) }}</text>
      </view>

      <view v-if="!filteredItems.length" class="empty">
        <text class="e-ico">🔍</text>
        <text class="e-text">没有找到相关内容</text>
        <text class="e-sub">换个关键词，或试试下方的热门搜索</text>
        <view class="empty-chips">
          <text v-for="w in hotWords.slice(0, 4)" :key="w" class="chip small" @click="quick(w)">{{ w }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { api } from '@/api'
import { coverOf } from '@/utils/media'

const HISTORY_KEY = 'km_search_history'

const keyword = ref('')
const hotWords = ref(['周杰伦', '晴天', '原创', '弹唱', '电台', '翻唱'])
const searched = ref(false)
const items = ref([])
const total = ref(0)
const history = ref([])
const hotSongs = ref([])
const filter = ref('ALL')

const tabs = [
  { key: 'ALL', label: '全部' },
  { key: 'SONG', label: '单曲' },
  { key: 'SINGER', label: '歌手' },
  { key: 'PLAYLIST', label: '歌单' },
  { key: 'LIVE_ROOM', label: '直播' }
]

const quickEntries = [
  { key: 'singer', icon: '🎤', label: '歌手', desc: '找歌手', url: '/pages/singer/singer', tab: false },
  { key: 'playlist', icon: '🎧', label: '歌单', desc: '按心情听', url: '/pages/playlist/playlist', tab: true },
  { key: 'mv', icon: '📺', label: 'MV', desc: '看现场', url: '/pages/mv/mv', tab: false },
  { key: 'live', icon: '📻', label: '直播', desc: '正在开播', url: '/pages/live/live', tab: true }
]

const plain = (s) => String(s || '').replace(/<[^>]+>/g, '')
const typeName = (t) => ({ SONG: '单曲', SINGER: '歌手', PLAYLIST: '歌单', LIVE_ROOM: '直播', MUSICIAN: '音乐人' }[t] || t)

const filteredItems = computed(() => (filter.value === 'ALL' ? items.value : items.value.filter((i) => i.type === filter.value)))

function countOf(type) {
  return items.value.filter((i) => i.type === type).length
}

function loadHistory() {
  history.value = uni.getStorageSync(HISTORY_KEY) || []
}

function pushHistory(word) {
  const list = [word, ...(history.value || []).filter((w) => w !== word)].slice(0, 10)
  history.value = list
  uni.setStorageSync(HISTORY_KEY, list)
}

function clearHistory() {
  uni.showModal({
    title: '清空搜索历史',
    content: '确定要清空全部搜索记录吗？',
    confirmText: '清空',
    success: (res) => {
      if (!res.confirm) return
      history.value = []
      uni.removeStorageSync(HISTORY_KEY)
    }
  })
}

async function doSearch() {
  const word = keyword.value.trim()
  if (!word) return
  pushHistory(word)
  const res = await api.search(word)
  items.value = res.data?.items || []
  total.value = res.data?.total || 0
  filter.value = 'ALL'
  searched.value = true
}

function quick(w) {
  keyword.value = w
  doSearch()
}

function goQuick(q) {
  if (q.tab) uni.switchTab({ url: q.url })
  else uni.navigateTo({ url: q.url })
}

function goRank() {
  uni.switchTab({ url: '/pages/rank/rank' })
}

function playSong(song) {
  uni.navigateTo({ url: `/pages/song/song?id=${song.songId || song.id}` })
}

async function loadHot() {
  try {
    const res = await api.hot(6)
    hotSongs.value = (res.data || []).slice(0, 6)
  } catch (e) { /* ignore */ }
}

onLoad((options) => {
  loadHistory()
  loadHot()
  if (options && options.keyword) {
    keyword.value = decodeURIComponent(options.keyword)
    doSearch()
  }
})

onShow(loadHistory)

function open(item) {
  if (item.type === 'SONG') uni.navigateTo({ url: `/pages/song/song?id=${item.bizId}` })
  else if (item.type === 'PLAYLIST') uni.navigateTo({ url: `/pages/playlist/detail?id=${item.bizId}` })
  else if (item.type === 'LIVE_ROOM') uni.navigateTo({ url: `/pages/live/room?id=${item.bizId}` })
  else if (item.type === 'SINGER') uni.navigateTo({ url: `/pages/singer/singer?id=${item.bizId}` })
  else if (item.type === 'MUSICIAN') uni.navigateTo({ url: `/pages/musician/detail?id=${item.bizId}` })
}
</script>

<style scoped>
.page { min-height: 100vh; padding-bottom: 60rpx; }

/* 搜索框 */
.search-bar { display: flex; align-items: center; gap: 16rpx; padding: 24rpx 24rpx 10rpx; }
.input-wrap {
  flex: 1; display: flex; align-items: center; gap: 12rpx;
  height: 78rpx; padding: 0 26rpx; border-radius: 999rpx;
  background: rgba(255, 255, 255, .06);
  border: 1rpx solid rgba(255, 255, 255, .09);
}
.s-ico { font-size: 24rpx; opacity: .5; }
.input { flex: 1; font-size: 26rpx; color: #f3f4f6; }
.ph { color: rgba(243, 244, 246, .3); }
.clear { font-size: 22rpx; color: rgba(243, 244, 246, .4); padding: 0 6rpx; }
.search-btn {
  height: 78rpx; line-height: 78rpx; padding: 0 34rpx; border-radius: 999rpx;
  background: linear-gradient(135deg, #f8c573, #e79a3f);
  color: #1a1206; font-size: 26rpx; font-weight: 800;
  box-shadow: 0 10rpx 24rpx rgba(245, 180, 91, .22);
}

/* 区块 */
.discover { padding-top: 8rpx; }
.block { margin-top: 18rpx; }
.block-head { display: flex; align-items: baseline; justify-content: space-between; padding: 18rpx 26rpx 10rpx; }
.sec-title { font-size: 30rpx; font-weight: 800; letter-spacing: .01em; color: #f7f1e8; }
.head-action { font-size: 22rpx; color: rgba(245, 180, 91, .85); }
.head-note { font-size: 20rpx; color: rgba(247, 241, 232, .3); }

/* 历史 */
.chips { display: flex; flex-wrap: wrap; padding: 4rpx 24rpx 0; }
.chip {
  padding: 14rpx 26rpx; margin: 0 14rpx 14rpx 0; border-radius: 999rpx;
  background: rgba(255, 255, 255, .05);
  border: 1rpx solid rgba(255, 255, 255, .07);
  font-size: 24rpx; color: rgba(247, 241, 232, .72);
}
.chip.small { font-size: 22rpx; padding: 10rpx 20rpx; }

/* 热门搜索 */
.hot-grid { display: flex; flex-wrap: wrap; padding: 0 18rpx; }
.hot-cell {
  width: calc(50% - 16rpx); margin: 0 8rpx 14rpx;
  display: flex; align-items: center; gap: 14rpx;
  padding: 18rpx 20rpx; border-radius: 20rpx;
  background: #131418; border: 1rpx solid rgba(255, 255, 255, .05);
}
.hot-cell.top { background: linear-gradient(135deg, rgba(245, 180, 91, .11), rgba(245, 180, 91, .02)); border-color: rgba(245, 180, 91, .22); }
.hot-rank { font-size: 26rpx; font-weight: 900; color: rgba(247, 241, 232, .28); width: 30rpx; }
.hot-cell.top .hot-rank { color: var(--km-accent); }
.hot-word { flex: 1; font-size: 26rpx; color: rgba(247, 241, 232, .82); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hot-cell.top .hot-word { color: #f7f1e8; font-weight: 700; }
.hot-fire { font-size: 24rpx; }

/* 快捷入口 */
.quick-grid { display: flex; padding: 4rpx 18rpx; }
.quick-cell {
  flex: 1; margin: 0 8rpx; padding: 22rpx 0 20rpx;
  display: flex; flex-direction: column; align-items: center; gap: 6rpx;
  border-radius: 22rpx; background: #131418; border: 1rpx solid rgba(255, 255, 255, .05);
}
.quick-ico { font-size: 36rpx; }
.quick-label { font-size: 24rpx; font-weight: 700; color: #f7f1e8; }
.quick-desc { font-size: 18rpx; color: rgba(247, 241, 232, .3); }

/* 猜你喜欢 */
.song-grid { display: flex; flex-wrap: wrap; padding: 0 18rpx; }
.song-card { width: calc(33.33% - 16rpx); margin: 0 8rpx 20rpx; }
.song-cover { width: 100%; height: 200rpx; border-radius: 20rpx; background: #1a1c21; }
.ph-cover { display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #1d1626, #2a1b33); color: rgba(245, 180, 91, .45); font-size: 48rpx; }
.song-name { display: block; margin-top: 10rpx; font-size: 24rpx; color: #f7f1e8; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.song-singer { display: block; margin-top: 2rpx; font-size: 20rpx; color: rgba(247, 241, 232, .35); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* 结果 */
.results { padding-top: 6rpx; }
.tabs { display: flex; gap: 10rpx; padding: 18rpx 24rpx 8rpx; overflow-x: auto; }
.tab {
  padding: 12rpx 24rpx; border-radius: 999rpx;
  font-size: 24rpx; color: rgba(247, 241, 232, .5);
  background: rgba(255, 255, 255, .04); border: 1rpx solid transparent;
  white-space: nowrap;
}
.tab.on { color: var(--km-accent); background: rgba(245, 180, 91, .1); border-color: rgba(245, 180, 91, .25); font-weight: 700; }
.tab-num { font-size: 18rpx; margin-left: 6rpx; opacity: .7; }

.item {
  display: flex; align-items: center; margin: 0 24rpx 14rpx; padding: 16rpx 18rpx;
  border-radius: 22rpx; background: #131418; border: 1rpx solid rgba(255, 255, 255, .05);
}
.cover { width: 96rpx; height: 96rpx; border-radius: 16rpx; background: #1a1c21; }
.meta { flex: 1; margin-left: 18rpx; display: flex; flex-direction: column; gap: 6rpx; min-width: 0; }
.title { font-size: 27rpx; font-weight: 600; color: #f7f1e8; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sub { font-size: 22rpx; color: rgba(247, 241, 232, .38); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.type { font-size: 19rpx; padding: 5rpx 14rpx; border-radius: 999rpx; color: rgba(247, 241, 232, .45); background: rgba(255, 255, 255, .05); }
.type.song { color: var(--km-accent); background: rgba(245, 180, 91, .1); }

.empty { display: flex; flex-direction: column; align-items: center; padding: 90rpx 40rpx; }
.e-ico { font-size: 64rpx; opacity: .35; }
.e-text { margin-top: 18rpx; font-size: 28rpx; color: rgba(247, 241, 232, .7); }
.e-sub { margin-top: 8rpx; font-size: 22rpx; color: rgba(247, 241, 232, .3); }
.empty-chips { display: flex; flex-wrap: wrap; justify-content: center; margin-top: 24rpx; }
</style>





