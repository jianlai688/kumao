<template>
  <div class="home-page">
    <header class="home-head">
      <div class="home-heading">
        <h1 class="home-title">发现音乐</h1>
        <p class="home-sub">
          <span>{{ todayDateLabel }}</span>
          <span class="sub-sep" aria-hidden="true">·</span>
          <span>{{ weekdayLabel }}</span>
          <span class="sub-sep" aria-hidden="true">·</span>
          <span>{{ hotSongs.length }} 首热歌</span>
          <span class="sub-sep" aria-hidden="true">·</span>
          <span class="sub-live" :class="{ muted: !liveRooms.length }"><i aria-hidden="true"></i>{{ liveRooms.length ? `${liveRooms.length} 个直播间正在开播` : '暂无直播间开播' }}</span>
        </p>
      </div>
      <div class="home-actions">
        <router-link to="/rank/hot" class="rank-link">
          查看榜单
          <KmIcon name="chevron-right" :size="13" />
        </router-link>
        <button class="play-all-btn" :disabled="!hotSongs.length" @click="playAllHot">
          <KmIcon name="play" :size="14" />
          播放今日热歌
        </button>
      </div>
    </header>

    <div v-if="loading" class="home-skeleton" aria-label="首页内容加载中">
      <div class="sk-head">
        <span class="sk-title"></span>
        <span class="sk-line"></span>
      </div>
      <div class="sk-hero"></div>
      <div class="sk-stage">
        <div class="sk-feature"></div>
        <div class="sk-side"></div>
      </div>
      <div class="sk-row"></div>
    </div>

    <template v-else>
      <section
        v-if="activeBanner"
        class="festival-hero banner-hero"
        role="link"
        tabindex="0"
        :aria-label="`${activeBanner.title || '首页活动'}，查看详情`"
        @click="openBanner(activeBanner)"
        @keydown.enter="openBanner(activeBanner)"
      >
        <img
          :src="activeBanner.imageUrl"
          class="banner-bg"
          alt=""
          @error="onBannerError(activeBanner)"
        />
        <span class="banner-tint" aria-hidden="true"></span>
        <button v-if="activeBanner.linkUrl" type="button" class="banner-cta" @click.stop="openBanner(activeBanner)">
          查看详情
          <KmIcon name="chevron-right" :size="13" />
        </button>
        <div class="hero-dots banner-dots" role="tablist" aria-label="首页轮播">
          <button
            v-for="(slide, i) in bannerSlides"
            :key="slide.id"
            :class="['hero-dot', { on: i === heroIndex }]"
            role="tab"
            :aria-selected="i === heroIndex"
            :aria-label="slide.title || `轮播 ${i + 1}`"
            @click.stop="setHeroIndex(i)"
          ></button>
        </div>
      </section>

      <section
        v-else-if="activeHero"
        class="festival-hero"
        :class="`hero--${activeHero.key}`"
        aria-labelledby="festival-title"
      >
        <div class="hero-orbits" aria-hidden="true"></div>

        <div class="hero-copy">
          <p class="hero-kicker"><span aria-hidden="true"></span>{{ activeHero.kicker }}</p>
          <h2 id="festival-title" class="hero-title">{{ activeHero.title }}</h2>
          <p class="hero-subtitle">{{ activeHero.subtitle }}</p>
          <button class="hero-cta" @click="handleHeroCta">
            {{ activeHero.cta }}
            <KmIcon name="chevron-right" :size="14" />
          </button>
        </div>

        <div class="hero-event">
          <div class="event-head">
            <span>{{ activeHero.event.label }}</span>
            <time>{{ activeHero.event.time }}</time>
          </div>
          <h3>{{ activeHero.event.title }}</h3>
          <p>{{ activeHero.event.note }}</p>
          <button class="event-action" :class="{ done: reserved && activeHero.key === 'live' }" @click="handleHeroEvent">
            <KmIcon :name="activeHero.key === 'live' ? 'bell' : activeHero.key === 'pick' ? 'play' : 'crown'" :size="13" />
            {{ activeHero.key === 'live' && reserved ? '已预约' : activeHero.event.action }}
          </button>
        </div>

        <div class="hero-dots" role="tablist" aria-label="首页推荐轮播">
          <button
            v-for="(slide, i) in heroSlides"
            :key="slide.key"
            :class="['hero-dot', { on: i === heroIndex }]"
            role="tab"
            :aria-selected="i === heroIndex"
            :aria-label="slide.title"
            @click="setHeroIndex(i)"
          ></button>
        </div>
      </section>

      <div class="home-stage">
        <section v-if="featuredSong" class="feature-card" aria-labelledby="featured-title">
          <div class="feature-cover">
            <img :src="featuredCover" :alt="`${featuredSong.name} 封面`" @error="onImg" />
            <span class="quality-stamp">24bit / 96kHz</span>
          </div>

          <div class="feature-copy">
            <span class="feature-pick"><KmIcon name="star" :size="12" />今日主打</span>
            <h2 id="featured-title" class="feature-title">
              <router-link :to="`/song/${featuredId}`">{{ featuredSong.name }}</router-link>
            </h2>
            <p class="feature-meta">
              <span>{{ featuredSong.singerName }}</span>
              <span class="meta-dot" aria-hidden="true">·</span>
              <span>{{ featuredYear }}</span>
              <span class="meta-dot" aria-hidden="true">·</span>
              <span>《{{ featuredAlbum }}》</span>
            </p>
            <div class="feature-tags">
              <span class="eq" aria-hidden="true"><i></i><i></i><i></i><i></i></span>
              <span>编辑精选</span>
              <span class="tag-dot" aria-hidden="true">·</span>
              <span>正在热播</span>
              <span class="heat-pill"><span aria-hidden="true">↑</span> 热度 +12%</span>
            </div>
            <p class="feature-desc">{{ featuredDescription }}</p>

            <div class="feature-actions">
              <button class="feature-play" @click="toggleFeaturedPlay">
                <KmIcon :name="isFeaturedPlaying ? 'pause' : 'play'" :size="14" />
                {{ isFeaturedPlaying ? '暂停播放' : isFeaturedCurrent && player.progress > 0 ? '继续播放' : '播放这首歌' }}
              </button>
              <button
                class="round-action"
                :class="{ liked: featuredLiked }"
                :aria-label="featuredLiked ? '取消收藏这首歌' : '收藏这首歌'"
                :aria-pressed="featuredLiked"
                @click="toggleFeaturedLike"
              >
                <KmIcon name="heart" :size="18" :filled="featuredLiked" />
              </button>
              <button class="round-action" aria-label="查看歌曲详情" @click="goDetail(featuredId)">
                <KmIcon name="more" :size="18" />
              </button>
            </div>
          </div>
        </section>

        <div v-else class="feature-card empty-feature">
          <p>今天还没有主打推荐，去热歌榜逛逛。</p>
          <router-link to="/rank/hot" class="text-link">打开热歌榜</router-link>
        </div>

        <aside class="live-panel" aria-labelledby="live-panel-title">
          <div class="live-head">
            <div class="live-heading">
              <h2 id="live-panel-title">{{ liveRooms.length ? '正在直播' : '直播预告' }}</h2>
              <span>{{ liveRooms.length ? `${liveRooms.length} 场` : '今晚' }}</span>
            </div>
            <router-link to="/live" class="live-more">全部 <KmIcon name="chevron-right" :size="12" /></router-link>
          </div>

          <div class="live-list">
            <router-link
              v-for="row in liveRows"
              :key="row.key"
              :to="row.to"
              class="live-row"
              :aria-label="row.upcoming ? `查看直播预告 ${row.title}` : `进入直播间 ${row.title}`"
            >
              <span class="live-avatar" :class="`tone-${row.tone}`" aria-hidden="true">{{ row.initial }}</span>
              <span class="live-copy">
                <span class="live-title">{{ row.title }}</span>
                <span class="live-meta">{{ row.meta }}</span>
              </span>
              <span v-if="!row.upcoming" class="live-tag"><i aria-hidden="true"></i>LIVE</span>
              <span v-else class="live-tag upcoming">预告</span>
            </router-link>

            <div v-if="!liveRows.length" class="live-empty">
              <p>今晚还没有开播的房间</p>
              <router-link to="/live">去直播广场看看</router-link>
            </div>
          </div>

          <router-link to="/live" class="live-cta">
            打开直播广场
            <KmIcon name="chevron-right" :size="14" />
          </router-link>
        </aside>
      </div>

      <section class="hot-today" aria-labelledby="hot-title">
        <header class="section-head">
          <div class="section-title-wrap">
            <h2 id="hot-title">今日热歌</h2>
            <p>{{ todayDateLabel }}更新 · 共 {{ hotSongs.length }} 首</p>
          </div>
          <button class="section-play" :disabled="!hotSongs.length" @click="playAllHot">
            <KmIcon name="play" :size="13" />
            播放全部
          </button>
        </header>

        <div v-if="hotSongs.length" class="song-table">
          <div class="song-table-head" aria-hidden="true">
            <span>#</span>
            <span></span>
            <span>歌曲</span>
            <span>歌手</span>
            <span>专辑</span>
            <span>时长</span>
            <span>播放</span>
            <span></span>
          </div>

          <div
            v-for="(song, i) in hotSongs"
            :key="song.songId"
            class="song-row"
            role="button"
            tabindex="0"
            :aria-label="`查看 ${song.name}，${song.singerName}`"
            @click="goDetail(song.songId)"
            @keydown.enter="goDetail(song.songId)"
            @keydown.space.prevent="goDetail(song.songId)"
          >
            <span class="song-rank" :class="{ top: i < 3 }">{{ String(i + 1).padStart(2, '0') }}</span>
            <div class="song-cover-wrap">
              <img :src="coverFor(song)" class="song-cover" alt="" @error="onImg" />
              <button class="song-play" :aria-label="`播放 ${song.name}`" @click.stop="playSong(song, hotSongs)">
                <KmIcon :name="isSongPlaying(song) ? 'pause' : 'play'" :size="11" />
              </button>
            </div>
            <div class="song-main">
              <span class="song-name">
                {{ song.name }}
                <span v-if="song.vipOnly" class="vip-tag">VIP</span>
              </span>
            </div>
            <span class="song-artist">{{ song.singerName }}</span>
            <span class="song-album">{{ song.albumName || '—' }}</span>
            <span class="song-duration">{{ formatDuration(song.duration) }}</span>
            <span class="song-plays">{{ formatPlay(song.playCount) }}</span>
            <span class="song-more" aria-hidden="true"><KmIcon name="more" :size="16" /></span>
          </div>
        </div>

        <div v-else class="empty-state">
          <p>热歌榜正在更新，稍后再来看看。</p>
          <router-link to="/rank/hot" class="text-link">去排行榜</router-link>
        </div>
      </section>

      <div class="home-lower">
        <section class="lower-panel" aria-labelledby="new-title">
          <header class="lower-head">
            <h2 id="new-title">新歌速递</h2>
            <router-link to="/rank/new" class="lower-more">更多 <KmIcon name="chevron-right" :size="12" /></router-link>
          </header>
          <div class="new-list">
            <router-link
              v-for="song in newSongList"
              :key="song.songId || song.id"
              :to="`/song/${song.songId || song.id}`"
              class="new-row"
            >
              <img :src="coverFor(song)" alt="" @error="onImg" />
              <span class="new-copy">
                <b>{{ song.name }}</b>
                <small>{{ song.singerName }} · {{ formatPlay(song.playCount) }} 次播放</small>
              </span>
              <span class="new-duration">{{ formatDuration(song.duration) }}</span>
            </router-link>
            <p v-if="!newSongs.length" class="lower-empty">暂无新歌</p>
          </div>
        </section>

        <section class="lower-panel" aria-labelledby="playlist-title">
          <header class="lower-head">
            <h2 id="playlist-title">精选歌单</h2>
            <router-link to="/playlists" class="lower-more">更多 <KmIcon name="chevron-right" :size="12" /></router-link>
          </header>
          <div class="playlist-list">
            <router-link v-for="pl in playlists.slice(0, 2)" :key="pl.id" :to="`/playlist/${pl.id}`" class="playlist-row">
              <img :src="pl.coverUrl || fallbackCover" alt="" @error="onImg" />
              <span class="playlist-copy">
                <b>{{ pl.name }}</b>
                <small>{{ pl.songCount }} 首 · {{ pl.creatorName }}</small>
                <span>{{ pl.description }}</span>
              </span>
              <KmIcon name="chevron-right" :size="14" />
            </router-link>
            <p v-if="!playlists.length" class="lower-empty">暂无公开歌单</p>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { musicApi, playlistApi, liveApi, likeApi } from '@/api'
import { usePlayerStore } from '@/stores/player'
import KmIcon from '@/components/KmIcon.vue'

const router = useRouter()
const player = usePlayerStore()

const hotSongs = ref([])
const newSongs = ref([])
const playlists = ref([])
const liveRooms = ref([])
const banners = ref([])
const failedBannerIds = ref([])
const heroSetting = ref({ heroAutoplay: 1, heroInterval: 5, heroCount: 3 })
const loading = ref(true)
const featuredLiked = ref(false)
const reserved = ref(false)
const heroIndex = ref(0)
const featuredIndex = ref(0)

const fallbackCover = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="300" height="300"><rect width="300" height="300" fill="#f3edf8"/><circle cx="150" cy="150" r="58" fill="none" stroke="#d9cbe8" stroke-width="2"/><circle cx="150" cy="150" r="8" fill="#d9cbe8"/><text x="150" y="258" font-size="16" text-anchor="middle" fill="#9d8cad">KUMAO</text></svg>'
)

const now = new Date()
const festivalDate = new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit' })
  .format(now)
  .replace('/', '.')
  .replace(/^0/, '')
const todayDateLabel = `${now.getMonth() + 1}月${now.getDate()}日`
const weekdayLabel = now.toLocaleDateString('zh-CN', { weekday: 'long' })

const heroSlides = computed(() => [
  {
    key: 'live',
    kicker: `KUMAO LIVE FESTIVAL · ${festivalDate}`,
    title: '音乐直播节',
    subtitle: '和主播一起听歌、点歌、聊天',
    cta: '进入会场',
    event: { label: '即将开始', time: '20:00', title: '陈奕迅 · 线上歌友会', note: '已有 3,842 人预约', action: '预约提醒' }
  },
  {
    key: 'pick',
    kicker: `KUMAO DAILY PICK · ${festivalDate}`,
    title: '今日主打',
    subtitle: '《富士山下》· 把一段旧时光听到透明',
    cta: '播放今日主打',
    event: { label: '正在热播', time: '04:18', title: '陈奕迅 · 富士山下', note: '编辑精选，热度持续上升', action: '立即收听' }
  },
  {
    key: 'vip',
    kicker: `KUMAO VIP · ${festivalDate}`,
    title: '把现场留在耳边',
    subtitle: 'Hi-Res 无损 · 专属弹幕 · 入场特效',
    cta: '查看会员权益',
    event: { label: '会员专享', time: '365 天', title: '酷猫 VIP 年卡', note: '高清 MV 与无损音质随时畅听', action: '查看权益' }
  }
])
const bannerSlides = computed(() => banners.value
  .filter((banner) => banner?.imageUrl && !failedBannerIds.value.includes(banner.id))
  .map((banner, index) => ({
    id: banner.id || `banner-${index}`,
    title: banner.title || '首页活动',
    imageUrl: banner.imageUrl,
    linkUrl: (banner.linkUrl || '').trim()
  })))
const activeBanner = computed(() => bannerSlides.value[heroIndex.value] || bannerSlides.value[0] || null)
const activeHero = computed(() => heroSlides.value[heroIndex.value] || heroSlides.value[0])

let heroTimer = null
let featuredTimer = null
function restartHeroTimer() {
  if (heroTimer) clearInterval(heroTimer)
  if (featuredTimer) clearInterval(featuredTimer)
  heroTimer = null
  featuredTimer = null
  const autoplay = Number(heroSetting.value.heroAutoplay) !== 0
  if (!autoplay) return
  const seconds = Math.max(2, Math.min(15, Number(heroSetting.value.heroInterval) || 5))
  const heroCount = bannerSlides.value.length || heroSlides.value.length
  if (heroCount >= 2) {
    heroTimer = setInterval(() => {
      heroIndex.value = (heroIndex.value + 1) % heroCount
    }, seconds * 1000)
  }
  const featuredCount = heroList.value.length
  if (featuredCount >= 2) {
    featuredTimer = setInterval(() => {
      featuredIndex.value = (featuredIndex.value + 1) % featuredCount
    }, seconds * 1000)
  }
}

function setHeroIndex(index) {
  heroIndex.value = index
  restartHeroTimer()
}

function openBanner(banner) {
  const link = (banner?.linkUrl || '').trim()
  if (!link) return
  if (/^https?:\/\//i.test(link)) {
    window.open(link, '_blank', 'noopener,noreferrer')
    return
  }
  router.push(link.startsWith('/') ? link : `/${link}`)
}

function onBannerError(banner) {
  if (!banner?.id) return
  if (!failedBannerIds.value.includes(banner.id)) {
    failedBannerIds.value = [...failedBannerIds.value, banner.id]
  }
  if (heroIndex.value >= bannerSlides.value.length) heroIndex.value = 0
}

const heroList = computed(() => {
  const configured = Array.isArray(heroSetting.value.songs) ? heroSetting.value.songs : []
  if (configured.length) return configured
  const count = Math.max(1, Math.min(10, Number(heroSetting.value.heroCount) || 3))
  return hotSongs.value.slice(0, count)
})
const featuredSong = computed(() => heroList.value[featuredIndex.value] || heroList.value[0] || hotSongs.value[0] || null)
const featuredId = computed(() => featuredSong.value?.songId || featuredSong.value?.id || null)
const newSongList = computed(() =>
  newSongs.value
    .filter((song) => song?.name && !/未审核|测试/i.test(song.name))
    .slice(0, 4)
)
const featuredYear = computed(() => {
  const raw = featuredSong.value?.releaseDate
  return raw ? String(raw).slice(0, 4) : '2006'
})
const featuredAlbum = computed(() => featuredSong.value?.albumName || 'What’s Going On…?')
const featuredCover = computed(() => coverFor(featuredSong.value))
const featuredDescription = computed(() => {
  const name = featuredSong.value?.name || ''
  const notes = {
    富士山下: '一首关于「放下」的歌。前奏响起的那一刻，2006 年的夏天和山顶的风一起回来了。',
    晴天: '一首关于夏天的歌。前奏一响，操场、晚风和没说出口的话就一起回来了。',
    江南: '当熟悉的旋律响起，江南的雨和旧时光一起落在耳边。',
    泡沫: '像泡沫一样轻盈，也像泡沫一样短暂。留在旋律里的，是仍然滚烫的心事。'
  }
  return notes[name] || '从热歌、新歌和直播现场开始，找到你的下一首循环。'
})
const isFeaturedCurrent = computed(() =>
  !!player.current && !!featuredId.value && String(player.current.id) === String(featuredId.value)
)
const isFeaturedPlaying = computed(() => isFeaturedCurrent.value && player.playing)

const liveRows = computed(() => {
  const rows = liveRooms.value.slice(0, 3).map((room, i) => ({
    key: room.id,
    title: room.title,
    meta: `${room.anchorName || '酷猫主播'} · ${formatNumber(room.realtimeOnline || room.onlineCount || 0)} 人在看`,
    initial: (room.title || '酷').trim().charAt(0),
    tone: ['purple', 'green', 'orange'][i % 3],
    upcoming: false,
    to: `/live/${room.id}`
  }))
  const scheduled = [
    { key: 'scheduled-1', title: '午夜电台 · 点歌专场', meta: '小满 · 22:00 开播', initial: '午', tone: 'green', upcoming: true, to: '/live' },
    { key: 'scheduled-2', title: '民谣现场 · 木吉他弹唱', meta: '阿树 · 23:30 开播', initial: '木', tone: 'orange', upcoming: true, to: '/live' }
  ]
  return rows.concat(scheduled.slice(0, Math.max(0, 3 - rows.length)))
})

function coverFor(song) {
  if (!song) return fallbackCover
  if (String(song.songId || song.id) === '3004') return '/media/fuji-cover.svg'
  return song.coverUrl || fallbackCover
}

function formatNumber(n) {
  return new Intl.NumberFormat('en-US').format(Number(n) || 0)
}

function formatPlay(n) {
  n = Number(n) || 0
  if (n >= 100000000) return `${(n / 100000000).toFixed(1)}亿`
  if (n >= 10000) return `${(n / 10000).toFixed(1)}万`
  return formatNumber(n)
}

function formatDuration(sec) {
  const value = Number(sec) || 0
  return `${String(Math.floor(value / 60)).padStart(2, '0')}:${String(Math.floor(value % 60)).padStart(2, '0')}`
}

function toTrack(song) {
  return {
    id: song.songId || song.id,
    name: song.name,
    singerName: song.singerName,
    coverUrl: coverFor(song),
    audioUrl: song.audioUrl,
    duration: song.duration
  }
}

function isSongPlaying(song) {
  return !!player.current && String(player.current.id) === String(song.songId || song.id) && player.playing
}

function playSong(song, list) {
  if (!song) return
  if (String(player.current?.id) === String(song.songId)) {
    player.toggle()
    return
  }
  const tracks = (list || []).map(toTrack)
  player.play(toTrack(song), tracks.length ? tracks : [toTrack(song)])
}

function toggleFeaturedPlay() {
  if (!featuredSong.value) return
  if (isFeaturedCurrent.value) {
    player.toggle()
    return
  }
  player.play(toTrack(featuredSong.value), hotSongs.value.map(toTrack))
}

function goDetail(songId) {
  if (songId) router.push(`/song/${songId}`)
}

function playAllHot() {
  if (!hotSongs.value.length) return
  player.play(toTrack(hotSongs.value[0]), hotSongs.value.map(toTrack))
}

async function loadLikeStatus() {
  featuredLiked.value = false
  if (!featuredId.value || !localStorage.getItem('km_token')) return
  try {
    const res = await likeApi.status(featuredId.value)
    featuredLiked.value = !!res.data
  } catch (e) {
    featuredLiked.value = false
  }
}

async function toggleFeaturedLike() {
  if (!featuredId.value) return
  if (!localStorage.getItem('km_token')) {
    ElMessage.info('登录后可收藏歌曲')
    return
  }
  try {
    if (featuredLiked.value) {
      await likeApi.unlike(featuredId.value)
      featuredLiked.value = false
      ElMessage.success('已取消收藏')
    } else {
      await likeApi.like(featuredId.value)
      featuredLiked.value = true
      ElMessage.success('已添加到我的音乐')
    }
  } catch (e) {
    /* handled globally */
  }
}

function handleHeroCta() {
  if (!activeHero.value) return
  if (activeHero.value.key === 'pick') {
    toggleFeaturedPlay()
    return
  }
  router.push(activeHero.value.key === 'vip' ? '/vip' : '/live')
}

function handleHeroEvent() {
  if (!activeHero.value) return
  if (activeHero.value.key === 'live') {
    reserved.value = !reserved.value
    ElMessage.success(reserved.value ? '已设置开播提醒' : '已取消预约')
  } else if (activeHero.value.key === 'pick') {
    toggleFeaturedPlay()
  } else {
    router.push('/vip')
  }
}

function onImg(e) {
  if (e.target.src !== fallbackCover) e.target.src = fallbackCover
}

watch(featuredSong, loadLikeStatus)
watch(heroList, (list) => {
  if (featuredIndex.value >= list.length) featuredIndex.value = 0
  restartHeroTimer()
})
watch(bannerSlides, (list) => {
  if (heroIndex.value >= list.length) heroIndex.value = 0
  restartHeroTimer()
})
watch(() => [heroSetting.value.heroAutoplay, heroSetting.value.heroInterval], restartHeroTimer)
onBeforeUnmount(() => {
  if (heroTimer) clearInterval(heroTimer)
  if (featuredTimer) clearInterval(featuredTimer)
})

onMounted(async () => {
  const [h, n, p, l, s, b] = await Promise.allSettled([
    musicApi.hot(12),
    musicApi.newSongs(6),
    playlistApi.publicPage({ pageNum: 1, pageSize: 4, sort: 'hot' }),
    liveApi.square({ pageNum: 1, pageSize: 3 }),
    musicApi.homeSettings(),
    musicApi.banners()
  ])
  if (h.status === 'fulfilled') hotSongs.value = h.value.data || []
  if (n.status === 'fulfilled') newSongs.value = n.value.data || []
  if (p.status === 'fulfilled') playlists.value = p.value.data?.records || []
  if (l.status === 'fulfilled') liveRooms.value = l.value.data?.records || []
  if (s.status === 'fulfilled' && s.value.data) heroSetting.value = s.value.data
  if (b.status === 'fulfilled') banners.value = b.value.data || []
  loading.value = false
  restartHeroTimer()
  if (!player.hasCurrent && featuredSong.value) {
    player.prime(toTrack(featuredSong.value), hotSongs.value.map(toTrack))
  }
  loadLikeStatus()
})
</script>

<style scoped>
.home-page {
  width: 100%;
  max-width: 1480px;
  margin: 0 auto;
  padding: 12px 32px 48px;
}

/* ---------------- 页头 ---------------- */
.home-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 14px;
}
.home-heading { min-width: 0; }
.home-title {
  margin: 0;
  font-size: 31px;
  line-height: 1.18;
  font-weight: 700;
  letter-spacing: -.045em;
  color: #1a1423;
}
.home-sub {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  margin: 9px 0 0;
  color: #776d83;
  font-size: 11.5px;
  line-height: 1.5;
}
.sub-sep { color: #b7abc2; }
.sub-live { display: inline-flex; align-items: center; gap: 6px; }
.sub-live i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #e33a6b;
  box-shadow: 0 0 0 2px #fde8ef;
}
.sub-live.muted i { background: #afa4bb; box-shadow: none; }
.home-actions { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.rank-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  height: 34px;
  padding: 0 6px;
  color: #4d4557;
  font-size: 12px;
  white-space: nowrap;
}
.rank-link:hover { color: #8b3fce; }
.play-all-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 15px;
  border: 1px solid #e8e1ef;
  border-radius: 9px;
  background: #fff;
  color: #2b2334;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 3px 10px rgba(61, 36, 92, .035);
  transition: border-color .16s, background .16s;
}
.play-all-btn:hover:not(:disabled) { border-color: #d9cae9; background: #fdfbff; }
.play-all-btn:disabled { opacity: .45; cursor: not-allowed; }

/* ---------------- 直播节 ---------------- */
.festival-hero {
  position: relative;
  min-height: 177px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 270px;
  align-items: center;
  gap: 28px;
  padding: 22px 34px 20px;
  border-radius: 18px;
  overflow: hidden;
  isolation: isolate;
  color: #fff;
  background:
    radial-gradient(140% 220% at 86% 48%, rgba(235, 64, 180, .45) 0%, transparent 42%),
    linear-gradient(102deg, #32116e 0%, #55178c 47%, #8e1e96 100%);
  box-shadow: 0 14px 38px rgba(80, 26, 119, .13);
}
.festival-hero.hero--pick {
  background:
    radial-gradient(140% 220% at 86% 48%, rgba(244, 114, 182, .36) 0%, transparent 44%),
    linear-gradient(102deg, #2c185f 0%, #5b2b8e 48%, #a33a8e 100%);
}
.festival-hero.hero--vip {
  background:
    radial-gradient(140% 220% at 86% 48%, rgba(251, 191, 36, .24) 0%, transparent 44%),
    linear-gradient(102deg, #3c1c63 0%, #6d2a7d 52%, #ab4b72 100%);
}
.banner-hero {
  display: block;
  min-height: 177px;
  padding: 0;
  cursor: pointer;
  background: #241031;
}
.banner-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: left center;
  transition: transform .5s ease;
}
.banner-hero:hover .banner-bg { transform: scale(1.012); }
.banner-tint {
  position: absolute;
  inset: 0;
  z-index: 1;
  background:
    linear-gradient(90deg, rgba(17, 8, 27, .04) 0%, transparent 48%, rgba(17, 8, 27, .16) 100%),
    linear-gradient(0deg, rgba(17, 8, 27, .22), transparent 34%);
  pointer-events: none;
}
.banner-cta {
  position: absolute;
  right: 21px;
  bottom: 18px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 33px;
  padding: 0 14px;
  border: 1px solid rgba(255, 255, 255, .42);
  border-radius: 8px;
  background: rgba(255, 255, 255, .15);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  backdrop-filter: blur(8px);
  transition: background .16s, border-color .16s;
}
.banner-cta:hover { background: rgba(255, 255, 255, .24); border-color: rgba(255, 255, 255, .62); }
.banner-hero .banner-dots { left: 50%; right: auto; bottom: 14px; transform: translateX(-50%); }
.banner-hero .hero-dot { background: rgba(255, 255, 255, .42); }
.banner-hero .hero-dot.on { background: #fff; }
.hero-orbits {
  position: absolute;
  z-index: -1;
  top: -178px;
  right: 185px;
  width: 560px;
  height: 560px;
  border-radius: 50%;
  background: repeating-radial-gradient(circle, transparent 0 40px, rgba(255, 255, 255, .085) 41px 42px);
  opacity: .8;
  pointer-events: none;
}
.hero-orbits::after {
  content: '';
  position: absolute;
  right: 26px;
  bottom: 26px;
  width: 280px;
  height: 280px;
  border: 1px solid rgba(255, 255, 255, .11);
  border-radius: 50%;
}
.hero-copy { min-width: 0; max-width: 560px; }
.hero-kicker {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 0 0 5px;
  color: rgba(255, 255, 255, .72);
  font-size: 10.5px;
  line-height: 1.4;
  letter-spacing: .16em;
  font-weight: 500;
  white-space: nowrap;
}
.hero-kicker span { width: 20px; height: 1px; background: rgba(255, 255, 255, .5); }
.hero-title {
  margin: 0;
  font-family: 'STZhongsong', 'Songti SC', 'Noto Serif SC', 'SimSun', serif;
  font-size: 38px;
  line-height: 1.15;
  font-weight: 700;
  letter-spacing: .06em;
  color: #fff;
  text-shadow: 0 2px 18px rgba(35, 8, 56, .12);
}
.hero-subtitle {
  margin: 5px 0 0;
  color: rgba(255, 255, 255, .82);
  font-size: 13.5px;
  line-height: 1.5;
  font-weight: 500;
}
.hero-cta {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 35px;
  margin-top: 11px;
  padding: 0 15px;
  border: 1px solid rgba(255, 255, 255, .27);
  border-radius: 8px;
  background: rgba(255, 255, 255, .10);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: background .16s, border-color .16s;
}
.hero-cta:hover { background: rgba(255, 255, 255, .16); border-color: rgba(255, 255, 255, .4); }

.hero-event {
  width: 270px;
  min-height: 131px;
  padding: 14px 16px 13px;
  border: 1px solid rgba(255, 255, 255, .6);
  border-radius: 13px;
  background: rgba(255, 255, 255, .975);
  color: #251c2e;
  box-shadow: 0 12px 30px rgba(31, 8, 48, .13);
}
.event-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #6f657b;
  font-size: 11px;
}
.event-head time { color: #2a2233; font-weight: 600; font-variant-numeric: tabular-nums; }
.hero-event h3 {
  margin: 15px 0 0;
  font-size: 13.5px;
  line-height: 1.35;
  font-weight: 600;
  color: #241c2d;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.hero-event p { margin: 4px 0 0; color: #6f647c; font-size: 11px; line-height: 1.5; }
.event-action {
  width: 100%;
  height: 31px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  margin-top: 9px;
  border: 1px solid #e3cff2;
  border-radius: 7px;
  background: #fcf8ff;
  color: #7335a5;
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: background .16s, border-color .16s;
}
.event-action:hover { background: #f7eeff; border-color: #d3b4eb; }
.event-action.done { color: #0d8a61; border-color: #ccebdd; background: #f5fcf9; }
.hero-dots {
  position: absolute;
  right: 19px;
  bottom: 13px;
  display: flex;
  align-items: center;
  gap: 5px;
}
.hero-dot {
  width: 5px;
  height: 5px;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, .34);
  cursor: pointer;
  transition: width .2s, background .2s;
}
.hero-dot.on { width: 20px; background: #f5e9ff; }

/* ---------------- 主打 + 直播 ---------------- */
.home-stage {
  display: grid;
  grid-template-columns: minmax(0, 1.96fr) minmax(300px, 1fr);
  gap: 16px;
  margin-top: 29px;
  align-items: stretch;
}
.feature-card {
  min-height: 311px;
  display: flex;
  align-items: stretch;
  gap: 26px;
  padding: 24px;
  border: 1px solid #eee9f3;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 26px rgba(61, 36, 92, .04);
}
.feature-cover {
  position: relative;
  width: 238px;
  height: 238px;
  flex: none;
  border-radius: 12px;
  overflow: hidden;
  background: #f4eef8;
  box-shadow: 0 8px 22px rgba(61, 36, 92, .08);
}
.feature-cover img { width: 100%; height: 100%; display: block; object-fit: cover; }
.quality-stamp {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 4px 8px;
  border-radius: 5px;
  background: rgba(35, 26, 45, .66);
  color: rgba(255, 255, 255, .92);
  font-size: 10px;
  line-height: 1.2;
  letter-spacing: .02em;
}
.feature-copy {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 1px 0 0;
}
.feature-pick {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 25px;
  padding: 0 9px;
  border: 1px solid #e5d4f6;
  border-radius: 6px;
  background: #fcf8ff;
  color: #813db3;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
}
.feature-pick .km-icon { color: #a158cf; }
.feature-title {
  margin: 9px 0 0;
  font-size: 31px;
  line-height: 1.2;
  font-weight: 700;
  letter-spacing: -.035em;
  color: #1a1423;
}
.feature-title a:hover { color: #7d2eb5; }
.feature-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  margin: 6px 0 0;
  color: #756a81;
  font-size: 11.5px;
  line-height: 1.55;
}
.meta-dot { color: #bbafc6; }
.feature-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  margin: 11px 0 0;
  color: #6b6177;
  font-size: 11.5px;
  line-height: 1.8;
}
.eq { display: inline-flex; align-items: flex-end; gap: 2px; width: 13px; height: 12px; margin-right: 2px; }
.eq i { display: block; width: 2px; border-radius: 2px; background: linear-gradient(180deg, #e055d4, #8c37cf); }
.eq i:nth-child(1) { height: 5px; }
.eq i:nth-child(2) { height: 10px; }
.eq i:nth-child(3) { height: 7px; }
.eq i:nth-child(4) { height: 12px; }
.tag-dot { color: #b4a8c0; }
.heat-pill {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px 7px;
  border-radius: 5px;
  background: #edf9f3;
  color: #0c8b60;
  font-size: 10px;
  font-weight: 500;
}
.heat-pill span { font-size: 9px; }
.feature-desc {
  margin: 10px 0 0;
  max-width: 420px;
  color: #5e5569;
  font-size: 12.5px;
  line-height: 1.75;
}
.feature-actions { display: flex; align-items: center; gap: 9px; margin-top: auto; padding-top: 14px; }
.feature-play {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: 37px;
  padding: 0 18px;
  border: 1px solid transparent;
  border-radius: 9px;
  background: var(--km-grad);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 5px 14px rgba(168, 54, 201, .2);
  transition: filter .16s, transform .16s;
}
.feature-play:hover { filter: brightness(1.05); transform: translateY(-1px); }
.round-action {
  width: 37px;
  height: 37px;
  border: 1px solid #e9e2f0;
  border-radius: 9px;
  background: #fff;
  color: #6e627b;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color .16s, color .16s, background .16s;
}
.round-action:hover { border-color: #d9c7e9; color: #8b3fce; background: #fdfaff; }
.round-action.liked { color: #e33b6d; border-color: #f3cddd; background: #fff7fa; }
.empty-feature { flex-direction: column; align-items: flex-start; justify-content: center; gap: 8px; color: var(--km-text-faint); font-size: 13px; }
.empty-feature p { margin: 0; }
.text-link { color: var(--km-brand-violet-dark); font-weight: 600; font-size: 12.5px; }

.live-panel {
  min-height: 311px;
  display: flex;
  flex-direction: column;
  padding: 20px 20px 16px;
  border: 1px solid #eee9f3;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 26px rgba(61, 36, 92, .04);
}
.live-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.live-heading { display: flex; align-items: center; gap: 7px; min-width: 0; }
.live-heading h2 { margin: 0; font-size: 15.5px; font-weight: 600; color: #211927; letter-spacing: -.01em; }
.live-heading > span {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 6px;
  border-radius: 5px;
  background: #f4eff8;
  color: #6f657b;
  font-size: 10.5px;
  font-weight: 500;
  white-space: nowrap;
}
.live-more { display: inline-flex; align-items: center; gap: 1px; color: #6f657b; font-size: 11px; }
.live-more:hover { color: #8b3fce; }

.live-list { flex: 1; display: flex; flex-direction: column; margin-top: 8px; }
.live-row {
  display: flex;
  align-items: center;
  gap: 11px;
  min-height: 63px;
  padding: 8px 2px;
  border-bottom: 1px solid #f3eff7;
  color: inherit;
  border-radius: 8px;
  transition: background .16s;
}
.live-row:last-child { border-bottom: 0; }
.live-row:hover { background: #fcf9fe; }
.live-avatar {
  width: 46px;
  height: 46px;
  flex: none;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 17px;
  font-weight: 500;
  box-shadow: 0 5px 12px rgba(74, 41, 101, .12);
}
.tone-purple { background: linear-gradient(135deg, #bd2ed0 0%, #7f2bcc 100%); }
.tone-green { background: linear-gradient(135deg, #31cfa0 0%, #0da67c 100%); }
.tone-orange { background: linear-gradient(135deg, #ffb52b 0%, #f27b1b 100%); }
.live-copy { min-width: 0; flex: 1; display: flex; flex-direction: column; gap: 5px; }
.live-title {
  font-size: 12.5px;
  line-height: 1.35;
  font-weight: 500;
  color: #2a2233;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.live-meta { color: #756b81; font-size: 11px; line-height: 1.35; }
.live-tag {
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  height: 21px;
  padding: 0 7px;
  border: 1px solid #fac5d4;
  border-radius: 5px;
  background: #fff4f7;
  color: #c82254;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: .02em;
}
.live-tag i { width: 4px; height: 4px; border-radius: 50%; background: currentColor; }
.live-tag.upcoming { border-color: #e7dff0; background: #faf7fd; color: #756b81; font-weight: 500; letter-spacing: 0; }
.live-empty { padding: 26px 4px; color: #756b81; font-size: 11px; text-align: center; }
.live-empty a { color: var(--km-brand-violet-dark); }
.live-cta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  height: 38px;
  margin-top: 10px;
  padding: 0 12px;
  border: 1px solid #e9e2f0;
  border-radius: 8px;
  background: #faf8fc;
  color: #4e4559;
  font-size: 11.5px;
  font-weight: 500;
  transition: background .16s, border-color .16s;
}
.live-cta:hover { background: #f7f1fb; border-color: #ddd0eb; color: #2d2437; }

/* ---------------- 今日热歌 ---------------- */
.hot-today { margin-top: 15px; }
.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 11px;
}
.section-title-wrap { display: flex; align-items: baseline; gap: 12px; min-width: 0; }
.section-title-wrap h2 {
  margin: 0;
  font-size: 17.5px;
  line-height: 1.3;
  font-weight: 700;
  letter-spacing: -.02em;
  color: #1e1727;
}
.section-title-wrap p { margin: 0; color: #6f657b; font-size: 11px; white-space: nowrap; }
.section-play {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 34px;
  padding: 0 14px;
  border: 1px solid #e8e1ef;
  border-radius: 8px;
  background: #fff;
  color: #342a3d;
  font-size: 11.5px;
  font-weight: 500;
  cursor: pointer;
  transition: border-color .16s, background .16s;
}
.section-play:hover:not(:disabled) { border-color: #d9c7e8; background: #fdfbff; }
.section-play:disabled { opacity: .45; cursor: not-allowed; }

.song-table { width: 100%; }
.song-table-head,
.song-row {
  display: grid;
  grid-template-columns: 32px 42px minmax(160px, 1.3fr) minmax(88px, .7fr) minmax(130px, .9fr) 48px 68px 24px;
  align-items: center;
  gap: 10px;
}
.song-table-head {
  height: 28px;
  padding: 0 10px;
  color: #756b81;
  font-size: 11px;
  border-bottom: 1px solid #f0ebf5;
}
.song-row {
  min-height: 57px;
  padding: 5px 10px;
  border-radius: 9px;
  cursor: pointer;
  transition: background .16s;
}
.song-row:hover { background: #fbf8fe; }
.song-row:hover .song-play { opacity: 1; transform: scale(1); }
.song-row:focus-visible { outline-offset: -2px; }
.song-rank {
  color: #756b81;
  font-size: 11px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  text-align: center;
}
.song-rank.top { color: #a83bd0; font-weight: 700; }
.song-cover-wrap { position: relative; width: 40px; height: 40px; border-radius: 7px; overflow: hidden; background: #f5eff9; }
.song-cover { width: 100%; height: 100%; display: block; object-fit: cover; }
.song-play {
  position: absolute;
  inset: 0;
  margin: auto;
  width: 25px;
  height: 25px;
  border: 0;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(35, 20, 43, .72);
  color: #fff;
  opacity: 0;
  transform: scale(.82);
  transition: opacity .16s, transform .16s;
  cursor: pointer;
}
.song-main { min-width: 0; display: flex; align-items: center; }
.song-name {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #2a2233;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.song-artist { color: #756b81; font-size: 11px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; min-width: 0; }
.vip-tag {
  display: inline-flex;
  align-items: center;
  height: 14px;
  padding: 0 4px;
  border-radius: 3px;
  background: #fff1ce;
  color: #9b6800;
  font-size: 8px;
  font-weight: 700;
}
.song-album { color: #756b81; font-size: 11px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.song-duration,
.song-plays { color: #756b81; font-size: 11px; font-variant-numeric: tabular-nums; white-space: nowrap; }
.song-more { color: #8d8299; display: flex; justify-content: center; }
.empty-state { padding: 30px; text-align: center; color: #6f657b; font-size: 12px; }
.empty-state p { margin: 0 0 6px; }

/* ---------------- 下方推荐 ---------------- */
.home-lower {
  display: grid;
  grid-template-columns: 1.04fr .96fr;
  gap: 16px;
  margin-top: 30px;
  align-items: start;
}
.lower-panel {
  padding: 18px 20px 14px;
  border: 1px solid #eee9f3;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 26px rgba(61, 36, 92, .035);
}
.lower-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 8px; }
.lower-head h2 { margin: 0; color: #241c2d; font-size: 14.5px; font-weight: 600; }
.lower-more { display: inline-flex; align-items: center; gap: 1px; color: #6f657b; font-size: 11px; }
.lower-more:hover { color: #8b3fce; }
.new-list, .playlist-list { display: flex; flex-direction: column; }
.new-row, .playlist-row {
  display: flex;
  align-items: center;
  gap: 11px;
  min-height: 56px;
  padding: 8px 4px;
  border-bottom: 1px solid #f4f0f8;
  color: inherit;
  border-radius: 7px;
  transition: background .16s;
}
.new-row:last-child, .playlist-row:last-child { border-bottom: 0; }
.new-row:hover, .playlist-row:hover { background: #fcf9fe; }
.new-row img { width: 40px; height: 40px; border-radius: 6px; object-fit: cover; flex: none; }
.new-copy, .playlist-copy { min-width: 0; flex: 1; display: flex; flex-direction: column; gap: 3px; }
.new-copy b, .playlist-copy b { color: #2d2537; font-size: 11.5px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.new-copy small, .playlist-copy small { color: #756b81; font-size: 11px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.new-duration { color: #756b81; font-size: 11px; font-variant-numeric: tabular-nums; }
.playlist-row img { width: 50px; height: 50px; border-radius: 8px; object-fit: cover; flex: none; }
.playlist-copy span { color: #756b81; font-size: 11px; line-height: 1.35; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.playlist-row > .km-icon { color: #8d8299; }
.lower-empty { margin: 0; padding: 22px 0; color: #756b81; text-align: center; font-size: 11px; }

/* ---------------- 骨架 ---------------- */
.home-skeleton { display: flex; flex-direction: column; gap: 16px; }
.sk-head { display: flex; flex-direction: column; gap: 9px; }
.sk-title { width: 145px; height: 30px; border-radius: 7px; background: #efe9f5; }
.sk-line { width: 290px; height: 12px; border-radius: 5px; background: #f3eef7; }
.sk-hero { height: 178px; border-radius: 18px; background: #eee7f6; }
.sk-stage { display: grid; grid-template-columns: minmax(0, 1.96fr) minmax(300px, 1fr); gap: 16px; }
.sk-feature { height: 311px; border-radius: 18px; background: #f1ebf7; }
.sk-side { height: 311px; border-radius: 18px; background: #f1ebf7; }
.sk-row { width: 44%; height: 15px; border-radius: 5px; background: #f0eaf6; }

@media (max-width: 1180px) {
  .home-page { padding: 18px 22px 42px; }
  .home-title { font-size: 28px; }
  .feature-card { gap: 20px; padding: 20px; }
  .feature-cover { width: 210px; height: 210px; }
  .feature-title { font-size: 28px; }
  .hero-title { font-size: 35px; }
}

@media (max-width: 980px) {
  .home-stage { grid-template-columns: 1fr; }
  .live-panel { min-height: 0; }
  .live-list { margin-bottom: 6px; }
  .home-lower { grid-template-columns: 1fr; }
  .sk-stage { grid-template-columns: 1fr; }
  .sk-side { display: none; }
  .hero-orbits { right: 120px; }
}

@media (max-width: 760px) {
  .home-page { padding: 16px 16px 34px; }
  .home-head { align-items: flex-start; flex-direction: column; gap: 13px; margin-bottom: 16px; }
  .home-title { font-size: 25px; }
  .home-sub { margin-top: 7px; font-size: 10.5px; }
  .home-actions { width: 100%; justify-content: space-between; }
  .rank-link { padding-left: 0; }
  .festival-hero { min-height: 0; grid-template-columns: 1fr; gap: 16px; padding: 22px 20px 25px; }
  .banner-hero { min-height: 164px; padding: 0; }
  .banner-hero .banner-cta { display: none; }
  .banner-hero .banner-dots { left: auto; right: 16px; bottom: 12px; transform: none; }
  .home-stage { margin-top: 16px; }
  .hero-title { font-size: 31px; }
  .hero-subtitle { font-size: 12.5px; }
  .hero-event { width: 100%; min-height: 0; }
  .hero-orbits { top: -210px; right: -170px; }
  .feature-card { flex-direction: column; gap: 16px; padding: 16px; border-radius: 16px; }
  .feature-cover { width: 100%; height: auto; aspect-ratio: 1.65; }
  .feature-title { font-size: 26px; }
  .feature-desc { font-size: 12px; }
  .live-panel { padding: 16px; border-radius: 16px; }
  .section-head { align-items: center; }
  .section-title-wrap { flex-direction: column; gap: 3px; }
  .song-table-head { display: none; }
  .song-row { grid-template-columns: 26px 40px minmax(0, 1fr) 42px 22px; min-height: 56px; gap: 9px; padding: 5px 3px; }
  .song-artist, .song-album, .song-plays { display: none; }
  .song-duration { font-size: 9.5px; }
  .home-lower { gap: 12px; margin-top: 24px; }
  .lower-panel { padding: 15px 14px 10px; border-radius: 14px; }
}
</style>
