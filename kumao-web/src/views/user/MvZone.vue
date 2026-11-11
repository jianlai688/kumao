<template>
  <div class="km-page">
    <div class="page-head">
      <div>
        <h1 class="page-title">MV 专区</h1>
        <p class="page-sub">超清画质 · 沉浸视听</p>
      </div>
    </div>

    <div class="mv-grid">
      <div v-for="mv in list" :key="mv.id" class="mv-card km-hover-card" @click="playMv(mv)">
        <div class="cover-wrap">
          <img :src="mv.coverUrl || fallback" class="mv-cover" @error="onImg" />
          <div class="play-mask"><el-icon><VideoPlay /></el-icon></div>
          <div class="quality">{{ mv.quality }}</div>
          <div v-if="mv.vipOnly" class="vip-flag"><span class="km-tag-vip">VIP</span></div>
          <div class="play-count">{{ formatNum(mv.playCount) }} 次播放</div>
        </div>
        <div class="mv-name">{{ mv.name }}</div>
        <div class="mv-sub">{{ mv.singerName }}</div>
      </div>
    </div>
    <el-empty v-if="!list.length" description="暂无 MV" />

    <div class="km-pager">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="query.pageSize"
                     v-model:current-page="query.pageNum" @current-change="load" />
    </div>

    <el-dialog v-model="dialog" :title="current?.name" width="880px" destroy-on-close class="mv-dialog">
      <video v-if="videoUrl" :src="videoUrl" controls autoplay class="mv-player" />
      <el-result v-else icon="warning" title="无法播放" sub-title="该 MV 为 VIP 专享，请开通会员后再试">
        <template #extra><el-button type="primary" round @click="$router.push('/vip')">开通会员</el-button></template>
      </el-result>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { musicApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 12, status: 1 })
const dialog = ref(false)
const current = ref(null)
const videoUrl = ref('')

const fallback = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="480" height="270"><rect width="480" height="270" fill="#15171b"/><text x="240" y="152" font-size="48" text-anchor="middle" fill="#2e3138">▶</text></svg>'
)

function formatNum(n) {
  if (!n) return '0'
  return n >= 10000 ? (n / 10000).toFixed(1) + '万' : String(n)
}

function onImg(e) {
  e.target.src = fallback
}

async function load() {
  const res = await musicApi.mvs(query)
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

async function playMv(mv) {
  if (!userStore.isLogin) return router.push('/login')
  current.value = mv
  videoUrl.value = ''
  dialog.value = true
  try {
    const res = await musicApi.playMv(mv.id)
    videoUrl.value = res.data.videoUrl
  } catch (e) {
    ElMessage.warning('该 MV 需要 VIP 会员权限')
  }
}

onMounted(load)
</script>

<style scoped>
.page-head { margin-bottom: 24px; }
.page-title { margin: 0; font-size: 32px; font-weight: 900; letter-spacing: -1px; }
.page-sub { margin: 8px 0 0; font-size: 13px; color: var(--km-text-dim); }

.mv-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 22px; }
.mv-card { cursor: pointer; }

.mv-cover { width: 100%; aspect-ratio: 16/9; object-fit: cover; display: block; }

.quality {
  position: absolute;
  right: 10px;
  top: 10px;
  z-index: 2;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: .6px;
  padding: 3px 8px;
  border-radius: 6px;
  background: rgba(0, 0, 0, .6);
  backdrop-filter: blur(8px);
  color: var(--km-accent);
}
.vip-flag { position: absolute; left: 10px; top: 10px; z-index: 2; }

.play-count {
  position: absolute;
  right: 10px;
  bottom: 10px;
  z-index: 2;
  font-size: 11px;
  color: rgba(255, 255, 255, .82);
  background: rgba(0, 0, 0, .55);
  backdrop-filter: blur(8px);
  padding: 3px 9px;
  border-radius: 999px;
}

.mv-name { margin-top: 12px; font-size: 15px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.mv-sub { font-size: 12px; color: var(--km-text-dim); margin-top: 4px; }

.mv-player { width: 100%; border-radius: 14px; background: #000; display: block; }
</style>
