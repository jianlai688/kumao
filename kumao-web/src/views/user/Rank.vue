<template>
  <div class="km-page rank-page">
    <div class="page-head">
      <div>
        <h1 class="page-title">排行榜</h1>
        <p class="page-sub">实时热度 · 每小时更新</p>
      </div>
      <div class="head-actions">
        <el-button text @click="playAll" :disabled="!list.length">
          <el-icon><VideoPlay /></el-icon> 播放全部
        </el-button>
      </div>
    </div>

    <!-- 榜单切换 -->
    <div class="rank-tabs">
      <div
        v-for="t in tabs"
        :key="t.key"
        :class="['rank-tab', { active: type === t.key }]"
        @click="switchTab(t.key)"
      >
        <div class="tab-glow"></div>
        <div class="tab-ico">{{ t.icon }}</div>
        <div class="tab-body">
          <div class="tab-name">{{ t.name }}</div>
          <div class="tab-desc">{{ t.desc }}</div>
        </div>
        <div class="tab-arrow"><el-icon><ArrowRight /></el-icon></div>
      </div>
    </div>

    <!-- 榜单列表 -->
    <div class="rank-panel">
      <div class="panel-head">
        <div class="panel-title">
          <h2>{{ currentTab.name }}</h2>
          <span class="panel-count">{{ list.length }} 首</span>
        </div>
        <span class="panel-tip">双击歌曲即可播放</span>
      </div>

      <div class="rank-list">
        <div
          v-for="(row, i) in list"
          :key="row.songId"
          class="rank-row"
          @dblclick="playRow(row)"
        >
          <div class="idx" :class="{ top: i < 3 }">{{ String(i + 1).padStart(2, '0') }}</div>

          <div class="row-cover-wrap" @click="$router.push(`/song/${row.songId}`)">
            <img :src="row.coverUrl || fallback" class="row-cover" @error="onImg" />
            <div class="row-play" @click.stop="playRow(row)">
              <el-icon><VideoPlay /></el-icon>
            </div>
          </div>

          <div class="row-main" @click="$router.push(`/song/${row.songId}`)">
            <div class="row-title">
              <span class="row-name">{{ row.name }}</span>
              <span v-if="row.vipOnly" class="km-tag-vip">VIP</span>
            </div>
            <div class="row-artist">{{ row.singerName }}</div>
          </div>

          <div class="trend">
            <el-icon v-if="row.trend === 'up'" class="up"><CaretTop /></el-icon>
            <el-icon v-else-if="row.trend === 'down'" class="down"><CaretBottom /></el-icon>
            <span v-else class="flat">—</span>
          </div>

          <div class="plays">{{ formatPlay(row.playCount) }}</div>
          <div class="dur">{{ fmt(row.duration) }}</div>

          <div class="row-actions">
            <button class="mini-btn" @click.stop="playRow(row)"><el-icon><VideoPlay /></el-icon></button>
            <button class="mini-btn" @click.stop="addToPlaylist(row)"><el-icon><Plus /></el-icon></button>
          </div>
        </div>

        <el-empty v-if="!list.length" description="暂无榜单数据" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { musicApi, playlistApi } from '@/api'
import { usePlayerStore } from '@/stores/player'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const player = usePlayerStore()
const userStore = useUserStore()

const tabs = [
  { key: 'hot', name: '热门榜', desc: '全站热度最高', icon: '🔥' },
  { key: 'new', name: '新歌榜', desc: '最新发行歌曲', icon: '🌙' },
  { key: 'rising', name: '飙升榜', desc: '近期热度飙升', icon: '🚀' }
]

const type = ref(route.params.type || 'hot')
const list = ref([])
const loading = ref(false)

const fallback = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="88" height="88"><rect width="88" height="88" fill="#1a1c21"/><text x="44" y="52" font-size="26" text-anchor="middle" fill="#33363e">♪</text></svg>'
)

const currentTab = computed(() => tabs.find((t) => t.key === type.value) || tabs[0])

async function load() {
  loading.value = true
  try {
    const api = type.value === 'hot' ? musicApi.hot : type.value === 'new' ? musicApi.newSongs : musicApi.rising
    const res = await api(30)
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

function switchTab(key) {
  if (key === type.value) return
  type.value = key
  router.replace(`/rank/${key}`)
  load()
}

const toTrack = (s) => ({
  id: s.songId, name: s.name, singerName: s.singerName,
  coverUrl: s.coverUrl, audioUrl: s.audioUrl, duration: s.duration
})

function playRow(row) {
  player.play(toTrack(row), list.value.map(toTrack))
}

function playAll() {
  if (!list.value.length) return
  player.play(toTrack(list.value[0]), list.value.map(toTrack))
}

async function addToPlaylist(row) {
  if (!userStore.isLogin) return router.push('/login')
  const res = await playlistApi.mine({ pageNum: 1, pageSize: 50 })
  const lists = res.data?.records || []
  if (!lists.length) return ElMessage.warning('暂无歌单，请先创建歌单')
  try {
    const { value } = await ElMessageBox.prompt(
      '输入歌单名称（需与已有歌单完全一致）\n可选：' + lists.map((p) => p.name).join('、'),
      '收藏到歌单'
    )
    const target = lists.find((p) => p.name === value)
    if (!target) return ElMessage.error('未找到该歌单')
    await playlistApi.addSong(target.id, row.songId)
    ElMessage.success('已收藏到歌单')
  } catch (e) {
    /* cancel */
  }
}

function formatPlay(n) {
  if (!n) return '0'
  if (n >= 100000000) return (n / 100000000).toFixed(1) + '亿'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return String(n)
}

function fmt(sec) {
  if (!sec) return '-'
  return `${Math.floor(sec / 60)}:${String(Math.floor(sec % 60)).padStart(2, '0')}`
}

function onImg(e) {
  e.target.src = fallback
}

watch(() => route.params.type, (v) => {
  if (v && v !== type.value) {
    type.value = v
    load()
  }
})

onMounted(load)
</script>

<style scoped>
.rank-page { padding-top: 30px; }

.page-head { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 24px; }
.page-title {
  margin: 0;
  font-size: 32px;
  font-weight: 900;
  letter-spacing: -1px;
}
.page-sub { margin: 8px 0 0; font-size: 13px; color: var(--km-text-dim); }
.head-actions :deep(.el-button) { color: var(--km-text-dim); }
.head-actions :deep(.el-button:hover) { color: var(--km-accent); }

/* 榜单切换卡 */
.rank-tabs { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 26px; }

.rank-tab {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  border-radius: 20px;
  border: 1px solid var(--km-border);
  background: linear-gradient(150deg, rgba(255, 255, 255, .04), transparent 70%);
  cursor: pointer;
  overflow: hidden;
  transition: all .25s cubic-bezier(.22, .61, .36, 1);
}
.rank-tab:hover { border-color: rgba(255, 255, 255, .2); transform: translateY(-2px); }
.rank-tab.active {
  border-color: rgba(192, 132, 252, .55);
  background: linear-gradient(150deg, rgba(192, 132, 252, .16), rgba(192, 132, 252, .02) 70%);
  box-shadow: 0 14px 40px rgba(192, 132, 252, .1);
}
.tab-glow {
  position: absolute;
  right: -30px;
  top: -40px;
  width: 130px;
  height: 130px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(192, 132, 252, .2), transparent 65%);
  opacity: 0;
  transition: opacity .25s;
}
.rank-tab.active .tab-glow { opacity: 1; }

.tab-ico { font-size: 26px; }
.tab-body { flex: 1; }
.tab-name { font-size: 16px; font-weight: 800; }
.tab-desc { font-size: 12px; color: var(--km-text-dim); margin-top: 4px; }
.tab-arrow { color: var(--km-text-faint); transition: transform .2s; }
.rank-tab.active .tab-arrow { color: var(--km-accent); transform: translateX(3px); }

/* 列表面板 */
.rank-panel {
  border-radius: 24px;
  border: 1px solid var(--km-border);
  background: linear-gradient(180deg, rgba(255, 255, 255, .032), rgba(255, 255, 255, .008));
  padding: 22px 18px 12px;
}

.panel-head { display: flex; align-items: baseline; justify-content: space-between; padding: 0 10px 16px; }
.panel-title { display: flex; align-items: baseline; gap: 12px; }
.panel-title h2 { margin: 0; font-size: 19px; font-weight: 800; letter-spacing: -.3px; }
.panel-count { font-size: 12px; color: var(--km-text-faint); }
.panel-tip { font-size: 12px; color: var(--km-text-faint); }

.rank-list { display: flex; flex-direction: column; }

.rank-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 9px 12px;
  border-radius: 14px;
  transition: background .18s;
  cursor: default;
}
.rank-row:hover { background: rgba(255, 255, 255, .05); }
.rank-row:hover .row-play { opacity: 1; transform: scale(1); }
.rank-row:hover .row-actions { opacity: 1; }

.idx {
  width: 34px;
  text-align: center;
  font-size: 14px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: var(--km-text-faint);
  flex-shrink: 0;
}
.idx.top {
  font-size: 17px;
  background: linear-gradient(120deg, #c084fc, #f472b6);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.row-cover-wrap { position: relative; width: 48px; height: 48px; border-radius: 11px; overflow: hidden; flex-shrink: 0; }
.row-cover { width: 100%; height: 100%; object-fit: cover; display: block; }
.row-play {
  position: absolute;
  right: 3px;
  bottom: 3px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0, 0, 0, .62);
  color: var(--km-accent);
  font-size: 13px;
  opacity: 0;
  transform: scale(.85);
  transition: all .2s;
  cursor: pointer;
}

.row-main { flex: 1; min-width: 0; }
.row-title { display: flex; align-items: center; gap: 8px; }
.row-name {
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.row-artist { font-size: 12px; color: var(--km-text-dim); margin-top: 3px; }

.trend { width: 34px; display: flex; justify-content: center; }
.trend .up { color: #ff8095; }
.trend .down { color: rgba(243, 244, 246, .3); }
.flat { color: var(--km-text-faint); }

.plays { width: 76px; text-align: right; font-size: 12px; color: var(--km-text-dim); font-variant-numeric: tabular-nums; }
.dur { width: 52px; text-align: right; font-size: 12px; color: var(--km-text-faint); font-variant-numeric: tabular-nums; }

.row-actions { display: flex; gap: 6px; opacity: 0; transition: opacity .18s; }
.mini-btn {
  width: 30px; height: 30px;
  border-radius: 50%;
  border: 1px solid var(--km-border-strong);
  background: transparent;
  color: var(--km-text-dim);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all .18s;
}
.mini-btn:hover { color: #07080a; background: var(--km-accent); border-color: var(--km-accent); }

@media (max-width: 980px) {
  .rank-tabs { grid-template-columns: 1fr; }
  .plays, .dur { display: none; }
}
</style>
