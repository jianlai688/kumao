<template>
  <div class="km-page singer-detail" v-if="singer">
    <!-- 歌手头部 -->
    <div class="singer-hero">
      <div class="hero-glow"></div>
      <div class="hero-grain"></div>

      <div class="hero-inner">
        <div class="avatar-box">
          <div class="avatar-ring"></div>
          <img :src="singer.avatar || fallbackAvatar" class="avatar" @error="onImg" />
          <span class="verified" v-if="singer.status === 1" title="认证歌手">✓</span>
        </div>

        <div class="hero-info">
          <div class="role-line">
            <span class="role-badge">歌手</span>
            <span class="km-faint">{{ singer.region || '华语' }}</span>
          </div>
          <h1 class="singer-name">{{ singer.name }}</h1>
          <div class="stats">
            <div class="stat"><b>{{ formatFans(singer.fansCount) }}</b><span>粉丝</span></div>
            <div class="stat-divider"></div>
            <div class="stat"><b>{{ songs.length }}</b><span>单曲</span></div>
            <div class="stat-divider"></div>
            <div class="stat"><b>{{ totalPlay }}</b><span>累计播放</span></div>
          </div>
          <p class="intro">{{ singer.intro || '这位歌手还没有留下简介。' }}</p>

          <div class="actions">
            <button class="btn-primary" :disabled="!songs.length" @click="playAll">
              <el-icon><VideoPlay /></el-icon>
              <span>播放全部</span>
            </button>
            <button :class="['btn-ghost', { active: followed }]" @click="toggleFollow">
              <el-icon><component :is="followed ? 'Check' : 'Plus'" /></el-icon>
              <span>{{ followed ? '已关注' : '关注' }}</span>
            </button>
            <button class="btn-icon" title="更多"><el-icon><MoreFilled /></el-icon></button>
          </div>
        </div>
      </div>
    </div>

    <!-- 歌曲列表 -->
    <div class="section-head">
      <h2 class="section-title">热门歌曲 <em>{{ songs.length }}</em></h2>
      <span class="km-faint">按播放量排序</span>
    </div>

    <div class="track-panel">
      <div v-for="(song, i) in songs" :key="song.id" :class="['track-row', { playing: player.current?.id === song.id }]" @dblclick="play(song)">
        <div class="t-idx">
          <span class="idx-num">{{ i + 1 }}</span>
          <button class="row-play" @click.stop="play(song)"><el-icon><VideoPlay /></el-icon></button>
        </div>

        <div class="cover-wrap mini-cover" @click="$router.push(`/song/${song.id}`)">
          <img :src="song.coverUrl || fallbackCover" @error="onImg" />
        </div>

        <div class="t-main" @click="$router.push(`/song/${song.id}`)">
          <div class="t-name">
            {{ song.name }}
            <span v-if="song.vipOnly" class="km-tag-vip mini">VIP</span>
          </div>
          <div class="t-sub">
            {{ song.albumName || '单曲' }}
            <template v-if="song.language"> · {{ song.language }}</template>
            <template v-if="song.style"> · {{ song.style }}</template>
          </div>
        </div>

        <div class="t-plays">{{ formatPlay(song.playCount) }} 次播放</div>

        <div class="t-act">
          <button class="mini-btn" title="播放" @click.stop="play(song)"><el-icon><VideoPlay /></el-icon></button>
          <button class="mini-btn" title="收藏"><el-icon><Star /></el-icon></button>
        </div>
      </div>

      <div v-if="!songs.length" class="empty-box">
        <div class="empty-ico">🎤</div>
        <p>这位歌手还没有发布歌曲</p>
      </div>
    </div>
  </div>

  <div v-else class="km-page"><el-skeleton :rows="8" animated /></div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VideoPlay, Plus, Check, Star, MoreFilled } from '@element-plus/icons-vue'
import { musicApi } from '@/api'
import { usePlayerStore } from '@/stores/player'

const route = useRoute()
const player = usePlayerStore()
const singer = ref(null)
const songs = ref([])
const followed = ref(false)

const fallbackAvatar = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="240" height="240"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#2a2d35"/><stop offset="1" stop-color="#131418"/></linearGradient></defs><rect width="240" height="240" fill="url(#g)"/><circle cx="120" cy="96" r="38" fill="#3d424c"/><path d="M42 240c0-43 35-78 78-78s78 35 78 78z" fill="#3d424c"/></svg>'
)
const fallbackCover = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#22252c"/><stop offset="1" stop-color="#101114"/></linearGradient></defs><rect width="200" height="200" fill="url(#g)"/><text x="100" y="118" font-size="46" text-anchor="middle" fill="#3a3e47">♪</text></svg>'
)

const totalPlay = computed(() => {
  const n = songs.value.reduce((a, s) => a + (s.playCount || 0), 0)
  return formatPlay(n)
})

function onImg(e) {
  e.target.src = e.target.classList.contains('avatar') ? fallbackAvatar : fallbackCover
}
function formatFans(n) {
  if (!n) return '0'
  if (n >= 100000000) return (n / 100000000).toFixed(1) + '亿'
  return n >= 10000 ? (n / 10000).toFixed(1) + '万' : String(n)
}
function formatPlay(n) {
  if (!n) return '0'
  if (n >= 100000000) return (n / 100000000).toFixed(1) + '亿'
  return n >= 10000 ? (n / 10000).toFixed(1) + '万' : String(n)
}
function toggleFollow() {
  followed.value = !followed.value
  ElMessage.success(followed.value ? '已关注该歌手' : '已取消关注')
}
function play(song) {
  player.play(
    { id: song.id, name: song.name, singerName: song.singerName, coverUrl: song.coverUrl, audioUrl: song.audioUrl, duration: song.duration },
    songs.value.map((s) => ({ id: s.id, name: s.name, singerName: s.singerName, coverUrl: s.coverUrl, audioUrl: s.audioUrl, duration: s.duration }))
  )
}
function playAll() {
  if (songs.value.length) play(songs.value[0])
}

onMounted(async () => {
  const [s, list] = await Promise.all([musicApi.singer(route.params.id), musicApi.singerSongs(route.params.id)])
  singer.value = s.data
  songs.value = list.data || []
})
</script>

<style scoped>
.singer-detail { padding-top: 0 !important; }

/* ---------------- Hero ---------------- */
.singer-hero {
  position: relative;
  margin: -8px -6px 34px;
  padding: 46px 40px;
  border-radius: 28px;
  overflow: hidden;
  background:
    radial-gradient(700px 400px at 10% 0%, rgba(192, 132, 252, .14), transparent 60%),
    radial-gradient(600px 400px at 90% 100%, rgba(96, 190, 255, .08), transparent 60%),
    linear-gradient(140deg, #14161a, #0a0b0d 70%);
  border: 1px solid var(--km-border);
}
.hero-glow {
  position: absolute;
  width: 420px;
  height: 420px;
  left: -120px;
  top: -140px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(192, 132, 252, .12), transparent 65%);
  filter: blur(30px);
  pointer-events: none;
}
.hero-grain {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 55%, rgba(5, 5, 6, .55));
  pointer-events: none;
}

.hero-inner { position: relative; display: flex; gap: 34px; align-items: center; }

.avatar-box { position: relative; flex-shrink: 0; }
.avatar-ring {
  position: absolute;
  inset: -10px;
  border-radius: 50%;
  border: 1px solid rgba(192, 132, 252, .22);
  box-shadow: 0 0 40px rgba(192, 132, 252, .12);
}
.avatar {
  width: 168px;
  height: 168px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
  border: 2px solid rgba(255, 255, 255, .14);
  box-shadow: 0 24px 50px rgba(0, 0, 0, .55);
}
.verified {
  position: absolute;
  right: 6px;
  bottom: 6px;
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--km-accent);
  color: #07080a;
  font-size: 14px;
  font-weight: 900;
  border: 3px solid #0b0c0f;
}

.hero-info { flex: 1; min-width: 0; }
.role-line { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; font-size: 12px; }
.role-badge {
  height: 22px;
  padding: 0 9px;
  display: inline-flex;
  align-items: center;
  border-radius: 6px;
  background: rgba(192, 132, 252, .14);
  border: 1px solid rgba(192, 132, 252, .3);
  color: var(--km-accent);
  font-size: 11px;
  font-weight: 800;
}

.singer-name {
  margin: 0 0 16px;
  font-size: 44px;
  font-weight: 900;
  letter-spacing: -1.6px;
  line-height: 1.1;
}

.stats { display: flex; align-items: center; gap: 22px; margin-bottom: 16px; }
.stat { display: flex; align-items: baseline; gap: 6px; }
.stat b { font-size: 18px; font-weight: 800; letter-spacing: -.4px; }
.stat span { font-size: 11px; color: var(--km-text-faint); }
.stat-divider { width: 1px; height: 14px; background: rgba(255, 255, 255, .12); }

.intro {
  margin: 0;
  max-width: 680px;
  font-size: 13px;
  line-height: 1.72;
  color: var(--km-text-dim);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.actions { display: flex; align-items: center; gap: 10px; margin-top: 22px; flex-wrap: wrap; }
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 46px;
  padding: 0 30px;
  border: none;
  border-radius: 999px;
  background: var(--km-accent);
  color: #07080a;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: all .2s;
  box-shadow: 0 10px 26px rgba(192, 132, 252, .25);
}
.btn-primary:hover:not(:disabled) { background: #d8b4fe; transform: translateY(-1px); }
.btn-primary:disabled { opacity: .45; cursor: not-allowed; }
.btn-ghost {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 46px;
  padding: 0 22px;
  border-radius: 999px;
  border: 1px solid var(--km-border-strong);
  background: rgba(255, 255, 255, .05);
  color: var(--km-text);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all .2s;
}
.btn-ghost:hover { background: rgba(255, 255, 255, .1); border-color: rgba(255, 255, 255, .28); }
.btn-ghost.active { color: var(--km-accent); border-color: var(--km-accent-line); background: rgba(192, 132, 252, .1); }
.btn-icon {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid var(--km-border);
  background: rgba(255, 255, 255, .04);
  color: var(--km-text-dim);
  cursor: pointer;
  transition: all .2s;
}
.btn-icon:hover { color: var(--km-text); border-color: var(--km-border-strong); }

/* ---------------- 列表 ---------------- */
.section-head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 16px; }
.section-title { margin: 0; font-size: 22px; font-weight: 900; letter-spacing: -.6px; }
.section-title em { font-style: normal; font-size: 13px; color: var(--km-text-faint); font-weight: 500; margin-left: 8px; }

.track-panel { background: var(--km-card); border: 1px solid var(--km-border); border-radius: var(--km-radius); padding: 10px; }

.track-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 9px 14px;
  border-radius: 12px;
  transition: background .16s;
  cursor: pointer;
}
.track-row:hover { background: rgba(255, 255, 255, .05); }
.track-row:hover .idx-num { opacity: 0; }
.track-row:hover .row-play { opacity: 1; transform: scale(1); }
.track-row.playing .t-name { color: var(--km-accent); }

.t-idx { width: 30px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; position: relative; }
.idx-num { font-size: 12px; color: var(--km-text-faint); font-variant-numeric: tabular-nums; }
.row-play {
  position: absolute;
  inset: 0;
  margin: auto;
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  border: 1px solid var(--km-border-strong);
  border-radius: 50%;
  background: rgba(255, 255, 255, .08);
  color: var(--km-text);
  cursor: pointer;
  opacity: 0;
  transform: scale(.8);
  transition: all .18s;
  font-size: 11px;
}
.row-play:hover { background: var(--km-accent); color: #07080a; border-color: var(--km-accent); }

.mini-cover { width: 44px; height: 44px; border-radius: 10px; overflow: hidden; flex-shrink: 0; }
.mini-cover img { width: 100%; height: 100%; object-fit: cover; display: block; }

.t-main { flex: 1; min-width: 0; }
.t-name {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13.5px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.km-tag-vip.mini {
  height: 17px;
  padding: 0 6px;
  font-size: 9px;
  letter-spacing: .3px;
  display: inline-flex;
  align-items: center;
}
.t-sub { font-size: 11.5px; color: var(--km-text-faint); margin-top: 3px; }

.t-plays { font-size: 12px; color: var(--km-text-faint); flex-shrink: 0; }

.t-act { display: flex; gap: 4px; flex-shrink: 0; }
.mini-btn {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid transparent;
  background: transparent;
  color: var(--km-text-faint);
  cursor: pointer;
  font-size: 12px;
  transition: all .16s;
}
.mini-btn:hover { color: var(--km-text); border-color: var(--km-border-strong); background: rgba(255, 255, 255, .06); }

.empty-box { padding: 60px 20px; text-align: center; }
.empty-ico { font-size: 34px; opacity: .5; margin-bottom: 10px; }
.empty-box p { margin: 0; color: var(--km-text-dim); font-size: 13px; }

@media (max-width: 860px) {
  .hero-inner { flex-direction: column; align-items: flex-start; gap: 22px; }
  .singer-hero { padding: 28px 20px; }
  .avatar { width: 120px; height: 120px; }
  .singer-name { font-size: 30px; }
  .t-plays { display: none; }
}
</style>
