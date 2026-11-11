<template>
  <div>
    <div class="toolbar km-card">
      <el-input v-model="query.keyword" placeholder="MV 名称" clearable style="width:200px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <div style="flex:1"></div>
      <el-button type="primary" @click="openDialog()">上传 MV</el-button>
    </div>

    <el-table :data="list" style="width:100%; margin-top:14px">
      <el-table-column label="MV" min-width="250">
        <template #default="{ row }">
          <div class="song-cell">
            <img :src="row.coverUrl" class="cell-cover" />
            <div>
              <div class="cell-name">{{ row.name }} <el-tag v-if="row.vipOnly" size="small" type="warning" effect="dark">VIP 高清</el-tag></div>
              <div class="cell-sub">{{ row.singerName }} · {{ row.quality }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="duration" label="时长(秒)" width="110" />
      <el-table-column prop="playCount" label="播放" width="110" />
      <el-table-column label="审核状态" width="105">
        <template #default="{ row }">
          <el-tag size="small" :type="row.auditStatus === 'PENDING' ? 'warning' : row.auditStatus === 'REJECTED' ? 'danger' : 'success'">
            {{ row.auditStatus === 'PENDING' ? '待审核' : row.auditStatus === 'REJECTED' ? '未通过' : '已通过' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="上架状态" width="95">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="255">
        <template #default="{ row }">
          <template v-if="row.auditStatus === 'PENDING'">
            <el-button size="small" type="success" @click="audit(row, true)">通过</el-button>
            <el-button size="small" type="danger" @click="audit(row, false)">拒绝</el-button>
          </template>
          <template v-else>
            <el-button size="small" text @click="openDialog(row)">编辑</el-button>
            <el-button size="small" text :type="row.status === 1 ? 'warning' : 'success'" @click="toggle(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button>
            <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="query.pageSize"
                     v-model:current-page="query.pageNum" @current-change="load" />
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑 MV' : '上传 MV'" width="620px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="MV 名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="关联歌曲"><el-input v-model="form.songId" placeholder="可选，歌曲 ID，如 3001" /></el-form-item>
        <el-form-item label="关联歌手">
          <el-select v-model="form.singerId" filterable style="width:100%" @change="onSinger">
            <el-option v-for="s in singers" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面"><el-input v-model="form.coverUrl" /></el-form-item>
        <el-form-item label="视频文件">
          <MediaDropzone
            v-model="form.videoUrl"
            kind="video"
            biz-type="video"
            title="上传 MV 视频"
            hint="MP4 / MOV / FLV / MKV · 最大 500MB"
            :max-size-mb="500"
          />
          <el-input v-model="form.videoUrl" placeholder="也可直接填写视频地址" style="margin-top:8px" />
        </el-form-item>
        <el-form-item label="时长(秒)"><el-input-number v-model="form.duration" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="清晰度">
          <el-radio-group v-model="form.quality">
            <el-radio-button value="SD">SD</el-radio-button>
            <el-radio-button value="HD">HD</el-radio-button>
            <el-radio-button value="UHD">UHD</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="VIP 专属"><el-switch v-model="form.vipOnly" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status"><el-radio-button :value="1">上架</el-radio-button><el-radio-button :value="0">下架</el-radio-button></el-radio-group>
        </el-form-item>
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
import MediaDropzone from '@/components/studio/MediaDropzone.vue'

const list = ref([])
const singers = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const dialog = ref(false)
const form = reactive({
  id: null, songId: null, name: '', singerId: null, singerName: '', coverUrl: '', videoUrl: '', duration: 240,
  quality: 'HD', vipOnly: false, status: 1, description: ''
})

async function load() {
  const res = await musicApi.adminMvs(query)
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

function openDialog(row) {
  Object.assign(form, row ? { ...row } : {
    id: null, songId: null, name: '', singerId: null, singerName: '', coverUrl: '', videoUrl: '', duration: 240,
    quality: 'HD', vipOnly: false, status: 1, description: ''
  })
  dialog.value = true
}

function onSinger(id) {
  form.singerName = singers.value.find((s) => s.id === id)?.name || ''
}

async function save() {
  if (!form.name || !form.videoUrl) return ElMessage.warning('请填写名称与视频地址')
  const payload = { ...form }
  delete payload.id
  if (form.id) await musicApi.updateMv(form.id, payload)
  else await musicApi.createMv(payload)
  ElMessage.success('保存成功')
  dialog.value = false
  await load()
}

async function audit(row, approved) {
  let reason = ''
  if (!approved) {
    try {
      const result = await ElMessageBox.prompt('请输入拒绝原因', '拒绝 MV', {
        confirmButtonText: '确认拒绝',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：视频文件无法播放，请重新上传'
      })
      reason = result.value || ''
    } catch (e) {
      return
    }
  }
  await musicApi.auditMv(row.id, approved, reason)
  ElMessage.success(approved ? 'MV 审核已通过' : '已拒绝 MV')
  await load()
}

async function toggle(row) {
  await musicApi.updateMvStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('操作成功')
  await load()
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除该 MV 吗？', '提示', { type: 'warning' })
    await musicApi.deleteMv(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) { /* cancel */ }
}

onMounted(async () => {
  await load()
  const s = await musicApi.singers({ pageNum: 1, pageSize: 100 })
  singers.value = s.data?.records || []
})
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.song-cell { display: flex; align-items: center; gap: 10px; }
.cell-cover { width: 66px; height: 40px; border-radius: 6px; object-fit: cover; }
.cell-name { font-weight: 600; display: flex; align-items: center; gap: 6px; }
.cell-sub { font-size: 12px; color: var(--km-text-dim); }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
