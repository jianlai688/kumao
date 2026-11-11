<template>
  <transition name="splash">
    <div v-if="visible && ad" class="ad-splash" @click="openAd">
      <img class="ad-splash__bg" :src="ad.coverUrl" :alt="ad.name" />
      <div class="ad-splash__shade"></div>
      <div class="ad-splash__top">
        <span class="ad-splash__tag">广告</span>
        <button class="ad-splash__skip" @click.stop="close">{{ left }}s 跳过</button>
      </div>
      <div class="ad-splash__bottom">
        <strong>{{ ad.name }}</strong>
        <p>{{ ad.content }}</p>
        <button class="ad-splash__cta">看广告领 1 天会员 ›</button>
      </div>
      <AdModal v-model="modalOpen" :ad="ad" @claimed="onClaimed" />
    </div>
  </transition>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { adApi, vipApi } from '@/api'
import AdModal from '@/components/AdModal.vue'

const ad = ref(null)
const visible = ref(false)
const left = ref(3)
const modalOpen = ref(false)
let timer = null

function close() {
  visible.value = false
  clearInterval(timer)
  timer = null
}

function openAd() {
  if (!ad.value) return
  close()
  modalOpen.value = true
  adApi.report(ad.value.planId, 'click').catch(() => {})
}

function onClaimed() {
  modalOpen.value = false
}

onMounted(async () => {
  if (sessionStorage.getItem('km_splash_shown')) return
  try {
    const status = await vipApi.status()
    if (status.data?.vip) return
  } catch (e) {
    return
  }
  try {
    const res = await adApi.active('SPLASH')
    ad.value = (res.data || [])[0] || null
  } catch (e) {
    ad.value = null
  }
  if (!ad.value) return

  sessionStorage.setItem('km_splash_shown', '1')
  visible.value = true
  left.value = 3
  adApi.report(ad.value.planId, 'exposure').catch(() => {})
  timer = setInterval(() => {
    left.value -= 1
    if (left.value <= 0) close()
  }, 1000)
})

onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.ad-splash {
  position: fixed;
  inset: 0;
  z-index: 3200;
  background: #000;
  cursor: pointer;
}
.ad-splash__bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.ad-splash__shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.45), transparent 38%, rgba(0, 0, 0, 0.82));
}
.ad-splash__top {
  position: absolute;
  top: 24px;
  left: 24px;
  right: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.ad-splash__tag {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.5);
  color: rgba(255, 255, 255, 0.72);
}
.ad-splash__skip {
  border: 1px solid rgba(255, 255, 255, 0.24);
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 13px;
  padding: 7px 16px;
  border-radius: 999px;
  cursor: pointer;
}
.ad-splash__bottom {
  position: absolute;
  left: 32px;
  right: 32px;
  bottom: 56px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #fff;
}
.ad-splash__bottom strong {
  font-size: 26px;
  letter-spacing: -0.5px;
}
.ad-splash__bottom p {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.72);
}
.ad-splash__cta {
  align-self: flex-start;
  margin-top: 8px;
  border: 0;
  cursor: pointer;
  padding: 12px 22px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 700;
  background: linear-gradient(135deg, #fbd08a, #de823c);
  color: #22150a;
}
.splash-enter-active,
.splash-leave-active {
  transition: opacity 0.25s ease;
}
.splash-enter-from,
.splash-leave-to {
  opacity: 0;
}
</style>
