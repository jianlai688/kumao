<template>
  <div v-if="ad && !isVip" class="ad-slot" :class="`ad-slot--${variant}`" @click="openAd">
    <div class="ad-slot__media">
      <img v-if="ad.coverUrl" :src="ad.coverUrl" :alt="ad.name" />
      <span v-else class="ad-slot__ph">AD</span>
    </div>
    <div class="ad-slot__copy">
      <span class="ad-slot__tag">广告 · {{ slotName }}</span>
      <strong class="ad-slot__name">{{ ad.name }}</strong>
      <p class="ad-slot__desc">{{ ad.content || '点击了解更多' }}</p>
    </div>
    <button class="ad-slot__cta">看广告领会员</button>

    <AdModal v-model="modalOpen" :ad="ad" @claimed="onClaimed" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adApi, vipApi } from '@/api'
import AdModal from '@/components/AdModal.vue'

const props = defineProps({
  slot: { type: String, default: 'HOME_BANNER' },
  slotLabel: { type: String, default: '' },
  variant: { type: String, default: 'banner' }
})

const ad = ref(null)
const isVip = ref(false)
const modalOpen = ref(false)

const slotName = computed(() => {
  if (props.slotLabel) return props.slotLabel
  const map = { SPLASH: '开屏', HOME_BANNER: '首页 Banner', LIVE: '直播页', PLAYER: '播放页' }
  return map[props.slot] || props.slot
})

async function load() {
  try {
    const res = await vipApi.status()
    isVip.value = !!res.data?.vip
  } catch (e) {
    isVip.value = false
  }
  if (isVip.value) return
  try {
    const res = await adApi.active(props.slot)
    ad.value = (res.data || [])[0] || null
  } catch (e) {
    ad.value = null
  }
}

function openAd() {
  if (!ad.value) return
  modalOpen.value = true
  adApi.report(ad.value.planId, 'click').catch(() => {})
}

function onClaimed() {
  modalOpen.value = false
  ElMessage.success('会员奖励已到账')
  load()
}

onMounted(load)
</script>

<style scoped>
.ad-slot {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  cursor: pointer;
  background: linear-gradient(135deg, rgba(245, 180, 91, 0.12), rgba(255, 255, 255, 0.03));
  border: 1px solid rgba(245, 180, 91, 0.22);
  transition: border-color 0.2s ease, transform 0.2s ease;
}
.ad-slot:hover {
  border-color: rgba(245, 180, 91, 0.45);
  transform: translateY(-1px);
}
.ad-slot__media {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  overflow: hidden;
  flex: none;
  background: #17151b;
  display: grid;
  place-items: center;
}
.ad-slot__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.ad-slot__ph {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
  letter-spacing: 2px;
}
.ad-slot__copy {
  flex: 1;
  min-width: 0;
}
.ad-slot__tag {
  font-size: 11px;
  font-weight: 700;
  color: rgba(245, 180, 91, 0.9);
}
.ad-slot__name {
  display: block;
  font-size: 14px;
  color: #f6f2ea;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ad-slot__desc {
  margin: 3px 0 0;
  font-size: 12px;
  color: rgba(246, 242, 234, 0.42);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ad-slot__cta {
  flex: none;
  border: 0;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  background: linear-gradient(135deg, #fbd08a, #de823c);
  color: #22150a;
}
.ad-slot--card {
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
}
.ad-slot--card .ad-slot__media {
  width: 100%;
  height: 140px;
}
.ad-slot--card .ad-slot__cta {
  align-self: flex-start;
}
.ad-slot--inline {
  padding: 10px 12px;
}
</style>

