<template>
  <div>
    <div class="toolbar km-card">
      <el-input v-model="query.songId" placeholder="输入歌曲 ID 筛选评论" clearable style="width:220px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="query.songId = ''; load()">重置</el-button>
    </div>

    <el-table :data="list" style="width:100%">
      <el-table-column prop="songId" label="歌曲ID" width="120" />
      <el-table-column prop="nickname" label="用户" width="140" />
      <el-table-column prop="content" label="评论内容" min-width="300" show-overflow-tooltip />
      <el-table-column prop="likeCount" label="点赞" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '正常' : '已屏蔽' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="180">
        <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 19) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" text type="warning" @click="toggle(row)">{{ row.status === 0 ? '屏蔽' : '恢复' }}</el-button>
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
import { commentApi } from '@/api'

const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, songId: '' })

async function load() {
  const res = await commentApi.adminPage({ ...query, songId: query.songId || undefined })
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

async function toggle(row) {
  await commentApi.adminStatus(row.id, row.status === 0 ? 1 : 0)
  ElMessage.success('操作成功')
  await load()
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除该评论吗？', '提示', { type: 'warning' })
    await commentApi.adminDelete(row.id)
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
