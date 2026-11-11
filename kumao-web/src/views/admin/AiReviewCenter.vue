<template>
  <div class="ai-review">
    <section class="hero">
      <div class="hero-copy">
        <span class="eyebrow">KUMAO OPS · AI REVIEW</span>
        <h2>AI 审核工作台</h2>
        <p>AI 自动审核待审内容：低风险通过项自动处理，未通过项进入人工复核。审核动作仍调用各业务真实 admin 接口，结论写入统一审核日志。</p>
      </div>
      <div class="hero-actions">
        <span class="model-chip" :class="{ offline: !model.online }">
          <i></i>{{ model.label }}
        </span>
        <router-link class="config-link" to="/admin/model-settings">模型配置</router-link>
        <el-button :loading="auto.runningAction" @click="runAuto">立即自动审核</el-button>
        <el-button type="primary" :loading="loading" @click="refreshAll">刷新审核队列</el-button>
      </div>
    </section>

    <el-alert class="policy-alert" type="info" :closable="false" show-icon>
      <template #title>审核策略</template>
      低风险内容由 AI 自动通过；AI 建议复核/驳回、自动处理失败或账号资质类项目进入人工复核。所有 AI 结论都会记录来源与原因，管理员仍可最终改判。
    </el-alert>

    <el-alert
      v-if="auto.status.enabled"
      class="auto-alert"
      :type="auto.status.modelConfigured ? 'success' : 'warning'"
      :closable="false"
      show-icon
    >
      <template #title>
        AI 自动审核{{ auto.status.modelConfigured ? '运行中' : '等待模型配置' }}
        <span v-if="auto.status.running" class="auto-running">· 正在执行</span>
      </template>
      每 {{ autoIntervalText }} 扫描一次；已自动通过 {{ auto.status.autoApproved || 0 }} 条，待人工复核 {{ auto.status.pendingAdmin || 0 }} 条。
      <span v-if="!auto.status.modelConfigured">请先在「模型配置」中启用审核模型。</span>
      <span v-else-if="auto.status.lastRunAt">最近执行：{{ formatTime(auto.status.lastRunAt) }}</span>
    </el-alert>

    <section class="stats-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <span class="stat-icon">{{ card.icon }}</span>
        <div><b>{{ card.value }}</b><small>{{ card.label }}</small></div>
        <em v-if="card.hint">{{ card.hint }}</em>
      </div>
    </section>

    <el-tabs v-model="activeTab" class="review-tabs" @tab-change="onTabChange">
      <el-tab-pane label="待审队列" name="queue">
        <div class="queue-toolbar">
          <div class="category-chips" role="tablist" aria-label="审核分类">
            <button
              v-for="cat in categoryFilters"
              :key="cat.key"
              type="button"
              class="category-chip"
              :class="{ active: categoryFilter === cat.key }"
              @click="categoryFilter = cat.key"
            >
              {{ cat.icon }} {{ cat.label }}
              <span>{{ cat.count }}</span>
            </button>
          </div>
          <div class="queue-tools">
            <el-input v-model="keyword" clearable placeholder="搜索对象 / 内容" style="width: 190px" />
            <el-select v-model="riskFilter" style="width: 120px">
              <el-option label="全部风险" value="ALL" />
              <el-option label="高风险" value="HIGH" />
              <el-option label="未分析" value="UNANALYZED" />
            </el-select>
            <el-button :loading="batchAnalyzing" @click="analyzeVisible">批量分析</el-button>
          </div>
        </div>

        <el-card class="queue-card" shadow="never">
          <el-table :data="filteredRows" v-loading="loading" empty-text="暂无待处理项目">
            <el-table-column label="类型" width="142">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ categoryLabel[row.category] }}</el-tag>
                <div class="sub mono">#{{ row.id }}</div>
              </template>
            </el-table-column>
            <el-table-column label="审核对象" min-width="290">
              <template #default="{ row }">
                <div class="object-title">{{ row.title || '未命名内容' }}</div>
                <div class="object-meta">{{ row.subtitle }}</div>
                <div class="object-content">{{ row.content || '暂无补充内容' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="AI 建议" width="142">
              <template #default="{ row }">
                <template v-if="row.ai">
                  <el-tag size="small" :type="decisionType(row.ai.decision)">{{ decisionText(row.ai.decision) }}</el-tag>
                  <div class="risk-line">
                    <span :class="riskClass(row.ai.riskScore)">{{ row.ai.riskScore }}%</span>
                    <span class="source-label">{{ sourceText(row.ai.source) }}</span>
                  </div>
                </template>
                <span v-else class="sub">待分析</span>
              </template>
            </el-table-column>
            <el-table-column label="分析依据" min-width="250">
              <template #default="{ row }">
                <div v-if="row.ai" class="reasons">{{ row.ai.reasons }}</div>
                <div v-else class="sub">点击“AI 分析”生成可解释建议</div>
                <div v-if="row.ai?.fallbackReason" class="fallback">{{ row.ai.fallbackReason }}</div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="205" fixed="right">
              <template #default="{ row }">
                <el-button size="small" :loading="row.analyzing" @click="analyze(row)">AI 分析</el-button>
                <el-button size="small" type="success" @click="openDecision(row, true)">通过</el-button>
                <el-button size="small" type="danger" plain @click="openDecision(row, false)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="queue-foot">
            <span>当前显示 {{ filteredRows.length }} 条 · 共加载 {{ rows.length }} 条待审</span>
            <span v-if="failedCategories.length" class="load-warning">{{ failedCategories.length }} 个分类加载失败</span>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="审核日志与统计" name="logs">
        <div class="log-overview">
          <div class="log-stat"><b>{{ stats.total || 0 }}</b><span>累计审核</span></div>
          <div class="log-stat"><b class="ok">{{ stats.approved || 0 }}</b><span>通过</span></div>
          <div class="log-stat"><b class="bad">{{ stats.rejected || 0 }}</b><span>驳回</span></div>
          <div class="log-stat"><b>{{ stats.today || 0 }}</b><span>今日处理</span></div>
        </div>
        <div class="category-breakdown">
          <div v-for="cat in categoryBreakdown" :key="cat.key" class="breakdown-item">
            <span>{{ cat.label }}</span>
            <b>{{ cat.total }}</b>
            <small>通过 {{ cat.approved }} · 驳回 {{ cat.rejected }}</small>
          </div>
        </div>
        <div class="log-toolbar">
          <el-select v-model="logCategory" clearable placeholder="审核类型" style="width: 150px" @change="loadLogs">
            <el-option v-for="cat in categories" :key="cat.key" :label="cat.label" :value="cat.key" />
          </el-select>
          <el-select v-model="logDecision" clearable placeholder="审核结论" style="width: 130px" @change="loadLogs">
            <el-option label="通过" value="APPROVED" />
            <el-option label="驳回" value="REJECTED" />
          </el-select>
          <el-button @click="loadLogs">刷新日志</el-button>
        </div>
        <el-card class="queue-card" shadow="never">
          <el-table :data="logRows" v-loading="logLoading" empty-text="暂无审核日志">
            <el-table-column label="时间" width="160">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="类型" width="125">
              <template #default="{ row }">{{ categoryLabel[row.category] || row.category }}</template>
            </el-table-column>
            <el-table-column label="对象" min-width="220">
              <template #default="{ row }">
                <div class="object-title">{{ row.itemTitle || '未命名内容' }}</div>
                <div class="sub mono">#{{ row.itemId }}</div>
              </template>
            </el-table-column>
            <el-table-column label="结论" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.decision === 'APPROVED' ? 'success' : 'danger'">{{ row.decision === 'APPROVED' ? '通过' : '驳回' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="AI 来源" width="130">
              <template #default="{ row }">
                <span class="source-label">{{ sourceText(row.aiSource) }}</span>
                <div class="sub">风险 {{ row.aiRiskScore ?? '--' }}%</div>
              </template>
            </el-table-column>
            <el-table-column label="管理员" width="120" prop="adminName" />
            <el-table-column label="人工备注" min-width="220">
              <template #default="{ row }">{{ row.adminReason || '—' }}</template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination
              v-model:current-page="logPage"
              v-model:page-size="logPageSize"
              :total="logTotal"
              layout="prev, pager, next, total"
              @current-change="loadLogs"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="decision.visible" :title="decision.approved ? '确认通过审核' : '确认驳回审核'" width="min(560px, calc(100vw - 24px))" class="decision-dialog">
      <div class="decision-object">
        <div><span>{{ categoryLabel[decision.row?.category] }}</span><strong>{{ decision.row?.title }}</strong></div>
        <small class="mono">#{{ decision.row?.id }} · {{ decision.row?.subtitle }}</small>
      </div>
      <div v-if="decision.row?.ai" class="decision-ai">
        <div class="decision-ai-head">
          <el-tag size="small" :type="decisionType(decision.row.ai.decision)">{{ decisionText(decision.row.ai.decision) }}</el-tag>
          <span>风险 {{ decision.row.ai.riskScore }}%</span>
          <span class="source-label">{{ sourceText(decision.row.ai.source) }}</span>
        </div>
        <p>{{ decision.row.ai.reasons }}</p>
      </div>
      <el-alert type="warning" :closable="false" show-icon>
        AI 建议仅供参考，最终审核结论由你确认；确认后将调用原业务 admin 接口，并写入统一审核日志。
      </el-alert>
      <el-form label-position="top" class="decision-form">
        <el-form-item :label="decision.approved ? '审核备注（可选）' : '驳回原因（必填）'">
          <el-input v-model="decision.reason" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请填写人工审核依据，便于后续追溯" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="decision.visible = false">取消</el-button>
        <el-button :type="decision.approved ? 'success' : 'danger'" :loading="decision.submitting" @click="confirmDecision">
          {{ decision.approved ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adApi, commentApi, liveApi, musicApi, musicianApi, reviewApi, userApi } from '@/api'
import { reviewWithAi } from '@/utils/aiReviewHarness'

const activeTab = ref('queue')
const loading = ref(false)
const categoryFilter = ref('ALL')
const riskFilter = ref('ALL')
const keyword = ref('')
const rows = ref([])
const failedCategories = ref([])
const batchAnalyzing = ref(false)
const stats = ref({ total: 0, approved: 0, rejected: 0, today: 0, byCategory: {}, bySource: {} })
const model = reactive({ online: true, label: 'AI 模型检测中' })
const auto = reactive({ status: {}, tasks: [], runningAction: false })
const logs = ref([])
const logLoading = ref(false)
const logTotal = ref(0)
const logPage = ref(1)
const logPageSize = ref(10)
const logCategory = ref('')
const logDecision = ref('')
const decision = reactive({ visible: false, row: null, approved: true, reason: '', submitting: false })

const categoryLabel = {
  ANCHOR_APPLY: '主播资质',
  MUSICIAN: '音乐人资质',
  ADVERTISER: '广告制作人',
  AD_MATERIAL: '广告素材',
  AD_PLAN: '广告投放',
  SONG: '歌曲',
  MV: 'MV',
  COMMENT: '评论',
  LIVE_ROOM: '直播间',
  DANMAKU: '直播弹幕',
  SINGER: '歌手认证'
}

const categories = [
  { key: 'ANCHOR_APPLY', label: '主播资质', icon: '🎙️', load: () => userApi.anchorApplies({ pageNum: 1, pageSize: 30, status: 'PENDING' }) },
  { key: 'MUSICIAN', label: '音乐人资质', icon: '🎵', load: () => musicianApi.adminPage({ pageNum: 1, pageSize: 30, status: 'PENDING' }) },
  { key: 'ADVERTISER', label: '广告制作人', icon: '📣', load: () => adApi.adminPage({ pageNum: 1, pageSize: 30, status: 'PENDING' }) },
  { key: 'AD_MATERIAL', label: '广告素材', icon: '🖼️', load: () => adApi.adminMaterials({ pageNum: 1, pageSize: 30, status: 'PENDING' }) },
  { key: 'AD_PLAN', label: '广告投放', icon: '📅', load: () => adApi.adminPlans({ pageNum: 1, pageSize: 30, status: 'PENDING' }) },
  { key: 'SONG', label: '歌曲', icon: '🎧', load: () => musicApi.songs({ pageNum: 1, pageSize: 30, status: 0 }) },
  { key: 'MV', label: 'MV', icon: '🎬', load: () => musicApi.adminMvs({ pageNum: 1, pageSize: 30, auditStatus: 'PENDING' }) },
  { key: 'COMMENT', label: '评论', icon: '💬', load: () => commentApi.adminPage({ pageNum: 1, pageSize: 30, status: 0 }) },
  { key: 'LIVE_ROOM', label: '直播间', icon: '📺', load: () => liveApi.adminPage({ pageNum: 1, pageSize: 30, status: 'LIVING', banned: 0 }) },
  { key: 'DANMAKU', label: '直播弹幕', icon: '💭', load: () => liveApi.adminDanmaku({ pageNum: 1, pageSize: 30 }) },
  { key: 'SINGER', label: '歌手认证', icon: '⭐', load: () => musicApi.adminSingers({ pageNum: 1, pageSize: 30, status: 0 }) }
]

const categoryFilters = computed(() => [
  { key: 'ALL', label: '全部', icon: '◉', count: rows.value.length },
  ...categories.map((cat) => ({ ...cat, count: rows.value.filter((row) => row.category === cat.key).length }))
])

const filteredRows = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return rows.value.filter((row) => {
    if (row.autoTask?.status === 'AUTO_APPROVED' || row.autoTask?.status === 'RESOLVED') return false
    if (categoryFilter.value !== 'ALL' && row.category !== categoryFilter.value) return false
    if (riskFilter.value === 'HIGH' && (row.ai?.riskScore || 0) < 55) return false
    if (riskFilter.value === 'UNANALYZED' && row.ai) return false
    if (text && ![row.title, row.subtitle, row.content, row.category].filter(Boolean).join(' ').toLowerCase().includes(text)) return false
    return true
  })
})

const categoryBreakdown = computed(() => categories.map((cat) => {
  const counts = stats.value.byCategory?.[cat.key] || {}
  return { ...cat, total: counts.total || 0, approved: counts.approved || 0, rejected: counts.rejected || 0 }
}))

const highRiskCount = computed(() => rows.value.filter((row) => (row.ai?.riskScore || 0) >= 55).length)
const autoIntervalText = computed(() => `${Math.round((auto.status.intervalMs || 60000) / 1000)} 秒`)
const statCards = computed(() => [
  { icon: '📥', label: '待处理项目', value: rows.value.length, hint: failedCategories.value.length ? `${failedCategories.value.length} 类加载失败` : '来自真实 admin 队列' },
  { icon: '⚠️', label: '高风险建议', value: highRiskCount.value, hint: '风险 ≥ 55%' },
  { icon: '🤖', label: 'AI 自动通过', value: auto.status.autoApproved || 0, hint: '累计自动处理' },
  { icon: '🪵', label: '今日已确认', value: stats.value.today || 0, hint: '已写入审核日志' }
])

function records(res) {
  const data = res?.data
  if (Array.isArray(data)) return data
  return data?.records || []
}

function parseSignals(value) {
  if (Array.isArray(value)) return value
  try {
    const parsed = JSON.parse(value || '[]')
    return Array.isArray(parsed) ? parsed : []
  } catch (e) {
    return []
  }
}

function applyAutoTasks() {
  const map = new Map((auto.tasks || []).map((task) => [`${task.category}:${task.itemId}`, task]))
  rows.value.forEach((row) => {
    const task = map.get(`${row.category}:${row.id}`)
    if (!task) return
    row.autoTask = task
    row.ai = {
      decision: task.decision,
      riskScore: task.riskScore,
      reasons: task.reasons,
      signals: parseSignals(task.signals),
      source: task.source,
      provider: task.provider,
      model: task.model,
      fallbackReason: task.status === 'APPROVAL_FAILED' ? (task.approvalError || '自动处理失败') : null
    }
  })
}

async function loadAuto() {
  try {
    const [statusRes, tasksRes] = await Promise.all([
      reviewApi.autoStatus(),
      reviewApi.autoTasks({ pageNum: 1, pageSize: 100 })
    ])
    auto.status = statusRes?.data || {}
    auto.tasks = tasksRes?.data?.records || []
    applyAutoTasks()
  } catch (e) {
    auto.status = {}
    auto.tasks = []
  }
}

function text(value) {
  return value == null ? '' : String(value)
}

function slotText(slot) {
  return ({ SPLASH: '开屏广告', HOME_BANNER: '首页 Banner', LIVE: '直播页', PLAYER: '播放页' }[slot] || slot || '未知版位')
}

function mapItem(category, raw) {
  let title = ''
  let subtitle = ''
  let content = ''
  let metadata = {}
  switch (category) {
    case 'ANCHOR_APPLY':
      title = raw.realName || `主播申请 #${raw.userId}`
      subtitle = `直播分类：${raw.liveCategory || '未填写'} · 用户 #${raw.userId}`
      content = raw.intro || ''
      metadata = { userId: raw.userId, liveCategory: raw.liveCategory, status: raw.status }
      break
    case 'MUSICIAN':
      title = raw.stageName || raw.realName || `音乐人 #${raw.id}`
      subtitle = `${raw.style || '未填写风格'} · ${raw.realName || '未填写实名'}`
      content = raw.intro || raw.representative || ''
      metadata = { stageName: raw.stageName, style: raw.style, status: raw.status }
      break
    case 'ADVERTISER':
      title = raw.companyName || `广告制作人 #${raw.id}`
      subtitle = `${raw.adType || '未填写类型'} · ${raw.realName || '未填写实名'}`
      content = raw.intro || raw.representative || ''
      metadata = { companyName: raw.companyName, adType: raw.adType, status: raw.status }
      break
    case 'AD_MATERIAL':
      title = raw.name || `广告素材 #${raw.id}`
      subtitle = `${raw.type || '素材'} · 广告制作人 #${raw.advertiserId}`
      content = raw.content || raw.coverUrl || raw.fileUrl || ''
      metadata = { type: raw.type, coverUrl: raw.coverUrl, mediaUrl: raw.fileUrl, status: raw.status }
      break
    case 'AD_PLAN':
      title = raw.materialName || `投放计划 #${raw.id}`
      subtitle = `${slotText(raw.slot)} · ${raw.days || '?'} 天 · ¥${Number(raw.amount || 0).toFixed(2)}`
      content = `支付状态：${raw.paymentStatus === 'PAID' ? '已支付' : '待支付'} · 投放状态：${raw.status}`
      metadata = { slot: raw.slot, days: raw.days, amount: raw.amount, paymentStatus: raw.paymentStatus, status: raw.status }
      break
    case 'SONG':
      title = raw.name || `歌曲 #${raw.id}`
      subtitle = `${raw.singerName || '未知歌手'} · ${raw.style || '未分类'}`
      content = `${raw.language || ''} ${raw.region || ''} ${raw.albumName || ''}`.trim()
      metadata = { singerName: raw.singerName, audioUrl: raw.audioUrl, coverUrl: raw.coverUrl, status: raw.status }
      break
    case 'MV':
      title = raw.name || `MV #${raw.id}`
      subtitle = `${raw.singerName || '未知歌手'} · ${raw.quality || 'HD'}`
      content = raw.description || ''
      metadata = { singerName: raw.singerName, mediaUrl: raw.videoUrl, coverUrl: raw.coverUrl, auditStatus: raw.auditStatus }
      break
    case 'COMMENT':
      title = raw.nickname || `用户 #${raw.userId}`
      subtitle = `评论歌曲 #${raw.songId} · ${raw.parentId ? '回复' : '主评论'}`
      content = raw.content || ''
      metadata = { userId: raw.userId, songId: raw.songId, status: raw.status }
      break
    case 'LIVE_ROOM':
      title = raw.title || `直播间 #${raw.id}`
      subtitle = `${raw.anchorName || '未知主播'} · ${raw.category || '未分类'} · ${raw.status}`
      content = raw.intro || ''
      metadata = { anchorId: raw.anchorId, banned: raw.banned, status: raw.status }
      break
    case 'DANMAKU':
      title = raw.nickname || `用户 #${raw.userId}`
      subtitle = `直播间 #${raw.roomId} · ${raw.type || 'TEXT'}`
      content = raw.content || ''
      metadata = { userId: raw.userId, roomId: raw.roomId, type: raw.type }
      break
    case 'SINGER':
      title = raw.name || `歌手 #${raw.id}`
      subtitle = `${raw.gender || '未填写'} · ${raw.region || '未填写地区'}`
      content = raw.intro || ''
      metadata = { musicianId: raw.musicianId, region: raw.region, status: raw.status }
      break
  }
  return {
    category,
    raw,
    id: raw.id,
    title,
    subtitle,
    content: text(content),
    metadata,
    ai: null,
    analyzing: false
  }
}

async function loadQueue() {
  loading.value = true
  failedCategories.value = []
  try {
    const results = await Promise.all(categories.map(async (cat) => {
      try {
        const res = await cat.load()
        let list = records(res)
        if (cat.key === 'AD_PLAN') list = list.filter((item) => item.paymentStatus === 'PAID')
        return list.map((item) => mapItem(cat.key, item))
      } catch (e) {
        failedCategories.value.push(cat.label)
        return []
      }
    }))
    rows.value = results.flat()
    applyAutoTasks()
    rows.value = rows.value.filter((row) => row.autoTask?.status !== 'AUTO_APPROVED'
      && row.autoTask?.status !== 'RESOLVED')
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res = await reviewApi.stats()
    stats.value = { ...stats.value, ...(res.data || {}) }
  } catch (e) {
    stats.value = { ...stats.value, total: 0, approved: 0, rejected: 0, today: 0 }
  }
}

async function loadModelStatus() {
  try {
    const res = await reviewApi.status()
    const data = res.data || {}
    model.online = data.mode === 'AI_MODEL'
    model.label = model.online
      ? `${data.providerLabel || 'AI'} · ${data.model || '未指定模型'}`
      : '未配置审核模型 · 本地规则'
  } catch (e) {
    model.online = false
    model.label = 'AI 服务暂不可用'
  }
}

async function loadLogs() {
  logLoading.value = true
  try {
    const res = await reviewApi.logs({ pageNum: logPage.value, pageSize: logPageSize.value, category: logCategory.value || undefined, decision: logDecision.value || undefined })
    logs.value = res.data?.records || []
    logTotal.value = res.data?.total || 0
  } catch (e) {
    logs.value = []
    logTotal.value = 0
  } finally {
    logLoading.value = false
  }
}

async function refreshAll() {
  await loadAuto()
  await Promise.all([loadQueue(), loadStats(), loadModelStatus()])
  if (activeTab.value === 'logs') await loadLogs()
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function runAuto() {
  if (auto.runningAction) return
  auto.runningAction = true
  try {
    const res = await reviewApi.runAuto()
    auto.status = { ...auto.status, running: true }
    ElMessage.success(res?.message || '已提交自动审核任务')
    for (let i = 0; i < 20; i++) {
      await sleep(1500)
      await loadAuto()
      if (!auto.status.running) break
    }
    await Promise.all([loadQueue(), loadStats(), loadModelStatus()])
    if (auto.status.lastSummary?.processed != null) {
      ElMessage.success(`自动审核完成：处理 ${auto.status.lastSummary.processed} 条，自动通过 ${auto.status.lastSummary.autoApproved || 0} 条`)
    }
  } catch (e) {
    ElMessage.error(e?.message || '自动审核触发失败')
  } finally {
    auto.runningAction = false
  }
}

async function analyze(row) {
  if (!row || row.analyzing) return
  row.analyzing = true
  try {
    row.ai = await reviewWithAi(row, row.category)
    ElMessage.success(row.ai.source === 'LOCAL_RULE' ? '本地规则分析完成（已标注降级原因）' : 'AI 模型分析完成')
  } catch (e) {
    ElMessage.error('AI 分析失败，请稍后重试')
  } finally {
    row.analyzing = false
  }
}

async function analyzeVisible() {
  const pending = filteredRows.value.filter((row) => !row.ai).slice(0, 8)
  if (!pending.length) return ElMessage.info('当前没有待分析项目')
  batchAnalyzing.value = true
  try {
    for (const row of pending) await analyze(row)
  } finally {
    batchAnalyzing.value = false
  }
}

function openDecision(row, approved) {
  decision.row = row
  decision.approved = approved
  decision.reason = ''
  decision.visible = true
}

async function executeAction(row, approved, reason) {
  switch (row.category) {
    case 'ANCHOR_APPLY':
      return userApi.auditAnchor(row.id, approved, reason)
    case 'MUSICIAN':
      return musicianApi.audit(row.id, approved, reason)
    case 'ADVERTISER':
      return adApi.audit(row.id, approved, reason)
    case 'AD_MATERIAL':
      return adApi.auditMaterial(row.id, approved, reason)
    case 'AD_PLAN':
      return adApi.auditPlan(row.id, approved, reason)
    case 'SONG':
      return musicApi.updateSongStatus(row.id, approved ? 1 : 2)
    case 'MV':
      return musicApi.auditMv(row.id, approved, reason)
    case 'COMMENT':
      return commentApi.adminStatus(row.id, approved ? 0 : 1)
    case 'LIVE_ROOM':
      return liveApi.ban(row.id, !approved, reason)
    case 'DANMAKU':
      if (!approved) return liveApi.deleteDanmaku(row.id)
      return Promise.resolve()
    case 'SINGER':
      return musicApi.auditSinger(row.id, approved, reason)
    default:
      throw new Error('未知审核类型')
  }
}

async function confirmDecision() {
  const row = decision.row
  if (!row) return
  if (!decision.approved && !decision.reason.trim()) return ElMessage.warning('驳回时必须填写原因')
  decision.submitting = true
  try {
    await executeAction(row, decision.approved, decision.reason.trim())
    if (row.autoTask) {
      try {
        await reviewApi.resolveAutoTask(row.category, String(row.id))
      } catch (e) {
        // 任务清理失败不影响本次人工审核结果。
      }
    }
    let logged = true
    try {
      await reviewApi.log({
        category: row.category,
        itemId: row.id == null ? null : String(row.id),
        itemTitle: row.title,
        decision: decision.approved ? 'APPROVED' : 'REJECTED',
        adminReason: decision.reason.trim(),
        aiDecision: row.ai?.decision || null,
        aiRiskScore: row.ai?.riskScore ?? null,
        aiSource: row.ai?.source || null,
        aiReasons: row.ai?.reasons || null
      })
    } catch (e) {
      logged = false
    }
    rows.value = rows.value.filter((item) => item !== row)
    decision.visible = false
    await Promise.all([loadStats(), loadAuto()])
    if (logged) ElMessage.success(decision.approved ? '已通过，审核日志已记录' : '已驳回，审核日志已记录')
    else ElMessage.warning('审核已执行，但审核日志写入失败，请检查 review-service')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '审核动作执行失败')
  } finally {
    decision.submitting = false
  }
}

function onTabChange(name) {
  if (name === 'logs') loadLogs()
}

function decisionText(value) {
  return ({ PASS: '建议通过', REVIEW: '建议复核', REJECT: '建议驳回' }[value] || '未分析')
}
function decisionType(value) {
  return ({ PASS: 'success', REVIEW: 'warning', REJECT: 'danger' }[value] || 'info')
}
function riskClass(score) {
  return score >= 70 ? 'risk-high' : score >= 35 ? 'risk-mid' : 'risk-low'
}
function sourceText(value) {
  if (value === 'LOCAL_RULE') return '本地规则'
  if (value === 'AI_MODEL' || value === 'DEEPSEEK') return 'AI 模型'
  return '未分析'
}
function formatTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

onMounted(refreshAll)
</script>

<style scoped>
.ai-review { display: flex; flex-direction: column; gap: 16px; }
.hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; padding: 24px; border: 1px solid var(--km-border); border-radius: 14px; background: linear-gradient(120deg, #171124, #0d111c 60%, #0b1118); }
.eyebrow { color: #c084fc; font-size: 11px; letter-spacing: .14em; }
.hero h2 { margin: 7px 0 6px; font-size: 25px; letter-spacing: -.04em; }
.hero p { margin: 0; max-width: 680px; color: var(--km-text-faint); font-size: 12px; line-height: 1.65; }
.hero-actions { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.model-chip { display: inline-flex; align-items: center; gap: 6px; height: 28px; padding: 0 9px; border: 1px solid rgba(74, 222, 128, .18); border-radius: 999px; color: #86efac; background: rgba(74, 222, 128, .06); font-size: 10.5px; white-space: nowrap; }
.model-chip i { width: 5px; height: 5px; border-radius: 50%; background: currentColor; box-shadow: 0 0 0 3px rgba(74, 222, 128, .1); }
.model-chip.offline { color: #f7d774; border-color: rgba(247, 215, 116, .2); background: rgba(247, 215, 116, .06); }
.model-chip.offline i { box-shadow: 0 0 0 3px rgba(247, 215, 116, .1); }
.config-link { padding: 0 2px; color: var(--km-text-faint); font-size: 11px; text-decoration: none; transition: color .18s; }
.config-link:hover { color: var(--km-accent); }
.policy-alert { background: #171424; border: 1px solid #44336b; }
.auto-alert { background: rgba(74, 222, 128, .04); border: 1px solid rgba(74, 222, 128, .16); }
.auto-alert :deep(.el-alert__title) { color: #86efac; font-size: 11px; }
.auto-alert :deep(.el-alert__content) { color: var(--km-text-faint); font-size: 10.5px; }
.auto-running { color: #fcd34d; font-weight: 500; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.stat-card { position: relative; display: flex; align-items: center; gap: 12px; min-height: 82px; padding: 15px 17px; border: 1px solid var(--km-border); border-radius: 12px; background: #ffffff04; }
.stat-icon { display: grid; place-items: center; width: 34px; height: 34px; border-radius: 9px; background: rgba(192, 132, 252, .09); font-size: 18px; }
.stat-card b { display: block; color: var(--km-text); font-size: 23px; line-height: 1.1; font-variant-numeric: tabular-nums; }
.stat-card small { display: block; margin-top: 5px; color: var(--km-text-faint); font-size: 10.5px; }
.stat-card em { position: absolute; right: 13px; top: 12px; color: var(--km-text-faint); font-size: 9.5px; font-style: normal; }
.review-tabs :deep(.el-tabs__header) { margin-bottom: 14px; }
.review-tabs :deep(.el-tabs__item) { font-size: 12px; }
.queue-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-bottom: 12px; }
.category-chips { display: flex; flex-wrap: wrap; gap: 5px; min-width: 0; }
.category-chip { display: inline-flex; align-items: center; gap: 5px; height: 27px; padding: 0 8px; border: 1px solid transparent; border-radius: 7px; background: transparent; color: var(--km-text-faint); font-size: 11px; cursor: pointer; white-space: nowrap; }
.category-chip:hover { color: var(--km-text); background: rgba(255, 255, 255, .04); }
.category-chip.active { color: var(--km-text); border-color: var(--km-border-strong); background: rgba(255, 255, 255, .06); }
.category-chip span { color: var(--km-accent); font-variant-numeric: tabular-nums; }
.queue-tools { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.queue-card { border: 1px solid var(--km-border); background: transparent; }
.queue-card :deep(.el-card__body) { padding: 0; }
.queue-card :deep(.el-table__row) { background: transparent; }
.queue-card :deep(.el-table) { --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: #ffffff04; }
.object-title { color: var(--km-text); font-size: 12.5px; font-weight: 600; }
.object-meta { margin-top: 4px; color: var(--km-text-faint); font-size: 10.5px; }
.object-content { display: -webkit-box; margin-top: 6px; overflow: hidden; color: var(--km-text-dim); font-size: 11px; line-height: 1.45; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.sub { color: var(--km-text-faint); font-size: 10.5px; }
.mono { font-variant-numeric: tabular-nums; letter-spacing: .02em; }
.risk-line { display: flex; align-items: center; gap: 6px; margin-top: 5px; font-size: 10px; }
.risk-high { color: #ff8fa2; font-weight: 700; }
.risk-mid { color: #f6c177; font-weight: 700; }
.risk-low { color: #72e6a2; font-weight: 700; }
.source-label { color: var(--km-text-faint); font-size: 9.5px; white-space: nowrap; }
.reasons { color: var(--km-text-dim); font-size: 11px; line-height: 1.55; }
.fallback { margin-top: 5px; color: #f7d774; font-size: 9.5px; line-height: 1.4; }
.queue-foot { display: flex; justify-content: space-between; gap: 10px; padding: 11px 15px; color: var(--km-text-faint); font-size: 10.5px; border-top: 1px solid var(--km-border); }
.load-warning { color: #f6c177; }
.log-overview { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 12px; }
.log-stat { padding: 13px 16px; border: 1px solid var(--km-border); border-radius: 10px; background: #ffffff04; }
.log-stat b { display: block; font-size: 22px; font-variant-numeric: tabular-nums; }
.log-stat b.ok { color: #72e6a2; }
.log-stat b.bad { color: #ff8fa2; }
.log-stat span { display: block; margin-top: 4px; color: var(--km-text-faint); font-size: 10.5px; }
.category-breakdown { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-bottom: 12px; }
.breakdown-item { padding: 9px 11px; border: 1px solid var(--km-border); border-radius: 8px; background: #ffffff03; }
.breakdown-item span { display: block; color: var(--km-text-faint); font-size: 10px; }
.breakdown-item b { display: inline-block; margin-top: 4px; color: var(--km-text); font-size: 17px; font-variant-numeric: tabular-nums; }
.breakdown-item small { display: block; margin-top: 2px; color: var(--km-text-faint); font-size: 9.5px; }
.log-toolbar { display: flex; gap: 8px; margin-bottom: 12px; }
.pager { display: flex; justify-content: flex-end; padding: 12px 15px; border-top: 1px solid var(--km-border); }
.decision-dialog { background: #0d0b11 !important; border: 1px solid #2c2634 !important; border-radius: 10px !important; }
.decision-dialog :deep(.el-dialog__header) { padding: 18px 20px 10px; margin: 0; border-bottom: 1px solid #25202d; }
.decision-dialog :deep(.el-dialog__title) { color: #f7f4fc; font-size: 16px; font-weight: 600; }
.decision-dialog :deep(.el-dialog__body) { padding: 16px 20px; color: #f7f4fc; }
.decision-dialog :deep(.el-dialog__footer) { padding: 0 20px 18px; }
.decision-object { padding: 12px 14px; border: 1px solid var(--km-border); border-radius: 9px; background: rgba(255, 255, 255, .025); }
.decision-object > div { display: flex; align-items: center; gap: 8px; justify-content: space-between; color: var(--km-text-faint); font-size: 10.5px; }
.decision-object strong { color: var(--km-text); font-size: 13px; font-weight: 600; text-align: right; }
.decision-object small { display: block; margin-top: 6px; color: var(--km-text-faint); font-size: 10px; }
.decision-ai { margin: 11px 0; padding: 11px 13px; border: 1px solid rgba(192, 132, 252, .18); border-radius: 9px; background: rgba(192, 132, 252, .06); }
.decision-ai-head { display: flex; align-items: center; gap: 8px; color: var(--km-text-dim); font-size: 10.5px; }
.decision-ai p { margin: 7px 0 0; color: var(--km-text-dim); font-size: 11px; line-height: 1.55; }
.decision-form { margin-top: 15px; }
@media (max-width: 1000px) {
  .stats-grid, .log-overview { grid-template-columns: repeat(2, 1fr); }
  .hero { align-items: flex-start; flex-direction: column; }
  .queue-toolbar { align-items: flex-start; flex-direction: column; }
  .queue-tools { flex-wrap: wrap; }
}
@media (max-width: 640px) {
  .hero { padding: 18px; }
  .hero h2 { font-size: 21px; }
  .hero-actions { width: 100%; justify-content: space-between; }
  .stats-grid, .log-overview { gap: 8px; }
  .category-breakdown { grid-template-columns: repeat(2, 1fr); }
  .stat-card { min-height: 70px; padding: 12px; }
  .stat-card b { font-size: 20px; }
  .stat-card em { display: none; }
  .category-chips { max-height: 86px; overflow-y: auto; }
  .queue-tools { width: 100%; }
  .queue-tools .el-input { flex: 1; min-width: 120px; }
  .log-toolbar { flex-wrap: wrap; }
}
</style>
