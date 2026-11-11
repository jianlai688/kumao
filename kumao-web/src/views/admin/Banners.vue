<template>
  <div>
    <div class="toolbar km-card">
      <b>轮播图管理</b>
      <span class="km-faint">共 {{ list.length }} 张轮播</span>
      <div style="flex:1"></div>
      <el-button type="primary" @click="openDialog()">新增轮播</el-button>
    </div>
    <el-table :data="list">
      <el-table-column label="图片" width="180">
        <template #default="{ row }"><img :src="row.imageUrl" class="banner-img" /></template>
      </el-table-column>
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="linkUrl" label="跳转链接" />
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
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="图片地址"><el-input v-model="form.imageUrl" /></el-form-item>
        <el-form-item label="跳转链接"><el-input v-model="form.linkUrl" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
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
import { musicApi } from '@/api'

const list = ref([])
const dialog = ref(false)
const form = reactive({ id: null, title: '', imageUrl: '', linkUrl: '', sort: 0, status: 1 })

async function load() {
  const res = await musicApi.banners()
  list.value = res.data || []
}

function openDialog(row) {
  Object.assign(form, row ? { ...row } : { id: null, title: '', imageUrl: '', linkUrl: '', sort: 0, status: 1 })
  dialog.value = true
}

async function save() {
  if (form.id) await request.put(`/music/banners/admin/${form.id}`, form)
  else await request.post('/music/banners/admin', form)
  ElMessage.success('保存成功')
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
.banner-img { width: 160px; height: 54px; object-fit: cover; border-radius: 6px; }
</style>
