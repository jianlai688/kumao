<template>
  <div class="earnings-page">
    <header class="earnings-head">
      <div>
        <router-link to="/studio/anchor" class="back-link">
          <el-icon><ArrowLeft /></el-icon>主播工作台
        </router-link>
        <span class="eyebrow">ANCHOR INCOME · LIVE GIFTS</span>
        <h1>礼物收益</h1>
        <p>直播礼物折算、结算进度与提现入口，都在这里。</p>
      </div>
      <div class="head-note">
        <span>结算规则</span>
        <b>T+1 自动结算</b>
        <small>结算后可在主播资料绑定收款账户</small>
      </div>
    </header>

    <section class="summary-grid" aria-label="礼物收益概览">
      <article v-for="item in summaryCards" :key="item.key" class="summary-card">
        <span class="summary-icon" :class="item.key">
          <el-icon><component :is="item.icon" /></el-icon>
        </span>
        <span class="summary-label">{{ item.label }}</span>
        <strong class="summary-value">
          <AnimatedNumber :value="item.value" :formatter="formatNumber" />
          <small>酷币</small>
        </strong>
        <span class="summary-note">{{ item.note }}</span>
      </article>

      <article class="summary-card wallet-card">
        <span class="summary-icon wallet">
          <el-icon><Wallet /></el-icon>
        </span>
        <span class="summary-label">可提现余额</span>
        <strong class="summary-value">
          <AnimatedNumber :value="withdrawable" :formatter="formatNumber" />
          <small>酷币</small>
        </strong>
        <button type="button" class="withdraw-btn" @click="openWithdraw">提现</button>
        <span class="summary-note">仅已结算收益可提现</span>
      </article>
    </section>

    <section class="detail-card">
      <div class="detail-head">
        <div>
          <span class="eyebrow">INCOME LEDGER</span>
          <h2>收益明细</h2>
        </div>
        <div class="detail-tools">
          <span class="record-total">共 {{ filteredRecords.length }} 条</span>
          <el-select v-model="statusFilter" class="status-select" size="small" @change="page = 1">
            <el-option label="全部状态" value="ALL" />
            <el-option label="待结算" value="PENDING" />
            <el-option label="已结算" value="SETTLED" />
          </el-select>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="pagedRecords"
        class="earnings-table"
        empty-text="暂无收益明细"
      >
        <el-table-column label="时间" width="155">
          <template #default="{ row }">
            <span class="cell-muted">{{ formatDateTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="来源直播间" min-width="190">
          <template #default="{ row }">
            <div class="room-cell">
              <span class="room-dot"></span>
              <span>{{ row.roomTitle }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="礼物" width="140">
          <template #default="{ row }">
            <span class="gift-name"><i>{{ row.giftIcon || '🎁' }}</i>{{ row.giftName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="80" align="right">
          <template #default="{ row }">×{{ row.count }}</template>
        </el-table-column>
        <el-table-column label="折算酷币" width="126" align="right">
          <template #default="{ row }">
            <strong class="coin-value">{{ formatNumber(row.totalPrice) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="112" align="right">
          <template #default="{ row }">
            <span :class="['status-chip', row.status === 'SETTLED' ? 'settled' : 'pending']">
              {{ row.status === 'SETTLED' ? '已结算' : '待结算' }}
            </span>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <span>每页 10 条 · 仅展示当前主播收到的礼物</span>
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="filteredRecords.length"
          layout="prev, pager, next"
          background
          small
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Calendar, Money, TrendCharts, Wallet } from '@element-plus/icons-vue'
import { liveApi } from '@/api'
import AnimatedNumber from '@/components/studio/AnimatedNumber.vue'
import { formatDateTime } from '@/utils/liveSession'

const router = useRouter()
const loading = ref(true)
const records = ref([])
const statusFilter = ref('ALL')
const page = ref(1)
const pageSize = 10
const today = new Date()
today.setHours(0, 0, 0, 0)

function formatNumber(value) {
  const n = Number(value) || 0
  if (n >= 100000000) return `${(n / 100000000).toFixed(1)}亿`
  if (n >= 10000) return `${(n / 10000).toFixed(1)}万`
  return String(Math.round(n))
}

function isSameDay(value) {
  const date = new Date(value)
  const now = new Date()
  return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth() && date.getDate() === now.getDate()
}

function isSameMonth(value) {
  const date = new Date(value)
  const now = new Date()
  return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth()
}

const summary = computed(() => {
  const todayTotal = records.value.filter((item) => isSameDay(item.createTime)).reduce((sum, item) => sum + item.totalPrice, 0)
  const monthTotal = records.value.filter((item) => isSameMonth(item.createTime)).reduce((sum, item) => sum + item.totalPrice, 0)
  const total = records.value.reduce((sum, item) => sum + item.totalPrice, 0)
  return { todayTotal, monthTotal, total }
})

const withdrawable = computed(() => records.value
  .filter((item) => item.status === 'SETTLED')
  .reduce((sum, item) => sum + item.totalPrice, 0))

const summaryCards = computed(() => [
  { key: 'today', label: '今日收益', value: summary.value.todayTotal, icon: Calendar, note: '今日收到的礼物折算' },
  { key: 'month', label: '本月收益', value: summary.value.monthTotal, icon: TrendCharts, note: '本自然月累计' },
  { key: 'total', label: '累计收益', value: summary.value.total, icon: Money, note: '全部直播礼物合计' }
])

const filteredRecords = computed(() => {
  if (statusFilter.value === 'ALL') return records.value
  return records.value.filter((item) => item.status === statusFilter.value)
})

const pagedRecords = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredRecords.value.slice(start, start + pageSize)
})

async function loadData() {
  loading.value = true
  try {
    const [giftRes, roomRes] = await Promise.all([
      liveApi.received({ pageNum: 1, pageSize: 500 }),
      liveApi.mine({ pageNum: 1, pageSize: 50 })
    ])
    const roomMap = new Map((roomRes.data?.records || []).map((room) => [String(room.id), room.title]))
    records.value = (giftRes.data?.records || [])
      .map((item) => ({
        ...item,
        roomTitle: roomMap.get(String(item.roomId)) || `直播间 #${item.roomId}`,
        totalPrice: Number(item.totalPrice) || 0,
        status: new Date(item.createTime) < today ? 'SETTLED' : 'PENDING'
      }))
      .sort((a, b) => String(b.createTime).localeCompare(String(a.createTime)))
  } catch (e) {
    ElMessage.error(e?.message || '收益明细加载失败')
  } finally {
    loading.value = false
  }
}

async function openWithdraw() {
  try {
    await ElMessageBox.confirm(
      '提现功能需绑定收款账户，支付宝，微信。\n请先前往主播资料完成收款账户绑定。',
      '提现提示',
      { confirmButtonText: '去绑定', cancelButtonText: '稍后再说', type: 'warning' }
    )
    router.push('/apply/anchor')
  } catch (e) { /* cancel */ }
}

onMounted(loadData)
</script>

<style scoped>
.earnings-page { width: 100%; max-width: 1480px; margin: 0 auto; padding: 0 0 42px; color: var(--km-text); }
.earnings-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; padding: 4px 2px 22px; border-bottom: 1px solid var(--km-border); margin-bottom: 18px; }
.back-link { display: inline-flex; align-items: center; gap: 4px; margin-bottom: 16px; color: var(--km-text-faint); font-size: 11.5px; }
.back-link:hover { color: var(--km-text); }
.eyebrow { display: block; color: var(--km-accent); font-size: 9px; font-weight: 800; letter-spacing: .17em; margin-bottom: 5px; }
.earnings-head h1 { margin: 0 0 6px; font-size: 27px; font-weight: 900; letter-spacing: -.05em; }
.earnings-head p { margin: 0; color: var(--km-text-faint); font-size: 11.5px; }
.head-note { display: flex; flex-direction: column; gap: 3px; min-width: 180px; padding: 12px 14px; border: 1px solid var(--km-border); border-radius: 10px; background: rgba(255,255,255,.02); }
.head-note span { color: var(--km-text-faint); font-size: 9.5px; }
.head-note b { color: #f7d774; font-size: 13px; }
.head-note small { color: var(--km-text-faint); font-size: 9.5px; }

.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 18px; }
.summary-card { position: relative; display: flex; min-height: 137px; flex-direction: column; align-items: flex-start; padding: 16px 17px; border: 1px solid var(--km-border); border-radius: 13px; background: linear-gradient(145deg, #18131f, #0f0c14 78%); overflow: hidden; }
.summary-card::after { content: ''; position: absolute; right: -36px; bottom: -54px; width: 150px; height: 150px; border-radius: 50%; background: radial-gradient(circle, rgba(216,180,254,.08), transparent 70%); pointer-events: none; }
.summary-icon { width: 27px; height: 27px; display: grid; place-items: center; margin-bottom: 11px; border-radius: 7px; background: rgba(216,180,254,.09); color: var(--km-accent); font-size: 13px; }
.summary-icon.today { color: #93c5fd; background: rgba(96,165,250,.09); }
.summary-icon.month { color: #86efac; background: rgba(74,222,128,.08); }
.summary-icon.total { color: #f9a8d4; background: rgba(249,168,212,.08); }
.summary-icon.wallet { color: #f7d774; background: rgba(247,215,116,.08); }
.summary-label { color: var(--km-text-faint); font-size: 10px; }
.summary-value { margin-top: 2px; color: var(--km-text); font-size: 25px; line-height: 1.15; font-weight: 900; letter-spacing: -.05em; }
.summary-value small { margin-left: 3px; color: var(--km-text-faint); font-size: 9px; font-weight: 400; letter-spacing: 0; }
.summary-note { margin-top: auto; color: var(--km-text-faint); font-size: 9px; }
.wallet-card { background: linear-gradient(145deg, rgba(247,215,116,.08), #0f0c14 70%); }
.withdraw-btn { position: absolute; right: 15px; bottom: 14px; height: 28px; padding: 0 11px; border: 1px solid rgba(247,215,116,.28); border-radius: 6px; background: rgba(247,215,116,.09); color: #f7d774; font: inherit; font-size: 10px; font-weight: 700; cursor: pointer; }
.withdraw-btn:hover { background: rgba(247,215,116,.17); }

.detail-card { padding: 18px 18px 13px; border: 1px solid var(--km-border); border-radius: 14px; background: #0f0c14; }
.detail-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-bottom: 15px; }
.detail-head h2 { margin: 0; font-size: 15px; font-weight: 800; letter-spacing: -.025em; }
.detail-tools { display: flex; align-items: center; gap: 12px; }
.record-total { color: var(--km-text-faint); font-size: 10px; }
.status-select { width: 120px; }
.status-select :deep(.el-select__wrapper) { background: #15111c; box-shadow: 0 0 0 1px #2c2538 inset; }
.earnings-table { --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: rgba(255,255,255,.025); --el-table-border-color: rgba(255,255,255,.055); --el-table-text-color: var(--km-text-dim); --el-table-header-text-color: var(--km-text-faint); --el-table-row-hover-bg-color: rgba(255,255,255,.025); font-size: 11px; }
.earnings-table :deep(th.el-table__cell) { font-size: 10px; font-weight: 600; }
.earnings-table :deep(td.el-table__cell) { height: 51px; }
.cell-muted { color: var(--km-text-faint); font-size: 10px; }
.room-cell { display: flex; align-items: center; gap: 7px; min-width: 0; }
.room-cell span:last-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.room-dot { width: 5px; height: 5px; flex: none; border-radius: 50%; background: #c084fc; box-shadow: 0 0 0 3px rgba(192,132,252,.1); }
.gift-name { display: inline-flex; align-items: center; gap: 5px; color: var(--km-text-dim); }
.gift-name i { font-style: normal; font-size: 13px; }
.coin-value { color: #f7d774; font-size: 12px; font-weight: 750; }
.status-chip { display: inline-flex; align-items: center; height: 20px; padding: 0 7px; border-radius: 4px; font-size: 9.5px; font-weight: 650; }
.status-chip.pending { background: rgba(247,215,116,.1); color: #e8ca70; }
.status-chip.settled { background: rgba(74,222,128,.09); color: #86efac; }
.table-footer { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding-top: 12px; color: var(--km-text-faint); font-size: 9.5px; }
.table-footer :deep(.el-pagination) { --el-pagination-bg-color: transparent; --el-pagination-button-color: var(--km-text-faint); --el-pagination-hover-color: var(--km-accent); }
.table-footer :deep(.el-pagination.is-background .btn-next), .table-footer :deep(.el-pagination.is-background .btn-prev), .table-footer :deep(.el-pagination.is-background .el-pager li) { background: rgba(255,255,255,.035); color: var(--km-text-dim); }
.table-footer :deep(.el-pagination.is-background .el-pager li.is-active) { background: var(--km-accent); color: #120c19; }

@media (max-width: 1000px) {
  .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 680px) {
  .earnings-head { align-items: flex-start; flex-direction: column; }
  .head-note { width: 100%; }
  .detail-head { align-items: flex-start; flex-direction: column; }
  .detail-tools { width: 100%; justify-content: space-between; }
  .table-footer { align-items: flex-start; flex-direction: column; }
}
</style>
