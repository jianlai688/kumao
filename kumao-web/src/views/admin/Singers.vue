<template>
  <div>
    <div class="toolbar km-card">
      <el-input v-model="query.keyword" placeholder="歌手名" clearable style="width:200px" @keyup.enter="load" />
      <el-select v-model="region" placeholder="地区" clearable style="width:130px">
        <el-option label="国内" value="国内" /><el-option label="国外" value="国外" /><el-option label="粤语" value="粤语" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <div style="flex:1"></div>
      <span class="km-faint" style="font-size:12px">只有已认证音乐人可提交歌手认证</span>
      <el-button type="primary" @click="openDialog()">新增歌手</el-button>
    </div>

    <el-table :data="list" style="width:100%; margin-top:14px">
      <el-table-column label="歌手" min-width="200">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar :size="40" :src="row.avatar">{{ row.name.slice(0, 1) }}</el-avatar>
            <div>
              <div class="u-name">{{ row.name }}</div>
              <div class="u-sub">{{ row.region }} · {{ row.gender }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="intro" label="简介" min-width="260" show-overflow-tooltip />
      <el-table-column prop="fansCount" label="粉丝" width="120" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <span :class="['singer-status', row.status === 0 ? 'pending' : row.status === 2 ? 'rejected' : 'approved']">
            {{ row.status === 0 ? '待审核' : row.status === 2 ? '未通过' : '已认证' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button size="small" type="success" @click="audit(row, true)">通过</el-button>
            <el-button size="small" type="danger" @click="audit(row, false)">拒绝</el-button>
          </template>
          <template v-else>
            <el-button size="small" text @click="openDialog(row)">编辑</el-button>
            <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="query.pageSize"
                     v-model:current-page="query.pageNum" @current-change="load" />
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑歌手' : '新增歌手'" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="歌手名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="头像">
          <AvatarUploader v-model="form.avatar" biz-type="avatar" fallback-text="歌" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender"><el-radio-button value="男">男</el-radio-button><el-radio-button value="女">女</el-radio-button></el-radio-group>
        </el-form-item>
        <el-form-item label="地区">
          <el-select v-model="form.region" style="width:100%">
            <el-option v-for="r in ['国内','国外','粤语']" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="简介"><el-input v-model="form.intro" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { musicApi } from '@/api'
import AvatarUploader from '@/components/AvatarUploader.vue'

const list = ref([])
const total = ref(0)
const region = ref('')
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const dialog = ref(false)
const form = reactive({ id: null, name: '', avatar: '', gender: '男', region: '国内', intro: '', status: 1 })

async function load() {
  const res = await musicApi.adminSingers({ ...query, region: region.value })
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

function openDialog(row) {
  Object.assign(form, row ? { ...row } : { id: null, name: '', avatar: '', gender: '男', region: '国内', intro: '', status: 1 })
  dialog.value = true
}

async function save() {
  if (!form.name) return ElMessage.warning('请输入歌手名')
  const payload = { ...form }
  delete payload.id
  if (form.id) await musicApi.updateSinger(form.id, payload)
  else await musicApi.createSinger(payload)
  ElMessage.success('保存成功')
  dialog.value = false
  await load()
}

async function audit(row, approved) {
  let reason = ''
  if (!approved) {
    try {
      const result = await ElMessageBox.prompt('请输入拒绝原因', '拒绝歌手认证', {
        confirmButtonText: '确认拒绝',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：资料不完整，请补充代表作品'
      })
      reason = result.value || ''
    } catch (e) {
      return
    }
  }
  await musicApi.auditSinger(row.id, approved, reason)
  ElMessage.success(approved ? '歌手认证已通过' : '已拒绝歌手认证')
  await load()
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除该歌手吗？', '提示', { type: 'warning' })
    await musicApi.deleteSinger(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) { /* cancel */ }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.user-cell { display: flex; align-items: center; gap: 10px; }
.u-name { font-weight: 600; }
.u-sub { font-size: 12px; color: var(--km-text-dim); }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>

<style scoped>
.singer-status { display: inline-flex; align-items: center; height: 22px; padding: 0 8px; border-radius: 999px; font-size: 11px; }
.singer-status.approved { color: #72e6a2; background: rgba(114, 230, 162, .1); }
.singer-status.pending { color: #f6c177; background: rgba(246, 193, 119, .1); }
.singer-status.rejected { color: #ff8fa2; background: rgba(255, 143, 162, .1); }
</style>
