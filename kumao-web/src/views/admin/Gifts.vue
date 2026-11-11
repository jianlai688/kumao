<template>
  <div>
    <div class="toolbar km-card">
      <b>礼物配置</b>
      <span class="km-faint">共 {{ list.length }} 个礼物</span>
      <div style="flex:1"></div>
      <el-button type="primary" @click="openDialog()">新增礼物</el-button>
    </div>

    <el-table :data="list">
      <el-table-column prop="icon" label="图标" width="90" />
      <el-table-column prop="name" label="名称" width="140" />
      <el-table-column prop="price" label="价格（虚拟币）" width="140" />
      <el-table-column prop="effect" label="特效" min-width="180" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" text @click="openDialog(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="query.pageSize"
                     v-model:current-page="query.pageNum" @current-change="load" />
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑礼物' : '新增礼物'" width="440px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="图标"><el-input v-model="form.icon" placeholder="emoji 或图片地址" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="form.price" :min="1" /></el-form-item>
        <el-form-item label="特效"><el-input v-model="form.effect" /></el-form-item>
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
import { liveApi } from '@/api'

const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 20 })
const dialog = ref(false)
const form = reactive({ id: null, name: '', icon: '🎁', price: 1, effect: '', status: 1 })

async function load() {
  const res = await liveApi.adminGifts(query)
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

function openDialog(row) {
  Object.assign(form, row ? { ...row } : { id: null, name: '', icon: '🎁', price: 1, effect: '', status: 1 })
  dialog.value = true
}

async function save() {
  if (!form.name) return ElMessage.warning('请输入礼物名称')
  if (form.id) await liveApi.updateGift(form.id, form)
  else await liveApi.createGift(form)
  ElMessage.success('保存成功')
  dialog.value = false
  await load()
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除该礼物吗？', '提示', { type: 'warning' })
    await liveApi.deleteGift(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) { /* cancel */ }
}

onMounted(load)
</script>

<style scoped>
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
