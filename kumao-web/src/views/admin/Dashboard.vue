<template>
  <div class="dash">
    <header class="dash-head">
      <div class="dash-heading">
        <h1 class="dash-title">数据看板</h1>
        <p class="dash-sub">{{ today }} · 平台关键指标与服务连接状态</p>
      </div>
      <div class="dash-actions">
        <span :class="['health-state', { bad: downCount > 0 }]">
          <i aria-hidden="true"></i>
          {{ downCount > 0 ? `${downCount} 个服务异常` : '服务连接正常' }}
        </span>
        <button class="dash-refresh" :disabled="loading" @click="loadAll">
          <el-icon><Refresh /></el-icon>
          {{ loading ? '刷新中' : '刷新数据' }}
        </button>
      </div>
    </header>

    <div v-if="loading" class="dash-skeleton" aria-label="看板数据加载中">
      <div class="sk-kpis">
        <div v-for="i in 4" :key="i" class="sk-kpi"></div>
      </div>
      <div class="sk-panels">
        <div class="sk-panel"></div>
        <div class="sk-panel"></div>
      </div>
    </div>

    <template v-else>
      <div v-if="failedCount" class="dash-notice" role="alert">
        <span>{{ failedCount }} 个统计接口暂不可用，以下数字可能不完整。</span>
        <button @click="loadAll">重新加载</button>
      </div>

      <section class="kpi-strip" aria-label="核心指标">
        <div v-for="c in cards" :key="c.label" class="kpi-cell">
          <span class="kpi-label">{{ c.label }}</span>
          <span class="kpi-value tabular">{{ formatNum(c.value) }}</span>
          <span class="kpi-note">{{ c.meta }}</span>
        </div>
      </section>

      <div class="dash-grid">
        <section class="panel panel-roles" aria-labelledby="roles-title">
          <div class="panel-head">
            <div>
              <h2 id="roles-title" class="panel-title">账号角色覆盖</h2>
              <p class="panel-sub">角色可重叠，按覆盖人数排列</p>
            </div>
            <span class="panel-meta">共 {{ formatNum(users.total || 0) }} 个账号</span>
          </div>
          <div class="role-list">
            <div v-for="row in roleRows" :key="row.label" class="role-row">
              <div class="role-row-head">
                <span class="role-label">{{ row.label }}</span>
                <span class="role-value tabular">{{ formatNum(row.value) }}</span>
              </div>
              <div class="role-track" aria-hidden="true">
                <span :style="{ width: `${row.width}%` }"></span>
              </div>
            </div>
            <div v-if="!roleRows.length" class="panel-empty">暂无账号数据</div>
          </div>
          <p class="role-caption">同一账号可同时拥有会员、主播与音乐人身份，条形按覆盖人数缩放。</p>
          <div class="panel-foot">
            <span>数据来自用户服务统计接口</span>
            <router-link to="/admin/users">查看用户 <el-icon><ArrowRight /></el-icon></router-link>
          </div>
        </section>

        <section class="panel panel-live" aria-labelledby="live-title">
          <div class="panel-head">
            <div>
              <h2 id="live-title" class="panel-title">直播概况</h2>
              <p class="panel-sub">房间状态与当前在线观众</p>
            </div>
            <router-link to="/admin/lives" class="panel-link">管理直播 <el-icon><ArrowRight /></el-icon></router-link>
          </div>
          <div class="stat-grid">
            <div class="stat-cell">
              <span class="stat-label">直播间总数</span>
              <span class="stat-value tabular">{{ formatNum(live.total || 0) }}</span>
            </div>
            <div class="stat-cell">
              <span class="stat-label">正在直播</span>
              <span class="stat-value tabular accent">{{ formatNum(live.living || 0) }}</span>
            </div>
            <div class="stat-cell">
              <span class="stat-label">当前在线观众</span>
              <span class="stat-value tabular">{{ formatNum(live.online || 0) }}</span>
            </div>
            <div class="stat-cell">
              <span class="stat-label">违规封禁</span>
              <span class="stat-value tabular">{{ formatNum(live.banned || 0) }}</span>
            </div>
          </div>
          <div class="live-note">
            <span class="live-note-dot" aria-hidden="true"></span>
            {{ Number(live.living) > 0 ? `${live.living} 个直播间正在产生内容` : '当前没有正在直播的房间' }}
          </div>
        </section>

        <section class="panel panel-revenue" aria-labelledby="revenue-title">
          <div class="panel-head">
            <div>
              <h2 id="revenue-title" class="panel-title">收益与结算</h2>
              <p class="panel-sub">会员、单曲、直播礼物、广告与音乐人收入汇总</p>
            </div>
            <span class="panel-meta">元</span>
          </div>
          <div class="kv-list">
            <div v-for="item in revenue.items" :key="item.key" class="kv-row">
              <span>{{ item.label }}</span>
              <b class="tabular" :class="{ cost: item.type === 'COST' }">{{ item.type === 'COST' ? '-' : '' }}¥{{ formatMoney(item.amount) }}</b>
            </div>
            <div class="kv-row total-row">
              <span>合计</span>
              <b class="tabular">¥{{ formatMoney(revenue.total) }}</b>
            </div>
            <div class="kv-row muted"><span>待审核提现</span><b class="tabular">{{ formatNum(musician.pendingWithdraw || 0) }} 笔</b></div>
          </div>
          <div class="panel-foot">
            <span>各业务统计接口汇总 · {{ revenue.updatedAt || '--' }}</span>
            <router-link to="/admin/musicians">查看结算 <el-icon><ArrowRight /></el-icon></router-link>
          </div>
        </section>

        <section class="panel panel-vip" aria-labelledby="vip-title">
          <div class="panel-head">
            <div>
              <h2 id="vip-title" class="panel-title">会员概况</h2>
              <p class="panel-sub">当前会员状态与续费</p>
            </div>
            <router-link to="/admin/vip" class="panel-link">管理会员 <el-icon><ArrowRight /></el-icon></router-link>
          </div>
          <div class="stat-grid">
            <div class="stat-cell">
              <span class="stat-label">会员总数</span>
              <span class="stat-value tabular">{{ formatNum(vip.totalMember || 0) }}</span>
            </div>
            <div class="stat-cell">
              <span class="stat-label">有效会员</span>
              <span class="stat-value tabular accent">{{ formatNum(vip.activeMember || 0) }}</span>
            </div>
            <div class="stat-cell">
              <span class="stat-label">会员收入</span>
              <span class="stat-value tabular">¥{{ formatMoney(vip.revenue) }}</span>
            </div>
            <div class="stat-cell">
              <span class="stat-label">续费率</span>
              <span class="stat-value tabular">{{ vip.renewRate || '—' }}</span>
            </div>
          </div>
        </section>
      </div>

      <section class="panel service-panel" aria-labelledby="service-title">
        <div class="panel-head">
          <div>
            <h2 id="service-title" class="panel-title">服务连接状态</h2>
            <p class="panel-sub">本次看板加载时，各业务统计接口的响应情况</p>
          </div>
          <span class="panel-meta">{{ services.length - downCount }}/{{ services.length }} 正常</span>
        </div>
        <div class="service-list">
          <div v-for="s in services" :key="s.name" :class="['service-item', { down: !s.up }]">
            <span class="service-dot" aria-hidden="true"></span>
            <span class="service-name">{{ s.name }}</span>
            <span class="service-state">{{ s.up ? '正常' : '异常' }}</span>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { userApi, liveApi, vipApi, musicianApi, adApi } from '@/api'
import request from '@/api/request'

const users = ref({})
const live = ref({})
const vip = ref({})
const musician = ref({})
const ad = ref({})
const revenue = ref({ total: 0, items: [] })
const loading = ref(true)
const failedCount = ref(0)
const updatedAt = ref('')
const services = ref([
  { name: '用户服务', up: true },
  { name: '直播服务', up: true },
  { name: '会员服务', up: true },
  { name: '音乐人服务', up: true },
  { name: '广告服务', up: true }
])

const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
const downCount = computed(() => services.value.filter((s) => !s.up).length)

const cards = computed(() => [
  { label: '注册用户', value: users.value.total || 0, meta: '平台累计账号' },
  { label: '今日登录', value: users.value.todayLogin || 0, meta: '今日有登录记录' },
  { label: '主播人数', value: users.value.anchor || 0, meta: '已认证主播' },
  { label: '音乐人数', value: users.value.musician || 0, meta: '已入驻音乐人' }
])

const roleRows = computed(() => {
  const rows = [
    { label: '会员', value: Number(users.value.vip) || 0 },
    { label: '主播', value: Number(users.value.anchor) || 0 },
    { label: '音乐人', value: Number(users.value.musician) || 0 }
  ]
  const max = Math.max(...rows.map((r) => r.value), 1)
  return rows.map((r) => ({ ...r, width: Math.round((r.value / max) * 100) }))
})

function formatNum(n) {
  n = Number(n) || 0
  if (n >= 100000000) return (n / 100000000).toFixed(1) + '亿'
  return n >= 10000 ? (n / 10000).toFixed(1) + '万' : String(n)
}

function formatMoney(n) {
  return new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(n) || 0)
}

async function loadAll() {
  loading.value = true
  const requests = [
    userApi.statistics(),
    liveApi.statistics(),
    vipApi.statistics(),
    musicianApi.adminIncome(),
    adApi.adminRevenue(),
    request.get('/music/admin/revenue/summary')
  ]
  const results = await Promise.allSettled(requests)
  if (results[0].status === 'fulfilled') users.value = results[0].value.data || {}
  if (results[1].status === 'fulfilled') live.value = results[1].value.data || {}
  if (results[2].status === 'fulfilled') vip.value = results[2].value.data || {}
  if (results[3].status === 'fulfilled') musician.value = results[3].value.data || {}
  if (results[4].status === 'fulfilled') ad.value = results[4].value.data || {}
  if (results[5].status === 'fulfilled') revenue.value = results[5].value.data || { total: 0, items: [] }
  services.value = services.value.map((s, i) => ({ ...s, up: results[i]?.status === 'fulfilled' }))
  failedCount.value = results.filter((r) => r.status === 'rejected').length
  updatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  loading.value = false
}

onMounted(loadAll)
</script>

<style scoped>
.dash { display: flex; flex-direction: column; gap: 16px; }

.dash-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; }
.dash-title { margin: 0; font-size: 26px; line-height: 1.25; font-weight: 700; letter-spacing: -.04em; color: var(--km-text); }
.dash-sub { margin: 7px 0 0; font-size: 12px; color: var(--km-text-faint); }
.dash-actions { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.health-state { display: inline-flex; align-items: center; gap: 6px; height: 30px; padding: 0 9px; border-radius: 7px; background: rgba(74, 222, 128, .06); border: 1px solid rgba(74, 222, 128, .12); color: #86efac; font-size: 11px; white-space: nowrap; }
.health-state i { width: 5px; height: 5px; border-radius: 50%; background: #4ade80; }
.health-state.bad { background: rgba(251, 113, 133, .07); border-color: rgba(251, 113, 133, .16); color: #fecdd3; }
.health-state.bad i { background: #fb7185; }
.dash-refresh { display: inline-flex; align-items: center; gap: 5px; height: 30px; padding: 0 10px; border: 1px solid var(--km-border); border-radius: 7px; background: transparent; color: var(--km-text-dim); font-size: 11px; cursor: pointer; transition: border-color .16s, color .16s; }
.dash-refresh:hover:not(:disabled) { color: var(--km-text); border-color: var(--km-border-strong); }
.dash-refresh:disabled { opacity: .5; cursor: wait; }

.dash-notice { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 9px 11px; border-radius: 8px; background: rgba(251, 191, 36, .06); border: 1px solid rgba(251, 191, 36, .14); color: #fde68a; font-size: 11px; }
.dash-notice button { border: 0; background: transparent; color: #fde68a; font-size: 11px; font-weight: 600; text-decoration: underline; cursor: pointer; }

.kpi-strip { display: grid; grid-template-columns: repeat(4, 1fr); border: 1px solid var(--km-border); border-radius: 16px; background: var(--km-card); overflow: hidden; }
.kpi-cell { padding: 16px 18px; display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.kpi-cell + .kpi-cell { border-left: 1px solid var(--km-border); }
.kpi-label { font-size: 11px; color: var(--km-text-faint); }
.kpi-value { font-size: 28px; line-height: 1.15; font-weight: 600; letter-spacing: -.04em; color: var(--km-text); font-variant-numeric: tabular-nums; }
.kpi-note { font-size: 11px; color: var(--km-text-faint); }

.dash-grid { display: grid; grid-template-columns: minmax(0, 1.08fr) minmax(0, .92fr); gap: 16px; }
.panel { display: flex; flex-direction: column; padding: 16px; border: 1px solid var(--km-border); border-radius: 16px; background: var(--km-card); min-width: 0; }
.panel-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.panel-title { margin: 0; font-size: 14px; font-weight: 600; color: var(--km-text); }
.panel-sub { margin: 4px 0 0; font-size: 11px; color: var(--km-text-faint); line-height: 1.5; }
.panel-meta { font-size: 11px; color: var(--km-text-faint); white-space: nowrap; padding-top: 2px; }
.panel-link { display: inline-flex; align-items: center; gap: 2px; font-size: 11px; color: var(--km-text-faint); white-space: nowrap; }
.panel-link:hover { color: var(--km-text); }

.role-list { display: flex; flex-direction: column; gap: 10px; }
.role-row { display: flex; flex-direction: column; gap: 5px; }
.role-row-head { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; }
.role-label { font-size: 11px; color: var(--km-text-dim); }
.role-value { font-size: 12px; color: var(--km-text); font-variant-numeric: tabular-nums; }
.role-track { height: 5px; border-radius: 3px; background: rgba(255, 255, 255, .06); overflow: hidden; }
.role-track span { display: block; height: 100%; border-radius: inherit; background: var(--km-accent); min-width: 2px; }
.role-row:nth-child(2) .role-track span { background: #c084fc; /* taste-skill-ignore: brand data hue */ }
.role-row:nth-child(3) .role-track span { background: #a78bfa; /* taste-skill-ignore: brand data hue */ }
.panel-foot { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: auto; padding-top: 12px; color: var(--km-text-faint); font-size: 11px; }
.panel-foot a { display: inline-flex; align-items: center; gap: 2px; color: var(--km-text-dim); font-size: 11px; }
.panel-foot a:hover { color: var(--km-text); }
.panel-empty { padding: 15px 0; color: var(--km-text-faint); font-size: 11px; }

.stat-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; border: 1px solid var(--km-border); border-radius: 10px; overflow: hidden; background: var(--km-border); }
.stat-cell { padding: 11px 12px; background: #0c0a11; display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.stat-label { font-size: 11px; color: var(--km-text-faint); }
.stat-value { font-size: 19px; line-height: 1.2; font-weight: 600; color: var(--km-text); font-variant-numeric: tabular-nums; letter-spacing: -.02em; }
.stat-value.accent { color: #e9d5ff; }
.live-note { display: flex; align-items: center; gap: 6px; margin-top: 11px; color: var(--km-text-faint); font-size: 11px; }
.live-note-dot { width: 5px; height: 5px; border-radius: 50%; background: #4ade80; flex-shrink: 0; }

.kv-list { display: flex; flex-direction: column; }
.kv-row { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; min-height: 30px; border-bottom: 1px solid rgba(255, 255, 255, .045); font-size: 11px; color: var(--km-text-dim); }
.kv-row:last-child { border-bottom: 0; }
.kv-row b { color: var(--km-text); font-size: 12px; font-weight: 600; font-variant-numeric: tabular-nums; }
.kv-row.total-row { border-top: 1px solid var(--km-border); border-bottom: 0; margin-top: 4px; padding-top: 8px; color: var(--km-text); }
.kv-row.total-row b { color: var(--km-accent); font-size: 13px; }
.kv-row b.cost { color: #ff9fb0; }
.kv-row.muted b { color: var(--km-text-faint); }

.service-panel { padding-bottom: 13px; }
.service-list { display: grid; grid-template-columns: repeat(5, 1fr); gap: 7px; }
.service-item { display: flex; align-items: center; gap: 6px; min-height: 32px; padding: 0 9px; border-radius: 7px; background: rgba(255, 255, 255, .025); border: 1px solid var(--km-border); min-width: 0; }
.service-dot { width: 5px; height: 5px; border-radius: 50%; background: #4ade80; flex-shrink: 0; }
.service-name { font-size: 11px; color: var(--km-text-dim); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.service-state { margin-left: auto; font-size: 11px; color: #86efac; flex-shrink: 0; }
.service-item.down { border-color: rgba(251, 113, 133, .16); background: rgba(251, 113, 133, .05); }
.service-item.down .service-dot { background: #fb7185; }
.service-item.down .service-state { color: #fecdd3; }

.dash-skeleton { display: flex; flex-direction: column; gap: 16px; }
.sk-kpis { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1px; border: 1px solid var(--km-border); border-radius: 16px; overflow: hidden; background: var(--km-border); }
.sk-kpi { height: 96px; background: #0c0a11; }
.sk-panels { display: grid; grid-template-columns: 1.08fr .92fr; gap: 16px; }
.sk-panel { height: 250px; border: 1px solid var(--km-border); border-radius: 16px; background: #0f0d14; }

.tabular { font-variant-numeric: tabular-nums; }

/* ---- 可读性层级 ---- */
.kpi-label, .kpi-note, .panel-sub, .panel-meta, .panel-link, .stat-label,
.service-name, .live-note, .role-label, .role-value, .kv-row, .kv-row b { font-size: 13px; }
.panel-foot, .panel-foot a, .service-state { font-size: 12px; }
.role-list { gap: 12px; }
.role-track span { background: #a855f7; /* taste-skill-ignore: brand data hue */ }
.role-row:nth-child(2) .role-track span,
.role-row:nth-child(3) .role-track span { background: #a855f7; /* taste-skill-ignore: brand data hue */ }
.role-caption { margin: 4px 0 0; color: var(--km-text-faint); font-size: 12px; line-height: 1.55; }
.side-version { color: var(--km-text-faint); }

@media (max-width: 1180px) {
  .dash-title { font-size: 23px; }
  .dash-grid { grid-template-columns: 1fr; }
  .service-list { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 720px) {
  .dash-head { align-items: flex-start; flex-direction: column; gap: 12px; }
  .dash-title { font-size: 21px; }
  .dash-actions { width: 100%; justify-content: space-between; }
  .kpi-strip { grid-template-columns: 1fr 1fr; }
  .kpi-cell + .kpi-cell { border-left: 0; }
  .kpi-cell:nth-child(even) { border-left: 1px solid var(--km-border); }
  .kpi-cell:nth-child(n + 3) { border-top: 1px solid var(--km-border); }
  .kpi-cell { padding: 13px; }
  .kpi-value { font-size: 24px; }
  .service-list { grid-template-columns: 1fr 1fr; }
  .sk-kpis { grid-template-columns: 1fr 1fr; }
  .sk-kpi:nth-child(n+3) { border-top: 1px solid var(--km-border); }
  .sk-panels { grid-template-columns: 1fr; }
}
</style>
