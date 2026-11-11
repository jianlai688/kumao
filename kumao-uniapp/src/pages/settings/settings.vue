<template>
  <view class="page">
    <km-nav-bar title="设置" />

    <view class="group">
      <text class="group-label">账号</text>
      <view class="card" v-if="token">
        <view class="row">
          <text class="k">当前账号</text>
          <text class="v">{{ user.nickname || user.username || '已登录' }}</text>
        </view>
        <view class="row">
          <text class="k">用户 ID</text>
          <text class="v">{{ user.userId || user.id || '-' }}</text>
        </view>
        <view class="row">
          <text class="k">会员状态</text>
          <text class="v gold">{{ vipText }}</text>
        </view>
        <view class="row link" @click="goProfile">
          <text class="k">个人资料</text>
          <text class="v">›</text>
        </view>
      </view>
      <view class="card" v-else>
        <view class="row link" @click="goLogin">
          <text class="k">未登录</text>
          <text class="v gold">去登录 ›</text>
        </view>
      </view>
    </view>

    <view class="group">
      <text class="group-label">播放</text>
      <view class="card">
        <view class="row">
          <text class="k">播放音质</text>
          <view class="opts">
            <text v-for="q in qualities" :key="q.value" :class="['opt', { on: quality === q.value }]" @click="setQuality(q.value)">{{ q.label }}</text>
          </view>
        </view>
        <view class="row">
          <text class="k">仅 Wi-Fi 下载</text>
          <switch :checked="wifiOnly" color="#f5b45b" style="transform: scale(.8)" @change="onWifiChange" />
        </view>
        <view class="row">
          <text class="k">自动播放下一首</text>
          <switch :checked="autoNext" color="#f5b45b" style="transform: scale(.8)" @change="onAutoChange" />
        </view>
      </view>
    </view>

    <view class="group">
      <text class="group-label">通用</text>
      <view class="card">
        <view class="row link" @click="clearCache">
          <text class="k">清理缓存</text>
          <text class="v">{{ cacheSize }}</text>
        </view>
        <view class="row link" @click="checkUpdate">
          <text class="k">检查更新</text>
          <text class="v">v{{ version }}</text>
        </view>
        <view class="row link" @click="showAbout">
          <text class="k">关于酷猫音乐</text>
          <text class="v">›</text>
        </view>
      </view>
    </view>

    <view class="logout" v-if="token" @click="logout">退出登录</view>
    <text class="copyright">酷猫音乐 · {{ version }}</text>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '@/api'
import KmNavBar from '@/components/km-nav-bar.vue'

const token = ref(uni.getStorageSync('km_token') || '')
const user = ref(uni.getStorageSync('km_user') || {})
const vipText = ref('未开通')
const quality = ref(uni.getStorageSync('km_quality') || 'standard')
const wifiOnly = ref(uni.getStorageSync('km_wifi_only') === '1')
const autoNext = ref(uni.getStorageSync('km_auto_next') !== '0')
const cacheSize = ref('计算中…')
const version = '1.4.2'

const qualities = [
  { label: '标准', value: 'standard' },
  { label: '高清', value: 'high' },
  { label: '无损', value: 'lossless' }
]

function setQuality(v) {
  quality.value = v
  uni.setStorageSync('km_quality', v)
  if (v === 'lossless' && vipText.value === '未开通') {
    uni.showToast({ title: '无损音质为会员专享', icon: 'none' })
  } else {
    uni.showToast({ title: '已切换为' + (qualities.find((q) => q.value === v) || {}).label + '音质', icon: 'none' })
  }
}

function onWifiChange(e) {
  wifiOnly.value = e.detail.value
  uni.setStorageSync('km_wifi_only', wifiOnly.value ? '1' : '0')
}

function onAutoChange(e) {
  autoNext.value = e.detail.value
  uni.setStorageSync('km_auto_next', autoNext.value ? '1' : '0')
}

function refreshCacheSize() {
  try {
    const info = uni.getStorageInfoSync()
    cacheSize.value = (info.currentSize || 0) + ' KB'
  } catch (e) {
    cacheSize.value = '未知'
  }
}

function clearCache() {
  uni.showModal({
    title: '清理缓存',
    content: '将清除本地缓存与临时文件，不会影响账号与收藏。',
    success: (res) => {
      if (!res.confirm) return
      const keep = ['km_token', 'km_user', 'km_quality', 'km_wifi_only', 'km_auto_next', 'km_current_song']
      try {
        const info = uni.getStorageInfoSync()
        ;(info.keys || []).forEach((k) => {
          if (!keep.includes(k)) uni.removeStorageSync(k)
        })
      } catch (e) { /* ignore */ }
      refreshCacheSize()
      uni.showToast({ title: '缓存已清理', icon: 'success' })
    }
  })
}

function checkUpdate() {
  uni.showModal({
    title: '检查更新',
    content: `当前已是最新版本 v${version}`,
    showCancel: false
  })
}

function showAbout() {
  uni.showModal({
    title: '关于酷猫音乐',
    content: '酷猫音乐 v1.4.2\n听见每一种情绪\n\n音乐播放 · 直播互动 · 会员权益',
    showCancel: false
  })
}

function goLogin() {
  uni.navigateTo({ url: '/pages/login/login' })
}

function goProfile() {
  uni.navigateTo({ url: '/pages/mine/profile' })
}

function logout() {
  uni.showModal({
    title: '退出登录',
    content: '确定要退出当前账号吗？',
    success: (res) => {
      if (!res.confirm) return
      uni.removeStorageSync('km_token')
      uni.removeStorageSync('km_user')
      token.value = ''
      user.value = {}
      uni.showToast({ title: '已退出登录', icon: 'none' })
      setTimeout(() => uni.navigateTo({ url: '/pages/mine/profile' }), 400)
    }
  })
}

onLoad(() => {
  token.value = uni.getStorageSync('km_token') || ''
  user.value = uni.getStorageSync('km_user') || {}
  refreshCacheSize()
  if (token.value) {
    api.vipStatus().then((r) => {
      const d = (r && r.data) || {}
      vipText.value = d.vip ? `${d.level || 'VIP'} · 剩余 ${d.remainDays || 0} 天` : '未开通'
    }).catch(() => {})
  }
})
</script>

<style scoped>
.page { min-height: 100vh; background: #08070a; padding-bottom: 60rpx; box-sizing: border-box; }
.group { padding: 18rpx 24rpx 0; }
.group-label { display: block; padding: 0 6rpx 10rpx; font-size: 20rpx; letter-spacing: 2rpx; color: rgba(247,241,232,.32); font-weight: 700; }
.card { border-radius: 20rpx; background: rgba(255,255,255,.035); border: 1rpx solid rgba(255,255,255,.055); overflow: hidden; }
.row { display: flex; align-items: center; justify-content: space-between; min-height: 92rpx; padding: 0 24rpx; border-bottom: 1rpx solid rgba(255,255,255,.045); gap: 16rpx; }
.row:last-child { border-bottom: 0; }
.row.link:active { background: rgba(255,255,255,.04); }
.k { font-size: 25rpx; color: #f3f4f6; }
.v { font-size: 23rpx; color: rgba(247,241,232,.42); max-width: 380rpx; text-align: right; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.v.gold { color: #f5b45b; font-weight: 700; }
.opts { display: flex; gap: 8rpx; }
.opt { padding: 8rpx 16rpx; border-radius: 999rpx; font-size: 20rpx; color: rgba(247,241,232,.45); background: rgba(255,255,255,.05); }
.opt.on { background: rgba(245,180,91,.18); color: #f5cf9a; font-weight: 800; }
.logout {
  margin: 40rpx 24rpx 0; height: 92rpx; line-height: 92rpx; text-align: center;
  border-radius: 20rpx; background: rgba(251,113,133,.08); border: 1rpx solid rgba(251,113,133,.2);
  color: #ff9fb0; font-size: 25rpx; font-weight: 700;
}
.copyright { display: block; text-align: center; margin-top: 26rpx; font-size: 19rpx; color: rgba(247,241,232,.22); }
</style>















