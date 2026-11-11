<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="brand-side">
      <div class="brand-glow"></div>
      <div class="brand-logo">
        <span class="logo-mark">酷</span>
        <span class="logo-name">酷猫音乐</span>
      </div>
      <div class="brand-copy">
        <h1>听见<br /><span class="hl">每一种情绪</span></h1>
        <p>千万曲库 · 沉浸聆听 · 与你同频</p>
      </div>
      <div class="brand-stats">
        <div class="stat"><b>1000万+</b><span>正版曲库</span></div>
        <div class="stat"><b>50万+</b><span>精选歌单</span></div>
        <div class="stat"><b>24h</b><span>音乐直播</span></div>
      </div>
      <div class="brand-discs">
        <div class="d d1"></div>
        <div class="d d2"></div>
        <div class="d d3"></div>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="form-side">
      <div class="login-box">
        <div class="box-head">
          <h2>欢迎回来</h2>
          <p>登录酷猫音乐，继续你的音乐旅程</p>
        </div>

        <!-- 登录方式切换 -->
        <div class="seg">
          <button :class="['seg-btn', { active: tab === 'password' }]" @click="tab = 'password'">密码登录</button>
          <button :class="['seg-btn', { active: tab === 'sms' }]" @click="tab = 'sms'">验证码登录</button>
          <button :class="['seg-btn', { active: tab === 'oauth' }]" @click="tab = 'oauth'">第三方</button>
        </div>

        <div v-if="tab === 'password'" class="form-body">
          <label class="f-label">用户名</label>
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" @keyup.enter="submit">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
          <label class="f-label">密码</label>
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" size="large" @keyup.enter="submit">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </div>

        <div v-if="tab === 'sms'" class="form-body">
          <label class="f-label">手机号</label>
          <el-input v-model="form.phone" placeholder="请输入手机号" size="large" @keyup.enter="submit">
            <template #prefix><el-icon><Iphone /></el-icon></template>
          </el-input>
          <label class="f-label">验证码</label>
          <div class="sms-row">
            <el-input v-model="form.smsCode" placeholder="6 位验证码" size="large" @keyup.enter="submit" />
            <button class="sms-btn" :disabled="countdown > 0" @click="sendSms">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </button>
          </div>
        </div>

        <div v-if="tab === 'oauth'" class="oauth-body">
          <button class="oauth-item wechat" @click="oauth('WECHAT')">
            <span class="oauth-ico">💬</span>
            <span>微信登录</span>
            <el-icon class="oauth-arrow"><ArrowRight /></el-icon>
          </button>
          <button class="oauth-item qq" @click="oauth('QQ')">
            <span class="oauth-ico">🐧</span>
            <span>QQ 登录</span>
            <el-icon class="oauth-arrow"><ArrowRight /></el-icon>
          </button>
          <p class="oauth-tip">演示环境将自动创建或登录对应的第三方账号</p>
        </div>

        <div class="device-row">
          <span class="d-label">登录设备</span>
          <div class="d-options">
            <button
              v-for="d in devices"
              :key="d.value"
              :class="['d-option', { active: device === d.value }]"
              @click="device = d.value"
            >{{ d.label }}</button>
          </div>
        </div>

        <button class="submit-btn" :disabled="loading" @click="submit">
          <span v-if="!loading">登 录</span>
          <span v-else class="loading-dot">登录中…</span>
        </button>

        <div class="bottom-row">
          <span>还没有账号？<router-link to="/register" class="link">立即注册</router-link></span>
          <router-link to="/" class="link dim">先逛逛 →</router-link>
        </div>

        <div class="demo-box">
          <div class="demo-head">
            <span>演示账号</span>
            <span class="demo-tip">点击自动填充</span>
          </div>
          <div class="demo-chips">
            <button v-for="a in demos" :key="a.username" class="demo-chip" @click="fill(a)">
              <b>{{ a.username }}</b>
              <span>{{ a.password }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const tab = ref('password')
const device = ref('PC')
const loading = ref(false)
const countdown = ref(0)

const devices = [
  { value: 'PC', label: 'PC Web' },
  { value: 'APP', label: 'APP' },
  { value: 'DESKTOP', label: '桌面端' }
]

const form = reactive({ username: '', password: '', phone: '', smsCode: '' })

const demos = [
  { username: 'admin', password: 'admin123' },
  { username: 'zhangsan', password: '123456' },
  { username: 'anchor01', password: '123456' },
  { username: 'musician01', password: '123456' },
  { username: 'adman01', password: '123456' },
  { username: 'vipuser', password: '123456' }
]

function fill(a) {
  tab.value = 'password'
  form.username = a.username
  form.password = a.password
  ElMessage.success(`已填充账号 ${a.username}`)
}

async function sendSms() {
  if (!form.phone) return ElMessage.warning('请先输入手机号')
  const res = await authApi.smsCode(form.phone)
  if (res.data?.mockCode) {
    form.smsCode = res.data.mockCode
    ElMessage.success(`验证码已发送（演示：${res.data.mockCode}）`)
  } else {
    ElMessage.success('验证码已发送')
  }
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
}

async function oauth(thirdType) {
  loading.value = true
  try {
    const res = await authApi.login({
      type: 'OAUTH',
      device: device.value,
      thirdType,
      thirdOpenId: 'demo-' + thirdType.toLowerCase()
    })
    userStore.setSession(res.data)
    ElMessage.success('登录成功')
    router.replace(route.query.redirect || '/')
  } catch (e) {
    /* handled */
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (tab.value === 'password' && (!form.username || !form.password)) return ElMessage.warning('请输入用户名和密码')
  if (tab.value === 'sms' && (!form.phone || !form.smsCode)) return ElMessage.warning('请输入手机号和验证码')
  if (tab.value === 'oauth') return oauth('WECHAT')

  loading.value = true
  try {
    const payload = {
      type: tab.value === 'sms' ? 'SMS' : 'PASSWORD',
      device: device.value,
      username: form.username,
      password: form.password,
      phone: form.phone,
      smsCode: form.smsCode
    }
    const data = await userStore.login(payload)
    ElMessage.success('登录成功，欢迎回来 ' + (data.nickname || data.username))
    const admin = (data.roles || '').split(',').includes('ADMIN')
    router.replace(route.query.redirect || (admin && device.value === 'PC' && form.username === 'admin' ? '/admin' : '/'))
  } catch (e) {
    /* handled */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  background: #050506;
}

/* ---------------- 左侧品牌 ---------------- */
.brand-side {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 54px 60px;
  overflow: hidden;
  background:
    radial-gradient(900px 600px at 15% 20%, rgba(192, 132, 252, .16), transparent 58%),
    radial-gradient(700px 500px at 80% 90%, rgba(96, 190, 255, .1), transparent 60%),
    #08090b;
  border-right: 1px solid rgba(255, 255, 255, .06);
}
.brand-glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 50% 50%, transparent 40%, rgba(0, 0, 0, .35));
  pointer-events: none;
}

.brand-logo { display: flex; align-items: center; gap: 12px; position: relative; z-index: 1; }
.logo-mark { font-size: 32px; filter: drop-shadow(0 0 20px rgba(192, 132, 252, .5)); }
.logo-name {
  font-size: 22px;
  font-weight: 900;
  letter-spacing: -.6px;
  background: linear-gradient(96deg, #f3f4f6, #c084fc);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.brand-copy { position: relative; z-index: 1; }
.brand-copy h1 {
  margin: 0 0 18px;
  font-size: 56px;
  line-height: 1.14;
  font-weight: 900;
  letter-spacing: -2.5px;
}
.hl {
  background: linear-gradient(96deg, #c084fc, #f472b6 65%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.brand-copy p { margin: 0; font-size: 15px; color: rgba(243, 244, 246, .48); letter-spacing: 1px; }

.brand-stats { display: flex; gap: 44px; position: relative; z-index: 1; }
.stat { display: flex; flex-direction: column; gap: 6px; }
.stat b { font-size: 24px; font-weight: 900; letter-spacing: -.5px; color: var(--km-accent); }
.stat span { font-size: 12px; color: rgba(243, 244, 246, .42); }

.brand-discs { position: absolute; right: -80px; top: 50%; transform: translateY(-50%); pointer-events: none; }
.d { position: absolute; border-radius: 50%; border: 1px solid rgba(192, 132, 252, .14); }
.d1 { width: 460px; height: 460px; top: -230px; left: -230px; }
.d2 { width: 320px; height: 320px; top: -160px; left: -160px; border-color: rgba(192, 132, 252, .22); }
.d3 { width: 190px; height: 190px; top: -95px; left: -95px; border-color: rgba(192, 132, 252, .32); }

/* ---------------- 右侧表单 ---------------- */
.form-side {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  background: #050506;
}

.login-box { width: 100%; max-width: 420px; }

.box-head { margin-bottom: 26px; }
.box-head h2 { margin: 0 0 8px; font-size: 27px; font-weight: 900; letter-spacing: -.8px; }
.box-head p { margin: 0; font-size: 13px; color: var(--km-text-dim); }

/* 分段控件 */
.seg {
  display: flex;
  gap: 4px;
  padding: 4px;
  border-radius: 14px;
  background: rgba(255, 255, 255, .045);
  border: 1px solid var(--km-border);
  margin-bottom: 24px;
}
.seg-btn {
  flex: 1;
  height: 36px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--km-text-dim);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all .2s;
}
.seg-btn:hover { color: var(--km-text); }
.seg-btn.active {
  background: var(--km-accent);
  color: #07080a;
  font-weight: 800;
  box-shadow: 0 6px 18px rgba(192, 132, 252, .22);
}

.form-body { display: flex; flex-direction: column; }
.f-label { font-size: 12px; color: var(--km-text-dim); margin: 0 0 8px; font-weight: 600; }
.form-body :deep(.el-input__wrapper) { height: 48px; margin-bottom: 18px; border-radius: 13px !important; }

.sms-row { display: flex; gap: 10px; }
.sms-row :deep(.el-input) { flex: 1; }
.sms-btn {
  height: 48px;
  padding: 0 18px;
  border-radius: 13px;
  border: 1px solid var(--km-accent-line);
  background: rgba(192, 132, 252, .08);
  color: var(--km-accent);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  transition: all .2s;
  margin-bottom: 18px;
}
.sms-btn:hover:not(:disabled) { background: var(--km-accent); color: #07080a; }
.sms-btn:disabled { opacity: .45; cursor: not-allowed; }

/* OAuth */
.oauth-body { display: flex; flex-direction: column; gap: 10px; }
.oauth-item {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 52px;
  padding: 0 18px;
  border-radius: 14px;
  border: 1px solid var(--km-border);
  background: rgba(255, 255, 255, .035);
  color: var(--km-text);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all .2s;
}
.oauth-item:hover { border-color: var(--km-border-strong); background: rgba(255, 255, 255, .07); transform: translateX(2px); }
.oauth-ico { font-size: 19px; }
.oauth-arrow { margin-left: auto; color: var(--km-text-faint); }
.oauth-tip { text-align: center; color: var(--km-text-faint); font-size: 12px; margin: 10px 0 0; }

/* 设备 */
.device-row { display: flex; align-items: center; gap: 14px; margin: 20px 0 6px; }
.d-label { font-size: 12px; color: var(--km-text-dim); }
.d-options { display: flex; gap: 6px; }
.d-option {
  height: 30px;
  padding: 0 13px;
  border-radius: 999px;
  border: 1px solid var(--km-border);
  background: transparent;
  color: var(--km-text-dim);
  font-size: 12px;
  cursor: pointer;
  transition: all .18s;
}
.d-option:hover { color: var(--km-text); border-color: var(--km-border-strong); }
.d-option.active { background: rgba(192, 132, 252, .14); border-color: var(--km-accent-line); color: var(--km-accent); font-weight: 700; }

/* 提交 */
.submit-btn {
  width: 100%;
  height: 50px;
  margin-top: 22px;
  border: none;
  border-radius: 14px;
  background: var(--km-accent);
  color: #07080a;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 4px;
  cursor: pointer;
  transition: all .2s;
  box-shadow: 0 12px 34px rgba(192, 132, 252, .22);
}
.submit-btn:hover:not(:disabled) { background: #d8b4fe; transform: translateY(-1px); box-shadow: 0 16px 40px rgba(192, 132, 252, .32); }
.submit-btn:disabled { opacity: .6; cursor: wait; }
.loading-dot { letter-spacing: 1px; }

.bottom-row {
  display: flex;
  justify-content: space-between;
  margin-top: 18px;
  font-size: 13px;
  color: var(--km-text-dim);
}
.link { color: var(--km-accent); font-weight: 600; }
.link.dim { color: var(--km-text-dim); font-weight: 400; }
.link.dim:hover { color: var(--km-text); }

/* 演示账号 */
.demo-box {
  margin-top: 26px;
  padding-top: 20px;
  border-top: 1px dashed rgba(255, 255, 255, .1);
}
.demo-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.demo-head span:first-child { font-size: 12px; font-weight: 600; color: var(--km-text-dim); }
.demo-tip { font-size: 11px; color: var(--km-text-faint); }
.demo-chips { display: grid; grid-template-columns: repeat(2, 1fr); gap: 7px; }
.demo-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 8px 11px;
  border-radius: 10px;
  border: 1px solid var(--km-border);
  background: rgba(255, 255, 255, .025);
  cursor: pointer;
  transition: all .18s;
}
.demo-chip:hover { border-color: rgba(192, 132, 252, .4); background: rgba(192, 132, 252, .06); }
.demo-chip b { font-size: 12px; color: var(--km-text); font-weight: 600; }
.demo-chip span { font-size: 11px; color: var(--km-text-faint); }

@media (max-width: 1024px) {
  .login-page { grid-template-columns: 1fr; }
  .brand-side { display: none; }
  .form-side { padding: 32px 20px; }
}
</style>
