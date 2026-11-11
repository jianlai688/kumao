<template>
  <div class="live-square">
    <header class="square-head">
      <div>
        <h1 class="square-title">音乐直播</h1>
        <p class="square-sub">{{ livingCount }} 个直播间正在开播 · 和主播一起听歌聊天</p>
      </div>
      <div class="head-actions">
        <div class="search-box">
          <el-input v-model="keyword" placeholder="搜索直播间 / 主播" clearable aria-label="搜索直播间或主播" @keyup.enter="reload">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <button v-if="userStore.hasRole('ANCHOR')" class="studio-btn" @click="$router.push('/studio/anchor')">
          <el-icon><VideoCamera /></el-icon> 主播工作台
        </button>
      </div>
    </header>

    <div class="chips-row" role="navigation" aria-label="直播分类">
      <button class="km-chip" :class="{ active: category === '' }" @click="switchCategory('')">全部</button>
      <button
        v-for="c in categories"
        :key="c"
        class="km-chip"
        :class="{ active: category === c }"
        @click="switchCategory(c)"
      >{{ c }}</button>
    </div>

    <div class="chips-row scene-row" role="navigation" aria-label="直播场景">
      <button class="km-chip" :class="{ active: scene === '' }" @click="switchScene('')">全部场景</button>
      <button
        v-for="s in sceneOptions"
        :key="s.value"
        class="km-chip"
        :class="{ active: scene === s.value }"
        @click="switchScene(s.value)"
      >{{ s.label }}</button>
    </div>

    <AdSlot slot="LIVE" variant="card" />

    <div class="live-grid" v-if="list.length">
      <router-link
        v-for="room in list"
        :key="room.id"
        :to="`/live/${room.id}`"
        class="live-card"
        :aria-label="`进入直播间 ${room.title}`"
      >
        <div class="live-cover-wrap">
          <img :src="room.coverUrl || fallback" class="cover" alt="" @error="onImg" />
          <span class="km-badge-live live-badge"><span class="km-dot"></span>{{ room.realtimeOnline }} 人在线</span>
          <span class="category-tag">{{ room.category }}</span>
          <span v-if="room.scene === 'OUTDOOR'" class="scene-tag">{{ room.location || '户外直播' }}</span>
        </div>
        <div class="live-body">
          <div class="title">{{ room.title }}</div>
          <div class="live-meta">
            <el-avatar :size="20" :src="room.anchorAvatar">{{ (room.anchorName || 'A').slice(0, 1) }}</el-avatar>
            <span>{{ room.anchorName }}</span>
            <span class="live-meta-dot" aria-hidden="true"></span>
            <span>{{ room.category }}</span>
          </div>
        </div>
      </router-link>
    </div>

    <div v-else class="square-empty">
      <span class="empty-ring" aria-hidden="true"></span>
      <p>暂无正在直播的房间</p>
      <button v-if="userStore.hasRole('ANCHOR')" class="studio-btn" @click="$router.push('/studio/anchor')">去开播</button>
    </div>

    <div class="km-pager" v-if="total > query.pageSize">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="query.pageSize"
                     v-model:current-page="query.pageNum" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { liveApi } from '@/api'
import { useUserStore } from '@/stores/user'
import AdSlot from '@/components/AdSlot.vue'

const userStore = useUserStore()
const list = ref([])
const total = ref(0)
const categories = ref([])
const category = ref('')
const scene = ref('')
const sceneOptions = ref([
  { value: 'INDOOR', label: '室内直播' },
  { value: 'OUTDOOR', label: '户外直播' }
])
const keyword = ref('')
const query = reactive({ pageNum: 1, pageSize: 12 })

const livingCount = computed(() => total.value || list.value.length)

const fallback = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="480" height="300"><rect width="480" height="300" fill="#14111b"/><circle cx="240" cy="145" r="46" fill="none" stroke="#2c2538" stroke-width="2"/><path d="M216 116v34a24 24 0 0 0 48 0v-34" fill="none" stroke="#3a3148" stroke-width="3" stroke-linecap="round"/><text x="240" y="240" font-size="14" text-anchor="middle" fill="#4a4058">KUMAO LIVE</text></svg>'
)

function switchCategory(c) {
  category.value = c
  query.pageNum = 1
  load()
}

function switchScene(s) {
  scene.value = s
  query.pageNum = 1
  load()
}

function reload() {
  query.pageNum = 1
  load()
}

function onImg(e) {
  e.target.src = fallback
}

async function load() {
  try {
    const res = await liveApi.square({ ...query, category: category.value, scene: scene.value, keyword: keyword.value })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  }
}

onMounted(async () => {
  try {
    const [c, s] = await Promise.all([liveApi.categories(), liveApi.scenes()])
    categories.value = c.data || []
    if (s.data?.length) sceneOptions.value = s.data
  } catch (e) {
    /* keep defaults */
  }
  await load()
})
</script>

<style scoped>
.live-square {
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
  padding: 24px 34px 46px;
}
.square-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 18px; flex-wrap: wrap; }
.square-title { margin: 0; font-size: 26px; line-height: 1.25; font-weight: 700; letter-spacing: -.04em; color: var(--km-text); }
.square-sub { margin: 7px 0 0; font-size: 12px; color: var(--km-text-faint); }
.head-actions { display: flex; align-items: center; gap: 8px; }
.search-box { width: 224px; }
.search-box :deep(.el-input__wrapper) { height: 34px; border-radius: 9px !important; }
.studio-btn { display: inline-flex; align-items: center; gap: 5px; height: 34px; padding: 0 12px; border-radius: 9px; border: 1px solid var(--km-accent-line); background: rgba(216, 180, 254, .08); color: #f5eaff; font-size: 11px; font-weight: 600; cursor: pointer; white-space: nowrap; }
.studio-btn:hover { background: rgba(216, 180, 254, .14); }
.chips-row { display: flex; gap: 7px; flex-wrap: wrap; margin-bottom: 16px; }
.scene-row { margin-top: -8px; }
.live-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 14px; }
.live-card { display: block; border-radius: 12px; overflow: hidden; border: 1px solid var(--km-border); background: var(--km-card); transition: border-color .18s, background .18s; }
.live-card:hover { border-color: var(--km-border-strong); background: var(--km-card-2); }
.live-cover-wrap { position: relative; aspect-ratio: 16/10; background: #1a1326; overflow: hidden; }
.cover { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform .3s; }
.live-card:hover .cover { transform: scale(1.025); }
.live-badge { position: absolute; top: 9px; left: 9px; z-index: 2; }
.category-tag, .scene-tag { position: absolute; z-index: 2; font-size: 11px; padding: 3px 7px; border-radius: 4px; }
.category-tag { top: 9px; right: 9px; background: rgba(8, 7, 11, .7); color: rgba(255, 255, 255, .8); }
.scene-tag { left: 9px; bottom: 9px; background: rgba(216, 180, 254, .16); color: #f5d0fe; border: 1px solid rgba(216, 180, 254, .18); }
.live-body { padding: 11px 12px 13px; }
.title { font-size: 13px; font-weight: 600; color: var(--km-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.live-meta { display: flex; align-items: center; gap: 6px; margin-top: 7px; color: var(--km-text-faint); font-size: 11px; }
.live-meta-dot { width: 2px; height: 2px; border-radius: 50%; background: var(--km-text-faint); }
.square-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; min-height: 300px; color: var(--km-text-faint); }
.empty-ring { width: 54px; height: 54px; border-radius: 50%; border: 1px solid var(--km-border-strong); box-shadow: 0 0 0 12px rgba(255, 255, 255, .015); }
.square-empty p { margin: 0; font-size: 12px; }
@media (max-width: 720px) {
  .live-square { padding: 16px 16px 32px; }
  .square-title { font-size: 22px; }
  .square-head { align-items: flex-start; flex-direction: column; gap: 12px; }
  .head-actions { width: 100%; }
  .search-box { flex: 1; width: auto; }
  .live-grid { grid-template-columns: 1fr 1fr; gap: 10px; }
  .live-body { padding: 9px 10px 11px; }
  .title { font-size: 12px; }
}
@media (max-width: 480px) {
  .live-grid { grid-template-columns: 1fr; }
}
</style>


