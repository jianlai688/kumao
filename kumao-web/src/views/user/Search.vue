<template>
  <div class="search-page">
    <header class="search-head">
      <h1 class="search-title">搜索音乐</h1>
      <p class="search-sub">在酷猫音乐里找到歌曲、歌手、歌单与正在发生的直播现场。</p>
    </header>

    <section class="search-work" aria-label="搜索音乐">
      <div class="search-field">
        <el-icon class="field-icon" aria-hidden="true"><Search /></el-icon>
        <input
          v-model="keyword"
          type="search"
          class="search-input"
          placeholder="搜索歌曲、歌手、歌单、直播间、音乐人"
          aria-label="搜索歌曲、歌手、歌单、直播间、音乐人"
          @keyup.enter="doSearch"
        />
        <span v-if="keyword" class="field-count">{{ keyword.length }}/40</span>
        <button class="search-submit" :disabled="!keyword.trim()" @click="doSearch">搜索</button>
      </div>

      <div class="search-scope" role="tablist" aria-label="搜索类型">
        <button
          v-for="t in types"
          :key="t.value"
          class="scope-tab"
          :class="{ active: type === t.value }"
          role="tab"
          :aria-selected="type === t.value"
          @click="switchType(t.value)"
        >{{ t.label }}</button>
      </div>
    </section>

    <div class="search-body">
      <main class="result-panel" aria-live="polite">
        <div v-if="searching" class="result-skeleton" aria-label="搜索结果加载中">
          <div v-for="i in 5" :key="i" class="sk-result">
            <span class="sk-cover"></span>
            <span class="sk-copy"><i></i><i></i></span>
          </div>
        </div>

        <template v-else-if="searched">
          <div class="result-head">
            <div>
              <h2 class="result-title">搜索结果</h2>
              <p class="result-sub">找到 <b>{{ result.total }}</b> 条与「{{ result.keyword }}」相关的内容</p>
            </div>
            <span class="result-type">{{ typeName(type) || '综合' }}</span>
          </div>

          <div v-if="result.items.length" class="result-list">
            <div
              v-for="item in result.items"
              :key="item.type + item.bizId"
              class="result-item"
              role="button"
              tabindex="0"
              :aria-label="`打开${typeName(item.type)} ${stripTags(item.title)}`"
              @click="openItem(item)"
              @keydown.enter="openItem(item)"
              @keydown.space.prevent="openItem(item)"
            >
              <div class="r-cover-wrap">
                <img :src="item.coverUrl || fallback" class="r-cover" alt="" @error="onImg" />
              </div>
              <div class="r-body">
                <div class="r-title">
                  <span v-html="item.title"></span>
                  <span class="r-type">{{ typeName(item.type) }}</span>
                </div>
                <div class="r-sub">{{ item.subtitle }}</div>
                <div v-if="item.description" class="r-desc">{{ item.description }}</div>
              </div>
              <span class="r-open"><el-icon><ArrowRight /></el-icon></span>
            </div>
          </div>

          <div v-if="result.items.length && result.items.length < 4 && hotWords.length" class="result-suggest">
            <span class="suggest-label">继续探索</span>
            <div class="suggest-list">
              <button v-for="w in hotWords.slice(0, 4)" :key="w" @click="quick(w)">{{ w }}</button>
            </div>
          </div>

          <div v-else class="search-empty">
            <span class="empty-art" aria-hidden="true">
              <svg viewBox="0 0 64 64">
                <circle cx="27" cy="27" r="15" fill="none" stroke="rgba(240,171,252,.55)" stroke-width="1.8" />
                <path d="m38 38 11 11" stroke="rgba(240,171,252,.55)" stroke-width="1.8" stroke-linecap="round" />
                <path d="M21 27h12M27 21v12" stroke="rgba(255,255,255,.25)" stroke-width="1.6" stroke-linecap="round" />
              </svg>
            </span>
            <h2>没有找到相关内容</h2>
            <p>换一个关键词，或从下面的热门搜索开始。</p>
            <div class="empty-suggestions">
              <button v-for="w in hotWords.slice(0, 4)" :key="w" @click="quick(w)">{{ w }}</button>
            </div>
          </div>
        </template>

        <div v-else class="search-discovery">
          <div class="discovery-copy">
            <span class="discovery-mark" aria-hidden="true">
              <svg viewBox="0 0 56 56">
                <circle cx="24" cy="24" r="12" fill="none" stroke="rgba(240,171,252,.55)" stroke-width="1.6" />
                <path d="m33 33 10 10" stroke="rgba(240,171,252,.55)" stroke-width="1.6" stroke-linecap="round" />
                <circle cx="24" cy="24" r="4" fill="rgba(240,171,252,.22)" />
              </svg>
            </span>
            <h2>从一个名字开始</h2>
            <p>搜索歌曲、歌手、歌单或直播间，酷猫会把相关内容放在这里。</p>
          </div>
          <div class="discovery-scopes">
            <button v-for="t in types.slice(1)" :key="t.value" class="discovery-scope" @click="discoverType(t.value)">
              <span>{{ t.label }}</span>
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </div>
      </main>

      <aside class="search-aside" aria-labelledby="hot-search-title">
        <section class="aside-block">
          <div class="aside-head">
            <h2 id="hot-search-title" class="aside-title">热门搜索</h2>
            <span class="aside-meta">实时</span>
          </div>
          <ol v-if="hotWords.length" class="hot-list">
            <li v-for="(w, i) in hotWords" :key="w">
              <button @click="quick(w)">
                <span :class="['hot-index', { top: i < 3 }]">{{ String(i + 1).padStart(2, '0') }}</span>
                <span class="hot-word">{{ w }}</span>
                <el-icon><ArrowRight /></el-icon>
              </button>
            </li>
          </ol>
          <p v-else class="aside-empty">热门搜索暂不可用</p>
        </section>

        <section class="aside-block aside-tips">
          <h2 class="aside-title">搜索提示</h2>
          <p>输入歌名、歌手或直播间标题，按 Enter 搜索。Ctrl K 可以随时聚焦顶部搜索框。</p>
        </section>
      </aside>
    </div>

    <section class="search-extra" aria-labelledby="extra-title">
      <div class="extra-head">
        <div>
          <h2 id="extra-title" class="extra-title">继续发现</h2>
          <p class="extra-sub">从分类、热门词与搜索技巧继续探索</p>
        </div>
      </div>
      <div class="extra-grid">
        <section class="extra-card">
          <h3>按分类浏览</h3>
          <div class="extra-list">
            <button v-for="t in types.slice(1)" :key="t.value" @click="discoverType(t.value)">
              <span>{{ t.label }}</span>
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </section>
        <section class="extra-card">
          <h3>大家都在搜</h3>
          <div class="extra-keywords">
            <button v-for="w in hotWords" :key="w" @click="quick(w)">{{ w }}</button>
          </div>
        </section>
        <section class="extra-card">
          <h3>搜索小贴士</h3>
          <p>输入歌名、歌手或完整的直播间标题，按 Enter 开始搜索。</p>
          <p class="extra-shortcut">Ctrl K 可以随时聚焦顶部搜索框。</p>
        </section>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchApi } from '@/api'

const route = useRoute()
const router = useRouter()
const keyword = ref(route.query.keyword || '')
const type = ref('')
const hotWords = ref([])
const searched = ref(false)
const searching = ref(false)
const result = reactive({ total: 0, keyword: '', items: [] })

const fallback = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120"><rect width="120" height="120" fill="#f4e9ff"/><circle cx="60" cy="56" r="26" fill="none" stroke="#d8b8ef" stroke-width="2"/><circle cx="60" cy="56" r="5" fill="#b56bdf"/><text x="60" y="104" font-size="11" text-anchor="middle" fill="#9469ae" letter-spacing="2">KUMAO</text></svg>'
)

const types = [
  { value: '', label: '综合' },
  { value: 'SONG', label: '歌曲' },
  { value: 'SINGER', label: '歌手' },
  { value: 'PLAYLIST', label: '歌单' },
  { value: 'LIVE_ROOM', label: '直播间' },
  { value: 'MUSICIAN', label: '音乐人' }
]

const typeName = (t) => ({ SONG: '歌曲', SINGER: '歌手', PLAYLIST: '歌单', LIVE_ROOM: '直播间', MUSICIAN: '音乐人' }[t] || '综合')

function stripTags(value = '') {
  return value.replace(/<[^>]*>/g, '')
}

async function doSearch() {
  if (!keyword.value.trim()) return
  searching.value = true
  try {
    const res = await searchApi.search({ keyword: keyword.value.trim(), type: type.value, pageNum: 1, pageSize: 30 })
    result.total = res.data?.total || 0
    result.keyword = res.data?.keyword || keyword.value
    result.items = res.data?.items || []
    searched.value = true
    const nextKeyword = keyword.value.trim()
    if (route.query.keyword !== nextKeyword) {
      router.replace({ path: '/search', query: { keyword: nextKeyword } })
    }
  } catch (e) {
    result.total = 0
    result.keyword = keyword.value
    result.items = []
    searched.value = true
  } finally {
    searching.value = false
  }
}

function switchType(v) {
  type.value = v
  if (searched.value || keyword.value) doSearch()
}

function discoverType(v) {
  type.value = v
  if (keyword.value) doSearch()
}

function quick(w) {
  keyword.value = w
  doSearch()
}

function openItem(item) {
  const map = {
    SONG: () => router.push(`/song/${item.bizId}`),
    SINGER: () => router.push(`/singer/${item.bizId}`),
    PLAYLIST: () => router.push(`/playlist/${item.bizId}`),
    LIVE_ROOM: () => router.push(`/live/${item.bizId}`),
    MUSICIAN: () => router.push(`/musician/${item.bizId}`)
  }
  map[item.type]?.()
}

function onImg(e) {
  e.target.src = fallback
}

watch(() => route.query.keyword, (v) => {
  if (v && v !== keyword.value) {
    keyword.value = v
    doSearch()
  }
})

onMounted(async () => {
  try {
    const h = await searchApi.hotKeywords()
    hotWords.value = h.data || []
  } catch (e) {
    hotWords.value = []
  }
  if (keyword.value) await doSearch()
})
</script>

<style scoped>
.search-page {
  width: 100%;
  max-width: 1560px;
  margin: 0 auto;
  padding: 24px 34px 46px;
}
.search-head { margin-bottom: 18px; }
.search-title { margin: 0; font-size: 26px; line-height: 1.25; font-weight: 700; letter-spacing: -.04em; color: var(--km-text); }
.search-sub { margin: 7px 0 0; font-size: 12px; color: var(--km-text-faint); }

.search-work {
  border: 1px solid var(--km-border);
  border-radius: 20px;
  background: var(--km-card);
  padding: 14px 16px 0;
  margin-bottom: 18px;
}
.search-field {
  display: flex;
  align-items: center;
  gap: 9px;
  min-height: 48px;
  padding: 0 5px 0 13px;
  border-radius: 11px;
  border: 1px solid var(--km-border-strong);
  background: rgba(255, 255, 255, .04);
  transition: border-color .16s, background .16s;
}
.search-field:focus-within { border-color: var(--km-accent-line); background: rgba(255, 255, 255, .055); }
.field-icon { color: var(--km-text-faint); font-size: 16px; flex-shrink: 0; }
.search-input {
  flex: 1;
  min-width: 0;
  height: 44px;
  border: 0;
  outline: none;
  background: transparent;
  color: var(--km-text);
  font-size: 14px;
}
.search-input::placeholder { color: var(--km-text-faint); }
.search-input::-webkit-search-cancel-button { display: none; }
.field-count { font-size: 11px; color: var(--km-text-faint); font-variant-numeric: tabular-nums; white-space: nowrap; }
.search-submit {
  height: 38px;
  padding: 0 20px;
  border: 0;
  border-radius: 9px;
  background: var(--km-grad);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: filter .16s, opacity .16s;
  flex-shrink: 0;
}
.search-submit:hover:not(:disabled) { filter: brightness(1.06); }
.search-submit:disabled { opacity: .38; cursor: not-allowed; }
.search-scope { display: flex; align-items: center; gap: 2px; overflow-x: auto; scrollbar-width: none; }
.search-scope::-webkit-scrollbar { display: none; }
.scope-tab {
  position: relative;
  height: 42px;
  padding: 0 12px;
  border: 0;
  background: transparent;
  color: var(--km-text-faint);
  font-size: 12px;
  cursor: pointer;
  transition: color .16s;
  white-space: nowrap;
}
.scope-tab:hover { color: var(--km-text-dim); }
.scope-tab.active { color: var(--km-text); font-weight: 600; }
.scope-tab.active::after { content: ''; position: absolute; left: 12px; right: 12px; bottom: 0; height: 2px; border-radius: 2px; background: var(--km-accent); }

.search-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 292px;
  gap: 18px;
  align-items: start;
}
.result-panel {
  min-height: 0;
  border: 1px solid var(--km-border);
  border-radius: 20px;
  background: var(--km-card);
  padding: 16px;
}
.result-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; padding: 1px 2px 13px; border-bottom: 1px solid var(--km-border); }
.result-title { margin: 0; font-size: 16px; font-weight: 600; color: var(--km-text); }
.result-sub { margin: 4px 0 0; font-size: 11px; color: var(--km-text-faint); }
.result-sub b { color: var(--km-text); font-weight: 600; }
.result-type { padding: 3px 7px; border-radius: 4px; background: rgba(255, 255, 255, .04); color: var(--km-text-faint); font-size: 13px; }

.result-list { display: flex; flex-direction: column; gap: 1px; padding-top: 5px; }
.result-suggest { display: flex; align-items: flex-start; gap: 10px; margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--km-border); }
.suggest-label { flex-shrink: 0; padding-top: 3px; color: var(--km-text-faint); font-size: 11px; }
.suggest-list { display: flex; flex-wrap: wrap; gap: 5px; }
.suggest-list button { height: 28px; padding: 0 9px; border-radius: 6px; border: 1px solid var(--km-border); background: transparent; color: var(--km-text-dim); font-size: 13px; cursor: pointer; transition: border-color .16s, color .16s; }
.suggest-list button:hover { color: var(--km-text); border-color: var(--km-border-strong); }
.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 72px;
  padding: 8px 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: background .16s;
}
.result-item:hover { background: rgba(255, 255, 255, .04); }
.result-item:focus-visible { outline-offset: 1px; }
.r-cover-wrap { width: 54px; height: 54px; flex-shrink: 0; border-radius: 8px; overflow: hidden; background: #f4eef8; }
.r-cover { width: 100%; height: 100%; object-fit: cover; display: block; }
.r-body { flex: 1; min-width: 0; }
.r-title { display: flex; align-items: center; gap: 7px; min-width: 0; font-size: 13px; font-weight: 600; color: var(--km-text); }
.r-title span:first-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.r-title :deep(em) { color: #f0abfc; font-style: normal; }
.r-type { flex-shrink: 0; padding: 1px 5px; border-radius: 3px; border: 1px solid var(--km-border); color: var(--km-text-faint); font-size: 13px; font-weight: 500; }
.r-sub { margin-top: 3px; font-size: 11px; color: var(--km-text-dim); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.r-desc { margin-top: 3px; font-size: 13px; color: var(--km-text-faint); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.r-open { color: var(--km-text-faint); display: grid; place-items: center; width: 28px; height: 28px; flex-shrink: 0; }

.search-empty { min-height: 330px; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; gap: 7px; padding: 24px; }
.empty-art { width: 60px; height: 60px; }
.empty-art svg { width: 100%; height: 100%; }
.search-empty h2 { margin: 4px 0 0; font-size: 16px; font-weight: 600; color: var(--km-text); }
.search-empty p { margin: 0; font-size: 11px; color: var(--km-text-faint); }
.empty-suggestions { display: flex; flex-wrap: wrap; justify-content: center; gap: 5px; margin-top: 6px; }
.empty-suggestions button { height: 27px; padding: 0 9px; border-radius: 5px; border: 1px solid var(--km-border); background: transparent; color: var(--km-text-dim); font-size: 11px; cursor: pointer; }
.empty-suggestions button:hover { border-color: var(--km-border-strong); color: var(--km-text); }

.search-discovery { min-height: 380px; display: flex; flex-direction: column; justify-content: space-between; gap: 24px; padding: 20px 8px 8px; }
.discovery-copy { max-width: 470px; }
.discovery-mark { display: block; width: 56px; height: 56px; margin-bottom: 12px; }
.discovery-mark svg { width: 100%; height: 100%; }
.discovery-copy h2 { margin: 0; font-size: 20px; font-weight: 600; letter-spacing: -.02em; color: var(--km-text); }
.discovery-copy p { margin: 6px 0 0; font-size: 12px; line-height: 1.6; color: var(--km-text-faint); }
.discovery-scopes { display: grid; grid-template-columns: repeat(3, 1fr); gap: 7px; max-width: 620px; }
.discovery-scope { display: flex; align-items: center; justify-content: space-between; height: 42px; padding: 0 11px; border-radius: 9px; border: 1px solid var(--km-border); background: rgba(255, 255, 255, .025); color: var(--km-text-dim); font-size: 13px; cursor: pointer; transition: border-color .16s, color .16s; }
.discovery-scope:hover { color: var(--km-text); border-color: var(--km-border-strong); }

.result-skeleton { display: flex; flex-direction: column; gap: 8px; padding-top: 6px; }
.sk-result { display: flex; gap: 12px; align-items: center; min-height: 72px; padding: 8px; }
.sk-cover { width: 54px; height: 54px; border-radius: 8px; background: rgba(255, 255, 255, .05); }
.sk-copy { flex: 1; display: flex; flex-direction: column; gap: 7px; }
.sk-copy i { display: block; height: 10px; width: 42%; border-radius: 4px; background: rgba(255, 255, 255, .05); }
.sk-copy i:last-child { width: 68%; height: 8px; background: rgba(255, 255, 255, .035); }

.search-aside { display: flex; flex-direction: column; gap: 12px; position: sticky; top: calc(var(--km-topbar-h) + 16px); }
.aside-block { padding: 15px; border: 1px solid var(--km-border); border-radius: 16px; background: var(--km-card); }
.aside-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 9px; }
.aside-title { margin: 0; font-size: 13px; font-weight: 600; color: var(--km-text); }
.aside-meta { font-size: 11px; color: var(--km-text-faint); }
.hot-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 1px; }
.hot-list button { display: flex; align-items: center; gap: 9px; width: 100%; min-height: 34px; padding: 0 4px; border: 0; border-radius: 7px; background: transparent; color: var(--km-text-dim); font-size: 11px; cursor: pointer; text-align: left; transition: background .16s, color .16s; }
.hot-list button:hover { background: rgba(255, 255, 255, .04); color: var(--km-text); }
.hot-index { width: 18px; font-variant-numeric: tabular-nums; color: var(--km-text-faint); font-size: 13px; }
.hot-index.top { color: #f0abfc; }
.hot-word { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hot-list .el-icon { color: var(--km-text-faint); font-size: 11px; }
.aside-tips p { margin: 7px 0 0; color: var(--km-text-faint); font-size: 13px; line-height: 1.65; }
.aside-empty { margin: 8px 0 2px; color: var(--km-text-faint); font-size: 11px; }

.search-extra { display: none; margin-top: 18px; padding: 16px; border: 1px solid var(--km-border); border-radius: 16px; background: var(--km-card); }
.extra-head { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 14px; }
.extra-title { margin: 0; font-size: 18px; line-height: 1.3; font-weight: 600; letter-spacing: -.02em; color: var(--km-text); }
.extra-sub { margin: 4px 0 0; font-size: 13px; color: var(--km-text-faint); }
.extra-grid { display: grid; grid-template-columns: 1fr 1fr 1.1fr; gap: 0; }
.extra-card { min-width: 0; min-height: 0; padding: 0 18px; border-left: 1px solid var(--km-border); }
.extra-card:first-child { padding-left: 0; border-left: 0; }
.extra-card:last-child { padding-right: 0; }
.extra-card h3 { margin: 0 0 10px; font-size: 14px; font-weight: 600; color: var(--km-text); }
.extra-card p { margin: 0; color: var(--km-text-faint); font-size: 13px; line-height: 1.65; }
.extra-card p + p { margin-top: 8px; }
.extra-list { display: flex; flex-direction: column; gap: 2px; }
.extra-list button { display: flex; align-items: center; justify-content: space-between; min-height: 27px; padding: 0 3px; border: 0; background: transparent; color: var(--km-text-dim); font-size: 13px; cursor: pointer; text-align: left; }
.extra-list button:hover { color: var(--km-text); }
.extra-list .el-icon { color: var(--km-text-faint); font-size: 12px; }
.extra-keywords { display: flex; flex-wrap: wrap; gap: 6px; }
.extra-keywords button { height: 29px; padding: 0 10px; border: 1px solid var(--km-border); border-radius: 5px; background: transparent; color: var(--km-text-dim); font-size: 13px; cursor: pointer; }
.extra-keywords button:hover { color: var(--km-text); border-color: var(--km-border-strong); }
.extra-shortcut { color: var(--km-text-dim) !important; }

@media (min-width: 1500px) {
  .search-extra { display: block; }
}

/* ---- 可读性层级 ---- */
.search-sub, .scope-tab, .result-sub, .r-sub, .hot-list button { font-size: 13px; }
.r-desc, .aside-meta, .aside-tips p, .suggest-label, .suggest-list button { font-size: 12px; }
.r-title { font-size: 14px; }
.aside-title { font-size: 14px; }

@media (max-width: 1180px) {
  .search-page { padding: 22px 20px 42px; }
  .search-body { grid-template-columns: minmax(0, 1fr) 260px; }
}

@media (max-width: 980px) {
  .search-body { grid-template-columns: 1fr; }
  .search-aside { position: static; display: grid; grid-template-columns: 1fr 1fr; }
}

@media (max-width: 620px) {
  .search-page { padding: 16px 16px 32px; }
  .search-title { font-size: 22px; }
  .search-work { padding: 10px 10px 0; margin-bottom: 13px; }
  .search-field { min-height: 44px; padding-left: 10px; }
  .search-input { font-size: 13px; }
  .field-count { display: none; }
  .search-submit { height: 34px; padding: 0 14px; font-size: 12px; }
  .scope-tab { height: 38px; padding: 0 9px; font-size: 11px; }
  .result-panel { padding: 12px; min-height: 0; }
  .result-item { gap: 9px; min-height: 64px; padding: 6px 4px; }
  .r-cover-wrap { width: 46px; height: 46px; }
  .r-type { display: none; }
  .r-desc { display: none; }
  .search-aside { grid-template-columns: 1fr; }
  .discovery-scopes { grid-template-columns: 1fr 1fr; }
  .search-discovery { min-height: 320px; padding: 12px 4px 4px; }
}
</style>
