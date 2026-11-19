<template>
  <div>
    <div class="toolbar km-card">
      <el-input v-model="roomId" placeholder="直播间ID（可空）" clearable style="width:180px" @keyup.enter="load" />
      <el-radio-group v-model="query.status" @change="load">
        <el-radio-button :value="''">全部</el-radio-button>
        <el-radio-button :value="2">待审核</el-radio-button>
        <el-radio-button :value="0">已通过</el-radio-button>
        <el-radio-button :value="1">已屏蔽</el-radio-button>
      </el-radio-group>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <el-button type="warning" plain :loading="rescanning" @click="aiRescan">AI 复检已通过</el-button>
    </div>

    <el-table :data="list" style="width:100%; margin-top:14px">
      <el-table-column prop="roomId" label="直播间" width="100" />
      <el-table-column prop="nickname" label="用户" width="130" />
      <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip />
      <el-table-column label="AI审核" width="200">
        <template #default="{ row }">
          <template v-if="row.aiDecision">
            <el-tag size="small" :type="decisionType(row.aiDecision)">{{ decisionText(row.aiDecision) }}</el-tag>
            <span class="risk-text">风险 {{ row.aiRiskScore ?? '--' }}%</span>
            <div class="reason-text" :title="row.aiReasons || ''">{{ row.aiReasons || '未记录审核依据' }}</div>
          </template>
          <span v-else class="muted">无 AI 记录</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="105">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="90" />
      <el-table-column label="时间" width="170">
        <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 19) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 2">
            <el-button size="small" text type="success" @click="setStatus(row, 0)">通过</el-button>
            <el-button size="small" text type="danger" @click="reject(row)">驳回</el-button>
          </template>
          <el-button v-else-if="row.status === 0" size="small" text type="warning" @click="reject(row)">屏蔽</el-button>
          <el-button v-else size="small" text type="success" @click="setStatus(row, 0)">恢复</el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="query.pageSize"
                     v-model:current-page="query.pageNum" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { liveApi } from '@/api'
import request from '@/api/request'

const list = ref([])
const total = ref(0)
const roomId = ref('')
const rescanning = ref(false)
const query = reactive({ pageNum: 1, pageSize: 20, status: '' })

async function aiRescan() {
  rescanning.value = true
  try {
    const res = await request.post('/live/live/danmaku/admin/ai-rescan', null, { params: { limit: 50 } })
    const data = res.data || {}
    ElMessage.success(`复检 ${data.scanned || 0} 条，封禁 ${data.blocked || 0} 条`)
    await load()
  } finally {
    rescanning.value = false
  }
}

async function load() {
  const res = await liveApi.adminDanmaku({
    ...query,
    roomId: roomId.value || undefined,
    status: query.status === '' ? undefined : query.status
  })
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

async function setStatus(row, status) {
  await liveApi.adminDanmakuStatus(row.id, status)
  ElMessage.success(status === 0 ? '已通过，弹幕将进入历史记录' : '已更新弹幕状态')
  await load()
}

async function reject(row) {
  try {
    await ElMessageBox.confirm('驳回/屏蔽后，该弹幕将不再公开展示。确定继续吗？', '确认驳回', {
      type: 'warning',
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消'
    })
    await setStatus(row, 1)
  } catch (e) { /* cancel */ }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除该弹幕吗？', '提示', { type: 'warning' })
    await liveApi.deleteDanmaku(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) { /* cancel */ }
}

function reset() {
  roomId.value = ''
  query.status = ''
  query.pageNum = 1
  load()
}

function statusText(status) {
  return ({ 0: '已通过', 1: '已屏蔽', 2: '待审核' }[status] || '未知')
}

function statusType(status) {
  return ({ 0: 'success', 1: 'danger', 2: 'warning' }[status] || 'info')
}

function decisionText(decision) {
  return ({ PASS: '建议通过', REVIEW: '建议复核', REJECT: '建议驳回' }[decision] || decision || '未分析')
}

function decisionType(decision) {
  return ({ PASS: 'success', REVIEW: 'warning', REJECT: 'danger' }[decision] || 'info')
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
.risk-text { margin-left: 6px; color: var(--km-text-faint); font-size: 10px; }
.reason-text { margin-top: 4px; overflow: hidden; color: var(--km-text-faint); font-size: 10px; line-height: 1.35; white-space: nowrap; text-overflow: ellipsis; }
.muted { color: var(--km-text-faint); font-size: 10.5px; }
</style>
