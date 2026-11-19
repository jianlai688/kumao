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
        <view class="row link" @click="cancelAccount">
          <text class="k danger">注销账户</text>
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
      <text class="group-label">外观</text>
      <view class="card">
        <view class="row link" @click="themeOpen = true">
          <text class="k">主题风格</text>
          <view class="theme-value">
            <text class="theme-dot-sm" :style="{ background: theme.accent }"></text>
            <text class="v">{{ theme.name }} ›</text>
          </view>
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

    <view class="group">
      <text class="group-label">服务器</text>
      <view class="card">
        <view class="row">
          <text class="k">当前接口地址</text>
          <text class="v">{{ apiBase }}</text>
        </view>
        <view class="row server-row">
          <input
            class="server-input"
            v-model="apiBaseDraft"
            type="text"
            placeholder="https://xxx.cpolar.top/api"
            confirm-type="done"
            @confirm="saveServer"
          />
        </view>
        <view class="row link" @click="saveServer">
          <text class="k">保存并重新加载</text>
          <text class="v gold">›</text>
        </view>
        <view class="row link" v-if="apiOverride" @click="resetServer">
          <text class="k">恢复默认地址</text>
          <text class="v">›</text>
        </view>
      </view>
      <text class="server-tip">cpolar 免费版换域名后，把新地址填到这里保存即可，无需重装 App。</text>
    </view>

    <view class="logout" v-if="token" @click="logout">退出登录</view>
    <text class="copyright">酷猫音乐 · {{ version }}</text>

    <!-- 主题选择面板 -->
    <view v-if="themeOpen" class="theme-mask" @click="themeOpen = false">
      <view class="theme-sheet" @click.stop>
        <view class="sheet-head">
          <text class="sheet-title">选择主题</text>
          <text class="sheet-close" @click="themeOpen = false">完成</text>
        </view>
        <view class="theme-grid">
          <view v-for="p in presets" :key="p.key" :class="['theme-item', { on: theme.key === p.key }]" @click="chooseTheme(p)">
            <view class="theme-dot" :style="{ background: p.accent }"></view>
            <text class="theme-name">{{ p.name }}</text>
          </view>
        </view>
        <text class="sheet-sub">自定义颜色</text>
        <view class="color-grid">
          <view v-for="c in customColors" :key="c" class="color-dot" :style="{ background: c }" @click="chooseCustom(c)"></view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '@/api'
import { CUSTOM_COLORS, THEME_PRESETS, applyTheme, loadTheme, saveTheme } from '@/utils/theme'
import { buildApiBase, getApiBase, hasApiBaseOverride, setApiBase } from '@/utils/server'
import KmNavBar from '@/components/km-nav-bar.vue'

const token = ref(uni.getStorageSync('km_token') || '')
const user = ref(uni.getStorageSync('km_user') || {})
const themeOpen = ref(false)
const theme = ref(loadTheme())
const presets = THEME_PRESETS
const customColors = CUSTOM_COLORS

function chooseTheme(preset) {
  theme.value = { ...preset }
  saveTheme(theme.value)
  uni.showToast({ title: `已切换：${preset.name}`, icon: 'none' })
}

function chooseCustom(color) {
  theme.value = { key: 'custom', name: '自定义', accent: color }
  saveTheme(theme.value)
  applyTheme(theme.value)
  uni.showToast({ title: '已应用自定义主题', icon: 'none' })
}
const vipText = ref('未开通')
const quality = ref(uni.getStorageSync('km_quality') || 'standard')
const wifiOnly = ref(uni.getStorageSync('km_wifi_only') === '1')
const autoNext = ref(uni.getStorageSync('km_auto_next') !== '0')
const cacheSize = ref('计算中…')
const version = '1.5.2'
const apiBase = ref(getApiBase())
const apiBaseDraft = ref(getApiBase())
const apiOverride = ref(hasApiBaseOverride())

function reloadApp() {
  // #ifdef H5
  if (typeof location !== 'undefined' && typeof location.reload === 'function') {
    location.reload()
    return
  }
  // #endif
  uni.reLaunch({ url: '/pages/index/index' })
}

function saveServer() {
  const value = String(apiBaseDraft.value || '').trim()
  if (!value) {
    uni.showToast({ title: '请输入接口地址', icon: 'none' })
    return
  }
  if (!/^https?:\/\//i.test(value)) {
    uni.showToast({ title: '地址需以 http:// 或 https:// 开头', icon: 'none' })
    return
  }
  apiBase.value = setApiBase(value)
  apiBaseDraft.value = apiBase.value
  apiOverride.value = hasApiBaseOverride()
  uni.showToast({ title: '已保存，正在重载', icon: 'none' })
  setTimeout(reloadApp, 600)
}

function resetServer() {
  setApiBase('')
  apiBase.value = getApiBase()
  apiBaseDraft.value = buildApiBase()
  apiOverride.value = false
  uni.showToast({ title: '已恢复默认地址', icon: 'none' })
  setTimeout(reloadApp, 600)
}

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
    content: '酷猫音乐 v1.5.2\n听见每一种情绪\n\n音乐播放 · 直播互动 · 会员权益',
    showCancel: false
  })
}

function goLogin() {
  uni.navigateTo({ url: '/pages/login/login' })
}

function goProfile() {
  uni.navigateTo({ url: '/pages/mine/profile' })
}

function cancelAccount() {
  if (!token.value) return
  uni.showModal({
    title: '注销账户',
    content: '注销后账号将无法登录，收藏、歌单、购买记录等数据不可恢复。',
    confirmText: '确认注销',
    confirmColor: '#e05b5b',
    success: async (r) => {
      if (!r.confirm) return
      uni.showLoading({ title: '注销中…', mask: true })
      try {
        await api.cancelAccount()
        uni.hideLoading()
        uni.removeStorageSync('km_token')
        uni.removeStorageSync('km_user')
        uni.removeStorageSync('km_owned_songs')
        token.value = ''
        user.value = {}
        uni.showToast({ title: '账号已注销', icon: 'none' })
        setTimeout(() => uni.reLaunch({ url: '/pages/login/login' }), 800)
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: (e && e.message) || '注销失败，请重试', icon: 'none' })
      }
    }
  })
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
.v.gold { color: var(--km-accent); font-weight: 700; }
.k.danger { color: #ff9fb0; }
.opts { display: flex; gap: 8rpx; }
.opt { padding: 8rpx 16rpx; border-radius: 999rpx; font-size: 20rpx; color: rgba(247,241,232,.45); background: rgba(255,255,255,.05); }
.opt.on { background: rgba(245,180,91,.18); color: #f5cf9a; font-weight: 800; }
.server-row { padding: 14rpx 24rpx; }
.server-input { flex: 1; height: 76rpx; padding: 0 20rpx; border-radius: 12rpx; background: rgba(255,255,255,.05); font-size: 22rpx; color: #f3f4f6; }
.server-input :deep(.uni-input-input) { font-size: 22rpx; color: #f3f4f6; }
.server-tip { display: block; padding: 12rpx 8rpx 0; font-size: 19rpx; line-height: 1.6; color: rgba(247,241,232,.28); }
.logout {
  margin: 40rpx 24rpx 0; height: 92rpx; line-height: 92rpx; text-align: center;
  border-radius: 20rpx; background: rgba(251,113,133,.08); border: 1rpx solid rgba(251,113,133,.2);
  color: #ff9fb0; font-size: 25rpx; font-weight: 700;
}
.copyright { display: block; text-align: center; margin-top: 26rpx; font-size: 19rpx; color: rgba(247,241,232,.22); }
.theme-value { display: flex; align-items: center; gap: 8rpx; }
.theme-dot-sm { width: 20rpx; height: 20rpx; border-radius: 50%; }
.theme-mask { position: fixed; left: 0; right: 0; top: 0; bottom: 0; background: rgba(0,0,0,.55); z-index: 99; display: flex; align-items: flex-end; }
.theme-sheet { width: 100%; background: #131118; border-radius: 32rpx 32rpx 0 0; padding: 32rpx 28rpx 48rpx; }
.sheet-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20rpx; }
.sheet-title { font-size: 32rpx; font-weight: 800; color: #f7f1e8; }
.sheet-close { font-size: 26rpx; color: var(--km-accent); }
.theme-grid { display: flex; flex-wrap: wrap; }
.theme-item { width: calc(33.33% - 16rpx); margin: 0 8rpx 16rpx; padding: 20rpx 0; border-radius: 20rpx; background: rgba(255,255,255,.04); border: 1rpx solid transparent; display: flex; flex-direction: column; align-items: center; gap: 10rpx; }
.theme-item.on { border-color: var(--km-accent); background: rgba(255,255,255,.07); }
.theme-dot { width: 44rpx; height: 44rpx; border-radius: 50%; }
.theme-name { font-size: 22rpx; color: rgba(247,241,232,.75); }
.sheet-sub { display: block; margin: 10rpx 0 16rpx; font-size: 24rpx; color: rgba(247,241,232,.45); }
.color-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.color-dot { width: 56rpx; height: 56rpx; border-radius: 50%; }</style>




















