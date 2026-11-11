<template>
  <view class="page">
    <view v-if="loading" class="state-page">
      <view class="state-orbit">
        <view class="spinner large"></view>
      </view>
      <text class="state-title">正在进入直播间</text>
      <text class="state-sub">正在获取主播信息和直播状态…</text>
    </view>

    <view v-else-if="loadError" class="state-page">
      <view class="state-icon error">!</view>
      <text class="state-title">直播间加载失败</text>
      <text class="state-sub">请确认手机与电脑在同一局域网，然后重新加载。</text>
      <button class="primary-btn" @click="retryLoad">重新加载</button>
      <button class="ghost-btn" @click="goLiveSquare">返回直播广场</button>
    </view>

    <template v-else>
      <view class="video-box" @click="onStageTap">
        <image v-if="!isLiving || playerError" class="stage-poster" :src="coverUrl" mode="aspectFill" />
        <video
          v-if="isLiving"
          id="liveVideo"
          :key="videoKey"
          :src="playUrl"
          class="video"
          :poster="coverUrl"
          :controls="false"
          :show-center-play-btn="false"
          :show-fullscreen-btn="false"
          :show-play-btn="false"
          :show-progress="false"
          :enable-progress-gesture="false"
          autoplay
          :muted="muted"
          playsinline
          webkit-playsinline
          object-fit="cover"
          @play="onPlay"
          @playing="onPlay"
          @pause="onPause"
          @waiting="onWaiting"
          @canplay="onCanplay"
          @loadeddata="onCanplay"
          @timeupdate="onTimeUpdate"
          @error="onVideoError"
        />
        <view class="stage-shade" aria-hidden="true"></view>

        <view class="stage-top">
          <view class="back-btn" @click="goBack">‹</view>
          <view class="anchor-chip">
            <image :src="avatarOf(room.anchorAvatar)" class="a-avatar" mode="aspectFill" />
            <view class="anchor-copy">
              <text class="a-name">{{ room.anchorName || '主播' }}</text>
              <text class="a-sub">{{ room.category || '音乐现场' }}</text>
            </view>
            <view :class="['live-pill', { offline: !isLiving }]">
              <text class="dot"></text>{{ isLiving ? 'LIVE' : 'OFFLINE' }}
            </view>
          </view>
          <view class="stage-tools">
            <view class="tool-chip icon-btn" @click.stop="toggleMute">{{ muted ? '🔇' : '🔊' }}</view>
            <view class="tool-chip">
              <text class="conn-dot" :class="{ on: wsConnected }"></text>
              <text class="tool-ico">👁</text>
              <text>{{ formatCount(room.realtimeOnline) }}</text>
            </view>
            <view class="tool-chip icon-btn" @click="shareRoom">↗</view>
          </view>
        </view>

        <view
          v-if="isLiving && (playerLoading || playerError || userPaused)"
          class="player-state"
          @click="handlePlayerTap"
        >
          <view v-if="playerError" class="state-badge error">!</view>
          <view v-else-if="playerLoading" class="spinner large"></view>
          <view v-else class="state-badge play">▶</view>
          <text class="player-state-text">
            {{ playerError ? '直播信号异常，点击重试' : playerLoading ? '正在接入直播画面…' : '点击继续播放' }}
          </text>
        </view>

        <view v-if="isLiving && muted" class="sound-hint" @click.stop="unmute">
          <text class="sound-ico">🔈</text>
          <text>点击开启声音</text>
        </view>

        <view v-if="!isLiving" class="offline-state">
          <text class="offline-eyebrow">LIVE ROOM</text>
          <text class="offline-title">主播暂未开播</text>
          <text class="offline-sub">关注主播，开播后就能第一时间收到提醒。</text>
          <button class="follow-cta" @click="followAnchor">{{ followed ? '已关注主播' : '+ 关注主播' }}</button>
        </view>

        <view class="stage-bottom">
          <text class="room-title">{{ room.title || '直播间' }}</text>
          <view class="room-meta">
            <text class="cat">{{ room.category || '音乐现场' }}</text>
            <text v-if="room.scene === 'OUTDOOR'" class="outdoor">📍 {{ room.location || '户外直播' }}</text>
            <text :class="['signal', { off: !isLiving }]"><i></i>{{ isLiving ? '正在直播' : '未开播' }}</text>
          </view>
        </view>
      </view>

      <view class="metric-strip">
        <view class="metric">
          <text class="m-value">{{ formatCount(room.realtimeOnline) }}</text>
          <text class="m-label">在线</text>
        </view>
        <view class="metric">
          <text class="m-value">{{ formatCount(room.totalViewers) }}</text>
          <text class="m-label">累计观看</text>
        </view>
        <view class="metric">
          <text class="m-value">{{ formatCount(room.likeCount) }}</text>
          <text class="m-label">点赞</text>
        </view>
        <view class="metric">
          <text class="m-value">{{ formatCount(room.giftIncome) }}</text>
          <text class="m-label">礼物收益</text>
        </view>
      </view>

      <scroll-view scroll-y class="danmaku" :scroll-top="scrollTop" :scroll-with-animation="true">
        <view v-for="(d, i) in list" :key="d.localId || i" :class="['dm', d.type]">
          <text v-if="d.type === 'GIFT'" class="gift">{{ d.content }}</text>
          <text v-else-if="d.type === 'ENTRANCE' || d.type === 'SYSTEM'" class="sys">{{ d.content }}</text>
          <text v-else class="msg">
            <text :class="['uname', { vip: d.vip }]">{{ d.nickname }}：</text>{{ d.content }}
          </text>
        </view>
        <view v-if="!list.length" class="dm-empty">
          <text>还没有弹幕</text>
          <text class="dm-empty-sub">和主播打个招呼，或者点一首老歌。</text>
        </view>
      </scroll-view>

      <scroll-view scroll-x class="gifts" :show-scrollbar="false">
        <view class="gift-shelf-label">
          <text>送礼</text>
          <text>支持主播</text>
        </view>
        <view v-for="g in gifts" :key="g.id" class="gift-item" @click="sendGift(g)">
          <text class="g-icon">{{ g.icon || '🎁' }}</text>
          <text class="g-name">{{ g.name }}</text>
          <text class="g-price">{{ g.price }} 币</text>
        </view>
      </scroll-view>

      <view class="composer">
        <view class="like-action" :class="{ active: liking }" @click="likeRoom">
          <text class="heart">♥</text>
          <text class="like-count">{{ formatCount(room.likeCount) }}</text>
        </view>
        <view class="input-wrap">
          <input
            v-model="text"
            :placeholder="loggedIn ? '发个弹幕，和大家一起嗨…' : '登录后参与互动'"
            placeholder-class="ph"
            class="input"
            confirm-type="send"
            @confirm="send"
            @focus="ensureLogin"
          />
        </view>
        <view class="send-btn" @click="send">发送</view>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { api } from '@/api'
import { avatarOf, coverOf, formatCount } from '@/utils/media'
import { liveMediaUrl, liveWsBase } from '@/utils/live'
import { roomSharePath, share } from '@/utils/share'

const room = ref({})
const playUrl = computed(() => liveMediaUrl(room.value.pullUrl))
const coverUrl = computed(() => coverOf(room.value.coverUrl))
const isLiving = computed(() => room.value.status === 'LIVING')

const loading = ref(true)
const loadError = ref(false)
const playerLoading = ref(false)
const playerError = ref(false)
const userPaused = ref(false)
const followed = ref(false)
const liking = ref(false)
const wsConnected = ref(false)
const muted = ref(uni.getStorageSync('km_live_sound') !== '1')
const loggedIn = ref(Boolean(uni.getStorageSync('km_token')))
const videoKey = ref(0)

const list = ref([])
const gifts = ref([])
const text = ref('')
const scrollTop = ref(0)

let roomId = ''
let socket = null
let heartbeatTimer = null
let metricsTimer = null
let likeTimer = null
let readyTimer = null
let reconnectTimer = null
let destroyed = false
let likeLock = false
const pendingDanmaku = []

function pushMessage(msg) {
  list.value.push({
    ...msg,
    localId: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  })
  if (list.value.length > 200) list.value.shift()
  nextTick(() => {
    scrollTop.value = list.value.length * 100
  })
}

function applyRoomMetrics(msg) {
  if (msg.onlineCount != null) {
    room.value.realtimeOnline = Number(msg.onlineCount) || 0
    room.value.onlineCount = Number(msg.onlineCount) || 0
  }
  if (msg.totalViewers != null) room.value.totalViewers = Number(msg.totalViewers) || 0
  if (msg.peakOnline != null) room.value.peakOnline = Number(msg.peakOnline) || 0
  if (msg.likeCount != null) room.value.likeCount = Number(msg.likeCount) || 0
  if (msg.giftIncome != null) room.value.giftIncome = Number(msg.giftIncome) || 0
  if (msg.roomStatus) {
    const wasLiving = isLiving.value
    room.value.status = msg.roomStatus
    if (!wasLiving && isLiving.value) reloadVideo()
  }
  if (msg.pullUrl && msg.pullUrl !== room.value.pullUrl) {
    room.value.pullUrl = msg.pullUrl
    reloadVideo()
  }
}

function handleSocketMessage(res) {
  if (res.data === 'pong') return
  try {
    const msg = JSON.parse(res.data)
    if (msg.type === 'ROOM_METRICS') {
      applyRoomMetrics(msg)
      return
    }
    if (msg.type === 'STREAM_STATUS') {
      room.value.status = msg.content === 'ONLINE' ? 'LIVING' : 'ENDED'
      if (isLiving.value) reloadVideo()
      else {
        playerLoading.value = false
        playerError.value = false
      }
      return
    }
    if (['TEXT', 'GIFT', 'ENTRANCE', 'SYSTEM'].includes(msg.type)) {
      pushMessage(msg)
    }
  } catch (e) { /* ignore malformed message */ }
}

function handleSocketOpen() {
  wsConnected.value = true
  sendRaw('ping')
  flushDanmaku()
  if (heartbeatTimer) clearInterval(heartbeatTimer)
  heartbeatTimer = setInterval(() => sendRaw('ping'), 20000)
}

function sendRaw(payload) {
  if (!socket || !wsConnected.value) return
  try {
    socket.send(payload)
  } catch (e) { /* ignore */ }
}

function handleSocketClose() {
  wsConnected.value = false
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

function bindSocket(task) {
  task.onMessage(handleSocketMessage)
  task.onOpen(handleSocketOpen)
  task.onClose(() => {
    handleSocketClose()
    scheduleReconnect()
  })
  task.onError(() => {
    handleSocketClose()
    scheduleReconnect()
  })
}

function connectSocket() {
  if (!roomId) return
  const token = uni.getStorageSync('km_token') || ''
  const url = `${liveWsBase()}/ws/live?roomId=${roomId}&token=${token}`
  try {
    // #ifdef H5
    // H5/WebView 直接用原生 WebSocket，避免 uni 包装层在部分内核上不回触发 open 回调。
    const nativeSocket = new WebSocket(url)
    socket = nativeSocket
    nativeSocket.onopen = handleSocketOpen
    nativeSocket.onmessage = handleSocketMessage
    nativeSocket.onclose = () => {
      handleSocketClose()
      scheduleReconnect()
    }
    nativeSocket.onerror = () => {
      handleSocketClose()
      scheduleReconnect()
    }
    // #endif
    // #ifndef H5
    socket = uni.connectSocket({ url })
    bindSocket(socket)
    // #endif
  } catch (e) {
    scheduleReconnect()
  }
}

function scheduleReconnect() {
  if (destroyed || reconnectTimer) return
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    connectSocket()
  }, 4000)
}

async function refreshMetrics() {
  if (!roomId) return
  const res = await api.liveRoom(roomId).catch(() => null)
  if (!res || !res.data) return
  const wasLiving = isLiving.value
  room.value.realtimeOnline = Number(res.data.realtimeOnline) || 0
  room.value.onlineCount = Number(res.data.onlineCount) || 0
  room.value.totalViewers = Number(res.data.totalViewers) || 0
  room.value.peakOnline = Number(res.data.peakOnline) || 0
  room.value.likeCount = Number(res.data.likeCount) || 0
  room.value.giftIncome = Number(res.data.giftIncome) || 0
  room.value.status = res.data.status
  if (!wasLiving && isLiving.value) reloadVideo()
}

async function loadHistory() {
  const res = await api.danmakuHistory(roomId, { pageNum: 1, pageSize: 30 }).catch(() => null)
  const startAt = room.value.liveStartedAt ? new Date(room.value.liveStartedAt).getTime() : 0
  const records = ((res && res.data && res.data.records) || [])
    .filter((item) => !startAt || new Date(item.createTime).getTime() >= startAt - 1000)
  records.slice().reverse().forEach((item) => {
    pushMessage({
      type: item.type || 'TEXT',
      nickname: item.nickname || '系统',
      content: item.content || '',
      color: item.color,
      vip: item.vip
    })
  })
}

async function loadGifts() {
  const res = await api.gifts().catch(() => null)
  gifts.value = (res && res.data) || []
}

function armReadyTimer() {
  if (readyTimer) clearTimeout(readyTimer)
  readyTimer = setTimeout(() => {
    if (!playerError.value) playerLoading.value = false
  }, 2600)
}

function reloadVideo() {
  playerError.value = false
  playerLoading.value = true
  userPaused.value = false
  videoKey.value += 1
  armReadyTimer()
  setTimeout(() => {
    try {
      uni.createVideoContext('liveVideo').play()
    } catch (e) { /* autoplay will retry */ }
  }, 250)
}

function playVideo() {
  userPaused.value = false
  playerLoading.value = true
  armReadyTimer()
  try {
    uni.createVideoContext('liveVideo').play()
  } catch (e) {
    playerError.value = true
  }
}

function handlePlayerTap() {
  if (playerError.value) {
    reloadVideo()
  } else {
    playVideo()
  }
}

function onPlay() {
  playerLoading.value = false
  playerError.value = false
  userPaused.value = false
}

function onPause() {
  userPaused.value = true
}

function onWaiting() {
  if (isLiving.value) playerLoading.value = true
}

function onCanplay() {
  playerLoading.value = false
  playerError.value = false
}

function onTimeUpdate(event) {
  const current = Number(event && event.detail && event.detail.currentTime) || 0
  if (current > 0) {
    playerLoading.value = false
    playerError.value = false
  }
}

function onVideoError() {
  playerLoading.value = false
  playerError.value = true
}

async function loadRoom() {
  loading.value = true
  loadError.value = false
  try {
    const res = await api.liveRoom(roomId)
    if (!res || !res.data || !res.data.id) throw new Error('empty room')
    room.value = res.data
    playerLoading.value = isLiving.value
    loggedIn.value = Boolean(uni.getStorageSync('km_token'))
    followed.value = uni.getStorageSync(`km_follow_anchor_${room.value.anchorId}`) === '1'
    await Promise.all([loadGifts(), loadHistory()])
    connectSocket()
    if (metricsTimer) clearInterval(metricsTimer)
    metricsTimer = setInterval(refreshMetrics, 5000)
    if (isLiving.value) {
      await nextTick()
      setTimeout(playVideo, 350)
    }
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function retryLoad() {
  loadRoom()
}

function onStageTap() {
  if (muted.value) unmute()
}

function unmute() {
  muted.value = false
  uni.setStorageSync('km_live_sound', '1')
  applyMute(false)
}

function toggleMute() {
  muted.value = !muted.value
  uni.setStorageSync('km_live_sound', muted.value ? '0' : '1')
  applyMute(muted.value)
}

function applyMute(value) {
  try {
    const el = document.querySelector('#liveVideo video') || document.querySelector('video')
    if (el) el.muted = value
  } catch (e) { /* ignore */ }
}

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) uni.navigateBack()
  else uni.switchTab({ url: '/pages/live/live' })
}

function goLiveSquare() {
  uni.switchTab({ url: '/pages/live/live' })
}

function ensureLogin() {
  if (loggedIn.value) return
  uni.navigateTo({ url: '/pages/login/login' })
}

async function send() {
  if (!loggedIn.value) {
    ensureLogin()
    return
  }
  const content = text.value.trim()
  if (!content) return
  if (!wsConnected.value || !socket) {
    pendingDanmaku.push(content)
    text.value = ''
    uni.showToast({ title: '连接中，恢复后自动发送', icon: 'none' })
    return
  }
  socket.send(JSON.stringify({ content }))
  text.value = ''
}

function flushDanmaku() {
  while (pendingDanmaku.length && socket && wsConnected.value) {
    const content = pendingDanmaku.shift()
    try {
      socket.send(JSON.stringify({ content }))
    } catch (e) {
      pendingDanmaku.unshift(content)
      break
    }
  }
}

async function sendGift(g) {
  if (!loggedIn.value) {
    ensureLogin()
    return
  }
  try {
    await api.sendGift({ roomId: Number(roomId), giftId: g.id, count: 1 })
    uni.showToast({ title: `已送出 ${g.name}`, icon: 'success' })
  } catch (e) { /* request handles toast */ }
}

async function likeRoom() {
  if (!loggedIn.value) {
    ensureLogin()
    return
  }
  if (likeLock) return
  likeLock = true
  liking.value = true
  try {
    const res = await api.likeLive(roomId)
    room.value.likeCount = Number(res.data) || 0
  } catch (e) { /* request handles toast */ }
  if (likeTimer) clearTimeout(likeTimer)
  likeTimer = setTimeout(() => {
    liking.value = false
    likeLock = false
  }, 700)
}

function followAnchor() {
  if (!loggedIn.value) {
    ensureLogin()
    return
  }
  followed.value = !followed.value
  uni.setStorageSync(`km_follow_anchor_${room.value.anchorId}`, followed.value ? '1' : '0')
  uni.showToast({ title: followed.value ? '已关注主播' : '已取消关注', icon: 'none' })
}

async function shareRoom() {
  await share(room.value.title || '酷猫音乐直播间', roomSharePath(roomId), room.value.anchorName || '')
}

onLoad((options) => {
  roomId = String(options.id || '')
  if (!roomId || roomId === 'undefined') {
    loadError.value = true
    loading.value = false
    return
  }
  loadRoom()
})

onShow(() => {
  loggedIn.value = Boolean(uni.getStorageSync('km_token'))
  if (roomId && room.value.id) refreshMetrics()
})

onUnload(() => {
  destroyed = true
  if (heartbeatTimer) clearInterval(heartbeatTimer)
  if (metricsTimer) clearInterval(metricsTimer)
  if (likeTimer) clearTimeout(likeTimer)
  if (readyTimer) clearTimeout(readyTimer)
  if (reconnectTimer) clearTimeout(reconnectTimer)
  if (socket) {
    try { socket.close() } catch (e) { /* ignore */ }
  }
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #08070a;
  overflow: hidden;
}

/* 加载 / 错误 */
.state-page {
  height: 100vh;
  padding: 120rpx 60rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18rpx;
  box-sizing: border-box;
}
.state-title { font-size: 32rpx; font-weight: 800; color: #f7f1e8; }
.state-sub { font-size: 23rpx; line-height: 1.7; color: rgba(247, 241, 232, .4); text-align: center; max-width: 540rpx; }
.state-icon {
  width: 82rpx; height: 82rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 40rpx; font-weight: 900;
}
.state-icon.error { color: #ff9fb0; background: rgba(251, 113, 133, .12); }
.primary-btn, .ghost-btn {
  width: 360rpx; height: 82rpx; margin-top: 16rpx;
  border-radius: 16rpx; font-size: 26rpx; font-weight: 800;
  display: flex; align-items: center; justify-content: center;
}
.primary-btn { background: linear-gradient(135deg, #fbd08a, #de823c); color: #231509; border: 0; }
.ghost-btn { background: transparent; border: 1rpx solid rgba(255,255,255,.12); color: rgba(247,241,232,.6); }
.primary-btn::after, .ghost-btn::after { border: 0; }

/* 直播画面 */
.video-box { position: relative; width: 100%; height: 540rpx; background: #000; flex-shrink: 0; overflow: hidden; }
.video, .stage-poster { width: 100%; height: 100%; display: block; }
.stage-poster { position: absolute; inset: 0; filter: blur(6rpx) brightness(.56); transform: scale(1.04); }
.video { position: relative; z-index: 1; }
.stage-shade {
  position: absolute; inset: 0; z-index: 2; pointer-events: none;
  background: linear-gradient(180deg, rgba(0,0,0,.62), transparent 28%, transparent 52%, rgba(0,0,0,.84));
}
.stage-top {
  position: absolute; z-index: 3; top: 78rpx; left: 22rpx; right: 22rpx;
  display: flex; align-items: center; gap: 12rpx;
}
.back-btn {
  width: 58rpx; height: 58rpx; flex: none; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  padding-bottom: 5rpx;
  background: rgba(10,11,13,.58); border: 1rpx solid rgba(255,255,255,.14);
  color: #fff; font-size: 44rpx; line-height: 1; backdrop-filter: blur(12rpx);
}
.anchor-chip {
  display: flex; align-items: center; gap: 10rpx;
  min-width: 0; height: 64rpx; padding: 0 14rpx 0 8rpx;
  border-radius: 999rpx; background: rgba(10,11,13,.62);
  border: 1rpx solid rgba(255,255,255,.14); backdrop-filter: blur(12rpx);
}
.a-avatar { width: 46rpx; height: 46rpx; border-radius: 50%; background: #2a2d35; flex: none; }
.anchor-copy { display: flex; flex-direction: column; gap: 1rpx; min-width: 0; }
.a-name { font-size: 22rpx; font-weight: 800; color: #f3f4f6; max-width: 150rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.a-sub { font-size: 16rpx; color: rgba(247,241,232,.42); max-width: 150rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.live-pill {
  display: flex; align-items: center; gap: 5rpx; flex: none;
  height: 30rpx; padding: 0 10rpx; border-radius: 999rpx;
  background: #f5b45b; color: #07080a; font-size: 16rpx; font-weight: 900; letter-spacing: 1rpx;
}
.live-pill.offline { background: rgba(255,255,255,.16); color: rgba(255,255,255,.68); }
.dot { width: 6rpx; height: 6rpx; border-radius: 50%; background: currentColor; }
.stage-tools { margin-left: auto; display: flex; align-items: center; gap: 8rpx; }
.tool-chip {
  height: 52rpx; padding: 0 15rpx; border-radius: 999rpx;
  display: flex; align-items: center; gap: 5rpx;
  background: rgba(10,11,13,.62); border: 1rpx solid rgba(255,255,255,.14);
  color: #f3f4f6; font-size: 20rpx; backdrop-filter: blur(12rpx);
}
.tool-chip.icon-btn { width: 52rpx; justify-content: center; font-size: 30rpx; color: rgba(255,255,255,.82); }
.conn-dot { width: 7rpx; height: 7rpx; border-radius: 50%; background: #f5b45b; }
.conn-dot.on { background: #4ade80; }

.player-state {
  position: absolute; z-index: 2; inset: 0;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16rpx;
  background: rgba(3,3,5,.32); color: rgba(247,241,232,.76); font-size: 22rpx;
}
.player-state-text { font-weight: 600; }
.state-badge {
  width: 92rpx; height: 92rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: rgba(10,11,13,.58); border: 1rpx solid rgba(255,255,255,.18);
  color: #fff; font-size: 36rpx; font-weight: 900;
}
.state-badge.play { padding-left: 6rpx; background: rgba(245,180,91,.92); border-color: transparent; color: #120c07; }
.state-badge.error { background: rgba(251,113,133,.16); border-color: rgba(251,113,133,.32); color: #ff9fb0; }

.spinner {
  width: 34rpx; height: 34rpx; border-radius: 50%;
  border: 4rpx solid rgba(255,255,255,.18); border-top-color: #f5b45b;
  animation: spin .8s linear infinite;
}
.spinner.large { width: 58rpx; height: 58rpx; border-width: 5rpx; }
@keyframes spin { to { transform: rotate(360deg); } }

.offline-state {
  position: absolute; z-index: 2; inset: 0;
  padding: 150rpx 50rpx 90rpx;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10rpx;
  box-sizing: border-box;
}
.offline-eyebrow { font-size: 17rpx; font-weight: 800; letter-spacing: 3rpx; color: #f5b45b; }
.offline-title { font-size: 38rpx; font-weight: 900; color: #f7f1e8; letter-spacing: -0.8rpx; }
.offline-sub { font-size: 22rpx; line-height: 1.6; color: rgba(247,241,232,.44); text-align: center; }
.follow-cta {
  margin-top: 14rpx; height: 64rpx; padding: 0 30rpx; border-radius: 999rpx;
  background: rgba(245,180,91,.14); border: 1rpx solid rgba(245,180,91,.3);
  color: #f5cf9a; font-size: 22rpx; font-weight: 800;
}
.follow-cta::after { border: 0; }

.stage-bottom { position: absolute; z-index: 3; left: 26rpx; right: 26rpx; bottom: 22rpx; }
.room-title { display: block; font-size: 34rpx; font-weight: 900; letter-spacing: -.8rpx; color: #fff; text-shadow: 0 2rpx 14rpx rgba(0,0,0,.6); }
.room-meta { display: flex; align-items: center; gap: 10rpx; margin-top: 10rpx; }
.cat { padding: 4rpx 12rpx; border-radius: 6rpx; background: rgba(255,255,255,.15); color: #f3f4f6; font-size: 18rpx; }
.outdoor { padding: 4rpx 12rpx; border-radius: 999rpx; background: #f5b45b; color: #07080a; font-size: 18rpx; font-weight: 800; }
.signal { display: flex; align-items: center; gap: 5rpx; color: rgba(243,244,246,.66); font-size: 20rpx; }
.signal i { width: 7rpx; height: 7rpx; border-radius: 50%; background: #fb7185; box-shadow: 0 0 0 3rpx rgba(251,113,133,.18); }
.signal.off i { background: #8a8497; box-shadow: none; }

/* 数据条 */
.metric-strip {
  display: grid; grid-template-columns: repeat(4, 1fr);
  padding: 20rpx 26rpx 18rpx; margin: 0 0 2rpx;
  background: #0d0c10; border-bottom: 1rpx solid rgba(255,255,255,.055);
  flex-shrink: 0;
}
.metric { display: flex; flex-direction: column; align-items: center; gap: 4rpx; border-right: 1rpx solid rgba(255,255,255,.055); }
.metric:last-child { border-right: 0; }
.m-value { font-size: 30rpx; font-weight: 900; color: #f7f1e8; }
.m-label { font-size: 18rpx; color: rgba(247,241,232,.34); }

/* 弹幕 */
.danmaku { flex: 1; padding: 20rpx 26rpx; overflow: hidden; min-height: 0; }
.dm { padding: 7rpx 0; font-size: 24rpx; line-height: 1.55; }
.msg { color: rgba(243,244,246,.88); }
.uname { color: #8fc7ff; font-weight: 600; }
.uname.vip { color: #f7d774; }
.gift {
  display: inline-block; padding: 7rpx 14rpx; border-radius: 10rpx;
  background: rgba(247,215,116,.1); border: 1rpx solid rgba(247,215,116,.2);
  color: #f7d774; font-weight: 600; font-size: 22rpx;
}
.sys { color: rgba(243,244,246,.42); font-size: 20rpx; font-style: italic; }
.dm-empty { display: flex; flex-direction: column; align-items: center; gap: 8rpx; padding: 100rpx 0; }
.dm-empty text:first-child { font-size: 24rpx; color: rgba(247,241,232,.36); }
.dm-empty-sub { font-size: 20rpx; color: rgba(247,241,232,.22); }

/* 礼物 */
.gifts { white-space: nowrap; padding: 8rpx 22rpx 10rpx; flex-shrink: 0; }
.gift-shelf-label {
  display: inline-flex; flex-direction: column; justify-content: center;
  width: 72rpx; height: 100rpx; margin-right: 10rpx; vertical-align: top;
  font-size: 19rpx; color: #f5b45b; font-weight: 800;
}
.gift-shelf-label text:last-child { margin-top: 4rpx; font-size: 15rpx; color: rgba(247,241,232,.28); font-weight: 500; }
.gift-item {
  display: inline-flex; flex-direction: column; align-items: center; gap: 3rpx;
  width: 120rpx; padding: 12rpx 8rpx; margin-right: 10rpx;
  border-radius: 18rpx; background: rgba(255,255,255,.035);
  border: 1rpx solid rgba(255,255,255,.065); vertical-align: top;
}
.g-icon { font-size: 36rpx; }
.g-name { font-size: 20rpx; color: rgba(243,244,246,.8); }
.g-price { font-size: 18rpx; color: #f7d774; font-weight: 700; }

/* 输入 */
.composer { display: flex; align-items: center; gap: 10rpx; padding: 12rpx 22rpx 34rpx; flex-shrink: 0; }
.like-action {
  width: 84rpx; height: 72rpx; border-radius: 999rpx; flex: none;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  background: rgba(251,113,133,.09); border: 1rpx solid rgba(251,113,133,.22);
  color: #ff9fb0; transition: transform .16s ease, background .16s ease;
}
.like-action.active { transform: scale(1.12); background: rgba(251,113,133,.22); }
.heart { font-size: 24rpx; line-height: 1; }
.like-count { font-size: 15rpx; margin-top: 3rpx; color: rgba(255,159,176,.86); }
.input-wrap {
  flex: 1; height: 72rpx; padding: 0 24rpx; border-radius: 999rpx;
  background: rgba(255,255,255,.055); border: 1rpx solid rgba(255,255,255,.08);
  display: flex; align-items: center; min-width: 0;
}
.input { flex: 1; font-size: 24rpx; color: #f3f4f6; }
.ph { color: rgba(243,244,246,.28); }
.send-btn {
  height: 72rpx; line-height: 72rpx; padding: 0 28rpx; flex: none;
  border-radius: 999rpx; background: #f5b45b; color: #07080a;
  font-size: 24rpx; font-weight: 800;
}

.sound-hint {
  position: absolute; z-index: 4; left: 50%; bottom: 120rpx; transform: translateX(-50%);
  display: flex; align-items: center; gap: 8rpx;
  padding: 12rpx 24rpx; border-radius: 999rpx;
  background: rgba(0,0,0,.62); border: 1rpx solid rgba(255,255,255,.18);
  color: #fff; font-size: 21rpx;
}
.sound-ico { font-size: 24rpx; }
</style>





