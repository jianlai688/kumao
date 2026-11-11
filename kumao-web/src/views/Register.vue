<template>
  <div class="register-page">
    <!-- 左侧品牌区 -->
    <div class="brand-side">
      <div class="brand-glow"></div>
      <router-link to="/" class="brand-logo">
        <span class="logo-mark">酷</span>
        <span class="logo-name">酷猫音乐</span>
      </router-link>

      <div class="brand-copy">
        <h1>加入酷猫<br /><span class="hl">让世界听见你</span></h1>
        <p>一个账号，同步你的收藏、歌单与播放记录</p>
      </div>

      <ul class="brand-points">
        <li><span class="p-ico">🎧</span><div><b>千万级曲库</b><em>无损音质 · 随心畅听</em></div></li>
        <li><span class="p-ico">💚</span><div><b>智能推荐</b><em>越听越懂你的口味</em></div></li>
        <li><span class="p-ico">🎤</span><div><b>创作者计划</b><em>上传作品，开启音乐人之路</em></div></li>
      </ul>

      <div class="brand-discs">
        <div class="d d1"></div>
        <div class="d d2"></div>
        <div class="d d3"></div>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="form-side">
      <div class="register-box">
        <div class="box-head">
          <h2>创建账号</h2>
          <p>几秒钟即可开始你的音乐旅程</p>
        </div>

        <div class="seg">
          <button :class="['seg-btn', { active: type === 'PASSWORD' }]" @click="type = 'PASSWORD'">账号密码注册</button>
          <button :class="['seg-btn', { active: type === 'PHONE' }]" @click="type = 'PHONE'">手机号注册</button>
        </div>

        <div class="form-body">
          <label class="f-label">用户名</label>
          <el-input v-model="form.username" placeholder="4-32 位用户名" size="large" @keyup.enter="submit">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>

          <label class="f-label">密码</label>
          <el-input v-model="form.password" type="password" show-password placeholder="6-32 位密码" size="large" @keyup.enter="submit">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>

          <template v-if="type === 'PHONE'">
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
          </template>
        </div>

        <div class="agree">
          <el-checkbox v-model="agree" size="small" />
          <span class="agree-text" @click="agree = !agree">我已阅读并同意 <a class="link" href="javascript:;">《用户协议》</a> 与 <a class="link" href="javascript:;">《隐私政策》</a></span>
        </div>

        <button class="submit-btn" :disabled="loading" @click="submit">
          <span v-if="!loading">注 册</span>
          <span v-else class="loading-dot">注册中…</span>
        </button>

        <div class="bottom-row">
          <span>已有账号？<router-link to="/login" class="link">去登录</router-link></span>
          <router-link to="/" class="link dim">先逛逛 →</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Iphone } from '@element-plus/icons-vue'
import { authApi } from '@/api'

const router = useRouter()
const type = ref('PASSWORD')
const loading = ref(false)
const countdown = ref(0)
const agree = ref(true)
const form = reactive({ username: '', password: '', phone: '', smsCode: '' })

async function sendSms() {
  if (!form.phone) {
    ElMessage.warning('请先输入手机号')
    return
  }
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

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  if (type.value === 'PHONE' && (!form.phone || !form.smsCode)) {
    ElMessage.warning('请填写手机号和验证码')
    return
  }
  if (!agree.value) {
    ElMessage.warning('请先同意用户协议与隐私政策')
    return
  }
  loading.value = true
  try {
    await authApi.register({ type: type.value, ...form })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    /* handled */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1.02fr;
  background: #050506;
}

/* ---------------- 左侧品牌 ---------------- */
.brand-side {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 34px;
  padding: 54px 60px;
  overflow: hidden;
  background:
    radial-gradient(900px 620px at 20% 15%, rgba(192, 132, 252, .15), transparent 58%),
    radial-gradient(700px 520px at 85% 92%, rgba(96, 190, 255, .1), transparent 60%),
    #08090b;
  border-right: 1px solid rgba(255, 255, 255, .06);
}
.brand-glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 50% 50%, transparent 40%, rgba(0, 0, 0, .35));
  pointer-events: none;
}

.brand-logo { display: flex; align-items: center; gap: 12px; position: relative; z-index: 1; text-decoration: none; }
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
  margin: 0 0 16px;
  font-size: 52px;
  line-height: 1.16;
  font-weight: 900;
  letter-spacing: -2.4px;
}
.hl {
  background: linear-gradient(96deg, #c084fc, #f472b6 65%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.brand-copy p { margin: 0; font-size: 14px; color: rgba(243, 244, 246, .48); letter-spacing: .4px; }

.brand-points {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: relative;
  z-index: 1;
}
.brand-points li { display: flex; align-items: center; gap: 14px; }
.p-ico {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 13px;
  font-size: 18px;
  background: rgba(192, 132, 252, .08);
  border: 1px solid rgba(192, 132, 252, .18);
}
.brand-points b { display: block; font-size: 14px; font-weight: 700; }
.brand-points em { font-style: normal; font-size: 12px; color: var(--km-text-faint); }

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
  background:
    radial-gradient(600px 400px at 85% 5%, rgba(192, 132, 252, .05), transparent 60%),
    #050506;
}
.register-box { width: 100%; max-width: 420px; }

.box-head { margin-bottom: 24px; }
.box-head h2 { margin: 0 0 8px; font-size: 27px; font-weight: 900; letter-spacing: -.8px; }
.box-head p { margin: 0; font-size: 13px; color: var(--km-text-dim); }

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

.agree {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 4px 0 2px;
  font-size: 12px;
  color: var(--km-text-dim);
}
.agree :deep(.el-checkbox) { height: auto; margin-right: 4px; }
.agree :deep(.el-checkbox__label) { display: none; }
.agree-text { cursor: pointer; }

.submit-btn {
  width: 100%;
  height: 50px;
  margin-top: 20px;
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
.link { color: var(--km-accent); font-weight: 600; text-decoration: none; }
.link.dim { color: var(--km-text-dim); font-weight: 400; }
.link.dim:hover { color: var(--km-text); }

@media (max-width: 1024px) {
  .register-page { grid-template-columns: 1fr; }
  .brand-side { display: none; }
  .form-side { padding: 32px 20px; }
}
</style>
