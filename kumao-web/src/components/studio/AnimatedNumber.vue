<template>
  <span class="animated-number">{{ formatter(display) }}</span>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'

const props = defineProps({
  value: { type: Number, default: 0 },
  duration: { type: Number, default: 300 },
  formatter: { type: Function, default: (value) => String(Math.round(value)) }
})

const display = ref(Number(props.value) || 0)
let frame = 0

function animateTo(target) {
  const from = Number(display.value) || 0
  const to = Number(target) || 0
  if (from === to) {
    display.value = to
    return
  }
  if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
    display.value = to
    return
  }
  cancelAnimationFrame(frame)
  const started = performance.now()
  const duration = Math.max(1, props.duration)
  const tick = (now) => {
    const progress = Math.min(1, (now - started) / duration)
    const eased = 1 - Math.pow(1 - progress, 3)
    display.value = from + (to - from) * eased
    if (progress < 1) frame = requestAnimationFrame(tick)
  }
  frame = requestAnimationFrame(tick)
}

watch(() => Number(props.value) || 0, animateTo)

onBeforeUnmount(() => cancelAnimationFrame(frame))
</script>

<style scoped>
.animated-number {
  font-variant-numeric: tabular-nums;
  letter-spacing: -.02em;
}
</style>
