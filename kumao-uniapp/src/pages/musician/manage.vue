<template>
  <view class="km-screen manage">
    <km-nav-bar title="作品管理">
      <view class="ibtn" @click="loadAll">
        <km-icon name="search" :size="18" color="rgba(247,241,232,.82)" />
      </view>
    </km-nav-bar>

    <!-- 切换 -->
    <view class="tabs">
      <view v-for="t in tabs" :key="t.key" :class="['tab', { on: tab === t.key }]" @click="tab = t.key">
        {{ t.label }}<text class="tab-n">{{ counts[t.key] }}</text>
      </view>
    </view>

    <view v-if="loading" class="empty">正在加载…</view>

    <!-- 歌曲 -->
    <view v-else-if="tab === 'song'" class="list">
      <view v-for="w in works" :key="w.id" class="card">
        <image class="cover" :src="coverOf(w.coverUrl)" mode="aspectFill" />
        <view class="info">
          <view class="row1">
            <text class="name">{{ w.name }}</text>
            <text :class="['pill', statusClass(w.status)]">{{ statusText(w.status) }}</text>
          </view>
          <text class="sub">{{ w.albumName || '单曲' }} · {{ w.style || '未分类' }} · {{ formatPlay(w.playCount) }} 播放</text>
          <view class="ops">
            <view class="op" @click="editWork(w)">编辑</view>
            <view v-if="w.status === 'APPROVED'" class="op warn" @click="offlineWork(w)">下架</view>
            <view v-if="w.status === 'OFFLINE'" class="op gold" @click="onlineWork(w)">重新上架</view>
            <view class="op danger" @click="removeWork(w)">删除</view>
          </view>
        </view>
      </view>
      <view v-if="!works.length" class="empty">还没有歌曲作品，去「音乐人入驻」发布第一首</view>
    </view>

    <!-- MV -->
    <view v-else-if="tab === 'mv'" class="list">
      <view v-for="m in mvs" :key="m.id" class="card">
        <image class="cover" :src="coverOf(m.coverUrl)" mode="aspectFill" />
        <view class="info">
          <view class="row1">
            <text class="name">{{ m.name }}</text>
            <text :class="['pill', mvStatusClass(m)]">{{ mvStatusText(m) }}</text>
          </view>
          <text class="sub">{{ formatPlay(m.playCount) }} 次播放 · {{ m.quality || 'HD' }}</text>
          <view class="ops">
            <view class="op" @click="editMv(m)">编辑</view>
            <view v-if="m.status === 1" class="op warn" @click="setMvStatus(m, 0)">下架</view>
            <view v-else class="op gold" @click="setMvStatus(m, 1)">上架</view>
            <view class="op danger" @click="removeMv(m)">删除</view>
          </view>
        </view>
      </view>
      <view v-if="!mvs.length" class="empty">还没有 MV 作品</view>
    </view>

    <!-- 专辑 -->
    <view v-else class="list">
      <view v-for="a in albums" :key="a.id" class="card">
        <image class="cover" :src="coverOf(a.coverUrl)" mode="aspectFill" />
        <view class="info">
          <view class="row1">
            <text class="name">{{ a.name }}</text>
            <text :class="['pill', a.status === 1 ? 'ok' : 'off']">{{ a.status === 1 ? '已上架' : '已下架' }}</text>
          </view>
          <text class="sub">{{ a.songCount || 0 }} 首 · {{ (a.releaseDate || '').slice(0, 10) || '未设置发行日期' }}</text>
          <view class="ops">
            <view class="op" @click="editAlbum(a)">编辑</view>
            <view v-if="a.status === 1" class="op warn" @click="setAlbumStatus(a, 0)">下架</view>
            <view v-else class="op gold" @click="setAlbumStatus(a, 1)">上架</view>
          </view>
        </view>
      </view>
      <view v-if="!albums.length" class="empty">还没有专辑</view>
    </view>

    <!-- 编辑弹层 -->
    <view v-if="editor.open" class="mask" @click="editor.open = false">
      <view class="sheet" @click.stop>
        <text class="sheet-title">{{ editor.title }}</text>

        <view class="field">
          <text class="label">名称</text>
          <input v-model="editor.form.name" class="input" placeholder="请输入名称" />
        </view>

        <view v-if="editor.type === 'song'" class="field">
          <text class="label">专辑名</text>
          <input v-model="editor.form.albumName" class="input" placeholder="单曲可留空" />
        </view>

        <view v-if="editor.type === 'song'" class="field half">
          <view class="field">
            <text class="label">曲风</text>
            <input v-model="editor.form.style" class="input" placeholder="如 民谣" />
          </view>
          <view class="field">
            <text class="label">语言</text>
            <input v-model="editor.form.language" class="input" placeholder="如 国语" />
          </view>
        </view>

        <view v-if="editor.type === 'song'" class="field">
          <text class="label">音频地址</text>
          <input v-model="editor.form.audioUrl" class="input" placeholder="上传后的音频 URL" />
        </view>

        <view v-if="editor.type === 'mv'" class="field">
          <text class="label">简介</text>
          <textarea v-model="editor.form.description" class="input area" placeholder="MV 简介" />
        </view>

        <view v-if="editor.type === 'album'" class="field">
          <text class="label">专辑介绍</text>
          <textarea v-model="editor.form.intro" class="input area" placeholder="专辑介绍" />
        </view>

        <view class="field">
          <text class="label">封面地址</text>
          <input v-model="editor.form.coverUrl" class="input" placeholder="封面图片 URL" />
        </view>

        <view v-if="editor.type !== 'album'" class="switch-row" @click="editor.form.vipOnly = !editor.form.vipOnly">
          <text class="label">仅 VIP 可听/看</text>
          <view :class="['switch', { on: editor.form.vipOnly }]"><view class="knob"></view></view>
        </view>

        <view class="sheet-ops">
          <view class="btn ghost" @click="editor.open = false">取消</view>
          <view class="btn primary" @click="save">保存</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import KmNavBar from '@/components/km-nav-bar.vue'
import KmIcon from '@/components/km-icon.vue'
import { api } from '@/api'
import { coverOf, formatPlay } from '@/utils/media'

const tabs = [
  { key: 'song', label: '歌曲' },
  { key: 'mv', label: 'MV' },
  { key: 'album', label: '专辑' }
]
const tab = ref('song')
const loading = ref(true)
const works = ref([])
const mvs = ref([])
const albums = ref([])

const counts = computed(() => ({ song: works.value.length, mv: mvs.value.length, album: albums.value.length }))

const editor = reactive({ open: false, type: 'song', title: '', id: null, form: {} })

function statusText(s) {
  return { APPROVED: '已上架', PENDING: '审核中', REJECTED: '未通过', OFFLINE: '已下架' }[s] || s
}
function statusClass(s) {
  return { APPROVED: 'ok', PENDING: 'wait', REJECTED: 'bad', OFFLINE: 'off' }[s] || 'off'
}
function mvStatusText(m) {
  if (m.status === 0) return '已下架'
  if (m.auditStatus === 'PENDING') return '审核中'
  if (m.auditStatus === 'REJECTED') return '未通过'
  return '已上架'
}
function mvStatusClass(m) {
  if (m.status === 0) return 'off'
  if (m.auditStatus === 'PENDING') return 'wait'
  if (m.auditStatus === 'REJECTED') return 'bad'
  return 'ok'
}

async function loadAll() {
  loading.value = true
  const [w, m, a] = await Promise.all([
    api.musicianWorks({ pageNum: 1, pageSize: 100 }).catch(() => null),
    api.mvsMine({ pageNum: 1, pageSize: 100 }).catch(() => null),
    api.musicianAlbums().catch(() => null)
  ])
  works.value = (w && w.data && w.data.records) || []
  mvs.value = (m && m.data && m.data.records) || []
  albums.value = (a && a.data) || []
  loading.value = false
}

function editWork(w) {
  editor.type = 'song'
  editor.id = w.id
  editor.title = '编辑歌曲'
  editor.form = {
    name: w.name,
    albumName: w.albumName || '',
    style: w.style || '',
    language: w.language || '',
    audioUrl: w.audioUrl || '',
    coverUrl: w.coverUrl || '',
    vipOnly: !!w.vipOnly
  }
  editor.open = true
}

function editMv(m) {
  editor.type = 'mv'
  editor.id = m.id
  editor.title = '编辑 MV'
  editor.form = {
    name: m.name,
    description: m.description || '',
    coverUrl: m.coverUrl || '',
    videoUrl: m.videoUrl || '',
    vipOnly: !!m.vipOnly
  }
  editor.open = true
}

function editAlbum(a) {
  editor.type = 'album'
  editor.id = a.id
  editor.title = '编辑专辑'
  editor.form = { name: a.name, intro: a.intro || '', coverUrl: a.coverUrl || '' }
  editor.open = true
}

async function save() {
  const f = editor.form
  if (!f.name) return uni.showToast({ title: '请填写名称', icon: 'none' })
  try {
    if (editor.type === 'song') {
      await api.updateMusicianWork(editor.id, {
        name: f.name,
        albumName: f.albumName,
        style: f.style,
        language: f.language,
        audioUrl: f.audioUrl,
        coverUrl: f.coverUrl,
        vipOnly: f.vipOnly
      })
      uni.showToast({ title: '已保存，待审核', icon: 'none' })
    } else if (editor.type === 'mv') {
      await api.updateMvMine(editor.id, {
        name: f.name,
        description: f.description,
        coverUrl: f.coverUrl,
        videoUrl: f.videoUrl,
        vipOnly: f.vipOnly
      })
      uni.showToast({ title: '已保存，待审核', icon: 'none' })
    } else {
      await api.updateMusicianAlbum(editor.id, { name: f.name, intro: f.intro, coverUrl: f.coverUrl })
      uni.showToast({ title: '已保存', icon: 'none' })
    }
    editor.open = false
    await loadAll()
  } catch (e) {
    /* toast 已由 request 统一提示 */
  }
}

async function offlineWork(w) {
  await api.offlineMusicianWork(w.id)
  uni.showToast({ title: '已下架', icon: 'none' })
  loadAll()
}

async function onlineWork(w) {
  await api.onlineMusicianWork(w.id)
  uni.showToast({ title: '已提交重新审核', icon: 'none' })
  loadAll()
}

async function setMvStatus(m, status) {
  await api.updateMvMineStatus(m.id, status)
  uni.showToast({ title: status === 1 ? '已上架' : '已下架', icon: 'none' })
  loadAll()
}

async function setAlbumStatus(a, status) {
  await api.updateMusicianAlbumStatus(a.id, status)
  uni.showToast({ title: status === 1 ? '已上架' : '已下架', icon: 'none' })
  loadAll()
}

function confirmRemove(title, fn) {
  uni.showModal({
    title,
    content: '删除后不可恢复，确定继续？',
    confirmText: '删除',
    confirmColor: '#e5484d',
    success: async (r) => {
      if (!r.confirm) return
      await fn()
      uni.showToast({ title: '已删除', icon: 'none' })
      loadAll()
    }
  })
}

function removeWork(w) {
  confirmRemove('删除歌曲', () => api.deleteMusicianWork(w.id))
}
function removeMv(m) {
  confirmRemove('删除 MV', () => api.deleteMvMine(m.id))
}

onLoad(() => loadAll())
</script>

<style scoped>
.manage { padding-bottom: calc(var(--window-bottom, 0px) + 40rpx); }
.ibtn { width: 56rpx; height: 56rpx; border-radius: 50%; background: rgba(255,255,255,.055); display: flex; align-items: center; justify-content: center; }
.tabs { display: flex; gap: 12rpx; padding: 18rpx 24rpx 6rpx; }
.tab {
  flex: 1; text-align: center; padding: 16rpx 0; border-radius: 14rpx;
  background: rgba(255,255,255,.05); color: rgba(247,241,232,.55); font-size: 24rpx;
}
.tab.on { background: rgba(245,180,91,.16); color: #f5cf9a; font-weight: 800; }
.tab-n { font-size: 19rpx; opacity: .7; margin-left: 6rpx; }
.list { padding: 14rpx 24rpx; display: flex; flex-direction: column; gap: 14rpx; }
.card { display: flex; gap: 16rpx; padding: 14rpx; border-radius: 16rpx; background: rgba(255,255,255,.04); }
.cover { width: 120rpx; height: 120rpx; border-radius: 12rpx; flex: none; background: #17151b; }
.info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8rpx; }
.row1 { display: flex; align-items: center; gap: 10rpx; }
.name { flex: 1; font-size: 25rpx; color: #f3f4f6; font-weight: 700; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pill { font-size: 17rpx; padding: 3rpx 10rpx; border-radius: 6rpx; flex: none; }
.pill.ok { background: rgba(74,222,128,.14); color: #86efac; }
.pill.wait { background: rgba(245,180,91,.16); color: #f5cf9a; }
.pill.bad { background: rgba(229,72,77,.16); color: #ff9a9d; }
.pill.off { background: rgba(255,255,255,.08); color: rgba(247,241,232,.45); }
.sub { font-size: 20rpx; color: rgba(247,241,232,.4); }
.ops { display: flex; gap: 12rpx; margin-top: 4rpx; }
.op { padding: 8rpx 18rpx; border-radius: 999rpx; background: rgba(255,255,255,.07); color: rgba(247,241,232,.75); font-size: 20rpx; }
.op.warn { background: rgba(245,180,91,.16); color: #f5cf9a; }
.op.gold { background: linear-gradient(135deg,#fbd08a,#de823c); color: #22150a; font-weight: 800; }
.op.danger { background: rgba(229,72,77,.14); color: #ff9a9d; }
.empty { padding: 80rpx 24rpx; text-align: center; font-size: 22rpx; color: rgba(247,241,232,.35); }

.mask { position: fixed; inset: 0; z-index: 90; background: rgba(0,0,0,.66); display: flex; align-items: flex-end; }
.sheet { width: 100%; background: #121116; border-radius: 26rpx 26rpx 0 0; padding: 26rpx 24rpx calc(30rpx + env(safe-area-inset-bottom)); box-sizing: border-box; }
.sheet-title { display: block; font-size: 27rpx; font-weight: 800; color: #f7f1e8; margin-bottom: 14rpx; }
.field { display: flex; flex-direction: column; gap: 8rpx; margin-bottom: 16rpx; }
.field.half { flex-direction: row; gap: 14rpx; }
.field.half > .field { flex: 1; margin-bottom: 0; }
.label { font-size: 20rpx; color: rgba(247,241,232,.45); }
.input {
  background: rgba(255,255,255,.06); border-radius: 12rpx; padding: 0 16rpx;
  height: 76rpx; color: #f3f4f6; font-size: 23rpx;
}
.input.area { height: 130rpx; padding: 14rpx 16rpx; }
.switch-row { display: flex; align-items: center; justify-content: space-between; margin: 4rpx 0 18rpx; }
.switch { width: 76rpx; height: 42rpx; border-radius: 999rpx; background: rgba(255,255,255,.12); padding: 4rpx; box-sizing: border-box; }
.switch.on { background: rgba(245,180,91,.55); }
.knob { width: 34rpx; height: 34rpx; border-radius: 50%; background: #fff; transition: transform .18s ease; }
.switch.on .knob { transform: translateX(34rpx); }
.sheet-ops { display: flex; gap: 14rpx; margin-top: 6rpx; }
.btn { flex: 1; height: 84rpx; line-height: 84rpx; text-align: center; border-radius: 14rpx; font-size: 24rpx; }
.btn.ghost { background: rgba(255,255,255,.06); color: rgba(247,241,232,.6); }
.btn.primary { background: linear-gradient(135deg,#fbd08a,#de823c); color: #22150a; font-weight: 800; }
</style>

