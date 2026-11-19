<template>
  <el-dialog :model-value="modelValue" :title="config.title" width="480px" @update:model-value="close">
    <el-form label-position="top">
      <template v-for="f in config.fields" :key="f.key">
        <el-form-item v-if="f.type === 'image'" :label="f.label">
          <div class="image-field">
            <el-image v-if="form[f.key]" :src="form[f.key]" fit="cover" class="image-preview">
              <template #error>
                <div class="image-empty">加载失败</div>
              </template>
            </el-image>
            <div v-else class="image-preview image-empty">未上传</div>
            <div class="image-actions">
              <el-upload :show-file-list="false" :http-request="(o) => uploadImage(o, f)" accept="image/*">
                <el-button size="small" :loading="uploading === f.key">
                  {{ form[f.key] ? '更换图片' : '选择图片' }}
                </el-button>
              </el-upload>
              <el-button v-if="form[f.key]" size="small" text type="danger" @click="form[f.key] = ''">移除</el-button>
            </div>
            <div class="image-hint">支持 JPG / PNG / WebP，单张不超过 10MB，上传后自动填入地址</div>
          </div>
        </el-form-item>
        <el-form-item v-else :label="f.label">
          <el-input v-model="form[f.key]" :maxlength="f.max || 60" :placeholder="f.placeholder" />
        </el-form-item>
      </template>
      <el-form-item :label="config.introLabel">
        <el-input v-model="form.intro" type="textarea" :rows="3" :placeholder="config.introPlaceholder" maxlength="200" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="close(false)">取消</el-button>
      <el-button type="primary" round :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { adApi, fileApi, musicianApi, userApi } from '@/api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  type: { type: String, default: 'musician' },
  record: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue', 'saved'])

const saving = ref(false)
const uploading = ref('')
const form = reactive({})

async function uploadImage(options, field) {
  const file = options && options.file
  if (!file) return
  if (!/^image\//.test(file.type || '')) {
    ElMessage.error('请选择图片文件（JPG / PNG / WebP）')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片不能超过 10MB')
    return
  }
  uploading.value = field.key
  try {
    const res = await fileApi.upload(file, field.key === 'coverUrl' ? 'cover' : 'avatar')
    const url = res?.data?.url || res?.data?.fileUrl || ''
    if (!url) {
      ElMessage.warning('上传成功但没有返回图片地址')
      return
    }
    form[field.key] = url
    ElMessage.success('图片已上传')
  } catch (error) {
    ElMessage.error(error?.message || '图片上传失败')
  } finally {
    uploading.value = ''
  }
}

const CONFIG = {
  anchor: {
    title: '主播个人信息',
    introLabel: '主播简介',
    introPlaceholder: '介绍一下你的直播内容、开播时间',
    fields: [
      { key: 'anchorName', label: '主播艺名', max: 20, placeholder: '如 甜心主播' },
      { key: 'liveCategory', label: '直播分类', max: 20, placeholder: '如 弹唱 / 电台' },
      { key: 'coverUrl', label: '直播间封面', type: 'image', max: 200, placeholder: '封面图片地址' },
      { key: 'realName', label: '实名认证（不可修改）', max: 20 }
    ]
  },
  musician: {
    title: '音乐人个人信息',
    introLabel: '艺人简介',
    introPlaceholder: '介绍你的音乐风格、创作经历',
    fields: [
      { key: 'stageName', label: '艺名', max: 20, placeholder: '如 木子' },
      { key: 'style', label: '音乐风格', max: 20, placeholder: '如 民谣' },
      { key: 'representative', label: '代表作', max: 40, placeholder: '如 城市夜归人' },
      { key: 'avatar', label: '头像', type: 'image', max: 200, placeholder: '头像图片地址' }
    ]
  },
  advertiser: {
    title: '广告商个人信息',
    introLabel: '主体介绍',
    introPlaceholder: '介绍你的公司或投放需求',
    fields: [
      { key: 'companyName', label: '主体名称', max: 40, placeholder: '如 酷猫广告工作室' },
      { key: 'adType', label: '广告类型', max: 20, placeholder: '如 视频广告' },
      { key: 'representative', label: '联系人', max: 20, placeholder: '如 张三' },
      { key: 'avatar', label: '头像', type: 'image', max: 200, placeholder: '头像图片地址' }
    ]
  }
}

const config = computed(() => CONFIG[props.type] || CONFIG.musician)

function fill() {
  Object.keys(form).forEach((k) => delete form[k])
  ;(config.value.fields || []).forEach((f) => { form[f.key] = props.record?.[f.key] || '' })
  form.intro = props.record?.intro || ''
}

watch(
  () => props.modelValue,
  (open) => { if (open) fill() }
)

function close(v = false) {
  emit('update:modelValue', v)
}

async function save() {
  saving.value = true
  try {
    if (props.type === 'anchor') {
      await userApi.updateAnchorProfile({
        anchorName: form.anchorName,
        liveCategory: form.liveCategory,
        coverUrl: form.coverUrl,
        intro: form.intro
      })
    } else if (props.type === 'musician') {
      await musicianApi.updateMine({
        stageName: form.stageName,
        style: form.style,
        representative: form.representative,
        avatar: form.avatar,
        intro: form.intro
      })
    } else {
      await adApi.updateMine({
        companyName: form.companyName,
        adType: form.adType,
        representative: form.representative,
        avatar: form.avatar,
        intro: form.intro
      })
    }
    ElMessage.success('资料已保存')
    emit('saved')
    close(false)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.image-field { display: flex; flex-direction: column; gap: 8px; }
.image-preview {
  width: 96px; height: 96px; border-radius: 12px; overflow: hidden;
  border: 1px solid var(--km-border, rgba(255, 255, 255, .12));
  background: rgba(255, 255, 255, .03);
}
.image-empty { display: grid; place-items: center; font-size: 12px; color: var(--km-text-faint, #8b8b93); }
.image-actions { display: flex; align-items: center; gap: 8px; }
.image-hint { font-size: 11px; color: var(--km-text-faint, #8b8b93); }
</style>
