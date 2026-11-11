<template>
  <div class="home-page">
    <header class="home-head">
      <div class="home-heading">
        <h1 class="home-title">发现音乐</h1>
        <p class="home-sub">
          {{ todayLabel }}
          <span class="sub-dot" aria-hidden="true"></span>
          {{ hotSongs.length }} 首热歌
          <span class="sub-dot" aria-hidden="true"></span>
          {{ liveRooms.length }} 个直播间正在开播
        </p>
      </div>
      <div class="home-actions">
        <button class="home-play" :disabled="!hotSongs.length" @click="playAllHot">
          <el-icon><VideoPlay /></el-icon>
          播放今日热歌
        </button>
        <router-link to="/rank/hot" class="home-link">
          查看榜单
          <el-icon><ArrowRight /></el-icon>
        </router-link>
      </div>
    </header>

    <div v-if="loading" class="home-skeleton" aria-label="首页内容加载中">
      <div class="sk-feature"></div>
      <div class="sk-side">
        <div class="sk-side-line"></div>
        <div class="sk-side-card"></div>
        <div class="sk-side-card"></div>
      </div>
      <div class="sk-row"></div>
    </div>

    <template v-else>
      <AdSlot slot="HOME_BANNER" slot-label="首页 Banner" />
      <section class="home-stage" aria-labelledby="featured-title">
        <article class="feature">
          <img v-if="heroCover" :src="heroCover" alt="" class="feature-bg" @error="onImg" />
          <div class="feature-scrim" aria-hidden="true"></div>
          <div class="feature-topline">
            <span class="feature-label">今日主打</span>
            <span v-if="liveRooms.length" class="feature-live">
              <span class="live-pip" aria-hidden="true"></span>
              {{ liveRooms.length }} 个现场正在发生
            </span>
          </div>

          <div class="feature-copy">
            <h2 id="featured-title" class="feature-title">
              {{ featuredSong?.name || '今天想听点什么？' }}
            </h2>
            <p class="feature-artist">
              {{ featuredSong ? featuredSong.singerName : '从热歌、新歌和直播现场开始，找到你的下一首循环。' }}
            </p>
            <p class="feature-meta">{{ featuredSong ? '编辑精选 · 正在播放' : '编辑精选' }}</p>
            <div class="feature-actions">
              <button v-if="featuredSong" class="feature-play" @click="playSong(featuredSong, hotSongs)">
                <el-icon><VideoPlay /></el-icon>
                立即播放
              </button>
              <router-link v-if="featuredSong" :to="`/song/${featuredSong.songId}`" class="feature-detail">
                查看详情
                <el-icon><ArrowRight /></el-icon>
              </router-link>
              <router-link v-else to="/live" class="feature-detail">
                进入直播广场
                <el-icon><ArrowRight /></el-icon>
              </router-link>
            </div>
          </div>

          <div v-if="featuredSong" class="feature-foot">
            <span class="foot-label">今日热歌</span>
            <span class="foot-note">{{ featuredSong.singerName }} · 全站热度持续上升</span>
          </div>
        </article>

        <aside class="live-side" aria-labelledby="live-side-title">
          <div class="side-head">
            <h2 id="live-side-title" class="side-title">正在直播</h2>
            <router-link to="/live" class="side-more">全部 <el-icon><ArrowRight /></el-icon></router-link>
          </div>

          <div class="side-rooms">
            <router-link
              v-for="room in liveRooms.slice(0, 2)"
              :key="room.id"
              :to="`/live/${room.id}`"
              class="side-room"
              :aria-label="`进入直播间 ${room.title}`"
            >
              <div class="room-cover">
                <img :src="room.coverUrl || fallbackCover" alt="" @error="onImg" />
                <span class="room-live"><i aria-hidden="true"></i>LIVE</span>
              </div>
              <div class="room-copy">
                <span class="room-title">{{ room.title }}</span>
                <span class="room-meta">{{ room.anchorName }} · {{ formatCount(room.realtimeOnline) }} 人在线</span>
              </div>
              <el-icon class="room-arrow"><ArrowRight /></el-icon>
            </router-link>

            <div v-if="!liveRooms.length" class="side-empty">
              <p>今晚还没有开播的房间</p>
              <router-link to="/live">去直播广场看看 <el-icon><ArrowRight /></el-icon></router-link>
            </div>
          </div>

          <div class="side-note">
            <span class="side-note-label">直播现场</span>
            <p>关注主播，开播时第一时间收到提醒；在弹幕里保持友善，和现场一起听歌。</p>
          </div>

          <router-link to="/live" class="side-cta">
            <span class="side-cta-copy">
              <b>打开直播广场</b>
              <small>和主播一起听歌、点歌、聊天</small>
            </span>
            <span class="side-cta-arrow"><el-icon><ArrowRight /></el-icon></span>
          </router-link>
        </aside>
      </section>

      <div class="chips-row" role="navigation" aria-label="快捷分类">
        <button
          v-for="c in quickChips"
          :key="c.label"
          class="km-chip"
          :class="{ active: c.active }"
          @click="c.action()"
        >{{ c.label }}</button>
      </div>

      <section class="km-section hot-section" v-if="hotSongs.length" aria-labelledby="hot-title">
        <div class="section-head">
          <div>
            <h2 id="hot-title" class="section-title">热门推荐</h2>
            <p class="section-sub">全站热度最高的歌曲，按播放量排序</p>
          </div>
          <router-link to="/rank/hot" class="section-more">
            查看全部
            <el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>
        <div class="hot-rail">
          <div
            v-for="(song, i) in hotSongs"
            :key="song.songId"
            class="hot-card"
            role="button"
            tabindex="0"
            :aria-label="`查看 ${song.name} 播放页`"
            @click="goDetail(song.songId)"
            @keydown.enter="goDetail(song.songId)"
            @keydown.space.prevent="goDetail(song.songId)"
          >
            <div class="hot-cover-wrap">
              <img :src="song.coverUrl || fallbackCover" class="hot-cover" alt="" @error="onImg" />
              <span v-if="i < 3" class="hot-rank">第 {{ i + 1 }} 名</span>
              <button class="hot-play" :aria-label="`播放 ${song.name}`" @click.stop="playSong(song, hotSongs)">
                <el-icon><VideoPlay /></el-icon>
              </button>
            </div>
            <div class="hot-name">{{ song.name }}</div>
            <div class="hot-artist">{{ song.singerName }}</div>
            <div class="hot-plays">{{ formatPlay(song.playCount) }} 次播放</div>
          </div>
        </div>
      </section>

      <div class="home-cols">
        <section class="content-panel" aria-labelledby="new-title">
          <div class="panel-head">
            <div>
              <h2 id="new-title" class="section-title">新歌速递</h2>
              <p class="section-sub">刚刚发行的好音乐</p>
            </div>
            <router-link to="/rank/new" class="section-more">更多 <el-icon><ArrowRight /></el-icon></router-link>
          </div>
          <div class="song-list">
            <div
              v-for="(song, i) in newSongs.slice(0, 5)"
              :key="song.songId"
              class="km-row song-row"
              role="button"
              tabindex="0"
              :aria-label="`查看 ${song.name} 播放页`"
              @click="goDetail(song.songId)"
              @keydown.enter="goDetail(song.songId)"
              @keydown.space.prevent="goDetail(song.songId)"
            >
              <span class="km-row-index" :class="{ top: i < 3 }">{{ String(i + 1).padStart(2, '0') }}</span>
              <img :src="song.coverUrl || fallbackCover" class="km-row-cover" alt="" @error="onImg" />
              <div class="km-row-main">
                <div class="km-row-name">
                  {{ song.name }}
                  <span v-if="song.vipOnly" class="km-tag-vip">VIP</span>
                </div>
                <div class="km-row-sub">{{ song.singerName }}</div>
              </div>
              <span class="row-plays">{{ formatPlay(song.playCount) }}</span>
              <button class="row-play-btn" :aria-label="`播放 ${song.name}`" @click.stop="playSong(song, newSongs)">
                <el-icon><VideoPlay /></el-icon>
              </button>
            </div>
            <div v-if="!newSongs.length" class="panel-empty">暂无新歌</div>
          </div>
        </section>

        <section class="content-panel" aria-labelledby="playlist-title">
          <div class="panel-head">
            <div>
              <h2 id="playlist-title" class="section-title">精选歌单</h2>
              <p class="section-sub">为不同心情准备的收听路径</p>
            </div>
            <router-link to="/playlists" class="section-more">更多 <el-icon><ArrowRight /></el-icon></router-link>
          </div>
          <div class="playlist-list">
            <div
              v-for="pl in playlists"
              :key="pl.id"
              class="playlist-row"
              role="button"
              tabindex="0"
              :aria-label="`打开歌单 ${pl.name}`"
              @click="$router.push(`/playlist/${pl.id}`)"
              @keydown.enter="$router.push(`/playlist/${pl.id}`)"
              @keydown.space.prevent="$router.push(`/playlist/${pl.id}`)"
            >
              <div class="playlist-cover"><img :src="pl.coverUrl || fallbackCover" alt="" @error="onImg" /></div>
              <div class="playlist-copy">
                <span class="playlist-name">{{ pl.name }}</span>
                <span class="playlist-meta">{{ pl.songCount }} 首 · {{ pl.creatorName }}</span>
              </div>
              <span class="playlist-open"><el-icon><ArrowRight /></el-icon></span>
            </div>

            <router-link to="/playlists" class="playlist-explore">
              <span class="explore-copy">
                <b>歌单广场</b>
                <small>按心情，找到下一首循环</small>
              </span>
              <span class="explore-tags">
                <span>华语流行</span><span>粤语经典</span><span>电子</span>
              </span>
              <el-icon class="explore-arrow"><ArrowRight /></el-icon>
            </router-link>

            <div v-if="playlists.length < 4" class="genre-block" aria-label="按场景探索歌单">
              <span class="genre-label">按场景探索</span>
              <div class="genre-grid">
                <router-link to="/playlists">通勤路上</router-link>
                <router-link to="/playlists">深夜独处</router-link>
                <router-link to="/playlists">专注工作</router-link>
                <router-link to="/playlists">现场感</router-link>
              </div>
            </div>

            <div v-if="!playlists.length" class="panel-empty">暂无歌单</div>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { musicApi, playlistApi, liveApi } from '@/api'
import AdSlot from '@/components/AdSlot.vue'
import { usePlayerStore } from '@/stores/player'

const router = useRouter()
const player = usePlayerStore()
const banners = ref([])
const hotSongs = ref([])
const newSongs = ref([])
const playlists = ref([])
const liveRooms = ref([])
const loading = ref(true)

const fallbackCover = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="300" height="300"><rect width="300" height="300" fill="#14111b"/><circle cx="150" cy="150" r="66" fill="none" stroke="#2c2538" stroke-width="2"/><circle cx="150" cy="150" r="8" fill="#2c2538"/><text x="150" y="260" font-size="18" text-anchor="middle" fill="#4a4058">KUMAO</text></svg>'
)

const heroCover = computed(() => hotSongs.value[0]?.coverUrl || banners.value[0]?.imageUrl || '')
const featuredSong = computed(() => hotSongs.value[0] || null)
const todayLabel = new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })

const quickChips = [
  { label: '热门', active: true, action: () => router.push('/rank/hot') },
  { label: '新歌', active: false, action: () => router.push('/rank/new') },
  { label: '飙升', active: false, action: () => router.push('/rank/rising') },
  { label: '歌手', active: false, action: () => router.push('/singers') },
  { label: 'MV', active: false, action: () => router.push('/mv') },
  { label: '直播', active: false, action: () => router.push('/live') }
]

function formatCount(n) {
  n = Number(n) || 0
  return n >= 10000 ? `${(n / 10000).toFixed(1)}万` : String(n)
}

function formatPlay(n) {
  if (!n) return '0'
  if (n >= 100000000) return (n / 100000000).toFixed(1) + '亿'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return String(n)
}

function toTrack(s) {
  return { id: s.songId, name: s.name, singerName: s.singerName, coverUrl: s.coverUrl, audioUrl: s.audioUrl, duration: s.duration }
}

function playSong(song, list) {
  const tracks = (list || []).map(toTrack)
  player.play(toTrack(song), tracks.length ? tracks : [toTrack(song)])
}

function goDetail(songId) {
  router.push(`/song/${songId}`)
}

function playAllHot() {
  if (!hotSongs.value.length) return
  player.play(toTrack(hotSongs.value[0]), hotSongs.value.map(toTrack))
}

function onImg(e) {
  e.target.src = fallbackCover
}

onMounted(async () => {
  const [b, h, n, p, l] = await Promise.allSettled([
    musicApi.banners(),
    musicApi.hot(12),
    musicApi.newSongs(10),
    playlistApi.publicPage({ pageNum: 1, pageSize: 5, sort: 'hot' }),
    liveApi.square({ pageNum: 1, pageSize: 4 })
  ])
  if (b.status === 'fulfilled') banners.value = b.value.data || []
  if (h.status === 'fulfilled') hotSongs.value = h.value.data || []
  if (n.status === 'fulfilled') newSongs.value = n.value.data || []
  if (p.status === 'fulfilled') playlists.value = p.value.data?.records || []
  if (l.status === 'fulfilled') liveRooms.value = l.value.data?.records || []
  loading.value = false
})
</script>

<style scoped>
.home-page {
  width: 100%;
  max-width: 1840px;
  margin: 0 auto;
  padding: 24px 34px 48px;
}

.home-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 22px;
}
.home-heading { min-width: 0; }
.home-title {
  margin: 0;
  font-size: 26px;
  line-height: 1.25;
  font-weight: 700;
  letter-spacing: -.04em;
  color: var(--km-text);
}
.home-sub {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  margin: 7px 0 0;
  color: var(--km-text-faint);
  font-size: 12px;
  line-height: 1.5;
}
.sub-dot { width: 2px; height: 2px; border-radius: 50%; background: var(--km-text-faint); opacity: .7; }
.home-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.home-play {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 35px;
  padding: 0 14px;
  border: 1px solid var(--km-accent-line);
  border-radius: 9px;
  background: rgba(216, 180, 254, .09);
  color: #f5eaff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background .16s, border-color .16s;
}
.home-play:hover:not(:disabled) { background: rgba(216, 180, 254, .15); }
.home-play:disabled { opacity: .45; cursor: not-allowed; }
.home-link { display: inline-flex; align-items: center; gap: 3px; height: 35px; padding: 0 8px; color: var(--km-text-dim); font-size: 12px; }
.home-link:hover { color: var(--km-text); }

.home-stage {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
  align-items: stretch;
}
.feature {
  position: relative;
  min-height: 356px;
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid var(--km-border);
  background: #0f0d14;
  isolation: isolate;
}
.feature-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 42%;
  filter: saturate(.85) brightness(.55) blur(1px);
  transform: scale(1.04);
  z-index: -2;
}
.feature-scrim {
  position: absolute;
  inset: 0;
  z-index: -1;
  background:
    linear-gradient(90deg, rgba(8, 7, 11, .96) 0%, rgba(8, 7, 11, .62) 52%, rgba(8, 7, 11, .25) 100%), /* taste-skill-ignore: neutral image scrim */
    linear-gradient(0deg, rgba(8, 7, 11, .8), transparent 55%); /* taste-skill-ignore: neutral image scrim */
}
.feature-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px;
}
.feature-label {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 8px;
  border-radius: 5px;
  background: rgba(255, 255, 255, .07);
  border: 1px solid rgba(255, 255, 255, .09);
  color: var(--km-text-dim);
  font-size: 11px;
}
.feature-live { display: inline-flex; align-items: center; gap: 6px; color: rgba(255, 255, 255, .62); font-size: 11px; }
.live-pip { width: 5px; height: 5px; border-radius: 50%; background: #fb7185; box-shadow: 0 0 0 3px rgba(251, 113, 133, .12); animation: km-pulse 1.6s infinite; }

.feature-copy { padding: 18px 20px 0; max-width: 590px; }
.feature-meta { margin: 7px 0 0; font-size: 12px; color: #e9d5ff; }
.feature-title {
  margin: 0;
  max-width: 520px;
  color: #fff;
  font-size: 32px;
  line-height: 1.18;
  font-weight: 700;
  letter-spacing: -.045em;
  overflow-wrap: anywhere;
}
.feature-artist { margin: 9px 0 0; color: rgba(255, 255, 255, .65); font-size: 12px; line-height: 1.55; max-width: 460px; }
.feature-actions { display: flex; align-items: center; gap: 9px; margin-top: 18px; }
.feature-play {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 15px;
  border: 0;
  border-radius: 9px;
  background: var(--km-grad);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: filter .16s;
}
.feature-play:hover { filter: brightness(1.06); }
.feature-detail { display: inline-flex; align-items: center; gap: 3px; height: 36px; padding: 0 10px; color: rgba(255, 255, 255, .7); font-size: 12px; }
.feature-detail:hover { color: #fff; }
.feature-foot {
  position: absolute;
  left: 20px;
  right: 20px;
  bottom: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(255, 255, 255, .5);
  font-size: 11px;
}
.foot-label { color: rgba(255, 255, 255, .75); font-weight: 600; }
.foot-note { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.live-side {
  display: flex;
  flex-direction: column;
  min-height: 356px;
  padding: 16px;
  border: 1px solid var(--km-border);
  border-radius: 20px;
  background: var(--km-card);
}
.side-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 11px; }
.side-title { margin: 0; font-size: 14px; font-weight: 600; color: var(--km-text); }
.side-more { display: inline-flex; align-items: center; gap: 2px; font-size: 11px; color: var(--km-text-faint); }
.side-more:hover { color: var(--km-text-dim); }
.side-rooms { display: flex; flex-direction: column; gap: 7px; }
.side-room {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 7px;
  border-radius: 10px;
  border: 1px solid transparent;
  transition: border-color .16s, background .16s;
}
.side-room:hover { background: rgba(255, 255, 255, .035); border-color: var(--km-border); }
.room-cover { position: relative; width: 72px; height: 48px; flex-shrink: 0; border-radius: 6px; overflow: hidden; background: #1a1326; }
.room-cover img { width: 100%; height: 100%; object-fit: cover; display: block; }
.room-live { position: absolute; left: 4px; bottom: 4px; display: inline-flex; align-items: center; gap: 3px; height: 15px; padding: 0 4px; border-radius: 3px; background: #9f1239; color: #fff; font-size: 11px; font-weight: 700; }
.room-live i { width: 3px; height: 3px; border-radius: 50%; background: #fff; }
.room-copy { min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.room-title { font-size: 12px; line-height: 1.35; color: var(--km-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.room-meta { font-size: 11px; color: var(--km-text-faint); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.room-arrow { margin-left: auto; color: var(--km-text-faint); font-size: 13px; flex-shrink: 0; }
.side-empty { padding: 24px 8px; text-align: center; color: var(--km-text-faint); font-size: 11px; line-height: 1.6; }
.side-empty p { margin: 0 0 5px; }
.side-empty a { display: inline-flex; align-items: center; gap: 2px; color: var(--km-text-dim); }
.side-note { margin-top: 8px; padding: 10px 11px; border-radius: 8px; border: 1px solid var(--km-border); background: rgba(255, 255, 255, .02); }
.side-note-label { display: block; margin-bottom: 4px; color: var(--km-text-dim); font-size: 13px; font-weight: 600; }
.side-note p { margin: 0; color: var(--km-text-faint); font-size: 13px; line-height: 1.55; }
.side-cta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
  padding: 11px 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, .03);
  border: 1px solid var(--km-border);
  transition: border-color .16s;
}
.side-cta:hover { border-color: var(--km-accent-line); }
.side-cta-copy { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.side-cta-copy b { font-size: 11px; color: var(--km-text-dim); font-weight: 600; }
.side-cta-copy small { font-size: 11px; color: var(--km-text-faint); }
.side-cta-arrow { margin-left: auto; color: var(--km-accent); display: grid; place-items: center; width: 24px; height: 24px; }

.chips-row { display: flex; gap: 8px; flex-wrap: wrap; margin: 24px 0 30px; }
.chips-row .km-chip { height: 30px; padding: 0 13px; font-size: 12px; }

.section-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-bottom: 14px; }
.section-title { margin: 0; font-size: 18px; line-height: 1.3; font-weight: 600; letter-spacing: -.02em; color: var(--km-text); }
.section-sub { margin: 4px 0 0; font-size: 11px; color: var(--km-text-faint); line-height: 1.5; }
.section-more { display: inline-flex; align-items: center; gap: 2px; font-size: 11px; color: var(--km-text-faint); padding: 4px 0; }
.section-more:hover { color: var(--km-text); }

.hot-section { margin-bottom: 34px; }
.hot-rail { display: grid; grid-auto-flow: column; grid-auto-columns: minmax(160px, 1fr); gap: 14px; overflow-x: auto; padding: 2px 2px 8px; scrollbar-width: none; scroll-snap-type: x mandatory; }
.hot-rail::-webkit-scrollbar { display: none; }
.hot-card { scroll-snap-align: start; min-width: 0; border-radius: 12px; cursor: pointer; padding: 7px; transition: background .18s; }
.hot-card:hover { background: rgba(255, 255, 255, .04); }
.hot-card:focus-visible { outline-offset: 1px; }
.hot-cover-wrap { position: relative; border-radius: 9px; overflow: hidden; aspect-ratio: 1; background: #1a1326; }
.hot-cover { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform .3s; }
.hot-card:hover .hot-cover { transform: scale(1.035); }
.hot-rank { position: absolute; top: 7px; left: 7px; padding: 2px 6px; border-radius: 4px; background: rgba(8, 7, 11, .72); color: #f5eaff; font-size: 11px; font-weight: 600; }
.hot-play {
  position: absolute;
  right: 8px;
  bottom: 8px;
  width: 30px;
  height: 30px;
  border: 1px solid rgba(255, 255, 255, .38);
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: transparent;
  color: #fff;
  opacity: 0;
  transform: translateY(4px);
  transition: opacity .18s, transform .18s, border-color .18s;
  cursor: pointer;
}
.hot-card:hover .hot-play { opacity: 1; transform: translateY(0); }
.hot-name { margin-top: 9px; font-size: 13px; font-weight: 600; color: var(--km-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hot-artist { margin-top: 3px; font-size: 11px; color: var(--km-text-dim); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hot-plays { margin-top: 5px; font-size: 11px; color: var(--km-text-faint); font-variant-numeric: tabular-nums; }

.home-cols { display: grid; grid-template-columns: 1.05fr 1fr; gap: 16px; align-items: stretch; }
.content-panel { display: flex; flex-direction: column; min-width: 0; padding: 16px; border: 1px solid var(--km-border); border-radius: 20px; background: var(--km-card); }
.panel-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 8px; }
.song-list, .playlist-list { display: flex; flex-direction: column; gap: 1px; }
.song-row { min-height: 58px; }
.row-plays { font-size: 11px; color: var(--km-text-faint); font-variant-numeric: tabular-nums; white-space: nowrap; }
.row-play-btn { width: 28px; height: 28px; display: grid; place-items: center; border: 1px solid transparent; border-radius: 7px; background: transparent; color: var(--km-text-faint); cursor: pointer; flex-shrink: 0; }
.row-play-btn:hover { color: var(--km-text); border-color: var(--km-border); background: rgba(255, 255, 255, .04); }

.playlist-row { display: flex; align-items: center; gap: 10px; min-height: 63px; padding: 7px 8px; border-radius: 10px; cursor: pointer; transition: background .16s; }
.playlist-row:hover { background: rgba(255, 255, 255, .04); }
.playlist-cover { width: 48px; height: 48px; flex-shrink: 0; border-radius: 7px; overflow: hidden; background: #1a1326; }
.playlist-cover img { width: 100%; height: 100%; object-fit: cover; display: block; }
.playlist-copy { min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.playlist-name { font-size: 12px; font-weight: 500; color: var(--km-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.playlist-meta { font-size: 11px; color: var(--km-text-faint); }
.playlist-open { margin-left: auto; color: var(--km-text-faint); font-size: 12px; }
.playlist-explore { display: flex; align-items: center; gap: 9px; margin-top: 5px; padding: 12px; border-radius: 10px; background: rgba(255, 255, 255, .025); border: 1px solid var(--km-border); transition: border-color .16s; }
.playlist-explore:hover { border-color: var(--km-accent-line); }
.explore-copy { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.explore-copy b { font-size: 11px; color: var(--km-text-dim); font-weight: 600; }
.explore-copy small { font-size: 11px; color: var(--km-text-faint); }
.explore-tags { display: flex; gap: 4px; margin-left: auto; }
.explore-tags span { padding: 3px 6px; border-radius: 4px; background: rgba(255, 255, 255, .04); color: var(--km-text-faint); font-size: 11px; }
.explore-arrow { margin-left: 2px; color: var(--km-accent); font-size: 12px; flex-shrink: 0; }
.genre-block { margin-top: 7px; padding-top: 9px; border-top: 1px solid var(--km-border); }
.genre-label { display: block; margin-bottom: 6px; color: var(--km-text-faint); font-size: 11px; }
.genre-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 5px; }
.genre-grid a { display: flex; align-items: center; min-height: 28px; padding: 0 8px; border-radius: 6px; border: 1px solid var(--km-border); color: var(--km-text-dim); font-size: 11px; transition: border-color .16s, color .16s; }
.genre-grid a:hover { color: var(--km-text); border-color: var(--km-border-strong); }
.panel-empty { padding: 24px 6px; color: var(--km-text-faint); font-size: 11px; text-align: center; }

.home-skeleton { display: grid; grid-template-columns: minmax(0, 1fr) 340px; gap: 16px; }
.sk-feature { height: 356px; border-radius: 20px; background: #0f0d14; border: 1px solid var(--km-border); }
.sk-side { display: flex; flex-direction: column; gap: 8px; padding: 16px; border: 1px solid var(--km-border); border-radius: 20px; background: #0f0d14; }
.sk-side-line { height: 14px; width: 90px; border-radius: 5px; background: rgba(255, 255, 255, .05); }
.sk-side-card { height: 64px; border-radius: 10px; background: rgba(255, 255, 255, .035); }
.sk-row { grid-column: 1 / -1; height: 16px; width: 46%; border-radius: 5px; background: rgba(255, 255, 255, .04); }

/* ---- 可读性层级 ---- */
.home-sub, .section-sub, .section-more, .hot-artist, .hot-plays, .room-meta,
.playlist-meta, .side-more, .room-title, .playlist-name { font-size: 13px; }
.feature-artist, .feature-meta, .foot-note, .side-cta-copy small,
.explore-copy small, .explore-tags span, .genre-label, .genre-grid a { font-size: 12px; }
.hot-plays, .row-plays { font-variant-numeric: tabular-nums; }

@media (max-width: 1180px) {
  .home-page { padding: 22px 20px 42px; }
  .home-stage { grid-template-columns: minmax(0, 1fr) 310px; }
  .feature-title { font-size: 28px; }
  .home-skeleton { grid-template-columns: minmax(0, 1fr) 310px; }
}

@media (max-width: 980px) {
  .home-stage { grid-template-columns: 1fr; }
  .live-side { min-height: 0; }
  .home-cols { grid-template-columns: 1fr; }
  .home-skeleton { grid-template-columns: 1fr; }
  .sk-side { display: none; }
}

@media (max-width: 720px) {
  .home-page { padding: 16px 16px 32px; }
  .home-head { align-items: flex-start; flex-direction: column; gap: 12px; margin-bottom: 16px; }
  .home-title { font-size: 22px; }
  .home-sub { font-size: 11px; }
  .home-actions { width: 100%; }
  .home-play { flex: 1; justify-content: center; height: 34px; }
  .home-link { height: 34px; padding: 0 6px; }
  .feature { min-height: 310px; }
  .feature-topline { padding: 14px 15px; }
  .feature-copy { padding: 12px 15px 0; }
  .feature-title { font-size: 24px; }
  .feature-artist { font-size: 11px; }
  .feature-foot { left: 15px; right: 15px; bottom: 12px; }
  .feature-live { display: none; }
  .chips-row { margin: 18px 0 24px; overflow-x: auto; flex-wrap: nowrap; padding-bottom: 2px; }
  .chips-row .km-chip { flex: none; }
  .hot-rail { grid-auto-columns: 136px; gap: 10px; }
  .hot-name { font-size: 12px; }
  .content-panel { padding: 13px; }
  .section-title { font-size: 16px; }
  .song-row { min-height: 54px; gap: 9px; padding-left: 4px; padding-right: 2px; }
  .row-plays { display: none; }
  .home-cols { gap: 12px; }
}
</style>
