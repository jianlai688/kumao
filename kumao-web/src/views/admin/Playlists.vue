<template>
  <div>
    <div class="toolbar km-card">
      <el-input v-model="query.keyword" placeholder="歌单名称" clearable style="width:220px" @keyup.enter="load" />
      <el-select v-model="isPublic" placeholder="可见性" clearable style="width:140px">
        <el-option label="公开" :value="1" /><el-option label="私有" :value="0" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="list" style="width:100%; margin-top:14px">
      <el-table-column label="歌单" min-width="240">
        <template #default="{ row }">
          <div class="song-cell">
            <img :src="row.coverUrl" class="cell-cover" />
            <div>
              <div class="cell-name">{{ row.name }}</div>
              <div class="cell-sub">{{ row.creatorName }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="songCount" label="歌曲数" width="100" />
      <el-table-column prop="collectCount" label="收藏" width="100" />
      <el-table-column label="可见性" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.isPublic ? 'success' : 'info'">{{ row.isPublic ? '公开' : '私有' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 19) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" text @click="$router.push(`/playlist/${row.id}`)">查看</el-button>
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
import { playlistApi } from '@/api'

const list = ref([])
const total = ref(0)
const isPublic = ref(null)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })

async function load() {
  const res = await playlistApi.adminPage({ ...query, isPublic: isPublic.value })
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除该歌单吗？', '提示', { type: 'warning' })
    await playlistApi.adminDelete(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) { /* cancel */ }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.song-cell { display: flex; align-items: center; gap: 10px; }
.cell-cover { width: 44px; height: 44px; border-radius: 8px; object-fit: cover; }
.cell-name { font-weight: 600; }
.cell-sub { font-size: 12px; color: var(--km-text-dim); }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
