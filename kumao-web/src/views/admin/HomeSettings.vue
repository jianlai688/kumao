<template>
  <div class="home-settings">
    <section class="km-card panel">
      <header class="panel-head">
        <div class="head-copy">
          <span class="eyebrow">KUMAO OPS · HOME HERO</span>
          <h2>首页设置</h2>
          <p>控制「发现音乐」页顶部轮播与「今日主打」位的自动切换；顶部 Banner 内容、图片和排序在「轮播管理」中维护。</p>
        </div>
        <el-button @click="$router.push('/admin/banners')">管理首页轮播</el-button>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </header>

      <el-form class="form" label-width="96px" v-loading="loading">
        <el-form-item label="自动轮播">
          <el-switch v-model="form.heroAutoplay" :active-value="1" :inactive-value="0" />
          <span class="hint">同时作用于顶部 Banner 与今日主打；关闭后固定展示第 1 张 / 第 1 首</span>
        </el-form-item>

        <el-form-item label="切换间隔">
          <el-input-number v-model="form.heroInterval" :min="2" :max="15" :step="1" />
          <span class="unit">秒</span>
          <span class="hint">顶部 Banner 与今日主打共用，建议 4–8 秒</span>
        </el-form-item>

        <el-form-item label="轮播条数" v-if="form.heroMode === 'HOT'">
          <el-input-number v-model="form.heroCount" :min="1" :max="10" :step="1" />
          <span class="unit">首</span>
          <span class="hint">取热歌榜前 N 首轮播，1 首即不轮播</span>
        </el-form-item>

        <el-form-item label="轮播内容">
          <el-radio-group v-model="form.heroMode">
            <el-radio-button value="HOT">热歌榜自动</el-radio-button>
            <el-radio-button value="MANUAL">手动选歌</el-radio-button>
          </el-radio-group>
          <span class="hint">手动模式按下方顺序展示，最多 10 首</span>
        </el-form-item>

        <template v-if="form.heroMode === 'MANUAL'">
          <el-form-item label="已选歌曲">
            <div class="picked">
              <div v-for="(s, i) in selectedSongs" :key="s.id" class="picked-item">
                <span class="idx">{{ i + 1 }}</span>
                <span class="nm">{{ s.name }}</span>
                <span class="sg">{{ s.singerName }}</span>
                <button class="mini" :disabled="i === 0" @click="moveSong(i, -1)">↑</button>
                <button class="mini" :disabled="i === selectedSongs.length - 1" @click="moveSong(i, 1)">↓</button>
                <button class="mini danger" @click="removeSong(i)">×</button>
              </div>
              <p v-if="!selectedSongs.length" class="hint">还没有选歌，从下面添加</p>
            </div>
          </el-form-item>

          <el-form-item label="添加歌曲">
            <div class="picker">
              <el-input v-model="keyword" placeholder="搜索歌曲 / 歌手" clearable style="width: 240px" />
              <div class="cand-list">
                <div v-for="s in filteredCandidates" :key="s.id" class="cand" @click="addSong(s)">
                  <span class="nm">{{ s.name }}</span>
                  <span class="sg">{{ s.singerName }}</span>
                  <span class="add">＋</span>
                </div>
                <p v-if="!filteredCandidates.length" class="hint">没有可添加的歌曲</p>
              </div>
            </div>
          </el-form-item>
        </template>

        <el-form-item label="生效范围">
          <span class="scope">PC Web 与 APP（uniapp / APK）首页 · 客户端下次进入首页自动拉取</span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存设置</el-button>
          <el-button @click="load">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="km-card panel">
      <header class="panel-head">
        <div class="head-copy">
          <h3>轮播预览</h3>
          <p>按当前配置取热歌榜前 {{ form.heroCount }} 首，依次在首页「今日主打」位展示。</p>
        </div>
        <span class="chip">{{ form.heroAutoplay ? `每 ${form.heroInterval} 秒切换` : '已关闭自动轮播' }}</span>
      </header>

      <div class="cards">
        <article v-for="(song, index) in previewList" :key="song.id || index" class="pv">
          <span class="pv-index">{{ index + 1 }}</span>
          <div class="pv-body">
            <b>{{ song.name }}</b>
            <span>{{ song.singerName }}</span>
          </div>
          <span class="pv-tag">{{ index === 0 ? '首屏' : '轮播' }}</span>
        </article>
        <p v-if="!previewList.length" class="km-faint">热歌榜暂无数据</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const form = reactive({ heroAutoplay: 1, heroInterval: 5, heroCount: 3, heroMode: 'HOT' })
const hotSongs = ref([])
const candidates = ref([])
const selectedSongs = ref([])
const keyword = ref('')

const previewList = computed(() => {
  if (form.heroMode === 'MANUAL') return selectedSongs.value
  return hotSongs.value.slice(0, Math.max(1, Number(form.heroCount) || 1))
})

const filteredCandidates = computed(() => {
  const selected = new Set(selectedSongs.value.map((s) => String(s.id)))
  const kw = keyword.value.trim().toLowerCase()
  return candidates.value
    .filter((s) => !selected.has(String(s.id)))
    .filter((s) => !kw || `${s.name} ${s.singerName}`.toLowerCase().includes(kw))
    .slice(0, 12)
})

function toSong(raw) {
  return {
    id: raw.id || raw.songId,
    name: raw.name,
    singerName: raw.singerName || '',
    coverUrl: raw.coverUrl || ''
  }
}

function addSong(song) {
  if (selectedSongs.value.length >= 10) {
    ElMessage.warning('最多选择 10 首')
    return
  }
  if (!selectedSongs.value.some((s) => String(s.id) === String(song.id))) {
    selectedSongs.value.push(toSong(song))
  }
}

function removeSong(index) {
  selectedSongs.value.splice(index, 1)
}

function moveSong(index, delta) {
  const target = index + delta
  if (target < 0 || target >= selectedSongs.value.length) return
  const list = selectedSongs.value
  ;[list[index], list[target]] = [list[target], list[index]]
}

async function load() {
  loading.value = true
  try {
    const [setting, hot] = await Promise.all([
      request.get('/music/home/settings'),
      request.get('/music/rank/hot', { params: { limit: 50 } }).catch(() => null)
    ])
    const data = setting?.data || {}
    form.heroAutoplay = Number(data.heroAutoplay ?? 1) === 0 ? 0 : 1
    form.heroInterval = Number(data.heroInterval) || 5
    form.heroCount = Number(data.heroCount) || 3
    form.heroMode = data.heroMode === 'MANUAL' ? 'MANUAL' : 'HOT'
    hotSongs.value = (hot?.data || []).map(toSong)
    candidates.value = hotSongs.value
    selectedSongs.value = Array.isArray(data.songs) ? data.songs.map(toSong) : []
  } finally {
    loading.value = false
  }
}

async function save() {
  if (form.heroMode === 'MANUAL' && !selectedSongs.value.length) {
    ElMessage.warning('手动模式请至少选择 1 首歌曲')
    return
  }
  saving.value = true
  try {
    await request.put('/music/home/admin/settings', {
      heroAutoplay: form.heroAutoplay,
      heroInterval: form.heroInterval,
      heroCount: form.heroCount,
      heroMode: form.heroMode,
      heroSongIds: selectedSongs.value.map((s) => s.id).join(',')
    })
    ElMessage.success('首页设置已保存')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.home-settings { display: flex; flex-direction: column; gap: 16px; }
.panel { padding: 20px 22px; }
.panel-head { display: flex; align-items: flex-start; gap: 16px; margin-bottom: 18px; }
.head-copy { flex: 1; min-width: 0; }
.eyebrow { font-size: 11px; letter-spacing: .16em; color: var(--km-text-faint); text-transform: uppercase; }
.head-copy h2 { margin: 6px 0 4px; font-size: 20px; }
.head-copy h3 { margin: 0 0 4px; font-size: 16px; }
.head-copy p { margin: 0; font-size: 12px; color: var(--km-text-dim); line-height: 1.7; }
.chip { padding: 6px 12px; border-radius: 999px; border: 1px solid var(--km-border); font-size: 12px; color: var(--km-text-dim); white-space: nowrap; }
.form { max-width: 720px; }
.hint { margin-left: 10px; font-size: 12px; color: var(--km-text-faint); }
.unit { margin-left: 6px; font-size: 12px; color: var(--km-text-dim); }
.scope { font-size: 12px; color: var(--km-text-dim); }
.picked { width: 100%; display: flex; flex-direction: column; gap: 6px; }
.picked-item { display: flex; align-items: center; gap: 8px; padding: 6px 10px; border: 1px solid var(--km-border); border-radius: 8px; }
.picked-item .idx { width: 20px; height: 20px; border-radius: 50%; display: grid; place-items: center; font-size: 11px; color: #22150a; background: linear-gradient(140deg,#fbd08a,#e08b3e); }
.picked-item .nm { font-size: 13px; }
.picked-item .sg { font-size: 11px; color: var(--km-text-faint); flex: 1; }
.mini { width: 24px; height: 24px; border-radius: 6px; border: 1px solid var(--km-border); background: transparent; color: var(--km-text-dim); cursor: pointer; }
.mini:disabled { opacity: .35; cursor: not-allowed; }
.mini.danger { color: #ff9fb0; }
.picker { width: 100%; display: flex; flex-direction: column; gap: 8px; }
.cand-list { display: flex; flex-direction: column; gap: 4px; max-height: 220px; overflow: auto; }
.cand { display: flex; align-items: center; gap: 8px; padding: 6px 10px; border-radius: 8px; cursor: pointer; }
.cand:hover { background: rgba(255,255,255,.05); }
.cand .nm { font-size: 13px; }
.cand .sg { font-size: 11px; color: var(--km-text-faint); flex: 1; }
.cand .add { color: var(--km-accent); font-weight: 700; }
.cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; }
.pv { display: flex; align-items: center; gap: 10px; padding: 12px 14px; border: 1px solid var(--km-border); border-radius: 12px; background: rgba(255, 255, 255, .02); }
.pv-index { width: 22px; height: 22px; border-radius: 50%; display: grid; place-items: center; font-size: 11px; color: #22150a; background: linear-gradient(140deg, #fbd08a, #e08b3e); }
.pv-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.pv-body b { font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pv-body span { font-size: 11px; color: var(--km-text-faint); }
.pv-tag { font-size: 10px; padding: 3px 8px; border-radius: 999px; border: 1px solid var(--km-border); color: var(--km-text-faint); }
</style>


