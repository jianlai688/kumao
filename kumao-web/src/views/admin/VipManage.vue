<template>
  <div>
    <el-tabs v-model="tab">
      <el-tab-pane label="会员套餐" name="packages">
        <div class="km-between" style="margin-bottom:14px">
          <b>套餐配置</b>
          <el-button type="primary" @click="openDialog()">新增套餐</el-button>
        </div>
        <el-table :data="packages">
          <el-table-column prop="name" label="套餐名称" width="150" />
          <el-table-column label="类型" width="120">
            <template #default="{ row }">{{ row.type === 'COIN' ? '酷猫币' : ({ MONTH: '月卡', QUARTER: '季卡', YEAR: '年卡' }[row.type] || row.type) }}</template>
          </el-table-column>
          <el-table-column label="天数 / 币数" width="110">
            <template #default="{ row }">{{ row.type === 'COIN' ? `${row.coinAmount} 币` : `${row.days} 天` }}</template>
          </el-table-column>
          <el-table-column prop="price" label="价格" width="110" />
          <el-table-column prop="originalPrice" label="原价" width="110" />
          <el-table-column prop="description" label="描述" min-width="200" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button size="small" text @click="openDialog(row)">编辑</el-button>
              <el-button size="small" text type="danger" @click="removePackage(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="会员记录" name="members">
        <el-table :data="members">
          <el-table-column prop="userId" label="用户ID" width="140" />
          <el-table-column prop="level" label="等级" width="110" />
          <el-table-column prop="status" label="状态" width="110" />
          <el-table-column prop="totalDays" label="累计天数" width="110" />
          <el-table-column label="到期时间" width="190">
            <template #default="{ row }">{{ (row.expireTime || '').replace('T', ' ').slice(0, 19) }}</template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination background layout="total, prev, pager, next" :total="memberTotal" :page-size="query.pageSize"
                         v-model:current-page="query.pageNum" @current-change="loadMembers" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="订单记录" name="orders">
        <el-table :data="orders">
          <el-table-column prop="orderNo" label="订单号" min-width="200" />
          <el-table-column prop="userId" label="用户ID" width="130" />
          <el-table-column prop="packageName" label="套餐" width="130" />
          <el-table-column prop="amount" label="金额" width="110" />
          <el-table-column label="支付方式" width="110">
            <template #default="{ row }">
              {{ row.payType === 'ALIPAY' ? '支付宝' : row.payType === 'WECHAT' ? '微信支付' : row.payType === 'AD_REWARD' ? '广告赠送' : '模拟支付' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              {{ row.status === 'PAID' ? '已支付' : row.status === 'UNPAID' ? '待支付' : '已关闭' }}
            </template>
          </el-table-column>
          <el-table-column prop="transactionId" label="交易号" min-width="180" />
          <el-table-column label="时间" width="180">
            <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 19) }}</template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination background layout="total, prev, pager, next" :total="orderTotal" :page-size="query.pageSize"
                         v-model:current-page="query.pageNum" @current-change="loadOrders" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="数据统计" name="stats">
        <el-row :gutter="16">
          <el-col :span="8"><div class="stat-card"><div class="value">{{ stats.totalMember || 0 }}</div><div class="label">会员总数</div></div></el-col>
          <el-col :span="8"><div class="stat-card"><div class="value">{{ stats.activeMember || 0 }}</div><div class="label">有效会员</div></div></el-col>
          <el-col :span="8"><div class="stat-card"><div class="value">¥{{ stats.revenue || 0 }}</div><div class="label">累计收入</div></div></el-col>
        </el-row>
        <div class="km-card" style="margin-top:16px">
          <EChart :option="chartOption" height="320px" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialog" :title="form.id ? '编辑套餐' : '新增套餐'" width="460px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="套餐名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width:100%">
            <el-option label="月卡" value="MONTH" /><el-option label="季卡" value="QUARTER" /><el-option label="年卡" value="YEAR" />
          </el-select>
        </el-form-item>
        <el-form-item label="天数"><el-input-number v-model="form.days" :min="1" /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="form.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="原价"><el-input-number v-model="form.originalPrice" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { vipApi } from '@/api'
import EChart from '@/components/EChart.vue'

const tab = ref('packages')
const packages = ref([])
const members = ref([])
const orders = ref([])
const memberTotal = ref(0)
const orderTotal = ref(0)
const stats = ref({})
const query = reactive({ pageNum: 1, pageSize: 10 })
const dialog = ref(false)
const form = reactive({ id: null, name: '', type: 'MONTH', days: 30, price: 18, originalPrice: 25, description: '', sort: 1, status: 1 })

const chartOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(19,20,24,.94)',
    borderColor: 'rgba(255,255,255,.1)',
    textStyle: { color: '#f3f4f6', fontSize: 12 }
  },
  grid: { left: 48, right: 22, top: 26, bottom: 30 },
  xAxis: {
    type: 'category',
    data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'],
    axisLine: { lineStyle: { color: 'rgba(255,255,255,.1)' } },
    axisTick: { show: false },
    axisLabel: { color: 'rgba(243,244,246,.4)', fontSize: 11 }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: 'rgba(255,255,255,.06)' } },
    axisLabel: { color: 'rgba(243,244,246,.4)', fontSize: 11 }
  },
  series: [{
    type: 'bar',
    data: [12, 18, 25, 22, 30, 42, 38, 45, 52, 61, 58, 70],
    barWidth: '46%',
    itemStyle: {
      borderRadius: [6, 6, 0, 0],
      color: {
        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [{ offset: 0, color: '#f7d774' }, { offset: 1, color: 'rgba(247,215,116,.14)' }]
      }
    }
  }]
}))

async function loadPackages() {
  const res = await vipApi.packages()
  packages.value = res.data || []
}
async function loadMembers() {
  const res = await vipApi.members(query)
  members.value = res.data?.records || []
  memberTotal.value = res.data?.total || 0
}
async function loadOrders() {
  const res = await vipApi.adminOrders(query)
  orders.value = res.data?.records || []
  orderTotal.value = res.data?.total || 0
}
async function loadStats() {
  const res = await vipApi.statistics()
  stats.value = res.data || {}
}

function openDialog(row) {
  Object.assign(form, row ? { ...row } : { id: null, name: '', type: 'MONTH', days: 30, price: 18, originalPrice: 25, description: '', sort: 1, status: 1 })
  dialog.value = true
}

async function save() {
  if (!form.name) return ElMessage.warning('请输入套餐名称')
  if (form.id) await vipApi.updatePackage(form.id, form)
  else await vipApi.createPackage(form)
  ElMessage.success('保存成功')
  dialog.value = false
  await loadPackages()
}

async function removePackage(row) {
  try {
    await ElMessageBox.confirm('确定删除该套餐吗？', '提示', { type: 'warning' })
    await vipApi.deletePackage(row.id)
    ElMessage.success('已删除')
    await loadPackages()
  } catch (e) { /* cancel */ }
}

watch(tab, (v) => {
  if (v === 'members') loadMembers()
  if (v === 'orders') loadOrders()
  if (v === 'stats') loadStats()
})

onMounted(loadPackages)
</script>

<style scoped>
.stat-card { background: linear-gradient(160deg, #16171c, #111216); border: 1px solid var(--km-border); border-radius: 18px; padding: 22px; text-align: center; }
.value { font-size: 30px; font-weight: 900; letter-spacing: -1px; color: #f7d774; }
.label { font-size: 12px; color: var(--km-text-faint); margin-top: 4px; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
