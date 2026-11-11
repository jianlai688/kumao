<template>
  <view class="km-screen list-page">
    <km-nav-bar title="歌手">
      <view class="ibtn" @click="goSearch">
        <km-icon name="search" :size="18" color="rgba(247,241,232,.82)" />
      </view>
    </km-nav-bar>

    <view class="head">
      <text class="km-sub">按首字母与地区，找到你喜欢的声音</text>
    </view>

    <view class="chips">
      <view v-for="r in regions" :key="r" :class="['chip', { on: region === r }]" @click="pick(r)">{{ r }}</view>
    </view>

    <view class="grid">
      <view v-for="s in filtered" :key="s.id" class="item" @click="open(s)">
        <image class="ava" :src="avatarOf(s.avatar || s.avatarUrl || s.coverUrl)" mode="aspectFill" />
        <text class="name">{{ s.name }}</text>
        <text class="meta">{{ s.region || '华语' }}<text v-if="s.songCount"> · {{ s.songCount }} 首</text></text>
      </view>
    </view>

    <view v-if="!filtered.length" class="km-empty">
      <km-icon name="user" :size="34" color="rgba(247,241,232,.22)" />
      <text>{{ loading ? '正在加载歌手…' : '暂无歌手数据' }}</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import KmNavBar from '@/components/km-nav-bar.vue'
import KmIcon from '@/components/km-icon.vue'
import { api } from '@/api'
import { avatarOf } from '@/utils/media'

const list = ref([])
const loading = ref(true)
const region = ref('全部')
const regions = ['全部', '华语', '欧美', '日韩']

const filtered = computed(() =>
  region.value === '全部' ? list.value : list.value.filter((s) => (s.region || '华语') === region.value)
)

function pick(r) { region.value = r }
function goSearch() { uni.navigateTo({ url: '/pages/search/search' }) }
function open(s) { uni.navigateTo({ url: `/pages/search/search?keyword=${encodeURIComponent(s.name)}` }) }

onMounted(async () => {
  const res = await api.singers({ pageNum: 1, pageSize: 40 }).catch(() => null)
  list.value = (res && res.data && res.data.records) || []
  loading.value = false
})
</script>

<style scoped>
.list-page { padding-bottom: calc(var(--window-bottom, 0px) + 40rpx); }
.ibtn {
  width: 56rpx; height: 56rpx; border-radius: 50%;
  background: rgba(255, 255, 255, .055);
  border: 1rpx solid rgba(255, 255, 255, .07);
  display: flex; align-items: center; justify-content: center;
}
.head { padding: 6rpx 36rpx 0; display: flex; flex-direction: column; gap: 10rpx; }
.chips { display: flex; gap: 12rpx; padding: 24rpx 36rpx 4rpx; }
.chip {
  height: 50rpx; padding: 0 22rpx; border-radius: 999rpx;
  border: 1rpx solid rgba(255, 255, 255, .075);
  background: rgba(255, 255, 255, .025);
  font-size: 19rpx; color: rgba(247, 241, 232, .5);
  display: flex; align-items: center;
}
.chip.on { background: linear-gradient(140deg, #f9c878, #e08b3e); border-color: transparent; color: #2b1a08; font-weight: 800; }

.grid { display: flex; flex-wrap: wrap; padding: 20rpx 28rpx 0; }
.item { width: 33.33%; padding: 18rpx 6rpx; display: flex; flex-direction: column; align-items: center; gap: 8rpx; }
.ava {
  width: 118rpx; height: 118rpx; border-radius: 50%;
  border: 1rpx solid rgba(255, 255, 255, .1);
  background: #1a1c21;
}
.name { font-size: 24rpx; font-weight: 600; color: #f7f1e8; max-width: 200rpx; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.meta { font-size: 18rpx; color: rgba(247, 241, 232, .32); }
</style>


