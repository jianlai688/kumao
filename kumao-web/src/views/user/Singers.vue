<template>
  <div class="km-page">
    <div class="page-head">
      <div>
        <h1 class="page-title">歌手</h1>
        <p class="page-sub">发现你喜爱的声音 · {{ total }} 位歌手</p>
      </div>
    </div>

    <div class="chips-row">
      <button
        v-for="r in regions"
        :key="r.value"
        class="km-chip"
        :class="{ active: region === r.value }"
        @click="switchRegion(r.value)"
      >{{ r.label }}</button>
    </div>

    <div class="singer-grid">
      <div
        v-for="s in list"
        :key="s.id"
        class="singer-card km-hover-card"
        @click="$router.push(`/singer/${s.id}`)"
      >
        <div class="avatar-wrap">
          <img :src="s.avatar || fallback" class="singer-avatar" @error="onImg" />
          <div class="avatar-ring"></div>
        </div>
        <div class="singer-name">{{ s.name }}</div>
        <div class="singer-meta">{{ s.region }} · {{ formatFans(s.fansCount) }} 粉丝</div>
        <div class="singer-intro">{{ s.intro }}</div>
      </div>
    </div>
    <el-empty v-if="!list.length" description="暂无歌手" />

    <div class="km-pager">
      <el-pagination background layout="total, prev, pager, next" :total="total"
                     :page-size="query.pageSize" v-model:current-page="query.pageNum" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { musicApi } from '@/api'

const list = ref([])
const total = ref(0)
const region = ref('')
const query = reactive({ pageNum: 1, pageSize: 24 })

const regions = [
  { value: '', label: '全部' },
  { value: '国内', label: '国内' },
  { value: '国外', label: '国外' },
  { value: '粤语', label: '粤语' }
]

const fallback = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120"><rect width="120" height="120" fill="#1a1c21"/><text x="60" y="74" font-size="38" text-anchor="middle" fill="#33363e">🎤</text></svg>'
)

function formatFans(n) {
  if (!n) return '0'
  return n >= 10000 ? (n / 10000).toFixed(1) + '万' : String(n)
}

function switchRegion(v) {
  region.value = v
  query.pageNum = 1
  load()
}

function onImg(e) {
  e.target.src = fallback
}

async function load() {
  const res = await musicApi.singers({ ...query, region: region.value })
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

onMounted(load)
</script>

<style scoped>
.page-head { margin-bottom: 22px; }
.page-title { margin: 0; font-size: 32px; font-weight: 900; letter-spacing: -1px; }
.page-sub { margin: 8px 0 0; font-size: 13px; color: var(--km-text-dim); }

.chips-row { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 26px; }

.singer-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 18px;
}

.singer-card {
  background: linear-gradient(170deg, rgba(255, 255, 255, .045), rgba(255, 255, 255, .01));
  border: 1px solid var(--km-border);
  border-radius: 22px;
  padding: 26px 16px 20px;
  text-align: center;
  transition: all .28s cubic-bezier(.22, .61, .36, 1);
}
.singer-card:hover {
  border-color: rgba(192, 132, 252, .38);
  background: linear-gradient(170deg, rgba(192, 132, 252, .09), transparent 70%);
  transform: translateY(-5px);
}

.avatar-wrap { position: relative; width: 96px; height: 96px; margin: 0 auto; }
.singer-avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
  position: relative;
  z-index: 1;
  border: 2px solid rgba(255, 255, 255, .08);
}
.avatar-ring {
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  border: 1px solid rgba(192, 132, 252, .35);
  opacity: 0;
  transition: opacity .28s, transform .28s;
  transform: scale(.94);
}
.singer-card:hover .avatar-ring { opacity: 1; transform: scale(1); }

.singer-name { margin-top: 16px; font-size: 15px; font-weight: 700; }
.singer-meta { font-size: 12px; color: var(--km-text-dim); margin: 5px 0 10px; }
.singer-intro {
  font-size: 12px;
  color: var(--km-text-faint);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 38px;
}
</style>
