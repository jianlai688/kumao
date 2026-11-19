<template>
  <div class="km-page apply-page">
    <!-- 头部 -->
    <div class="apply-hero">
      <div class="hero-glow"></div>
      <div class="hero-left">
        <span class="hero-badge">主播入驻</span>
        <h1 class="hero-title">开一场属于自己的直播</h1>
        <p class="hero-sub">上传资料，通过审核后即可开播，与粉丝实时互动、收礼物、赚收益</p>
        <div class="hero-steps">
          <div class="step"><span class="s-num">1</span><span>填写资料</span></div>
          <i class="s-line"></i>
          <div class="step"><span class="s-num">2</span><span>平台审核</span></div>
          <i class="s-line"></i>
          <div class="step"><span class="s-num">3</span><span>开播赚钱</span></div>
        </div>
      </div>
      <div class="hero-emoji" aria-hidden="true"><el-icon><Microphone /></el-icon></div>
    </div>

    <div class="layout">
      <!-- 表单 -->
      <div class="panel form-card">
        <div class="panel-head">
          <h2>申请资料</h2>
          <span class="km-faint">请确保信息真实有效</span>
        </div>

        <div class="f-grid">
          <div class="f-item">
            <label class="f-label">真实姓名 <em>*</em></label>
            <el-input v-model="form.realName" placeholder="请输入真实姓名" size="large" />
          </div>
          <div class="f-item">
            <label class="f-label">身份证号 <em>*</em></label>
            <el-input v-model="form.idCard" placeholder="请输入身份证号" size="large" />
          </div>
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
          <label class="f-label">直播分类 <em>*</em></label>
          <div class="cat-row">
            <button
              v-for="c in categories"
              :key="c.value"
              :class="['cat-card', { active: form.liveCategory === c.value }]"
              @click="form.liveCategory = c.value"
            >
              <span class="cat-ico">{{ c.icon }}</span>
              <span class="cat-name">{{ c.value }}</span>
              <span class="cat-desc">{{ c.desc }}</span>
            </button>
          </div>
        </div>

        <div class="f-item">
          <label class="f-label">直播封面</label>
          <MediaDropzone
            v-model="form.coverUrl"
            kind="cover"
            biz-type="cover"
            title="拖拽直播封面到这里，或选择图片"
            hint="支持 JPG / PNG / WebP，建议 16:9，单张不超过 10MB"
            :max-size-mb="10"
          />
          <el-input v-model="form.coverUrl" class="cover-url-fallback" placeholder="也可以粘贴封面图片地址（https://...）" size="large">
            <template #prefix><el-icon><Picture /></el-icon></template>
          </el-input>
        </div>

        <div class="f-item">
          <label class="f-label">个人简介</label>
          <el-input v-model="form.intro" type="textarea" :rows="4" resize="none" maxlength="300" show-word-limit placeholder="介绍一下你的直播内容、风格与开播时间…" />
        </div>

        <button class="submit-btn" :disabled="loading" @click="submit">
          {{ loading ? '提交中…' : '提交申请' }}
        </button>
      </div>

      <!-- 记录 -->
      <aside class="panel record-card">
        <div class="panel-head">
          <h2>申请记录</h2>
          <span class="km-faint">{{ applies.length }} 条</span>
        </div>

        <div v-if="applies.length" class="record-list">
          <div v-for="a in applies" :key="a.id" class="record-item">
            <div class="r-dot" :class="statusClass(a.status)"></div>
            <div class="r-body">
              <div class="r-top">
                <span class="r-cat">{{ a.liveCategory }}</span>
                <span :class="['pill', statusClass(a.status)]">{{ statusText(a.status) }}</span>
              </div>
              <div class="r-time">{{ (a.createTime || '').replace('T', ' ').slice(0, 16) }}</div>
              <div v-if="a.rejectReason" class="r-reject">拒绝原因：{{ a.rejectReason }}</div>
            </div>
          </div>
        </div>

        <div v-else class="empty-box">
          <div class="empty-ico">📋</div>
          <p>暂无申请记录</p>
        </div>

        <div class="tips-card">
          <div class="tips-title">💡 入驻小贴士</div>
          <ul>
            <li>年满 18 周岁，实名认证通过</li>
            <li>准备一台可推流的设备与稳定网络</li>
            <li>审核通常在 1-3 个工作日内完成</li>
          </ul>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { userApi } from '@/api'
import MediaDropzone from '@/components/studio/MediaDropzone.vue'

const loading = ref(false)
const applies = ref([])
const form = reactive({ realName: '', idCard: '', idCardFrontUrl: '', idCardBackUrl: '', liveCategory: '弹唱', intro: '', coverUrl: '' })

const categories = [
  { value: '弹唱', icon: '🎸', desc: '吉他弹唱 · 沉浸现场' },
  { value: '电台', icon: '📻', desc: '深夜电台 · 声音陪伴' },
  { value: '翻唱', icon: '🎤', desc: '经典翻唱 · 实力演绎' }
]

const statusText = (s) => ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }[s] || s)
const statusClass = (s) => ({ PENDING: 'warn', APPROVED: 'ok', REJECTED: 'bad' }[s] || 'muted')

async function submit() {
  if (!form.realName || !form.idCard || !form.idCardFrontUrl || !form.idCardBackUrl || !form.liveCategory) {
    return ElMessage.warning('请填写真实姓名、身份证号并上传身份证正反面')
  }
  loading.value = true
  try {
    await userApi.applyAnchor(form)
    ElMessage.success('提交成功，请等待管理员审核')
    await load()
  } finally {
    loading.value = false
  }
}

async function load() {
  const res = await userApi.myAnchorApply()
  applies.value = res.data || []
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
    radial-gradient(500px 300px at 95% 100%, rgba(96, 190, 255, .08), transparent 60%),
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
.panel { background: var(--km-card); border: 1px solid var(--km-border); border-radius: var(--km-radius); padding: 24px; }
.panel-head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 20px; }
.panel-head h2 { margin: 0; font-size: 17px; font-weight: 800; letter-spacing: -.3px; }

.f-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.f-item { margin-bottom: 18px; }
.f-label { display: block; font-size: 12px; color: var(--km-text-dim); font-weight: 600; margin-bottom: 8px; }
.f-label em { font-style: normal; color: var(--km-accent); }
.cover-url-fallback { margin-top: 8px; }
.cover-url-fallback :deep(.el-input__wrapper) { background: rgba(255, 255, 255, .025); }

.cat-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.cat-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
  padding: 14px 14px 12px;
  border-radius: 14px;
  border: 1px solid var(--km-border);
  background: rgba(255, 255, 255, .025);
  cursor: pointer;
  transition: all .18s;
  text-align: left;
}
.cat-card:hover { border-color: var(--km-border-strong); background: rgba(255, 255, 255, .05); }
.cat-card.active { border-color: var(--km-accent-line); background: rgba(192, 132, 252, .08); }
.cat-ico { font-size: 20px; }
.cat-name { font-size: 13px; font-weight: 800; color: var(--km-text); }
.cat-card.active .cat-name { color: var(--km-accent); }
.cat-desc { font-size: 10.5px; color: var(--km-text-faint); }

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

/* 记录 */
.record-list { display: flex; flex-direction: column; gap: 2px; }
.record-item { display: flex; gap: 12px; padding: 12px 0; position: relative; }
.record-item + .record-item { border-top: 1px solid rgba(255, 255, 255, .05); }
.r-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
  box-shadow: 0 0 0 4px rgba(255, 255, 255, .04);
}
.r-dot.warn { background: #f7d774; }
.r-dot.ok { background: var(--km-accent); }
.r-dot.bad { background: #ff8095; }
.r-dot.muted { background: rgba(255, 255, 255, .3); }
.r-body { flex: 1; min-width: 0; }
.r-top { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.r-cat { font-size: 13px; font-weight: 700; }
.r-time { font-size: 11px; color: var(--km-text-faint); margin-top: 4px; }
.r-reject { margin-top: 6px; font-size: 11.5px; color: #ff9aab; background: rgba(255, 77, 109, .08); padding: 6px 10px; border-radius: 8px; }

.pill {
  height: 20px;
  padding: 0 9px;
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

.empty-box { padding: 40px 10px; text-align: center; }
.empty-ico { font-size: 30px; opacity: .45; margin-bottom: 10px; }
.empty-box p { margin: 0; font-size: 12.5px; color: var(--km-text-faint); }

.tips-card {
  margin-top: 18px;
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(192, 132, 252, .05);
  border: 1px solid rgba(192, 132, 252, .14);
}
.tips-title { font-size: 12px; font-weight: 800; color: var(--km-accent); margin-bottom: 8px; }
.tips-card ul { margin: 0; padding-left: 16px; }
.tips-card li { font-size: 11.5px; line-height: 1.85; color: var(--km-text-dim); }

@media (max-width: 1000px) {
  .layout { grid-template-columns: 1fr; }
  .hero-emoji { display: none; }
  .f-grid { grid-template-columns: 1fr; }
}
@media (max-width: 640px) {
  .cat-row { grid-template-columns: 1fr; }
  .apply-hero { padding: 24px 20px; }
  .hero-title { font-size: 22px; }
}
</style>
