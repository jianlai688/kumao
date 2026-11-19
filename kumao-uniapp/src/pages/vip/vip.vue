<template>
  <view class="page">
    <!-- 会员头图 -->
    <view class="banner">
      <view class="banner-glow"></view>
      <view class="crown-box"><text class="crown">👑</text></view>
      <text class="title">酷猫 VIP 会员</text>
      <text class="sub">{{ status.vip ? `有效期至 ${(status.expireTime || '').slice(0, 10)}` : '开通即享高清 MV 与专属特权' }}</text>
      <view v-if="status.vip" class="active-badge">● 会员生效中</view>
    </view>

    <!-- 看广告领会员 -->
    <view v-if="!status.vip" class="ad-reward" @click="openRewardAd">
      <view class="ar-ico">🎬</view>
      <view class="ar-copy">
        <text class="ar-title">看广告 · 免费领 1 天会员</text>
        <text class="ar-sub">{{ status.adRewardClaimedToday ? '今日已领取，明天 0 点刷新' : '观看 15 秒广告即可领取，每天 1 次' }}</text>
      </view>
      <view :class="['ar-btn', { done: status.adRewardClaimedToday }]">{{ status.adRewardClaimedToday ? '已领取' : '去观看' }}</view>
    </view>
    <view v-else class="ad-free">
      <text class="af-ico">✓</text>
      <text class="af-text">会员免广告已生效，全站广告已为你关闭</text>
    </view>
    <!-- 套餐 -->
    <view class="pkg-list">
      <view v-for="(p, i) in packages" :key="p.id" :class="['pkg', { recommend: i === 1 }]">
        <view v-if="i === 1" class="ribbon">推荐</view>
        <view class="pkg-left">
          <text class="pkg-name">{{ p.name }}</text>
          <text class="pkg-days">{{ p.days }} 天 · {{ p.description }}</text>
        </view>
        <view class="pkg-right">
          <view class="price-box">
            <text class="price">¥{{ p.price }}</text>
            <text v-if="p.originalPrice" class="origin">¥{{ p.originalPrice }}</text>
          </view>
          <view class="buy-btn" @click="open(p)">立即开通</view>
        </view>
      </view>
    </view>

    <!-- 权益 -->
    <text class="sec-title">会员专属权益</text>
    <view class="rights">
      <view v-for="r in rights" :key="r.t" class="right-item">
        <text class="r-ico">{{ r.i }}</text>
        <view class="r-copy">
          <text class="r-t">{{ r.t }}</text>
          <text class="r-d">{{ r.d }}</text>
        </view>
      </view>
    </view>

    <view class="tips">· 会员服务为虚拟商品，开通后不支持退款<br />· 演示环境使用模拟支付，不会产生真实扣费</view>

    <km-ad-modal v-model:visible="adOpen" :ad="currentAd" @claimed="onAdClaimed" />
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { api } from '@/api'
import KmAdModal from '@/components/km-ad-modal.vue'

const packages = ref([])
const status = reactive({ vip: false, expireTime: '', adRewardClaimedToday: false })
const adOpen = ref(false)
const currentAd = ref({})

const rights = [
  { i: '🎬', t: '高清 MV', d: '1080P 超清画质，无损音质' },
  { i: '🎨', t: '专属弹幕', d: 'VIP 弹幕颜色与入场特效' },
  { i: '🎧', t: '无损音质', d: 'FLAC 母带级音乐随心听' },
  { i: '🚫', t: '免广告', d: '全站音频、视频广告免播放' },
  { i: '⬇️', t: '离线下载', d: '歌单整张下载，随时离线听' },
  { i: '👑', t: '尊贵标识', d: '专属 VIP 身份标识展示' }
]

async function open(p) {
  uni.showModal({
    title: '模拟支付',
    content: `确认支付 ¥${p.price} 开通 ${p.name}？`,
    success: async (res) => {
      if (!res.confirm) return
      await api.openVip({ packageId: p.id, payType: 'MOCK' })
      uni.showToast({ title: '开通成功' })
      await load()
    }
  })
}

async function openRewardAd() {
  if (status.adRewardClaimedToday) {
    uni.showToast({ title: '今日已领取，明天再来', icon: 'none' })
    return
  }
  const res = await api.adsActive().catch(() => null)
  const ads = (res && res.data) || []
  if (!ads.length) {
    uni.showToast({ title: '暂无可用广告，稍后再试', icon: 'none' })
    return
  }
  currentAd.value = ads[0]
  adOpen.value = true
  if (currentAd.value.planId) api.adReport(currentAd.value.planId, 'click').catch(() => {})
}

async function onAdClaimed() {
  adOpen.value = false
  await load()
}

async function load() {
  const [p, s] = await Promise.all([api.vipPackages(), api.vipStatus().catch(() => ({ data: {} }))])
  packages.value = p.data || []
  Object.assign(status, s.data || {})
}

onMounted(load)
</script>

<style scoped>
.page { padding-bottom: 60rpx; }

/* 头图 */
.banner {
  position: relative;
  margin: 28rpx 24rpx 22rpx;
  padding: 50rpx 32rpx 40rpx;
  border-radius: 34rpx;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  background:
    radial-gradient(420rpx 260rpx at 50% 0%, rgba(247, 215, 116, .22), transparent 66%),
    linear-gradient(160deg, #2a2110, #16120a 70%);
  border: 1rpx solid rgba(247, 215, 116, .2);
}
.banner-glow {
  position: absolute;
  width: 360rpx;
  height: 360rpx;
  left: 50%;
  transform: translateX(-50%);
  top: -220rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(247, 215, 116, .28), transparent 65%);
  filter: blur(30rpx);
}
.crown-box {
  position: relative;
  width: 130rpx;
  height: 130rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, rgba(247, 215, 116, .2), rgba(247, 215, 116, .04));
  border: 1rpx solid rgba(247, 215, 116, .3);
}
.crown { font-size: 66rpx; filter: drop-shadow(0 6rpx 20rpx rgba(247, 215, 116, .45)); }
.title {
  position: relative;
  font-size: 40rpx;
  font-weight: 900;
  letter-spacing: -1rpx;
  margin-top: 18rpx;
  background: linear-gradient(96deg, #fff5cf, #f7d774);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.sub { position: relative; font-size: 23rpx; color: rgba(247, 215, 116, .6); margin-top: 10rpx; }
.active-badge {
  position: relative;
  margin-top: 18rpx;
  padding: 8rpx 24rpx;
  border-radius: 999rpx;
  background: rgba(245, 180, 91, .12);
  border: 1rpx solid rgba(245, 180, 91, .28);
  color: var(--km-accent);
  font-size: 21rpx;
  font-weight: 700;
}

/* 套餐 */
.pkg-list { padding: 0 24rpx; display: flex; flex-direction: column; gap: 18rpx; }
.pkg {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 26rpx;
  border-radius: 26rpx;
  background: #131418;
  border: 1rpx solid rgba(255, 255, 255, .07);
  overflow: hidden;
}
.pkg.recommend {
  border-color: rgba(247, 215, 116, .3);
  background: linear-gradient(150deg, rgba(247, 215, 116, .08), #131418 62%);
}
.ribbon {
  position: absolute;
  right: 0;
  top: 0;
  padding: 6rpx 20rpx;
  border-bottom-left-radius: 16rpx;
  background: linear-gradient(96deg, #f7d774, #e8bd4f);
  color: #2a2110;
  font-size: 19rpx;
  font-weight: 900;
}
.pkg-left { display: flex; flex-direction: column; gap: 8rpx; }
.pkg-name { font-size: 30rpx; font-weight: 800; }
.pkg-days { font-size: 21rpx; color: rgba(243, 244, 246, .4); }
.pkg-right { display: flex; align-items: center; gap: 20rpx; }
.price-box { display: flex; flex-direction: column; align-items: flex-end; }
.price { font-size: 38rpx; color: #f7d774; font-weight: 900; letter-spacing: -1rpx; }
.origin { font-size: 20rpx; color: rgba(243, 244, 246, .28); text-decoration: line-through; }
.buy-btn {
  padding: 14rpx 26rpx;
  border-radius: 999rpx;
  background: linear-gradient(96deg, #f7d774, #e8bd4f);
  color: #2a2110;
  font-size: 23rpx;
  font-weight: 800;
}

/* 权益 */
.sec-title { display: block; font-size: 30rpx; font-weight: 800; padding: 40rpx 24rpx 18rpx; }
.rights { display: flex; flex-wrap: wrap; padding: 0 24rpx; gap: 16rpx; }
.right-item {
  width: calc(50% - 8rpx);
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
  padding: 22rpx 18rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, .03);
  border: 1rpx solid rgba(255, 255, 255, .06);
  box-sizing: border-box;
}
.r-ico {
  width: 56rpx;
  height: 56rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  background: rgba(247, 215, 116, .1);
  border: 1rpx solid rgba(247, 215, 116, .18);
  flex-shrink: 0;
}
.r-copy { display: flex; flex-direction: column; gap: 5rpx; min-width: 0; }
.r-t { font-size: 25rpx; font-weight: 700; }
.r-d { font-size: 20rpx; color: rgba(243, 244, 246, .35); line-height: 1.5; }

.tips { padding: 36rpx 32rpx 0; font-size: 20rpx; line-height: 1.9; color: rgba(243, 244, 246, .22); }

/* 看广告领会员 */
.ad-reward {
  display: flex; align-items: center; gap: 16rpx; margin: 0 24rpx 22rpx;
  padding: 22rpx; border-radius: 20rpx;
  background: linear-gradient(135deg, rgba(245,180,91,.14), rgba(255,255,255,.03));
  border: 1rpx solid rgba(245,180,91,.26);
}
.ar-ico { width: 76rpx; height: 76rpx; border-radius: 18rpx; background: rgba(245,180,91,.16); display: flex; align-items: center; justify-content: center; font-size: 34rpx; flex: none; }
.ar-copy { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6rpx; }
.ar-title { font-size: 26rpx; font-weight: 800; color: #f7f1e8; }
.ar-sub { font-size: 20rpx; color: rgba(247,241,232,.42); }
.ar-btn {
  flex: none; padding: 12rpx 24rpx; border-radius: 999rpx;
  background: linear-gradient(135deg,#fbd08a,#de823c); color: #22150a;
  font-size: 21rpx; font-weight: 800;
}
.ar-btn.done { background: rgba(255,255,255,.08); color: rgba(247,241,232,.45); }

.ad-free {
  display: flex; align-items: center; gap: 12rpx; margin: 0 24rpx 22rpx;
  padding: 20rpx 22rpx; border-radius: 18rpx;
  background: rgba(74,222,128,.08); border: 1rpx solid rgba(74,222,128,.22);
}
.af-ico { color: #86efac; font-size: 24rpx; font-weight: 900; }
.af-text { font-size: 21rpx; color: rgba(134,239,172,.85); }
</style>





