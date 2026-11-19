<template>
  <view class="page">
    <km-nav-bar :title="name || '音乐人'" />

    <view v-if="loading" class="state"><view class="spinner"></view><text>正在加载音乐人主页…</text></view>

    <view v-else-if="error" class="state">
      <text>主页加载失败</text>
      <view class="retry" @click="load">重新加载</view>
    </view>

    <template v-else>
      <view class="banner">
        <image class="banner-bg" :src="avatarOf(musician.avatar)" mode="aspectFill" />
        <view class="banner-shade"></view>
        <view class="profile">
          <image class="avatar" :src="avatarOf(musician.avatar)" mode="aspectFill" />
          <view class="profile-copy">
            <text class="stage">{{ musician.stageName || '音乐人' }}</text>
            <view class="tags">
              <text v-if="musician.style" class="tag">{{ musician.style }}</text>
              <text v-if="musician.representative" class="tag ghost">代表作 {{ musician.representative }}</text>
            </view>
          </view>
          <view :class="['follow', { on: followed }]" @click="toggleFollow">
            {{ followed ? '已关注' : '+ 关注' }}
          </view>
        </view>
      </view>

      <view class="stats">
        <view class="stat"><text class="n">{{ formatCount(musician.fansCount) }}</text><text class="l">粉丝</text></view>
        <view class="stat"><text class="n">{{ formatCount(musician.totalPlayCount) }}</text><text class="l">播放</text></view>
        <view class="stat"><text class="n">{{ formatCount(musician.totalLikeCount) }}</text><text class="l">获赞</text></view>
        <view class="stat"><text class="n">{{ works.length }}</text><text class="l">作品</text></view>
      </view>

      <view class="intro" v-if="musician.intro">
        <text class="intro-label">简介</text>
        <text class="intro-text">{{ musician.intro }}</text>
      </view>

      <view class="section-head">
        <text class="section-title">作品</text>
        <text class="section-sub">{{ works.length }} 首</text>
      </view>

      <view v-if="!works.length" class="empty">这位音乐人还没有发布作品</view>
      <view v-else class="works">
        <view v-for="(w, i) in works" :key="w.id" class="work" @click="playWork(i)">
          <image class="w-cover" :src="coverOf(w.coverUrl)" mode="aspectFill" />
          <view class="w-meta">
            <text class="w-name">{{ w.title || w.name }}</text>
            <text class="w-sub">{{ w.album || w.style || musician.stageName }} · {{ formatCount(w.playCount) }} 次播放</text>
          </view>
          <view class="w-play"><km-icon name="play" :size="14" color="#22150a" /></view>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '@/api'
import KmNavBar from '@/components/km-nav-bar.vue'
import KmIcon from '@/components/km-icon.vue'
import { avatarOf, coverOf, formatCount } from '@/utils/media'
import { play } from '@/utils/player'

const id = ref('')
const loading = ref(true)
const error = ref(false)
const followed = ref(false)
const musician = ref({})
const works = ref([])

const name = computed(() => musician.value.stageName || '')

async function load() {
  loading.value = true
  error.value = false
  const res = await api.musicianHomepage(id.value).catch(() => null)
  if (!res || !res.data) {
    error.value = true
    loading.value = false
    return
  }
  const data = res.data
  musician.value = data.musician || {}
  followed.value = !!data.followed
  works.value = (data.works || []).map((w) => ({
    ...w,
    id: w.songId || w.id,
    name: w.title || w.name,
    audioUrl: w.audioUrl || w.url
  }))
  loading.value = false
}

async function toggleFollow() {
  if (!uni.getStorageSync('km_token')) {
    uni.navigateTo({ url: '/pages/login/login' })
    return
  }
  try {
    if (followed.value) {
      await api.musicianUnfollow(id.value)
      followed.value = false
      musician.value.fansCount = Math.max(0, (musician.value.fansCount || 0) - 1)
    } else {
      await api.musicianFollow(id.value)
      followed.value = true
      musician.value.fansCount = (musician.value.fansCount || 0) + 1
    }
  } catch (e) { /* toast handled */ }
}

function playWork(i) {
  const w = works.value[i]
  if (!w.audioUrl) {
    uni.showToast({ title: '该作品暂无音源', icon: 'none' })
    return
  }
  play(w, works.value)
  uni.navigateTo({ url: `/pages/song/song?id=${w.id}` })
}

onLoad((options) => {
  id.value = String(options.id || '')
  if (!id.value) {
    error.value = true
    loading.value = false
    return
  }
  load()
})
</script>

<style scoped>
.page { min-height: 100vh; background: #08070a; padding-bottom: 60rpx; box-sizing: border-box; }
.state { padding: 140rpx 0; display: flex; flex-direction: column; align-items: center; gap: 16rpx; color: rgba(247,241,232,.4); font-size: 23rpx; }
.spinner { width: 44rpx; height: 44rpx; border-radius: 50%; border: 4rpx solid rgba(255,255,255,.14); border-top-color: var(--km-accent); animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.retry { margin-top: 10rpx; padding: 14rpx 30rpx; border-radius: 999rpx; background: rgba(245,180,91,.14); color: #f5cf9a; font-size: 22rpx; }

.banner { position: relative; height: 380rpx; overflow: hidden; }
.banner-bg { position: absolute; inset: 0; width: 100%; height: 100%; filter: blur(26rpx) brightness(.5); transform: scale(1.2); }
.banner-shade { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(8,7,10,.35), rgba(8,7,10,.95)); }
.profile { position: absolute; left: 24rpx; right: 24rpx; bottom: 26rpx; display: flex; align-items: center; gap: 18rpx; }
.avatar { width: 132rpx; height: 132rpx; border-radius: 50%; border: 3rpx solid rgba(245,180,91,.5); background: #17151b; flex: none; }
.profile-copy { flex: 1; min-width: 0; }
.stage { font-size: 38rpx; font-weight: 900; color: #fff; letter-spacing: -1rpx; }
.tags { display: flex; gap: 8rpx; margin-top: 8rpx; flex-wrap: wrap; }
.tag { padding: 4rpx 12rpx; border-radius: 6rpx; background: rgba(245,180,91,.18); color: #f5cf9a; font-size: 18rpx; }
.tag.ghost { background: rgba(255,255,255,.1); color: rgba(247,241,232,.7); }
.follow {
  height: 62rpx; line-height: 62rpx; padding: 0 26rpx; border-radius: 999rpx; flex: none;
  background: linear-gradient(135deg,#fbd08a,#de823c); color: #22150a; font-size: 22rpx; font-weight: 800;
}
.follow.on { background: rgba(255,255,255,.08); color: rgba(247,241,232,.6); font-weight: 600; }

.stats { display: grid; grid-template-columns: repeat(4, 1fr); padding: 24rpx 0; margin: 0 24rpx; border-bottom: 1rpx solid rgba(255,255,255,.05); }
.stat { display: flex; flex-direction: column; align-items: center; gap: 4rpx; }
.n { font-size: 30rpx; font-weight: 900; color: #f7f1e8; }
.l { font-size: 19rpx; color: rgba(247,241,232,.34); }

.intro { margin: 22rpx 24rpx 0; padding: 22rpx; border-radius: 18rpx; background: rgba(255,255,255,.035); border: 1rpx solid rgba(255,255,255,.05); }
.intro-label { font-size: 19rpx; letter-spacing: 2rpx; color: var(--km-accent); font-weight: 800; }
.intro-text { display: block; margin-top: 8rpx; font-size: 23rpx; line-height: 1.7; color: rgba(247,241,232,.62); }

.section-head { display: flex; align-items: baseline; gap: 10rpx; padding: 34rpx 24rpx 14rpx; }
.section-title { font-size: 30rpx; font-weight: 900; color: #f7f1e8; }
.section-sub { font-size: 20rpx; color: rgba(247,241,232,.32); }
.empty { padding: 80rpx 0; text-align: center; font-size: 22rpx; color: rgba(247,241,232,.3); }
.works { padding: 0 16rpx; }
.work { display: flex; align-items: center; gap: 16rpx; padding: 14rpx 8rpx; border-radius: 16rpx; }
.work:active { background: rgba(255,255,255,.04); }
.w-cover { width: 96rpx; height: 96rpx; border-radius: 12rpx; flex: none; background: #17151b; }
.w-meta { flex: 1; min-width: 0; }
.w-name { display: block; font-size: 26rpx; color: #f3f4f6; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.w-sub { display: block; margin-top: 4rpx; font-size: 20rpx; color: rgba(247,241,232,.36); }
.w-play { width: 52rpx; height: 52rpx; border-radius: 50%; background: rgba(245,180,91,.16); display: flex; align-items: center; justify-content: center; }
</style>


