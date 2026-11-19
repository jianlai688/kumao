<template>
  <div class="id-card-preview" :class="{ compact }">
    <div class="id-card-shots">
      <el-image
        v-if="front"
        class="id-card-shot"
        :src="front"
        :preview-src-list="previewList"
        :initial-index="0"
        fit="cover"
        preview-teleported
        :alt="`${label}人像面`"
      >
        <template #error><span class="id-card-missing">加载失败</span></template>
      </el-image>
      <div v-else class="id-card-shot id-card-missing">正面<br />未上传</div>

      <el-image
        v-if="back"
        class="id-card-shot"
        :src="back"
        :preview-src-list="previewList"
        :initial-index="front ? 1 : 0"
        fit="cover"
        preview-teleported
        :alt="`${label}国徽面`"
      >
        <template #error><span class="id-card-missing">加载失败</span></template>
      </el-image>
      <div v-else class="id-card-shot id-card-missing">反面<br />未上传</div>
    </div>
    <div v-if="number" class="id-card-number" :title="number">{{ number }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  front: { type: String, default: '' },
  back: { type: String, default: '' },
  number: { type: String, default: '' },
  label: { type: String, default: '身份证' },
  compact: { type: Boolean, default: false }
})

const previewList = computed(() => [props.front, props.back].filter(Boolean))
</script>

<style scoped>
.id-card-preview { display: flex; flex-direction: column; gap: 5px; }
.id-card-shots { display: flex; gap: 5px; }
.id-card-shot {
  width: 68px;
  height: 44px;
  flex: none;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 6px;
  border: 1px solid var(--km-border);
  background: rgba(255, 255, 255, .04);
  color: var(--km-text-faint);
  font-size: 9px;
  line-height: 1.25;
  text-align: center;
}
.id-card-shot :deep(img) { width: 100%; height: 100%; object-fit: cover; }
.id-card-missing { color: var(--km-text-faint); font-size: 9px; }
.id-card-number { max-width: 160px; overflow: hidden; color: var(--km-text-faint); font-size: 9.5px; font-variant-numeric: tabular-nums; white-space: nowrap; text-overflow: ellipsis; }
.compact .id-card-shot { width: 56px; height: 36px; }
</style>
