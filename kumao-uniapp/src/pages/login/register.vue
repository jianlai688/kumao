<template>
  <view class="page">
    <view class="glow"></view>
    <km-nav-bar title="注册" />

    <view class="brand">
      <text class="logo">🐱</text>
      <text class="title">创建酷猫账号</text>
      <text class="sub">注册即可收藏歌曲、关注歌手、开通会员</text>
    </view>

    <view class="seg">
      <view :class="['seg-item', { on: mode === 'password' }]" @click="switchMode('password')">账号注册</view>
      <view :class="['seg-item', { on: mode === 'phone' }]" @click="switchMode('phone')">手机号注册</view>
    </view>

    <view class="form">
      <template v-if="mode === 'password'">
        <view class="field">
          <text class="f-ico">👤</text>
          <input v-model="form.username" placeholder="用户名（4-32 位）" placeholder-class="ph" class="input" maxlength="32" />
        </view>
      </template>

      <template v-else>
        <view class="field">
          <text class="f-ico">📱</text>
          <input v-model="form.phone" type="number" placeholder="手机号" placeholder-class="ph" class="input" maxlength="11" />
        </view>
        <view class="field">
          <text class="f-ico">🔑</text>
          <input v-model="form.smsCode" type="number" placeholder="6 位验证码" placeholder-class="ph" class="input" maxlength="6" />
          <view :class="['code-btn', { disabled: counting || sending }]" @click="sendCode">{{ counting ? countdown + 's' : '获取验证码' }}</view>
        </view>
        <view class="field">
          <text class="f-ico">👤</text>
          <input v-model="form.username" placeholder="用户名（4-32 位，可留空自动生成）" placeholder-class="ph" class="input" maxlength="32" />
        </view>
      </template>

      <view class="field">
        <text class="f-ico">🔒</text>
        <input v-model="form.password" password placeholder="密码（6-32 位）" placeholder-class="ph" class="input" maxlength="32" />
      </view>
      <view class="field">
        <text class="f-ico">🔒</text>
        <input v-model="form.confirm" password placeholder="确认密码" placeholder-class="ph" class="input" maxlength="32" />
      </view>

      <view class="agree" @click="agreed = !agreed">
        <view :class="['checkbox', { on: agreed }]"><text v-if="agreed" class="tick">✓</text></view>
        <text class="agree-t">我已阅读并同意《用户协议》与《隐私政策》</text>
      </view>

      <button class="btn" :loading="loading" @click="submit">注 册</button>

      <view class="to-login" @click="goLogin">
        <text class="to-t">已有账号？</text>
        <text class="to-l">去登录</text>
      </view>
    </view>

    <view class="foot">
      <text class="foot-t">演示环境·短信验证码固定为 123456</text>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import KmNavBar from '@/components/km-nav-bar.vue'
import { api } from '@/api'

const mode = ref('password')
const loading = ref(false)
const agreed = ref(false)
const sending = ref(false)
const counting = ref(false)
const countdown = ref(0)

const form = reactive({ username: '', password: '', confirm: '', phone: '', smsCode: '' })

let timer = null

function switchMode(m) {
  if (mode.value === m) return
  mode.value = m
  form.username = ''
  form.phone = ''
  form.smsCode = ''
}

async function sendCode() {
  if (counting.value || sending.value) return
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  sending.value = true
  try {
    const res = await api.smsCode(form.phone)
    const code = (res && res.data && res.data.mockCode) || ''
    if (code) form.smsCode = String(code)
    uni.showToast({ title: code ? `验证码：${code}` : '验证码已发送', icon: 'none' })
    counting.value = true
    countdown.value = 60
    if (timer) clearInterval(timer)
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) {
        clearInterval(timer)
        timer = null
        counting.value = false
      }
    }, 1000)
  } catch (e) {
    /* toast handled */
  } finally {
    sending.value = false
  }
}

function validate() {
  if (mode.value === 'phone' && !/^1[3-9]\d{9}$/.test(form.phone)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return false
  }
  if (mode.value === 'phone' && !/^\d{6}$/.test(form.smsCode)) {
    uni.showToast({ title: '请输入 6 位验证码', icon: 'none' })
    return false
  }
  if (mode.value === 'password' && form.username.trim().length < 4) {
    uni.showToast({ title: '用户名至少 4 位', icon: 'none' })
    return false
  }
  if (mode.value === 'phone' && form.username.trim() && form.username.trim().length < 4) {
    uni.showToast({ title: '用户名至少 4 位', icon: 'none' })
    return false
  }
  if (form.password.length < 6) {
    uni.showToast({ title: '密码至少 6 位', icon: 'none' })
    return false
  }
  if (form.password !== form.confirm) {
    uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
    return false
  }
  if (!agreed.value) {
    uni.showToast({ title: '请先同意用户协议与隐私政策', icon: 'none' })
    return false
  }
  return true
}

async function submit() {
  if (loading.value) return
  if (!validate()) return
  loading.value = true
  try {
    const payload =
      mode.value === 'password'
        ? { type: 'PASSWORD', username: form.username.trim(), password: form.password }
        : {
            type: 'PHONE',
            phone: form.phone,
            smsCode: form.smsCode,
            username: form.username.trim() || undefined,
            password: form.password
          }
    await api.register(payload)

    const loginName = form.username.trim() || (mode.value === 'phone' ? `km_${form.phone}` : '')
    const res = await api.login({ type: 'PASSWORD', device: 'APP', username: loginName, password: form.password })
    const data = (res && res.data) || {}
    if (data.token) {
      uni.setStorageSync('km_token', data.token)
      uni.setStorageSync('km_user', {
        userId: data.userId,
        id: data.userId,
        username: data.username,
        nickname: data.nickname,
        avatar: data.avatar,
        roles: data.roles,
        device: 'APP'
      })
      uni.showToast({ title: '注册成功', icon: 'success' })
      setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 600)
    } else {
      uni.showToast({ title: '注册成功，请登录', icon: 'none' })
      setTimeout(() => goLogin(), 800)
    }
  } catch (e) {
    /* toast handled */
  } finally {
    loading.value = false
  }
}

function goLogin() {
  const pages = getCurrentPages()
  if (pages.length > 1) uni.navigateBack()
  else uni.redirectTo({ url: '/pages/login/login' })
}
</script>

<style scoped>
.page {
  min-height: 100vh; background: #0a090d; padding: 0 0 60rpx;
  position: relative; overflow: hidden;
}
.glow {
  pointer-events: none;
  position: absolute; top: -180rpx; left: 50%; transform: translateX(-50%);
  width: 620rpx; height: 620rpx; border-radius: 50%;
  background: radial-gradient(circle, rgba(245,180,91,.26), transparent 68%);
}
.brand { position: relative; display: flex; flex-direction: column; align-items: center; gap: 10rpx; padding: 30rpx 0 26rpx; }
.logo { font-size: 84rpx; }
.title { font-size: 42rpx; font-weight: 900; color: #f7f1e8; letter-spacing: 2rpx; }
.sub { font-size: 22rpx; color: rgba(247,241,232,.45); }

.seg { display: flex; margin: 0 48rpx 22rpx; background: rgba(255,255,255,.05); border-radius: 999rpx; padding: 6rpx; }
.seg-item { flex: 1; text-align: center; padding: 14rpx 0; border-radius: 999rpx; font-size: 23rpx; color: rgba(247,241,232,.5); }
.seg-item.on { background: rgba(245,180,91,.18); color: #f5cf9a; font-weight: 800; }

.form { position: relative; padding: 0 48rpx; display: flex; flex-direction: column; gap: 18rpx; }
.field {
  display: flex; align-items: center; gap: 14rpx; height: 96rpx;
  padding: 0 22rpx; border-radius: 18rpx;
  background: rgba(255,255,255,.05); border: 1rpx solid rgba(255,255,255,.07);
}
.f-ico { font-size: 26rpx; }
.input { flex: 1; height: 100%; font-size: 26rpx; color: #f3f4f6; }
.code-btn {
  flex: none; padding: 10rpx 18rpx; border-radius: 999rpx;
  background: rgba(245,180,91,.16); color: #f5cf9a; font-size: 20rpx; font-weight: 700;
}
.code-btn.disabled { opacity: .5; }

.agree { display: flex; align-items: center; gap: 12rpx; margin-top: 4rpx; }
.checkbox {
  width: 34rpx; height: 34rpx; border-radius: 8rpx; flex: none;
  border: 1rpx solid rgba(255,255,255,.25); display: flex; align-items: center; justify-content: center;
}
.checkbox.on { background: linear-gradient(135deg,#fbd08a,#de823c); border-color: transparent; }
.tick { font-size: 20rpx; color: #22150a; font-weight: 900; }
.agree-t { font-size: 20rpx; color: rgba(247,241,232,.45); }

.btn {
  width: 100%;
  margin-top: 10rpx; height: 96rpx; line-height: 96rpx; border-radius: 999rpx;
  background: linear-gradient(135deg,#fbd08a,#de823c); color: #22150a;
  font-size: 30rpx; font-weight: 900; letter-spacing: 6rpx;
  box-shadow: 0 16rpx 40rpx rgba(224,139,62,.3);
}
.to-login { display: flex; justify-content: center; gap: 6rpx; margin-top: 6rpx; }
.to-t { font-size: 22rpx; color: rgba(247,241,232,.35); }
.to-l { font-size: 22rpx; color: var(--km-accent); }
.foot { margin-top: 40rpx; text-align: center; }
.foot-t { font-size: 19rpx; color: rgba(247,241,232,.22); }
</style>




