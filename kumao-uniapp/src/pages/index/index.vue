<template>
  <view class="km-screen home">
    <!-- 开屏广告 -->
    <view v-if="splashVisible && splashAd" class="splash" @click="openSplashAd">
      <image class="splash-bg" :src="coverOf(splashAd.coverUrl)" mode="aspectFill" />
      <view class="splash-shade"></view>
      <view class="splash-top">
        <text class="splash-tag">广告</text>
        <view class="splash-skip" @click.stop="closeSplash">{{ splashLeft }}s 跳过</view>
      </view>
      <view class="splash-bottom">
        <text class="splash-name">{{ splashAd.name }}</text>
        <text class="splash-desc">{{ splashAd.content }}</text>
        <view class="splash-cta">看广告领 1 天会员 ›</view>
      </view>
    </view>

    <!-- ============ 顶部品牌栏 ============ -->
    <view class="topbar">
      <image class="logo" :src="LOGO" mode="aspectFit" />
      <view class="brand">
        <text class="bw">发现音乐</text>
        <text class="be">KUMAO MUSIC</text>
      </view>
      <view class="sp"></view>
      <view class="ibtn" @click="goSearch">
        <km-icon name="search" :size="18" color="rgba(247,241,232,.82)" />
      </view>
      <view class="ibtn" @click="onNotice">
        <km-icon name="bell" :size="19" color="rgba(247,241,232,.82)" />
      </view>
      <image class="ava" :src="avatarOf(user.avatar)" mode="aspectFill" @click="goMine" />
    </view>

    <!-- ============ 搜索 ============ -->
    <view class="searchbox" @click="goSearch">
      <km-icon name="search" :size="17" color="rgba(247,241,232,.55)" />
      <text class="ph">搜索音乐、歌手、歌单、直播间</text>
      <text class="badge">免</text>
    </view>

    <!-- ============ 首页轮播图（后台轮播管理） ============ -->
    <swiper
      v-if="bannerList.length"
      class="banner-swiper"
      circular
      autoplay
      :interval="5000"
      indicator-dots
      indicator-color="rgba(255,255,255,.35)"
      indicator-active-color="#f5b45b"
    >
      <swiper-item v-for="b in bannerList" :key="b.id">
        <image class="banner-swiper__img" :src="coverOf(b.imageUrl)" mode="aspectFill" @click="openBanner(b)" />
      </swiper-item>
    </swiper>

    <!-- ============ 今日主打（后台可配自动轮播） ============ -->
    <swiper
      v-if="heroList.length"
      class="hero-swiper"
      :autoplay="heroAuto"
      :interval="heroIntervalMs"
      :duration="420"
      circular
      @change="onHeroChange"
    >
      <swiper-item v-for="(song, i) in heroList" :key="song.songId || song.id || i">
        <view class="hero" @click="openSong(song)">
          <image class="hero-bg" :src="coverOf(song.coverUrl)" mode="aspectFill" />
          <view class="hero-scrim"></view>
          <view class="hero-top">
            <text class="tag">今日主打</text>
            <view v-if="liveRooms.length" class="live-now">
              <view class="pip"></view>{{ liveRooms.length }} 个现场正在发生
            </view>
          </view>
          <view class="hero-copy">
            <text class="hero-name">{{ song.name }}</text>
            <text class="hero-artist">{{ song.singerName }} · 编辑精选 · 正在播放</text>
            <view class="hero-cta">
              <view class="btn-primary" @click.stop="playFeatured(song)">
                <km-icon name="play" :size="11" color="#2B1A08" />
                <text>立即播放</text>
              </view>
              <text class="ghost" @click.stop="openSong(song)">查看详情 ›</text>
            </view>
          </view>
          <view class="hero-foot">
            <text class="b">今日热歌</text>
            <text class="t">{{ song.singerName }} · 全站热度持续上升</text>
          </view>
          <view v-if="heroList.length > 1" class="hero-dots">
            <view v-for="(s, di) in heroList" :key="di" :class="['hero-dot', { on: di === heroIndex }]"></view>
          </view>
        </view>
      </swiper-item>
    </swiper>

    <view v-else class="hero hero-single" @click="goLive">
      <view class="hero-scrim"></view>
      <view class="hero-copy">
        <text class="hero-name">今天想听点什么？</text>
        <text class="hero-artist">从热歌、新歌和直播现场开始，找到你的下一首循环</text>
        <view class="hero-cta">
          <text class="ghost" @click.stop="goLive">进入直播广场 ›</text>
        </view>
      </view>
    </view>

    <!-- ============ 六大入口 ============ -->
    <view class="grid">
      <view v-for="g in entries" :key="g.label" class="g" @click="g.action()">
        <view class="gi">
          <km-icon :name="g.icon" :size="19" color="#F3C384" />
        </view>
        <text class="gl">{{ g.label }}</text>
      </view>
    </view>

    <!-- ============ 正在直播 ============ -->
    <view class="sec-head">
      <text class="sec-title">正在直播</text>
      <view class="live-chip"><view class="pip"></view>LIVE</view>
      <view class="sp"></view>
      <text class="more" @click="goLive">全部 {{ liveTotal }} 个 ›</text>
    </view>

    <view v-if="liveRooms.length" class="live-row" @click="openLive(liveRooms[0])">
      <view class="thumb">
        <image class="img" :src="coverOf(liveRooms[0].coverUrl)" mode="aspectFill" />
        <text class="lt">LIVE</text>
      </view>
      <view class="info">
        <text class="t">{{ liveRooms[0].title }}</text>
        <text class="s">{{ liveRooms[0].anchorName || '主播' }} · {{ formatCount(liveRooms[0].realtimeOnline) }} 人在线</text>
      </view>
      <view class="go">
        <km-icon name="play" :size="10" color="#F5B45B" />
      </view>
    </view>
    <view v-else class="live-empty" @click="goLive">
      <text>今晚还没有开播的房间 · 去直播广场看看 ›</text>
    </view>

    <!-- ============ 筛选 ============ -->
    <scroll-view class="filters" scroll-x :show-scrollbar="false">
      <view
        v-for="f in filters"
        :key="f.key"
        :class="['f2', { on: filterKey === f.key }]"
        @click="switchFilter(f)"
      >{{ f.label }}</view>
    </scroll-view>

    <!-- ============ 榜单 ============ -->
    <!-- 广告位（广告商投放） -->
    <view v-if="bannerAd && !isVip" class="ad-strip" @click="openAd(bannerAd)">
      <image class="ad-cover" :src="coverOf(bannerAd.coverUrl)" mode="aspectFill" />
      <view class="ad-copy">
        <text class="ad-kicker">广告 · {{ bannerAd.name }}</text>
        <text class="ad-text">{{ bannerAd.content || '点击了解更多' }}</text>
      </view>
      <view class="ad-cta">看广告领会员</view>
    </view>
    <view class="rank-head">
      <text class="sec-title">{{ listTitle }}</text>
      <text class="rank-sub">{{ listSub }}</text>
      <view class="sp"></view>
      <text class="more" @click="goRank">查看全部 ›</text>
    </view>

    <view class="rank-list">
      <km-song-row
        v-for="(s, i) in list"
        :key="s.songId || s.id"
        :song="s"
        :index="i"
        :show-rank="true"
        :show-plays="true"
        :liked="likedIds.indexOf(s.songId) >= 0"
        @select="openSong"
        @like="toggleLike"
      />
      <view v-if="!list.length" class="km-empty">
        <text>{{ loading ? '正在加载…' : '暂无榜单数据' }}</text>
      </view>
    </view>

    <km-mini-player />

    <km-ad-modal v-model:visible="adOpen" :ad="currentAd" @claimed="onAdClaimed" />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import KmIcon from '@/components/km-icon.vue'
import KmSongRow from '@/components/km-song-row.vue'
import KmMiniPlayer from '@/components/km-mini-player.vue'
import KmAdModal from '@/components/km-ad-modal.vue'
import { api } from '@/api'
import { coverOf, avatarOf, formatCount } from '@/utils/media'
import { usePlayer } from '@/utils/player'

const LOGO =
  'data:image/svg+xml;charset=utf-8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" fill="none">' +
      '<path d="M7.6 13.2 5.4 5.6l6.8 3.2h7.6l6.8-3.2-2.2 7.6c1.1 1.7 1.7 3.8 1.7 5.9 0 5.5-4.6 9.2-11.1 9.2S4.1 24.6 4.1 19.1c0-2.1.6-4.2 1.7-5.9z" stroke="#F5B45B" stroke-width="2" stroke-linejoin="round"/>' +
      '<circle cx="12.4" cy="18.4" r="1.5" fill="#F5B45B"/><circle cx="19.6" cy="18.4" r="1.5" fill="#F5B45B"/>' +
      '<path d="M14.6 22.6c.9.7 1.9.7 2.8 0" stroke="#F5B45B" stroke-width="1.6" stroke-linecap="round"/></svg>'
  )

const { play } = usePlayer()

const user = ref({})
const hotSongs = ref([])
const bannerAd = ref(null)
const bannerList = ref([])
const isVip = ref(false)
const splashAd = ref(null)
const splashVisible = ref(false)
const splashLeft = ref(3)
let splashTimer = null
const currentAd = ref({})
const adOpen = ref(false)
const liveRooms = ref([])
const liveTotal = ref(0)
const list = ref([])
const loading = ref(true)
const likedIds = ref([])

const filterKey = ref('hot')
const filters = [
  { key: 'hot', label: '热门' },
  { key: 'new', label: '新歌' },
  { key: 'rising', label: '飙升' },
  { key: 'singer', label: '歌手', go: '/pages/singer/singer' },
  { key: 'mv', label: 'MV', go: '/pages/mv/mv' },
  { key: 'live', label: '直播', go: '/pages/live/live', tab: true }
]

const featured = computed(() => heroList.value[heroIndex.value] || heroList.value[0] || null)

/* ---- 今日主打轮播（后台「首页设置」可配） ---- */
const heroSetting = ref({ heroAutoplay: 1, heroInterval: 5, heroCount: 3 })
const heroIndex = ref(0)
const heroList = computed(() => {
  const configured = Array.isArray(heroSetting.value.songs) ? heroSetting.value.songs : []
  if (configured.length) return configured
  const count = Math.max(1, Math.min(10, Number(heroSetting.value.heroCount) || 3))
  return hotSongs.value.slice(0, count)
})
const heroAuto = computed(() => Number(heroSetting.value.heroAutoplay) !== 0 && heroList.value.length > 1)
const heroIntervalMs = computed(() => Math.max(2, Math.min(15, Number(heroSetting.value.heroInterval) || 5)) * 1000)
function onHeroChange(e) {
  heroIndex.value = (e && e.detail && e.detail.current) || 0
}
const listTitle = computed(() => (filterKey.value === 'new' ? '新歌速递' : filterKey.value === 'rising' ? '飙升榜' : '热门推荐'))
const listSub = computed(() =>
  filterKey.value === 'new' ? '刚刚发行的好音乐' : filterKey.value === 'rising' ? '热度上升最快' : '按播放量排序'
)

const entries = [
  { label: '排行榜', icon: 'rank', action: () => uni.switchTab({ url: '/pages/rank/rank' }) },
  { label: '歌手', icon: 'user', action: () => uni.navigateTo({ url: '/pages/singer/singer' }) },
  { label: '歌单', icon: 'music', action: () => uni.navigateTo({ url: '/pages/playlist/playlist' }) },
  { label: 'MV', icon: 'video', action: () => uni.navigateTo({ url: '/pages/mv/mv' }) },
  { label: '直播', icon: 'broadcast', action: () => uni.switchTab({ url: '/pages/live/live' }) },
  { label: '音乐人', icon: 'mic', action: () => uni.navigateTo({ url: '/pages/musician/musician' }) }
]

function goSearch() { uni.navigateTo({ url: '/pages/search/search' }) }
function goMine() { uni.switchTab({ url: '/pages/mine/mine' }) }
function goLive() { uni.switchTab({ url: '/pages/live/live' }) }
function openSplashAd() {
  if (!splashAd.value) return
  closeSplash()
  currentAd.value = splashAd.value
  adOpen.value = true
  if (splashAd.value.planId) api.adReport(splashAd.value.planId, 'click').catch(() => {})
}

function closeSplash() {
  splashVisible.value = false
  if (splashTimer) clearInterval(splashTimer)
  splashTimer = null
}

function startSplash() {
  splashLeft.value = 3
  splashVisible.value = true
  if (splashAd.value && splashAd.value.planId) api.adReport(splashAd.value.planId, 'exposure').catch(() => {})
  if (splashTimer) clearInterval(splashTimer)
  splashTimer = setInterval(() => {
    splashLeft.value -= 1
    if (splashLeft.value <= 0) closeSplash()
  }, 1000)
}

function openAd(ad) {
  if (!ad) return
  currentAd.value = ad
  adOpen.value = true
  if (ad.planId) api.adReport(ad.planId, 'click').catch(() => {})
}

function onAdClaimed() {
  adOpen.value = false
}

function goRank() { uni.switchTab({ url: '/pages/rank/rank' }) }
function onNotice() { uni.showToast({ title: '暂无新消息', icon: 'none' }) }

function openLive(room) {
  if (!room) return goLive()
  uni.navigateTo({ url: `/pages/live/room?id=${room.id}` })
}

function openSong(song) {
  const id = song.songId || song.id
  if (!id) return
  uni.setStorageSync('km_current_song', song)
  api.playRecord({ songId: id, device: 'APP' }).catch(() => {})
  uni.navigateTo({ url: `/pages/song/song?id=${id}` })
}

function playFeatured(song) {
  const target = song && (song.songId || song.id) ? song : featured.value
  if (target) play(target, hotSongs.value)
}

function toggleLike(song) {
  const id = song.songId || song.id
  const idx = likedIds.value.indexOf(id)
  if (idx >= 0) {
    likedIds.value.splice(idx, 1)
    api.unlike(id).catch(() => {})
  } else {
    likedIds.value.push(id)
    api.like(id).catch(() => {})
  }
}

async function loadRank(key) {
  const fn = key === 'new' ? api.newSongs : key === 'rising' ? api.rising : api.hot
  const res = await fn(20).catch(() => null)
  list.value = (res && res.data) || []
}

function switchFilter(f) {
  if (f.go) {
    if (f.tab) uni.switchTab({ url: f.go })
    else uni.navigateTo({ url: f.go })
    return
  }
  if (filterKey.value === f.key) return
  filterKey.value = f.key
  list.value = []
  loadRank(f.key)
}

let splashShown = false

function openBanner(banner) {
  const link = (banner?.linkUrl || '').trim()
  if (!link) return
  if (/^https?:/i.test(link)) {
    // #ifdef H5
    window.open(link, '_blank')
    // #endif
    // #ifndef H5
    uni.setClipboardData({ data: link })
    uni.showToast({ title: '链接已复制，请在浏览器打开', icon: 'none' })
    // #endif
    return
  }
  const song = link.match(/^\/song\/(\d+)/)
  if (song) return uni.navigateTo({ url: `/pages/song/song?id=${song[1]}` })
  if (link.startsWith('/live')) return uni.switchTab({ url: '/pages/live/live' })
  if (link.startsWith('/vip')) return uni.switchTab({ url: '/pages/vip/vip' })
  if (link.startsWith('/mine')) return uni.switchTab({ url: '/pages/mine/mine' })
  if (link.startsWith('/playlist/')) return uni.navigateTo({ url: `/pages/playlist/detail?id=${link.split('/').pop()}` })
  uni.navigateTo({ url: link })
}

async function bootstrap() {
  const [h, adRes, splashRes, l, vipRes, settingRes, bannerRes] = await Promise.all([
    api.hot(20).catch(() => null),
    api.adsActive('HOME_BANNER').catch(() => null),
    api.adsActive('SPLASH').catch(() => null),
    api.liveSquare({ pageNum: 1, pageSize: 4 }).catch(() => null),
    uni.getStorageSync('km_token') ? api.vipStatus().catch(() => null) : Promise.resolve(null),
    api.homeSettings().catch(() => null),
    api.banners().catch(() => null)
  ])
  bannerList.value = (bannerRes && bannerRes.data) || []
  hotSongs.value = (h && h.data) || []
  if (settingRes && settingRes.data) heroSetting.value = settingRes.data
  heroIndex.value = 0
  const ads = (adRes && adRes.data) || []
  bannerAd.value = ads.find((a) => a.slot === 'HOME_BANNER') || ads[0] || null
  isVip.value = !!(vipRes && vipRes.data && vipRes.data.vip)
  const splashAds = (splashRes && splashRes.data) || []
  splashAd.value = splashAds[0] || null
  if (splashAd.value && !isVip.value && !splashShown) {
    splashShown = true
    startSplash()
  }
  const rooms = (l && l.data && l.data.records) || []
  liveRooms.value = rooms
  liveTotal.value = (l && l.data && l.data.total) || rooms.length
  if (!list.value.length) list.value = hotSongs.value
  loading.value = false
}

onShow(() => {
  user.value = uni.getStorageSync('km_user') || {}
  if (uni.getStorageSync('km_token')) {
    api.vipStatus().then((r) => { isVip.value = !!(r && r.data && r.data.vip) }).catch(() => {})
  } else {
    isVip.value = false
  }
  if (!hotSongs.value.length) bootstrap()
})
</script>

<style scoped>
.home {
  padding-top: calc(var(--status-bar-height, 0px) + 26rpx);
  padding-bottom: calc(var(--window-bottom, 0px) + 190rpx);
}

/* 品牌栏 */
.topbar { height: 76rpx; padding: 0 36rpx; display: flex; align-items: center; gap: 14rpx; }
.logo { width: 46rpx; height: 46rpx; }
.brand { display: flex; flex-direction: column; gap: 2rpx; }
.bw { font-size: 32rpx; font-weight: 900; letter-spacing: .5rpx; color: #f7f1e8; }
.be { font-size: 12rpx; letter-spacing: 3.4rpx; color: rgba(247, 241, 232, .28); }
.sp { flex: 1; }
.ibtn {
  width: 56rpx; height: 56rpx; border-radius: 50%;
  background: rgba(255, 255, 255, .055);
  border: 1rpx solid rgba(255, 255, 255, .07);
  display: flex; align-items: center; justify-content: center;
}
.ava {
  width: 52rpx; height: 52rpx; border-radius: 50%;
  border: 1.5rpx solid rgba(245, 180, 91, .5);
  margin-left: 4rpx; background: #1a1c21;
}

/* 搜索 */
.searchbox {
  margin: 20rpx 36rpx 0;
  height: 80rpx;
  border-radius: 40rpx;
  background: rgba(255, 255, 255, .055);
  border: 1rpx solid rgba(255, 255, 255, .075);
  display: flex; align-items: center; gap: 14rpx;
  padding: 0 22rpx;
}
.ph { flex: 1; font-size: 24rpx; color: rgba(247, 241, 232, .4); }
.badge {
  width: 32rpx; height: 32rpx; border-radius: 9rpx;
  border: 1rpx solid rgba(245, 180, 91, .55);
  color: var(--km-accent); font-size: 16rpx; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}

/* 今日主打 */
.banner-swiper { height: 210rpx; margin: 0 24rpx 20rpx; border-radius: 20rpx; overflow: hidden; }
.banner-swiper__img { width: 100%; height: 100%; display: block; }
.hero-swiper {
  margin: 26rpx 36rpx 0;
  height: 300rpx;
  border-radius: 40rpx;
  overflow: hidden;
}
.hero {
  position: relative;
  height: 100%;
  border-radius: 40rpx;
  overflow: hidden;
  background: #14121a;
  border: 1rpx solid rgba(255, 255, 255, .08);
  box-shadow: 0 20rpx 40rpx rgba(0, 0, 0, .5);
}
.hero-single { margin: 26rpx 36rpx 0; height: 300rpx; }
.hero-dots {
  position: absolute; right: 26rpx; bottom: 56rpx;
  display: flex; align-items: center; gap: 8rpx;
}
.hero-dot {
  width: 8rpx; height: 8rpx; border-radius: 50%;
  background: rgba(255, 255, 255, .3);
  transition: width .25s ease, background .25s ease;
}
.hero-dot.on { width: 22rpx; border-radius: 999rpx; background: var(--km-accent); }
.hero-bg { position: absolute; inset: 0; width: 100%; height: 100%; }
.hero-scrim {
  position: absolute; inset: 0;
  background:
    linear-gradient(90deg, rgba(8, 7, 10, .95) 0%, rgba(8, 7, 10, .74) 40%, rgba(8, 7, 10, .16) 78%, rgba(8, 7, 10, .34) 100%),
    linear-gradient(180deg, rgba(8, 7, 10, .5) 0%, transparent 30%, transparent 58%, rgba(8, 7, 10, .74) 100%);
}
.hero-top { position: absolute; left: 24rpx; right: 24rpx; top: 22rpx; display: flex; align-items: center; }
.tag {
  height: 36rpx; padding: 0 14rpx; border-radius: 10rpx;
  background: rgba(255, 255, 255, .12);
  border: 1rpx solid rgba(255, 255, 255, .16);
  font-size: 16rpx; letter-spacing: 1rpx; color: rgba(255, 255, 255, .9);
  display: flex; align-items: center;
}
.live-now { margin-left: auto; display: flex; align-items: center; gap: 8rpx; font-size: 16rpx; color: rgba(255, 255, 255, .6); }
.pip { width: 8rpx; height: 8rpx; border-radius: 50%; background: #f0605d; box-shadow: 0 0 10rpx rgba(240, 96, 93, .9); }
.hero-copy { position: absolute; left: 28rpx; bottom: 62rpx; right: 28rpx; }
.hero-name {
  display: block;
  font-size: 46rpx; font-weight: 900; letter-spacing: -.5rpx; color: #fff;
  text-shadow: 0 4rpx 18rpx rgba(0, 0, 0, .5);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.hero-artist {
  display: block; margin-top: 8rpx;
  font-size: 19rpx; color: rgba(255, 255, 255, .66);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.hero-cta { margin-top: 16rpx; display: flex; align-items: center; gap: 20rpx; }
.btn-primary {
  height: 54rpx; padding: 0 24rpx 0 18rpx; border-radius: 999rpx;
  background: linear-gradient(140deg, #fbd08a, #e08b3e);
  display: flex; align-items: center; gap: 10rpx;
  font-size: 20rpx; font-weight: 800; color: #2b1a08;
  box-shadow: 0 10rpx 22rpx rgba(224, 139, 62, .32);
}
.ghost { font-size: 18rpx; color: rgba(255, 255, 255, .55); }
.hero-foot {
  position: absolute; left: 0; right: 0; bottom: 0; height: 44rpx;
  padding: 0 26rpx;
  display: flex; align-items: center; gap: 12rpx;
  border-top: 1rpx solid rgba(255, 255, 255, .07);
  background: linear-gradient(180deg, rgba(8, 7, 10, .1), rgba(8, 7, 10, .78));
  font-size: 16rpx; color: rgba(255, 255, 255, .45);
}
.hero-foot .b { color: rgba(255, 255, 255, .8); font-weight: 600; }
.hero-foot .t { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* 六宫格 */
.grid { margin: 26rpx 30rpx 0; display: flex; justify-content: space-between; }
.g { width: 92rpx; display: flex; flex-direction: column; align-items: center; gap: 10rpx; }
.gi {
  width: 66rpx; height: 66rpx; border-radius: 22rpx;
  background: linear-gradient(160deg, rgba(255, 255, 255, .09), rgba(255, 255, 255, .03));
  border: 1rpx solid rgba(255, 255, 255, .08);
  display: flex; align-items: center; justify-content: center;
}
.gl { font-size: 18rpx; color: rgba(247, 241, 232, .55); }

/* 分区标题 */
.sec-head { margin: 30rpx 36rpx 12rpx; display: flex; align-items: center; gap: 12rpx; }
.sec-title { font-size: 30rpx; font-weight: 800; letter-spacing: .2rpx; color: #f7f1e8; }
.live-chip {
  height: 28rpx; padding: 0 8rpx; border-radius: 6rpx;
  background: rgba(240, 96, 93, .14);
  color: #ff8a85; font-size: 14rpx; font-weight: 700; letter-spacing: 1rpx;
  display: flex; align-items: center; gap: 5rpx;
}
.more { font-size: 18rpx; color: rgba(247, 241, 232, .34); }

/* 直播条 */
.live-row {
  margin: 0 36rpx;
  height: 104rpx;
  border-radius: 28rpx;
  background: linear-gradient(100deg, rgba(255, 255, 255, .06), rgba(255, 255, 255, .025));
  border: 1rpx solid rgba(255, 255, 255, .07);
  display: flex; align-items: center; gap: 16rpx;
  padding: 0 16rpx;
}
.thumb { position: relative; width: 72rpx; height: 72rpx; border-radius: 20rpx; overflow: hidden; flex-shrink: 0; background: #1a1c21; }
.thumb .img { width: 100%; height: 100%; }
.lt {
  position: absolute; left: 6rpx; bottom: 6rpx;
  height: 20rpx; padding: 0 5rpx; border-radius: 4rpx;
  background: linear-gradient(96deg, #ff8a6b, #e94c55);
  color: #fff; font-size: 11rpx; font-weight: 800; letter-spacing: .6rpx;
  display: flex; align-items: center;
}
.info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6rpx; }
.info .t { font-size: 23rpx; font-weight: 600; color: #f7f1e8; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.info .s { font-size: 18rpx; color: rgba(247, 241, 232, .38); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.go {
  width: 48rpx; height: 48rpx; border-radius: 50%;
  background: rgba(245, 180, 91, .16);
  display: flex; align-items: center; justify-content: center;
}
.live-empty {
  margin: 0 36rpx; padding: 26rpx 0;
  border-radius: 24rpx; border: 1rpx dashed rgba(255, 255, 255, .12);
  text-align: center; font-size: 20rpx; color: rgba(247, 241, 232, .32);
}

/* 筛选 */
.filters { margin: 22rpx 0 0; padding: 0 36rpx; white-space: nowrap; }
.f2 {
  display: inline-flex; align-items: center;
  height: 50rpx; padding: 0 20rpx; margin-right: 12rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(255, 255, 255, .075);
  background: rgba(255, 255, 255, .025);
  font-size: 19rpx; color: rgba(247, 241, 232, .5);
}
.f2.on { background: linear-gradient(140deg, #f9c878, #e08b3e); border-color: transparent; color: #2b1a08; font-weight: 800; }

/* 榜单 */
.rank-head { margin: 26rpx 36rpx 6rpx; display: flex; align-items: flex-end; gap: 10rpx; }
.rank-sub { font-size: 17rpx; color: rgba(247, 241, 232, .3); padding-bottom: 2rpx; }
.rank-list { padding: 0 28rpx; }

/* 广告位 */
.ad-strip {
  display: flex; align-items: center; gap: 16rpx; margin: 8rpx 24rpx 6rpx;
  padding: 14rpx; border-radius: 18rpx;
  background: linear-gradient(135deg, rgba(245,180,91,.12), rgba(255,255,255,.03));
  border: 1rpx solid rgba(245,180,91,.22);
}
.ad-cover { width: 96rpx; height: 96rpx; border-radius: 12rpx; flex: none; background: #17151b; }
.ad-copy { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6rpx; }
.ad-kicker { font-size: 18rpx; color: rgba(245,180,91,.9); font-weight: 700; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ad-text { font-size: 23rpx; color: #f3f4f6; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ad-cta {
  flex: none; padding: 10rpx 18rpx; border-radius: 999rpx;
  background: linear-gradient(135deg,#fbd08a,#de823c); color: #22150a;
  font-size: 20rpx; font-weight: 800;
}

/* 开屏广告 */
.splash { position: fixed; inset: 0; z-index: 950; background: #000; overflow: hidden; }
.splash-bg { position: absolute; inset: 0; width: 100%; height: 100%; }
.splash-shade { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0,0,0,.45), transparent 40%, rgba(0,0,0,.8)); }
.splash-top { position: absolute; top: 90rpx; left: 30rpx; right: 30rpx; display: flex; align-items: center; justify-content: space-between; }
.splash-tag { font-size: 18rpx; padding: 4rpx 12rpx; border-radius: 6rpx; background: rgba(0,0,0,.5); color: rgba(255,255,255,.75); }
.splash-skip { font-size: 22rpx; padding: 10rpx 22rpx; border-radius: 999rpx; background: rgba(0,0,0,.55); border: 1rpx solid rgba(255,255,255,.2); color: #fff; }
.splash-bottom { position: absolute; left: 34rpx; right: 34rpx; bottom: 120rpx; display: flex; flex-direction: column; gap: 10rpx; }
.splash-name { font-size: 38rpx; font-weight: 900; color: #fff; letter-spacing: -1rpx; }
.splash-desc { font-size: 24rpx; color: rgba(255,255,255,.72); }
.splash-cta {
  align-self: flex-start; margin-top: 12rpx; padding: 18rpx 30rpx; border-radius: 999rpx;
  background: linear-gradient(135deg,#fbd08a,#de823c); color: #22150a; font-size: 24rpx; font-weight: 800;
}
</style>















