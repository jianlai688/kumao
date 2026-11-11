<template>
  <div class="vip-page">
    <header class="vip-head">
      <div>
        <h1>会员中心</h1>
        <p>听歌、看现场、逛直播间，把喜欢的声音留在身边。</p>
      </div>
      <div class="account-state" :class="{ active: status.vip }">
        <span class="state-led" aria-hidden="true"></span>
        <div>
          <strong>{{ status.vip ? '畅听会员已生效' : '当前为免费用户' }}</strong>
          <small v-if="status.vip">有效期至 {{ formatDate(status.expireTime) }} · {{ status.remainDays > 0 ? `剩余 ${status.remainDays} 天` : '剩余不足 1 天' }}</small>
          <small v-else>看广告可领取 1 天体验</small>
        </div>
      </div>
    </header>

    <div v-if="loadError" class="page-alert" role="alert">
      <span>{{ loadError }}</span>
      <button type="button" @click="load">重新加载</button>
    </div>

    <section class="ad-pass" aria-labelledby="ad-pass-title">
      <div class="ad-pass-main">
        <span class="ad-badge">每日广告福利</span>
        <h2 id="ad-pass-title">看一段广告<br />今天免费听</h2>
        <p class="ad-pass-copy">完整观看 15 秒广告，领取 1 天酷猫 VIP。付费歌曲、高清 MV 与专属弹幕立即生效，有效期可叠加。</p>
        <div class="ad-pass-actions">
          <button
            class="ad-cta"
            type="button"
            :disabled="adButtonDisabled"
            :aria-label="adButtonLabel"
            @click="openRewardAd"
          >
            <el-icon aria-hidden="true"><VideoPlay /></el-icon>
            {{ adButtonLabel }}
          </button>
          <span class="ad-limit">{{ claimedToday ? '明天 00:00 后可再次领取' : '每日 1 次 · 约 15 秒' }}</span>
        </div>
        <div class="ad-facts" aria-label="广告会员规则">
          <span>15 秒广告</span>
          <span>1 天 VIP</span>
          <span>可叠加有效期</span>
        </div>
      </div>

      <aside class="ad-ticket" aria-label="广告会员卡">
        <div class="ticket-head">
          <span>酷猫音乐</span>
          <span>广告畅听卡</span>
        </div>
        <div class="ticket-number">
          <strong>1</strong>
          <span>天</span>
        </div>
        <div class="ticket-name">广告畅听卡</div>
        <div class="week-strip" aria-label="近 7 天广告会员领取记录">
          <span
            v-for="day in weekDays"
            :key="day.key"
            :class="['week-day', { today: day.today, claimed: day.claimed }]"
            :aria-label="`${day.fullLabel}${day.claimed ? '，已领取' : '，未领取'}`"
          >
            <small>{{ day.weekday }}</small>
            <b>{{ day.day }}</b>
            <i v-if="day.claimed" aria-hidden="true"></i>
          </span>
        </div>
        <span class="ticket-caption">近 7 天领取记录</span>
      </aside>
    </section>

    <section class="content-section" aria-labelledby="plans-title">
      <div class="section-heading">
        <div>
          <h2 id="plans-title">选择会员套餐</h2>
          <p>需要更长周期时，可以选择连续畅听套餐。</p>
        </div>
        <span class="section-meta">{{ packages.length }} 个套餐</span>
      </div>

      <div v-if="loading" class="plan-grid" aria-hidden="true">
        <div v-for="n in 3" :key="n" class="plan-card skeleton-plan">
          <span class="skeleton skeleton-plan-title"></span>
          <span class="skeleton skeleton-plan-price"></span>
          <span class="skeleton skeleton-plan-line"></span>
          <span class="skeleton skeleton-plan-button"></span>
        </div>
      </div>
      <p v-if="loading" class="sr-only" role="status">正在加载会员套餐</p>

      <div v-else-if="packages.length" class="plan-grid">
        <article
          v-for="(pkg, index) in packages"
          :key="pkg.id"
          class="plan-card"
          :class="{ recommended: index === 1 }"
        >
          <div class="plan-head">
            <span class="plan-name">{{ pkg.name }}</span>
            <span v-if="index === 1" class="plan-badge">推荐</span>
          </div>
          <div class="plan-price"><small>¥</small>{{ formatPrice(pkg.price) }}</div>
          <div class="plan-meta">
            <span>{{ pkg.days }} 天</span>
            <del v-if="pkg.originalPrice">¥{{ formatPrice(pkg.originalPrice) }}</del>
          </div>
          <p>{{ pkg.description }}</p>
          <button type="button" class="plan-button" :class="{ primary: index === 1 }" @click="open(pkg)">
            {{ status.vip ? '续费这个周期' : '立即开通' }}
            <el-icon aria-hidden="true"><ArrowRight /></el-icon>
          </button>
        </article>
      </div>

      <div v-else class="empty-panel">
        <span class="empty-mark" aria-hidden="true">—</span>
        <strong>暂无可开通套餐</strong>
        <span>套餐由平台运营配置，请稍后再来。</span>
      </div>
    </section>

    <section class="content-section" aria-labelledby="rights-title">
      <div class="section-heading">
        <div>
          <h2 id="rights-title">会员权益</h2>
          <p>看广告获得的 VIP 与付费会员使用同一套权益。</p>
        </div>
      </div>
      <div class="benefit-strip">
        <div v-for="right in rights" :key="right.title" class="benefit-item">
          <el-icon class="benefit-icon" aria-hidden="true"><component :is="right.icon" /></el-icon>
          <div>
            <strong>{{ right.title }}</strong>
            <span>{{ right.desc }}</span>
          </div>
        </div>
      </div>
    </section>

    <section v-if="userStore.isLogin" class="content-section" aria-labelledby="orders-title">
      <div class="section-heading">
        <div>
          <h2 id="orders-title">我的开通记录</h2>
          <p>付费开通与广告赠送记录都会保存在这里。</p>
        </div>
        <span class="section-meta">{{ orders.length }} 条</span>
      </div>

      <div v-if="orders.length" class="order-panel">
        <div v-for="order in orders" :key="order.orderNo" class="order-row">
          <div class="order-identity">
            <span class="order-date">{{ formatDate(order.createTime) }}</span>
            <strong>{{ order.packageName || '会员开通' }}</strong>
            <small>{{ order.orderNo }}</small>
          </div>
          <div class="order-type">
            <span :class="{ reward: order.payType === 'AD_REWARD' }">{{ payTypeLabel(order.payType) }}</span>
          </div>
          <div class="order-amount">{{ order.payType === 'AD_REWARD' ? '赠送' : `¥${formatPrice(order.amount)}` }}</div>
          <div class="order-status" :class="{ active: order.status === 'PAID' }">{{ orderStatusLabel(order) }}</div>
        </div>
      </div>
      <div v-else class="empty-panel">
        <span class="empty-mark" aria-hidden="true">○</span>
        <strong>暂无开通记录</strong>
        <span>看广告领取或开通套餐后，记录会显示在这里。</span>
      </div>
    </section>

    <section v-else class="login-strip">
      <span>登录后领取广告会员，并查看你的有效期与开通记录。</span>
      <button type="button" class="text-link" @click="goLogin">去登录 <el-icon><ArrowRight /></el-icon></button>
    </section>

    <el-dialog
      v-model="pay.visible"
      width="440px"
      class="vip-pay-dialog"
      :show-close="!pay.qr"
      :close-on-click-modal="false"
      @closed="stopPolling"
    >
      <template #header>
        <div class="pay-head">
          <div>
            <h2>{{ pay.qr ? '等待支付结果' : `开通 ${pay.pkg?.name || '会员'}` }}</h2>
          </div>
          <span v-if="pay.pkg" class="pay-days">{{ pay.pkg.days }} 天</span>
        </div>
      </template>

      <div v-if="!pay.qr" class="pay-body">
        <div class="pay-amount"><span>¥</span>{{ formatPrice(pay.pkg?.price) }}</div>
        <p class="pay-note">支付成功后立即生效，会员时长会叠加到现有有效期。</p>

        <div class="pay-methods" role="radiogroup" aria-label="支付方式">
          <button
            type="button"
            class="pay-method"
            :class="{ active: pay.method === 'ALIPAY' }"
            role="radio"
            :aria-checked="pay.method === 'ALIPAY'"
            @click="pay.method = 'ALIPAY'"
          >
            <span class="pay-logo alipay">支</span>
            <span><b>支付宝</b><small>{{ channels.alipayEnabled ? '跳转支付宝支付' : '未配置 · 模拟支付' }}</small></span>
          </button>
          <button
            type="button"
            class="pay-method"
            :class="{ active: pay.method === 'WECHAT' }"
            role="radio"
            :aria-checked="pay.method === 'WECHAT'"
            @click="pay.method = 'WECHAT'"
          >
            <span class="pay-logo wechat">微</span>
            <span><b>微信支付</b><small>{{ channels.wechatEnabled ? '扫码支付' : '未配置 · 模拟支付' }}</small></span>
          </button>
        </div>

        <div v-if="channels.mockEnabled && (!channels.alipayEnabled || !channels.wechatEnabled)" class="pay-tip">
          当前环境会使用本地模拟支付，仅用于开发演示。
        </div>

        <button type="button" class="pay-submit" :disabled="pay.loading" @click="submitPay">
          {{ pay.loading ? '正在创建订单…' : `支付 ¥${formatPrice(pay.pkg?.price)}` }}
        </button>
      </div>

      <div v-else class="pay-qr">
        <img :src="pay.qr" alt="微信支付二维码" />
        <p>请使用微信扫一扫</p>
        <span>{{ pay.statusText }}</span>
        <button type="button" class="text-link" @click="cancelPay">取消支付</button>
      </div>
    </el-dialog>

    <el-dialog
      v-model="adViewer.visible"
      class="ad-reward-dialog"
      width="880px"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      destroy-on-close
      aria-label="观看广告领取会员"
      @closed="resetAdViewer"
    >
      <div class="ad-viewer">
        <div class="ad-stage">
          <button type="button" class="ad-close" aria-label="关闭广告" @click="closeRewardAd">
            <el-icon><Close /></el-icon>
          </button>

          <div v-if="adViewer.loading" class="ad-state" role="status">
            <span class="ad-loader" aria-hidden="true"></span>
            <strong>正在准备广告</strong>
            <p>广告由酷猫广告平台提供。</p>
          </div>

          <div v-else-if="adViewer.error" class="ad-state is-error" role="alert">
            <el-icon><Warning /></el-icon>
            <strong>广告暂时无法播放</strong>
            <p>{{ adViewer.error }}</p>
            <button type="button" class="ad-retry" @click="retryRewardAd">重新加载</button>
          </div>

          <template v-else-if="adViewer.ad">
            <video
              v-if="isVideoAd"
              ref="adVideo"
              class="ad-media"
              :src="adViewer.ad.fileUrl"
              :poster="adViewer.ad.coverUrl || ''"
              autoplay
              muted
              loop
              playsinline
              @playing="startAdCountdown"
              @error="onAdMediaError"
            ></video>
            <div v-else-if="isAudioAd" class="ad-media ad-audio">
              <img :src="adViewer.ad.coverUrl || fallbackAdCover" :alt="`${adViewer.ad.name}广告封面`" />
              <audio ref="adAudio" :src="adViewer.ad.fileUrl" autoplay @playing="startAdCountdown" @error="onAdMediaError"></audio>
              <div class="audio-bars" aria-hidden="true"><i></i><i></i><i></i><i></i><i></i></div>
            </div>
            <img v-else class="ad-media" :src="adViewer.ad.coverUrl || fallbackAdCover" :alt="`${adViewer.ad.name}广告画面`" @error="onAdMediaError" />

            <div class="ad-topline">
              <span class="ad-label">广告</span>
              <span class="ad-name">{{ adViewer.ad.name }}</span>
            </div>
            <div class="ad-bottomline">
              <p>{{ adViewer.ad.content || '感谢观看广告，支持酷猫音乐持续提供好音乐。' }}</p>
              <div class="ad-countdown" role="timer" aria-live="off">
                <strong>{{ adViewer.completed ? '完成' : adViewer.remaining }}</strong>
                <span>{{ adViewer.completed ? '广告已看完' : '秒后可领取' }}</span>
              </div>
            </div>
            <div class="ad-progress" aria-hidden="true"><span :style="{ width: `${adProgress}%` }"></span></div>
          </template>
        </div>

        <div class="ad-viewer-foot">
          <div>
            <strong>观看结束后领取 1 天 VIP</strong>
            <span>关闭广告或中途退出不会获得奖励；每个账号每天限领 1 次。</span>
            <span v-if="adViewer.claimError" class="ad-claim-error" role="alert">{{ adViewer.claimError }}</span>
          </div>
          <button
            v-if="adViewer.completed"
            type="button"
            class="ad-claim-button"
            :disabled="adViewer.claiming"
            @click="claimAdReward"
          >{{ adViewer.claiming ? '领取中…' : '领取 1 天 VIP' }}</button>
          <button v-else type="button" class="ad-claim-button" disabled>{{ adViewer.loading ? '准备中' : `${adViewer.remaining} 秒后可领取` }}</button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowRight,
  ChatDotRound,
  Close,
  Headset,
  MagicStick,
  VideoCamera,
  VideoPlay,
  Warning
} from '@element-plus/icons-vue'
import { adApi, vipApi } from '@/api'
import { useUserStore } from '@/stores/user'

const AD_SECONDS = 15
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const packages = ref([])
const orders = ref([])
const loading = ref(true)
const loadError = ref('')
const status = reactive({ vip: false, expireTime: '', remainDays: 0, adRewardClaimedToday: false })
const channels = reactive({ alipayEnabled: false, wechatEnabled: false, mockEnabled: true })
const adVideo = ref(null)
const adAudio = ref(null)
const pay = reactive({
  visible: false,
  pkg: null,
  method: 'ALIPAY',
  loading: false,
  mock: false,
  qr: '',
  orderNo: '',
  statusText: '',
  timer: null
})
const adViewer = reactive({
  visible: false,
  loading: false,
  ad: null,
  error: '',
  remaining: AD_SECONDS,
  timer: null,
  completed: false,
  claiming: false,
  claimError: ''
})

const rights = [
  { icon: VideoCamera, title: '高清 MV', desc: '会员专属高清画质' },
  { icon: Headset, title: '付费歌曲', desc: '付费曲库畅听' },
  { icon: ChatDotRound, title: '专属弹幕', desc: '会员弹幕颜色' },
  { icon: MagicStick, title: '入场特效', desc: '直播间身份特效' }
]
const fallbackAdCover = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="960" height="540"><rect width="960" height="540" fill="#171717"/><circle cx="480" cy="238" r="68" fill="#272727"/><path d="M464 202l42 36-42 36z" fill="#c8f74f"/><rect x="250" y="350" width="460" height="10" rx="5" fill="#2d2d2d"/></svg>'
)

const claimedToday = computed(() => !!status.adRewardClaimedToday)
const adButtonLabel = computed(() => {
  if (!userStore.isLogin) return '登录后领取'
  if (claimedToday.value) return '今日已领取'
  if (status.vip) return '看广告再叠 1 天'
  return '看广告领 1 天'
})
const adButtonDisabled = computed(() => userStore.isLogin && claimedToday.value)
const todayKey = computed(() => dateKey(new Date()))
const weekDays = computed(() => {
  const result = []
  const now = new Date()
  for (let offset = 6; offset >= 0; offset -= 1) {
    const date = new Date(now)
    date.setDate(now.getDate() - offset)
    const key = dateKey(date)
    const claimed = orders.value.some((order) => order.payType === 'AD_REWARD' && dateKey(order.createTime) === key)
      || (key === todayKey.value && claimedToday.value)
    result.push({
      key,
      weekday: ['日', '一', '二', '三', '四', '五', '六'][date.getDay()],
      day: date.getDate(),
      fullLabel: `${date.getMonth() + 1} 月 ${date.getDate()} 日`,
      today: key === todayKey.value,
      claimed
    })
  }
  return result
})
const isVideoAd = computed(() => adViewer.ad?.type === 'VIDEO' || /\.(mp4|webm|mov)(\?|$)/i.test(adViewer.ad?.fileUrl || ''))
const isAudioAd = computed(() => !isVideoAd.value && (adViewer.ad?.type === 'AUDIO' || /\.(mp3|wav|m4a|aac)(\?|$)/i.test(adViewer.ad?.fileUrl || '')))
const adProgress = computed(() => Math.min(100, Math.round(((AD_SECONDS - adViewer.remaining) / AD_SECONDS) * 100)))

const statusMap = { PAID: '已支付', UNPAID: '待支付', CLOSED: '已关闭' }
const payTypeMap = { ALIPAY: '支付宝', WECHAT: '微信支付', MOCK: '模拟支付', AD_REWARD: '广告赠送' }

function dateKey(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function formatDate(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 10)
}

function formatPrice(value) {
  if (value === null || value === undefined || value === '') return '0.00'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function statusLabel(value) {
  return statusMap[value] || value || '-'
}

function orderStatusLabel(order) {
  if (order?.payType === 'AD_REWARD' && order?.status === 'PAID') return '已生效'
  return statusLabel(order?.status)
}

function payTypeLabel(value) {
  return payTypeMap[value] || value || '-'
}

function goLogin() {
  router.push({ path: '/login', query: { redirect: '/vip' } })
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const [packageRes, statusRes] = await Promise.all([
      vipApi.packages(),
      userStore.isLogin ? vipApi.status() : Promise.resolve({ data: null })
    ])
    packages.value = (packageRes.data || []).filter((pkg) => pkg.type !== 'COIN')
    if (statusRes?.data) Object.assign(status, statusRes.data)
    if (userStore.isLogin) {
      const [orderRes, channelRes] = await Promise.all([
        vipApi.orders({ pageNum: 1, pageSize: 20 }),
        vipApi.payChannels()
      ])
      orders.value = orderRes.data?.records || []
      Object.assign(channels, channelRes.data || {})
    } else {
      orders.value = []
    }
  } catch (error) {
    loadError.value = error?.message || '会员信息加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function open(pkg) {
  if (!userStore.isLogin) return goLogin()
  pay.pkg = pkg
  pay.method = channels.alipayEnabled ? 'ALIPAY' : (channels.wechatEnabled ? 'WECHAT' : 'ALIPAY')
  pay.qr = ''
  pay.orderNo = ''
  pay.statusText = ''
  pay.mock = false
  pay.visible = true
}

async function submitPay() {
  if (!pay.pkg) return
  pay.loading = true
  try {
    const res = await vipApi.createPayment({ packageId: pay.pkg.id, payType: pay.method })
    const data = res.data
    pay.orderNo = data.orderNo
    pay.mock = data.mock

    if (data.mock) {
      await ElMessageBox.confirm(
        `当前为本地模拟支付（未配置真实商户参数），确认支付 ¥${data.amount}？`,
        '模拟支付',
        { type: 'warning', confirmButtonText: '模拟支付成功', cancelButtonText: '取消' }
      )
      const paid = await vipApi.mockPay(data.orderNo)
      if (paid.data?.status === 'PAID') {
        ElMessage.success('开通成功，欢迎加入酷猫 VIP')
        pay.visible = false
        await load()
        await userStore.refresh()
      }
      return
    }

    if (data.payType === 'ALIPAY') {
      window.location.href = data.payUrl
      return
    }

    if (data.payType === 'WECHAT') {
      pay.qr = data.qrCode
      startPolling(data.orderNo)
    }
  } catch (error) {
    /* 用户取消或接口已在拦截器中提示 */
  } finally {
    pay.loading = false
  }
}

function startPolling(orderNo) {
  stopPolling()
  pay.statusText = '等待扫码支付…'
  pay.timer = setInterval(async () => {
    try {
      const res = await vipApi.payStatus(orderNo)
      const state = res.data?.status
      if (state === 'PAID') {
        stopPolling()
        pay.statusText = '支付成功'
        ElMessage.success('支付成功，欢迎加入酷猫 VIP')
        pay.visible = false
        await load()
        await userStore.refresh()
      } else if (state === 'CLOSED') {
        stopPolling()
        pay.statusText = '订单已关闭，请重新下单'
      }
    } catch (error) {
      /* 下次轮询继续 */
    }
  }, 2500)
}

function stopPolling() {
  if (pay.timer) {
    clearInterval(pay.timer)
    pay.timer = null
  }
}

function cancelPay() {
  stopPolling()
  pay.visible = false
}

async function checkPayReturn() {
  const hashQuery = location.hash.includes('?')
    ? new URLSearchParams(location.hash.split('?')[1])
    : new URLSearchParams()
  const searchQuery = new URLSearchParams(location.search)
  const orderNo = hashQuery.get('out_trade_no') || searchQuery.get('out_trade_no') || route.query.out_trade_no
  const fromAlipay = route.query.pay === 'alipay' || hashQuery.get('pay') === 'alipay'
  if (!orderNo && !fromAlipay) return
  if (orderNo) {
    for (let i = 0; i < 8; i += 1) {
      try {
        const res = await vipApi.payStatus(orderNo)
        if (res.data?.status === 'PAID') {
          ElMessage.success('支付宝支付成功，会员已开通')
          break
        }
      } catch (error) { /* ignore */ }
      await new Promise((resolve) => setTimeout(resolve, 1500))
    }
  }
  await load()
}

function stopAdMedia() {
  if (adVideo.value) {
    adVideo.value.pause()
    adVideo.value.currentTime = 0
  }
  if (adAudio.value) {
    adAudio.value.pause()
    adAudio.value.currentTime = 0
  }
}

function stopAdCountdown() {
  if (adViewer.timer) {
    clearInterval(adViewer.timer)
    adViewer.timer = null
  }
}

function resetAdViewer() {
  stopAdCountdown()
  stopAdMedia()
  adViewer.loading = false
  adViewer.ad = null
  adViewer.error = ''
  adViewer.remaining = AD_SECONDS
  adViewer.completed = false
  adViewer.claiming = false
  adViewer.claimError = ''
}

async function openRewardAd() {
  if (!userStore.isLogin) return goLogin()
  if (claimedToday.value) return ElMessage.info('今日广告会员已领取，明天再来')
  resetAdViewer()
  adViewer.visible = true
  adViewer.loading = true
  try {
    const res = await adApi.active()
    const ads = (res.data || []).filter((item) => item.planId && (item.fileUrl || item.coverUrl))
    const ad = ads.find((item) => item.type === 'VIDEO' && item.fileUrl)
      || ads.find((item) => item.type === 'AUDIO' && item.fileUrl)
      || ads[0]
    if (!ad) throw new Error('当前没有可播放的广告，请稍后再来')
    adViewer.ad = ad
    adApi.report(ad.planId, 'EXPOSURE', '会员中心', new Date().getHours()).catch(() => {})
    await nextTick()
    if (ad.type === 'VIDEO' && ad.fileUrl) {
      adVideo.value?.play?.().catch(() => {})
    } else if (ad.type === 'AUDIO' && ad.fileUrl) {
      adAudio.value?.play?.().catch(() => {})
    } else {
      window.setTimeout(() => startAdCountdown(), 300)
    }
  } catch (error) {
    adViewer.error = error?.message || '广告加载失败，请稍后重试'
  } finally {
    adViewer.loading = false
  }
}

function retryRewardAd() {
  openRewardAd()
}

function startAdCountdown() {
  if (adViewer.timer || adViewer.completed) return
  adViewer.remaining = AD_SECONDS
  adViewer.timer = setInterval(() => {
    adViewer.remaining = Math.max(0, adViewer.remaining - 1)
    if (adViewer.remaining === 0) {
      stopAdCountdown()
      adViewer.completed = true
      stopAdMedia()
    }
  }, 1000)
}

function onAdMediaError() {
  stopAdCountdown()
  adViewer.error = '广告素材加载失败，请检查网络后重试'
}

function closeRewardAd() {
  if (adViewer.claiming) return
  adViewer.visible = false
}

async function claimAdReward() {
  if (!adViewer.completed || !adViewer.ad?.planId) return
  adViewer.claiming = true
  adViewer.claimError = ''
  try {
    const res = await vipApi.claimAdReward(adViewer.ad.planId, AD_SECONDS)
    const expireTime = res.data?.status?.expireTime || res.data?.expireTime
    ElMessage.success(expireTime ? `领取成功，VIP 有效期至 ${formatDate(expireTime)}` : '领取成功，1 天 VIP 已到账')
    adViewer.visible = false
    await load()
    await userStore.refresh()
  } catch (error) {
    adViewer.claimError = error?.response?.data?.message || error?.message || '领取失败，请稍后重试'
    await load()
  } finally {
    adViewer.claiming = false
  }
}

onMounted(async () => {
  await load()
  await checkPayReturn()
})

onBeforeUnmount(() => {
  stopPolling()
  resetAdViewer()
})
</script>

<style scoped>
.vip-page {
  --vip-lime: #c8f74f;
  --vip-ink: #10110e;
  --vip-ground: #080808;
  --vip-surface: #111110;
  --vip-raised: #181817;
  --vip-border: #282826;
  --vip-text: #f7f7f3;
  --vip-muted: #9c9c96;
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  padding: 22px 28px calc(var(--km-player-h) + 38px);
  color: var(--vip-text);
}

.vip-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding: 4px 0 18px;
}
.vip-head h1 {
  margin: 0;
  color: var(--vip-text);
  font-size: 30px;
  line-height: 1.15;
  font-weight: 700;
  letter-spacing: -.04em;
}
.vip-head p {
  margin: 6px 0 0;
  color: var(--vip-muted);
  font-size: 12.5px;
  line-height: 1.5;
}
.account-state {
  min-width: 226px;
  display: flex;
  align-items: flex-start;
  gap: 9px;
  padding: 10px 12px;
  border: 1px solid var(--vip-border);
  border-radius: 10px;
  background: var(--vip-surface);
}
.account-state.active { border-color: rgba(200, 247, 79, .3); }
.account-state > div { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.account-state strong { color: var(--vip-text); font-size: 12.5px; font-weight: 600; }
.account-state small { color: var(--vip-muted); font-size: 11px; line-height: 1.45; }
.state-led { width: 7px; height: 7px; margin-top: 5px; flex: none; border-radius: 50%; background: #696962; }
.account-state.active .state-led { background: var(--vip-lime); }

.page-alert {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 9px 12px;
  border: 1px solid rgba(255, 143, 162, .3);
  border-radius: 8px;
  background: rgba(255, 143, 162, .07);
  color: #ffbdc8;
  font-size: 12px;
}
.page-alert span { flex: 1; }
.page-alert button, .text-link {
  border: 0;
  background: transparent;
  color: var(--vip-lime);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.ad-pass {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 270px;
  gap: 0;
  overflow: hidden;
  border-radius: 24px;
  background: var(--vip-lime);
  color: var(--vip-ink);
}
.ad-pass-main { min-width: 0; padding: 26px 30px 24px; }
.ad-badge {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 9px;
  border: 1px solid rgba(16, 17, 14, .28);
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .02em;
}
.ad-pass h2 {
  max-width: 520px;
  margin: 17px 0 9px;
  color: var(--vip-ink);
  font-size: clamp(30px, 3.2vw, 40px);
  line-height: 1.04;
  font-weight: 800;
  letter-spacing: -.06em;
}
.ad-pass-copy {
  max-width: 520px;
  margin: 0;
  color: rgba(16, 17, 14, .72);
  font-size: 12.5px;
  line-height: 1.65;
}
.ad-pass-actions {
  display: flex;
  align-items: center;
  gap: 13px;
  flex-wrap: wrap;
  margin-top: 19px;
}
.ad-cta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 18px;
  border: 0;
  border-radius: 999px;
  background: var(--vip-ink);
  color: var(--vip-lime);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: background-color .16s, transform .16s;
}
.ad-cta:hover:not(:disabled) { background: #2b2c28; transform: translateY(-1px); }
.ad-cta:disabled { cursor: not-allowed; background: #23241e; color: #898b7c; opacity: 1; }
.ad-limit { color: rgba(16, 17, 14, .62); font-size: 11px; }
.ad-facts {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-top: 19px;
  color: rgba(16, 17, 14, .62);
  font-size: 11px;
}
.ad-facts span + span { position: relative; }
.ad-facts span + span::before {
  content: '';
  position: absolute;
  left: -9px;
  top: 5px;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: rgba(16, 17, 14, .3);
}

.ad-ticket {
  margin: 14px;
  padding: 16px 16px 13px;
  border-radius: 18px;
  background: var(--vip-ink);
  color: var(--vip-text);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 250px;
}
.ticket-head { display: flex; justify-content: space-between; color: #a3a496; font-size: 11px; font-weight: 600; }
.ticket-number { display: flex; align-items: baseline; gap: 4px; margin-top: 8px; }
.ticket-number strong { color: var(--vip-lime); font-size: 76px; line-height: .86; font-weight: 800; letter-spacing: -.08em; font-variant-numeric: tabular-nums; }
.ticket-number span { color: var(--vip-lime); font-size: 16px; font-weight: 700; }
.ticket-name { margin-top: 3px; color: #d7d8cf; font-size: 12px; font-weight: 600; }
.week-strip { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; margin-top: 14px; }
.week-day {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 0;
  padding: 5px 0 6px;
  border: 1px solid #2a2b26;
  border-radius: 6px;
  color: #83847a;
}
.week-day small { font-size: 11px; line-height: 1; }
.week-day b { font-size: 11px; line-height: 1; font-weight: 500; }
.week-day.today { border-color: rgba(200, 247, 79, .55); color: var(--vip-lime); }
.week-day.claimed { background: rgba(200, 247, 79, .12); color: var(--vip-lime); }
.week-day i { position: absolute; right: 3px; top: 3px; width: 3px; height: 3px; border-radius: 50%; background: var(--vip-lime); }
.ticket-caption { margin-top: 7px; color: #a3a496; font-size: 11px; }

.content-section { margin-top: 30px; }
.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 13px;
}
.section-heading h2 {
  margin: 0;
  color: var(--vip-text);
  font-size: 19px;
  line-height: 1.25;
  font-weight: 650;
  letter-spacing: -.025em;
}
.section-heading p { margin: 4px 0 0; color: var(--vip-muted); font-size: 11.5px; line-height: 1.5; }
.section-meta { color: var(--vip-muted); font-size: 11px; white-space: nowrap; }

.plan-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.plan-card {
  display: flex;
  flex-direction: column;
  min-height: 206px;
  padding: 17px;
  border: 1px solid var(--vip-border);
  border-radius: 16px;
  background: var(--vip-surface);
  transition: border-color .18s, transform .18s;
}
.plan-card:hover { border-color: #54554b; transform: translateY(-2px); }
.plan-card.recommended { border-color: rgba(200, 247, 79, .62); background: #151710; }
.plan-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.plan-name { color: var(--vip-text); font-size: 13px; font-weight: 600; }
.plan-badge { padding: 3px 7px; border-radius: 999px; background: var(--vip-lime); color: var(--vip-ink); font-size: 11px; font-weight: 700; }
.plan-price { display: flex; align-items: baseline; gap: 2px; margin: 14px 0 2px; color: var(--vip-text); font-size: 40px; line-height: 1; font-weight: 700; letter-spacing: -.055em; font-variant-numeric: tabular-nums; }
.plan-price small { color: var(--vip-muted); font-size: 17px; font-weight: 500; letter-spacing: 0; }
.plan-meta { display: flex; align-items: center; gap: 8px; color: var(--vip-muted); font-size: 11px; }
.plan-meta del { color: #a1a296; }
.plan-card p { flex: 1; margin: 13px 0 15px; color: var(--vip-muted); font-size: 11.5px; line-height: 1.55; }
.plan-button {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid #3b3c36;
  border-radius: 999px;
  background: transparent;
  color: var(--vip-text);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
}
.plan-button:hover { border-color: #7b7c6c; background: #1c1d19; }
.plan-button.primary { border-color: var(--vip-lime); background: var(--vip-lime); color: var(--vip-ink); }
.plan-button.primary:hover { background: #d7ff7d; }
.plan-card.recommended .plan-price { color: var(--vip-lime); }

.benefit-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border: 1px solid var(--vip-border);
  border-radius: 14px;
  background: var(--vip-surface);
  overflow: hidden;
}
.benefit-item { display: flex; align-items: flex-start; gap: 10px; padding: 16px 15px; border-left: 1px solid var(--vip-border); }
.benefit-item:first-child { border-left: 0; }
.benefit-icon { flex: none; margin-top: 1px; color: var(--vip-lime); font-size: 17px; }
.benefit-item div { display: flex; flex-direction: column; gap: 3px; }
.benefit-item strong { color: var(--vip-text); font-size: 12px; font-weight: 600; }
.benefit-item span { color: var(--vip-muted); font-size: 11px; line-height: 1.45; }

.order-panel { overflow: hidden; border: 1px solid var(--vip-border); border-radius: 14px; background: var(--vip-surface); }
.order-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 100px 84px 72px;
  align-items: center;
  gap: 12px;
  min-height: 60px;
  padding: 10px 15px;
  border-top: 1px solid #20211e;
}
.order-row:first-child { border-top: 0; }
.order-identity { min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.order-identity strong { color: var(--vip-text); font-size: 12px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.order-identity small { color: #92938a; font-size: 11px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.order-date { color: var(--vip-muted); font-size: 11px; }
.order-type span { display: inline-flex; padding: 3px 7px; border-radius: 4px; background: #23241f; color: #b8b9ac; font-size: 11px; white-space: nowrap; }
.order-type span.reward { background: rgba(200, 247, 79, .12); color: var(--vip-lime); }
.order-amount { color: var(--vip-text); font-size: 12px; text-align: right; font-variant-numeric: tabular-nums; }
.order-status { color: var(--vip-muted); font-size: 11px; text-align: right; }
.order-status.active { color: var(--vip-lime); }
.empty-panel, .login-strip {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 88px;
  padding: 16px;
  border: 1px dashed #343530;
  border-radius: 14px;
  color: var(--vip-muted);
  font-size: 11.5px;
}
.empty-panel { flex-direction: column; justify-content: center; }
.empty-panel strong { color: var(--vip-text); font-size: 12px; font-weight: 600; }
.empty-mark { color: var(--vip-lime); font-size: 16px; line-height: 1; }
.login-strip { justify-content: space-between; background: var(--vip-surface); }
.text-link { color: var(--vip-lime); }

.sr-only {
  position: absolute !important;
  width: 1px !important;
  height: 1px !important;
  padding: 0 !important;
  margin: -1px !important;
  overflow: hidden !important;
  clip: rect(0, 0, 0, 0) !important;
  white-space: nowrap !important;
  border: 0 !important;
}
.skeleton { display: block; border-radius: 4px; background: #252620; }
.skeleton-plan { gap: 12px; }
.skeleton-plan-title { width: 52%; height: 12px; }
.skeleton-plan-price { width: 62%; height: 38px; }
.skeleton-plan-line { width: 74%; height: 11px; }
.skeleton-plan-button { width: 100%; height: 36px; margin-top: auto; border-radius: 999px; }

@media (max-width: 980px) {
  .ad-pass { grid-template-columns: minmax(0, 1fr) 238px; }
  .ad-ticket { min-height: 230px; }
  .plan-grid { grid-template-columns: repeat(3, 1fr); }
  .benefit-strip { grid-template-columns: 1fr 1fr; }
  .benefit-item:nth-child(3) { border-left: 0; border-top: 1px solid var(--vip-border); }
  .benefit-item:nth-child(4) { border-top: 1px solid var(--vip-border); }
}
@media (max-width: 760px) {
  .vip-page { padding: 16px 16px calc(var(--km-player-h) + 28px); }
  .vip-head { align-items: flex-start; flex-direction: column; gap: 12px; }
  .vip-head h1 { font-size: 27px; }
  .account-state { width: 100%; }
  .ad-pass { grid-template-columns: 1fr; }
  .ad-pass-main { padding: 22px 20px 18px; }
  .ad-pass h2 { font-size: 31px; }
  .ad-ticket { min-height: 0; margin: 0 14px 14px; }
  .ticket-number strong { font-size: 58px; }
  .week-strip { max-width: 330px; }
  .plan-grid { grid-template-columns: 1fr; }
  .plan-card { min-height: 178px; }
  .plan-price { font-size: 36px; }
  .benefit-strip { grid-template-columns: 1fr 1fr; }
  .order-row { grid-template-columns: minmax(0, 1fr) 78px 66px; gap: 8px; padding: 10px 12px; }
  .order-type { display: none; }
  .order-status { font-size: 10px; }
  .section-heading h2 { font-size: 18px; }
}
@media (max-width: 480px) {
  .ad-pass-actions { align-items: flex-start; flex-direction: column; gap: 8px; }
  .ad-cta { width: 100%; justify-content: center; }
  .ad-facts { gap: 14px; flex-wrap: wrap; }
  .plan-grid { gap: 9px; }
  .benefit-strip { grid-template-columns: 1fr; }
  .benefit-item { border-left: 0; border-top: 1px solid var(--vip-border); }
  .benefit-item:first-child { border-top: 0; }
  .order-row { grid-template-columns: minmax(0, 1fr) 64px; }
  .order-status { display: none; }
  .order-amount { font-size: 11.5px; }
}
</style>

<style>
.vip-pay-dialog,
.ad-reward-dialog {
  background: #0d0e0b !important;
  border: 1px solid #2c2d27 !important;
  border-radius: 16px !important;
  box-shadow: 0 24px 70px rgba(0, 0, 0, .58) !important; /* taste-skill-ignore: floating dialog */
}
.vip-pay-dialog .el-dialog__header {
  padding: 20px 20px 0;
  margin: 0;
}
.vip-pay-dialog .el-dialog__body { padding: 18px 20px 20px; color: #f7f7f3; }
.vip-pay-dialog .el-dialog__headerbtn .el-dialog__close { color: #8d8e84; }
.vip-pay-dialog .el-dialog__headerbtn:hover .el-dialog__close { color: #f7f7f3; }
.pay-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.pay-head h2 { margin: 4px 0 0; color: #f7f7f3; font-size: 20px; font-weight: 650; letter-spacing: -.03em; }
.pay-days { padding: 4px 8px; border: 1px solid #3a3b32; border-radius: 999px; color: #c8f74f; font-size: 10.5px; white-space: nowrap; }
.pay-body { padding-top: 4px; }
.pay-amount { margin: 12px 0 2px; color: #f7f7f3; font-size: 48px; line-height: 1; font-weight: 700; letter-spacing: -.06em; font-variant-numeric: tabular-nums; }
.pay-amount span { margin-right: 3px; color: #9c9c96; font-size: 22px; font-weight: 500; letter-spacing: 0; }
.pay-note { margin: 8px 0 20px; color: #9c9c96; font-size: 11.5px; }
.pay-methods { display: grid; grid-template-columns: 1fr 1fr; gap: 9px; }
.pay-method {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 11px;
  border: 1px solid #30312b;
  border-radius: 10px;
  background: #151613;
  color: #f7f7f3;
  text-align: left;
  cursor: pointer;
}
.pay-method:hover { border-color: #5b5d4f; }
.pay-method.active { border-color: #c8f74f; background: #1b1e13; }
.pay-logo { width: 30px; height: 30px; flex: none; display: grid; place-items: center; border-radius: 8px; color: #fff; font-size: 14px; font-weight: 700; }
.pay-logo.alipay { background: #1677ff; }
.pay-logo.wechat { background: #07c160; }
.pay-method > span:last-child { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.pay-method b { font-size: 12px; font-weight: 600; }
.pay-method small { color: #8e8f84; font-size: 10px; white-space: nowrap; }
.pay-tip { margin: 12px 0 15px; color: #c8b77a; font-size: 11px; line-height: 1.55; }
.pay-submit { width: 100%; height: 44px; border: 0; border-radius: 999px; background: #c8f74f; color: #10110e; font-size: 13px; font-weight: 700; cursor: pointer; }
.pay-submit:hover:not(:disabled) { background: #d7ff7d; }
.pay-submit:disabled { opacity: .55; cursor: wait; }
.pay-qr { display: flex; flex-direction: column; align-items: center; gap: 7px; padding: 12px 0 4px; }
.pay-qr img { width: 230px; height: 230px; padding: 7px; border-radius: 10px; background: #fff; }
.pay-qr p { margin: 4px 0 0; color: #f7f7f3; font-size: 13px; font-weight: 600; }
.pay-qr span { color: #9c9c96; font-size: 11px; }
.pay-qr .text-link { margin-top: 4px; }

.ad-reward-dialog .el-dialog__header { display: none; }
.ad-reward-dialog .el-dialog__body { padding: 0; }
.ad-viewer { color: #f7f7f3; }
.ad-stage { position: relative; display: grid; place-items: center; min-height: 460px; overflow: hidden; background: #000; }
.ad-media { width: 100%; max-height: 62vh; display: block; object-fit: contain; background: #000; }
.ad-audio { position: relative; display: grid; place-items: center; padding: 42px; background: #11120e; }
.ad-audio > img { width: min(300px, 74%); aspect-ratio: 1; border-radius: 14px; object-fit: cover; opacity: .85; }
.audio-bars { position: absolute; right: 30px; bottom: 32px; display: flex; align-items: flex-end; gap: 4px; height: 26px; }
.audio-bars i { display: block; width: 3px; height: 8px; border-radius: 2px; background: #c8f74f; animation: audio-bar 1.1s ease-in-out infinite alternate; }
.audio-bars i:nth-child(2) { height: 17px; animation-delay: .16s; }
.audio-bars i:nth-child(3) { height: 24px; animation-delay: .32s; }
.audio-bars i:nth-child(4) { height: 13px; animation-delay: .48s; }
.audio-bars i:nth-child(5) { height: 20px; animation-delay: .64s; }
@keyframes audio-bar { to { transform: scaleY(.45); } }
.ad-close {
  position: absolute;
  top: 14px;
  right: 14px;
  z-index: 3;
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, .2);
  border-radius: 50%;
  background: rgba(0, 0, 0, .45);
  color: #fff;
  cursor: pointer;
}
.ad-close:hover { background: rgba(0, 0, 0, .78); }
.ad-topline { position: absolute; left: 18px; top: 16px; z-index: 2; display: flex; align-items: center; gap: 8px; }
.ad-label { padding: 3px 6px; border-radius: 3px; background: #c8f74f; color: #10110e; font-size: 11px; font-weight: 700; }
.ad-name { color: rgba(255, 255, 255, .76); font-size: 10.5px; }
.ad-bottomline { position: absolute; left: 0; right: 0; bottom: 0; z-index: 2; display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; padding: 42px 20px 19px; background: rgba(0, 0, 0, .72); }
.ad-bottomline p { max-width: 520px; margin: 0; color: rgba(255, 255, 255, .82); font-size: 11.5px; line-height: 1.55; }
.ad-countdown { flex: none; display: flex; align-items: baseline; gap: 5px; color: #c8f74f; }
.ad-countdown strong { font-size: 28px; line-height: 1; font-weight: 700; font-variant-numeric: tabular-nums; }
.ad-countdown span { color: rgba(255, 255, 255, .72); font-size: 10px; }
.ad-progress { position: absolute; left: 0; right: 0; bottom: 0; z-index: 3; height: 3px; background: rgba(255, 255, 255, .18); }
.ad-progress span { display: block; height: 100%; background: #c8f74f; transition: width 1s linear; }
.ad-state { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 7px; min-height: 460px; color: #9c9c96; text-align: center; }
.ad-state strong { color: #f7f7f3; font-size: 14px; }
.ad-state p { max-width: 340px; margin: 0; font-size: 11.5px; line-height: 1.5; }
.ad-state > .el-icon { color: #c8f74f; font-size: 24px; }
.ad-state.is-error > .el-icon { color: #ff8fa2; }
.ad-loader { width: 24px; height: 24px; border: 2px solid #3b3c34; border-top-color: #c8f74f; border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.ad-retry { margin-top: 8px; padding: 7px 12px; border: 1px solid #484a3e; border-radius: 999px; background: transparent; color: #c8f74f; font-size: 11px; cursor: pointer; }
.ad-viewer-foot { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 14px 18px; background: #12130f; border-top: 1px solid #282a22; }
.ad-viewer-foot > div { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.ad-viewer-foot strong { color: #f7f7f3; font-size: 12px; font-weight: 600; }
.ad-viewer-foot span { color: #909187; font-size: 10.5px; line-height: 1.45; }
.ad-claim-error { color: #ffb2bd !important; }
.ad-claim-button { flex: none; height: 38px; padding: 0 15px; border: 0; border-radius: 999px; background: #c8f74f; color: #10110e; font-size: 11.5px; font-weight: 700; cursor: pointer; }
.ad-claim-button:hover:not(:disabled) { background: #d7ff7d; }
.ad-claim-button:disabled { opacity: .45; cursor: not-allowed; }
@media (max-width: 720px) {
  .vip-pay-dialog, .ad-reward-dialog { width: calc(100vw - 24px) !important; margin-top: 4vh; }
  .ad-stage, .ad-state { min-height: 360px; }
  .ad-viewer-foot { align-items: flex-start; flex-direction: column; }
  .ad-claim-button { width: 100%; }
}
@media (prefers-reduced-motion: reduce) {
  .audio-bars i, .ad-loader { animation: none; }
}
</style>
