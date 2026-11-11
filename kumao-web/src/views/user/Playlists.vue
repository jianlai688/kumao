<template>
  <div class="km-page">
    <div class="page-head">
      <div>
        <h1 class="page-title">歌单广场</h1>
        <p class="page-sub">每一份歌单，都是一种心情</p>
      </div>
      <button class="btn-accent" @click="openCreate">
        <el-icon><Plus /></el-icon> 创建歌单
      </button>
    </div>

    <div class="chips-row">
      <button class="km-chip" :class="{ active: sort === 'hot' }" @click="switchSort('hot')">最热</button>
      <button class="km-chip" :class="{ active: sort === 'new' }" @click="switchSort('new')">最新</button>
    </div>

    <div class="pl-grid">
      <div
        v-for="pl in list"
        :key="pl.id"
        class="pl-card km-hover-card"
        @click="$router.push(`/playlist/${pl.id}`)"
      >
        <div class="cover-wrap">
          <img :src="pl.coverUrl || fallback" class="pl-cover" @error="onImg" />
          <div class="play-mask"><el-icon><VideoPlay /></el-icon></div>
          <div class="pl-count">
            <el-icon><Headset /></el-icon> {{ pl.songCount }}
          </div>
        </div>
        <div class="pl-name">{{ pl.name }}</div>
        <div class="pl-sub">{{ pl.creatorName }} · 收藏 {{ formatNum(pl.collectCount) }}</div>
      </div>
    </div>
    <el-empty v-if="!list.length" description="暂无公开歌单" />

    <div class="km-pager">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="query.pageSize"
                     v-model:current-page="query.pageNum" @current-change="load" />
    </div>

    <el-dialog v-model="createDialog" title="创建歌单" width="460px">
      <el-form label-position="top">
        <el-form-item label="歌单名称">
          <el-input v-model="form.name" placeholder="给歌单起个名字" size="large" />
        </el-form-item>
        <el-form-item label="封面">
          <MediaDropzone
            v-model="form.coverUrl"
            kind="cover"
            biz-type="cover"
            title="上传歌单封面"
            hint="JPG / PNG / WebP / GIF · 最大 10MB"
            pick-label="从文件夹选择"
            :max-size-mb="10"
          />
          <el-input v-model="form.coverUrl" placeholder="或填写封面图片地址" style="margin-top:8px" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="介绍一下这个歌单吧" />
        </el-form-item>
        <el-form-item label="可见性">
          <el-radio-group v-model="form.isPublic">
            <el-radio-button :value="1">公开</el-radio-button>
            <el-radio-button :value="0">私有</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" round @click="create">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { playlistApi } from '@/api'
import { useUserStore } from '@/stores/user'
import MediaDropzone from '@/components/studio/MediaDropzone.vue'

const router = useRouter()
const userStore = useUserStore()
const list = ref([])
const total = ref(0)
const sort = ref('hot')
const query = reactive({ pageNum: 1, pageSize: 20 })
const createDialog = ref(false)
const form = reactive({
  name: '',
  coverUrl: 'https://picsum.photos/seed/newpl/600',
  description: '',
  isPublic: 1
})

const fallback = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="300" height="300"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#1f2127"/><stop offset="1" stop-color="#121316"/></linearGradient></defs><rect width="300" height="300" fill="url(#g)"/><text x="150" y="172" font-size="58" text-anchor="middle" fill="#33363e">♫</text></svg>'
)

function formatNum(n) {
  if (!n) return '0'
  return n >= 10000 ? (n / 10000).toFixed(1) + '万' : String(n)
}

function switchSort(v) {
  sort.value = v
  query.pageNum = 1
  load()
}

function openCreate() {
  if (!userStore.isLogin) return router.push('/login')
  createDialog.value = true
}

function onImg(e) {
  e.target.src = fallback
}

async function load() {
  const res = await playlistApi.publicPage({ ...query, sort: sort.value })
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

async function create() {
  if (!form.name.trim()) return ElMessage.warning('请输入歌单名称')
  const res = await playlistApi.create(form)
  ElMessage.success('创建成功')
  createDialog.value = false
  router.push(`/playlist/${res.data}`)
}

onMounted(load)
</script>

<style scoped>
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-bottom: 22px; }
.page-title { margin: 0; font-size: 32px; font-weight: 900; letter-spacing: -1px; }
.page-sub { margin: 8px 0 0; font-size: 13px; color: var(--km-text-dim); }

.btn-accent {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 40px;
  padding: 0 20px;
  border-radius: 999px;
  border: none;
  background: var(--km-accent);
  color: #07080a;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: all .2s;
  box-shadow: 0 10px 26px rgba(192, 132, 252, .22);
}
.btn-accent:hover { background: #d8b4fe; transform: translateY(-1px); }

.chips-row { display: flex; gap: 10px; margin-bottom: 24px; }

.pl-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 22px;
}

.pl-card { cursor: pointer; }

.pl-cover {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  display: block;
}

.pl-count {
  position: absolute;
  right: 10px;
  bottom: 10px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 9px;
  border-radius: 999px;
  background: rgba(0, 0, 0, .58);
  backdrop-filter: blur(8px);
  color: rgba(255, 255, 255, .9);
}

.pl-name {
  margin-top: 12px;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pl-sub {
  font-size: 12px;
  color: var(--km-text-dim);
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
