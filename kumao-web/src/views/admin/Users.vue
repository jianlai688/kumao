<template>
  <div>
    <div class="toolbar km-card">
      <el-input v-model="query.keyword" placeholder="用户名/昵称/手机号" clearable style="width:220px" @keyup.enter="load" />
      <el-select v-model="status" placeholder="账号状态" clearable style="width:140px">
        <el-option label="正常" :value="0" />
        <el-option label="禁用" :value="1" />
      </el-select>
      <el-select v-model="role" placeholder="角色" clearable style="width:150px">
        <el-option v-for="r in roles" :key="r.value" :label="r.label" :value="r.value" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <div style="flex:1"></div>
      <el-button @click="logDialog = true">登录设备记录</el-button>
    </div>

    <el-table :data="list" style="width:100%; margin-top:14px">
      <el-table-column label="用户" min-width="220">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar :size="36" :src="row.avatar">{{ (row.nickname || 'U').slice(0, 1) }}</el-avatar>
            <div>
              <div class="u-name">{{ row.nickname || row.username }}</div>
              <div class="u-sub">{{ row.username }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="150" />
      <el-table-column label="角色" min-width="200">
        <template #default="{ row }">
          <el-tag v-for="r in (row.roles || '').split(',')" :key="r" size="small" effect="dark" style="margin-right:4px">{{ roleText(r) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 0" @change="(v) => toggleStatus(row, v)" />
        </template>
      </el-table-column>
      <el-table-column prop="lastLoginTime" label="最后登录" width="180" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-dropdown @command="(cmd) => grant(row, cmd)">
            <el-button size="small" text type="primary">授予角色<el-icon><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="r in grantRoles" :key="r.value" :command="r.value">{{ r.label }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="query.pageSize"
                     v-model:current-page="query.pageNum" @current-change="load" />
    </div>

    <el-drawer v-model="logDialog" title="用户登录设备记录" size="50%">
      <div class="km-flex" style="margin-bottom:12px">
        <el-input v-model="logUserId" placeholder="按用户ID筛选（可空）" style="width:220px" @keyup.enter="loadLogs" />
        <el-button type="primary" @click="loadLogs">查询</el-button>
      </div>
      <el-table :data="logs">
        <el-table-column prop="userId" label="用户ID" width="120" />
        <el-table-column prop="device" label="设备" width="100" />
        <el-table-column prop="ip" label="IP" width="150" />
        <el-table-column prop="loginType" label="方式" width="100" />
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 19) }}</template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api'

const list = ref([])
const total = ref(0)
const status = ref(null)
const role = ref('')
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const logDialog = ref(false)
const logs = ref([])
const logUserId = ref('')

const roles = [
  { label: '普通用户', value: 'USER' }, { label: '主播', value: 'ANCHOR' }, { label: '音乐人', value: 'MUSICIAN' },
  { label: 'VIP', value: 'VIP' }, { label: '广告制作人', value: 'ADVERTISER' }, { label: '管理员', value: 'ADMIN' }
]
const grantRoles = roles.filter((r) => r.value !== 'USER')
const roleText = (r) => roles.find((x) => x.value === r)?.label || r

async function load() {
  const res = await userApi.adminPage({ ...query, status: status.value, role: role.value })
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

function reset() {
  query.keyword = ''
  status.value = null
  role.value = ''
  load()
}

async function toggleStatus(row, enabled) {
  await userApi.updateStatus(row.id, enabled ? 0 : 1)
  ElMessage.success(enabled ? '已启用' : '已禁用')
  await load()
}

async function grant(row, r) {
  await userApi.grantRole(row.id, r)
  ElMessage.success('角色已更新')
  await load()
}

async function loadLogs() {
  const res = await userApi.adminLoginLogs({ pageNum: 1, pageSize: 50, userId: logUserId.value || undefined })
  logs.value = res.data?.records || []
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 6px; }
.user-cell { display: flex; align-items: center; gap: 10px; }
.u-name { font-weight: 600; }
.u-sub { font-size: 12px; color: var(--km-text-dim); }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
