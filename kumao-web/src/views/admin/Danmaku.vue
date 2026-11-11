<template>
  <div>
    <div class="toolbar km-card">
      <el-input v-model="roomId" placeholder="直播间ID（可空）" clearable style="width:200px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="list" style="width:100%; margin-top:14px">
      <el-table-column prop="roomId" label="直播间" width="110" />
      <el-table-column prop="nickname" label="用户" width="140" />
      <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column label="时间" width="180">
        <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 19) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110">
        <template #default="{ row }">
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

const list = ref([])
const total = ref(0)
const roomId = ref('')
const query = reactive({ pageNum: 1, pageSize: 20 })

async function load() {
  const res = await liveApi.adminDanmaku({ ...query, roomId: roomId.value || undefined })
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除该弹幕吗？', '提示', { type: 'warning' })
    await liveApi.deleteDanmaku(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) { /* cancel */ }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
