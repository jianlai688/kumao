<template>
  <div class="km-page">
    <div class="page-head">
      <div>
        <h1 class="page-title">音乐人</h1>
        <p class="page-sub">发现独立声音，支持你喜欢的创作者</p>
      </div>
      <button class="btn-accent" @click="$router.push('/apply/musician')">
        <el-icon><Microphone /></el-icon>
        <span>申请成为音乐人</span>
      </button>
    </div>

    <div class="chips-row">
      <button v-for="c in styles" :key="c" :class="['km-chip', { active: style === c }]" @click="pick(c)">{{ c }}</button>
    </div>

    <div class="m-grid">
      <div v-for="m in filtered" :key="m.id" class="m-card km-hover-card" @click="$router.push(`/musician/${m.id}`)">
        <div class="avatar-wrap">
          <div class="avatar-ring"></div>
          <img :src="m.avatar || fallbackAvatar" class="m-avatar" @error="onImg" />
          <span class="play-hint"><el-icon><VideoPlay /></el-icon></span>
        </div>

        <div class="m-body">
          <div class="m-name">
            {{ m.stageName }}
            <span class="v-badge" title="认证音乐人">✓</span>
          </div>
          <div class="m-style">{{ m.style || '独立音乐' }}</div>
          <div class="m-fans">
            <span class="f-num">{{ formatFans(m.fansCount) }}</span>
            <span class="f-label">粉丝</span>
          </div>
          <p class="m-intro">{{ m.intro || '暂无简介' }}</p>
        </div>
      </div>
    </div>

    <div v-if="!filtered.length" class="empty-box">
      <div class="empty-ico">🎸</div>
      <p>暂无音乐人</p>
    </div>

    <div class="km-pager">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="query.pageSize"
                     v-model:current-page="query.pageNum" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Microphone, VideoPlay } from '@element-plus/icons-vue'
import { musicianApi } from '@/api'

const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 12 })
const style = ref('全部')
const styles = ['全部', '流行', '摇滚', '民谣', '电子', '说唱', '爵士', '古风']

const fallbackAvatar = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="240" height="240"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#2a2d35"/><stop offset="1" stop-color="#131418"/></linearGradient></defs><rect width="240" height="240" fill="url(#g)"/><circle cx="120" cy="96" r="38" fill="#3d424c"/><path d="M42 240c0-43 35-78 78-78s78 35 78 78z" fill="#3d424c"/></svg>'
)

const filtered = computed(() => {
  if (style.value === '全部') return list.value
  return list.value.filter((m) => (m.style || '').includes(style.value))
})

function onImg(e) {
  e.target.src = fallbackAvatar
}
function pick(c) {
  style.value = c
}
function formatFans(n) {
  if (!n) return '0'
  if (n >= 100000000) return (n / 100000000).toFixed(1) + '亿'
  return n >= 10000 ? (n / 10000).toFixed(1) + '万' : String(n)
}

async function load() {
  const res = await musicianApi.page(query)
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}
onMounted(load)
</script>

<style scoped>
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 24px; flex-wrap: wrap; }
.page-title { margin: 0 0 6px; font-size: 32px; font-weight: 900; letter-spacing: -1.2px; }
.page-sub { margin: 0; font-size: 13px; color: var(--km-text-dim); }

.btn-accent {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 42px;
  padding: 0 22px;
  border: none;
  border-radius: 999px;
  background: var(--km-accent);
  color: #07080a;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: all .2s;
  box-shadow: 0 8px 22px rgba(192, 132, 252, .2);
}
.btn-accent:hover { background: #d8b4fe; transform: translateY(-1px); }

.chips-row { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 26px; }

.m-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 18px; }

.m-card {
  background: linear-gradient(180deg, #15161b, #101115);
  border: 1px solid var(--km-border);
  border-radius: 20px;
  padding: 24px 20px 20px;
  text-align: center;
  transition: all .24s;
  position: relative;
  overflow: hidden;
}
.m-card::before {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: 20px;
  padding: 1px;
  background: linear-gradient(160deg, rgba(192, 132, 252, .5), transparent 42%);
  -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  opacity: 0;
  transition: opacity .24s;
  pointer-events: none;
}
.m-card:hover { transform: translateY(-5px); box-shadow: 0 22px 46px rgba(0, 0, 0, .55); }
.m-card:hover::before { opacity: 1; }
.m-card:hover .avatar-ring { inset: -9px; opacity: 1; }
.m-card:hover .play-hint { opacity: 1; transform: scale(1); }

.avatar-wrap { position: relative; width: 96px; height: 96px; margin: 0 auto 14px; }
.avatar-ring {
  position: absolute;
  inset: -7px;
  border-radius: 50%;
  border: 1px solid rgba(192, 132, 252, .18);
  opacity: .55;
  transition: all .24s;
}
.m-avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
  border: 1px solid rgba(255, 255, 255, .1);
}
.play-hint {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--km-accent);
  color: #07080a;
  font-size: 13px;
  opacity: 0;
  transform: scale(.7);
  transition: all .22s;
  box-shadow: 0 6px 16px rgba(192, 132, 252, .3);
}

.m-body { position: relative; }
.m-name {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: -.3px;
}
.v-badge {
  width: 15px;
  height: 15px;
  display: inline-grid;
  place-items: center;
  border-radius: 50%;
  background: var(--km-accent);
  color: #07080a;
  font-size: 9px;
  font-weight: 900;
}
.m-style {
  margin-top: 5px;
  font-size: 11px;
  color: var(--km-accent);
  font-weight: 600;
  letter-spacing: .3px;
}
.m-fans { margin-top: 10px; display: inline-flex; align-items: baseline; gap: 4px; }
.f-num { font-size: 17px; font-weight: 800; }
.f-label { font-size: 10.5px; color: var(--km-text-faint); }
.m-intro {
  margin: 10px 0 0;
  font-size: 11.5px;
  line-height: 1.55;
  color: var(--km-text-dim);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 36px;
}

.empty-box { padding: 70px 20px; text-align: center; }
.empty-ico { font-size: 38px; opacity: .45; margin-bottom: 12px; }
.empty-box p { margin: 0; color: var(--km-text-dim); font-size: 13px; }

@media (max-width: 640px) {
  .m-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .m-card { padding: 18px 12px 16px; }
  .page-title { font-size: 26px; }
}
</style>
