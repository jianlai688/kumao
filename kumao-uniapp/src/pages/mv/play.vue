<template>
  <view class="km-screen play-page">
    <km-nav-bar title="MV">
      <view class="ibtn" @click="onShare">
        <km-icon name="share" :size="17" color="rgba(247,241,232,.82)" />
      </view>
    </km-nav-bar>

    <!-- 播放器 -->
    <view :class="['stage', { full: fullscreen, rotated: fullscreen && rotate }]">
      <video
        v-if="videoUrl"
        id="mvVideo"
        class="video"
        :src="videoUrl"
        :poster="cover"
        :autoplay="true"
        :controls="false"
        :show-center-play-btn="false"
        :show-fullscreen-btn="false"
        object-fit="contain"
        @error="onVideoError"
        @timeupdate="onTimeUpdate"
        @loadedmetadata="onLoadedMeta"
        @play="onPlay"
        @pause="onPause"
        @ended="onEnded"
      ></video>
      <view v-else class="placeholder">
        <image class="ph-bg" :src="cover" mode="aspectFill" />
        <view class="ph-mask"></view>
        <view class="ph-body">
          <view class="ph-play"><km-icon name="play" :size="18" color="#2B1A08" /></view>
          <text class="ph-t">{{ loading ? '正在加载视频…' : '视频暂不可播放' }}</text>
          <text class="ph-s">{{ loading ? '' : '可能是源文件缺失，换个 MV 试试' }}</text>
        </view>
      </view>

      <!-- 全屏时的退出栏：始终可点，避免退不出去 -->
      <view v-if="fullscreen" class="full-top" @click.stop>
        <view class="full-exit" @click.stop="exitFullscreen">
          <km-icon name="close" :size="16" color="#fff" />
        </view>
        <text class="full-title">{{ mv.name || 'MV' }}</text>
        <view class="full-exit-text" @click.stop="exitFullscreen">退出全屏</view>
      </view>

      <!-- 自定义控制层 -->
      <view v-if="videoUrl" class="ctl" :class="{ show: controlsVisible || !playing }" @click.stop="toggleControls">
        <view class="ctl-center" @click="togglePlay">
          <km-icon :name="playing ? 'pause' : 'play'" :size="24" color="#fff" />
        </view>
        <view class="ctl-bar">
          <view class="ctl-track" @click="onSeek">
            <view class="ctl-fill" :style="{ width: progress + '%' }"></view>
            <view class="ctl-dot" :style="{ left: progress + '%' }"></view>
          </view>
          <view class="ctl-row">
            <text class="ctl-time">{{ formatTime(currentTime) }} / {{ formatTime(duration) }}</text>
            <view class="ctl-spacer"></view>
            <view class="ctl-btn" @click="toggleMute">
              <km-icon :name="muted ? 'bell' : 'music'" :size="15" :color="muted ? 'rgba(255,255,255,.45)' : '#fff'" />
            </view>
            <view v-if="fullscreen" class="ctl-btn" @click="toggleRotate">
              <text class="ctl-fs">{{ rotate ? '竖屏' : '横屏' }}</text>
            </view>
            <view class="ctl-btn" @click="toggleFullscreen">
              <text class="ctl-fs">{{ fullscreen ? '退出' : '全屏' }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 信息 -->
    <view class="info">
      <text class="name">{{ mv.name || '未命名 MV' }}</text>
      <view class="meta">
        <text class="singer">{{ mv.singerName || '酷猫音乐' }}</text>
        <text class="dot">·</text>
        <text class="plays">{{ formatPlay(mv.playCount) }} 次播放</text>
        <text v-if="mv.quality" class="q">{{ mv.quality }}</text>
      </view>

      <view class="actions">
        <view class="act" @click="toggleLike">
          <km-icon :name="liked ? 'heart-fill' : 'heart'" :size="19" :color="liked ? '#F5B45B' : 'rgba(247,241,232,.8)'" />
          <text :class="['at', { on: liked }]">{{ liked ? '已喜欢' : '喜欢' }}</text>
        </view>
        <view class="act" @click="openComments">
          <km-icon name="comment" :size="19" color="rgba(247,241,232,.8)" />
          <text class="at">评论</text>
        </view>
        <view class="act" @click="onShare">
          <km-icon name="share" :size="19" color="rgba(247,241,232,.8)" />
          <text class="at">分享</text>
        </view>
        <view class="act" @click="onDownload">
          <km-icon name="download" :size="19" color="rgba(247,241,232,.8)" />
          <text class="at">下载</text>
        </view>
      </view>

      <view v-if="mv.description" class="desc">
        <text class="desc-t">简介</text>
        <text class="desc-c">{{ mv.description }}</text>
      </view>

      <view class="related-head">
        <text class="rh-t">更多 MV</text>
        <text class="rh-m" @click="goList">全部 ›</text>
      </view>
      <view class="related">
        <view v-for="m in related" :key="m.id" class="rcard" @click="openOther(m)">
          <view class="rph">
            <image class="rimg" :src="coverOf(m.coverUrl)" mode="aspectFill" />
            <view class="rmask"></view>
            <view class="rplay"><km-icon name="play" :size="9" color="#fff" /></view>
          </view>
          <text class="rt">{{ m.name }}</text>
          <text class="rs">{{ m.singerName }}</text>
        </view>
      </view>
    </view>

    <km-comment-sheet v-model:visible="commentOpen" :song-id="mv.songId || ''" placeholder="聊聊这支 MV…" />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onBackPress, onLoad, onUnload } from '@dcloudio/uni-app'
import { share, mvSharePath } from '@/utils/share'
import KmNavBar from '@/components/km-nav-bar.vue'
import KmIcon from '@/components/km-icon.vue'
import KmCommentSheet from '@/components/km-comment-sheet.vue'
import { api } from '@/api'
import { coverOf, fileUrl, formatPlay } from '@/utils/media'

const mv = ref({})
const currentId = ref('')
const rawUrl = ref('')
const loading = ref(true)
const liked = ref(false)
const commentOpen = ref(false)
const related = ref([])

const fullscreen = ref(false)
const rotate = ref(false)
const playing = ref(false)
const muted = ref(false)
const controlsVisible = ref(true)
const currentTime = ref(0)
const duration = ref(0)

const progress = computed(() => {
  if (!duration.value) return 0
  return Math.min(100, Math.max(0, (currentTime.value / duration.value) * 100))
})

let hideTimer = null
let ctx = null

function videoCtx() {
  if (!ctx) ctx = uni.createVideoContext('mvVideo')
  return ctx
}

function showControls() {
  controlsVisible.value = true
  if (hideTimer) clearTimeout(hideTimer)
  hideTimer = setTimeout(() => { controlsVisible.value = false }, 3200)
}

function toggleControls() {
  if (controlsVisible.value) controlsVisible.value = false
  else showControls()
}

function togglePlay() {
  const v = videoCtx()
  if (playing.value) v.pause()
  else v.play()
  showControls()
}

function onPlay() { playing.value = true; showControls() }
function onPause() { playing.value = false; controlsVisible.value = true }
function onEnded() { playing.value = false; controlsVisible.value = true }

function onLoadedMeta(e) {
  const d = e && e.detail && e.detail.duration
  if (d) duration.value = d
  showControls()
}

function onTimeUpdate(e) {
  const t = e && e.detail && e.detail.currentTime
  if (typeof t === 'number') currentTime.value = t
  const d = e && e.detail && e.detail.duration
  if (d) duration.value = d
}

function onSeek(e) {
  const x = e && e.detail && e.detail.x
  uni.createSelectorQuery().in(null).select('.stage').boundingClientRect((rect) => {
    if (!rect || x == null) return
    const ratio = Math.min(1, Math.max(0, (x - rect.left) / rect.width))
    const target = ratio * (duration.value || 0)
    videoCtx().seek(target)
    currentTime.value = target
  }).exec()
}

function toggleMute() {
  muted.value = !muted.value
  // H5 的 video 上下文不暴露音量，这里通过原生 video 元素兜底
  try {
    const el = document.querySelector('#mvVideo video') || document.querySelector('video')
    if (el) el.muted = muted.value
  } catch (err) { /* ignore */ }
  uni.showToast({ title: muted.value ? '已静音' : '已取消静音', icon: 'none' })
}

function enterFullscreen() {
  fullscreen.value = true
  // 默认铺满当前屏幕方向，用户可再点“横屏”旋转，避免一进全屏就 sideways。
  rotate.value = false
}

function exitFullscreen() {
  fullscreen.value = false
  rotate.value = false
  try {
    if (screen && screen.orientation && screen.orientation.unlock) screen.orientation.unlock()
  } catch (err) { /* ignore */ }
}

function toggleRotate() {
  rotate.value = !rotate.value
}

function toggleFullscreen() {
  if (fullscreen.value) exitFullscreen()
  else enterFullscreen()
}

function isPortrait() {
  try {
    const info = uni.getSystemInfoSync()
    return (info.windowHeight || 0) >= (info.windowWidth || 0)
  } catch (err) {
    return true
  }
}

function formatTime(sec) {
  const s = Math.max(0, Math.floor(Number(sec) || 0))
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
}

const cover = computed(() => coverOf(mv.value.coverUrl))
const videoUrl = computed(() => fileUrl(rawUrl.value))

function onVideoError() {
  rawUrl.value = ''
}

function toggleLike() {
  if (!mv.value.songId) {
    uni.showToast({ title: '该 MV 暂未关联歌曲', icon: 'none' })
    return
  }
  const id = mv.value.songId
  if (liked.value) {
    liked.value = false
    api.unlike(id).catch(() => {})
  } else {
    liked.value = true
    api.like(id).catch(() => {})
  }
}

function openComments() {
  if (!mv.value.songId) {
    uni.showToast({ title: '该 MV 还没有关联歌曲评论', icon: 'none' })
    return
  }
  commentOpen.value = true
}

async function onShare() {
  await share(mv.value.title || '酷猫音乐 MV', mvSharePath(currentId.value), mv.value.singerName || '')
}
function onDownload() {
  if (!uni.getStorageSync('km_token')) { uni.navigateTo({ url: '/pages/login/login' }); return }
  api.vipStatus().then((r) => {
    if (!(r && r.data && r.data.vip)) {
      uni.showModal({ title: '会员专享', content: '缓存 MV 需要开通酷猫会员，是否前往开通？', confirmText: '去开通', success: (m) => { if (m.confirm) uni.switchTab({ url: '/pages/vip/vip' }) } })
      return
    }
    uni.showToast({ title: '已加入缓存队列', icon: 'success' })
  }).catch(() => {})
}
function goList() { uni.navigateBack() }

function openOther(m) {
  uni.redirectTo({ url: `/pages/mv/play?id=${m.id}` })
}

onBackPress(() => {
  if (fullscreen.value) {
    exitFullscreen()
    return true
  }
  return false
})

onUnload(() => {
  if (hideTimer) clearTimeout(hideTimer)
})

onLoad(async (options) => {
  const id = options && options.id
  if (!id) return
  currentId.value = String(id)
  const [detail, list] = await Promise.all([
    api.mv(id).catch(() => null),
    api.mvs({ pageNum: 1, pageSize: 8 }).catch(() => null)
  ])
  if (detail && detail.data) mv.value = detail.data
  related.value = ((list && list.data && list.data.records) || []).filter((m) => String(m.id) !== String(id)).slice(0, 4)

  const play = await api.playMv(id).catch(() => null)
  rawUrl.value = (play && play.data && (play.data.videoUrl || play.data.url)) || mv.value.videoUrl || ''
  loading.value = false
})
</script>

<style scoped>
.play-page { padding-bottom: calc(var(--window-bottom, 0px) + 40rpx); }
.ibtn {
  width: 56rpx; height: 56rpx; border-radius: 50%;
  background: rgba(255, 255, 255, .055);
  border: 1rpx solid rgba(255, 255, 255, .07);
  display: flex; align-items: center; justify-content: center;
}

.stage { margin: 8rpx 0 0; background: #000; }
.video { width: 100%; height: 422rpx; background: #000; }
.placeholder {
  position: relative; width: 100%; height: 422rpx; overflow: hidden;
  background: #14121a;
}
.ph-bg { position: absolute; inset: 0; width: 100%; height: 100%; opacity: .5; }
.ph-mask { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(8,7,10,.35), rgba(8,7,10,.75)); }
.ph-body {
  position: absolute; inset: 0;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12rpx;
}
.ph-play {
  width: 88rpx; height: 88rpx; border-radius: 50%;
  background: linear-gradient(160deg, #fbd08a, #e08b3e);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 12rpx 30rpx rgba(224, 139, 62, .34);
}
.ph-t { font-size: 24rpx; color: rgba(247, 241, 232, .82); }
.ph-s { font-size: 19rpx; color: rgba(247, 241, 232, .34); }

.info { padding: 28rpx 36rpx 0; }
.name { font-size: 40rpx; font-weight: 900; letter-spacing: -.4rpx; color: #f7f1e8; }
.meta { display: flex; align-items: center; gap: 10rpx; margin-top: 12rpx; }
.singer { font-size: 23rpx; color: rgba(247, 241, 232, .6); }
.dot { color: rgba(247, 241, 232, .22); font-size: 20rpx; }
.plays { font-size: 21rpx; color: rgba(247, 241, 232, .38); }
.q {
  margin-left: 4rpx; font-size: 16rpx; color: #f5b45b;
  border: 1rpx solid rgba(245, 180, 91, .3); border-radius: 6rpx; padding: 1rpx 8rpx;
}

.actions { display: flex; align-items: center; gap: 54rpx; margin-top: 28rpx; }
.act { display: flex; align-items: center; gap: 10rpx; }
.at { font-size: 21rpx; color: rgba(247, 241, 232, .5); }
.at.on { color: #f5b45b; }

.desc { margin-top: 28rpx; padding: 24rpx; border-radius: 26rpx; background: #121116; border: 1rpx solid rgba(255, 255, 255, .06); }
.desc-t { font-size: 21rpx; color: rgba(247, 241, 232, .5); font-weight: 700; }
.desc-c { display: block; margin-top: 10rpx; font-size: 22rpx; line-height: 1.7; color: rgba(247, 241, 232, .62); }

.related-head { display: flex; align-items: baseline; margin-top: 32rpx; }
.rh-t { font-size: 28rpx; font-weight: 800; color: #f7f1e8; }
.rh-m { margin-left: auto; font-size: 19rpx; color: rgba(247, 241, 232, .34); }
.related { display: flex; flex-wrap: wrap; margin-top: 16rpx; }
.rcard { width: calc(50% - 10rpx); margin-bottom: 24rpx; }
.rcard:nth-child(2n) { margin-left: 20rpx; }
.rph { position: relative; width: 100%; height: 180rpx; border-radius: 22rpx; overflow: hidden; background: #1a1c21; }
.rimg { width: 100%; height: 100%; }
.rmask { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(8,7,10,.2), transparent 45%, rgba(8,7,10,.6)); }
.rplay {
  position: absolute; right: 10rpx; bottom: 10rpx;
  width: 40rpx; height: 40rpx; border-radius: 50%;
  background: rgba(255, 255, 255, .2); border: 1rpx solid rgba(255, 255, 255, .3);
  display: flex; align-items: center; justify-content: center;
}
.rt { display: block; margin-top: 12rpx; font-size: 22rpx; color: #f7f1e8; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rs { display: block; margin-top: 4rpx; font-size: 18rpx; color: rgba(247, 241, 232, .32); }

/* 自定义播放器 */
.stage { position: relative; width: 100%; height: 420rpx; background: #000; overflow: hidden; }
.stage.full {
  position: fixed; inset: 0; width: 100vw; height: 100vh; z-index: 999;
  display: flex; align-items: center; justify-content: center;
}
.stage.full.rotated {
  width: 100vh; height: 100vw; left: 50%; top: 50%;
  transform: translate(-50%, -50%) rotate(90deg);
}
.stage .video { width: 100%; height: 100%; }
.stage.full .video { object-fit: contain; }

.full-top {
  position: absolute; top: 0; left: 0; right: 0; z-index: 3;
  display: flex; align-items: center; gap: 12rpx;
  padding: 26rpx 28rpx; background: linear-gradient(180deg, rgba(0,0,0,.6), transparent);
}
.full-exit {
  width: 62rpx; height: 62rpx; border-radius: 50%; flex: none;
  background: rgba(255,255,255,.16); display: flex; align-items: center; justify-content: center;
}
.full-title { flex: 1; min-width: 0; font-size: 24rpx; color: #fff; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.full-exit-text { font-size: 22rpx; color: rgba(255,255,255,.75); padding: 8rpx 16rpx; border-radius: 999rpx; background: rgba(255,255,255,.14); }

.ctl { position: absolute; inset: 0; z-index: 2; opacity: 0; transition: opacity .2s ease; }
.ctl.show { opacity: 1; }
.ctl-center {
  position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%);
  width: 96rpx; height: 96rpx; border-radius: 50%;
  background: rgba(0,0,0,.45); border: 1rpx solid rgba(255,255,255,.24);
  display: flex; align-items: center; justify-content: center;
}
.ctl-bar { position: absolute; left: 0; right: 0; bottom: 0; padding: 20rpx 24rpx 18rpx; background: linear-gradient(0deg, rgba(0,0,0,.7), transparent); }
.ctl-track { position: relative; height: 6rpx; border-radius: 999rpx; background: rgba(255,255,255,.24); }
.ctl-fill { position: absolute; left: 0; top: 0; bottom: 0; border-radius: 999rpx; background: #f5b45b; }
.ctl-dot { position: absolute; top: 50%; width: 16rpx; height: 16rpx; margin-left: -8rpx; border-radius: 50%; background: #f5b45b; transform: translateY(-50%); }
.ctl-row { display: flex; align-items: center; gap: 10rpx; margin-top: 14rpx; }
.ctl-time { font-size: 20rpx; color: rgba(255,255,255,.72); }
.ctl-spacer { flex: 1; }
.ctl-btn { padding: 6rpx 14rpx; border-radius: 999rpx; background: rgba(255,255,255,.12); }
.ctl-fs { font-size: 20rpx; color: #fff; }
</style>











