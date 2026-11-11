<template>
  <div>
    <div class="toolbar km-card">
      <el-select v-model="fileType" placeholder="文件类型" clearable style="width:150px">
        <el-option label="图片" value="IMAGE" /><el-option label="音频" value="AUDIO" />
        <el-option label="视频" value="VIDEO" /><el-option label="歌词" value="LYRIC" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="文件名" clearable style="width:220px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="list" style="width:100%; margin-top:14px">
      <el-table-column prop="originalName" label="文件名" min-width="220" show-overflow-tooltip />
      <el-table-column prop="fileType" label="类型" width="100" />
      <el-table-column prop="bizType" label="业务" width="120" />
      <el-table-column label="大小" width="110">
        <template #default="{ row }">{{ (row.size / 1024 / 1024).toFixed(2) }} MB</template>
      </el-table-column>
      <el-table-column label="访问地址" min-width="260" show-overflow-tooltip>
        <template #default="{ row }"><a :href="row.url" target="_blank" class="link">{{ row.url }}</a></template>
      </el-table-column>
      <el-table-column label="上传时间" width="180">
        <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 19) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100">
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
import { fileApi } from '@/api'

const list = ref([])
const total = ref(0)
const fileType = ref('')
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })

async function load() {
  const res = await fileApi.adminPage({ ...query, fileType: fileType.value })
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除该文件吗？', '提示', { type: 'warning' })
    await fileApi.remove(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) { /* cancel */ }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
.link { color: var(--km-accent); font-size: 12px; }
.link:hover { text-decoration: underline; }
</style>
