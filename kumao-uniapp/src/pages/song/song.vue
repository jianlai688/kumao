<template>
  <view class="player">
    <!-- 背景 -->
    <image class="bg-blur" :src="cover" mode="aspectFill" />
    <image class="bg-img" :src="cover" mode="aspectFill" />
    <view class="scrim"></view>
    <view class="glow"></view>

    <!-- 顶部 -->
    <view class="topbar">
      <view class="cbtn" @click="back">
        <km-icon name="chevron-down" :size="16" color="#FFFFFF" />
      </view>
      <view class="listeners">
        <view class="pip"></view>
        <text>{{ listeners }} 人在听</text>
      </view>
      <view class="cbtn" @click="onShare">
        <km-icon name="share" :size="16" color="#FFFFFF" />
      </view>
    </view>

    <view class="dots">
      <view class="d on"></view><view class="d"></view><view class="d"></view><view class="d"></view>
    </view>

    <!-- 歌词 -->
    <view class="lyric">
      <view class="line1">
        <view class="bar"></view>
        <text class="l1">{{ currentLine }}</text>
      </view>
      <text class="l2">{{ nextLine }}</text>
    </view>

    <!-- 控制区 -->
    <view class="sheet">
      <view class="title-row">
        <text class="title">{{ song.name || '正在播放' }}</text>
        <text class="sub">{{ song.albumName ? '（' + song.albumName + '）' : '（Live 版）' }}</text>
      </view>

      <view class="meta-row">
        <text class="artist">{{ song.singerName || '未知歌手' }}</text>
        <view :class="['follow', { on: followed }]" @click="onFollow">{{ followed ? '已关注' : '关注' }}</view>
        <view class="divider"></view>
        <view class="chip" @click="qualityOpen = true">{{ qualityLabel }}</view>
        <view class="chip" @click="speedOpen = true">{{ rateLabel }}</view>
        <view :class="['chip', { on: playerState.sleepAt > 0 }]" @click="sleepOpen = true">{{ sleepLabel }}</view>
      </view>

      <!-- 单曲付费 -->
      <view v-if="locked" class="paywall">
        <view class="pw-copy">
          <text class="pw-tag">单曲付费</text>
          <text class="pw-title">购买后即可完整播放</text>
          <text class="pw-sub">¥{{ priceText }} · 一次购买，永久可听</text>
        </view>
        <view :class="['pw-btn', { busy: purchasing }]" @click="buySong">
          {{ purchasing ? '购买中…' : '¥' + priceText + ' 购买单曲' }}
        </view>
      </view>

      <view class="actions">
        <view class="act" @click="openSimilar">
          <km-icon name="search" :size="19" color="rgba(247,241,232,.8)" />
        </view>
        <view class="act wrap" @click="onDownload">
          <km-icon name="download" :size="19" color="rgba(247,241,232,.8)" />
          <text class="vip">VIP</text>
        </view>
        <view class="act" @click="onRing">
          <km-icon name="bell" :size="19" color="rgba(247,241,232,.8)" />
        </view>
        <view class="act" @click="toggleLike">
          <km-icon :name="liked ? 'heart-fill' : 'heart'" :size="19" :color="liked ? '#F5B45B' : 'rgba(247,241,232,.8)'" />
          <text class="cnt">{{ likeText }}</text>
        </view>
        <view class="act" @click="onComment">
          <km-icon name="comment" :size="19" color="rgba(247,241,232,.8)" />
          <text class="cnt">{{ commentText }}</text>
        </view>
        <view class="act" @click="onMore">
          <km-icon name="more" :size="18" color="rgba(247,241,232,.8)" />
        </view>
      </view>

      <!-- 播放页广告位（非会员） -->
      <view v-if="playAd && !vipUser" class="play-ad" @click="openPlayAd">
        <image class="pa-cover" :src="coverOf(playAd.coverUrl)" mode="aspectFill" />
        <view class="pa-copy">
          <text class="pa-tag">广告 · 播放页</text>
          <text class="pa-name">{{ playAd.name }}</text>
        </view>
        <view class="pa-cta">看广告领会员</view>
      </view>
      <view class="seek">
        <view class="track" @touchend="onSeek" @click="onSeek">
          <view class="fill" :style="{ width: percent + '%' }"><view class="knob"></view></view>
        </view>
        <view class="times">
          <text>{{ formatTime(playerState.current) }}</text>
          <text>{{ formatTime(playerState.duration || song.duration || 0) }}</text>
        </view>
      </view>

      <view class="controls">
        <view class="side" @click="onShuffle">
          <km-icon name="shuffle" :size="20" :color="playerState.shuffle ? '#F5B45B' : 'rgba(247,241,232,.72)'" />
        </view>
        <view class="side" @click="prev">
          <km-icon name="prev" :size="22" color="rgba(247,241,232,.86)" />
        </view>
        <view class="play-btn" @click="onToggle">
          <km-icon :name="playerState.playing ? 'pause' : 'play'" :size="22" color="#2B1A08" />
        </view>
        <view class="side" @click="next">
          <km-icon name="next" :size="22" color="rgba(247,241,232,.86)" />
        </view>
        <view class="side" @click="onList">
          <km-icon name="list" :size="20" color="rgba(247,241,232,.72)" />
        </view>
      </view>
    </view>

    <!-- 音质 -->
    <view v-if="qualityOpen" class="mask" @click="qualityOpen = false">
      <view class="panel" @click.stop>
        <text class="panel-title">音质选择</text>
        <view v-for="q in qualityOptions" :key="q.value" class="panel-row" @click="chooseQuality(q)">
          <view class="panel-left">
            <text class="panel-name">{{ q.label }}</text>
            <text class="panel-desc">{{ q.desc }}</text>
          </view>
          <km-icon v-if="quality === q.value" name="check" :size="18" color="#F5B45B" />
          <text v-else-if="q.vip" class="panel-vip">VIP</text>
        </view>
        <view class="panel-cancel" @click="qualityOpen = false">取消</view>
      </view>
    </view>

    <!-- 倍速 -->
    <view v-if="speedOpen" class="mask" @click="speedOpen = false">
      <view class="panel" @click.stop>
        <text class="panel-title">播放速度</text>
        <view class="speed-grid">
          <view v-for="r in speedOptions" :key="r" :class="['speed-item', { on: Math.abs(playerState.rate - r) < 0.01 }]" @click="chooseSpeed(r)">
            {{ r.toFixed(2).replace(/0+$/, '').replace(/\.$/, '') }}x
          </view>
        </view>
        <view class="panel-cancel" @click="speedOpen = false">取消</view>
      </view>
    </view>

    <!-- 定时关闭 -->
    <view v-if="sleepOpen" class="mask" @click="sleepOpen = false">
      <view class="panel" @click.stop>
        <text class="panel-title">定时关闭</text>
        <view v-for="s in sleepOptions" :key="s.value" class="panel-row" @click="chooseSleep(s.value)">
          <text class="panel-name">{{ s.label }}</text>
          <km-icon v-if="sleepPick === s.value" name="check" :size="18" color="#F5B45B" />
        </view>
        <view class="panel-cancel" @click="sleepOpen = false">取消</view>
      </view>
    </view>

    <!-- 相似歌曲 -->
    <view v-if="similarOpen" class="mask" @click="similarOpen = false">
      <view class="panel tall" @click.stop>
        <text class="panel-title">相似歌曲 · {{ song.singerName || '' }}</text>
        <view v-if="similarLoading" class="panel-loading">正在匹配…</view>
        <scroll-view v-else scroll-y class="similar-list">
          <view v-for="(s, i) in similarSongs" :key="s.id" class="panel-row" @click="playSimilar(s)">
            <image class="s-cover" :src="coverOf(s.coverUrl)" mode="aspectFill" />
            <view class="panel-left">
              <text class="panel-name">{{ s.name }}</text>
              <text class="panel-desc">{{ s.singerName }} · {{ formatPlay(s.playCount) }} 次播放</text>
            </view>
          </view>
          <view v-if="!similarSongs.length" class="panel-loading">没有找到相似歌曲</view>
        </scroll-view>
        <view class="panel-cancel" @click="similarOpen = false">关闭</view>
      </view>
    </view>

    <!-- 更多 -->
    <view v-if="moreOpen" class="mask" @click="moreOpen = false">
      <view class="panel" @click.stop>
        <text class="panel-title">{{ song.name }}</text>
        <view class="panel-row" @click="openAddToPlaylist">
          <text class="panel-name">添加到歌单</text>
        </view>
        <view class="panel-row" @click="addToNext">
          <text class="panel-name">下一首播放</text>
        </view>
        <view class="panel-row" @click="openSinger">
          <text class="panel-name">查看歌手</text>
        </view>
        <view class="panel-row" @click="onShare">
          <text class="panel-name">分享给好友</text>
        </view>
        <view class="panel-cancel" @click="moreOpen = false">取消</view>
      </view>
    </view>

    <!-- 选择歌单 -->
    <view v-if="playlistOpen" class="mask" @click="playlistOpen = false">
      <view class="panel tall" @click.stop>
        <text class="panel-title">选择歌单</text>
        <view v-if="playlistLoading" class="panel-loading">正在加载歌单…</view>
        <scroll-view v-else scroll-y class="similar-list">
          <view v-for="p in myPlaylists" :key="p.id" class="panel-row" @click="addToPlaylist(p)">
            <image class="s-cover" :src="coverOf(p.coverUrl)" mode="aspectFill" />
            <view class="panel-left">
              <text class="panel-name">{{ p.name }}</text>
              <text class="panel-desc">{{ p.songCount || 0 }} 首</text>
            </view>
          </view>
          <view v-if="!myPlaylists.length" class="panel-loading">还没有歌单，先去「我的歌单」创建一个</view>
        </scroll-view>
        <view class="panel-cancel" @click="playlistOpen = false">关闭</view>
      </view>
    </view>
    <km-ad-modal v-model:visible="playAdOpen" :ad="playAd" @claimed="onPlayAdClaimed" />

    <km-comment-sheet
      v-model:visible="commentOpen"
      :song-id="song.songId || song.id || ''"
      placeholder="聊聊这首歌…"
      @count="commentCount = $event"
    />
    <km-queue-sheet v-model:visible="queueOpen" />
  </view>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { share, songSharePath } from '@/utils/share'
import { downloadSong, formatSize, isDownloaded } from '@/utils/download'
import KmIcon from '@/components/km-icon.vue'
import KmCommentSheet from '@/components/km-comment-sheet.vue'
import KmQueueSheet from '@/components/km-queue-sheet.vue'
import { api } from '@/api'
import { coverOf, formatCount, formatPlay } from '@/utils/media'
import { usePlayer } from '@/utils/player'
import KmAdModal from '@/components/km-ad-modal.vue'

const { state: playerState, play, toggle, seek, next, prev, formatTime, toggleShuffle, setRate, setSleep, playNext } = usePlayer()

const song = ref({})
const lyric = ref([])
const activeIndex = ref(0)
const liked = ref(false)
const followed = ref(false)
const vipUser = ref(false)
const payInfo = ref({ price: 0, owned: true })
const purchasing = ref(false)
const playAd = ref(null)
const playAdOpen = ref(false)
const commentCount = ref(0)
const commentOpen = ref(false)
const queueOpen = ref(false)
const qualityOpen = ref(false)
const speedOpen = ref(false)
const sleepOpen = ref(false)
const similarOpen = ref(false)
const moreOpen = ref(false)
const playlistOpen = ref(false)
const similarLoading = ref(false)
const playlistLoading = ref(false)
const similarSongs = ref([])
const myPlaylists = ref([])
const sleepPick = ref(0)

const quality = ref(uni.getStorageSync('km_quality') || 'standard')
const qualityOptions = [
  { label: '标准', value: 'standard', desc: '128kbps · 省流量' },
  { label: '高清', value: 'high', desc: '320kbps · 推荐' },
  { label: '无损', value: 'lossless', desc: 'FLAC · 会员专享', vip: true }
]
const speedOptions = [0.75, 1, 1.25, 1.5, 2]
const sleepOptions = [
  { label: '不开启', value: 0 },
  { label: '15 分钟后', value: 15 },
  { label: '30 分钟后', value: 30 },
  { label: '60 分钟后', value: 60 }
]

const qualityLabel = computed(() => (qualityOptions.find((q) => q.value === quality.value) || qualityOptions[0]).label)
const rateLabel = computed(() => (playerState.rate === 1 ? '倍速' : playerState.rate + 'x'))
const sleepLabel = computed(() => {
  if (!playerState.sleepAt) return '定时'
  const left = Math.max(0, Math.round((playerState.sleepAt - Date.now()) / 60000))
  return left > 0 ? left + ' 分钟' : '即将关闭'
})

const cover = computed(() => coverOf(song.value.coverUrl))
const percent = computed(() => {
  const d = playerState.duration || song.value.duration || 0
  if (!d) return 0
  return Math.min(100, Math.max(0, (playerState.current / d) * 100))
})
const listeners = computed(() => {
  const n = Number(song.value.playCount) || 0
  if (!n) return '1 万'
  return formatCount(Math.max(1, Math.round(n / 800)))
})
const likeText = computed(() => formatPlay(song.value.likeCount || song.value.playCount / 50))
const commentText = computed(() => (commentCount.value ? formatCount(commentCount.value) : '—'))
const locked = computed(() => Number(payInfo.value.price) > 0 && !payInfo.value.owned)
const priceText = computed(() => (Number(payInfo.value.price || 0) / 100).toFixed(2))

const currentLine = computed(() => {
  const l = lyric.value[activeIndex.value]
  return l && l.text ? l.text : song.value.name || '酷猫音乐'
})
const nextLine = computed(() => {
  const l = lyric.value[activeIndex.value + 1]
  return l && l.text ? l.text : ''
})

watch(
  () => playerState.current,
  (t) => {
    if (!lyric.value.length) return
    const ms = t * 1000
    let idx = 0
    for (let i = 0; i < lyric.value.length; i++) {
      if ((lyric.value[i].time || 0) <= ms) idx = i
      else break
    }
    activeIndex.value = idx
  }
)

function onToggle() {
  if (locked.value) {
    uni.showToast({ title: '购买单曲后才能播放', icon: 'none' })
    return
  }
  toggle()
}

function markOwned(id) {
  const list = (uni.getStorageSync('km_owned_songs') || []).map(String)
  if (!list.includes(String(id))) {
    list.push(String(id))
    uni.setStorageSync('km_owned_songs', list)
  }
}

async function buySong() {
  if (purchasing.value) return
  if (!uni.getStorageSync('km_token')) {
    uni.navigateTo({ url: '/pages/login/login' })
    return
  }
  purchasing.value = true
  try {
    const res = await api.purchaseSong(song.value.songId || song.value.id)
    if (!res || res.code !== 200) throw new Error('购买失败')
    payInfo.value = { ...payInfo.value, owned: true }
    markOwned(song.value.songId || song.value.id)
    uni.showToast({ title: '购买成功，已解锁', icon: 'none' })
    const queue = uni.getStorageSync('km_queue')
    play(song.value, Array.isArray(queue) && queue.length ? queue : null)
  } catch (e) {
    uni.showToast({ title: (e && e.message) || '购买失败，请重试', icon: 'none' })
  } finally {
    purchasing.value = false
  }
}

function back() {
  const pages = getCurrentPages()
  if (pages.length > 1) uni.navigateBack()
  else uni.switchTab({ url: '/pages/index/index' })
}

function toggleLike() {
  const id = song.value.songId || song.value.id
  if (!id) return
  if (liked.value) {
    liked.value = false
    api.unlike(id).catch(() => {})
  } else {
    liked.value = true
    api.like(id).catch(() => {})
  }
}

function onFollow() {
  const key = `km_follow_singer_${song.value.singerId || song.value.singerName || 'unknown'}`
  followed.value = !followed.value
  uni.setStorageSync(key, followed.value ? '1' : '0')
  uni.showToast({ title: followed.value ? '已关注该歌手' : '已取消关注', icon: 'none' })
}

function onSeek(e) {
  const touch = (e.changedTouches && e.changedTouches[0]) || (e.touches && e.touches[0]) || null
  const x = touch ? touch.clientX : e.detail && e.detail.x
  if (x == null) return
  const info = uni.getSystemInfoSync()
  const left = 24
  const width = info.windowWidth - left * 2
  const ratio = Math.min(1, Math.max(0, (x - left) / width))
  seek(ratio * (playerState.duration || song.value.duration || 0))
}

async function openSimilar() {
  similarOpen.value = true
  if (similarSongs.value.length) return
  similarLoading.value = true
  const keyword = song.value.singerName || song.value.name || ''
  const res = await api.search(keyword).catch(() => null)
  const data = (res && res.data) || {}
  const list = data.songs || data.records || (Array.isArray(data) ? data : [])
  similarSongs.value = (list || []).filter((s) => String(s.id) !== String(song.value.id)).slice(0, 20)
  if (!similarSongs.value.length) {
    const hot = await api.hot(20).catch(() => null)
    similarSongs.value = ((hot && hot.data) || []).filter((s) => String(s.id) !== String(song.value.id)).slice(0, 20)
  }
  similarLoading.value = false
}
async function onDownload() {
  if (!uni.getStorageSync('km_token')) {
    uni.navigateTo({ url: '/pages/login/login' })
    return
  }
  const songId = song.value.songId || song.value.id
  const res = await api.vipStatus().catch(() => null)
  const vip = !!(res && res.data && res.data.vip)
  if (!vip) {
    uni.showModal({
      title: '会员专享',
      content: '下载歌曲需要开通酷猫会员，是否前往开通？',
      confirmText: '去开通',
      success: (m) => { if (m.confirm) uni.switchTab({ url: '/pages/vip/vip' }) }
    })
    return
  }
  if (await isDownloaded(songId)) {
    uni.showModal({
      title: '已下载',
      content: '这首歌已经在本地，是否前往“我的下载”查看？',
      confirmText: '去看看',
      success: (m) => { if (m.confirm) uni.navigateTo({ url: '/pages/mine/downloads' }) }
    })
    return
  }
  let last = -1
  uni.showLoading({ title: '下载中 0%', mask: true })
  try {
    const record = await downloadSong({
      ...song.value,
      id: songId,
      name: song.value.name,
      singer: song.value.singerName,
      audioUrl: song.value.audioUrl
    }, (p) => {
      if (p !== last) {
        last = p
        uni.showLoading({ title: `下载中 ${p}%`, mask: true })
      }
    })
    uni.hideLoading()
    uni.showToast({ title: `已下载 ${formatSize(record.size)}`, icon: 'success' })
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '下载失败', icon: 'none' })
  }
}

async function onRing() {
  const songId = song.value.songId || song.value.id
  if (!(await isDownloaded(songId))) {
    uni.showModal({
      title: '设为铃声',
      content: '需要先下载这首歌到本地，是否立即下载？',
      confirmText: '下载',
      success: async (m) => {
        if (!m.confirm) return
        try {
          await downloadSong({ ...song.value, id: songId, name: song.value.name, singer: song.value.singerName, audioUrl: song.value.audioUrl })
          uni.showModal({ title: '已保存到本地', content: '文件已保存到「我的下载」，请在手机 设置 → 声音与振动 → 铃声 中选择该文件。', showCancel: false })
        } catch (e) {
          uni.showToast({ title: e.message || '下载失败', icon: 'none' })
        }
      }
    })
    return
  }
  uni.showModal({ title: '已保存到本地', content: '文件已在「我的下载」中，请在手机 设置 → 声音与振动 → 铃声 里选择它。', showCancel: false })
}
function openPlayAd() {
  if (!playAd.value) return
  playAdOpen.value = true
  if (playAd.value.planId) api.adReport(playAd.value.planId, 'click').catch(() => {})
}

function onPlayAdClaimed() {
  playAdOpen.value = false
  api.vipStatus().then((r) => { vipUser.value = !!(r && r.data && r.data.vip) }).catch(() => {})
  api.adsActive('PLAYER').then((r) => {
    playAd.value = ((r && r.data) || [])[0] || null
  }).catch(() => {})
}

function onComment() { commentOpen.value = true }
function onMore() { moreOpen.value = true }

function onShuffle() {
  const on = toggleShuffle()
  uni.showToast({ title: on ? '随机播放已开启' : '顺序播放', icon: 'none' })
}

async function openAddToPlaylist() {
  moreOpen.value = false
  playlistOpen.value = true
  if (myPlaylists.value.length) return
  playlistLoading.value = true
  const res = await api.playlistsMine({ pageNum: 1, pageSize: 50 }).catch(() => null)
  myPlaylists.value = ((res && res.data && res.data.records) || [])
  playlistLoading.value = false
}

async function addToPlaylist(p) {
  const songId = song.value.songId || song.value.id
  try {
    await api.addSongToPlaylist(p.id, songId)
    playlistOpen.value = false
    uni.showToast({ title: `已添加到「${p.name}」`, icon: 'success' })
  } catch (e) { /* toast handled */ }
}

function addToNext() {
  moreOpen.value = false
  playNext(song.value)
  uni.showToast({ title: '已设为下一首播放', icon: 'none' })
}

function openSinger() {
  moreOpen.value = false
  if (song.value.singerId) {
      uni.navigateTo({ url: `/pages/singer/singer?id=${song.value.singerId}` })
  } else {
      uni.navigateTo({ url: `/pages/search/search?keyword=${encodeURIComponent(song.value.singerName || '')}` })
  }
}

function chooseQuality(q) {
  if (q.vip && !vipUser.value) {
    uni.showModal({ title: '会员专享', content: '无损音质需要酷猫会员，是否前往开通？', confirmText: '去开通', success: (m) => { if (m.confirm) uni.switchTab({ url: '/pages/vip/vip' }) } })
    return
  }
  quality.value = q.value
  uni.setStorageSync('km_quality', q.value)
  qualityOpen.value = false
  uni.showToast({ title: '已切换为' + q.label + '音质', icon: 'none' })
}

function chooseSpeed(r) {
  setRate(r)
  speedOpen.value = false
  uni.showToast({ title: r === 1 ? '已恢复 1x 速度' : `已切换为 ${r}x`, icon: 'none' })
}

function chooseSleep(v) {
  sleepPick.value = v
  setSleep(v)
  sleepOpen.value = false
  uni.showToast({ title: v ? `${v} 分钟后自动停止播放` : '已关闭定时', icon: 'none' })
}

function playSimilar(s) {
  similarOpen.value = false
  play(s, similarSongs.value)
  uni.navigateTo({ url: `/pages/song/song?id=${s.id}` })
}
async function onShare() {
  const sid = song.value.songId || song.value.id || ''
  await share(song.value.name || '酷猫音乐', songSharePath(sid), song.value.singerName || '')
}

function onList() { queueOpen.value = true }

onLoad(async (options) => {
  const id = options && options.id
  if (!id) return
  const cached = uni.getStorageSync('km_current_song')
  if (cached && (cached.songId || cached.id) == id) song.value = cached

  const [detail, lyr, pay] = await Promise.all([
    api.song(id).catch(() => null),
    api.lyric(id).catch(() => null),
    api.songPurchaseStatus(id).catch(() => null)
  ])
  if (detail && detail.data) song.value = detail.data
  if (pay && pay.data) payInfo.value = { price: Number(pay.data.price || 0), owned: !!pay.data.owned }
  if (payInfo.value.owned && payInfo.value.price > 0) markOwned(id)
  const rawLines = (lyr && lyr.data) || []
  const real = rawLines.filter((l) => l && l.text && !/^(作词|作曲|编曲|制作人|混音|录音|监制|出品|母带)[:：]/.test(l.text))
  lyric.value = real.length ? real : rawLines

  const current = playerState.song
  const same = current && (current.songId || current.id) == id
  if ((!same || !playerState.playing) && !locked.value) {
    const queue = uni.getStorageSync('km_queue')
    play(song.value, Array.isArray(queue) && queue.length ? queue : null)
  }

  if (options && options.sheet === 'comment') commentOpen.value = true
  if (options && options.sheet === 'list') queueOpen.value = true

  followed.value = uni.getStorageSync(`km_follow_singer_${song.value.singerId || song.value.singerName || 'unknown'}`) === '1'
  api.vipStatus().then((r) => { vipUser.value = !!(r && r.data && r.data.vip) }).catch(() => {})
  api.adsActive('PLAYER').then((r) => {
    playAd.value = ((r && r.data) || [])[0] || null
  }).catch(() => {})
  api.likeStatus(id).then((r) => { liked.value = !!(r && r.data && (r.data.liked || r.data === true)) }).catch(() => {})
  api.commentCount(id).then((r) => {
    const d = r && r.data
    const n = typeof d === 'number' ? d : d && (d.count || d.total)
    commentCount.value = Number(n) || 0
  }).catch(() => {})
})
</script>

<style scoped>
.player {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background: #08070a;
  display: flex;
  flex-direction: column;
}
.bg-blur {
  position: absolute;
  inset: -60rpx;
  width: calc(100% + 120rpx);
  height: calc(100% + 120rpx);
  filter: blur(60rpx) brightness(.55);
  transform: scale(1.15);
  opacity: .9;
}
.bg-img { position: absolute; inset: 0; width: 100%; height: 100%; opacity: .96; }
.scrim {
  position: absolute; inset: 0;
  background:
    linear-gradient(180deg, rgba(6, 5, 7, .78) 0%, rgba(6, 5, 7, .3) 12%, rgba(6, 5, 7, .02) 28%, rgba(6, 5, 7, 0) 40%, rgba(6, 5, 7, .26) 52%, rgba(7, 6, 9, .8) 66%, rgba(8, 7, 10, .97) 78%, #08070a 90%);
}
.glow {
  position: absolute; inset: 0; pointer-events: none;
  background:
    radial-gradient(58% 26% at 8% 86%, rgba(245, 180, 91, .16), transparent 70%),
    radial-gradient(50% 22% at 92% 96%, rgba(238, 127, 147, .10), transparent 72%);
}

/* 顶部 */
.topbar {
  position: relative;
  z-index: 10;
  padding: calc(var(--status-bar-height, 0px) + 12rpx) 34rpx 0;
  height: calc(var(--status-bar-height, 0px) + 84rpx);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.cbtn {
  width: 64rpx; height: 64rpx; border-radius: 50%;
  background: rgba(255, 255, 255, .1);
  border: 1rpx solid rgba(255, 255, 255, .13);
  display: flex; align-items: center; justify-content: center;
}
.listeners {
  height: 56rpx; padding: 0 22rpx; border-radius: 999rpx;
  background: rgba(255, 255, 255, .1);
  border: 1rpx solid rgba(255, 255, 255, .12);
  display: flex; align-items: center; gap: 12rpx;
  font-size: 20rpx; color: rgba(255, 255, 255, .9);
}
.pip { width: 10rpx; height: 10rpx; border-radius: 50%; background: #57d98b; box-shadow: 0 0 12rpx rgba(87, 217, 139, .9); }

.dots { position: relative; z-index: 10; display: flex; justify-content: center; gap: 8rpx; margin-top: 14rpx; }
.d { width: 8rpx; height: 8rpx; border-radius: 4rpx; background: rgba(255, 255, 255, .28); }
.d.on { width: 22rpx; background: rgba(255, 255, 255, .92); }

/* 歌词 */
.lyric { position: absolute; z-index: 10; left: 46rpx; right: 80rpx; top: 52%; }
.line1 { display: flex; align-items: center; gap: 16rpx; }
.bar {
  width: 4rpx; height: 34rpx; border-radius: 4rpx;
  background: linear-gradient(180deg, var(--km-accent), rgba(245, 180, 91, .2));
  flex-shrink: 0;
}
.l1 {
  font-size: 38rpx; font-weight: 700; letter-spacing: .5rpx; color: rgba(255, 255, 255, .95);
  text-shadow: 0 4rpx 20rpx rgba(0, 0, 0, .6);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.l2 {
  display: block; margin-top: 18rpx; padding-left: 20rpx;
  font-size: 26rpx; color: rgba(255, 255, 255, .42);
  text-shadow: 0 4rpx 16rpx rgba(0, 0, 0, .55);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

/* 控制区 */
.sheet { position: absolute; z-index: 10; left: 0; right: 0; bottom: 0; padding: 0 46rpx calc(env(safe-area-inset-bottom) + 52rpx); }
.title-row { display: flex; align-items: flex-end; gap: 14rpx; }
.title {
  font-size: 52rpx; font-weight: 900; letter-spacing: -.4rpx; color: #fbf6ef;
  max-width: 460rpx; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.sub { font-size: 22rpx; color: rgba(247, 241, 232, .46); padding-bottom: 8rpx; }
.meta-row { margin-top: 18rpx; display: flex; align-items: center; gap: 12rpx; }
.paywall {
  margin-top: 18rpx; padding: 20rpx 22rpx; border-radius: 20rpx;
  display: flex; align-items: center; gap: 18rpx;
  background: linear-gradient(135deg, rgba(245,180,91,.16), rgba(224,139,62,.06));
  border: 1rpx solid rgba(245,180,91,.28);
}
.pw-copy { flex: 1; display: flex; flex-direction: column; gap: 4rpx; }
.pw-tag { font-size: 18rpx; color: var(--km-accent); letter-spacing: 1rpx; }
.pw-title { font-size: 24rpx; color: #f7f1e8; font-weight: 700; }
.pw-sub { font-size: 18rpx; color: rgba(247,241,232,.5); }
.pw-btn {
  flex: none; padding: 16rpx 24rpx; border-radius: 999rpx;
  background: linear-gradient(140deg,#fbd08a,#e08b3e); color: #2b1a08;
  font-size: 22rpx; font-weight: 800;
}
.pw-btn.busy { opacity: .6; }
.artist { font-size: 22rpx; color: rgba(247, 241, 232, .62); }
.follow {
  height: 38rpx; padding: 0 16rpx; border-radius: 999rpx;
  border: 1rpx solid rgba(245, 180, 91, .5);
  color: var(--km-accent); font-size: 17rpx; font-weight: 600;
  display: flex; align-items: center;
}
.follow.on { background: rgba(245, 180, 91, .16); }
.divider { width: 2rpx; height: 22rpx; background: rgba(255, 255, 255, .14); margin: 0 4rpx; }
.chip {
  height: 38rpx; padding: 0 14rpx; border-radius: 8rpx;
  background: rgba(255, 255, 255, .08);
  color: rgba(247, 241, 232, .62);
  font-size: 17rpx;
  display: flex; align-items: center;
}
.chip.on { color: var(--km-accent); background: rgba(245, 180, 91, .14); }

.actions { margin-top: 34rpx; display: flex; align-items: center; justify-content: space-between; }
.act { display: flex; align-items: center; gap: 8rpx; }
.act.wrap { position: relative; }
.vip {
  position: absolute; right: -16rpx; bottom: -4rpx;
  font-size: 11rpx; font-weight: 800; color: #2b1a08;
  background: linear-gradient(96deg, #f8c97c, #e4934c);
  border-radius: 4rpx; padding: 0 5rpx; line-height: 15rpx;
}
.cnt { font-size: 18rpx; color: rgba(247, 241, 232, .45); }

.seek { margin-top: 40rpx; }
.track { position: relative; height: 5rpx; border-radius: 5rpx; background: rgba(255, 255, 255, .16); }
.fill {
  position: relative; height: 100%; border-radius: 5rpx;
  background: linear-gradient(90deg, #f9c878, #e58f46);
  box-shadow: 0 0 12rpx rgba(245, 180, 91, .45);
}
.knob {
  position: absolute; right: -8rpx; top: 50%; margin-top: -8rpx;
  width: 16rpx; height: 16rpx; border-radius: 50%;
  background: #f9c878;
  box-shadow: 0 0 0 5rpx rgba(245, 180, 91, .18), 0 0 14rpx rgba(245, 180, 91, .8);
}
.times { margin-top: 14rpx; display: flex; justify-content: space-between; font-size: 18rpx; color: rgba(247, 241, 232, .4); }

.controls { margin-top: 34rpx; display: flex; align-items: center; justify-content: space-between; }
.side { width: 70rpx; height: 70rpx; display: flex; align-items: center; justify-content: center; }
.play-btn {
  width: 118rpx; height: 118rpx; border-radius: 50%;
  background: linear-gradient(160deg, #fbd08a 0%, #efa254 46%, #de823c 100%);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 14rpx 34rpx rgba(224, 139, 62, .38), inset 0 1rpx 0 rgba(255, 255, 255, .5);
}

/* 弹层 */
.mask { position: fixed; inset: 0; z-index: 80; background: rgba(0,0,0,.62); display: flex; align-items: flex-end; }
.panel {
  width: 100%; background: #121116; border-radius: 28rpx 28rpx 0 0;
  padding: 28rpx 24rpx calc(38rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}
.panel.tall { max-height: 72vh; display: flex; flex-direction: column; }
.panel-title { display: block; font-size: 28rpx; font-weight: 800; color: #f7f1e8; margin-bottom: 16rpx; }
.panel-row {
  display: flex; align-items: center; gap: 18rpx;
  min-height: 96rpx; padding: 6rpx 8rpx; border-bottom: 1rpx solid rgba(255,255,255,.05);
}
.panel-row:last-of-type { border-bottom: 0; }
.panel-left { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4rpx; }
.panel-name { font-size: 25rpx; color: #f3f4f6; }
.panel-desc { font-size: 20rpx; color: rgba(247,241,232,.34); }
.panel-vip { font-size: 17rpx; font-weight: 800; color: var(--km-accent); border: 1rpx solid rgba(245,180,91,.4); border-radius: 6rpx; padding: 2rpx 8rpx; }
.panel-cancel {
  margin-top: 18rpx; height: 88rpx; line-height: 88rpx; text-align: center;
  border-radius: 16rpx; background: rgba(255,255,255,.06); color: rgba(247,241,232,.62); font-size: 25rpx;
}
.panel-loading { padding: 50rpx 0; text-align: center; font-size: 22rpx; color: rgba(247,241,232,.35); }
.similar-list { flex: 1; max-height: 52vh; }
.s-cover { width: 76rpx; height: 76rpx; border-radius: 10rpx; background: #17151b; flex: none; }
.speed-grid { display: flex; flex-wrap: wrap; gap: 14rpx; padding: 6rpx 0 10rpx; }
.speed-item {
  flex: 1 1 28%; text-align: center; padding: 22rpx 0; border-radius: 14rpx;
  background: rgba(255,255,255,.05); color: rgba(247,241,232,.6); font-size: 24rpx;
}
.speed-item.on { background: rgba(245,180,91,.18); color: #f5cf9a; font-weight: 800; }

/* 播放页广告位 */
.play-ad {
  display: flex; align-items: center; gap: 14rpx;
  margin: 24rpx 0 4rpx; padding: 12rpx 14rpx; border-radius: 16rpx;
  background: linear-gradient(135deg, rgba(245,180,91,.12), rgba(255,255,255,.03));
  border: 1rpx solid rgba(245,180,91,.22);
}
.pa-cover { width: 72rpx; height: 72rpx; border-radius: 10rpx; flex: none; background: #17151b; }
.pa-copy { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4rpx; }
.pa-tag { font-size: 16rpx; color: rgba(245,180,91,.9); font-weight: 700; }
.pa-name { font-size: 22rpx; color: #f3f4f6; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pa-cta { flex: none; padding: 8rpx 16rpx; border-radius: 999rpx; background: linear-gradient(135deg,#fbd08a,#de823c); color: #22150a; font-size: 18rpx; font-weight: 800; }
</style>


















