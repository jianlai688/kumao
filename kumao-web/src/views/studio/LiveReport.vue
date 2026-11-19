<template>
  <div v-if="report" class="report-page">
    <header class="report-head">
      <div class="report-title-group">
        <router-link to="/studio/anchor" class="report-back">
          <el-icon><ArrowLeft /></el-icon>主播工作台
        </router-link>
        <span class="report-divider" aria-hidden="true"></span>
        <div>
          <span class="eyebrow">SESSION REVIEW · LIVE REPORT</span>
          <h1>{{ report.title }}</h1>
          <div class="report-meta">
            <span>{{ formatDateTime(report.startTime) }} — {{ formatDateTime(report.endTime) }}</span>
            <span>时长 {{ formatDuration(report.durationSeconds) }}</span>
            <span>房间 #{{ report.roomId }}</span>
          </div>
        </div>
      </div>
      <div class="report-actions">
        <button type="button" class="ghost-action" @click="replay">
          <el-icon><Refresh /></el-icon>再来一场
        </button>
        <button type="button" class="primary-action" @click="exportReport">
          <el-icon><Download /></el-icon>导出报告
        </button>
      </div>
    </header>

    <section class="kpi-grid" aria-label="本场关键指标">
      <article v-for="item in kpiCards" :key="item.key" class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-label">{{ item.label }}</span>
          <span :class="['kpi-trend', item.change > 0 ? 'up' : item.change < 0 ? 'down' : 'flat']">
            <b>{{ item.change > 0 ? '↑' : item.change < 0 ? '↓' : '→' }}</b>
            {{ Math.abs(item.change).toFixed(1) }}%
          </span>
        </div>
        <strong class="kpi-value">
          <AnimatedNumber :value="item.value" :formatter="item.formatter" />
        </strong>
        <span class="kpi-note">{{ item.note }}</span>
      </article>
    </section>

    <div class="report-grid">
      <section class="report-card chart-card">
        <div class="card-head">
          <div>
            <span class="eyebrow">AUDIENCE PULSE</span>
            <h2>在线人数曲线</h2>
          </div>
          <span class="card-caption">峰值 {{ formatNumber(report.peakOnline) }} 人</span>
        </div>
        <div ref="chartWrap" class="chart-wrap" @mouseleave="hoverIndex = -1">
          <div class="chart-y-axis">
            <span v-for="line in chartGrid" :key="line.value" :style="{ top: line.top }">{{ line.label }}</span>
          </div>
          <svg
            ref="chartSvg"
            class="line-chart"
            viewBox="0 0 760 220"
            role="img"
            aria-label="在线人数曲线"
            @mousemove="onChartMove"
          >
            <defs>
              <linearGradient id="lineFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#d8b4fe" stop-opacity=".24" />
                <stop offset="100%" stop-color="#d8b4fe" stop-opacity="0" />
              </linearGradient>
              <linearGradient id="lineStroke" x1="0" y1="0" x2="1" y2="0">
                <stop offset="0%" stop-color="#a855f7" />
                <stop offset="100%" stop-color="#f0abfc" />
              </linearGradient>
            </defs>
            <g class="chart-grid">
              <line v-for="line in chartGrid" :key="`h-${line.value}`" :x1="chartPad.left" :x2="760 - chartPad.right" :y1="line.y" :y2="line.y" />
              <line v-for="line in chartVGrid" :key="`v-${line}`" :x1="line" :x2="line" :y1="chartPad.top" :y2="220 - chartPad.bottom" />
            </g>
            <polygon :points="areaPoints" fill="url(#lineFill)" />
            <polyline :points="linePoints" fill="none" stroke="url(#lineStroke)" stroke-width="2.4" stroke-linejoin="round" stroke-linecap="round" />
            <template v-if="hoverPoint">
              <line class="hover-line" :x1="hoverPoint.x" :x2="hoverPoint.x" :y1="chartPad.top" :y2="220 - chartPad.bottom" />
              <circle class="hover-dot" :cx="hoverPoint.x" :cy="hoverPoint.y" r="5" />
            </template>
          </svg>
          <div class="chart-x-axis">
            <span v-for="(point, index) in chartXLabels" :key="`x-${index}`" :style="{ left: `${(point.x / 760) * 100}%` }">{{ point.label }}</span>
          </div>
          <div v-if="hoverPoint" class="chart-tooltip" :style="tooltipStyle">
            <span>{{ hoverPoint.label }}</span>
            <strong>{{ hoverPoint.value }} <small>人在线</small></strong>
          </div>
        </div>
      </section>

      <section class="report-card gift-chart-card">
        <div class="card-head">
          <div>
            <span class="eyebrow">GIFT RHYTHM</span>
            <h2>礼物分钟分布</h2>
          </div>
          <span class="card-caption">{{ formatNumber(report.giftIncome) }} 酷币</span>
        </div>
        <div class="gift-bars" aria-label="礼物分钟分布柱状图">
          <div v-for="(item, index) in report.giftMinutes" :key="index" class="gift-bar-col" :title="`${item.label} · ${item.value} 酷币`">
            <span class="gift-bar-value">{{ item.value }}</span>
            <i class="gift-bar" :style="{ height: `${giftBarHeight(item.value)}%` }"></i>
            <span v-if="index % 2 === 0" class="gift-bar-label">{{ item.label }}</span>
          </div>
        </div>
      </section>

      <section class="report-card audience-card">
        <div class="card-head">
          <div>
            <span class="eyebrow">AUDIENCE MIX</span>
            <h2>观众画像</h2>
          </div>
          <span class="card-caption">
            {{ report.audience?.sampleCount ? `实时在线样本 ${report.audience.sampleCount}` : '本场互动观众' }}
          </span>
        </div>
        <div class="audience-grid">
          <div v-for="group in audienceGroups" :key="group.label" class="audience-group">
            <h3>{{ group.label }}</h3>
            <div v-for="item in group.items" :key="item.label" class="audience-row">
              <span class="audience-label">{{ item.label }}</span>
              <span class="audience-track"><i :style="{ width: `${Math.min(100, item.value)}%` }"></i></span>
              <b>{{ item.value }}%</b>
            </div>
          </div>
        </div>
      </section>

      <section class="report-card settlement-card">
        <div class="card-head">
          <div>
            <span class="eyebrow">SETTLEMENT</span>
            <h2>收益状态</h2>
          </div>
          <span :class="['settlement-badge', report.settlement?.status === 'SETTLED' ? 'settled' : 'pending']">
            {{ report.settlement?.status === 'SETTLED' ? '已结算' : '待结算（T+1）' }}
          </span>
        </div>
        <div class="settlement-amount">
          <span>本场礼物收益</span>
          <strong><AnimatedNumber :value="report.giftIncome" :formatter="formatNumber" /><small> 酷币</small></strong>
        </div>
        <p>{{ report.settlement?.note || '本场礼物收益将在直播结束后的次日（T+1）结算，结算完成后可在礼物收益中提现。' }}</p>
        <router-link to="/studio/earnings" class="settlement-link">查看礼物收益明细 <el-icon><ArrowRight /></el-icon></router-link>
      </section>
    </div>

    <footer class="report-footer">
      <div>
        <span class="eyebrow">ONE MORE SET</span>
        <h2>今晚的状态很好，下一场继续。</h2>
        <p>复用本场直播间的配置，直接进入开播准备。</p>
      </div>
      <button type="button" class="primary-action" @click="replay">
        <el-icon><Refresh /></el-icon>再来一场
      </button>
    </footer>
  </div>
  <div v-else class="report-page"><el-empty description="报告不存在" /></div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Download, Refresh } from '@element-plus/icons-vue'
import AnimatedNumber from '@/components/studio/AnimatedNumber.vue'
import {
  fallbackLiveReport,
  formatDateTime,
  formatDuration,
  loadLiveReport
} from '@/utils/liveSession'

const route = useRoute()
const router = useRouter()
const report = ref(null)
const hoverIndex = ref(-1)
const chartWrap = ref(null)
const chartSvg = ref(null)
const chartPad = { left: 38, right: 18, top: 18, bottom: 30 }
const chartHeight = 220

const formatNumber = (value) => {
  const n = Number(value) || 0
  if (n >= 100000000) return `${(n / 100000000).toFixed(1)}亿`
  if (n >= 10000) return `${(n / 10000).toFixed(1)}万`
  return String(Math.round(n))
}

const kpiCards = computed(() => {
  if (!report.value) return []
  const changes = report.value.changes || {}
  return [
    { key: 'peak', label: '峰值在线', value: report.value.peakOnline, formatter: formatNumber, change: changes.peakOnline || 0, note: '同时在线人数峰值' },
    { key: 'stay', label: '平均停留', value: report.value.avgStaySeconds, formatter: formatDuration, change: changes.avgStay || 0, note: '观众平均停留时长' },
    { key: 'interaction', label: '互动率', value: report.value.interactionRate, formatter: (v) => `${Number(v).toFixed(1)}%`, change: changes.interactionRate || 0, note: '弹幕、点赞与礼物互动' },
    { key: 'gift', label: '礼物收益', value: report.value.giftIncome, formatter: formatNumber, change: changes.giftIncome || 0, note: '本场礼物折算酷币' }
  ]
})

const chartPoints = computed(() => {
  const series = report.value?.onlineSeries || []
  if (!series.length) return []
  const max = Math.max(...series.map((item) => item.value), 1)
  const plotWidth = 760 - chartPad.left - chartPad.right
  const plotHeight = chartHeight - chartPad.top - chartPad.bottom
  return series.map((item, index) => ({
    ...item,
    x: chartPad.left + (series.length === 1 ? 0 : (plotWidth * index) / (series.length - 1)),
    y: chartPad.top + plotHeight - (item.value / max) * plotHeight
  }))
})

const chartLinePoints = computed(() => chartPoints.value.map((point) => `${point.x},${point.y}`).join(' '))
const linePoints = computed(() => chartLinePoints.value)
const areaPoints = computed(() => {
  if (!chartPoints.value.length) return ''
  const bottom = chartHeight - chartPad.bottom
  return `${chartPad.left},${bottom} ${chartLinePoints.value} ${760 - chartPad.right},${bottom}`
})

const chartGrid = computed(() => {
  const max = Math.max(...(report.value?.onlineSeries || []).map((item) => item.value), 1)
  const roundedMax = Math.max(50, Math.ceil((max * 1.15) / 50) * 50)
  return [0, 0.25, 0.5, 0.75, 1].map((ratio) => {
    const value = Math.round(roundedMax * (1 - ratio))
    return {
      value,
      label: formatNumber(value),
      top: `${(chartPad.top + (chartHeight - chartPad.top - chartPad.bottom) * ratio) / chartHeight * 100}%`,
      y: chartPad.top + (chartHeight - chartPad.top - chartPad.bottom) * ratio
    }
  })
})

const chartXLabels = computed(() => {
  const points = chartPoints.value
  if (points.length <= 6) return points
  return points.filter((_, index) => index === 0 || index === points.length - 1 || index % 2 === 0)
})

const chartVGrid = computed(() => {
  const count = 6
  const plotWidth = 760 - chartPad.left - chartPad.right
  return Array.from({ length: count }, (_, index) => chartPad.left + (plotWidth * index) / (count - 1))
})

const hoverPoint = computed(() => (hoverIndex.value >= 0 ? chartPoints.value[hoverIndex.value] : null))
const tooltipStyle = computed(() => {
  if (!hoverPoint.value) return {}
  const left = Math.min(88, Math.max(8, (hoverPoint.value.x / 760) * 100))
  const top = Math.max(10, (hoverPoint.value.y / chartHeight) * 100)
  return { left: `${left}%`, top: `${top}%` }
})

function onChartMove(event) {
  const svg = chartSvg.value
  if (!svg || !chartPoints.value.length) return
  const rect = svg.getBoundingClientRect()
  const x = ((event.clientX - rect.left) / rect.width) * 760
  let nearest = 0
  let distance = Infinity
  chartPoints.value.forEach((point, index) => {
    const current = Math.abs(point.x - x)
    if (current < distance) {
      distance = current
      nearest = index
    }
  })
  hoverIndex.value = nearest
}

const audienceGroups = computed(() => {
  const audience = report.value?.audience || {}
  return [
    { label: '性别分布', items: audience.gender || [] },
    { label: '年龄分布', items: audience.age || [] },
    { label: '地区分布', items: audience.region || [] }
  ]
})

function giftBarHeight(value) {
  const max = Math.max(...(report.value?.giftMinutes || []).map((item) => item.value), 1)
  return Math.max(3, (value / max) * 100)
}

function replay() {
  if (!report.value?.roomId) return
  router.push(`/studio/live/${report.value.roomId}/prepare`)
}

function exportReport() {
  if (!report.value) return
  const blob = new Blob([JSON.stringify(report.value, null, 2)], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `kumao-live-report-${report.value.sessionId}.json`
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}

onMounted(() => {
  const sessionId = String(route.params.sessionId || '')
  report.value = loadLiveReport(sessionId) || fallbackLiveReport(sessionId)
})
</script>

<style scoped>
.report-page { width: 100%; max-width: 1480px; margin: 0 auto; padding: 0 0 42px; color: var(--km-text); }
.report-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; flex-wrap: wrap; padding: 4px 2px 20px; border-bottom: 1px solid var(--km-border); margin-bottom: 18px; }
.report-title-group { display: flex; align-items: flex-start; gap: 15px; min-width: 0; }
.report-back { display: inline-flex; align-items: center; gap: 4px; padding-top: 5px; color: var(--km-text-faint); font-size: 11.5px; white-space: nowrap; }
.report-back:hover { color: var(--km-text); }
.report-divider { width: 1px; height: 40px; margin-top: 2px; background: var(--km-border); }
.eyebrow { display: block; color: var(--km-accent); font-size: 9px; font-weight: 800; letter-spacing: .17em; margin-bottom: 5px; }
.report-head h1 { margin: 0 0 7px; font-size: 25px; font-weight: 900; letter-spacing: -.045em; }
.report-meta { display: flex; flex-wrap: wrap; gap: 8px 16px; color: var(--km-text-faint); font-size: 11px; }
.report-meta span + span::before { content: '·'; margin-right: 16px; color: var(--km-border-strong); }
.report-actions { display: flex; align-items: center; gap: 8px; flex: none; }
.ghost-action, .primary-action { display: inline-flex; align-items: center; gap: 6px; height: 36px; padding: 0 14px; border-radius: 8px; font: inherit; font-size: 11.5px; font-weight: 700; cursor: pointer; }
.ghost-action { border: 1px solid var(--km-border); background: transparent; color: var(--km-text-dim); }
.ghost-action:hover { color: var(--km-text); border-color: var(--km-border-strong); }
.primary-action { border: 0; background: var(--km-accent); color: #120c19; }
.primary-action:hover { background: #e9d5ff; }

.kpi-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 18px; }
.kpi-card { position: relative; display: flex; flex-direction: column; min-height: 130px; padding: 16px 17px; border: 1px solid var(--km-border); border-radius: 13px; background: linear-gradient(145deg, #18131f, #0f0c14 74%); overflow: hidden; }
.kpi-card::after { content: ''; position: absolute; right: -34px; bottom: -58px; width: 150px; height: 150px; border-radius: 50%; background: radial-gradient(circle, rgba(216,180,254,.09), transparent 68%); pointer-events: none; }
.kpi-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.kpi-label { color: var(--km-text-faint); font-size: 11px; }
.kpi-trend { display: inline-flex; align-items: center; gap: 2px; font-size: 10px; font-weight: 750; }
.kpi-trend b { font-size: 13px; line-height: 1; }
.kpi-trend.up { color: #4ade80; }
.kpi-trend.down { color: #fb7185; }
.kpi-trend.flat { color: var(--km-text-faint); }
.kpi-value { margin: auto 0 4px; color: var(--km-text); font-size: 30px; font-weight: 900; letter-spacing: -.06em; line-height: 1.05; }
.kpi-note { color: var(--km-text-faint); font-size: 9.5px; }

.report-grid { display: grid; grid-template-columns: minmax(0, 1.45fr) minmax(280px, .75fr); gap: 14px; }
.report-card { padding: 17px 18px; border: 1px solid var(--km-border); border-radius: 14px; background: #0f0c14; }
.chart-card { grid-column: 1 / 2; }
.gift-chart-card { grid-column: 2 / 3; }
.audience-card { grid-column: 1 / 2; }
.settlement-card { grid-column: 2 / 3; }
.card-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 15px; }
.card-head h2 { margin: 0; font-size: 14px; font-weight: 800; letter-spacing: -.02em; }
.card-caption { padding-top: 4px; color: var(--km-text-faint); font-size: 10px; white-space: nowrap; }

/* 在线曲线 */
.chart-wrap { position: relative; height: 254px; margin-top: 2px; }
.line-chart { width: 100%; height: 220px; overflow: visible; }
.chart-grid line { stroke: rgba(255,255,255,.07); stroke-width: 1; }
.chart-grid line:first-child { stroke: rgba(255,255,255,.11); }
.hover-line { stroke: rgba(216,180,254,.5); stroke-width: 1; stroke-dasharray: 3 4; }
.hover-dot { fill: #f0abfc; stroke: #22132e; stroke-width: 3; }
.chart-y-axis { position: absolute; top: 18px; bottom: 64px; left: 0; width: 28px; pointer-events: none; }
.chart-y-axis span { position: absolute; left: 0; transform: translateY(-50%); color: var(--km-text-faint); font-size: 8.5px; }
.chart-x-axis { position: absolute; left: 0; right: 0; top: 225px; height: 18px; pointer-events: none; }
.chart-x-axis span { position: absolute; transform: translateX(-50%); color: var(--km-text-faint); font-size: 8.5px; white-space: nowrap; }
.chart-tooltip { position: absolute; z-index: 2; min-width: 92px; padding: 7px 9px; border: 1px solid rgba(216,180,254,.22); border-radius: 6px; background: #1a1222; box-shadow: 0 8px 24px rgba(0,0,0,.35); pointer-events: none; transform: translate(-50%, calc(-100% - 8px)); }
.chart-tooltip span { display: block; color: var(--km-text-faint); font-size: 9px; margin-bottom: 3px; }
.chart-tooltip strong { color: var(--km-text); font-size: 13px; }
.chart-tooltip small { color: var(--km-text-faint); font-size: 9px; font-weight: 400; }

/* 礼物柱状 */
.gift-bars { display: flex; align-items: flex-end; gap: 5px; height: 203px; padding: 18px 2px 20px; border-bottom: 1px solid rgba(255,255,255,.07); }
.gift-bar-col { position: relative; display: flex; flex: 1; height: 100%; flex-direction: column; align-items: center; justify-content: flex-end; min-width: 0; }
.gift-bar-value { margin-bottom: 4px; color: var(--km-text-faint); font-size: 8px; opacity: 0; transition: opacity .15s; }
.gift-bar-col:hover .gift-bar-value { opacity: 1; }
.gift-bar { display: block; width: 100%; max-width: 16px; min-height: 4px; border-radius: 3px 3px 0 0; background: linear-gradient(180deg, #f0abfc, #8b5cf6); box-shadow: 0 0 12px rgba(216,180,254,.12); transition: filter .15s; }
.gift-bar-col:hover .gift-bar { filter: brightness(1.3); }
.gift-bar-label { position: absolute; bottom: -16px; color: var(--km-text-faint); font-size: 7.5px; white-space: nowrap; }

/* 观众画像 */
.audience-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 20px; }
.audience-group h3 { margin: 0 0 11px; color: var(--km-text-dim); font-size: 10.5px; font-weight: 650; }
.audience-row { display: flex; align-items: center; gap: 7px; margin-bottom: 9px; }
.audience-label { width: 35px; flex: none; color: var(--km-text-faint); font-size: 9.5px; }
.audience-track { flex: 1; height: 6px; overflow: hidden; border-radius: 999px; background: rgba(255,255,255,.07); }
.audience-track i { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #8b5cf6, #f0abfc); }
.audience-row b { width: 31px; flex: none; color: var(--km-text-dim); text-align: right; font-size: 9.5px; font-weight: 500; }

/* 结算 */
.settlement-card { display: flex; flex-direction: column; background: linear-gradient(155deg, rgba(247,215,116,.06), #0f0c14 50%); }
.settlement-badge { display: inline-flex; align-items: center; height: 22px; padding: 0 8px; border-radius: 5px; font-size: 9.5px; font-weight: 700; }
.settlement-badge.pending { background: rgba(247,215,116,.11); color: #e8ca70; }
.settlement-badge.settled { background: rgba(74,222,128,.1); color: #86efac; }
.settlement-amount { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; margin-top: 7px; padding-bottom: 12px; border-bottom: 1px solid rgba(255,255,255,.06); }
.settlement-amount span { color: var(--km-text-faint); font-size: 10px; }
.settlement-amount strong { color: #f7d774; font-size: 26px; letter-spacing: -.04em; }
.settlement-amount small { color: var(--km-text-faint); font-size: 9px; font-weight: 400; }
.settlement-card p { flex: 1; margin: 13px 0 12px; color: var(--km-text-faint); font-size: 10.5px; line-height: 1.7; }
.settlement-link { display: inline-flex; align-items: center; gap: 3px; color: var(--km-accent); font-size: 10.5px; font-weight: 650; }
.settlement-link:hover { color: #e9d5ff; }

.report-footer { display: flex; align-items: center; justify-content: space-between; gap: 18px; margin-top: 18px; padding: 22px 24px; border: 1px solid rgba(216,180,254,.16); border-radius: 14px; background: radial-gradient(600px 160px at 10% 0%, rgba(216,180,254,.09), transparent 65%), #100c16; }
.report-footer h2 { margin: 0 0 4px; font-size: 16px; letter-spacing: -.025em; }
.report-footer p { margin: 0; color: var(--km-text-faint); font-size: 10.5px; }

@media (max-width: 1100px) {
  .report-grid { grid-template-columns: 1fr; }
  .chart-card, .gift-chart-card, .audience-card, .settlement-card { grid-column: auto; }
  .kpi-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 700px) {
  .report-title-group { flex-wrap: wrap; }
  .report-divider { display: none; }
  .report-head h1 { font-size: 21px; }
  .report-actions { width: 100%; }
  .kpi-grid { grid-template-columns: 1fr 1fr; }
  .kpi-card { min-height: 116px; }
  .audience-grid { grid-template-columns: 1fr; gap: 14px; }
  .report-footer { align-items: flex-start; flex-direction: column; }
}
</style>
