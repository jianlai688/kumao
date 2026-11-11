<template>
  <view class="page">
    <view class="glow"></view>
    <view class="rings"><view class="r r1"></view><view class="r r2"></view><view class="r r3"></view></view>

    <view class="brand">
      <text class="logo">🐱</text>
      <text class="title">酷猫音乐</text>
      <text class="sub">听见<text class="hl">每一种情绪</text></text>
    </view>

    <view class="form">
      <view class="field">
        <text class="f-ico">👤</text>
        <input v-model="form.username" placeholder="用户名" placeholder-class="ph" class="input" />
      </view>
      <view class="field">
        <text class="f-ico">🔒</text>
        <input v-model="form.password" password placeholder="密码" placeholder-class="ph" class="input" />
      </view>

      <button class="btn" :loading="loading" @click="login">登 录</button>
      <button class="btn ghost" @click="oneClick">运营商一键登录</button>
      <view class="register-row" @click="goRegister">
        <text class="rr-t">还没有账号？</text>
        <text class="rr-l">立即注册</text>
      </view>
    </view>

    <view class="demo">
      <text class="demo-label">演示账号 · 点击填充</text>
      <view class="demo-chips">
        <view v-for="a in demos" :key="a.username" class="demo-chip" @click="fill(a)">
          <text class="dc-user">{{ a.username }}</text>
          <text class="dc-pass">{{ a.password }}</text>
        </view>
      </view>
    </view>

    <view class="foot">
      <text class="foot-t">登录即表示同意《用户协议》与《隐私政策》</text>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { api } from '@/api'

const loading = ref(false)
const form = reactive({ username: 'zhangsan', password: '123456' })
const demos = [
  { username: 'zhangsan', password: '123456' },
  { username: 'anchor01', password: '123456' },
  { username: 'musician01', password: '123456' },
  { username: 'admin', password: '123456' }
]

function goRegister() {
  uni.navigateTo({ url: '/pages/login/register' })
}

function fill(a) {
  form.username = a.username
  form.password = a.password
}

async function login() {
  loading.value = true
  try {
    const res = await api.login({ type: 'PASSWORD', device: 'APP', ...form })
    uni.setStorageSync('km_token', res.data.token)
    uni.setStorageSync('km_user', res.data)
    uni.showToast({ title: '登录成功' })
    setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 600)
  } finally {
    loading.value = false
  }
}

async function oneClick() {
  // 演示：调用运营商一键登录接口（APP 专属）
  const res = await api.login({ type: 'ONE_CLICK', device: 'APP', carrierToken: 'demo-carrier-token' })
  uni.setStorageSync('km_token', res.data.token)
  uni.setStorageSync('km_user', res.data)
  uni.showToast({ title: '一键登录成功' })
}
</script>

<style scoped>
.page {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100vh;
  padding: 130rpx 56rpx 60rpx;
  overflow: hidden;
  box-sizing: border-box;
}
.glow {
  position: absolute;
  width: 700rpx;
  height: 700rpx;
  left: -180rpx;
  top: -260rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(245, 180, 91, .2), transparent 62%);
  filter: blur(40rpx);
}
.rings { position: absolute; right: -140rpx; top: 120rpx; }
.r { position: absolute; border-radius: 50%; border: 1rpx solid rgba(245, 180, 91, .12); }
.r1 { width: 520rpx; height: 520rpx; left: -260rpx; top: -260rpx; }
.r2 { width: 360rpx; height: 360rpx; left: -180rpx; top: -180rpx; border-color: rgba(245, 180, 91, .18); }
.r3 { width: 200rpx; height: 200rpx; left: -100rpx; top: -100rpx; border-color: rgba(245, 180, 91, .26); }

.brand { position: relative; display: flex; flex-direction: column; align-items: center; gap: 12rpx; }
.logo { font-size: 110rpx; filter: drop-shadow(0 0 30rpx rgba(245, 180, 91, .42)); }
.title {
  font-size: 50rpx;
  font-weight: 900;
  letter-spacing: -1.6rpx;
  background: linear-gradient(96deg, #f3f4f6, #f5b45b);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.sub { font-size: 26rpx; color: rgba(243, 244, 246, .45); letter-spacing: 1rpx; }
.hl { color: #f5b45b; }

.form { position: relative; width: 100%; margin-top: 80rpx; display: flex; flex-direction: column; gap: 22rpx; }
.field {
  display: flex;
  align-items: center;
  gap: 16rpx;
  height: 96rpx;
  padding: 0 28rpx;
  border-radius: 26rpx;
  background: rgba(255, 255, 255, .05);
  border: 1rpx solid rgba(255, 255, 255, .08);
}
.f-ico { font-size: 28rpx; opacity: .6; }
.input { flex: 1; font-size: 28rpx; color: #f3f4f6; }
.ph { color: rgba(243, 244, 246, .3); }

.btn {
  width: 100%;
  height: 94rpx;
  line-height: 94rpx;
  margin-top: 12rpx;
  border-radius: 26rpx;
  background: #f5b45b;
  color: #07080a;
  font-size: 30rpx;
  font-weight: 800;
  letter-spacing: 6rpx;
  border: none;
  box-shadow: 0 16rpx 44rpx rgba(245, 180, 91, .24);
}
.btn::after { border: none; }
.btn.ghost {
  margin-top: 0;
  background: transparent;
  color: rgba(243, 244, 246, .6);
  border: 1rpx solid rgba(255, 255, 255, .12);
  letter-spacing: 1rpx;
  font-weight: 500;
  box-shadow: none;
}

.demo { position: relative; width: 100%; margin-top: 60rpx; }
.demo-label { font-size: 21rpx; color: rgba(243, 244, 246, .3); letter-spacing: .5rpx; }
.demo-chips { display: flex; flex-wrap: wrap; gap: 14rpx; margin-top: 16rpx; }
.demo-chip {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 12rpx 20rpx;
  border-radius: 14rpx;
  border: 1rpx solid rgba(255, 255, 255, .08);
  background: rgba(255, 255, 255, .025);
}
.dc-user { font-size: 23rpx; color: rgba(243, 244, 246, .8); font-weight: 600; }
.dc-pass { font-size: 20rpx; color: rgba(243, 244, 246, .3); }

.foot { margin-top: auto; padding-top: 40rpx; }
.foot-t { font-size: 20rpx; color: rgba(243, 244, 246, .22); }

.register-row { display: flex; justify-content: center; gap: 6rpx; margin-top: 6rpx; }
.rr-t { font-size: 22rpx; color: rgba(247,241,232,.35); }
.rr-l { font-size: 22rpx; color: #f5b45b; }
</style>


