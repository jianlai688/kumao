<template>
  <el-dialog :model-value="modelValue" title="主题设置" width="540px"
             @update:model-value="$emit('update:modelValue', $event)">
    <div class="theme-block">
      <div class="block-title">预设主题</div>
      <div class="swatches">
        <button v-for="p in presets" :key="p.key" type="button"
                :class="['swatch', { on: current.key === p.key }]" @click="pick(p)">
          <span class="dot" :style="{ background: p.accent }"></span>
          <span class="name">{{ p.name }}</span>
        </button>
      </div>
    </div>

    <div class="theme-block">
      <div class="block-title">自定义颜色</div>
      <div class="custom-row">
        <el-color-picker v-model="current.accent" @change="markCustom" />
        <span class="hint">挑一个喜欢的颜色，按钮、高亮、渐变都会跟着变</span>
      </div>
    </div>

    <div class="preview" :style="{ '--pv': current.accent }">
      <span class="pv-label">效果预览</span>
      <div class="pv-card">
        <span class="pv-dot"></span>
        <div class="pv-copy">
          <b>酷猫音乐</b>
          <small>正在播放 · 主题预览</small>
        </div>
        <span class="pv-btn">立即播放</span>
      </div>
    </div>

    <template #footer>
      <el-button @click="reset">恢复默认</el-button>
      <el-button type="primary" @click="apply">应用主题</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { THEME_PRESETS, applyTheme, loadTheme, saveTheme } from '@/utils/theme'

defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])

const presets = THEME_PRESETS
const current = ref({ ...loadTheme() })

watch(() => current.value.accent, (value) => {
  applyTheme({ ...current.value, accent: value })
})

function pick(preset) {
  current.value = { ...preset }
}

function markCustom() {
  current.value = { ...current.value, key: 'custom', name: '自定义' }
}

function reset() {
  current.value = { ...THEME_PRESETS[0] }
}

function apply() {
  saveTheme({ ...current.value })
  ElMessage.success('主题已应用')
  emit('update:modelValue', false)
}
</script>

<style scoped>
.theme-block { margin-bottom: 18px; }
.block-title { font-size: 12px; color: var(--km-text-faint); margin-bottom: 10px; }
.swatches { display: flex; flex-wrap: wrap; gap: 10px; }
.swatch {
  display: flex; align-items: center; gap: 8px; padding: 8px 12px;
  border-radius: 10px; border: 1px solid var(--km-border); background: rgba(255, 255, 255, .03);
  color: var(--km-text-dim); font-size: 12px; cursor: pointer;
}
.swatch.on { border-color: var(--km-accent); color: var(--km-text); background: var(--km-accent-soft); }
.dot { width: 14px; height: 14px; border-radius: 50%; }
.custom-row { display: flex; align-items: center; gap: 12px; }
.hint { font-size: 12px; color: var(--km-text-faint); }
.preview { border: 1px solid var(--km-border); border-radius: 12px; padding: 12px 14px; }
.pv-label { font-size: 11px; color: var(--km-text-faint); }
.pv-card { display: flex; align-items: center; gap: 10px; margin-top: 8px; }
.pv-dot { width: 34px; height: 34px; border-radius: 10px; background: var(--pv); box-shadow: 0 0 18px color-mix(in srgb, var(--pv) 45%, transparent); }
.pv-copy { flex: 1; display: flex; flex-direction: column; }
.pv-copy b { font-size: 13px; color: var(--km-text); }
.pv-copy small { font-size: 11px; color: var(--km-text-faint); }
.pv-btn { padding: 6px 14px; border-radius: 999px; background: var(--pv); color: #fff; font-size: 12px; font-weight: 700; }
</style>
