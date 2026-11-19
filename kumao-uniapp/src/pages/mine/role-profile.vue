<template>
  <view class="km-screen role">
    <km-nav-bar :title="cfg.title">
      <view class="ibtn" @click="toggleEdit">
        <text class="ibtn-t">{{ editing ? '取消' : '编辑' }}</text>
      </view>
    </km-nav-bar>

    <view v-if="loading" class="empty">正在加载…</view>

    <view v-else-if="!record" class="empty">
      <text>{{ cfg.emptyText }}</text>
      <view class="go-apply" @click="goApply">{{ cfg.applyText }}</view>
    </view>

    <template v-else>
      <!-- 头部 -->
      <view class="hero">
        <image class="hero-bg" :src="coverOf(record.coverUrl || record.avatar)" mode="aspectFill" />
        <view class="hero-mask"></view>
        <view class="hero-body">
          <image class="avatar" :src="avatarOf(record.avatar || user.avatar)" mode="aspectFill" />
          <view class="hero-copy">
            <text class="name">{{ displayName }}</text>
            <text class="sub">{{ cfg.roleLabel }} · {{ record.status === 'APPROVED' ? '已认证' : record.status === 'PENDING' ? '审核中' : '未通过' }}</text>
          </view>
          <text :class="['pill', record.status === 'APPROVED' ? 'ok' : record.status === 'PENDING' ? 'wait' : 'bad']">
            {{ record.status === 'APPROVED' ? '已认证' : record.status === 'PENDING' ? '审核中' : '未通过' }}
          </text>
        </view>
      </view>

      <!-- 数据 -->
      <view v-if="stats.length" class="stats">
        <view v-for="s in stats" :key="s.label" class="stat">
          <text class="sv">{{ s.value }}</text>
          <text class="sl">{{ s.label }}</text>
        </view>
      </view>

      <!-- 资料 -->
      <view class="card">
        <view v-for="f in cfg.fields" :key="f.key" class="row">
          <text class="k">{{ f.label }}</text>
          <input
            v-if="editing && f.editable"
            v-model="form[f.key]"
            class="input"
            :placeholder="f.placeholder || ''"
            :maxlength="f.max || 60"
          />
          <text v-else class="v">{{ record[f.key] || f.empty || '未设置' }}</text>
        </view>
        <view v-if="cfg.introKey" class="row col">
          <text class="k">{{ cfg.introLabel }}</text>
          <textarea
            v-if="editing"
            v-model="form[cfg.introKey]"
            class="textarea"
            maxlength="200"
            :placeholder="cfg.introPlaceholder || ''"
          />
          <text v-else class="intro">{{ record[cfg.introKey] || '还没有填写介绍' }}</text>
        </view>
      </view>

      <view v-if="editing" class="save-bar">
        <view class="save" @click="save">{{ saving ? '保存中…' : '保存资料' }}</view>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import KmNavBar from '@/components/km-nav-bar.vue'
import { api } from '@/api'
import { avatarOf, coverOf, formatPlay } from '@/utils/media'

const type = ref('musician')
const loading = ref(true)
const editing = ref(false)
const saving = ref(false)
const record = ref(null)
const user = ref(uni.getStorageSync('km_user') || {})
const form = reactive({})

const CONFIG = {
  anchor: {
    title: '主播个人信息',
    roleLabel: '主播',
    emptyText: '你还没有通过主播认证',
    applyText: '去申请主播认证',
    applyUrl: '/pages/mine/mine',
    displayKey: 'anchorName',
    introKey: 'intro',
    introLabel: '主播简介',
    introPlaceholder: '介绍一下你的直播内容、开播时间',
    fields: [
      { key: 'anchorName', label: '主播艺名', editable: true, max: 20, placeholder: '如 甜心主播' },
      { key: 'liveCategory', label: '直播分类', editable: true, max: 20, placeholder: '如 弹唱 / 电台 / 翻唱' },
      { key: 'coverUrl', label: '直播间封面', editable: true, max: 200, placeholder: '封面图片地址' },
      { key: 'realName', label: '实名认证', editable: false },
      { key: 'status', label: '认证状态', editable: false }
    ]
  },
  musician: {
    title: '音乐人个人信息',
    roleLabel: '音乐人',
    emptyText: '你还没有通过音乐人认证',
    applyText: '去申请音乐人认证',
    applyUrl: '/pages/musician/apply',
    displayKey: 'stageName',
    introKey: 'intro',
    introLabel: '艺人简介',
    introPlaceholder: '介绍你的音乐风格、创作经历',
    fields: [
      { key: 'stageName', label: '艺名', editable: true, max: 20, placeholder: '如 木子' },
      { key: 'style', label: '音乐风格', editable: true, max: 20, placeholder: '如 民谣 / 流行' },
      { key: 'representative', label: '代表作', editable: true, max: 40, placeholder: '如 城市夜归人' },
      { key: 'avatar', label: '头像', editable: true, max: 200, placeholder: '头像图片地址' },
      { key: 'realName', label: '实名认证', editable: false },
      { key: 'status', label: '认证状态', editable: false }
    ]
  },
  advertiser: {
    title: '广告商个人信息',
    roleLabel: '广告主',
    emptyText: '你还没有通过广告主认证',
    applyText: '去申请广告主认证',
    applyUrl: '/pages/mine/mine',
    displayKey: 'companyName',
    introKey: 'intro',
    introLabel: '主体介绍',
    introPlaceholder: '介绍你的公司或投放需求',
    fields: [
      { key: 'companyName', label: '主体名称', editable: true, max: 40, placeholder: '如 酷猫广告工作室' },
      { key: 'adType', label: '广告类型', editable: true, max: 20, placeholder: '如 视频广告' },
      { key: 'representative', label: '联系人', editable: true, max: 20, placeholder: '如 张三' },
      { key: 'realName', label: '实名认证', editable: false },
      { key: 'avatar', label: '头像', editable: true, max: 200, placeholder: '头像图片地址' },
      { key: 'status', label: '认证状态', editable: false }
    ]
  }
}

const cfg = computed(() => CONFIG[type.value] || CONFIG.musician)
const displayName = computed(() => {
  const r = record.value || {}
  return r[cfg.value.displayKey] || user.value.nickname || '未命名'
})

const stats = computed(() => {
  const r = record.value || {}
  if (type.value === 'musician') {
    return [
      { label: '粉丝', value: formatPlay(r.fansCount || 0) },
      { label: '作品播放', value: formatPlay(r.totalPlayCount || 0) },
      { label: '获赞', value: formatPlay(r.totalLikeCount || 0) }
    ]
  }
  if (type.value === 'advertiser') {
    return []
  }
  return []
})

function toggleEdit() {
  if (!record.value) return
  if (editing.value) {
    editing.value = false
    fill(record.value)
  } else {
    fill(record.value)
    editing.value = true
  }
}

function fill(r) {
  Object.keys(CONFIG[type.value].fields.reduce((o, f) => ({ ...o, [f.key]: 1 }), {})).forEach((k) => {
    form[k] = r[k] || ''
  })
  const ik = cfg.value.introKey
  if (ik) form[ik] = r[ik] || ''
}

async function load() {
  loading.value = true
  record.value = null
  try {
    if (type.value === 'anchor') {
      const res = await api.anchorMy()
      const list = (res && res.data) || []
      record.value = list.find((x) => x.status === 'APPROVED') || list[0] || null
    } else if (type.value === 'musician') {
      const res = await api.musicianMine()
      record.value = (res && res.data) || null
    } else {
      const res = await api.advertiserMine()
      record.value = (res && res.data) || null
    }
    if (record.value) fill(record.value)
  } catch (e) {
    record.value = null
  }
  loading.value = false
}

async function save() {
  if (saving.value) return
  if (!record.value) return
  saving.value = true
  try {
    if (type.value === 'anchor') {
      if (!form.anchorName) {
        uni.showToast({ title: '主播艺名不能为空', icon: 'none' })
        return
      }
      await api.updateAnchorProfile({
        anchorName: form.anchorName,
        liveCategory: form.liveCategory,
        coverUrl: form.coverUrl,
        intro: form.intro
      })
    } else if (type.value === 'musician') {
      if (!form.stageName) {
        uni.showToast({ title: '艺名不能为空', icon: 'none' })
        return
      }
      await api.updateMusicianProfile({
        stageName: form.stageName,
        style: form.style,
        representative: form.representative,
        avatar: form.avatar,
        intro: form.intro
      })
    } else {
      if (!form.companyName) {
        uni.showToast({ title: '主体名称不能为空', icon: 'none' })
        return
      }
      await api.updateAdvertiserProfile({
        companyName: form.companyName,
        adType: form.adType,
        representative: form.representative,
        avatar: form.avatar,
        intro: form.intro
      })
    }
    editing.value = false
    uni.showToast({ title: '已保存', icon: 'success' })
    await load()
  } catch (e) {
    /* 统一提示 */
  } finally {
    saving.value = false
  }
}

function goApply() {
  const pages = getCurrentPages()
  if (pages.length > 1) uni.navigateBack()
  else uni.switchTab({ url: '/pages/mine/mine' })
}

onLoad((options) => {
  type.value = (options && options.type) || 'musician'
  load()
})
</script>

<style scoped>
.role { padding-bottom: calc(var(--window-bottom, 0px) + 160rpx); }
.ibtn { padding: 0 22rpx; height: 56rpx; border-radius: 999rpx; background: rgba(255,255,255,.06); display: flex; align-items: center; }
.ibtn-t { font-size: 22rpx; color: var(--km-accent); font-weight: 700; }

.hero { position: relative; height: 300rpx; margin: 0 24rpx 18rpx; border-radius: 22rpx; overflow: hidden; background: #17151b; }
.hero-bg { position: absolute; inset: 0; width: 100%; height: 100%; }
.hero-mask { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0,0,0,.15), rgba(0,0,0,.78)); }
.hero-body { position: absolute; left: 24rpx; right: 24rpx; bottom: 22rpx; display: flex; align-items: center; gap: 16rpx; }
.avatar { width: 104rpx; height: 104rpx; border-radius: 50%; border: 2rpx solid rgba(255,255,255,.25); background: #1b1a21; flex: none; }
.hero-copy { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6rpx; }
.name { font-size: 32rpx; font-weight: 800; color: #fff; }
.sub { font-size: 20rpx; color: rgba(255,255,255,.6); }
.pill { font-size: 18rpx; padding: 4rpx 12rpx; border-radius: 999rpx; flex: none; }
.pill.ok { background: rgba(74,222,128,.2); color: #86efac; }
.pill.wait { background: rgba(245,180,91,.2); color: #f5cf9a; }
.pill.bad { background: rgba(229,72,77,.2); color: #ff9a9d; }

.stats { display: flex; margin: 0 24rpx 18rpx; padding: 20rpx 0; border-radius: 18rpx; background: rgba(255,255,255,.035); }
.stat { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4rpx; }
.sv { font-size: 30rpx; font-weight: 800; color: #f7f1e8; }
.sl { font-size: 19rpx; color: rgba(247,241,232,.4); }

.card { margin: 0 24rpx 18rpx; padding: 6rpx 20rpx; border-radius: 18rpx; background: rgba(255,255,255,.035); border: 1rpx solid rgba(255,255,255,.05); }
.row { display: flex; align-items: center; gap: 16rpx; min-height: 92rpx; border-bottom: 1rpx solid rgba(255,255,255,.05); }
.row:last-child { border-bottom: 0; }
.row.col { flex-direction: column; align-items: stretch; justify-content: center; padding: 18rpx 0; gap: 10rpx; }
.k { width: 170rpx; flex: none; font-size: 23rpx; color: rgba(247,241,232,.45); }
.row.col .k { width: auto; }
.v { flex: 1; font-size: 24rpx; color: #f3f4f6; text-align: right; }
.input { flex: 1; text-align: right; font-size: 24rpx; color: #f3f4f6; }
.textarea { width: 100%; min-height: 120rpx; background: rgba(255,255,255,.05); border-radius: 12rpx; padding: 14rpx 16rpx; box-sizing: border-box; font-size: 23rpx; color: #f3f4f6; }
.intro { font-size: 23rpx; color: rgba(247,241,232,.72); line-height: 1.6; }
.empty { padding: 120rpx 40rpx; display: flex; flex-direction: column; align-items: center; gap: 20rpx; font-size: 23rpx; color: rgba(247,241,232,.4); }
.go-apply { padding: 16rpx 32rpx; border-radius: 999rpx; background: linear-gradient(135deg,#fbd08a,#de823c); color: #22150a; font-size: 23rpx; font-weight: 800; }
.save-bar { position: fixed; left: 0; right: 0; bottom: 0; padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom)); background: rgba(10,9,13,.9); }
.save { height: 88rpx; line-height: 88rpx; text-align: center; border-radius: 16rpx; background: linear-gradient(135deg,#fbd08a,#de823c); color: #22150a; font-weight: 800; font-size: 25rpx; }
</style>

