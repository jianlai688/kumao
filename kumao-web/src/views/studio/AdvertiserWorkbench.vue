<template>
  <div class="workbench">
    <!-- 头部 -->
    <div class="wb-hero">
      <div class="hero-glow"></div>
      <div class="wb-left">
        <span class="wb-emoji">📣</span>
        <div class="wb-copy">
          <div class="wb-eyebrow">AD STUDIO</div>
          <h1 class="wb-title">{{ advertiser?.companyName || '广告制作人工作台' }}</h1>
          <div class="wb-tags">
            <span :class="['status-pill', advertiser?.status === 'APPROVED' ? 'ok' : 'warn']">
              {{ advertiser?.status === 'APPROVED' ? '已认证' : '认证审核中' }}
            </span>
            <span v-if="advertiser?.adType" class="style-pill">{{ advertiser.adType }}</span>
            <span class="km-faint">{{ materials.length }} 个素材 · {{ plans.length }} 个投流计划</span>
          </div>
        </div>
      </div>
      <button class="btn-accent" @click="openMaterialDialog()">
        <el-icon><Upload /></el-icon>上传广告素材
      </button>
    </div>

    <div v-if="!advertiser || advertiser.status !== 'APPROVED'" class="notice-bar">
      <span class="nb-ico">⚠️</span>
      <span>您的广告制作人认证尚未通过，暂不能上传素材与投流。</span>
      <router-link to="/apply/advertiser" class="nb-link">前往认证 →</router-link>
    </div>

    <el-tabs v-model="tab" class="wb-tabs">
      <el-tab-pane label="素材管理" name="materials">
        <div class="toolbar">
          <span class="km-faint">共 {{ materials.length }} 个素材</span>
        </div>
        <el-table :data="materials" style="width:100%">
          <el-table-column label="素材" min-width="220">
            <template #default="{ row }">
              <div class="material-cell">
                <img :src="row.coverUrl" class="m-cover" />
                <div>
                  <div class="m-name">{{ row.name }}</div>
                  <div class="m-sub">{{ typeText(row.type) }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="exposureCount" label="曝光" width="110" />
          <el-table-column prop="clickCount" label="点击" width="100" />
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button size="small" text @click="openMaterialDialog(row)">编辑</el-button>
              <el-button size="small" text type="warning" @click="offlineMaterial(row)">下架</el-button>
              <el-button size="small" text type="danger" @click="removeMaterial(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!materials.length" class="empty-box">
          <div class="empty-ico">🖼️</div>
          <p>暂无素材，上传第一个广告素材吧</p>
        </div>
      </el-tab-pane>

      <el-tab-pane label="投流管理" name="plans">
        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-ico">💸</div>
            <div class="stat-body"><div class="stat-value">¥{{ formatMoney(planSummary.spend) }}</div><div class="stat-label">累计投流费用</div></div>
          </div>
          <div class="stat-card">
            <div class="stat-ico">🚀</div>
            <div class="stat-body"><div class="stat-value accent">{{ planSummary.running }}</div><div class="stat-label">投放中计划</div></div>
          </div>
          <div class="stat-card">
            <div class="stat-ico">👁️</div>
            <div class="stat-body"><div class="stat-value">{{ formatNum(planSummary.exposure) }}</div><div class="stat-label">累计投流曝光</div></div>
          </div>
        </div>
        <div class="plan-note"><strong>多广告位投放</strong>：同一广告位可创建多个投流计划，平台会在审核通过、完成支付且处于投放时段时轮播展示；按广告位与投放天数结算。</div>
        <div class="toolbar">
          <span class="km-faint">共 {{ plans.length }} 个投流计划</span>
          <button class="btn-accent sm" @click="openPlanDialog">
            <el-icon><Plus /></el-icon>新建投流计划
          </button>
        </div>
        <el-table :data="plans" style="width:100%">
          <el-table-column prop="materialName" label="素材" min-width="180" />
          <el-table-column label="广告位" width="130">
            <template #default="{ row }">{{ slotText(row.slot) }}</template>
          </el-table-column>
          <el-table-column label="投放时段" min-width="240">
            <template #default="{ row }">
              {{ (row.startTime || '').replace('T', ' ').slice(0, 16) }} ~ {{ (row.endTime || '').replace('T', ' ').slice(0, 16) }}
            </template>
          </el-table-column>
          <el-table-column label="投流费用" width="120">
            <template #default="{ row }"><strong class="plan-amount">¥{{ formatMoney(row.amount) }}</strong></template>
          </el-table-column>
          <el-table-column label="支付状态" width="105">
            <template #default="{ row }">
              <el-tag size="small" :type="row.paymentStatus === 'PAID' ? 'success' : 'warning'">{{ row.paymentStatus === 'PAID' ? '已支付' : '待支付' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="投流状态" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="planDisplayType(row)">{{ planDisplayStatus(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="exposureCount" label="曝光" width="100" />
          <el-table-column prop="clickCount" label="点击" width="90" />
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button v-if="row.paymentStatus === 'UNPAID'" size="small" text type="warning" @click="openPlanPayment(row)">去支付</el-button>
              <el-button v-if="row.paymentStatus === 'PAID' && row.status === 'RUNNING'" size="small" text @click="planAction(row, 'PAUSE')">暂停</el-button>
              <el-button v-if="row.paymentStatus === 'PAID' && row.status === 'PAUSED'" size="small" text @click="planAction(row, 'RESUME')">续投</el-button>
              <el-button v-if="row.paymentStatus === 'PAID' && ['RUNNING','PAUSED'].includes(row.status)" size="small" text type="danger" @click="planAction(row, 'FINISH')">终止</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!plans.length" class="empty-box">
          <div class="empty-ico">🚀</div>
          <p>暂无投流计划，新建投流计划获取广告流量</p>
        </div>
      </el-tab-pane>

      <el-tab-pane label="数据统计" name="stats">
        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-ico">👁️</div>
            <div class="stat-body"><div class="stat-value">{{ formatNum(stats.exposure || 0) }}</div><div class="stat-label">总曝光量</div></div>
          </div>
          <div class="stat-card">
            <div class="stat-ico">🖱️</div>
            <div class="stat-body"><div class="stat-value accent">{{ formatNum(stats.click || 0) }}</div><div class="stat-label">总点击量</div></div>
          </div>
          <div class="stat-card">
            <div class="stat-ico">📈</div>
            <div class="stat-body"><div class="stat-value">{{ stats.ctr || '0%' }}</div><div class="stat-label">点击率 CTR</div></div>
          </div>
          <div class="stat-card">
            <div class="stat-ico">🗂️</div>
            <div class="stat-body"><div class="stat-value">{{ stats.materialCount || 0 }}</div><div class="stat-label">素材数量</div></div>
          </div>
        </div>
        <div class="two-col">
          <div class="panel">
            <div class="panel-head"><b>单条素材数据排行榜</b></div>
            <el-table :data="rank" size="small">
              <el-table-column prop="name" label="素材" />
              <el-table-column prop="exposureCount" label="曝光" width="100" />
              <el-table-column prop="clickCount" label="点击" width="90" />
            </el-table>
          </div>
          <div class="panel">
            <div class="panel-head"><b>分时段曝光趋势</b></div>
            <EChart :option="hourlyOption" height="270px" />
          </div>
        </div>
        <div class="panel">
          <div class="panel-head"><b>投放地域分布</b></div>
          <EChart :option="regionOption" height="270px" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="materialDialog" :title="materialForm.id ? '编辑素材' : '上传广告素材'" width="560px">
      <el-form :model="materialForm" label-width="90px">
        <el-form-item label="素材名称"><el-input v-model="materialForm.name" /></el-form-item>
        <el-form-item label="素材类型">
          <el-radio-group v-model="materialForm.type">
            <el-radio-button value="AUDIO">音频</el-radio-button>
            <el-radio-button value="VIDEO">视频</el-radio-button>
            <el-radio-button value="IMAGE_TEXT">图文</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="封面">
          <el-input v-model="materialForm.coverUrl">
            <template #append>
              <el-upload :show-file-list="false" :http-request="(o) => upload(o, 'ad-cover', 'coverUrl')" accept="image/*">
                <el-button>上传</el-button>
              </el-upload>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="素材文件">
          <el-input v-model="materialForm.fileUrl" placeholder="音频/视频文件地址">
            <template #append>
              <el-upload :show-file-list="false" :http-request="(o) => upload(o, 'ad', 'fileUrl')" accept="audio/*,video/*">
                <el-button>上传</el-button>
              </el-upload>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="图文内容">
          <el-input v-model="materialForm.content" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="materialDialog = false">取消</el-button>
        <el-button type="primary" @click="saveMaterial">保存并提交审核</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="planDialog" title="新建投流计划" width="520px">
      <el-form label-width="100px">
        <el-form-item label="广告素材">
          <el-select v-model="planForm.materialId" style="width:100%">
            <el-option v-for="m in approvedMaterials" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="广告位">
          <div class="slot-grid" role="radiogroup" aria-label="选择广告位">
            <button v-for="item in pricing" :key="item.slot" type="button" class="slot-option"
                    :class="{ selected: planForm.slot === item.slot }" role="radio"
                    :aria-checked="planForm.slot === item.slot" @click="planForm.slot = item.slot">
              <span class="slot-option-name">{{ slotText(item.slot) }}</span>
              <span class="slot-option-price">¥{{ formatMoney(item.dailyPrice) }}<small>/天</small></span>
            </button>
          </div>
        </el-form-item>
        <el-form-item label="目标人群">
          <el-select v-model="planForm.targetAudience" style="width:100%">
            <el-option label="全部用户" value="ALL" />
            <el-option label="VIP 会员" value="VIP" />
            <el-option label="新用户" value="NEW_USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="投放时段">
          <el-date-picker v-model="planRange" type="datetimerange" value-format="YYYY-MM-DDTHH:mm:ss"
                          start-placeholder="开始时间" end-placeholder="结束时间" style="width:100%" />
        </el-form-item>
      </el-form>
      <div class="plan-quote" aria-live="polite">
        <div><span>投流费用</span><strong>¥{{ formatMoney(planQuote.amount) }}</strong></div>
        <small v-if="planQuote.days">{{ slotText(planForm.slot) }} · {{ planQuote.days }} 天 × ¥{{ formatMoney(planQuote.dailyPrice) }}/天 · 按天计费</small>
        <small v-else-if="planQuote.dailyPrice">{{ slotText(planForm.slot) }} · ¥{{ formatMoney(planQuote.dailyPrice) }}/天 · 选择时段后计算总价</small>
        <small v-else>选择投放时段后显示报价</small>
      </div>
      <template #footer>
        <el-button @click="planDialog = false">取消</el-button>
        <el-button type="primary" @click="savePlan">创建并支付</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="planPay.visible" title="支付投流费用" width="min(460px, calc(100vw - 24px))" class="withdraw-dialog" :close-on-click-modal="false" @closed="resetPlanPay">
      <div class="plan-pay-summary">
        <div><span>{{ planPay.plan?.materialName }}</span><strong>¥{{ formatMoney(planPay.plan?.amount) }}</strong></div>
        <small>{{ slotText(planPay.plan?.slot) }} · {{ planPay.plan?.days }} 天 · 投流费用</small>
      </div>
      <div v-if="planPay.qr" class="plan-pay-qr">
        <img :src="planPay.qr" alt="微信支付二维码" />
        <p>{{ planPay.statusText }}</p>
      </div>
      <template v-else>
        <div class="plan-pay-label">支付方式</div>
        <div class="withdraw-channel-grid" role="radiogroup" aria-label="选择投流费用支付方式">
          <button type="button" class="withdraw-channel" :class="{ selected: planPay.method === 'WECHAT' }" role="radio" :aria-checked="planPay.method === 'WECHAT'" @click="planPay.method = 'WECHAT'; planPay.error = ''">
            <span class="withdraw-channel-logo wechat" aria-hidden="true">微</span>
            <span><b>微信支付</b><small>{{ planChannelHint('WECHAT') }}</small></span>
            <i class="withdraw-channel-dot" aria-hidden="true"></i>
          </button>
          <button type="button" class="withdraw-channel" :class="{ selected: planPay.method === 'ALIPAY' }" role="radio" :aria-checked="planPay.method === 'ALIPAY'" @click="planPay.method = 'ALIPAY'; planPay.error = ''">
            <span class="withdraw-channel-logo alipay" aria-hidden="true">支</span>
            <span><b>支付宝</b><small>{{ planChannelHint('ALIPAY') }}</small></span>
            <i class="withdraw-channel-dot" aria-hidden="true"></i>
          </button>
        </div>
        <p v-if="planPayChannels.mockEnabled && (!planPayChannels.alipayEnabled || !planPayChannels.wechatEnabled)" class="plan-pay-note">当前环境未配置真实商户，将使用本地模拟支付；支付成功后才可提交审核。</p>
        <p v-if="planPay.error" class="plan-pay-error" role="alert">{{ planPay.error }}</p>
      </template>
      <template #footer>
        <el-button @click="planPay.visible = false">{{ planPay.qr ? '关闭' : '取消' }}</el-button>
        <el-button v-if="!planPay.qr" type="primary" :disabled="planPay.submitting || !planPay.plan" @click="submitPlanPayment">{{ planPay.submitting ? '创建订单中…' : '支付 ¥' + formatMoney(planPay.plan?.amount) }}</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Plus } from '@element-plus/icons-vue'
import { adApi, fileApi, vipApi } from '@/api'
import EChart from '@/components/EChart.vue'

function formatNum(n) {
  n = Number(n) || 0
  if (n >= 100000000) return (n / 100000000).toFixed(1) + '亿'
  return n >= 10000 ? (n / 10000).toFixed(1) + '万' : String(n)
}

const tab = ref('materials')
const advertiser = ref(null)
const materials = ref([])
const plans = ref([])
const stats = ref({})
const rank = ref([])
const hourly = ref({ hours: [] })
const region = ref({ byRegion: {} })
const materialDialog = ref(false)
const planDialog = ref(false)
const planRange = ref([])
const pricing = ref([])
const planPayChannels = reactive({ alipayEnabled: false, wechatEnabled: false, mockEnabled: true })
const planPay = reactive({
  visible: false, plan: null, method: 'WECHAT', submitting: false, qr: '', orderNo: '', statusText: '', error: '', timer: null
})

const materialForm = reactive({ id: null, name: '', type: 'AUDIO', coverUrl: '', fileUrl: '', content: '' })
const planForm = reactive({ materialId: null, slot: 'SPLASH', targetAudience: 'ALL', startTime: '', endTime: '' })

const approvedMaterials = computed(() => materials.value.filter((m) => m.status === 'APPROVED'))
const planSummary = computed(() => {
  const paid = plans.value.filter((p) => p.paymentStatus === 'PAID')
  return {
    spend: paid.reduce((sum, p) => sum + Number(p.amount || 0), 0),
    running: plans.value.filter((p) => p.status === 'RUNNING').length,
    exposure: plans.value.reduce((sum, p) => sum + Number(p.exposureCount || 0), 0)
  }
})
const typeText = (t) => ({ AUDIO: '音频广告', VIDEO: '视频广告', IMAGE_TEXT: '图文广告' }[t] || t)
const statusText = (s) => ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '未通过', OFFLINE: '已下架' }[s] || s)
const statusType = (s) => ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', OFFLINE: 'info' }[s] || 'info')
const slotText = (s) => ({ SPLASH: '开屏', HOME_BANNER: '首页Banner', LIVE: '直播页', PLAYER: '播放页' }[s] || s)
const planStatusText = (s) => ({ PENDING: '待审核', RUNNING: '投放中', PAUSED: '已暂停', FINISHED: '已终止', REJECTED: '已拒绝' }[s] || s)
const planStatusType = (s) => ({ PENDING: 'warning', RUNNING: 'success', PAUSED: 'info', FINISHED: 'info', REJECTED: 'danger' }[s] || 'info')
const planDisplayStatus = (row) => row.paymentStatus === 'UNPAID' ? '待支付' : planStatusText(row.status)
const planDisplayType = (row) => row.paymentStatus === 'UNPAID' ? 'warning' : planStatusType(row.status)

function formatMoney(n) {
  return Number(n || 0).toFixed(2)
}

const planQuote = computed(() => {
  const slot = pricing.value.find((item) => item.slot === planForm.slot)
  const range = planRange.value
  const dailyPrice = slot ? Number(slot.dailyPrice) || 0 : 0
  if (!slot || !range || range.length !== 2) return { days: 0, dailyPrice, amount: 0 }
  const start = new Date(range[0]).getTime()
  const end = new Date(range[1]).getTime()
  if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) return { days: 0, dailyPrice, amount: 0 }
  const days = Math.max(1, Math.ceil((end - start) / 86400000))
  return { days, dailyPrice, amount: days * dailyPrice }
})

const hourlyOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(19,20,24,.94)',
    borderColor: 'rgba(255,255,255,.1)',
    textStyle: { color: '#f3f4f6', fontSize: 12 }
  },
  grid: { left: 48, right: 20, top: 20, bottom: 30 },
  xAxis: {
    type: 'category',
    data: Array.from({ length: 24 }, (_, i) => `${i}时`),
    axisLine: { lineStyle: { color: 'rgba(255,255,255,.1)' } },
    axisTick: { show: false },
    axisLabel: { color: 'rgba(243,244,246,.4)', fontSize: 10, interval: 2 }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: 'rgba(255,255,255,.06)' } },
    axisLabel: { color: 'rgba(243,244,246,.4)', fontSize: 11 }
  },
  series: [{
    type: 'bar',
    data: hourly.value.hours || [],
    barWidth: '55%',
    itemStyle: {
      borderRadius: [5, 5, 0, 0],
      color: {
        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: '#c084fc' },
          { offset: 1, color: 'rgba(192,132,252,.18)' }
        ]
      }
    }
  }]
}))

const regionOption = computed(() => ({
  backgroundColor: 'transparent',
  color: ['#c084fc', '#f472b6', '#7ab8ff', '#f7d774', '#ff9aab', '#c6a8ff'],
  tooltip: {
    trigger: 'item',
    backgroundColor: 'rgba(19,20,24,.94)',
    borderColor: 'rgba(255,255,255,.1)',
    textStyle: { color: '#f3f4f6', fontSize: 12 }
  },
  legend: { bottom: 0, textStyle: { color: 'rgba(243,244,246,.5)', fontSize: 11 }, itemWidth: 8, itemHeight: 8 },
  series: [{
    type: 'pie',
    radius: ['46%', '70%'],
    center: ['50%', '46%'],
    itemStyle: { borderColor: '#131418', borderWidth: 3, borderRadius: 4 },
    data: Object.entries(region.value.byRegion || {}).map(([name, value]) => ({ name, value })),
    label: { color: 'rgba(243,244,246,.6)', fontSize: 11 }
  }]
}))

async function loadPricing() {
  try {
    const res = await adApi.pricing()
    pricing.value = res.data || []
  } catch (e) {
    pricing.value = []
  }
}

async function loadBase() {
  const [a, m] = await Promise.all([adApi.mine(), adApi.materials({ pageNum: 1, pageSize: 100 }).catch(() => ({ data: { records: [] } }))])
  advertiser.value = a.data
  materials.value = m.data?.records || []
}

async function loadPlans() {
  try {
    const res = await adApi.plans({ pageNum: 1, pageSize: 100 })
    plans.value = res.data?.records || []
  } catch (e) { plans.value = [] }
}

async function loadStats() {
  const [s, r, h, g] = await Promise.all([
    adApi.statistics(), adApi.rank(), adApi.hourly(), adApi.region()
  ])
  stats.value = s.data || {}
  rank.value = r.data || []
  hourly.value = h.data || { hours: [] }
  region.value = g.data || { byRegion: {} }
}

function openMaterialDialog(row) {
  Object.assign(materialForm, row ? { ...row } : { id: null, name: '', type: 'AUDIO', coverUrl: '', fileUrl: '', content: '' })
  materialDialog.value = true
}

async function upload(options, biz, field) {
  const res = await fileApi.upload(options.file, biz)
  materialForm[field] = res.data.url
  ElMessage.success('上传成功')
}

async function saveMaterial() {
  if (!materialForm.name) return ElMessage.warning('请输入素材名称')
  const payload = { ...materialForm }
  delete payload.id
  if (materialForm.id) await adApi.updateMaterial(materialForm.id, payload)
  else await adApi.createMaterial(payload)
  ElMessage.success('已提交审核')
  materialDialog.value = false
  await loadBase()
}

async function offlineMaterial(row) {
  await adApi.offlineMaterial(row.id)
  ElMessage.success('已下架')
  await loadBase()
}

async function removeMaterial(row) {
  try {
    await ElMessageBox.confirm('确定删除该素材吗？', '提示', { type: 'warning' })
    await adApi.deleteMaterial(row.id)
    ElMessage.success('已删除')
    await loadBase()
  } catch (e) { /* cancel */ }
}

function openPlanDialog() {
  planForm.materialId = null
  planForm.slot = 'SPLASH'
  planForm.targetAudience = 'ALL'
  planForm.startTime = ''
  planForm.endTime = ''
  planRange.value = []
  planDialog.value = true
  if (!pricing.value.length) loadPricing()
}

function planChannelHint(method) {
  const enabled = method === 'WECHAT' ? planPayChannels.wechatEnabled : planPayChannels.alipayEnabled
  return enabled ? '安全跳转支付' : '未配置 · 模拟支付'
}

function stopPlanPolling() {
  if (planPay.timer) {
    clearInterval(planPay.timer)
    planPay.timer = null
  }
}

function resetPlanPay() {
  stopPlanPolling()
  planPay.plan = null
  planPay.method = 'WECHAT'
  planPay.submitting = false
  planPay.qr = ''
  planPay.orderNo = ''
  planPay.statusText = ''
  planPay.error = ''
}

async function openPlanPayment(plan) {
  if (!plan || plan.paymentStatus === 'PAID') return
  planPay.plan = plan
  planPay.visible = true
  planPay.error = ''
  planPay.qr = ''
  try {
    const res = await vipApi.payChannels()
    Object.assign(planPayChannels, res.data || {})
    planPay.method = planPayChannels.wechatEnabled || !planPayChannels.alipayEnabled ? 'WECHAT' : 'ALIPAY'
  } catch (e) {
    planPay.error = e?.response?.data?.message || e?.message || '支付渠道加载失败'
  }
}

async function finishPlanPayment() {
  stopPlanPolling()
  await loadPlans()
  ElMessage.success('投流费用支付成功，计划已提交审核')
  planPay.visible = false
}

function startPlanPolling(orderNo) {
  stopPlanPolling()
  planPay.statusText = '等待扫码支付…'
  planPay.timer = setInterval(async () => {
    try {
      const res = await vipApi.payStatus(orderNo)
      if (res.data?.status === 'PAID') await finishPlanPayment()
      else if (res.data?.status === 'CLOSED') {
        stopPlanPolling()
        planPay.statusText = '订单已关闭，请重新发起支付'
      }
    } catch (e) { /* keep polling */ }
  }, 2500)
}

async function submitPlanPayment() {
  if (!planPay.plan) return
  planPay.submitting = true
  planPay.error = ''
  try {
    const res = await vipApi.createAdPayment(planPay.plan.id, planPay.method)
    const data = res.data
    if (data.mock) {
      await ElMessageBox.confirm(
        `当前为本地模拟支付，确认支付 ¥${formatMoney(data.amount)} 投流费用？`,
        '模拟支付',
        { type: 'warning', confirmButtonText: '模拟支付成功', cancelButtonText: '取消' }
      )
      const paid = await vipApi.mockPay(data.orderNo)
      if (paid.data?.status === 'PAID') await finishPlanPayment()
      return
    }
    if (data.payType === 'ALIPAY') {
      window.location.href = data.payUrl
      return
    }
    if (data.payType === 'WECHAT') {
      planPay.qr = data.qrCode
      planPay.orderNo = data.orderNo
      startPlanPolling(data.orderNo)
    }
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    planPay.error = error?.response?.data?.message || error?.message || '投流费用支付失败'
  } finally {
    planPay.submitting = false
  }
}

async function savePlan() {
  if (!planForm.materialId) return ElMessage.warning('请选择素材')
  if (!planRange.value || planRange.value.length !== 2) return ElMessage.warning('请选择投放时段')
  planForm.startTime = planRange.value[0]
  planForm.endTime = planRange.value[1]
  const res = await adApi.createPlan(planForm)
  planDialog.value = false
  await loadPlans()
  ElMessage.success('投流计划已创建，请完成投流费用支付')
  await openPlanPayment(res.data)
}

async function planAction(row, action) {
  await adApi.planAction(row.id, action)
  ElMessage.success('操作成功')
  await loadPlans()
}

watch(tab, (v) => {
  if (v === 'materials') loadBase()
  if (v === 'plans') { loadBase(); loadPlans() }
  if (v === 'stats') loadStats()
})

onMounted(async () => {
  await loadPricing()
  await loadBase()
  await loadPlans()
})
</script>

<style scoped>
.workbench { padding-bottom: 40px; }

.wb-hero {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 26px 28px;
  margin-bottom: 20px;
  border-radius: 24px;
  overflow: hidden;
  background:
    radial-gradient(600px 300px at 5% 0%, rgba(192, 132, 252, .14), transparent 62%),
    radial-gradient(500px 300px at 95% 100%, rgba(255, 179, 71, .07), transparent 60%),
    linear-gradient(140deg, #14161a, #0a0b0d 72%);
  border: 1px solid var(--km-border);
  flex-wrap: wrap;
}
.hero-glow {
  position: absolute;
  width: 320px;
  height: 320px;
  left: -80px;
  top: -160px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(192, 132, 252, .16), transparent 65%);
  filter: blur(28px);
  pointer-events: none;
}
.wb-left { display: flex; align-items: center; gap: 18px; position: relative; }
.wb-emoji { font-size: 52px; filter: drop-shadow(0 10px 24px rgba(192, 132, 252, .22)); }
.wb-eyebrow { font-size: 10px; letter-spacing: 2px; color: var(--km-accent); font-weight: 800; margin-bottom: 3px; }
.wb-title { margin: 0 0 7px; font-size: 24px; font-weight: 900; letter-spacing: -.8px; }
.wb-tags { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; font-size: 12px; }
.status-pill {
  height: 20px;
  padding: 0 9px;
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  font-size: 10.5px;
  font-weight: 700;
}
.status-pill.ok { background: rgba(192, 132, 252, .14); color: var(--km-accent); }
.status-pill.warn { background: rgba(247, 215, 116, .14); color: #f7d774; }
.style-pill {
  height: 20px;
  padding: 0 9px;
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, .06);
  color: var(--km-text-dim);
  font-size: 10.5px;
}

.btn-accent {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 18px;
  border: none;
  border-radius: 999px;
  background: var(--km-accent);
  color: #07080a;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all .2s;
  box-shadow: 0 8px 22px rgba(192, 132, 252, .22);
  position: relative;
}
.btn-accent:hover { background: #d8b4fe; transform: translateY(-1px); }
.btn-accent.sm { height: 34px; padding: 0 15px; font-size: 12px; }

.notice-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  margin-bottom: 18px;
  border-radius: 14px;
  background: rgba(247, 215, 116, .08);
  border: 1px solid rgba(247, 215, 116, .22);
  font-size: 12.5px;
  color: #f7d774;
}
.nb-ico { font-size: 14px; }
.nb-link { margin-left: auto; color: var(--km-accent); font-weight: 700; }

.wb-tabs :deep(.el-tabs__header) { margin-bottom: 20px; }
.wb-tabs :deep(.el-tabs__item) { font-size: 14px; padding: 0 20px; }
.wb-tabs :deep(.el-table) {
  --el-table-bg-color: var(--km-card);
  --el-table-tr-bg-color: var(--km-card);
  border: 1px solid var(--km-border);
  border-radius: 16px;
  overflow: hidden;
  padding: 4px 8px;
}
.wb-tabs :deep(.el-table th.el-table__cell) {
  background: rgba(255, 255, 255, .03) !important;
  font-size: 12px;
  color: var(--km-text-dim) !important;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
  flex-wrap: wrap;
  padding: 12px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, .025);
  border: 1px solid var(--km-border);
}

.material-cell { display: flex; align-items: center; gap: 11px; }
.m-cover { width: 44px; height: 44px; border-radius: 9px; object-fit: cover; }
.m-name { font-weight: 600; font-size: 13.5px; }
.m-sub { font-size: 11.5px; color: var(--km-text-faint); margin-top: 2px; }

.stats-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap: 14px; margin-bottom: 18px; }
.plan-note { margin: 0 0 14px; padding: 10px 13px; border: 1px solid rgba(192, 132, 252, .22); border-radius: 10px; background: rgba(192, 132, 252, .07); color: var(--km-text-dim); font-size: 12px; line-height: 1.6; }
.plan-note strong { color: var(--km-accent); }
.slot-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 9px; width: 100%; }
.slot-option { display: flex; align-items: center; justify-content: space-between; gap: 10px; min-height: 56px; padding: 10px 12px; border: 1px solid var(--km-border); border-radius: 12px; background: rgba(255,255,255,.025); color: var(--km-text); text-align: left; cursor: pointer; transition: .18s ease; }
.slot-option:hover { border-color: var(--km-accent-line); background: rgba(216,180,254,.06); }
.slot-option.selected { border-color: var(--km-accent); background: rgba(216,180,254,.12); box-shadow: 0 0 0 2px rgba(216,180,254,.08); }
.slot-option-name { font-size: 12.5px; font-weight: 700; }
.slot-option-price { color: var(--km-accent); font-size: 15px; font-weight: 800; white-space: nowrap; }
.slot-option-price small { margin-left: 2px; color: var(--km-text-faint); font-size: 10px; font-weight: 500; }
.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: linear-gradient(160deg, #16171c, #111216);
  border: 1px solid var(--km-border);
  border-radius: 16px;
  padding: 18px;
}
.stat-ico {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: rgba(192, 132, 252, .08);
  border: 1px solid rgba(192, 132, 252, .16);
  font-size: 16px;
  flex-shrink: 0;
}
.stat-body { min-width: 0; }
.stat-value { font-size: 22px; font-weight: 900; letter-spacing: -.8px; line-height: 1.2; }
.stat-value.accent { color: var(--km-accent); }
.stat-label { font-size: 11.5px; color: var(--km-text-faint); margin-top: 3px; }

.panel {
  background: var(--km-card);
  border: 1px solid var(--km-border);
  border-radius: var(--km-radius);
  padding: 20px;
  margin-bottom: 16px;
}
.panel-head { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-bottom: 14px; }
.panel-head b { font-size: 14px; font-weight: 800; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

.empty-box { padding: 60px 20px; text-align: center; }
.empty-ico { font-size: 34px; opacity: .5; margin-bottom: 10px; }
.empty-box p { margin: 0; color: var(--km-text-dim); font-size: 13px; }


.withdraw-channel-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 9px; width: 100%; }
.withdraw-channel { position: relative; display: flex; align-items: center; gap: 9px; min-height: 56px; padding: 9px 10px; border: 1px solid var(--km-border); border-radius: 10px; background: rgba(255, 255, 255, .025); color: var(--km-text); text-align: left; cursor: pointer; }
.withdraw-channel:hover { border-color: rgba(192, 132, 252, .45); }
.withdraw-channel.selected { border-color: rgba(192, 132, 252, .7); background: rgba(192, 132, 252, .1); }
.withdraw-channel-logo { width: 29px; height: 29px; flex: none; display: grid; place-items: center; border-radius: 8px; color: #fff; font-size: 13px; font-weight: 800; }
.withdraw-channel-logo.alipay { background: #1677ff; }
.withdraw-channel-logo.wechat { background: #07c160; }
.withdraw-channel > span:nth-child(2) { display: flex; flex-direction: column; gap: 2px; }
.withdraw-channel b { font-size: 12.5px; font-weight: 700; }
.withdraw-channel small { color: var(--km-text-faint); font-size: 10.5px; }
.withdraw-channel-dot { position: absolute; top: 9px; right: 9px; width: 10px; height: 10px; border: 1px solid #5c5364; border-radius: 50%; }
.withdraw-channel.selected .withdraw-channel-dot { border: 3px solid var(--km-accent); }
@media (max-width: 540px) { .withdraw-channel-grid { grid-template-columns: 1fr; } }
@media (max-width: 540px) { .slot-grid { grid-template-columns: 1fr; } }
.plan-amount { color: #f7d774; font-size: 12px; font-weight: 700; font-variant-numeric: tabular-nums; }
.plan-quote { display: flex; flex-direction: column; gap: 5px; margin: 2px 0 0; padding: 12px 14px; border: 1px solid rgba(192, 132, 252, .2); border-radius: 9px; background: rgba(192, 132, 252, .06); }
.plan-quote > div { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; color: var(--km-text-dim); font-size: 12px; }
.plan-quote strong { color: var(--km-accent); font-size: 21px; font-weight: 700; letter-spacing: -.03em; font-variant-numeric: tabular-nums; }
.plan-quote small { color: var(--km-text-faint); font-size: 10.5px; line-height: 1.5; }
.plan-pay-summary { display: flex; flex-direction: column; gap: 4px; padding: 13px 14px; margin-bottom: 16px; border: 1px solid rgba(192, 132, 252, .18); border-radius: 9px; background: rgba(192, 132, 252, .06); }
.plan-pay-summary > div { display: flex; justify-content: space-between; gap: 10px; color: var(--km-text-dim); font-size: 12px; }
.plan-pay-summary strong { color: var(--km-accent); font-size: 21px; font-variant-numeric: tabular-nums; }
.plan-pay-summary small { color: var(--km-text-faint); font-size: 10.5px; }
.plan-pay-label { margin-bottom: 8px; color: var(--km-text-dim); font-size: 11.5px; }
.plan-pay-note { margin: 11px 0 0; color: #f7d774; font-size: 10.5px; line-height: 1.55; }
.plan-pay-error { margin: 11px 0 0; color: #ff9eae; font-size: 11px; line-height: 1.5; }
.plan-pay-qr { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 8px 0 14px; }
.plan-pay-qr img { width: 230px; height: 230px; padding: 8px; border-radius: 10px; background: #fff; }
.plan-pay-qr p { margin: 0; color: var(--km-text-faint); font-size: 11px; }
@media (max-width: 540px) { .plan-pay-qr img { width: 200px; height: 200px; } }
@media (max-width: 980px) {
  .two-col { grid-template-columns: 1fr; }
  .wb-hero { padding: 20px; }
  .wb-title { font-size: 20px; }
}
</style>
