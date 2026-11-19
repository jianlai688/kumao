<template>
  <div>
    <div class="toolbar km-card">
      <el-radio-group v-model="status" size="small" @change="load">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="PENDING">待审核</el-radio-button>
        <el-radio-button value="APPROVED">已通过</el-radio-button>
        <el-radio-button value="REJECTED">已拒绝</el-radio-button>
      </el-radio-group>
    </div>

    <el-table :data="list" style="width:100%; margin-top:14px">
      <el-table-column prop="userId" label="用户ID" width="130" />
      <el-table-column prop="realName" label="真实姓名" width="130" />
      <el-table-column label="身份证认证" width="170">
        <template #default="{ row }">
          <IdCardPreview
            :front="row.idCardFrontUrl"
            :back="row.idCardBackUrl"
            :number="row.idCard"
          />
        </template>
      </el-table-column>
      <el-table-column prop="liveCategory" label="直播分类" width="120" />
      <el-table-column prop="intro" label="简介" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 'APPROVED' ? 'success' : row.status === 'PENDING' ? 'warning' : 'danger'">
            {{ row.status === 'APPROVED' ? '已通过' : row.status === 'PENDING' ? '待审核' : '已拒绝' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" width="180">
        <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 19) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button size="small" type="success" @click="audit(row, true)">通过</el-button>
            <el-button size="small" type="danger" @click="audit(row, false)">拒绝</el-button>
          </template>
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
import { userApi } from '@/api'
import IdCardPreview from '@/components/IdCardPreview.vue'

const list = ref([])
const total = ref(0)
const status = ref('')
const query = reactive({ pageNum: 1, pageSize: 10 })

async function load() {
  const res = await userApi.anchorApplies({ ...query, status: status.value })
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

async function audit(row, approved) {
  let reason = ''
  if (!approved) {
    try {
      const { value } = await ElMessageBox.prompt('请输入拒绝原因', '审核拒绝')
      reason = value
    } catch (e) {
      return
    }
  }
  await userApi.auditAnchor(row.id, approved, reason)
  ElMessage.success('审核完成')
  await load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
