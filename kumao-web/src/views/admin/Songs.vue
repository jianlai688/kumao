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
      <el-button plain @click="triggerFolderUpload">文件夹上传</el-button>
      <input
        ref="folderInput"
        type="file"
        webkitdirectory
        directory
        multiple
        class="folder-input"
        @change="onFolderChange"
      />
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

    <el-dialog v-model="folderDialog" title="文件夹批量上传" width="900px" destroy-on-close>
      <div v-if="!folderItems.length" class="folder-empty">
        <p>请选择包含音频文件的文件夹。</p>
        <p class="km-faint">支持 MP3 / WAV / FLAC / M4A / AAC / OGG；文件名建议使用「歌手 - 歌名」，同目录下的 cover.jpg 和同名 .lrc 会一起导入。</p>
      </div>
      <template v-else>
        <div class="folder-summary">
          <span>文件夹：<b>{{ folderName || '未命名文件夹' }}</b></span>
          <span>音频：<b>{{ folderItems.length }}</b> 个</span>
          <span>封面：<b>{{ coverFile?.name || '未找到，可后补' }}</b></span>
        </div>
        <div class="folder-options">
          <label>默认歌手
            <el-select v-model="folderSingerId" filterable size="small" style="width:170px">
              <el-option v-for="s in singers" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
          </label>
          <label>专辑
            <el-input v-model="folderAlbum" size="small" style="width:170px" placeholder="默认使用文件夹名" />
          </label>
          <label>状态
            <el-select v-model="folderStatus" size="small" style="width:110px">
              <el-option label="待审核" :value="0" />
              <el-option label="上架" :value="1" />
              <el-option label="下架" :value="2" />
            </el-select>
          </label>
        </div>

        <el-table :data="folderItems" size="small" max-height="380" class="folder-table">
          <el-table-column width="42">
            <template #default="{ row }"><el-checkbox v-model="row.selected" /></template>
          </el-table-column>
          <el-table-column prop="fileName" label="文件" min-width="200" show-overflow-tooltip />
          <el-table-column label="歌曲名" min-width="150">
            <template #default="{ row }"><el-input v-model="row.name" size="small" /></template>
          </el-table-column>
          <el-table-column label="歌手" width="150">
            <template #default="{ row }"><el-input v-model="row.singerName" size="small" placeholder="默认歌手" /></template>
          </el-table-column>
          <el-table-column label="时长" width="75">
            <template #default="{ row }">{{ row.duration ? `${row.duration}s` : '—' }}</template>
          </el-table-column>
          <el-table-column label="结果" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.result === 'ok'" size="small" type="success">已导入</el-tag>
              <el-tag v-else-if="row.result === 'fail'" size="small" type="danger">失败</el-tag>
              <span v-else class="km-faint">待上传</span>
            </template>
          </el-table-column>
        </el-table>
        <el-progress v-if="folderImporting" :percentage="folderProgress" :stroke-width="8" style="margin-top:12px" />
        <p v-if="folderError" class="folder-error">{{ folderError }}</p>
      </template>
      <template #footer>
        <el-button @click="folderDialog = false">取消</el-button>
        <el-button type="primary" :loading="folderImporting" :disabled="!selectedFolderCount" @click="importFolder">
          上传 {{ selectedFolderCount }} 首
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fileApi, musicApi } from '@/api'

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

const folderInput = ref(null)
const folderDialog = ref(false)
const folderItems = ref([])
const folderName = ref('')
const coverFile = ref(null)
const folderSingerId = ref(null)
const folderAlbum = ref('')
const folderStatus = ref(1)
const folderImporting = ref(false)
const folderProgress = ref(0)
const folderError = ref('')
const selectedFolderCount = computed(() => folderItems.value.filter((item) => item.selected).length)
const AUDIO_EXT = /\.(mp3|wav|flac|m4a|aac|ogg)$/i
const IMAGE_EXT = /\.(jpg|jpeg|png|webp)$/i
const LRC_EXT = /\.lrc$/i

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

function stripExt(name) {
  return String(name || '').replace(/\.[^.]+$/, '')
}

function triggerFolderUpload() {
  folderInput.value?.click()
}

function loadDuration(item) {
  const url = URL.createObjectURL(item.file)
  const audio = new Audio()
  audio.preload = 'metadata'
  const cleanup = () => URL.revokeObjectURL(url)
  audio.onloadedmetadata = () => {
    if (Number.isFinite(audio.duration)) item.duration = Math.round(audio.duration)
    cleanup()
  }
  audio.onerror = cleanup
  audio.src = url
}

function onFolderChange(event) {
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  const audioFiles = files.filter((file) => AUDIO_EXT.test(file.name) && !file.name.startsWith('.'))
  if (!audioFiles.length) {
    ElMessage.warning('文件夹中没有找到支持的音频文件')
    return
  }
  const images = files.filter((file) => IMAGE_EXT.test(file.name) && !file.name.startsWith('.'))
  coverFile.value = images.find((file) => /^(cover|folder|front|album|专辑)/i.test(stripExt(file.name))) || images[0] || null
  const lrcMap = new Map(
    files.filter((file) => LRC_EXT.test(file.name)).map((file) => [stripExt(file.name).toLowerCase(), file])
  )
  const relative = audioFiles[0].webkitRelativePath || audioFiles[0].name
  folderName.value = relative.includes('/') ? relative.split('/')[0] : stripExt(audioFiles[0].name)
  folderAlbum.value = folderName.value
  folderSingerId.value = singers.value[0]?.id || null
  folderStatus.value = 1
  folderError.value = ''
  folderProgress.value = 0
  folderItems.value = audioFiles.map((file) => {
    const base = stripExt(file.name).replace(/^\d{1,3}[.\s_-]+/, '').trim()
    const parts = base.split(/\s*[-–—]\s*/).map((part) => part.trim()).filter(Boolean)
    let name = base
    let singerName = ''
    if (parts.length >= 2) {
      const first = parts[0]
      const second = parts.slice(1).join(' - ')
      const firstIsSinger = singers.value.some((s) => s.name.toLowerCase() === first.toLowerCase())
      const secondIsSinger = singers.value.some((s) => s.name.toLowerCase() === second.toLowerCase())
      if (secondIsSinger && !firstIsSinger) {
        name = first
        singerName = second
      } else {
        name = second
        singerName = first
      }
    }
    return {
      file,
      fileName: file.name,
      name,
      singerName,
      duration: 0,
      lyricFile: lrcMap.get(stripExt(file.name).toLowerCase()) || null,
      selected: true,
      result: '',
      error: ''
    }
  })
  folderDialog.value = true
  folderItems.value.forEach(loadDuration)
}

async function importFolder() {
  if (!folderSingerId.value) {
    folderError.value = '请选择默认歌手'
    return
  }
  const selected = folderItems.value.filter((item) => item.selected)
  if (!selected.length) {
    folderError.value = '请至少选择一首歌曲'
    return
  }
  folderImporting.value = true
  folderProgress.value = 0
  folderError.value = ''
  try {
    let coverUrl = ''
    if (coverFile.value) {
      try {
        const coverRes = await fileApi.upload(coverFile.value, 'cover')
        coverUrl = coverRes.data?.url || ''
      } catch (e) {
        folderError.value = '封面超过大小限制或上传失败，已跳过封面'
      }
    }
    for (const [index, item] of selected.entries()) {
      item.result = ''
      try {
        if (!item.name.trim()) throw new Error('歌曲名为空')
        if (item.file.size > 100 * 1024 * 1024) throw new Error('音频文件超过 100MB')
        const audioRes = await fileApi.upload(item.file, 'audio')
        const audioUrl = audioRes.data?.url
        if (!audioUrl) throw new Error('音频上传失败')
        let lyricUrl = ''
        if (item.lyricFile) {
          const lyricRes = await fileApi.upload(item.lyricFile, 'lyric')
          lyricUrl = lyricRes.data?.url || ''
        }
        const defaultSinger = singers.value.find((s) => s.id === folderSingerId.value)
        const inputSinger = item.singerName.trim()
        const matchedSinger = inputSinger
          ? singers.value.find((s) => s.name.toLowerCase() === inputSinger.toLowerCase())
          : null
        const singer = matchedSinger || defaultSinger
        await musicApi.createSong({
          name: item.name.trim(),
          singerId: singer?.id || null,
          singerName: singer?.name || inputSinger,
          albumName: folderAlbum.value.trim() || folderName.value,
          coverUrl,
          audioUrl,
          lyricUrl,
          duration: item.duration || 0,
          language: '国语',
          style: '流行',
          region: '国内',
          vipOnly: false,
          status: folderStatus.value
        })
        item.result = 'ok'
      } catch (e) {
        item.result = 'fail'
        item.error = e?.message || '导入失败'
      }
      folderProgress.value = Math.round(((index + 1) / selected.length) * 100)
    }
    const success = selected.filter((item) => item.result === 'ok').length
    const failed = selected.length - success
    if (success) ElMessage.success(`成功导入 ${success} 首歌曲`)
    if (failed) {
      folderError.value = `${failed} 首导入失败，请检查文件名、音频格式或文件大小`
    } else {
      folderDialog.value = false
      await load()
    }
  } finally {
    folderImporting.value = false
  }
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
.folder-input { display: none; }
.folder-empty { padding: 24px 4px; text-align: center; color: var(--km-text-dim); }
.folder-empty p { margin: 0 0 8px; }
.folder-summary { display: flex; flex-wrap: wrap; gap: 18px; padding: 10px 12px; margin-bottom: 12px; border-radius: 8px; background: var(--km-bg-soft); color: var(--km-text-dim); font-size: 12px; }
.folder-summary b { color: var(--km-text); font-weight: 600; }
.folder-options { display: flex; flex-wrap: wrap; align-items: center; gap: 14px; margin-bottom: 12px; color: var(--km-text-dim); font-size: 12px; }
.folder-options label { display: inline-flex; align-items: center; gap: 8px; }
.folder-table { width: 100%; }
.folder-error { margin: 10px 0 0; color: var(--km-negative); font-size: 12px; }
.song-cell { display: flex; align-items: center; gap: 10px; }
.cell-cover { width: 40px; height: 40px; border-radius: 8px; object-fit: cover; }
.cell-name { font-weight: 600; display: flex; align-items: center; gap: 6px; }
.cell-sub { font-size: 12px; color: var(--km-text-dim); }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
