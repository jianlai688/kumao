<template>
  <div>
    <div class="toolbar km-card">
      <el-select v-model="status" placeholder="直播状态" clearable style="width:150px">
        <el-option label="未开播" value="NOT_STARTED" />
        <el-option label="直播中" value="LIVING" />
        <el-option label="已下播" value="ENDED" />
        <el-option label="已封禁" value="BANNED" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="直播间标题" clearable style="width:200px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="list" style="width:100%; margin-top:14px">
      <el-table-column label="直播间" min-width="240">
        <template #default="{ row }">
          <div class="song-cell">
            <img :src="row.coverUrl" class="cell-cover" />
            <div>
              <div class="cell-name">{{ row.title }} <el-tag v-if="row.banned" size="small" type="danger">已封禁</el-tag></div>
              <div class="cell-sub">
                {{ row.anchorName }} · {{ row.category }} ·
                {{ row.scene === 'OUTDOOR' ? '户外' + (row.location ? ' · ' + row.location : '') : '室内' }}
              </div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column prop="onlineCount" label="在线" width="90" />
      <el-table-column prop="peakOnline" label="峰值" width="90" />
      <el-table-column prop="giftIncome" label="礼物收益" width="110" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button v-if="!row.banned" size="small" text type="danger" @click="ban(row, true)">封禁</el-button>
          <el-button v-else size="small" text type="success" @click="ban(row, false)">解封</el-button>
          <el-button size="small" text @click="$router.push(`/live/${row.id}`)">查看</el-button>
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
const status = ref('')
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })

async function load() {
  const res = await liveApi.adminPage({ ...query, status: status.value })
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

async function ban(row, banned) {
  let reason = ''
  if (banned) {
    try {
      const { value } = await ElMessageBox.prompt('请输入封禁原因', '封禁直播间')
      reason = value
    } catch (e) { return }
  }
  await liveApi.ban(row.id, banned, reason)
  ElMessage.success(banned ? '已封禁' : '已解封')
  await load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; }
.song-cell { display: flex; align-items: center; gap: 10px; }
.cell-cover { width: 62px; height: 38px; border-radius: 6px; object-fit: cover; }
.cell-name { font-weight: 600; display: flex; gap: 6px; align-items: center; }
.cell-sub { font-size: 12px; color: var(--km-text-dim); }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
