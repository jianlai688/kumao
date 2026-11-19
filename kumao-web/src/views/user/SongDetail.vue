<template>
  <div class="song-page" v-if="song">
    <div class="bg" :style="{ backgroundImage: song.coverUrl ? `url('${song.coverUrl}')` : 'none' }"></div>
    <div class="bg-mask"></div>

    <div class="stage">
      <!-- 左侧：封面 + 歌曲信息 -->
      <section class="left-stage">
        <div class="cover-shell" @click="play">
          <img :src="song.coverUrl || fallbackCover" class="cover" @error="onImg" />
          <div class="cover-shade"></div>
          <div class="cover-play">
            <el-icon v-if="isCurrent && player.playing"><VideoPause /></el-icon>
            <el-icon v-else><VideoPlay /></el-icon>
          </div>
          <div v-if="songMv" class="mv-badge">MV</div>
        </div>

        <h1 class="song-title">{{ song.name }}</h1>

        <div class="artist-row">
          <span class="artist" @click="$router.push(`/singer/${song.singerId}`)">{{ song.singerName }}</span>
          <button :class="['follow-btn', { on: followed }]" @click="toggleFollow">
            {{ followed ? '已关注' : '关注' }}
          </button>
          <span class="like-count">♥ {{ formatCount(song.likeCount) }}</span>
        </div>

        <div class="tag-row">
          <span v-if="song.vipOnly" class="km-tag-vip">VIP 专属</span>
          <span v-if="song.region" class="km-chip mini">{{ song.region }}</span>
          <span v-if="song.language" class="km-chip mini">{{ song.language }}</span>
          <span v-if="song.style" class="km-chip mini">{{ song.style }}</span>
        </div>

        <div class="song-sub">
          {{ song.albumName || '单曲' }} · {{ song.releaseDate || '未知日期' }} · {{ durationText }}
        </div>

        <AdSlot slot="PLAYER" variant="inline" />

        <div class="stage-actions">
          <button class="btn-primary" @click="play">
            <el-icon><VideoPlay /></el-icon>
            {{ isCurrent && player.playing ? '暂停' : '播放' }}
          </button>
          <button :class="['btn-ghost', { liked }]" @click="toggleLike">
            <el-icon><component :is="liked ? 'StarFilled' : 'Star'" /></el-icon>
            {{ liked ? '已喜欢' : '喜欢' }}
          </button>
          <button class="btn-ghost" :disabled="!songMv" @click="switchMv">
            <el-icon><VideoCamera /></el-icon>
            切换 MV
          </button>
          <button class="btn-ghost" @click="showAdd = true">
            <el-icon><Plus /></el-icon>
            收藏
          </button>
        </div>
      </section>

      <!-- 右侧：歌词 / 评论 -->
      <section class="right-stage panel">
        <div class="stage-tabs">
          <button :class="['tab-btn', { active: tab === 'lyric' }]" @click="tab = 'lyric'">歌词</button>
          <button :class="['tab-btn', { active: tab === 'comment' }]" @click="tab = 'comment'">
            评论 <em>{{ commentTotal }}</em>
          </button>
          <span class="tab-hint">{{ tab === 'lyric' ? 'LRC' : '友善交流' }}</span>
        </div>

        <div v-if="tab === 'lyric'" class="lyrics-view">
          <div class="credits" v-if="creditLines.length">
            <p v-for="line in creditLines" :key="line">{{ line }}</p>
          </div>
          <div class="lyric-scroll" ref="lyricBox">
            <div
              v-for="(line, i) in displayLyrics"
              :key="i"
              :class="['lyric-line', { active: i === activeLyricIndex }]"
            >
              {{ line.text || '♪' }}
            </div>
            <el-empty v-if="!displayLyrics.length" description="暂无歌词" :image-size="80" />
          </div>
        </div>

        <div v-else class="comments-view">
          <div class="comment-input">
            <el-avatar :size="34" :src="userStore.user?.avatar">{{ (userStore.user?.nickname || 'U').slice(0, 1) }}</el-avatar>
            <div class="ci-main">
              <el-input
                v-model="commentText"
                type="textarea"
                :rows="2"
                maxlength="500"
                show-word-limit
                resize="none"
                placeholder="说点什么吧，友善的评论是交流的起点…"
              />
              <div class="ci-foot">
                <span class="km-faint">{{ commentText.length }}/500</span>
                <button class="btn-primary sm" :disabled="!commentText.trim() || commentSubmitting" @click="submitComment">{{ commentSubmitting ? '发表中…' : '发表评论' }}</button>
              </div>
            </div>
          </div>

          <div class="comment-list">
            <div v-for="c in comments" :key="c.id" class="comment-item">
              <el-avatar :size="34" :src="c.avatar">{{ (c.nickname || 'U').slice(0, 1) }}</el-avatar>
              <div class="c-body">
                <div class="c-head">
                  <span class="c-name">{{ c.nickname }}</span>
                  <span class="c-time">{{ (c.createTime || '').replace('T', ' ').slice(0, 16) }}</span>
                </div>
                <div class="c-content">{{ c.content }}</div>
              </div>
            </div>
            <el-empty v-if="!comments.length" description="还没有评论，来抢沙发" :image-size="70" />
          </div>

          <el-pagination
            v-if="commentTotal > query.pageSize"
            small
            background
            layout="prev, pager, next"
            :total="commentTotal"
            :page-size="query.pageSize"
            v-model:current-page="query.pageNum"
            @current-change="loadComments"
            class="km-pager"
          />
        </div>
      </section>
    </div>

    <el-dialog v-model="showAdd" title="收藏到歌单" width="420px">
      <el-select v-model="selectedPlaylist" placeholder="选择歌单" style="width: 100%">
        <el-option v-for="p in myPlaylists" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="addToPlaylist">确定</el-button>
      </template>
    </el-dialog>
  </div>

  <div v-else class="km-page"><el-skeleton :rows="8" animated /></div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VideoPlay, VideoPause, VideoCamera, Star, StarFilled, Plus } from '@element-plus/icons-vue'
import { musicApi, commentApi, likeApi, playlistApi } from '@/api'
import { usePlayerStore } from '@/stores/player'
import { useUserStore } from '@/stores/user'
import AdSlot from '@/components/AdSlot.vue'

const route = useRoute()
const router = useRouter()
const player = usePlayerStore()
const userStore = useUserStore()

const song = ref(null)
const songMv = ref(null)
const lyrics = ref([])
const liked = ref(false)
const followed = ref(false)
const tab = ref('lyric')
const comments = ref([])
const commentTotal = ref(0)
const commentText = ref('')
const commentSubmitting = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10 })
const showAdd = ref(false)
const myPlaylists = ref([])
const selectedPlaylist = ref(null)
const lyricBox = ref(null)

const fallbackCover = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#22252c"/><stop offset="1" stop-color="#101114"/></linearGradient></defs><rect width="400" height="400" fill="url(#g)"/><text x="200" y="228" font-size="88" text-anchor="middle" fill="#3a3e47">♪</text></svg>'
)

const isCurrent = computed(() => String(player.current?.id) === String(song.value?.id))
const durationText = computed(() => {
  const d = song.value?.duration
  if (!d) return '--:--'
  const m = Math.floor(d / 60)
  const s = String(Math.round(d % 60)).padStart(2, '0')
  return `${m}:${s}`
})
const creditLines = computed(() =>
  lyrics.value
    .map((l) => (l.text || '').trim())
    .filter((t) => /^(作词|作曲|编曲|制作人|出品)/.test(t))
)
const displayLyrics = computed(() =>
  lyrics.value.filter((l) => !/^(作词|作曲|编曲|制作人|出品)/.test((l.text || '').trim()))
)

/** 当前播放到第几行歌词（按当前歌曲的播放进度计算，兼容过滤掉制作人信息后的行号） */
const activeLyricIndex = computed(() => {
  if (!isCurrent.value) return -1
  const currentMs = (Number(player.progress) || 0) * 1000
  let index = -1
  displayLyrics.value.forEach((line, i) => {
    if (Number(line.time) <= currentMs) index = i
  })
  return index
})

function onImg(e) {
  e.target.src = fallbackCover
}

function formatCount(n) {
  n = Number(n) || 0
  return n >= 10000 ? (n / 10000).toFixed(1) + '万' : String(n)
}

function toTrack() {
  return {
    id: song.value.id,
    name: song.value.name,
    singerName: song.value.singerName,
    coverUrl: song.value.coverUrl,
    audioUrl: song.value.audioUrl,
    duration: song.value.duration
  }
}

function play() {
  if (isCurrent.value) return player.toggle()
  player.play(toTrack(), [toTrack()])
}

function switchMv() {
  if (!songMv.value) return ElMessage.info('该歌曲暂无 MV')
  if (!isCurrent.value) play()
  player.requestMv()
}

function toggleFollow() {
  followed.value = !followed.value
  localStorage.setItem(`km_follow_singer_${song.value.singerId}`, followed.value ? '1' : '0')
  ElMessage.success(followed.value ? '已关注该歌手' : '已取消关注')
}

async function toggleLike() {
  if (!userStore.isLogin) return router.push('/login')
  if (liked.value) {
    await likeApi.unlike(song.value.id)
    liked.value = false
  } else {
    await likeApi.like(song.value.id)
    liked.value = true
  }
  ElMessage.success(liked.value ? '已添加到我喜欢的音乐' : '已取消喜欢')
}

async function loadComments() {
  const res = await commentApi.bySong(route.params.id, query)
  comments.value = res.data?.records || []
  commentTotal.value = res.data?.total || 0
}

async function submitComment() {
  if (!userStore.isLogin) return router.push('/login')
  if (commentSubmitting.value) return
  commentSubmitting.value = true
  try {
    const res = await commentApi.create({ songId: route.params.id, content: commentText.value.trim() })
    commentText.value = ''
    if (String(res?.message || '').includes('复核')) {
      ElMessage.warning(res.message)
    } else {
      ElMessage.success(res?.message || '评论成功')
    }
    await loadComments()
  } catch (e) {
    /* 错误提示由请求拦截器统一处理 */
  } finally {
    commentSubmitting.value = false
  }
}

async function addToPlaylist() {
  if (!selectedPlaylist.value) return ElMessage.warning('请选择歌单')
  await playlistApi.addSong(selectedPlaylist.value, song.value.id)
  ElMessage.success('已收藏')
  showAdd.value = false
}

watch(
  () => activeLyricIndex.value,
  async (i) => {
    if (i < 0) return
    await nextTick()
    const box = lyricBox.value
    if (!box) return
    const el = box.querySelectorAll('.lyric-line')[i]
    if (el) box.scrollTo({ top: el.offsetTop - box.clientHeight / 2 + 24, behavior: 'smooth' })
  },
  { immediate: true }
)

onMounted(async () => {
  const [s, l, mv] = await Promise.all([
    musicApi.song(route.params.id),
    musicApi.lyric(route.params.id),
    musicApi.songMv(route.params.id).catch(() => ({ data: null }))
  ])
  song.value = s.data
  lyrics.value = l.data || []
  songMv.value = mv?.data || null
  followed.value = localStorage.getItem(`km_follow_singer_${song.value.singerId}`) === '1'
  await loadComments()
  if (userStore.isLogin) {
    likeApi.status(route.params.id).then((r) => (liked.value = !!r.data)).catch(() => {})
    const p = await playlistApi.mine({ pageNum: 1, pageSize: 50 })
    myPlaylists.value = p.data?.records || []
  }
})
</script>

<style scoped>
.song-page {
  position: relative;
  min-height: calc(100vh - 100px);
  padding: 10px 4px 130px;
  overflow: hidden;
  border-radius: 26px;
}
.bg {
  position: absolute;
  inset: -30px;
  background-size: cover;
  background-position: center;
  filter: blur(80px) saturate(170%) brightness(.42);
  transform: scale(1.12);
  z-index: 0;
}
.bg-mask {
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    linear-gradient(180deg, rgba(5, 5, 6, .35), rgba(5, 5, 6, .78) 60%, rgba(5, 5, 6, .92)),
    radial-gradient(700px 380px at 12% 15%, rgba(192, 132, 252, .1), transparent 62%);
}

.stage {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 400px minmax(0, 1fr);
  gap: 54px;
  max-width: 1240px;
  margin: 0 auto;
  padding: 34px 10px 0;
}

/* 左侧 */
.left-stage { display: flex; flex-direction: column; align-items: center; text-align: center; }
.cover-shell {
  position: relative;
  width: 330px;
  height: 330px;
  border-radius: 24px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 34px 80px rgba(0, 0, 0, .65);
  border: 1px solid rgba(255, 255, 255, .14);
}
.cover { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform .4s; }
.cover-shell:hover .cover { transform: scale(1.04); }
.cover-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 55%, rgba(0, 0, 0, .45));
  opacity: 0;
  transition: opacity .25s;
}
.cover-shell:hover .cover-shade { opacity: 1; }
.cover-play {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%) scale(.85);
  width: 62px;
  height: 62px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(192, 132, 252, .94);
  color: #07080a;
  font-size: 26px;
  opacity: 0;
  transition: all .25s;
  box-shadow: 0 12px 30px rgba(0, 0, 0, .35);
}
.cover-shell:hover .cover-play { opacity: 1; transform: translate(-50%, -50%) scale(1); }
.mv-badge {
  position: absolute;
  right: 14px;
  top: 14px;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(0, 0, 0, .55);
  border: 1px solid rgba(255, 255, 255, .2);
  color: #fff;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 1px;
  backdrop-filter: blur(8px);
}

.song-title {
  margin: 20px 0 10px;
  font-size: 28px;
  font-weight: 900;
  letter-spacing: -1px;
  line-height: 1.2;
  max-width: 100%;
}
.artist-row { display: flex; align-items: center; gap: 10px; font-size: 13px; }
.artist { color: rgba(243, 244, 246, .8); font-weight: 600; cursor: pointer; }
.artist:hover { color: var(--km-accent); }
.follow-btn {
  height: 24px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, .22);
  background: transparent;
  color: var(--km-text-dim);
  font-size: 11px;
  cursor: pointer;
}
.follow-btn:hover, .follow-btn.on { border-color: var(--km-accent); color: var(--km-accent); }
.like-count { margin-left: 6px; color: var(--km-text-faint); font-size: 12px; }

.tag-row { display: flex; gap: 7px; flex-wrap: wrap; justify-content: center; margin-top: 16px; }
.km-chip.mini { height: 24px; padding: 0 10px; font-size: 11px; }
.song-sub { margin-top: 14px; font-size: 12px; color: var(--km-text-faint); }

.stage-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; justify-content: center; margin-top: 20px; }
.btn-primary {
  display: inline-flex; align-items: center; gap: 8px;
  height: 44px; padding: 0 28px;
  border: none; border-radius: 999px;
  background: var(--km-grad); color: #fff;
  font-size: 14px; font-weight: 800; cursor: pointer;
  box-shadow: 0 10px 26px rgba(168, 85, 247, .25);
}
.btn-primary:hover { filter: brightness(1.06); transform: translateY(-1px); }
.btn-primary.sm { height: 32px; padding: 0 16px; font-size: 12px; box-shadow: none; }
.btn-primary:disabled { opacity: .45; cursor: not-allowed; }
.btn-ghost {
  display: inline-flex; align-items: center; gap: 6px;
  height: 44px; padding: 0 18px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, .16);
  background: rgba(255, 255, 255, .05);
  color: var(--km-text);
  font-size: 13px; font-weight: 600; cursor: pointer;
}
.btn-ghost:hover { background: rgba(255, 255, 255, .1); border-color: rgba(255, 255, 255, .28); }
.btn-ghost.liked { color: var(--km-accent); border-color: var(--km-accent-line); background: rgba(192, 132, 252, .1); }
.btn-ghost:disabled { opacity: .38; cursor: not-allowed; }

/* 右侧 */
.right-stage {
  height: 590px;
  display: flex;
  flex-direction: column;
  background: rgba(16, 17, 21, .55);
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: 24px;
  padding: 20px 24px;
  backdrop-filter: blur(18px);
}
.stage-tabs { display: flex; align-items: center; gap: 22px; border-bottom: 1px solid rgba(255, 255, 255, .07); padding-bottom: 12px; }
.tab-btn {
  border: none; background: transparent;
  color: var(--km-text-faint);
  font-size: 14px; font-weight: 600;
  cursor: pointer; padding: 4px 0;
  position: relative;
}
.tab-btn em { font-style: normal; font-size: 11px; margin-left: 3px; }
.tab-btn.active { color: var(--km-text); font-weight: 800; }
.tab-btn.active::after {
  content: '';
  position: absolute;
  left: 0; right: 0; bottom: -13px;
  height: 2px;
  background: var(--km-accent);
  border-radius: 2px;
}
.tab-hint { margin-left: auto; font-size: 11px; color: var(--km-text-faint); }

.lyrics-view { flex: 1; display: flex; flex-direction: column; min-height: 0; padding-top: 18px; }
.credits { flex-shrink: 0; padding: 0 4px 12px; border-bottom: 1px solid rgba(255, 255, 255, .05); }
.credits p { margin: 2px 0; font-size: 12.5px; color: var(--km-text-dim); line-height: 1.7; }
.lyric-scroll {
  flex: 1;
  overflow: auto;
  text-align: center;
  padding: 26px 10px 40px;
  scroll-behavior: smooth;
  mask-image: linear-gradient(180deg, transparent, #000 10%, #000 88%, transparent);
}
.lyric-scroll::-webkit-scrollbar { width: 0; }
.lyric-line {
  padding: 9px 0;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(243, 244, 246, .38);
  transition: all .25s;
}
.lyric-line.active {
  color: var(--km-accent);
  font-size: 17px;
  font-weight: 800;
  text-shadow: 0 0 26px rgba(192, 132, 252, .25);
}

/* 评论 */
.comments-view { flex: 1; display: flex; flex-direction: column; min-height: 0; padding-top: 16px; }
.comment-input { display: flex; gap: 10px; padding-bottom: 14px; border-bottom: 1px solid rgba(255, 255, 255, .05); flex-shrink: 0; }
.ci-main { flex: 1; }
.ci-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; }
.ci-foot .km-faint { font-size: 11px; }
.comment-list { flex: 1; overflow: auto; margin-top: 4px; }
.comment-item { display: flex; gap: 10px; padding: 14px 0; border-bottom: 1px solid rgba(255, 255, 255, .05); }
.comment-item:last-child { border-bottom: none; }
.c-body { flex: 1; min-width: 0; }
.c-head { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; }
.c-name { font-size: 12.5px; font-weight: 700; }
.c-time { font-size: 11px; color: var(--km-text-faint); }
.c-content { margin-top: 5px; font-size: 13px; line-height: 1.65; color: rgba(243, 244, 246, .82); word-break: break-word; }
.km-pager { flex-shrink: 0; padding-top: 8px; }

@media (max-width: 1100px) {
  .stage { grid-template-columns: 340px minmax(0, 1fr); gap: 32px; }
  .cover-shell { width: 300px; height: 300px; }
}
@media (max-width: 880px) {
  .stage { grid-template-columns: 1fr; }
  .right-stage { height: 520px; }
  .cover-shell { width: 260px; height: 260px; }
  .song-title { font-size: 25px; }
}
</style>


