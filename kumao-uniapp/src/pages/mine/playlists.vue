<template>
  <view class="page">
    <km-nav-bar title="我的歌单" />

    <view class="topbar">
      <view class="seg">
        <view :class="['seg-item', { on: tab === 'mine' }]" @click="switchTab('mine')">我创建的</view>
        <view :class="['seg-item', { on: tab === 'collected' }]" @click="switchTab('collected')">我收藏的</view>
      </view>
      <view class="create" @click="openCreate">
        <km-icon name="plus" :size="16" color="#22150a" />
        <text>新建</text>
      </view>
    </view>

    <view v-if="loading" class="state"><view class="spinner"></view><text>正在加载歌单…</text></view>

    <view v-else-if="!list.length" class="empty">
      <view class="empty-ico"><km-icon name="list" :size="28" color="rgba(245,180,91,.8)" /></view>
      <text class="empty-title">{{ tab === 'mine' ? '还没有创建歌单' : '还没有收藏歌单' }}</text>
      <text class="empty-sub">{{ tab === 'mine' ? '把喜欢的歌整理成歌单，随时一键播放。' : '在歌单广场收藏喜欢的歌单，会出现在这里。' }}</text>
      <view class="empty-btn" @click="tab === 'mine' ? openCreate() : goSquare()">{{ tab === 'mine' ? '新建歌单' : '去歌单广场' }}</view>
    </view>

    <scroll-view v-else scroll-y class="list" @scrolltolower="loadMore">
      <view v-for="p in list" :key="p.id" class="card" @click="openDetail(p)">
        <image class="cover" :src="coverOf(p.coverUrl)" mode="aspectFill" />
        <view class="meta">
          <text class="name">{{ p.name }}</text>
          <text class="desc">{{ p.description || '暂无简介' }}</text>
          <text class="count">{{ p.songCount || p.songs || 0 }} 首 · {{ p.playCount ? formatCount(p.playCount) + ' 次播放' : '刚刚创建' }}</text>
        </view>
        <km-icon name="chevron-right" :size="16" color="rgba(247,241,232,.3)" />
      </view>
      <view class="list-foot">{{ hasMore ? '加载中…' : '没有更多了' }}</view>
    </scroll-view>

    <view v-if="creating" class="mask" @click="creating = false">
      <view class="sheet" @click.stop>
        <text class="sheet-title">新建歌单</text>
        <input v-model="form.name" class="field" placeholder="给歌单起个名字" placeholder-class="ph" maxlength="30" />
        <input v-model="form.description" class="field" placeholder="简介（选填）" placeholder-class="ph" maxlength="80" />
        <view class="sheet-actions">
          <view class="cancel" @click="creating = false">取消</view>
          <view class="confirm" @click="submitCreate">创建</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { api } from '@/api'
import KmNavBar from '@/components/km-nav-bar.vue'
import KmIcon from '@/components/km-icon.vue'
import { coverOf, formatCount } from '@/utils/media'

const tab = ref('mine')
const list = ref([])
const page = ref(1)
const hasMore = ref(true)
const loading = ref(true)
const creating = ref(false)
const form = reactive({ name: '', description: '' })

function goLoginIfNeeded() {
  if (uni.getStorageSync('km_token')) return true
  uni.navigateTo({ url: '/pages/login/login' })
  return false
}

async function load(reset = false) {
  if (!goLoginIfNeeded()) {
    loading.value = false
    return
  }
  if (reset) {
    page.value = 1
    hasMore.value = true
  }
  if (!hasMore.value) return
  loading.value = page.value === 1
  const fetcher = tab.value === 'mine' ? api.playlistsMine : api.playlistsCollected
  const res = await fetcher({ pageNum: page.value, pageSize: 20 }).catch(() => null)
  const data = (res && res.data) || {}
  const records = data.records || []
  list.value = page.value === 1 ? records : list.value.concat(records)
  hasMore.value = records.length === 20
  page.value += 1
  loading.value = false
}

function loadMore() {
  if (hasMore.value && !loading.value) load()
}

function switchTab(t) {
  if (tab.value === t) return
  tab.value = t
  list.value = []
  load(true)
}

function openCreate() {
  if (!goLoginIfNeeded()) return
  form.name = ''
  form.description = ''
  creating.value = true
}

async function submitCreate() {
  const name = form.name.trim()
  if (!name) {
    uni.showToast({ title: '请输入歌单名称', icon: 'none' })
    return
  }
  try {
    await api.createPlaylist({ name, description: form.description.trim(), publicFlag: 1 })
    creating.value = false
    uni.showToast({ title: '歌单已创建', icon: 'success' })
    tab.value = 'mine'
    load(true)
  } catch (e) { /* toast handled */ }
}

function openDetail(p) {
  uni.navigateTo({ url: `/pages/playlist/detail?id=${p.id}` })
}

function goSquare() {
  uni.navigateTo({ url: '/pages/playlist/playlist' })
}

onLoad(() => load(true))
onShow(() => {
  if (list.value.length) load(true)
})
</script>

<style scoped>
.page { min-height: 100vh; background: #08070a; padding-bottom: 60rpx; box-sizing: border-box; }
.topbar { display: flex; align-items: center; gap: 14rpx; padding: 6rpx 24rpx 20rpx; }
.seg { flex: 1; display: flex; padding: 5rpx; border-radius: 999rpx; background: rgba(255,255,255,.05); }
.seg-item { flex: 1; text-align: center; height: 56rpx; line-height: 56rpx; border-radius: 999rpx; font-size: 22rpx; color: rgba(247,241,232,.5); }
.seg-item.on { background: rgba(245,180,91,.16); color: #f5cf9a; font-weight: 800; }
.create {
  display: flex; align-items: center; gap: 4rpx; height: 62rpx; padding: 0 22rpx; border-radius: 999rpx;
  background: linear-gradient(135deg,#fbd08a,#de823c); color: #22150a; font-size: 22rpx; font-weight: 800;
}
.state { padding: 120rpx 0; display: flex; flex-direction: column; align-items: center; gap: 16rpx; color: rgba(247,241,232,.4); font-size: 23rpx; }
.spinner { width: 44rpx; height: 44rpx; border-radius: 50%; border: 4rpx solid rgba(255,255,255,.14); border-top-color: #f5b45b; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.empty { padding: 110rpx 60rpx; display: flex; flex-direction: column; align-items: center; gap: 12rpx; }
.empty-ico { width: 108rpx; height: 108rpx; border-radius: 50%; background: rgba(245,180,91,.08); display: flex; align-items: center; justify-content: center; }
.empty-title { font-size: 30rpx; font-weight: 800; color: #f7f1e8; }
.empty-sub { font-size: 22rpx; color: rgba(247,241,232,.38); text-align: center; line-height: 1.6; }
.empty-btn { margin-top: 10rpx; height: 66rpx; line-height: 66rpx; padding: 0 30rpx; border-radius: 999rpx; background: rgba(245,180,91,.14); border: 1rpx solid rgba(245,180,91,.3); color: #f5cf9a; font-size: 23rpx; font-weight: 700; }
.list { padding: 0 20rpx; }
.card { display: flex; align-items: center; gap: 18rpx; padding: 16rpx; margin-bottom: 14rpx; border-radius: 20rpx; background: rgba(255,255,255,.035); border: 1rpx solid rgba(255,255,255,.05); }
.card:active { background: rgba(255,255,255,.06); }
.cover { width: 116rpx; height: 116rpx; border-radius: 14rpx; flex: none; background: #17151b; }
.meta { flex: 1; min-width: 0; }
.name { display: block; font-size: 26rpx; font-weight: 700; color: #f3f4f6; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.desc { display: block; margin-top: 4rpx; font-size: 20rpx; color: rgba(247,241,232,.36); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.count { display: block; margin-top: 6rpx; font-size: 19rpx; color: rgba(245,180,91,.7); }
.list-foot { text-align: center; padding: 24rpx 0 40rpx; font-size: 20rpx; color: rgba(247,241,232,.24); }
.mask { position: fixed; inset: 0; background: rgba(0,0,0,.6); display: flex; align-items: flex-end; z-index: 60; }
.sheet { width: 100%; padding: 30rpx 26rpx 50rpx; background: #121116; border-radius: 28rpx 28rpx 0 0; box-sizing: border-box; }
.sheet-title { display: block; font-size: 30rpx; font-weight: 800; color: #f7f1e8; margin-bottom: 22rpx; }
.field {
  height: 84rpx; padding: 0 24rpx; margin-bottom: 16rpx; border-radius: 16rpx;
  background: rgba(255,255,255,.05); border: 1rpx solid rgba(255,255,255,.07);
  color: #f3f4f6; font-size: 24rpx;
}
.ph { color: rgba(243,244,246,.26); }
.sheet-actions { display: flex; gap: 14rpx; margin-top: 10rpx; }
.cancel, .confirm { flex: 1; height: 80rpx; line-height: 80rpx; text-align: center; border-radius: 16rpx; font-size: 25rpx; font-weight: 800; }
.cancel { background: rgba(255,255,255,.06); color: rgba(247,241,232,.6); }
.confirm { background: linear-gradient(135deg,#fbd08a,#de823c); color: #22150a; }
</style>

