<template>
  <view class="km-screen list-page">
    <km-nav-bar title="MV 专区">
      <view class="ibtn" @click="goSearch">
        <km-icon name="search" :size="18" color="rgba(247,241,232,.82)" />
      </view>
    </km-nav-bar>

    <view class="head">
      <text class="km-sub">把歌看完，听见画面里的情绪</text>
    </view>

    <view class="grid">
      <view v-for="m in list" :key="m.id" class="card" @click="open(m)">
        <view class="ph">
          <image class="img" :src="coverOf(m.coverUrl || m.cover || m.posterUrl)" mode="aspectFill" />
          <view class="mask"></view>
          <view class="play"><km-icon name="play" :size="11" color="#fff" /></view>
          <text v-if="m.duration" class="dur">{{ fmt(m.duration) }}</text>
        </view>
        <text class="t">{{ m.title || m.name }}</text>
        <text class="s">{{ m.singerName || m.artistName || '酷猫音乐' }}<text v-if="m.playCount"> · {{ formatPlay(m.playCount) }} 次</text></text>
      </view>
    </view>

    <view v-if="!list.length" class="km-empty">
      <km-icon name="video" :size="34" color="rgba(247,241,232,.22)" />
      <text>{{ loading ? '正在加载 MV…' : '暂无 MV 内容' }}</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import KmNavBar from '@/components/km-nav-bar.vue'
import KmIcon from '@/components/km-icon.vue'
import { api } from '@/api'
import { coverOf, formatPlay } from '@/utils/media'

const list = ref([])
const loading = ref(true)

function fmt(sec) {
  const s = Math.floor(Number(sec) || 0)
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
}

function goSearch() { uni.navigateTo({ url: '/pages/search/search' }) }
function open(m) {
  uni.navigateTo({ url: `/pages/mv/play?id=${m.id}` })
}

onMounted(async () => {
  const res = await api.mvs({ pageNum: 1, pageSize: 20 }).catch(() => null)
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

.grid { display: flex; flex-wrap: wrap; padding: 24rpx 30rpx 0; }
.card { width: calc(50% - 10rpx); margin-bottom: 26rpx; }
.card:nth-child(2n) { margin-left: 20rpx; }
.ph {
  position: relative;
  width: 100%;
  height: 190rpx;
  border-radius: 24rpx;
  overflow: hidden;
  background: #1a1c21;
  border: 1rpx solid rgba(255, 255, 255, .07);
}
.img { width: 100%; height: 100%; }
.mask { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(8, 7, 10, .2), transparent 45%, rgba(8, 7, 10, .6)); }
.play {
  position: absolute; right: 12rpx; bottom: 12rpx;
  width: 46rpx; height: 46rpx; border-radius: 50%;
  background: rgba(255, 255, 255, .2);
  border: 1rpx solid rgba(255, 255, 255, .3);
  display: flex; align-items: center; justify-content: center;
}
.dur {
  position: absolute; left: 12rpx; bottom: 12rpx;
  font-size: 15rpx; color: rgba(255, 255, 255, .8);
}
.t { display: block; margin-top: 14rpx; font-size: 24rpx; font-weight: 600; color: #f7f1e8; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.s { display: block; margin-top: 6rpx; font-size: 18rpx; color: rgba(247, 241, 232, .34); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>




