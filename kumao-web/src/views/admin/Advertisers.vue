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
          <el-table-column prop="intro" label="简介" min-width="180" show-overflow-tooltip />
          <el-table-column label="身份证认证" width="170">
            <template #default="{ row }">
              <IdCardPreview
                :front="row.idCardFrontUrl"
                :back="row.idCardBackUrl"
                :number="row.idCard"
              />
            </template>
          </el-table-column>
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
          <el-radio-button value="PAUSED">已暂停</el-radio-button>
          <el-radio-button value="">全部</el-radio-button>
        </el-radio-group>
        <el-table :data="plans">
          <el-table-column prop="materialName" label="素材" min-width="180" />
          <el-table-column label="广告位" width="140">
            <template #default="{ row }">{{ slotText(row.slot) }}</template>
          </el-table-column>
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
              <template v-else-if="row.status === 'RUNNING'">
                <el-button size="small" plain type="warning" @click="adminPlanAction(row, 'PAUSE')">暂停</el-button>
                <el-button size="small" plain type="danger" @click="adminPlanAction(row, 'FINISH')">终止</el-button>
              </template>
              <template v-else-if="row.status === 'PAUSED'">
                <el-button size="small" plain type="success" @click="adminPlanAction(row, 'RESUME')">续投</el-button>
                <el-button size="small" plain type="danger" @click="adminPlanAction(row, 'FINISH')">终止</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="投流定价" name="pricing">
        <div class="pricing-panel">
          <div class="pricing-head">
            <div>
              <b>广告位日单价</b>
              <p>新建投流计划将按这里配置的日单价 × 投放天数计费；调整价格不会改变已经创建的计划。</p>
            </div>
            <el-button type="primary" :loading="pricingSaving" @click="savePricing">保存价格</el-button>
          </div>
          <el-table v-loading="pricingLoading" :data="pricingRows" style="width:100%">
            <el-table-column label="广告位" width="220">
              <template #default="{ row }"><b>{{ slotText(row.slot) }}</b></template>
            </el-table-column>
            <el-table-column label="日单价（元 / 天）" width="260">
              <template #default="{ row }">
                <el-input-number v-model="row.dailyPrice" :min="0.01" :max="1000000" :precision="2" :step="10" controls-position="right" style="width:180px" />
              </template>
            </el-table-column>
            <el-table-column label="计费规则">
              <template #default>投流费用 = 投放天数 × 日单价，按天向上取整</template>
            </el-table-column>
          </el-table>
          <p class="pricing-tip">价格保存后，广告主下次打开「新建投流计划」时立即使用新价格；已支付或待支付的旧计划保持原金额。</p>
        </div>
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
import IdCardPreview from '@/components/IdCardPreview.vue'
import { adSlotLabel } from '@/constants/adSlot'

const tab = ref('apply')
const list = ref([])
const status = ref('')
const materials = ref([])
const materialStatus = ref('PENDING')
const plans = ref([])
const planStatus = ref('PENDING')
const revenue = ref({})
const paidPlans = ref([])
const pricingRows = ref([])
const pricingLoading = ref(false)
const pricingSaving = ref(false)

const statusText = (s) => ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', OFFLINE: '已下架', RUNNING: '投放中', PAUSED: '已暂停', FINISHED: '已终止' }[s] || s)
const statusType = (s) => ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', OFFLINE: 'info', RUNNING: 'success', PAUSED: 'info', FINISHED: 'info' }[s] || 'info')
const slotText = adSlotLabel
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

async function loadPricing() {
  pricingLoading.value = true
  try {
    const res = await adApi.adminPricing()
    pricingRows.value = (res.data || []).map((item) => ({
      slot: item.slot,
      dailyPrice: Number(item.dailyPrice) || 0
    }))
  } finally {
    pricingLoading.value = false
  }
}

async function savePricing() {
  if (pricingRows.value.some((row) => !(Number(row.dailyPrice) > 0))) {
    ElMessage.warning('所有广告位日单价都必须大于 0')
    return
  }
  pricingSaving.value = true
  try {
    await adApi.updateAdminPricing(pricingRows.value.map((row) => ({
      slot: row.slot,
      dailyPrice: Number(row.dailyPrice)
    })))
    ElMessage.success('投流价格已保存，新计划立即生效')
    await loadPricing()
  } finally {
    pricingSaving.value = false
  }
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

async function adminPlanAction(row, action) {
  const label = { PAUSE: '暂停', RESUME: '续投', FINISH: '终止' }[action] || action
  const tip = action === 'FINISH'
    ? `终止后广告将立即停止展示，已产生的投流费用不予退还。确定终止「${row.materialName}」的投放计划吗？`
    : `确定要${label}「${row.materialName}」的投放计划吗？`
  try {
    await ElMessageBox.confirm(tip, `${label}投放计划`, {
      type: action === 'FINISH' ? 'warning' : 'info',
      confirmButtonText: `确定${label}`,
      cancelButtonText: '取消'
    })
  } catch (e) { return }
  await adApi.adminPlanAction(row.id, action)
  ElMessage.success(`已${label}`)
  await loadPlans()
}

watch(tab, (v) => {
  if (v === 'materials') loadMaterials()
  if (v === 'plans') loadPlans()
  if (v === 'pricing') loadPricing()
  if (v === 'revenue') loadRevenue()
})

onMounted(load)
</script>

<style scoped>
.user-cell { display: flex; align-items: center; gap: 10px; }
.u-name { font-weight: 600; }
.u-sub { font-size: 12px; color: var(--km-text-dim); }
.w-cover { width: 44px; height: 44px; border-radius: 8px; object-fit: cover; }
.stat-card { background: linear-gradient(180deg, #fff, #fdfbff); border: 1px solid #ece7f2; border-radius: 16px; padding: 20px 22px; text-align: center; box-shadow: 0 8px 22px rgba(61, 36, 92, .035); }
.value { font-size: 30px; font-weight: 800; letter-spacing: -.04em; color: var(--km-accent); font-variant-numeric: tabular-nums; }
.label { font-size: 12px; color: #6f657b; margin-top: 5px; }
.revenue-note { margin: 14px 0 0; font-size: 12px; }
.pricing-panel { padding: 20px; border: 1px solid var(--km-border); border-radius: 14px; background: var(--km-card); }
.pricing-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; margin-bottom: 18px; }
.pricing-head b { font-size: 15px; }
.pricing-head p { max-width: 700px; margin: 6px 0 0; color: var(--km-text-dim); font-size: 12px; line-height: 1.7; }
.pricing-tip { margin: 16px 0 0; padding: 10px 12px; border-radius: 8px; background: #faf7fd; color: var(--km-text-faint); font-size: 11.5px; line-height: 1.6; }
</style>
