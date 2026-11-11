<template>
  <view class="km-screen live">
    <km-nav-bar title="音乐直播">
      <view class="ibtn" @click="goSearch">
        <km-icon name="search" :size="18" color="rgba(247,241,232,.82)" />
      </view>
      <view class="ibtn" @click="onBell">
        <km-icon name="bell" :size="19" color="rgba(247,241,232,.82)" />
      </view>
    </km-nav-bar>

    <!-- 分类 -->
    <scroll-view class="chips" scroll-x :show-scrollbar="false">
      <view
        v-for="c in cats"
        :key="c"
        :class="['c2', { on: cat === c }]"
        @click="cat = c"
      >{{ c }}</view>
    </scroll-view>

    <!-- 广告位（非会员展示） -->
    <view v-if="ad && !isVip" class="ad-strip" @click="openAd">
      <image class="ad-cover" :src="coverOf(ad.coverUrl)" mode="aspectFill" />
      <view class="ad-copy">
        <text class="ad-kicker">广告 · {{ ad.name }}</text>
        <text class="ad-text">{{ ad.content || '点击了解详情' }}</text>
      </view>
      <view class="ad-cta">看广告领会员</view>
    </view>
    <!-- 精选直播 -->
    <view v-if="featured" class="feat" @click="open(featured)">
      <image class="bg" :src="coverOf(featured.coverUrl)" mode="aspectFill" />
      <view class="scrim"></view>
      <view class="badge"><view class="pip"></view>LIVE</view>
      <view class="watch">{{ formatCount(featured.realtimeOnline) }} 人在看</view>
      <view class="info">
        <text class="t">{{ featured.title }}</text>
        <text class="s">{{ featured.anchorName || '主播' }}<text v-if="featured.category"> · {{ featured.category }}</text></text>
      </view>
      <view class="enter">
        <km-icon name="play" :size="9" color="#2B1A08" />
        <text>进入</text>
      </view>
    </view>

    <view class="sec-head">
      <text class="sec-title">正在直播</text>
      <text class="sec-sub">{{ rooms.length }} 个直播间</text>
      <view class="sp"></view>
      <text class="more" @click="onMore">查看全部 ›</text>
    </view>

    <view class="grid">
      <view v-for="r in filtered" :key="r.id" class="cell">
        <km-live-card :room="r" @select="open" />
      </view>
    </view>

    <view v-if="!filtered.length" class="km-empty">
      <km-icon name="broadcast" :size="34" color="rgba(247,241,232,.22)" />
      <text>{{ loading ? '正在加载直播…' : '此刻还没有人开播' }}</text>
      <text class="hint">主播开播后会第一时间出现在这里</text>
    </view>

    <km-ad-modal v-model:visible="adOpen" :ad="ad" @claimed="onAdClaimed" />
  </view>
</template>

<script setup>
import KmAdModal from '@/components/km-ad-modal.vue'
import { ref, computed, onMounted } from 'vue'
import KmNavBar from '@/components/km-nav-bar.vue'
import KmIcon from '@/components/km-icon.vue'
import KmLiveCard from '@/components/km-live-card.vue'
import { api } from '@/api'
import { coverOf, formatCount } from '@/utils/media'

const rooms = ref([])
const ad = ref(null)
const adOpen = ref(false)
const isVip = ref(false)
const cat = ref('全部')
const loading = ref(true)

const cats = computed(() => {
  const set = []
  rooms.value.forEach((r) => {
    if (r.category && set.indexOf(r.category) < 0) set.push(r.category)
  })
  return ['全部'].concat(set).slice(0, 6)
})

const filtered = computed(() =>
  cat.value === '全部' ? rooms.value : rooms.value.filter((r) => r.category === cat.value)
)

const featured = computed(() => {
  if (!rooms.value.length) return null
  return rooms.value.slice().sort((a, b) => (b.realtimeOnline || 0) - (a.realtimeOnline || 0))[0]
})

async function fallbackAd() {
  const res = await api.adsActive('HOME_BANNER').catch(() => null)
  return ((res && res.data) || [])[0] || null
}

function openAd() {
  if (!ad.value) return
  adOpen.value = true
  if (ad.value.planId) api.adReport(ad.value.planId, 'click').catch(() => {})
}

function onAdClaimed() {
  adOpen.value = false
  if (uni.getStorageSync('km_token')) api.vipStatus().then((r) => { isVip.value = !!(r && r.data && r.data.vip) }).catch(() => {})
}

function open(room) {
  if (!room) return
  uni.navigateTo({ url: `/pages/live/room?id=${room.id}` })
}

function goSearch() { uni.navigateTo({ url: '/pages/search/search' }) }
function onBell() { uni.showToast({ title: '暂无新消息', icon: 'none' }) }
function onMore() { uni.navigateTo({ url: '/pages/live/all' }) }

onMounted(async () => {
  if (uni.getStorageSync('km_token')) {
    api.vipStatus().then((r) => { isVip.value = !!(r && r.data && r.data.vip) }).catch(() => {})
  }
  const adRes = await api.adsActive('LIVE').catch(() => null)
  ad.value = ((adRes && adRes.data) || [])[0] || (await fallbackAd())
  const res = await api.liveSquare({ pageNum: 1, pageSize: 20 }).catch(() => null)
  rooms.value = (res && res.data && res.data.records) || []
  loading.value = false
})
</script>

<style scoped>
.live { padding-bottom: calc(var(--window-bottom, 0px) + 40rpx); }

.ibtn {
  width: 56rpx; height: 56rpx; border-radius: 50%;
  background: rgba(255, 255, 255, .055);
  border: 1rpx solid rgba(255, 255, 255, .07);
  display: flex; align-items: center; justify-content: center;
}

.chips { padding: 2rpx 36rpx 0; white-space: nowrap; }
.c2 {
  display: inline-flex; align-items: center;
  height: 50rpx; padding: 0 22rpx; margin-right: 12rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(255, 255, 255, .075);
  background: rgba(255, 255, 255, .025);
  font-size: 19rpx; color: rgba(247, 241, 232, .5);
}
.c2.on { background: linear-gradient(140deg, #f9c878, #e08b3e); border-color: transparent; color: #2b1a08; font-weight: 800; }

/* 精选直播 */
.feat {
  position: relative;
  margin: 22rpx 36rpx 0;
  height: 268rpx;
  border-radius: 36rpx;
  overflow: hidden;
  background: #14121a;
  border: 1rpx solid rgba(255, 255, 255, .08);
  box-shadow: 0 18rpx 36rpx rgba(0, 0, 0, .46);
}
.bg { position: absolute; inset: 0; width: 100%; height: 100%; }
.scrim {
  position: absolute; inset: 0;
  background:
    linear-gradient(180deg, rgba(8, 7, 10, .42) 0%, transparent 34%, rgba(8, 7, 10, .55) 68%, rgba(8, 7, 10, .94) 100%),
    linear-gradient(90deg, rgba(8, 7, 10, .5), transparent 62%);
}
.badge {
  position: absolute; left: 20rpx; top: 18rpx;
  height: 32rpx; padding: 0 10rpx; border-radius: 8rpx;
  background: linear-gradient(96deg, #ff8a6b, #e94c55);
  display: flex; align-items: center; gap: 6rpx;
  font-size: 14rpx; font-weight: 800; letter-spacing: 1rpx; color: #fff;
}
.pip { width: 7rpx; height: 7rpx; border-radius: 50%; background: #fff; }
.watch {
  position: absolute; right: 20rpx; top: 18rpx;
  font-size: 16rpx; color: rgba(255, 255, 255, .72);
  background: rgba(0, 0, 0, .34); border-radius: 999rpx; padding: 6rpx 14rpx;
}
.info { position: absolute; left: 24rpx; bottom: 20rpx; right: 150rpx; }
.info .t {
  display: block; font-size: 34rpx; font-weight: 900; color: #fff;
  text-shadow: 0 3rpx 16rpx rgba(0, 0, 0, .6);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.info .s { display: block; margin-top: 8rpx; font-size: 18rpx; color: rgba(255, 255, 255, .56); }
.enter {
  position: absolute; right: 22rpx; bottom: 24rpx;
  height: 52rpx; padding: 0 20rpx; border-radius: 999rpx;
  background: linear-gradient(140deg, #fbd08a, #e08b3e);
  display: flex; align-items: center; gap: 8rpx;
  font-size: 18rpx; font-weight: 800; color: #2b1a08;
  box-shadow: 0 10rpx 22rpx rgba(224, 139, 62, .34);
}

.sec-head { margin: 30rpx 36rpx 14rpx; display: flex; align-items: flex-end; gap: 10rpx; }
.sec-title { font-size: 30rpx; font-weight: 800; color: #f7f1e8; }
.sec-sub { font-size: 17rpx; color: rgba(247, 241, 232, .3); padding-bottom: 2rpx; }
.sp { flex: 1; }
.more { font-size: 18rpx; color: rgba(247, 241, 232, .34); }

.grid { display: flex; flex-wrap: wrap; padding: 0 32rpx; }
.cell { width: calc(50% - 11rpx); margin-bottom: 26rpx; }
.cell:nth-child(2n) { margin-left: 22rpx; }

.hint { font-size: 20rpx; color: rgba(247, 241, 232, .24); }

/* 广告位 */
.ad-strip {
  display: flex; align-items: center; gap: 16rpx; margin: 10rpx 24rpx 14rpx;
  padding: 14rpx; border-radius: 18rpx;
  background: linear-gradient(135deg, rgba(245,180,91,.12), rgba(255,255,255,.03));
  border: 1rpx solid rgba(245,180,91,.22);
}
.ad-cover { width: 96rpx; height: 96rpx; border-radius: 12rpx; flex: none; background: #17151b; }
.ad-copy { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6rpx; }
.ad-kicker { font-size: 18rpx; color: rgba(245,180,91,.9); font-weight: 700; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ad-text { font-size: 23rpx; color: #f3f4f6; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ad-cta { flex: none; padding: 10rpx 18rpx; border-radius: 999rpx; background: linear-gradient(135deg,#fbd08a,#de823c); color: #22150a; font-size: 20rpx; font-weight: 800; }
</style>







