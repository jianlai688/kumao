<template>
  <view class="km-screen anchor-page">
    <km-nav-bar title="手机开播" />

    <view class="hero">
      <view class="hero-mark">📱</view>
      <view class="hero-copy">
        <text class="eyebrow">MOBILE BROADCAST</text>
        <text class="hero-title">用手机摄像头开播</text>
        <text class="hero-sub">获取本房间的 RTMP 推流地址，在手机推流 App 中粘贴后即可用摄像头直播；观众可在酷猫音乐 App 内实时观看。</text>
      </view>
    </view>

    <view v-if="!loggedIn" class="empty-card">
      <text class="empty-title">登录主播账号后开播</text>
      <text class="empty-sub">演示账号：anchor01 / 123456</text>
      <button class="primary-btn" @click="goLogin">去登录</button>
    </view>

    <view v-else-if="!isAnchor" class="empty-card">
      <text class="empty-title">当前账号不是主播</text>
      <text class="empty-sub">请使用主播账号登录，或在创作者中心申请主播身份。</text>
    </view>

    <template v-else>
      <view class="section-head">
        <text class="section-title">我的直播间</text>
        <text class="section-count">{{ rooms.length }} 个</text>
      </view>

      <view v-if="rooms.length" class="room-list">
        <view
          v-for="r in rooms"
          :key="r.id"
          :class="['room-row', { selected: selected && selected.id === r.id }]"
          @click="chooseRoom(r)"
        >
          <view class="room-main">
            <text class="room-title">{{ r.title }}</text>
            <view class="room-meta">
              <text :class="['status-pill', r.status === 'LIVING' ? 'live' : 'idle']">
                {{ statusText(r.status) }}
              </text>
              <text class="room-online">{{ r.realtimeOnline || 0 }} 人在线</text>
            </view>
          </view>
          <text class="chev">›</text>
        </view>
      </view>
      <view v-else class="empty-card small">
        <text class="empty-title">还没有直播间</text>
        <text class="empty-sub">请先在网页版主播工作台创建直播间。</text>
      </view>

      <view v-if="selected" class="push-card">
        <view class="push-head">
          <view>
            <text class="eyebrow">RTMP PUSH URL</text>
            <text class="push-title">{{ selected.title }}</text>
          </view>
          <text v-if="selected.status === 'LIVING'" class="live-dot">● 直播中</text>
        </view>

        <view v-if="!pushUrl" class="prepare-box">
          <text class="prepare-title">准备开播</text>
          <text class="prepare-sub">点击下方按钮，系统会将直播间置为直播中并生成手机推流地址。</text>
          <button class="primary-btn" :loading="loading" @click="prepare">开始直播并获取推流地址</button>
        </view>

        <template v-else>
          <view class="url-box">
            <text class="url-text">{{ pushUrl }}</text>
          </view>
          <view class="action-row">
            <button class="primary-btn half" @click="copyPush">复制推流地址</button>
            <button class="ghost-btn half" @click="openViewer">进入观众视角</button>
          </view>

          <view class="steps">
            <text class="steps-title">手机推流步骤</text>
            <view class="step"><text class="step-num">1</text><text>在手机安装支持 RTMP 的推流 App，例如 Larix Broadcaster、推流助手。</text></view>
            <view class="step"><text class="step-num">2</text><text>新建 RTMP/SRT 连接，把上面的地址完整粘贴到「URL / 推流地址」。</text></view>
            <view class="step"><text class="step-num">3</text><text>选择后置摄像头，点击开始直播；回到本 App 进入该直播间即可观看。</text></view>
          </view>
          <text class="tip">提示：手机和电脑需要同一局域网；如推流 App 要求分开填写服务器和密钥，服务器填 rtmp://192.168.20.103:1935/live，密钥填 room_11001?token=…（完整地址已复制到剪贴板）。</text>
        </template>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import KmNavBar from '@/components/km-nav-bar.vue'
import { api } from '@/api'
import { copyText } from '@/utils/clipboard'

const rooms = ref([])
const selected = ref(null)
const pushUrl = ref('')
const loading = ref(false)

const user = computed(() => uni.getStorageSync('km_user') || {})
const loggedIn = computed(() => Boolean(uni.getStorageSync('km_token')))
const isAnchor = computed(() => String(user.value.roles || '').split(',').includes('ANCHOR'))

const statusText = (status) => ({
  NOT_STARTED: '未开播',
  LIVING: '直播中',
  ENDED: '已下播',
  BANNED: '已封禁'
}[status] || status || '未开播')

function goLogin() {
  uni.navigateTo({ url: '/pages/login/login' })
}

async function loadRooms() {
  if (!loggedIn.value || !isAnchor.value) return
  const res = await api.liveMine({ pageNum: 1, pageSize: 20 }).catch(() => null)
  rooms.value = (res && res.data && res.data.records) || []
  if (selected.value) {
    selected.value = rooms.value.find((r) => String(r.id) === String(selected.value.id)) || selected.value
  }
}

async function chooseRoom(room) {
  selected.value = room
  pushUrl.value = ''
  if (room.status === 'LIVING') {
    const res = await api.livePushUrl(room.id).catch(() => null)
    pushUrl.value = (res && res.data && res.data.pushUrl) || room.pushUrl || ''
  }
}

async function prepare() {
  if (!selected.value) return
  loading.value = true
  try {
    if (selected.value.status !== 'LIVING') {
      const started = await api.liveStart(selected.value.id)
      selected.value = { ...selected.value, ...(started.data || {}), status: 'LIVING' }
    }
    const res = await api.livePushUrl(selected.value.id)
    pushUrl.value = (res.data && res.data.pushUrl) || selected.value.pushUrl || ''
    if (!pushUrl.value) uni.showToast({ title: '推流地址生成失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function copyPush() {
  const ok = await copyText(pushUrl.value)
  uni.showToast({ title: ok ? '推流地址已复制' : '复制失败，请长按选择', icon: ok ? 'success' : 'none' })
}

function openViewer() {
  if (!selected.value) return
  uni.navigateTo({ url: `/pages/live/room?id=${selected.value.id}` })
}

onShow(loadRooms)
</script>

<style scoped>
.anchor-page { min-height: 100vh; padding-bottom: 50rpx; }
.hero {
  display: flex;
  gap: 22rpx;
  margin: 26rpx 30rpx 28rpx;
  padding: 28rpx;
  border: 1rpx solid rgba(245, 180, 91, .18);
  border-radius: 28rpx;
  background:
    radial-gradient(420rpx 180rpx at 0% 0%, rgba(245, 180, 91, .15), transparent 72%),
    #121116;
}
.hero-mark {
  width: 92rpx; height: 92rpx; flex: none;
  display: flex; align-items: center; justify-content: center;
  border-radius: 24rpx; font-size: 44rpx;
  background: rgba(245, 180, 91, .12);
}
.hero-copy { display: flex; flex-direction: column; gap: 8rpx; min-width: 0; }
.eyebrow { font-size: 17rpx; font-weight: 800; letter-spacing: 2.4rpx; color: #f5b45b; }
.hero-title { font-size: 34rpx; font-weight: 900; color: #f7f1e8; letter-spacing: -0.8rpx; }
.hero-sub { font-size: 23rpx; line-height: 1.65; color: rgba(247, 241, 232, .52); }

.empty-card {
  margin: 0 30rpx;
  padding: 42rpx 30rpx;
  border: 1rpx solid rgba(255, 255, 255, .08);
  border-radius: 24rpx;
  background: #121116;
  display: flex; flex-direction: column; align-items: center; gap: 12rpx;
}
.empty-card.small { padding: 32rpx 24rpx; }
.empty-title { font-size: 28rpx; font-weight: 800; color: #f7f1e8; }
.empty-sub { font-size: 22rpx; line-height: 1.6; color: rgba(247, 241, 232, .42); text-align: center; }

.section-head { display: flex; align-items: baseline; justify-content: space-between; margin: 12rpx 34rpx 16rpx; }
.section-title { font-size: 28rpx; font-weight: 800; color: #f7f1e8; }
.section-count { font-size: 21rpx; color: rgba(247, 241, 232, .35); }

.room-list { margin: 0 30rpx; border: 1rpx solid rgba(255, 255, 255, .07); border-radius: 24rpx; overflow: hidden; background: #121116; }
.room-row { display: flex; align-items: center; gap: 16rpx; padding: 26rpx 24rpx; border-bottom: 1rpx solid rgba(255, 255, 255, .055); }
.room-row:last-child { border-bottom: 0; }
.room-row.selected { background: rgba(245, 180, 91, .08); }
.room-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 10rpx; }
.room-title { font-size: 26rpx; font-weight: 700; color: #f7f1e8; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.room-meta { display: flex; align-items: center; gap: 14rpx; }
.status-pill { padding: 4rpx 12rpx; border-radius: 999rpx; font-size: 18rpx; font-weight: 800; }
.status-pill.live { color: #ff9fb0; background: rgba(251, 113, 133, .12); }
.status-pill.idle { color: rgba(247, 241, 232, .42); background: rgba(255, 255, 255, .05); }
.room-online { font-size: 19rpx; color: rgba(247, 241, 232, .34); }
.chev { font-size: 38rpx; color: rgba(247, 241, 232, .2); }

.push-card { margin: 22rpx 30rpx 0; padding: 26rpx; border: 1rpx solid rgba(245, 180, 91, .16); border-radius: 26rpx; background: #121116; }
.push-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12rpx; margin-bottom: 18rpx; }
.push-title { display: block; margin-top: 6rpx; font-size: 29rpx; font-weight: 800; color: #f7f1e8; }
.live-dot { font-size: 19rpx; font-weight: 800; color: #ff9fb0; white-space: nowrap; }
.prepare-box { display: flex; flex-direction: column; gap: 14rpx; padding: 20rpx 0 4rpx; }
.prepare-title { font-size: 26rpx; font-weight: 800; color: #f7f1e8; }
.prepare-sub { font-size: 22rpx; line-height: 1.6; color: rgba(247, 241, 232, .45); }

.url-box { padding: 18rpx 20rpx; border-radius: 14rpx; background: #09080b; border: 1rpx solid rgba(245, 180, 91, .14); }
.url-text { font-size: 21rpx; line-height: 1.55; color: #f5cf9a; word-break: break-all; }

.action-row { display: flex; gap: 14rpx; margin-top: 18rpx; }
.primary-btn, .ghost-btn { height: 80rpx; border-radius: 16rpx; font-size: 25rpx; font-weight: 800; display: flex; align-items: center; justify-content: center; }
.primary-btn { background: linear-gradient(135deg, #fbd08a, #de823c); color: #231509; border: 0; box-shadow: 0 12rpx 34rpx rgba(222, 130, 60, .2); }
.ghost-btn { background: transparent; color: rgba(247, 241, 232, .62); border: 1rpx solid rgba(255, 255, 255, .12); }
.primary-btn.half, .ghost-btn.half { flex: 1; }
.primary-btn::after, .ghost-btn::after { border: 0; }

.steps { margin-top: 26rpx; padding-top: 22rpx; border-top: 1rpx solid rgba(255, 255, 255, .06); display: flex; flex-direction: column; gap: 14rpx; }
.steps-title { font-size: 24rpx; font-weight: 800; color: #f7f1e8; }
.step { display: flex; gap: 14rpx; font-size: 21rpx; line-height: 1.6; color: rgba(247, 241, 232, .48); }
.step-num { width: 34rpx; height: 34rpx; flex: none; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: rgba(245, 180, 91, .14); color: #f5b45b; font-size: 18rpx; font-weight: 800; }
.tip { display: block; margin-top: 22rpx; padding: 16rpx; border-radius: 12rpx; background: rgba(255, 255, 255, .035); font-size: 19rpx; line-height: 1.65; color: rgba(247, 241, 232, .38); }
</style>


