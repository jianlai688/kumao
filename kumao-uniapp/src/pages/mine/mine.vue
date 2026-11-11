<template>
  <view class="km-screen mine">
    <!-- 个人头图 -->
    <view class="profile" @click="goProfile">
      <view class="glow"></view>
      <view class="inner">
        <view class="avatar-wrap">
          <image :src="user.avatar || defaultAvatar" class="avatar" mode="aspectFill" />
          <view class="ring"></view>
        </view>
        <view class="info">
          <text class="name">{{ user.nickname || user.username || '未登录' }}</text>
          <text class="sub">{{ roleText }}</text>
          <view class="tags" v-if="token">
            <text v-if="vip" class="tag gold">黑胶 VIP</text>
            <text class="tag">Lv.1</text>
          </view>
        </view>
        <view v-if="!token" class="login-btn" @click="goLogin">登录</view>
        <view v-else class="set-btn" @click="goSettings">
          <km-icon name="list" :size="16" color="rgba(247,241,232,.7)" />
        </view>
      </view>
    </view>

    <!-- 数据概览 -->
    <view class="stats" v-if="token">
      <view class="stat">
        <text class="v">{{ likeCount }}</text>
        <text class="l">喜欢</text>
      </view>
      <view class="div"></view>
      <view class="stat">
        <text class="v">{{ collectCount }}</text>
        <text class="l">收藏</text>
      </view>
      <view class="div"></view>
      <view class="stat">
        <text class="v">{{ followCount }}</text>
        <text class="l">关注</text>
      </view>
    </view>

    <!-- 快捷入口 -->
    <view class="grid">
      <view class="cell" @click="goLikes">
        <view class="ico"><km-icon name="heart" :size="20" color="#F5B45B" /></view>
        <text class="ct">我喜欢的</text>
      </view>
      <view class="cell" @click="goVip">
        <view class="ico"><km-icon name="crown" :size="20" color="#F5B45B" /></view>
        <text class="ct">会员中心</text>
      </view>
      <view class="cell" @click="goLive">
        <view class="ico"><km-icon name="mic" :size="20" color="#F5B45B" /></view>
        <text class="ct">主播中心</text>
      </view>
      <view class="cell" @click="goIndex">
        <view class="ico"><km-icon name="search" :size="20" color="#F5B45B" /></view>
        <text class="ct">发现音乐</text>
      </view>
    </view>

    <!-- 列表 -->
    <view class="list" v-if="token">
      <view class="row" @click="goLikes">
        <km-icon name="music" :size="18" color="rgba(247,241,232,.55)" />
        <text class="rt">我喜欢的音乐</text>
        <text class="rr">{{ likeCount }} 首 ›</text>
      </view>
      <view class="row" @click="goVip">
        <km-icon name="crown" :size="18" color="rgba(247,241,232,.55)" />
        <text class="rt">会员状态</text>
        <text :class="['rr', { gold: vip }]">{{ vip ? 'VIP 会员 ›' : '未开通 ›' }}</text>
      </view>
      <view class="row" @click="goMusicianManage">
        <km-icon name="music" :size="18" color="rgba(247,241,232,.55)" />
        <text class="rt">音乐人作品管理</text>
        <text class="rr">歌曲 / MV / 专辑 ›</text>
      </view>      <view class="row" @click="goPlaylists">
        <km-icon name="list" :size="18" color="rgba(247,241,232,.55)" />
        <text class="rt">我的歌单</text>
        <text class="rr">›</text>
      </view>
      <view class="row" @click="goDownloads">
        <km-icon name="download" :size="18" color="rgba(247,241,232,.55)" />
        <text class="rt">我的下载</text>
        <text class="rr">离线收听 ›</text>
      </view>
      <view class="row" @click="goSettings">
        <km-icon name="ring" :size="18" color="rgba(247,241,232,.55)" />
        <text class="rt">设置</text>
        <text class="rr">›</text>
      </view>
      <view class="row" @click="logout">
        <km-icon name="close" :size="18" color="rgba(255,110,120,.7)" />
        <text class="rt danger">退出登录</text>
        <text class="rr"></text>
      </view>
    </view>

    <view class="scan" v-if="!token" @click="goLogin">
      <text class="scan-t">登录后同步收藏、歌单与播放记录</text>
      <text class="scan-s">一次登录，App 与网页端数据互通</text>
    </view>

    <view class="footer">酷猫音乐 · v1.0.0</view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import KmIcon from '@/components/km-icon.vue'
import { api } from '@/api'
import { avatarOf } from '@/utils/media'

const user = ref({})
const token = ref('')
const likeCount = ref(0)
const collectCount = ref(0)
const followCount = ref(0)
const vip = ref(false)
const defaultAvatar = avatarOf('')

const roleText = computed(() => {
  if (!token.value) return '登录后同步收藏与播放记录'
  return user.value.nickname ? '@' + user.value.username : '酷猫音乐用户'
})

function goLogin() { uni.navigateTo({ url: '/pages/login/login' }) }
function goLikes() { uni.navigateTo({ url: '/pages/mine/likes' }) }
function goVip() { uni.switchTab({ url: '/pages/vip/vip' }) }
function goLive() {
  if (!uni.getStorageSync('km_token')) {
    uni.navigateTo({ url: '/pages/login/login' })
    return
  }
  const roles = String((uni.getStorageSync('km_user') || {}).roles || '')
  if (!roles.split(',').includes('ANCHOR')) {
    uni.showToast({ title: '请使用主播账号登录', icon: 'none' })
    return
  }
  uni.navigateTo({ url: '/pages/live/anchor' })
}
function goIndex() { uni.switchTab({ url: '/pages/index/index' }) }
async function goMusicianManage() {
  const res = await api.musicianMine().catch(() => null)
  const m = res && res.data
  if (m && m.status === 'APPROVED') {
    uni.navigateTo({ url: '/pages/musician/manage' })
  } else {
    uni.navigateTo({ url: '/pages/musician/apply' })
  }
}

function goPlaylists() { uni.navigateTo({ url: '/pages/mine/playlists' }) }
function goDownloads() { uni.navigateTo({ url: '/pages/mine/downloads' }) }
function goProfile() {
  if (!token.value) return goLogin()
  uni.navigateTo({ url: '/pages/mine/profile' })
}

function goSettings() { uni.navigateTo({ url: '/pages/settings/settings' }) }

function logout() {
  uni.removeStorageSync('km_token')
  uni.removeStorageSync('km_user')
  token.value = ''
  user.value = {}
  uni.showToast({ title: '已退出登录' })
}

onMounted(async () => {
  token.value = uni.getStorageSync('km_token') || ''
  if (!token.value) return
  user.value = uni.getStorageSync('km_user') || {}
  const [me, likes, status] = await Promise.all([
    api.me().catch(() => null),
    api.likes({ pageNum: 1, pageSize: 1 }).catch(() => null),
    api.vipStatus().catch(() => null)
  ])
  if (me && me.data) user.value = { ...user.value, ...me.data }
  if (likes && likes.data) likeCount.value = likes.data.total || 0
  if (status && status.data) vip.value = !!status.data.vip
})
</script>

<style scoped>
.mine { padding: 18rpx 26rpx calc(var(--window-bottom, 0px) + 40rpx); }

.profile {
  position: relative;
  padding: 36rpx 30rpx;
  border-radius: 34rpx;
  overflow: hidden;
  background:
    radial-gradient(420rpx 240rpx at 6% 0%, rgba(245, 180, 91, .18), transparent 66%),
    linear-gradient(150deg, #17141b, #0b0a0e 76%);
  border: 1rpx solid rgba(255, 255, 255, .07);
}
.glow {
  position: absolute; right: -70rpx; top: -90rpx;
  width: 240rpx; height: 240rpx; border-radius: 50%;
  background: radial-gradient(circle, rgba(245, 180, 91, .22), transparent 66%);
  filter: blur(18rpx);
}
.inner { position: relative; display: flex; align-items: center; }
.avatar-wrap { position: relative; width: 124rpx; height: 124rpx; flex-shrink: 0; }
.avatar { width: 124rpx; height: 124rpx; border-radius: 50%; background: #1a1c21; border: 1rpx solid rgba(255, 255, 255, .12); }
.ring { position: absolute; inset: -8rpx; border-radius: 50%; border: 1rpx solid rgba(245, 180, 91, .26); }
.info { flex: 1; margin-left: 24rpx; display: flex; flex-direction: column; gap: 8rpx; min-width: 0; }
.name { font-size: 36rpx; font-weight: 900; letter-spacing: -.6rpx; color: #f7f1e8; }
.sub { font-size: 20rpx; color: rgba(247, 241, 232, .42); }
.tags { display: flex; gap: 10rpx; margin-top: 2rpx; }
.tag {
  font-size: 16rpx; padding: 3rpx 12rpx; border-radius: 999rpx;
  background: rgba(255, 255, 255, .07); color: rgba(247, 241, 232, .58);
}
.tag.gold { background: rgba(245, 180, 91, .14); color: #f5b45b; font-weight: 700; }
.login-btn {
  height: 56rpx; padding: 0 26rpx; border-radius: 999rpx;
  background: linear-gradient(140deg, #fbd08a, #e08b3e);
  color: #2b1a08; font-size: 22rpx; font-weight: 800;
  display: flex; align-items: center;
}
.set-btn {
  width: 56rpx; height: 56rpx; border-radius: 50%;
  background: rgba(255, 255, 255, .06);
  border: 1rpx solid rgba(255, 255, 255, .08);
  display: flex; align-items: center; justify-content: center;
}

.stats {
  display: flex; align-items: center; justify-content: space-around;
  margin-top: 20rpx; padding: 24rpx 0;
  border-radius: 28rpx; background: #121116;
  border: 1rpx solid rgba(255, 255, 255, .06);
}
.stat { display: flex; flex-direction: column; align-items: center; gap: 6rpx; }
.v { font-size: 32rpx; font-weight: 900; color: #f7f1e8; }
.l { font-size: 18rpx; color: rgba(247, 241, 232, .36); }
.div { width: 1rpx; height: 40rpx; background: rgba(255, 255, 255, .08); }

.grid { display: flex; margin-top: 24rpx; }
.cell { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 12rpx; }
.ico {
  width: 84rpx; height: 84rpx; border-radius: 26rpx;
  background: linear-gradient(160deg, rgba(255, 255, 255, .08), rgba(255, 255, 255, .03));
  border: 1rpx solid rgba(255, 255, 255, .08);
  display: flex; align-items: center; justify-content: center;
}
.ct { font-size: 19rpx; color: rgba(247, 241, 232, .58); }

.list { margin-top: 26rpx; padding: 6rpx 22rpx; border-radius: 30rpx; background: #121116; border: 1rpx solid rgba(255, 255, 255, .06); }
.row { display: flex; align-items: center; gap: 16rpx; padding: 26rpx 0; border-bottom: 1rpx solid rgba(255, 255, 255, .05); }
.row:last-child { border-bottom: none; }
.rt { flex: 1; font-size: 24rpx; color: rgba(247, 241, 232, .82); }
.rt.danger { color: #ff8a94; }
.rr { font-size: 20rpx; color: rgba(247, 241, 232, .32); }
.rr.gold { color: #f5b45b; font-weight: 700; }

.scan {
  margin-top: 28rpx; padding: 28rpx 26rpx;
  border-radius: 26rpx; border: 1rpx dashed rgba(255, 255, 255, .12);
  display: flex; flex-direction: column; gap: 8rpx;
}
.scan-t { font-size: 23rpx; color: rgba(247, 241, 232, .6); }
.scan-s { font-size: 18rpx; color: rgba(247, 241, 232, .3); }

.footer { margin-top: 34rpx; text-align: center; font-size: 17rpx; color: rgba(247, 241, 232, .18); }
</style>



