<template>
  <div>
    <el-tabs v-model="tab">
      <el-tab-pane label="入驻审核" name="apply">
        <el-radio-group v-model="status" size="small" @change="load" style="margin-bottom:12px">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="PENDING">待审核</el-radio-button>
          <el-radio-button value="APPROVED">已通过</el-radio-button>
          <el-radio-button value="REJECTED">已拒绝</el-radio-button>
        </el-radio-group>
        <el-table :data="list">
          <el-table-column label="广告制作人" min-width="220">
            <template #default="{ row }">
              <div class="user-cell">
                <el-avatar :size="36" :src="row.avatar">{{ row.companyName?.slice(0, 1) }}</el-avatar>
                <div>
                  <div class="u-name">{{ row.companyName }}</div>
                  <div class="u-sub">{{ row.realName }} · {{ row.adType }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="intro" label="简介" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING'">
                <el-button size="small" type="success" @click="audit(row, true)">通过</el-button>
                <el-button size="small" type="danger" @click="audit(row, false)">拒绝</el-button>
              </template>
              <el-button v-if="row.status === 'APPROVED'" size="small" plain type="danger" @click="revoke(row)">取消资格</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="素材审核" name="materials">
        <el-radio-group v-model="materialStatus" size="small" @change="loadMaterials" style="margin-bottom:12px">
          <el-radio-button value="PENDING">待审核</el-radio-button>
          <el-radio-button value="APPROVED">已通过</el-radio-button>
          <el-radio-button value="">全部</el-radio-button>
        </el-radio-group>
        <el-table :data="materials">
          <el-table-column label="素材" min-width="240">
            <template #default="{ row }">
              <div class="user-cell">
                <img :src="row.coverUrl" class="w-cover" />
                <div>
                  <div class="u-name">{{ row.name }}</div>
                  <div class="u-sub">{{ row.type }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="exposureCount" label="曝光" width="100" />
          <el-table-column prop="clickCount" label="点击" width="100" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING'">
                <el-button size="small" type="success" @click="auditMaterial(row, true)">通过</el-button>
                <el-button size="small" type="danger" @click="auditMaterial(row, false)">拒绝</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="投放计划" name="plans">
        <el-radio-group v-model="planStatus" size="small" @change="loadPlans" style="margin-bottom:12px">
          <el-radio-button value="PENDING">待审核</el-radio-button>
          <el-radio-button value="RUNNING">投放中</el-radio-button>
          <el-radio-button value="">全部</el-radio-button>
        </el-radio-group>
        <el-table :data="plans">
          <el-table-column prop="materialName" label="素材" min-width="180" />
          <el-table-column prop="slot" label="广告位" width="130" />
          <el-table-column label="投放时段" min-width="240">
            <template #default="{ row }">{{ (row.startTime || '').replace('T', ' ').slice(0, 16) }} ~ {{ (row.endTime || '').replace('T', ' ').slice(0, 16) }}</template>
          </el-table-column>
          <el-table-column label="费用" width="110">
            <template #default="{ row }">¥{{ Number(row.amount || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="支付状态" width="105">
            <template #default="{ row }">
              <el-tag size="small" :type="row.paymentStatus === 'PAID' ? 'success' : 'warning'">{{ row.paymentStatus === 'PAID' ? '已支付' : '待支付' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="210">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING'">
                <template v-if="row.paymentStatus === 'PAID'">
                  <el-button size="small" type="success" @click="auditPlan(row, true)">通过并投放</el-button>
                  <el-button size="small" type="danger" @click="auditPlan(row, false)">拒绝</el-button>
                </template>
                <span v-else class="km-faint">等待用户支付</span>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="投放收入" name="revenue">
        <el-row :gutter="16">
          <el-col :span="8"><div class="stat-card"><div class="value">¥{{ formatMoney(revenue.placementRevenue) }}</div><div class="label">累计投放收入</div></div></el-col>
          <el-col :span="8"><div class="stat-card"><div class="value">{{ revenue.paidPlanCount || 0 }}</div><div class="label">已支付投流计划</div></div></el-col>
          <el-col :span="8"><div class="stat-card"><div class="value">{{ revenue.advertiserCount || 0 }}</div><div class="label">广告主数量</div></div></el-col>
        </el-row>
        <p class="km-faint revenue-note">广告主只为投流付费，平台按投放消耗确认收入；广告主侧没有收益与提现。</p>
        <el-table :data="paidPlans" style="margin-top:16px">
          <el-table-column prop="advertiserId" label="广告主ID" width="120" />
          <el-table-column prop="materialName" label="素材" min-width="180" />
          <el-table-column label="广告位" width="130">
            <template #default="{ row }">{{ slotText(row.slot) }}</template>
          </el-table-column>
          <el-table-column label="投流费用" width="120">
            <template #default="{ row }">¥{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="投放状态" width="110">
            <template #default="{ row }"><el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adApi } from '@/api'

const tab = ref('apply')
const list = ref([])
const status = ref('')
const materials = ref([])
const materialStatus = ref('PENDING')
const plans = ref([])
const planStatus = ref('PENDING')
const revenue = ref({})
const paidPlans = ref([])

const statusText = (s) => ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', OFFLINE: '已下架', RUNNING: '投放中', PAUSED: '已暂停', FINISHED: '已终止' }[s] || s)
const statusType = (s) => ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', OFFLINE: 'info', RUNNING: 'success', PAUSED: 'info', FINISHED: 'info' }[s] || 'info')
const slotText = (s) => ({ SPLASH: '开屏', HOME_BANNER: '首页Banner', LIVE: '直播页', PLAYER: '播放页' }[s] || s)
const formatMoney = (n) => Number(n || 0).toFixed(2)

async function load() {
  const res = await adApi.adminPage({ pageNum: 1, pageSize: 50, status: status.value })
  list.value = res.data?.records || []
}
async function loadMaterials() {
  const res = await adApi.adminMaterials({ pageNum: 1, pageSize: 50, status: materialStatus.value })
  materials.value = res.data?.records || []
}
async function loadPlans() {
  const res = await adApi.adminPlans({ pageNum: 1, pageSize: 50, status: planStatus.value })
  plans.value = res.data?.records || []
}
async function loadRevenue() {
  const [r, p] = await Promise.all([adApi.adminRevenue(), adApi.adminPlans({ pageNum: 1, pageSize: 100 })])
  revenue.value = r.data || {}
  paidPlans.value = (p.data?.records || []).filter((row) => row.paymentStatus === 'PAID')
}

async function audit(row, approved) {
  let reason = ''
  if (!approved) {
    try {
      const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝认证')
      reason = value
    } catch (e) { return }
  }
  await adApi.audit(row.id, approved, reason)
  ElMessage.success('审核完成')
  await load()
}

async function revoke(row) {
  try {
    const { value } = await ElMessageBox.prompt('取消资格将下架全部素材，请输入原因', '取消资格')
    await adApi.revoke(row.id, value)
    ElMessage.success('已取消资格')
    await load()
  } catch (e) { /* cancel */ }
}

async function auditMaterial(row, approved) {
  let reason = ''
  if (!approved) {
    try {
      const { value } = await ElMessageBox.prompt('请输入拒绝原因', '素材审核')
      reason = value
    } catch (e) { return }
  }
  await adApi.auditMaterial(row.id, approved, reason)
  ElMessage.success('操作完成')
  await loadMaterials()
}

async function auditPlan(row, approved) {
  await adApi.auditPlan(row.id, approved, '')
  ElMessage.success('操作完成')
  await loadPlans()
}

watch(tab, (v) => {
  if (v === 'materials') loadMaterials()
  if (v === 'plans') loadPlans()
  if (v === 'revenue') loadRevenue()
})

onMounted(load)
</script>

<style scoped>
.user-cell { display: flex; align-items: center; gap: 10px; }
.u-name { font-weight: 600; }
.u-sub { font-size: 12px; color: var(--km-text-dim); }
.w-cover { width: 44px; height: 44px; border-radius: 8px; object-fit: cover; }
.stat-card { background: linear-gradient(160deg, #16171c, #111216); border: 1px solid var(--km-border); border-radius: 18px; padding: 22px; text-align: center; }
.value { font-size: 30px; font-weight: 900; letter-spacing: -1px; color: var(--km-accent); }
.label { font-size: 12px; color: var(--km-text-faint); margin-top: 4px; }
.revenue-note { margin: 14px 0 0; font-size: 12px; }
</style>
