<template>
  <div class="km-page musician-page" v-if="data">
    <!-- 音乐人主页头图 -->
    <div class="m-hero">
      <div class="hero-cover" :style="{ backgroundImage: `url('${data.musician.coverUrl || data.musician.avatar || fallbackCover}')` }"></div>
      <div class="hero-mask"></div>
      <div class="hero-grain"></div>

      <div class="hero-inner">
        <div class="avatar-box">
          <div class="avatar-ring"></div>
          <img :src="data.musician.avatar || fallbackAvatar" class="avatar" @error="onImg" />
          <span class="badge-musician" title="认证音乐人">🎸</span>
        </div>

        <div class="hero-info">
          <div class="role-line">
            <span class="role-badge">音乐人</span>
            <span v-if="data.musician.style" class="style-text">{{ data.musician.style }}</span>
          </div>
          <h1 class="stage-name">{{ data.musician.stageName }}</h1>

          <div class="stats">
            <div class="stat"><b>{{ formatNum(data.musician.fansCount) }}</b><span>粉丝</span></div>
            <div class="stat-divider"></div>
            <div class="stat"><b>{{ formatNum(data.musician.totalPlayCount) }}</b><span>播放</span></div>
            <div class="stat-divider"></div>
            <div class="stat"><b>{{ (data.works || []).length }}</b><span>作品</span></div>
          </div>

          <p class="intro">{{ data.musician.intro || '这位音乐人还没有留下简介。' }}</p>

          <div class="actions">
            <button class="btn-primary" :disabled="!data.works?.length" @click="playAll">
              <el-icon><VideoPlay /></el-icon>
              <span>播放全部</span>
            </button>
            <button :class="['btn-ghost', { active: data.followed }]" @click="toggleFollow">
              <el-icon><component :is="data.followed ? 'Check' : 'Plus'" /></el-icon>
              <span>{{ data.followed ? '已关注' : '关注' }}</span>
            </button>
            <button class="btn-icon" title="分享"><el-icon><Share /></el-icon></button>
          </div>
        </div>
      </div>
    </div>

    <div class="two-col">
      <!-- 作品 -->
      <section>
        <div class="section-head">
          <h2 class="section-title">作品 <em>{{ (data.works || []).length }}</em></h2>
          <span class="km-faint">双击播放</span>
        </div>

        <div class="panel">
          <div
            v-for="(w, i) in data.works"
            :key="w.id"
            :class="['track-row', { playing: player.current?.id === (w.songId || w.id) }]"
            @dblclick="play(w)"
          >
            <div class="t-idx">
              <span class="idx-num">{{ i + 1 }}</span>
              <button class="row-play" @click.stop="play(w)"><el-icon><VideoPlay /></el-icon></button>
            </div>
            <div class="mini-cover" @click="$router.push(`/song/${w.songId || w.id}`)">
              <img :src="w.coverUrl || fallbackCover" @error="onImg" />
            </div>
            <div class="t-main" @click="$router.push(`/song/${w.songId || w.id}`)">
              <div class="t-name">
                {{ w.name }}
                <span v-if="w.vipOnly" class="km-tag-vip mini">VIP</span>
              </div>
              <div class="t-sub">{{ w.albumName || '单曲' }} · {{ formatNum(w.playCount) }} 次播放</div>
            </div>
            <div class="t-act">
              <button class="mini-btn" @click.stop="play(w)"><el-icon><VideoPlay /></el-icon></button>
            </div>
          </div>

          <div v-if="!data.works?.length" class="empty-box">
            <div class="empty-ico">🎵</div>
            <p>暂无作品</p>
          </div>
        </div>
      </section>

      <!-- 专辑 -->
      <section>
        <div class="section-head">
          <h2 class="section-title">专辑 <em>{{ (data.albums || []).length }}</em></h2>
        </div>

        <div class="album-grid">
          <div v-for="a in data.albums" :key="a.id" class="album-card km-hover-card">
            <div class="cover-wrap album-cover">
              <img :src="a.coverUrl || fallbackCover" @error="onImg" />
              <div class="play-mask"><el-icon><VideoPlay /></el-icon></div>
            </div>
            <div class="album-name">{{ a.name }}</div>
            <div class="album-sub">{{ a.releaseDate }} · {{ a.songCount }} 首</div>
            <div class="album-intro">{{ a.intro }}</div>
          </div>

          <div v-if="!data.albums?.length" class="empty-box wide">
            <div class="empty-ico">💿</div>
            <p>暂无专辑</p>
          </div>
        </div>
      </section>
    </div>
  </div>

  <div v-else class="km-page"><el-skeleton :rows="8" animated /></div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VideoPlay, Plus, Check, Share } from '@element-plus/icons-vue'
import { musicianApi } from '@/api'
import { usePlayerStore } from '@/stores/player'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const player = usePlayerStore()
const userStore = useUserStore()
const data = ref(null)

const fallbackAvatar = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="240" height="240"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#2a2d35"/><stop offset="1" stop-color="#131418"/></linearGradient></defs><rect width="240" height="240" fill="url(#g)"/><circle cx="120" cy="96" r="38" fill="#3d424c"/><path d="M42 240c0-43 35-78 78-78s78 35 78 78z" fill="#3d424c"/></svg>'
)
const fallbackCover = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="300" height="300"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#22252c"/><stop offset="1" stop-color="#101114"/></linearGradient></defs><rect width="300" height="300" fill="url(#g)"/><text x="150" y="170" font-size="64" text-anchor="middle" fill="#33363e">♪</text></svg>'
)

function onImg(e) {
  e.target.src = e.target.classList.contains('avatar') ? fallbackAvatar : fallbackCover
}

function formatNum(n) {
  if (!n) return '0'
  if (n >= 100000000) return (n / 100000000).toFixed(1) + '亿'
  return n >= 10000 ? (n / 10000).toFixed(1) + '万' : String(n)
}

async function load() {
  const res = await musicianApi.homepage(route.params.id)
  data.value = res.data
}

function play(w) {
  player.play(
    { id: w.songId || w.id, name: w.name, singerName: data.value.musician.stageName, coverUrl: w.coverUrl, audioUrl: w.audioUrl, duration: w.duration },
    data.value.works.map((x) => ({ id: x.songId || x.id, name: x.name, singerName: data.value.musician.stageName, coverUrl: x.coverUrl, audioUrl: x.audioUrl, duration: x.duration }))
  )
}
function playAll() {
  if (data.value.works?.length) play(data.value.works[0])
}

async function toggleFollow() {
  if (!userStore.isLogin) return
  if (data.value.followed) {
    await musicianApi.unfollow(route.params.id)
    ElMessage.success('已取消关注')
  } else {
    await musicianApi.follow(route.params.id)
    ElMessage.success('关注成功')
  }
  await load()
}

onMounted(load)
</script>

<style scoped>
.musician-page { padding-top: 0 !important; }

/* ---------------- Hero ---------------- */
.m-hero {
  position: relative;
  margin: -8px -6px 34px;
  padding: 46px 40px;
  border-radius: 28px;
  overflow: hidden;
  border: 1px solid var(--km-border);
  background: #0c0d10;
}
.hero-cover {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  filter: blur(64px) saturate(180%) brightness(.42);
  transform: scale(1.3);
}
.hero-mask {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(5, 5, 6, .3), rgba(5, 5, 6, .88)),
    radial-gradient(680px 340px at 10% 0%, rgba(192, 132, 252, .14), transparent 62%);
}
.hero-grain {
  position: absolute;
  inset: 0;
  opacity: .5;
  background-image: radial-gradient(rgba(255, 255, 255, .05) 1px, transparent 1px);
  background-size: 5px 5px;
  pointer-events: none;
}
.hero-inner { position: relative; display: flex; gap: 34px; align-items: center; }

.avatar-box { position: relative; flex-shrink: 0; }
.avatar-ring {
  position: absolute;
  inset: -10px;
  border-radius: 50%;
  border: 1px solid rgba(192, 132, 252, .22);
  box-shadow: 0 0 44px rgba(192, 132, 252, .14);
}
.avatar {
  width: 160px;
  height: 160px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
  border: 2px solid rgba(255, 255, 255, .14);
  box-shadow: 0 24px 50px rgba(0, 0, 0, .55);
}
.badge-musician {
  position: absolute;
  right: 4px;
  bottom: 4px;
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #0d0e11;
  border: 2px solid var(--km-accent);
  font-size: 14px;
}

.hero-info { flex: 1; min-width: 0; }
.role-line { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
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
.style-text { font-size: 12px; color: var(--km-text-dim); }

.stage-name {
  margin: 0 0 16px;
  font-size: 42px;
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
  max-width: 660px;
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

/* ---------------- 两栏 ---------------- */
.two-col { display: grid; grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr); gap: 26px; align-items: start; }
.section-head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 14px; }
.section-title { margin: 0; font-size: 20px; font-weight: 900; letter-spacing: -.5px; }
.section-title em { font-style: normal; font-size: 12.5px; color: var(--km-text-faint); font-weight: 500; margin-left: 8px; }

.panel { background: var(--km-card); border: 1px solid var(--km-border); border-radius: var(--km-radius); padding: 10px; }

.track-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 12px;
  transition: background .16s;
  cursor: pointer;
}
.track-row:hover { background: rgba(255, 255, 255, .05); }
.track-row:hover .idx-num { opacity: 0; }
.track-row:hover .row-play { opacity: 1; transform: scale(1); }
.track-row.playing .t-name { color: var(--km-accent); }

.t-idx { width: 26px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; position: relative; }
.idx-num { font-size: 12px; color: var(--km-text-faint); font-variant-numeric: tabular-nums; }
.row-play {
  position: absolute;
  inset: 0;
  margin: auto;
  width: 24px;
  height: 24px;
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
  font-size: 10px;
}
.row-play:hover { background: var(--km-accent); color: #07080a; border-color: var(--km-accent); }

.mini-cover { width: 42px; height: 42px; border-radius: 9px; overflow: hidden; flex-shrink: 0; }
.mini-cover img { width: 100%; height: 100%; object-fit: cover; display: block; }

.t-main { flex: 1; min-width: 0; }
.t-name { display: flex; align-items: center; gap: 7px; font-size: 13.5px; font-weight: 600; }
.km-tag-vip.mini { height: 17px; padding: 0 6px; font-size: 9px; display: inline-flex; align-items: center; }
.t-sub { font-size: 11.5px; color: var(--km-text-faint); margin-top: 3px; }

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

.album-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 16px; }
.album-card {
  background: var(--km-card);
  border: 1px solid var(--km-border);
  border-radius: 16px;
  padding: 12px;
  transition: all .2s;
}
.album-card:hover { border-color: var(--km-border-strong); transform: translateY(-3px); }
.album-cover { width: 100%; aspect-ratio: 1; border-radius: 12px; }
.album-name { margin-top: 10px; font-size: 13px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.album-sub { font-size: 11px; color: var(--km-text-faint); margin: 4px 0 6px; }
.album-intro {
  font-size: 11px;
  color: var(--km-text-dim);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.empty-box { padding: 56px 20px; text-align: center; }
.empty-box.wide { grid-column: 1 / -1; }
.empty-ico { font-size: 32px; opacity: .5; margin-bottom: 10px; }
.empty-box p { margin: 0; color: var(--km-text-dim); font-size: 13px; }

@media (max-width: 980px) {
  .two-col { grid-template-columns: 1fr; }
  .hero-inner { flex-direction: column; align-items: flex-start; gap: 22px; }
  .m-hero { padding: 28px 20px; }
  .avatar { width: 118px; height: 118px; }
  .stage-name { font-size: 30px; }
}
</style>
