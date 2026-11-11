<template>
  <div>
    <div class="toolbar km-card">
      <el-input v-model="query.keyword" placeholder="歌曲名" clearable style="width:200px" @keyup.enter="load" />
      <el-select v-model="region" placeholder="地区" clearable style="width:130px">
        <el-option label="国内" value="国内" /><el-option label="国外" value="国外" /><el-option label="粤语" value="粤语" />
      </el-select>
      <el-select v-model="status" placeholder="状态" clearable style="width:130px">
        <el-option label="待审核" :value="0" /><el-option label="已上架" :value="1" /><el-option label="已下架" :value="2" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <div style="flex:1"></div>
      <el-button type="primary" @click="openDialog()">新增歌曲</el-button>
    </div>

    <el-table :data="list" style="width:100%; margin-top:14px">
      <el-table-column label="歌曲" min-width="250">
        <template #default="{ row }">
          <div class="song-cell">
            <img :src="row.coverUrl" class="cell-cover" />
            <div>
              <div class="cell-name">{{ row.name }} <el-tag v-if="row.vipOnly" size="small" type="warning" effect="dark">VIP</el-tag></div>
              <div class="cell-sub">{{ row.singerName }} · {{ row.albumName || '单曲' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="language" label="语种" width="90" />
      <el-table-column prop="style" label="风格" width="90" />
      <el-table-column prop="region" label="地区" width="90" />
      <el-table-column prop="playCount" label="播放" width="110" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : row.status === 0 ? 'warning' : 'info'">
            {{ row.status === 1 ? '已上架' : row.status === 0 ? '待审核' : '已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230">
        <template #default="{ row }">
          <el-button size="small" text @click="openDialog(row)">编辑</el-button>
          <el-button size="small" text :type="row.status === 1 ? 'warning' : 'success'" @click="toggle(row)">
            {{ row.status === 1 ? '下架' : '上架' }}
          </el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="query.pageSize"
                     v-model:current-page="query.pageNum" @current-change="load" />
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑歌曲' : '新增歌曲'" width="680px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="歌曲名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="歌手">
          <el-select v-model="form.singerId" filterable style="width:100%" @change="onSingerChange">
            <el-option v-for="s in singers" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="专辑名称"><el-input v-model="form.albumName" /></el-form-item>
        <el-form-item label="封面"><el-input v-model="form.coverUrl" /></el-form-item>
        <el-form-item label="音频地址"><el-input v-model="form.audioUrl" /></el-form-item>
        <el-form-item label="LRC 歌词地址"><el-input v-model="form.lyricUrl" /></el-form-item>
        <el-row>
          <el-col :span="12"><el-form-item label="时长(秒)"><el-input-number v-model="form.duration" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="发行日期"><el-date-picker v-model="form.releaseDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="8"><el-form-item label="语种">
            <el-select v-model="form.language"><el-option v-for="l in ['国语','粤语','英语','日语','韩语']" :key="l" :label="l" :value="l" /></el-select>
          </el-form-item></el-col>
          <el-col :span="8"><el-form-item label="风格">
            <el-select v-model="form.style"><el-option v-for="s in ['流行','摇滚','民谣','电子','说唱','古风']" :key="s" :label="s" :value="s" /></el-select>
          </el-form-item></el-col>
          <el-col :span="8"><el-form-item label="地区">
            <el-select v-model="form.region"><el-option v-for="r in ['国内','国外','粤语']" :key="r" :label="r" :value="r" /></el-select>
          </el-form-item></el-col>
        </el-row>
        <el-form-item label="VIP 专属"><el-switch v-model="form.vipOnly" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button :value="0">待审核</el-radio-button>
            <el-radio-button :value="1">上架</el-radio-button>
            <el-radio-button :value="2">下架</el-radio-button>
          </el-radio-group>
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

const list = ref([])
const singers = ref([])
const total = ref(0)
const region = ref('')
const status = ref(null)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const dialog = ref(false)
const form = reactive({
  id: null, name: '', singerId: null, singerName: '', albumName: '', coverUrl: '', audioUrl: '',
  lyricUrl: '', duration: 240, releaseDate: '', language: '国语', style: '流行', region: '国内',
  vipOnly: false, status: 1
})

async function load() {
  const res = await musicApi.songs({ ...query, region: region.value, status: status.value })
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

function openDialog(row) {
  Object.assign(form, row ? { ...row } : {
    id: null, name: '', singerId: null, singerName: '', albumName: '', coverUrl: '', audioUrl: '',
    lyricUrl: '', duration: 240, releaseDate: '', language: '国语', style: '流行', region: '国内',
    vipOnly: false, status: 1
  })
  dialog.value = true
}

function onSingerChange(id) {
  form.singerName = singers.value.find((s) => s.id === id)?.name || ''
}

async function save() {
  if (!form.name) return ElMessage.warning('请输入歌曲名称')
  const payload = { ...form }
  delete payload.id
  if (form.id) await musicApi.updateSong(form.id, payload)
  else await musicApi.createSong(payload)
  ElMessage.success('保存成功')
  dialog.value = false
  await load()
}

async function toggle(row) {
  await musicApi.updateSongStatus(row.id, row.status === 1 ? 2 : 1)
  ElMessage.success('操作成功')
  await load()
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除该歌曲吗？', '提示', { type: 'warning' })
    await musicApi.deleteSong(row.id)
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
.cell-cover { width: 40px; height: 40px; border-radius: 8px; object-fit: cover; }
.cell-name { font-weight: 600; display: flex; align-items: center; gap: 6px; }
.cell-sub { font-size: 12px; color: var(--km-text-dim); }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
