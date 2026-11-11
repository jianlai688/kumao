<template>
  <view class="page">
    <km-nav-bar title="音乐人入驻" />

    <view v-if="checking" class="state"><view class="spinner"></view><text>正在读取入驻状态…</text></view>

    <template v-else>
      <view v-if="existing" class="status-card">
        <view class="status-top">
          <text class="status-name">{{ existing.stageName }}</text>
          <text :class="['status-badge', String(existing.status).toLowerCase()]">{{ statusText(existing.status) }}</text>
        </view>
        <text class="status-sub">{{ statusHint }}</text>
        <text v-if="existing.rejectReason" class="status-reason">原因：{{ existing.rejectReason }}</text>
        <view v-if="existing.status === 'APPROVED'" class="status-actions">
          <view class="primary" @click="goHomepage">查看我的音乐人主页</view>
        </view>
        <view v-else class="status-actions">
          <view class="ghost" @click="editAgain">修改并重新提交</view>
        </view>
      </view>

      <view v-if="!existing || editing" class="form">
        <view class="field">
          <text class="label">艺名 <text class="req">*</text></text>
          <input v-model="form.stageName" class="input" placeholder="对外展示的名字" placeholder-class="ph" maxlength="20" />
        </view>

        <view class="field">
          <text class="label">真实姓名 <text class="req">*</text></text>
          <input v-model="form.realName" class="input" placeholder="用于实名认证" placeholder-class="ph" maxlength="20" />
        </view>

        <view class="field">
          <text class="label">音乐风格 <text class="req">*</text></text>
          <view class="chips">
            <text
              v-for="s in styles"
              :key="s"
              :class="['chip', { on: form.style === s }]"
              @click="form.style = s"
            >{{ s }}</text>
          </view>
        </view>

        <view class="field">
          <text class="label">代表作</text>
          <input v-model="form.representative" class="input" placeholder="例如《城市夜归人》" placeholder-class="ph" maxlength="30" />
        </view>

        <view class="field">
          <text class="label">个人简介</text>
          <textarea
            v-model="form.intro"
            class="textarea"
            placeholder="介绍一下你的音乐风格与经历（选填）"
            placeholder-class="ph"
            maxlength="200"
          />
          <text class="counter">{{ form.intro.length }}/200</text>
        </view>

        <view class="field">
          <text class="label">头像链接</text>
          <input v-model="form.avatar" class="input" placeholder="https://… （选填）" placeholder-class="ph" />
        </view>

        <view class="tips">
          <text class="tips-title">入驻说明</text>
          <text class="tips-line">· 提交后进入人工审核，通常 1–3 个工作日</text>
          <text class="tips-line">· 审核通过后可发布作品、查看播放数据与收益结算</text>
          <text class="tips-line">· 请确保艺名与作品不侵犯他人权益</text>
        </view>

        <view :class="['submit', { disabled: submitting }]" @click="submit">
          {{ submitting ? '提交中…' : '提交入驻申请' }}
        </view>
      </view>
    </template>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import KmNavBar from '@/components/km-nav-bar.vue'
import { api } from '@/api'

const styles = ['民谣', '流行', '摇滚', '电子', '说唱', '古风', '爵士', '纯音乐']

const checking = ref(true)
const submitting = ref(false)
const editing = ref(false)
const existing = ref(null)

const form = reactive({
  stageName: '',
  realName: '',
  style: '',
  representative: '',
  intro: '',
  avatar: ''
})

const statusHint = ref('')

const statusMap = {
  PENDING: '资料已提交，正在审核中，请耐心等待。',
  APPROVED: '恭喜，你已通过音乐人认证。',
  REJECTED: '很抱歉，本次入驻未通过，可修改资料后重新提交。',
  REVOKED: '入驻资格已被撤销，如有疑问请联系客服。'
}

function statusText(s) {
  return { PENDING: '审核中', APPROVED: '已认证', REJECTED: '未通过', REVOKED: '已撤销' }[s] || '未知'
}

function fillForm(data) {
  if (!data) return
  form.stageName = data.stageName || ''
  form.realName = data.realName || ''
  form.style = data.style || ''
  form.representative = data.representative || ''
  form.intro = data.intro || ''
  form.avatar = data.avatar || ''
}

function editAgain() {
  fillForm(existing.value)
  editing.value = true
}

function goHomepage() {
  if (existing.value && existing.value.id) {
    uni.navigateTo({ url: `/pages/musician/detail?id=${existing.value.id}` })
  }
}

async function submit() {
  if (submitting.value) return
  if (!uni.getStorageSync('km_token')) {
    uni.navigateTo({ url: '/pages/login/login' })
    return
  }
  if (!form.stageName.trim()) {
    uni.showToast({ title: '请填写艺名', icon: 'none' })
    return
  }
  if (!form.realName.trim()) {
    uni.showToast({ title: '请填写真实姓名', icon: 'none' })
    return
  }
  if (!form.style) {
    uni.showToast({ title: '请选择音乐风格', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const res = await api.musicianApply({
      stageName: form.stageName.trim(),
      realName: form.realName.trim(),
      style: form.style,
      representative: form.representative.trim(),
      intro: form.intro.trim(),
      avatar: form.avatar.trim()
    })
    uni.showModal({
      title: '提交成功',
      content: '你的入驻申请已提交，审核结果会在这里展示。',
      showCancel: false,
      success: () => {
        editing.value = false
        load()
      }
    })
    if (res && res.data) {
      existing.value = { ...(existing.value || {}), id: res.data, stageName: form.stageName.trim(), status: 'PENDING' }
      statusHint.value = statusMap.PENDING
    }
  } catch (e) {
    /* toast handled by request */
  } finally {
    submitting.value = false
  }
}

async function load() {
  checking.value = true
  if (!uni.getStorageSync('km_token')) {
    checking.value = false
    return
  }
  const res = await api.musicianMine().catch(() => null)
  const data = res && res.data
  if (data) {
    existing.value = data
    statusHint.value = statusMap[data.status] || ''
  } else {
    existing.value = null
  }
  checking.value = false
}

onLoad(load)
</script>

<style scoped>
.page { min-height: 100vh; background: #08070a; padding-bottom: 70rpx; box-sizing: border-box; }
.state { padding: 140rpx 0; display: flex; flex-direction: column; align-items: center; gap: 16rpx; color: rgba(247,241,232,.4); font-size: 23rpx; }
.spinner { width: 44rpx; height: 44rpx; border-radius: 50%; border: 4rpx solid rgba(255,255,255,.14); border-top-color: #f5b45b; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.status-card { margin: 16rpx 24rpx 0; padding: 26rpx; border-radius: 20rpx; background: rgba(245,180,91,.07); border: 1rpx solid rgba(245,180,91,.22); }
.status-top { display: flex; align-items: center; gap: 14rpx; }
.status-name { font-size: 32rpx; font-weight: 900; color: #f7f1e8; }
.status-badge { padding: 4rpx 14rpx; border-radius: 999rpx; font-size: 18rpx; font-weight: 800; background: rgba(255,255,255,.1); color: rgba(247,241,232,.7); }
.status-badge.pending { background: rgba(245,180,91,.2); color: #f5cf9a; }
.status-badge.approved { background: rgba(74,222,128,.16); color: #86efac; }
.status-badge.rejected { background: rgba(251,113,133,.16); color: #ff9fb0; }
.status-sub { display: block; margin-top: 12rpx; font-size: 22rpx; color: rgba(247,241,232,.55); line-height: 1.6; }
.status-reason { display: block; margin-top: 8rpx; font-size: 21rpx; color: #ff9fb0; }
.status-actions { margin-top: 20rpx; }
.status-actions .primary, .status-actions .ghost { height: 70rpx; line-height: 70rpx; text-align: center; border-radius: 14rpx; font-size: 23rpx; font-weight: 800; }
.status-actions .primary { background: linear-gradient(135deg,#fbd08a,#de823c); color: #22150a; }
.status-actions .ghost { background: rgba(255,255,255,.06); color: rgba(247,241,232,.7); }

.form { padding: 10rpx 24rpx 0; }
.field { margin-bottom: 24rpx; }
.label { display: block; font-size: 22rpx; color: rgba(247,241,232,.6); margin-bottom: 10rpx; }
.req { color: #fb7185; }
.input, .textarea {
  width: 100%; box-sizing: border-box; padding: 0 22rpx; height: 84rpx;
  border-radius: 16rpx; background: rgba(255,255,255,.05); border: 1rpx solid rgba(255,255,255,.07);
  color: #f3f4f6; font-size: 24rpx;
}
.textarea { height: 190rpx; padding: 18rpx 22rpx; line-height: 1.6; }
.ph { color: rgba(243,244,246,.26); }
.counter { display: block; text-align: right; margin-top: 6rpx; font-size: 18rpx; color: rgba(247,241,232,.24); }
.chips { display: flex; flex-wrap: wrap; gap: 10rpx; }
.chip { padding: 12rpx 24rpx; border-radius: 999rpx; background: rgba(255,255,255,.05); color: rgba(247,241,232,.55); font-size: 22rpx; }
.chip.on { background: rgba(245,180,91,.18); color: #f5cf9a; font-weight: 800; }

.tips { margin: 10rpx 0 26rpx; padding: 20rpx; border-radius: 16rpx; background: rgba(255,255,255,.03); }
.tips-title { display: block; font-size: 20rpx; color: #f5b45b; font-weight: 800; letter-spacing: 1rpx; margin-bottom: 8rpx; }
.tips-line { display: block; font-size: 20rpx; color: rgba(247,241,232,.4); line-height: 1.8; }

.submit {
  height: 92rpx; line-height: 92rpx; text-align: center; border-radius: 18rpx;
  background: linear-gradient(135deg,#fbd08a,#de823c); color: #22150a; font-size: 27rpx; font-weight: 900;
}
.submit.disabled { opacity: .6; }
</style>

