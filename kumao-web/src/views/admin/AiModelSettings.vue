<template>
  <div class="model-settings">
    <section class="hero">
      <div class="hero-copy">
        <span class="eyebrow">KUMAO OPS · MODEL ROUTING</span>
        <h2>AI 模型配置</h2>
        <p>为内容审核、文本生成与图片识别分别指定模型。配置即时生效；审核模型不可用时自动降级可解释本地规则，不会阻断管理员处理队列。</p>
      </div>
      <div class="hero-actions">
        <span class="route-chip" :class="{ off: overview.status.mode !== 'AI_MODEL' }">
          <i></i>
          审核链路 · {{ overview.status.mode === 'AI_MODEL' ? `${overview.status.providerLabel} ${overview.status.model}` : '本地规则' }}
        </span>
        <el-button :loading="loading" @click="loadAll">刷新状态</el-button>
      </div>
    </section>

    <el-alert class="security-alert" type="info" :closable="false" show-icon>
      <template #title>密钥安全</template>
      API Key 加密后存储，页面只显示掩码；保存时留空表示沿用原密钥。更换部署口令 <code>AI_CONFIG_SECRET</code> 后需要重新录入。
    </el-alert>

    <section class="model-grid" v-loading="loading">
      <article
        v-for="card in cards"
        :key="card.type"
        class="model-card"
        :class="[`tone-${card.tone}`, { active: card.active }]"
      >
        <header class="card-head">
          <span class="card-icon">{{ card.icon }}</span>
          <div class="card-title">
            <span class="card-kicker">{{ card.index }} · {{ card.kicker }}</span>
            <h3>{{ card.label }}</h3>
          </div>
          <span class="status-pill" :class="statusClass(card)">{{ statusText(card) }}</span>
        </header>

        <p class="card-desc">{{ card.description }}</p>

        <dl class="spec-list">
          <div>
            <dt>服务商</dt>
            <dd>{{ card.providerLabel || '未配置' }}</dd>
          </div>
          <div>
            <dt>模型</dt>
            <dd class="mono">{{ card.modelName || '未填写' }}</dd>
          </div>
          <div class="wide">
            <dt>接口地址</dt>
            <dd class="mono ellipsis" :title="card.baseUrl">{{ card.baseUrl || '未填写' }}</dd>
          </div>
          <div>
            <dt>API Key</dt>
            <dd class="mono">{{ card.apiKeyMasked || '未配置' }}</dd>
          </div>
          <div>
            <dt>调用参数</dt>
            <dd>温度 {{ Number(card.temperature ?? 0).toFixed(2) }} · 超时 {{ card.timeoutSeconds }}s</dd>
          </div>
        </dl>

        <div v-if="card.environmentConfigured && card.type === 'REVIEW'" class="env-hint">
          环境变量兜底已生效，数据库配置启用后将优先使用页面设置
        </div>

        <footer class="card-foot">
          <span class="updated">{{ card.updateTime ? `更新于 ${formatTime(card.updateTime)}${card.updateByName ? ` · ${card.updateByName}` : ''}` : '尚未保存过配置' }}</span>
          <div class="foot-actions">
            <el-button size="small" :loading="testingType === card.type" @click="testSaved(card)">测试连接</el-button>
            <el-button size="small" type="primary" plain @click="openEdit(card)">配置</el-button>
          </div>
        </footer>
      </article>
    </section>

    <section class="routing-panel">
      <div class="routing-head">
        <span class="eyebrow">ROUTING MAP</span>
        <h3>三类模型的调用关系</h3>
        <p>配置统一存放在共享库 <code>ai_model_config</code>，各业务服务按能力类型读取，避免把模型名散落在代码与环境变量里。</p>
      </div>
      <div class="routing-list">
        <div class="routing-row">
          <span class="route-tag review">REVIEW</span>
          <div>
            <b>审核模型</b>
            <p>review-service 的 AI 审核工作台使用，生成 PASS / REVIEW / REJECT 建议与风险分；失败时降级本地规则。</p>
          </div>
          <span class="route-target">review-service</span>
        </div>
        <div class="routing-row">
          <span class="route-tag text">TEXT</span>
          <div>
            <b>文本模型</b>
            <p>供标题、简介、歌词、运营文案等文本生成能力读取，统一温度与超时策略。</p>
          </div>
          <span class="route-target">业务服务</span>
        </div>
        <div class="routing-row">
          <span class="route-tag vision">VISION</span>
          <div>
            <b>识别图片模型</b>
            <p>供封面、广告素材、证件图片等内容识别与辅助审核能力读取，建议使用支持多模态的模型。</p>
          </div>
          <span class="route-target">业务服务</span>
        </div>
      </div>
    </section>

    <el-drawer
      v-model="drawer.visible"
      class="model-drawer"
      size="min(540px, 100vw)"
      :title="drawerTitle"
      destroy-on-close
    >
      <template #header>
        <div class="drawer-head">
          <span class="drawer-icon">{{ currentMeta.icon }}</span>
          <div>
            <span class="card-kicker">{{ currentMeta.kicker }}</span>
            <h3>{{ currentMeta.label }}</h3>
          </div>
        </div>
      </template>

      <el-form label-position="top" class="model-form" @submit.prevent>
        <div class="enable-row">
          <div>
            <b>启用该模型</b>
            <p>关闭后该能力会直接使用降级策略或等待业务方接入。</p>
          </div>
          <el-switch v-model="form.enabled" />
        </div>

        <div class="form-grid">
          <el-form-item label="服务商">
            <el-select v-model="form.provider" @change="onProviderChange">
              <el-option v-for="preset in overview.presets" :key="preset.value" :label="preset.label" :value="preset.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="模型名称" required>
            <div class="model-picker">
              <el-select
                v-model="form.modelName"
                filterable
                allow-create
                default-first-option
                :loading="drawer.loadingModels"
                no-data-text="先填写接口地址和 API Key，再获取模型"
                placeholder="选择或输入模型 ID"
              >
                <el-option v-for="model in modelOptions" :key="model" :label="model" :value="model" />
              </el-select>
              <el-button size="small" :loading="drawer.loadingModels" @click="fetchModels(false)">获取模型</el-button>
            </div>
            <div class="field-hint">填写接口地址和 API Key 后会自动获取模型列表，也可直接输入模型 ID。</div>
          </el-form-item>
        </div>

        <el-form-item label="接口地址（OpenAI 兼容）" required>
          <el-input v-model="form.baseUrl" placeholder="https://api.deepseek.com" @blur="autoFetchModels" />
          <div class="field-hint">服务会自动追加 <code>/chat/completions</code>；测试连接优先探测 <code>/models</code>。</div>
        </el-form-item>

        <el-form-item label="API Key">
          <el-input
            v-model="form.apiKey"
            type="password"
            show-password
            autocomplete="new-password"
            :placeholder="keyPlaceholder"
            @blur="autoFetchModels"
          />
          <div class="field-hint">
            已保存密钥不会回显。留空保持不变，勾选下方选项可清除。
          </div>
        </el-form-item>
        <el-checkbox v-model="form.clearApiKey" class="clear-key">清除已保存的 API Key</el-checkbox>

        <div class="form-grid three">
          <el-form-item label="温度">
            <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" :precision="2" controls-position="right" />
          </el-form-item>
          <el-form-item label="超时（秒）">
            <el-input-number v-model="form.timeoutSeconds" :min="1" :max="120" controls-position="right" />
          </el-form-item>
          <el-form-item label="最大重试">
            <el-input-number v-model="form.maxRetries" :min="0" :max="5" controls-position="right" />
          </el-form-item>
        </div>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit placeholder="例如：审核主力模型，成本优先" />
        </el-form-item>

        <el-alert
          v-if="drawer.result"
          class="test-result"
          :type="drawer.result.success ? 'success' : 'error'"
          :title="drawer.result.message"
          :closable="false"
          show-icon
        >
          <span v-if="drawer.result.latencyMs != null">耗时 {{ drawer.result.latencyMs }} ms</span>
          <span v-if="drawer.result.httpStatus"> · HTTP {{ drawer.result.httpStatus }}</span>
          <span v-if="drawer.result.models?.length"> · 可用模型 {{ drawer.result.models.length }} 个</span>
        </el-alert>
      </el-form>

      <template #footer>
        <div class="drawer-foot">
          <el-button :loading="drawer.testing" @click="testDraft">测试连接</el-button>
          <div>
            <el-button @click="drawer.visible = false">取消</el-button>
            <el-button type="primary" :loading="drawer.saving" @click="save">保存配置</el-button>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { reviewApi } from '@/api'

const cardMeta = {
  REVIEW: { index: '01', icon: '🛡️', kicker: 'CONTENT SAFETY', tone: 'violet', usage: 'AI 审核工作台' },
  TEXT: { index: '02', icon: '✍️', kicker: 'TEXT GENERATION', tone: 'blue', usage: '文本生成能力' },
  VISION: { index: '03', icon: '👁️', kicker: 'IMAGE UNDERSTANDING', tone: 'amber', usage: '图片识别能力' }
}

const loading = ref(false)
const testingType = ref('')
const overview = reactive({ items: [], presets: [], status: {} })
const drawer = reactive({ visible: false, type: 'REVIEW', saving: false, testing: false, loadingModels: false, modelCatalog: [], result: null })
const form = reactive({
  enabled: false,
  provider: 'deepseek',
  modelName: '',
  baseUrl: '',
  apiKey: '',
  clearApiKey: false,
  temperature: 0.1,
  timeoutSeconds: 30,
  maxRetries: 2,
  remark: ''
})
const previousProvider = ref('deepseek')
let catalogTimer = null

watch(() => [form.baseUrl, form.apiKey], () => {
  if (catalogTimer) clearTimeout(catalogTimer)
  if (!form.baseUrl?.trim() || !form.apiKey?.trim()) return
  catalogTimer = setTimeout(() => autoFetchModels(), 700)
})

const cards = computed(() => (overview.items || []).map((item) => ({
  ...cardMeta[item.type],
  ...item
})))

const currentCard = computed(() => (overview.items || []).find((item) => item.type === drawer.type))
const currentMeta = computed(() => ({
  ...(cardMeta[drawer.type] || cardMeta.REVIEW),
  label: currentCard.value?.label || '',
  description: currentCard.value?.description || ''
}))
const drawerTitle = computed(() => `配置${currentMeta.value.label}`)
const modelOptions = computed(() => {
  const preset = (overview.presets || []).find((item) => item.value === form.provider)
  return [...new Set([...(drawer.modelCatalog || []), ...(preset?.models || []), form.modelName].filter(Boolean))]
})
const keyPlaceholder = computed(() => {
  const card = currentCard.value
  if (card?.apiKeyMasked) return `已保存 ${card.apiKeyMasked}，留空保持不变`
  return 'sk-...'
})

async function loadAll() {
  loading.value = true
  try {
    const res = await reviewApi.models()
    const data = res?.data || {}
    overview.items = data.items || []
    overview.presets = data.presets || []
    overview.status = data.status || {}
  } catch (e) {
    ElMessage.error(e?.message || '模型配置加载失败，请确认 review-service 已启动')
  } finally {
    loading.value = false
  }
}

function openEdit(card) {
  drawer.type = card.type
  drawer.result = null
  drawer.modelCatalog = []
  form.enabled = Boolean(card.enabled)
  form.provider = card.provider || 'deepseek'
  form.modelName = card.modelName || ''
  form.baseUrl = card.baseUrl || ''
  form.apiKey = ''
  form.clearApiKey = false
  form.temperature = Number(card.temperature ?? 0.1)
  form.timeoutSeconds = Number(card.timeoutSeconds ?? 30)
  form.maxRetries = Number(card.maxRetries ?? 2)
  form.remark = card.remark || ''
  previousProvider.value = form.provider
  drawer.visible = true
}

function onProviderChange(value) {
  const previous = (overview.presets || []).find((item) => item.value === previousProvider.value)
  const next = (overview.presets || []).find((item) => item.value === value)
  if (!next) return
  drawer.modelCatalog = []
  if (!form.baseUrl || form.baseUrl === previous?.baseUrl) {
    form.baseUrl = next.baseUrl
  }
  if (!form.modelName || previous?.models?.includes(form.modelName)) {
    form.modelName = next.models?.[0] || ''
  }
  previousProvider.value = value
  autoFetchModels()
}

async function fetchModels(silent = false) {
  if (drawer.loadingModels) return
  const card = currentCard.value
  if (!form.baseUrl?.trim()) {
    if (!silent) ElMessage.warning('请先填写接口地址')
    return
  }
  if (!form.apiKey?.trim() && !card?.apiKeyConfigured) {
    if (!silent) ElMessage.warning('请先填写 API Key')
    return
  }
  drawer.loadingModels = true
  try {
    const res = await reviewApi.listModels(drawer.type, payload())
    const result = res?.data || {}
    if (result.success) {
      drawer.modelCatalog = result.models || []
      if (!form.modelName && drawer.modelCatalog.length) form.modelName = drawer.modelCatalog[0]
      if (!silent) ElMessage.success(result.message || `已获取 ${drawer.modelCatalog.length} 个模型`)
    } else if (!silent) {
      ElMessage.warning(result.message || '未获取到模型')
    }
  } catch (e) {
    if (!silent) ElMessage.warning(e?.message || '获取模型列表失败')
  } finally {
    drawer.loadingModels = false
  }
}

function autoFetchModels() {
  if (!drawer.visible) return
  if (form.baseUrl?.trim() && (form.apiKey?.trim() || currentCard.value?.apiKeyConfigured)) {
    fetchModels(true)
  }
}

function payload() {
  return {
    provider: form.provider,
    modelName: form.modelName?.trim(),
    baseUrl: form.baseUrl?.trim(),
    apiKey: form.apiKey?.trim() || undefined,
    clearApiKey: form.clearApiKey,
    temperature: form.temperature,
    timeoutSeconds: form.timeoutSeconds,
    maxRetries: form.maxRetries,
    enabled: form.enabled,
    remark: form.remark
  }
}

async function save() {
  if (form.enabled && !form.modelName?.trim()) return ElMessage.warning('启用前请填写模型名称')
  if (form.enabled && !form.baseUrl?.trim()) return ElMessage.warning('启用前请填写接口地址')
  if (form.enabled && !form.apiKey?.trim()
    && (!currentCard.value?.apiKeyConfigured || form.clearApiKey)) {
    return ElMessage.warning('启用前请填写 API Key')
  }
  drawer.saving = true
  try {
    await reviewApi.saveModel(drawer.type, payload())
    ElMessage.success(`${currentMeta.value.label}配置已保存`)
    drawer.visible = false
    await loadAll()
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    drawer.saving = false
  }
}

async function testDraft() {
  drawer.testing = true
  drawer.result = null
  try {
    const res = await reviewApi.testModel(drawer.type, payload())
    drawer.result = res?.data || { success: false, message: '测试未返回结果' }
    if (drawer.result.models?.length) {
      drawer.modelCatalog = drawer.result.models
      if (!form.modelName) form.modelName = drawer.modelCatalog[0]
    }
  } catch (e) {
    drawer.result = { success: false, message: e?.message || '连接测试失败' }
  } finally {
    drawer.testing = false
  }
}

async function testSaved(card) {
  testingType.value = card.type
  try {
    const res = await reviewApi.testModel(card.type, {})
    const result = res?.data || {}
    if (result.success) ElMessage.success(`${card.label}：${result.message}`)
    else ElMessage.warning(`${card.label}：${result.message || '连接失败'}`)
  } catch (e) {
    ElMessage.error(e?.message || '连接测试失败')
  } finally {
    testingType.value = ''
  }
}

function statusText(card) {
  if (card.active) return '运行中'
  if (card.type === 'REVIEW' && card.environmentConfigured) return '环境变量生效'
  if (card.enabled && !card.apiKeyConfigured) return '缺少密钥'
  if (card.enabled) return '已启用'
  return '未启用'
}

function statusClass(card) {
  if (card.active || (card.type === 'REVIEW' && card.environmentConfigured)) return 'ok'
  if (card.enabled) return 'warn'
  return 'idle'
}

function formatTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

onBeforeUnmount(() => {
  if (catalogTimer) clearTimeout(catalogTimer)
})

onMounted(loadAll)
</script>

<style scoped>
.model-settings { display: flex; flex-direction: column; gap: 16px; }
.hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 22px; padding: 25px 26px; border: 1px solid var(--km-border); border-radius: 16px; background: linear-gradient(118deg, #171124, #0d111c 58%, #0b1218); }
.eyebrow { color: #c084fc; font-size: 10.5px; letter-spacing: .16em; }
.hero h2 { margin: 8px 0 7px; font-size: 26px; letter-spacing: -.04em; }
.hero p { max-width: 720px; margin: 0; color: var(--km-text-faint); font-size: 12px; line-height: 1.7; }
.hero-actions { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.route-chip { display: inline-flex; align-items: center; gap: 6px; height: 29px; padding: 0 11px; border: 1px solid rgba(74, 222, 128, .18); border-radius: 999px; color: #86efac; background: rgba(74, 222, 128, .06); font-size: 10.5px; white-space: nowrap; }
.route-chip i { width: 5px; height: 5px; border-radius: 50%; background: currentColor; box-shadow: 0 0 0 3px rgba(74, 222, 128, .1); }
.route-chip.off { color: #f7d774; border-color: rgba(247, 215, 116, .2); background: rgba(247, 215, 116, .06); }
.route-chip.off i { box-shadow: 0 0 0 3px rgba(247, 215, 116, .1); }
.security-alert { background: #171424; border: 1px solid #44336b; }
.security-alert code, .routing-panel code, .field-hint code { color: #d8b4fe; font-size: 10.5px; }
.model-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.model-card { position: relative; display: flex; flex-direction: column; min-height: 300px; padding: 18px 18px 15px; border: 1px solid var(--km-border); border-radius: 14px; background: #0f0d14; overflow: hidden; transition: border-color .2s, transform .2s; }
.model-card::before { content: ''; position: absolute; inset: 0 0 auto; height: 3px; background: var(--tone-line); opacity: .7; }
.model-card:hover { border-color: var(--km-border-strong); }
.tone-violet { --tone-line: linear-gradient(90deg, #7c3aed, #c026d3); --tone-soft: rgba(192, 132, 252, .1); --tone-ink: #d8b4fe; }
.tone-blue { --tone-line: linear-gradient(90deg, #2563eb, #22d3ee); --tone-soft: rgba(96, 165, 250, .1); --tone-ink: #93c5fd; }
.tone-amber { --tone-line: linear-gradient(90deg, #d97706, #f472b6); --tone-soft: rgba(251, 191, 36, .09); --tone-ink: #fcd34d; }
.model-card.active { border-color: rgba(74, 222, 128, .2); }
.card-head { display: flex; align-items: center; gap: 11px; }
.card-icon { display: grid; place-items: center; width: 38px; height: 38px; border-radius: 10px; background: var(--tone-soft); font-size: 19px; flex-shrink: 0; }
.card-title { min-width: 0; flex: 1; }
.card-kicker { display: block; color: var(--km-text-faint); font-size: 9px; letter-spacing: .11em; }
.card-title h3 { margin: 3px 0 0; color: var(--km-text); font-size: 15px; font-weight: 650; letter-spacing: -.01em; }
.status-pill { display: inline-flex; align-items: center; height: 22px; padding: 0 8px; border-radius: 999px; font-size: 9.5px; white-space: nowrap; }
.status-pill.ok { color: #86efac; border: 1px solid rgba(74, 222, 128, .2); background: rgba(74, 222, 128, .07); }
.status-pill.warn { color: #fcd34d; border: 1px solid rgba(251, 191, 36, .2); background: rgba(251, 191, 36, .07); }
.status-pill.idle { color: var(--km-text-faint); border: 1px solid var(--km-border); background: rgba(255, 255, 255, .02); }
.card-desc { min-height: 34px; margin: 13px 0 12px; color: var(--km-text-faint); font-size: 11px; line-height: 1.55; }
.spec-list { display: grid; grid-template-columns: 1fr 1fr; gap: 9px 14px; margin: 0; padding: 12px 0 0; border-top: 1px solid var(--km-border); }
.spec-list .wide { grid-column: 1 / -1; }
.spec-list dt { color: var(--km-text-faint); font-size: 9.5px; }
.spec-list dd { margin: 3px 0 0; color: var(--km-text-dim); font-size: 11px; }
.mono { font-variant-numeric: tabular-nums; letter-spacing: .01em; }
.ellipsis { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.env-hint { margin-top: 10px; padding: 7px 9px; border: 1px solid rgba(251, 191, 36, .15); border-radius: 7px; color: #f7d774; background: rgba(251, 191, 36, .05); font-size: 9.5px; line-height: 1.5; }
.card-foot { display: flex; align-items: flex-end; justify-content: space-between; gap: 8px; margin-top: auto; padding-top: 14px; }
.updated { max-width: 175px; color: var(--km-text-faint); font-size: 9px; line-height: 1.5; }
.foot-actions { display: flex; gap: 6px; flex-shrink: 0; }
.routing-panel { padding: 20px 22px; border: 1px solid var(--km-border); border-radius: 14px; background: #ffffff02; }
.routing-head h3 { margin: 6px 0 5px; font-size: 15px; letter-spacing: -.01em; }
.routing-head p { margin: 0; color: var(--km-text-faint); font-size: 11px; line-height: 1.6; }
.routing-list { display: flex; flex-direction: column; margin-top: 14px; }
.routing-row { display: grid; grid-template-columns: 68px minmax(0, 1fr) 112px; align-items: center; gap: 14px; padding: 12px 0; border-top: 1px solid var(--km-border); }
.routing-row b { font-size: 12px; font-weight: 600; }
.routing-row p { margin: 3px 0 0; color: var(--km-text-faint); font-size: 10.5px; line-height: 1.55; }
.route-tag { display: inline-grid; place-items: center; height: 22px; border-radius: 6px; font-size: 8.5px; letter-spacing: .08em; }
.route-tag.review { color: #d8b4fe; background: rgba(192, 132, 252, .1); }
.route-tag.text { color: #93c5fd; background: rgba(96, 165, 250, .1); }
.route-tag.vision { color: #fcd34d; background: rgba(251, 191, 36, .09); }
.route-target { color: var(--km-text-faint); font-size: 9.5px; text-align: right; }
.drawer-head { display: flex; align-items: center; gap: 11px; }
.drawer-icon { display: grid; place-items: center; width: 36px; height: 36px; border-radius: 9px; background: rgba(192, 132, 252, .1); font-size: 18px; }
.drawer-head h3 { margin: 3px 0 0; color: var(--km-text); font-size: 16px; }
.model-form { display: flex; flex-direction: column; }
.enable-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 18px; padding: 12px 14px; border: 1px solid var(--km-border); border-radius: 9px; background: rgba(255, 255, 255, .02); }
.enable-row b { font-size: 12px; }
.enable-row p { margin: 3px 0 0; color: var(--km-text-faint); font-size: 10px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 12px; }
.form-grid.three { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.model-picker { display: flex; gap: 6px; width: 100%; }
.model-picker .el-select { flex: 1; min-width: 0; }
.model-picker .el-button { flex-shrink: 0; }
.field-hint { margin-top: 4px; color: var(--km-text-faint); font-size: 9.5px; line-height: 1.5; }
.clear-key { margin: -8px 0 16px; }
.test-result { margin-top: 6px; }
.drawer-foot { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
@media (max-width: 1200px) {
  .model-grid { grid-template-columns: 1fr; }
  .model-card { min-height: 0; }
  .card-desc { min-height: 0; }
}
@media (max-width: 760px) {
  .hero { align-items: flex-start; flex-direction: column; padding: 20px; }
  .hero-actions { width: 100%; justify-content: space-between; }
  .routing-row { grid-template-columns: 64px minmax(0, 1fr); }
  .route-target { display: none; }
  .form-grid, .form-grid.three { grid-template-columns: 1fr; }
  .spec-list { grid-template-columns: 1fr 1fr; }
}
</style>
