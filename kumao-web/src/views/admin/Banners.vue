<template>
  <div>
    <div class="toolbar km-card">
      <div class="toolbar-copy">
        <b>首页轮播</b>
        <span class="km-faint">启用后按排序展示在「发现音乐」顶部，建议图片 1200 × 400</span>
      </div>
      <div style="flex:1"></div>
      <el-button type="primary" @click="openDialog()">新增轮播</el-button>
    </div>
    <el-table :data="list" empty-text="暂无轮播，新增后会自动展示在首页顶部">
      <el-table-column label="图片" width="200">
        <template #default="{ row }"><img :src="row.imageUrl" class="banner-img" alt="" /></template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="160" />
      <el-table-column prop="linkUrl" label="跳转链接" min-width="160" />
      <el-table-column prop="sort" label="排序" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" text @click="openDialog(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑轮播' : '新增轮播'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题"><el-input v-model="form.title" placeholder="用于后台识别和无障碍描述" /></el-form-item>
        <el-form-item label="图片">
          <div class="img-field">
            <el-input v-model="form.imageUrl" placeholder="图片地址，或点右侧上传" />
            <el-upload :show-file-list="false" :before-upload="uploadImage" accept="image/*">
              <el-button :loading="uploading">上传图片</el-button>
            </el-upload>
          </div>
          <p class="field-tip">建议使用 1200 × 400 的横版图片；首页会以左右居中裁切展示。</p>
          <img v-if="form.imageUrl" :src="form.imageUrl" class="img-preview" alt="" />
        </el-form-item>
        <el-form-item label="跳转链接"><el-input v-model="form.linkUrl" placeholder="如 /song/3001、/vip、/live" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /><span class="field-tip inline">数字越小越靠前</span></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status"><el-radio-button :value="1">启用</el-radio-button><el-radio-button :value="0">停用</el-radio-button></el-radio-group>
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
import request from '@/api/request'
import { musicApi, fileApi } from '@/api'

const list = ref([])
const dialog = ref(false)
const uploading = ref(false)
const form = reactive({ id: null, title: '', imageUrl: '', linkUrl: '', sort: 0, status: 1 })

async function uploadImage(file) {
  uploading.value = true
  try {
    const res = await fileApi.upload(file, 'banner')
    const url = res?.data?.url || res?.data?.fileUrl || ''
    if (!url) {
      ElMessage.warning('上传成功但没有返回图片地址')
      return false
    }
    form.imageUrl = url
    ElMessage.success('图片已上传')
  } catch (e) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
  return false
}

async function load() {
  const res = await musicApi.bannersAdmin()
  list.value = res.data || []
}

function nextSort() {
  return list.value.reduce((max, item) => Math.max(max, Number(item.sort) || 0), 0) + 1
}

function openDialog(row) {
  Object.assign(form, row ? { ...row } : { id: null, title: '', imageUrl: '', linkUrl: '', sort: nextSort(), status: 1 })
  dialog.value = true
}

async function save() {
  if (!form.imageUrl.trim()) {
    ElMessage.warning('请先上传或填写轮播图片')
    return
  }
  if (!form.title.trim()) {
    ElMessage.warning('请填写轮播标题')
    return
  }
  if (form.id) await request.put(`/music/banners/admin/${form.id}`, form)
  else await request.post(`/music/banners/admin`, form)
  ElMessage.success('保存成功，首页轮播已同步')
  dialog.value = false
  await load()
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除该轮播吗？', '提示', { type: 'warning' })
    await request.delete(`/music/banners/admin/${row.id}`)
    ElMessage.success('已删除')
    await load()
  } catch (e) { /* cancel */ }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.toolbar-copy { display: flex; align-items: baseline; gap: 10px; flex-wrap: wrap; }
.toolbar-copy .km-faint { font-size: 12px; }
.banner-img { width: 180px; height: 60px; object-fit: cover; border-radius: 7px; background: #f3edf8; }
.img-field { display: flex; gap: 10px; width: 100%; }
.field-tip { width: 100%; margin: 7px 0 0; color: var(--km-text-faint); font-size: 11px; line-height: 1.6; }
.field-tip.inline { width: auto; margin: 0 0 0 9px; }
.img-preview { margin-top: 8px; width: 360px; max-width: 100%; aspect-ratio: 3 / 1; object-fit: cover; border-radius: 8px; border: 1px solid var(--km-border); background: #f3edf8; }
</style>
