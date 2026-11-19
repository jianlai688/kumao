<template>
  <div>
    <el-tabs v-model="tab">
      <el-tab-pane label="入驻审核" name="apply">
        <div class="toolbar km-card">
          <el-radio-group v-model="status" size="small" @change="load">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="PENDING">待审核</el-radio-button>
            <el-radio-button value="APPROVED">已通过</el-radio-button>
            <el-radio-button value="REJECTED">已拒绝</el-radio-button>
            <el-radio-button value="REVOKED">已取消</el-radio-button>
          </el-radio-group>
        </div>
        <el-table :data="list" style="width:100%; margin-top:12px">
          <el-table-column label="音乐人" min-width="200">
            <template #default="{ row }">
              <div class="user-cell">
                <el-avatar :size="36" :src="row.avatar">{{ row.stageName?.slice(0, 1) }}</el-avatar>
                <div>
                  <div class="u-name">{{ row.stageName }}</div>
                  <div class="u-sub">{{ row.realName }} · {{ row.style }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="representative" label="代表作" min-width="160" show-overflow-tooltip />
          <el-table-column label="身份证认证" width="170">
            <template #default="{ row }">
              <IdCardPreview
                :front="row.idCardFrontUrl"
                :back="row.idCardBackUrl"
                :number="row.idCard"
              />
            </template>
          </el-table-column>
          <el-table-column prop="fansCount" label="粉丝" width="100" />
          <el-table-column label="状态" width="100">
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
              <el-button v-if="row.status === 'APPROVED'" size="small" type="danger" plain @click="revoke(row)">取消资格</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="query.pageSize"
                         v-model:current-page="query.pageNum" @current-change="load" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="作品审核" name="works">
        <el-radio-group v-model="workStatus" size="small" @change="loadWorks" style="margin-bottom:12px">
          <el-radio-button value="PENDING">待审核</el-radio-button>
          <el-radio-button value="APPROVED">已通过</el-radio-button>
          <el-radio-button value="REJECTED">未通过</el-radio-button>
          <el-radio-button value="">全部</el-radio-button>
        </el-radio-group>
        <el-table :data="works">
          <el-table-column label="作品" min-width="220">
            <template #default="{ row }">
              <div class="user-cell">
                <img :src="row.coverUrl" class="w-cover" />
                <div>
                  <div class="u-name">{{ row.name }}</div>
                  <div class="u-sub">{{ row.albumName || '单曲' }} · {{ row.language }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="playCount" label="播放" width="100" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING'">
                <el-button size="small" type="success" @click="auditWork(row, true)">通过并入库</el-button>
                <el-button size="small" type="danger" @click="auditWork(row, false)">拒绝</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="收益与提现" name="income">
        <el-row :gutter="16">
          <el-col :span="8"><div class="stat-card"><div class="value">{{ income.totalIncome || 0 }}</div><div class="label">音乐人总收益</div></div></el-col>
          <el-col :span="8"><div class="stat-card"><div class="value">{{ income.pendingWithdraw || 0 }}</div><div class="label">待审提现</div></div></el-col>
          <el-col :span="8"><div class="stat-card"><div class="value">{{ income.musicianCount || 0 }}</div><div class="label">音乐人数量</div></div></el-col>
        </el-row>

        <el-table :data="withdraws" style="margin-top:16px">
          <el-table-column prop="musicianId" label="音乐人ID" width="130" />
          <el-table-column prop="userId" label="用户ID" width="130" />
          <el-table-column prop="amount" label="金额" width="100" />
          <el-table-column label="提现方式" width="110">
            <template #default="{ row }">{{ row.payType === 'WECHAT' ? '微信' : row.payType === 'ALIPAY' ? '支付宝' : '未标注' }}</template>
          </el-table-column>
          <el-table-column prop="account" label="收款账号" min-width="180" />
          <el-table-column prop="status" label="状态" width="110" />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING'">
                <el-button size="small" type="success" @click="auditWithdraw(row, true)">通过</el-button>
                <el-button size="small" type="danger" @click="auditWithdraw(row, false)">拒绝</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { musicianApi } from '@/api'
import IdCardPreview from '@/components/IdCardPreview.vue'

const tab = ref('apply')
const list = ref([])
const total = ref(0)
const status = ref('')
const works = ref([])
const workStatus = ref('PENDING')
const withdraws = ref([])
const income = ref({})
const query = reactive({ pageNum: 1, pageSize: 10 })

const statusText = (s) => ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', REVOKED: '已取消', OFFLINE: '已下架' }[s] || s)
const statusType = (s) => ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', REVOKED: 'info', OFFLINE: 'info' }[s] || 'info')

async function load() {
  const res = await musicianApi.adminPage({ ...query, status: status.value })
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

async function loadWorks() {
  const res = await musicianApi.adminWorks({ pageNum: 1, pageSize: 50, status: workStatus.value })
  works.value = res.data?.records || []
}

async function loadIncome() {
  const [i, w] = await Promise.all([musicianApi.adminIncome(), musicianApi.adminWithdraws({ pageNum: 1, pageSize: 50 })])
  income.value = i.data || {}
  withdraws.value = w.data?.records || []
}

async function audit(row, approved) {
  let reason = ''
  if (!approved) {
    try {
      const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝认证')
      reason = value
    } catch (e) { return }
  }
  await musicianApi.audit(row.id, approved, reason)
  ElMessage.success('审核完成')
  await load()
}

async function revoke(row) {
  try {
    const { value } = await ElMessageBox.prompt('取消资格将下架其全部作品，请输入原因', '取消资格')
    await musicianApi.revoke(row.id, value)
    ElMessage.success('已取消资格')
    await load()
  } catch (e) { /* cancel */ }
}

async function auditWork(row, approved) {
  let reason = ''
  if (!approved) {
    try {
      const { value } = await ElMessageBox.prompt('请输入拒绝原因', '作品审核')
      reason = value
    } catch (e) { return }
  }
  await musicianApi.auditWork(row.id, approved, reason)
  ElMessage.success(approved ? '已通过并同步至歌曲库' : '已拒绝')
  await loadWorks()
}

async function auditWithdraw(row, approved) {
  await musicianApi.auditWithdraw(row.id, approved, '')
  ElMessage.success('操作完成')
  await loadIncome()
}

watch(tab, (v) => {
  if (v === 'works') loadWorks()
  if (v === 'income') loadIncome()
})

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; }
.user-cell { display: flex; align-items: center; gap: 10px; }
.u-name { font-weight: 600; }
.u-sub { font-size: 12px; color: var(--km-text-dim); }
.w-cover { width: 40px; height: 40px; border-radius: 8px; object-fit: cover; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
.stat-card { background: linear-gradient(180deg, #fff, #fdfbff); border: 1px solid #ece7f2; border-radius: 16px; padding: 20px 22px; text-align: center; box-shadow: 0 8px 22px rgba(61, 36, 92, .035); }
.value { font-size: 30px; font-weight: 800; letter-spacing: -.04em; color: var(--km-accent); font-variant-numeric: tabular-nums; }
.label { font-size: 12px; color: #6f657b; margin-top: 5px; }
</style>
