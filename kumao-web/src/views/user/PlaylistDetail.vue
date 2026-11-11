<template>
  <div class="km-page pl-detail" v-if="playlist">
    <!-- 顶部氛围头图 -->
    <div class="pl-hero">
      <div class="hero-bg" :style="{ backgroundImage: `url('${playlist.coverUrl || fallbackCover}')` }"></div>
      <div class="hero-mask"></div>

      <div class="hero-inner">
        <div class="cover-box">
          <img :src="playlist.coverUrl || fallbackCover" @error="onImg" />
          <span class="cover-badge">{{ playlist.isPublic ? '公开歌单' : '私有歌单' }}</span>
        </div>

        <div class="hero-info">
          <div class="crumb">
            <span class="km-faint">歌单</span>
            <el-icon class="km-faint"><ArrowRight /></el-icon>
            <span class="km-faint">{{ playlist.isPublic ? '公开' : '私有' }}</span>
          </div>

          <h1 class="pl-name">{{ playlist.name }}</h1>
          <p class="pl-desc">{{ playlist.description || '这个歌单还没有简介～' }}</p>

          <div class="meta-line">
            <span class="creator">
              <el-avatar :size="22" :src="playlist.creatorAvatar">{{ (playlist.creatorName || 'U').slice(0, 1) }}</el-avatar>
              {{ playlist.creatorName || '未知用户' }}
            </span>
            <span class="dot-sep">·</span>
            <span>{{ playlist.songCount || playlist.songs?.length || 0 }} 首</span>
            <span class="dot-sep">·</span>
            <span>{{ playlist.collectCount || 0 }} 人收藏</span>
          </div>

          <div class="actions">
            <button class="btn-primary" :disabled="!playlist.songs?.length" @click="playAll">
              <el-icon><VideoPlay /></el-icon>
              <span>播放全部</span>
            </button>
            <button class="btn-ghost" :class="{ active: playlist.collected }" @click="toggleCollect">
              <el-icon><component :is="playlist.collected ? 'StarFilled' : 'Star'" /></el-icon>
              <span>{{ playlist.collected ? '已收藏' : '收藏歌单' }}</span>
            </button>
            <template v-if="isOwner">
              <button class="btn-ghost" @click="editDialog = true"><el-icon><Edit /></el-icon><span>编辑</span></button>
              <button class="btn-icon danger" title="删除歌单" @click="removePlaylist"><el-icon><Delete /></el-icon></button>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- 歌曲列表 -->
    <div class="track-panel">
      <div class="list-head">
        <span class="col-idx">#</span>
        <span class="col-title">标题</span>
        <span class="col-dur">时长</span>
        <span class="col-act"></span>
      </div>

      <div
        v-for="(row, i) in playlist.songs || []"
        :key="row.songId"
        :class="['track-row', { playing: player.current?.id === row.songId }]"
        @dblclick="playRow(row)"
      >
        <div class="col-idx">
          <span class="idx-num">{{ i + 1 }}</span>
          <button class="row-play" @click.stop="playRow(row)">
            <el-icon><VideoPlay /></el-icon>
          </button>
        </div>

        <div class="col-title">
          <div class="cover-wrap mini-cover" @click="$router.push(`/song/${row.songId}`)">
            <img :src="row.coverUrl || fallbackCover" @error="onImg" />
          </div>
          <div class="t-main" @click="$router.push(`/song/${row.songId}`)">
            <div class="t-name">{{ row.songName }}</div>
            <div class="t-sub">{{ row.singerName }}</div>
          </div>
        </div>

        <div class="col-dur">{{ fmt(row.duration) }}</div>

        <div class="col-act">
          <button class="mini-btn" @click.stop="playRow(row)"><el-icon><VideoPlay /></el-icon></button>
          <button v-if="isOwner" class="mini-btn danger" title="从歌单移除" @click.stop="removeSong(row)">
            <el-icon><Delete /></el-icon>
          </button>
        </div>
      </div>

      <div v-if="!playlist.songs?.length" class="empty-box">
        <div class="empty-ico">🎵</div>
        <p>歌单还是空的，去发现更多好音乐吧</p>
        <button class="btn-primary sm" @click="$router.push('/')">去发现音乐</button>
      </div>
    </div>

    <el-dialog v-model="editDialog" title="编辑歌单" width="440px">
      <div class="dlg-form">
        <label class="f-label">歌单名称</label>
        <el-input v-model="editForm.name" placeholder="给歌单起个名字" />
        <label class="f-label">封面</label>
        <MediaDropzone
          v-model="editForm.coverUrl"
          kind="cover"
          biz-type="cover"
          title="上传歌单封面"
          hint="JPG / PNG / WebP / GIF · 最大 10MB"
          pick-label="从文件夹选择"
          :max-size-mb="10"
        />
        <el-input v-model="editForm.coverUrl" placeholder="或填写封面图片地址" style="margin-top:8px" />
        <label class="f-label">简介</label>
        <el-input v-model="editForm.description" type="textarea" :rows="3" resize="none" placeholder="介绍一下这个歌单" />
        <label class="f-label">可见性</label>
        <el-radio-group v-model="editForm.isPublic">
          <el-radio-button :value="1">公开</el-radio-button>
          <el-radio-button :value="0">私有</el-radio-button>
        </el-radio-group>
      </div>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>

  <div v-else class="km-page"><el-skeleton :rows="8" animated /></div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, VideoPlay, Star, StarFilled, Edit, Delete } from '@element-plus/icons-vue'
import { playlistApi } from '@/api'
import { usePlayerStore } from '@/stores/player'
import { useUserStore } from '@/stores/user'
import MediaDropzone from '@/components/studio/MediaDropzone.vue'

const route = useRoute()
const router = useRouter()
const player = usePlayerStore()
const userStore = useUserStore()

const playlist = ref(null)
const editDialog = ref(false)
const editForm = reactive({ name: '', coverUrl: '', description: '', isPublic: 1 })

const fallbackCover = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#22252c"/><stop offset="1" stop-color="#101114"/></linearGradient></defs><rect width="400" height="400" fill="url(#g)"/><text x="200" y="228" font-size="88" text-anchor="middle" fill="#3a3e47">♪</text></svg>'
)

const isOwner = computed(() => userStore.user?.userId && playlist.value?.userId === userStore.user.userId)

function onImg(e) {
  e.target.src = fallbackCover
}

async function load() {
  const res = await playlistApi.detail(route.params.id)
  playlist.value = res.data
  Object.assign(editForm, {
    name: playlist.value.name,
    coverUrl: playlist.value.coverUrl,
    description: playlist.value.description,
    isPublic: playlist.value.isPublic
  })
}

function playRow(row) {
  player.play(
    { id: row.songId, name: row.songName, singerName: row.singerName, coverUrl: row.coverUrl, audioUrl: row.audioUrl, duration: row.duration },
    (playlist.value.songs || []).map((s) => ({ id: s.songId, name: s.songName, singerName: s.singerName, coverUrl: s.coverUrl, audioUrl: s.audioUrl, duration: s.duration }))
  )
}

function playAll() {
  if (playlist.value.songs?.length) playRow(playlist.value.songs[0])
}

async function toggleCollect() {
  if (!userStore.isLogin) return router.push('/login')
  if (playlist.value.collected) {
    await playlistApi.cancelCollect(playlist.value.id)
    ElMessage.success('已取消收藏')
  } else {
    await playlistApi.collect(playlist.value.id)
    ElMessage.success('收藏成功')
  }
  await load()
}

async function removeSong(row) {
  await playlistApi.removeSong(playlist.value.id, row.songId)
  ElMessage.success('已移除')
  await load()
}

async function saveEdit() {
  await playlistApi.update(playlist.value.id, editForm)
  ElMessage.success('保存成功')
  editDialog.value = false
  await load()
}

async function removePlaylist() {
  try {
    await ElMessageBox.confirm('确定删除该歌单吗？', '提示', { type: 'warning' })
    await playlistApi.remove(playlist.value.id)
    ElMessage.success('已删除')
    router.push('/playlists')
  } catch (e) {
    /* cancel */
  }
}

function fmt(sec) {
  if (!sec) return '--:--'
  return `${Math.floor(sec / 60)}:${String(Math.floor(sec % 60)).padStart(2, '0')}`
}

onMounted(load)
</script>

<style scoped>
.pl-detail { padding-top: 0 !important; }

/* ---------------- Hero ---------------- */
.pl-hero {
  position: relative;
  margin: -8px -6px 26px;
  padding: 44px 40px 38px;
  border-radius: 28px;
  overflow: hidden;
  border: 1px solid var(--km-border);
}
.hero-bg {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  filter: blur(58px) saturate(180%) brightness(.5);
  transform: scale(1.25);
}
.hero-mask {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(5, 5, 6, .3), rgba(5, 5, 6, .84)),
    radial-gradient(620px 300px at 12% 18%, rgba(192, 132, 252, .1), transparent 60%);
}
.hero-inner { position: relative; display: flex; gap: 32px; align-items: flex-end; }

.cover-box { position: relative; flex-shrink: 0; }
.cover-box img {
  width: 210px;
  height: 210px;
  border-radius: 20px;
  object-fit: cover;
  display: block;
  box-shadow: 0 26px 60px rgba(0, 0, 0, .62);
  border: 1px solid rgba(255, 255, 255, .12);
}
.cover-badge {
  position: absolute;
  left: 12px;
  bottom: 12px;
  height: 24px;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  color: #07080a;
  background: var(--km-accent);
}

.hero-info { flex: 1; min-width: 0; padding-bottom: 4px; }
.crumb { display: flex; align-items: center; gap: 6px; font-size: 11px; margin-bottom: 12px; }
.crumb .el-icon { font-size: 11px; }

.pl-name {
  margin: 0 0 10px;
  font-size: 38px;
  line-height: 1.16;
  font-weight: 900;
  letter-spacing: -1.2px;
}
.pl-desc {
  margin: 0 0 14px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--km-text-dim);
  max-width: 720px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.meta-line { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; font-size: 12.5px; color: var(--km-text-dim); }
.creator { display: inline-flex; align-items: center; gap: 7px; color: var(--km-text); font-weight: 600; }
.dot-sep { color: var(--km-text-faint); }

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
.btn-primary.sm { height: 36px; padding: 0 20px; font-size: 12.5px; box-shadow: none; }

.btn-ghost {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 46px;
  padding: 0 20px;
  border-radius: 999px;
  border: 1px solid var(--km-border-strong);
  background: rgba(255, 255, 255, .05);
  color: var(--km-text);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all .2s;
  backdrop-filter: blur(12px);
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
.btn-icon.danger:hover { color: #ff8095; border-color: rgba(255, 128, 149, .4); background: rgba(255, 77, 109, .1); }

/* ---------------- 列表 ---------------- */
.track-panel {
  background: var(--km-card);
  border: 1px solid var(--km-border);
  border-radius: var(--km-radius);
  padding: 8px 10px 12px;
}
.list-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px 10px;
  font-size: 11px;
  letter-spacing: .5px;
  color: var(--km-text-faint);
  border-bottom: 1px solid var(--km-border);
}
.col-idx { width: 34px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; position: relative; }
.col-title { flex: 1; display: flex; align-items: center; gap: 12px; min-width: 0; }
.col-dur { width: 70px; text-align: right; flex-shrink: 0; font-size: 12px; color: var(--km-text-faint); }
.col-act { width: 76px; display: flex; justify-content: flex-end; gap: 6px; flex-shrink: 0; }
.list-head .col-dur { color: var(--km-text-faint); }

.track-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 14px;
  border-radius: 12px;
  transition: background .16s;
  cursor: pointer;
}
.track-row:hover { background: rgba(255, 255, 255, .05); }
.track-row:hover .idx-num { opacity: 0; }
.track-row:hover .row-play { opacity: 1; transform: scale(1); }
.track-row:hover .col-dur { color: var(--km-text-dim); }
.track-row.playing .t-name { color: var(--km-accent); }

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

.mini-cover {
  width: 42px;
  height: 42px;
  border-radius: 9px;
  overflow: hidden;
  flex-shrink: 0;
}
.mini-cover img { width: 100%; height: 100%; object-fit: cover; display: block; }

.t-main { min-width: 0; }
.t-name {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--km-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.t-sub { font-size: 11.5px; color: var(--km-text-faint); margin-top: 3px; }

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
.mini-btn.danger:hover { color: #ff8095; border-color: rgba(255, 128, 149, .35); background: rgba(255, 77, 109, .1); }

.empty-box { padding: 60px 20px; text-align: center; }
.empty-ico { font-size: 34px; opacity: .5; margin-bottom: 10px; }
.empty-box p { margin: 0 0 18px; color: var(--km-text-dim); font-size: 13px; }

.dlg-form { display: flex; flex-direction: column; }
.f-label { font-size: 12px; color: var(--km-text-dim); font-weight: 600; margin: 0 0 8px; }
.dlg-form :deep(.el-input), .dlg-form :deep(.el-textarea) { margin-bottom: 16px; }

@media (max-width: 860px) {
  .hero-inner { flex-direction: column; align-items: flex-start; gap: 20px; }
  .pl-hero { padding: 26px 20px; }
  .cover-box img { width: 150px; height: 150px; }
  .pl-name { font-size: 26px; }
  .col-dur { display: none; }
}
</style>
