<template>
  <div class="km-page apply-page">
    <div class="apply-hero">
      <div class="hero-glow"></div>
      <div class="hero-left">
        <span class="hero-badge">广告制作人</span>
        <h1 class="hero-title">让你的创意触达千万听众</h1>
        <p class="hero-sub">认证后可创建广告投放、管理素材、查看曝光与转化数据</p>
        <div class="hero-steps">
          <div class="step"><span class="s-num">1</span><span>提交资质</span></div>
          <i class="s-line"></i>
          <div class="step"><span class="s-num">2</span><span>平台审核</span></div>
          <i class="s-line"></i>
          <div class="step"><span class="s-num">3</span><span>投放广告</span></div>
        </div>
      </div>
      <div class="hero-emoji" aria-hidden="true"><el-icon><Promotion /></el-icon></div>
    </div>

    <div class="layout">
      <div class="panel form-card">
        <div class="panel-head">
          <h2>认证资料</h2>
          <span v-if="mine" :class="['pill', statusClass(mine.status)]">{{ statusText(mine.status) }}</span>
          <span v-else class="km-faint">首次提交</span>
        </div>

        <div v-if="mine?.rejectReason" class="reject-box">
          <b>拒绝原因</b>
          <span>{{ mine.rejectReason }}</span>
        </div>

        <div class="f-grid">
          <div class="f-item">
            <label class="f-label">艺名 / 主体名称 <em>*</em></label>
            <el-input v-model="form.companyName" placeholder="公司或个人主体名称" size="large" />
          </div>
          <div class="f-item">
            <label class="f-label">真实姓名 <em>*</em></label>
            <el-input v-model="form.realName" placeholder="请输入真实姓名" size="large" />
          </div>
        </div>

        <div class="f-item">
          <label class="f-label">身份证号 <em>*</em></label>
          <el-input v-model="form.idCard" placeholder="请输入身份证号" size="large" />
        </div>

        <div class="f-grid">
          <div class="f-item">
            <label class="f-label">身份证人像面 <em>*</em></label>
            <MediaDropzone
              v-model="form.idCardFrontUrl"
              kind="cover"
              biz-type="id-card"
              title="上传身份证人像面"
              hint="请上传清晰、完整的身份证人像面，单张不超过 10MB"
              done-title="身份证人像面已上传"
              ready-text="身份证人像面已就绪"
              :max-size-mb="10"
            />
          </div>
          <div class="f-item">
            <label class="f-label">身份证国徽面 <em>*</em></label>
            <MediaDropzone
              v-model="form.idCardBackUrl"
              kind="cover"
              biz-type="id-card"
              title="上传身份证国徽面"
              hint="请上传清晰、完整的身份证国徽面，单张不超过 10MB"
              done-title="身份证国徽面已上传"
              ready-text="身份证国徽面已就绪"
              :max-size-mb="10"
            />
          </div>
        </div>

        <div class="f-item">
          <label class="f-label">广告类型 <em>*</em></label>
          <div class="type-row">
            <button
              v-for="t in adTypes"
              :key="t.value"
              :class="['type-card', { active: form.adType === t.value }]"
              @click="form.adType = t.value"
            >
              <span class="type-ico">{{ t.icon }}</span>
              <span class="type-name">{{ t.value }}</span>
              <span class="type-desc">{{ t.desc }}</span>
            </button>
          </div>
        </div>

        <div class="f-item">
          <label class="f-label">头像</label>
          <AvatarUploader v-model="form.avatar" biz-type="avatar" fallback-text="广告" />
        </div>

        <div class="f-item">
          <label class="f-label">个人简介</label>
          <el-input v-model="form.intro" type="textarea" :rows="4" resize="none" maxlength="300" show-word-limit placeholder="介绍你的广告业务与成功案例…" />
        </div>

        <div class="f-item">
          <label class="f-label">代表作说明</label>
          <el-input v-model="form.representative" type="textarea" :rows="2" resize="none" placeholder="列举 1-3 个代表作品或案例" />
        </div>

        <button class="submit-btn" :disabled="loading" @click="submit">
          {{ loading ? '提交中…' : (mine ? '更新认证资料' : '提交认证') }}
        </button>
      </div>

      <aside class="side-col">
        <div class="panel">
          <div class="panel-head"><h2>投放能力</h2></div>
          <div class="perk-list">
            <div v-for="p in perks" :key="p.title" class="perk">
              <span class="perk-ico">{{ p.icon }}</span>
              <div>
                <b>{{ p.title }}</b>
                <span>{{ p.desc }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-head"><h2>审核说明</h2></div>
          <ul class="notice-list">
            <li>平台将在 1-3 个工作日内完成审核</li>
            <li>广告素材需符合平台内容规范</li>
            <li>资料不实将取消认证资格</li>
            <li>审核通过后自动开通广告工作台</li>
          </ul>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adApi } from '@/api'
import AvatarUploader from '@/components/AvatarUploader.vue'
import MediaDropzone from '@/components/studio/MediaDropzone.vue'

const loading = ref(false)
const mine = ref(null)
const form = reactive({
  companyName: '',
  realName: '',
  idCard: '',
  idCardFrontUrl: '',
  idCardBackUrl: '',
  adType: '音频广告',
  avatar: '',
  intro: '',
  representative: ''
})

const adTypes = [
  { value: '音频广告', icon: '🎧', desc: '贴片 / 插播音频' },
  { value: '视频广告', icon: '🎬', desc: 'MV 前贴片视频' },
  { value: '图文广告', icon: '🖼️', desc: '首页横幅 / 卡片' }
]

const perks = [
  { icon: '📈', title: '精准投放', desc: '按地区、年龄段定向投放' },
  { icon: '🎯', title: '素材管理', desc: '多版本素材在线管理与审核' },
  { icon: '📊', title: '效果报表', desc: '曝光、点击、转化全链路数据' },
  { icon: '💳', title: '账户余额', desc: '在线充值，按投放消耗结算' }
]

const statusText = (s) => ({ PENDING: '审核中', APPROVED: '已通过', REJECTED: '已拒绝', REVOKED: '已取消' }[s] || s)
const statusClass = (s) => ({ PENDING: 'warn', APPROVED: 'ok', REJECTED: 'bad', REVOKED: 'bad' }[s] || 'muted')

async function submit() {
  if (!form.companyName || !form.realName || !form.idCard || !form.idCardFrontUrl || !form.idCardBackUrl || !form.adType) {
    return ElMessage.warning('请填写主体名称、真实姓名、身份证号并上传身份证正反面')
  }
  loading.value = true
  try {
    await adApi.apply(form)
    ElMessage.success('提交成功，请等待审核')
    await load()
  } finally {
    loading.value = false
  }
}

async function load() {
  const res = await adApi.mine()
  mine.value = res.data
  if (res.data) Object.assign(form, res.data)
}

onMounted(load)
</script>

<style scoped>
.apply-page { padding-top: 4px !important; }

.apply-hero {
  position: relative;
  margin: 0 0 24px;
  padding: 34px 36px;
  border-radius: 26px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  background:
    radial-gradient(600px 320px at 8% 0%, rgba(192, 132, 252, .16), transparent 62%),
    radial-gradient(500px 300px at 95% 100%, rgba(255, 179, 71, .08), transparent 60%),
    linear-gradient(140deg, #14161a, #0a0b0d 70%);
  border: 1px solid var(--km-border);
}
.hero-glow {
  position: absolute;
  width: 360px;
  height: 360px;
  right: 40px;
  top: -160px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(192, 132, 252, .16), transparent 65%);
  filter: blur(30px);
  pointer-events: none;
}
.hero-left { position: relative; flex: 1; }
.hero-badge {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 6px;
  background: rgba(192, 132, 252, .14);
  border: 1px solid rgba(192, 132, 252, .3);
  color: var(--km-accent);
  font-size: 11px;
  font-weight: 800;
  margin-bottom: 12px;
}
.hero-title { margin: 0 0 10px; font-size: 30px; font-weight: 900; letter-spacing: -1px; }
.hero-sub { margin: 0 0 20px; font-size: 13px; line-height: 1.7; color: var(--km-text-dim); max-width: 560px; }

.hero-steps { display: flex; align-items: center; gap: 12px; }
.step { display: flex; align-items: center; gap: 7px; font-size: 12px; color: var(--km-text-dim); }
.s-num {
  width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: rgba(192, 132, 252, .14);
  border: 1px solid rgba(192, 132, 252, .3);
  color: var(--km-accent);
  font-size: 10px;
  font-weight: 800;
}
.s-line { width: 26px; height: 1px; background: rgba(255, 255, 255, .14); }
.hero-emoji {
  font-size: 92px;
  line-height: 1;
  filter: drop-shadow(0 12px 30px rgba(192, 132, 252, .25));
  animation: float 4s ease-in-out infinite;
}
@keyframes float { 50% { transform: translateY(-8px); } }

.layout { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr); gap: 22px; align-items: start; }
.side-col { display: flex; flex-direction: column; gap: 18px; }
.panel { background: var(--km-card); border: 1px solid var(--km-border); border-radius: var(--km-radius); padding: 24px; }
.panel-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.panel-head h2 { margin: 0; font-size: 17px; font-weight: 800; letter-spacing: -.3px; }

.reject-box {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  margin-bottom: 18px;
  border-radius: 12px;
  background: rgba(255, 77, 109, .08);
  border: 1px solid rgba(255, 77, 109, .22);
  font-size: 12px;
  color: #ff9aab;
}
.reject-box b { font-size: 11.5px; color: #ff8095; }

.f-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.f-item { margin-bottom: 18px; }
.f-label { display: block; font-size: 12px; color: var(--km-text-dim); font-weight: 600; margin-bottom: 8px; }
.f-label em { font-style: normal; color: var(--km-accent); }

.type-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.type-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid var(--km-border);
  background: rgba(255, 255, 255, .025);
  cursor: pointer;
  transition: all .18s;
  text-align: left;
}
.type-card:hover { border-color: var(--km-border-strong); background: rgba(255, 255, 255, .05); }
.type-card.active { border-color: var(--km-accent-line); background: rgba(192, 132, 252, .08); }
.type-ico { font-size: 20px; }
.type-name { font-size: 13px; font-weight: 800; color: var(--km-text); }
.type-card.active .type-name { color: var(--km-accent); }
.type-desc { font-size: 10.5px; color: var(--km-text-faint); }

.submit-btn {
  width: 100%;
  height: 48px;
  margin-top: 6px;
  border: none;
  border-radius: 14px;
  background: var(--km-accent);
  color: #07080a;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 1px;
  cursor: pointer;
  transition: all .2s;
  box-shadow: 0 12px 30px rgba(192, 132, 252, .2);
}
.submit-btn:hover:not(:disabled) { background: #d8b4fe; transform: translateY(-1px); }
.submit-btn:disabled { opacity: .6; cursor: wait; }

.pill {
  height: 22px;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  font-size: 10.5px;
  font-weight: 700;
}
.pill.warn { background: rgba(247, 215, 116, .14); color: #f7d774; }
.pill.ok { background: rgba(192, 132, 252, .14); color: var(--km-accent); }
.pill.bad { background: rgba(255, 77, 109, .14); color: #ff8095; }
.pill.muted { background: rgba(255, 255, 255, .07); color: var(--km-text-dim); }

.perk-list { display: flex; flex-direction: column; gap: 16px; }
.perk { display: flex; gap: 12px; align-items: flex-start; }
.perk-ico {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 11px;
  background: rgba(192, 132, 252, .08);
  border: 1px solid rgba(192, 132, 252, .16);
  font-size: 16px;
  flex-shrink: 0;
}
.perk b { display: block; font-size: 13px; font-weight: 700; margin-bottom: 2px; }
.perk span { font-size: 11.5px; color: var(--km-text-faint); line-height: 1.5; }

.notice-list { margin: 0; padding-left: 18px; }
.notice-list li { font-size: 12px; line-height: 1.95; color: var(--km-text-dim); }

@media (max-width: 1000px) {
  .layout { grid-template-columns: 1fr; }
  .hero-emoji { display: none; }
  .f-grid { grid-template-columns: 1fr; }
}
@media (max-width: 640px) {
  .type-row { grid-template-columns: 1fr; }
  .apply-hero { padding: 24px 20px; }
  .hero-title { font-size: 22px; }
}
</style>
