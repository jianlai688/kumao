<template>
  <div class="km-page live-page" v-if="room">
    <div class="live-topline">
      <router-link to="/live" class="back-link">
        <el-icon><ArrowLeft /></el-icon>
        全部直播
      </router-link>
      <span class="topline-sep" aria-hidden="true">/</span>
      <span class="topline-current">{{ room.category || '直播间' }}</span>
      <span class="room-code">房间 {{ room.id }}</span>
    </div>

    <div class="live-workspace">
      <!-- ============ 主舞台 ============ -->
      <div class="live-main">
        <article class="stage-card" aria-labelledby="room-title">
          <div class="stage-media">
            <video
              v-show="room.status === 'LIVING' && !videoError"
              ref="videoRef"
              :poster="room.coverUrl || undefined"
              class="stage-video"
              controls
              autoplay
              muted
              playsinline
              aria-label="直播间画面"
              @canplay="videoLoading = false"
              @loadeddata="videoLoading = false"
              @waiting="videoLoading = true"
              @error="onVideoError"
            ></video>

            <div class="stage-scrim" aria-hidden="true"></div>

            <!-- 状态层：主播身份 + 信号 + 观众 -->
            <div class="stage-top">
              <div class="anchor-chip">
                <el-avatar :size="34" :src="room.anchorAvatar" class="anchor-avatar">
                  {{ (room.anchorName || 'A').slice(0, 1) }}
                </el-avatar>
                <div class="anchor-copy">
                  <div class="anchor-name-row">
                    <span class="anchor-name">{{ room.anchorName || '主播' }}</span>
                    <span class="anchor-role">主播</span>
                  </div>
                  <div class="anchor-sub">{{ room.category || '音乐' }} · {{ sceneLabel }}</div>
                </div>
              </div>

              <div class="stage-status">
                <span :class="['live-state', { off: room.status !== 'LIVING' }]">
                  <i aria-hidden="true"></i>
                  {{ room.status === 'LIVING' ? '直播中' : '未开播' }}
                </span>
                <span :class="['signal-state', signalTone]">
                  <i aria-hidden="true"></i>{{ signalLabel }}
                </span>
                <span class="viewer-state">
                  <el-icon><View /></el-icon>
                  <b class="tabular">{{ formatNumber(realtimeOnline) }}</b>
                </span>
                <button class="stage-icon-btn" aria-label="复制并分享直播间" @click="shareRoom">
                  <el-icon><Share /></el-icon>
                </button>
              </div>
            </div>

            <!-- 加载 / 断流 / 未开播 -->
            <div
              v-if="room.status !== 'LIVING' || videoError || videoLoading"
              class="media-state"
              role="status"
              aria-live="polite"
            >
              <div class="signal-art" aria-hidden="true">
                <svg viewBox="0 0 120 120">
                  <circle cx="60" cy="60" r="52" fill="none" stroke="rgba(255,255,255,.07)" />
                  <circle cx="60" cy="60" r="38" fill="none" stroke="rgba(255,255,255,.09)" stroke-dasharray="2 8" />
                  <path d="M22 38c14 6 22 14 26 24M98 38c-14 6-22 14-26 24" fill="none" stroke="rgba(232,121,249,.35)" stroke-width="1.5" stroke-linecap="round" />
                  <rect x="52" y="35" width="16" height="30" rx="8" fill="none" stroke="#f0abfc" stroke-width="2" />
                  <path d="M46 57c0 8 6 14 14 14s14-6 14-14M60 71v10M52 81h16" fill="none" stroke="#f0abfc" stroke-width="2" stroke-linecap="round" />
                  <path d="M34 63h8M78 63h8M37 72h5M78 72h5" stroke="rgba(240,171,252,.5)" stroke-width="2" stroke-linecap="round" />
                </svg>
              </div>

              <template v-if="room.status !== 'LIVING'">
                <h2 class="state-title">主播暂未开播</h2>
                <p class="state-copy">关注主播，开播时第一时间收到提醒。</p>
                <div class="state-actions">
                  <button class="km-btn km-btn-primary" @click="followAnchor">
                    <el-icon><Plus /></el-icon>{{ followed ? '已关注' : '关注主播' }}
                  </button>
                  <router-link to="/live" class="km-btn km-btn-quiet">看看其他直播</router-link>
                </div>
              </template>

              <template v-else-if="videoError">
                <h2 class="state-title">直播信号中断</h2>
                <p class="state-copy">直播画面暂时无法播放，可能是推流未就绪或浏览器不支持该视频格式。</p>
                <div class="state-actions">
                  <button class="km-btn km-btn-primary" @click="retryVideo">
                    <el-icon><RefreshRight /></el-icon>重新连接
                  </button>
                  <button class="km-btn km-btn-quiet" @click="followAnchor">
                    {{ followed ? '已关注主播' : '关注主播' }}
                  </button>
                </div>
                <details class="tech-details">
                  <summary>技术信息</summary>
                  <code>{{ room.pullUrl || '未配置拉流地址' }}</code>
                </details>
                <p class="state-hint">持续无法播放？请确认推流服务已启动，或使用支持 HLS 的浏览器。</p>
              </template>

              <template v-else>
                <h2 class="state-title">正在接入直播信号</h2>
                <p class="state-copy">已进入直播间，正在等待主播画面，请稍候。</p>
                <div class="loading-track" aria-hidden="true"><span></span></div>
              </template>

            </div>

            <!-- 视频底部字幕层：只在画面正常时出现 -->
            <div v-if="room.status === 'LIVING' && !videoError && !videoLoading" class="stage-caption">
              <span class="caption-dot" aria-hidden="true"></span>
              {{ room.title }}
            </div>
          </div>

          <div class="stage-info">
            <div class="stage-copy">
              <h1 id="room-title" class="stage-title">{{ room.title }}</h1>
              <div class="stage-meta-line">
                <span class="meta-category">{{ room.category || '音乐现场' }}</span>
                <span v-if="room.scene === 'OUTDOOR'" class="meta-location">{{ room.location || '户外直播' }}</span>
                <span class="meta-status">{{ room.status === 'LIVING' ? '正在直播' : '回放与预告' }}</span>
              </div>
              <p class="stage-desc">{{ room.intro || '和主播一起听歌、聊天，分享此刻的心情。' }}</p>
            </div>
            <div class="stage-actions">
              <button
                class="like-btn"
                :disabled="!userStore.isLogin || room.status !== 'LIVING'"
                aria-label="点赞直播间"
                @click="likeRoom"
              >
                <el-icon><Star /></el-icon>
                <span>{{ formatNumber(room.likeCount) }}</span>
              </button>
              <button class="follow-btn" :class="{ followed }" :aria-pressed="followed" aria-label="关注主播" @click="followAnchor">
                <el-icon><component :is="followed ? 'Select' : 'Plus'" /></el-icon>
                {{ followed ? '已关注' : '关注' }}
              </button>
              <button class="share-btn" aria-label="分享直播间" @click="shareRoom">
                <el-icon><Share /></el-icon>
                <span>分享</span>
              </button>
            </div>
          </div>

          <div class="stage-metrics" aria-label="直播间实时指标">
            <div class="metric-cell">
              <span class="metric-label">当前在线</span>
              <span class="metric-value tabular">{{ formatNumber(realtimeOnline) }}</span>
            </div>
            <div class="metric-cell">
              <span class="metric-label">本场峰值</span>
              <span class="metric-value tabular">{{ formatNumber(room.peakOnline) }}</span>
            </div>
            <div class="metric-cell">
              <span class="metric-label">弹幕互动</span>
              <span class="metric-value tabular">{{ formatNumber(danmakuList.length) }}</span>
            </div>
            <div class="metric-note">
              <span :class="['mini-signal', signalTone]"><i aria-hidden="true"></i></span>
              <span>{{ statusNarrative }}</span>
            </div>
          </div>
        </article>

        <!-- ============ 资产带 ============ -->
        <div class="asset-band">
          <section class="wallet-card" aria-labelledby="wallet-title">
          <div class="wallet-head">
            <span class="wallet-mark" aria-hidden="true">
              <svg viewBox="0 0 24 24">
                <circle cx="12" cy="12" r="8.5" fill="none" stroke="currentColor" stroke-width="1.6" />
                <path d="M9 9.5h6M9 12h6M10 9.5c2.4 0 3.2 1.2 3.2 2.5S12.4 14.5 10 14.5h5" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
              </svg>
            </span>
            <div>
              <h2 id="wallet-title" class="wallet-label">我的酷猫币</h2>
              <p class="wallet-note">用于给主播送出礼物，支持每一次现场。</p>
            </div>
          </div>
          <div class="wallet-balance">
            <span class="balance-value tabular">{{ userStore.isLogin ? formatNumber(balance) : '—' }}</span>
            <span class="balance-unit">币</span>
          </div>
          <div class="wallet-actions">
            <button class="km-btn km-btn-primary wallet-recharge" aria-label="充值酷猫币，支持微信和支付宝" @click="openCoinRecharge">
              <el-icon><Plus /></el-icon>
              充值
            </button>
            <span v-if="userStore.isLogin" class="wallet-pay">微信 / 支付宝</span>
            <router-link v-if="!userStore.isLogin" to="/login" class="wallet-login">登录后查看余额</router-link>
            <router-link v-else to="/vip" class="wallet-login">会员权益</router-link>
          </div>
        </section>

        <!-- ============ 礼物 ============ -->
        <section class="gift-panel" aria-labelledby="gift-title">
          <div class="gift-head">
            <div>
              <h2 id="gift-title" class="gift-title">送一份心意</h2>
              <p class="gift-sub">点击礼物即可送出，礼物会出现在弹幕互动中。</p>
            </div>
            <span class="gift-balance">{{ userStore.isLogin ? `${formatNumber(balance)} 酷猫币可用` : '登录后可赠送' }}</span>
          </div>
          <div class="gift-list" role="list">
            <button
              v-for="gift in gifts"
              :key="gift.id"
              class="gift-item"
              role="listitem"
              :aria-label="`赠送${gift.name}，${gift.price}酷猫币`"
              @click="sendGift(gift)"
            >
              <span class="gift-icon" aria-hidden="true">
                <el-icon><component :is="giftIcon(gift)" /></el-icon>
              </span>
              <span class="gift-name">{{ gift.name }}</span>
              <span class="gift-price"><i aria-hidden="true">◈</i>{{ gift.price }}</span>
            </button>
            <div v-if="!gifts.length" class="gift-loading" aria-live="polite">礼物列表加载中</div>
          </div>
        </section>
        </div>

        <div v-if="room.replayUrl" class="replay-bar">
          <span class="replay-ico" aria-hidden="true"><el-icon><VideoPlay /></el-icon></span>
          <span>本场直播已结束，可观看回放</span>
          <a :href="room.replayUrl" target="_blank" rel="noopener" class="replay-link">观看回放<el-icon><ArrowRight /></el-icon></a>
        </div>
      </div>

      <!-- ============ 弹幕互动 ============ -->
      <aside class="chat-panel" aria-labelledby="chat-title">
        <header class="chat-head">
          <div class="chat-title-row">
            <h2 id="chat-title" class="chat-title">弹幕互动</h2>
            <span class="chat-count">{{ danmakuList.length }}</span>
          </div>
          <span :class="['conn-state', wsState]">
            <i aria-hidden="true"></i>{{ wsLabel }}
          </span>
        </header>

        <div class="room-notice">
          <span class="notice-label">直播公告</span>
          <p>{{ room.intro || '欢迎来到直播间，文明互动，尊重主播与其他观众。' }}</p>
        </div>

        <div class="chat-tabs" role="tablist" aria-label="弹幕筛选">
          <button
            v-for="tab in chatTabs"
            :key="tab.value"
            class="chat-tab"
            :class="{ active: chatFilter === tab.value }"
            role="tab"
            :aria-selected="chatFilter === tab.value"
            @click="chatFilter = tab.value"
          >{{ tab.label }}</button>
          <span class="chat-tab-note">实时</span>
        </div>

        <div class="chat-summary" aria-label="本场互动数据">
          <span><b class="tabular">{{ danmakuList.length }}</b> 条消息</span>
          <span><b class="tabular">{{ giftCount }}</b> 份礼物</span>
          <span><b class="tabular">{{ formatNumber(realtimeOnline) }}</b> 人在线</span>
        </div>

        <div v-if="danmakuList.length < 5" class="chat-topics">
          <span class="topic-label">互动话题</span>
          <button @click="useQuickReply('今晚想听哪首老歌？')">今晚想听哪首老歌？</button>
          <button @click="useQuickReply('你从哪首歌认识主播的？')">你从哪首歌认识主播的？</button>
        </div>

        <div ref="dmListRef" class="chat-list" role="log" aria-live="polite" aria-label="弹幕消息">
          <template v-for="(d, i) in filteredDanmaku" :key="d.id || `${i}-${d.content}`">
            <div v-if="d.type === 'GIFT'" class="chat-gift">
              <span class="gift-bullet" aria-hidden="true"><el-icon><Present /></el-icon></span>
              <p><b>{{ d.nickname || '观众' }}</b> {{ giftSentence(d.content) }}</p>
            </div>
            <div v-else-if="d.type === 'ENTRANCE'" class="chat-system">
              <span class="system-line" aria-hidden="true"></span>
              <span>{{ d.content }}</span>
            </div>
            <div v-else class="chat-message">
              <el-avatar :size="28" class="chat-avatar">{{ (d.nickname || 'U').slice(0, 1) }}</el-avatar>
              <div class="chat-body">
                <div class="chat-meta">
                  <span class="chat-name" :class="{ vip: d.vip }">{{ d.nickname || '观众' }}</span>
                  <span v-if="d.vip" class="vip-chip">VIP</span>
                  <span v-if="d.createTime" class="chat-time">{{ formatTime(d.createTime) }}</span>
                </div>
                <p class="chat-text" :style="d.color ? { color: d.color } : null">{{ d.content }}</p>
              </div>
            </div>
          </template>

          <div v-if="!filteredDanmaku.length" class="chat-empty">
            <span class="empty-mark" aria-hidden="true">
              <svg viewBox="0 0 48 48">
                <path d="M9 12.5h30v20H23l-8 6v-6H9z" fill="none" stroke="rgba(240,171,252,.65)" stroke-width="1.6" stroke-linejoin="round" />
                <path d="M16 20h16M16 25h10" stroke="rgba(255,255,255,.35)" stroke-width="1.6" stroke-linecap="round" />
              </svg>
            </span>
            <b>{{ chatFilter === 'ALL' ? '还没有弹幕' : '这个筛选下还没有消息' }}</b>
            <p>{{ chatFilter === 'ALL' ? '发第一条弹幕，和主播打个招呼。' : '切换到「全部」看看完整互动。' }}</p>
            <div v-if="chatFilter === 'ALL'" class="quick-replies">
              <button v-for="q in quickReplies" :key="q" @click="useQuickReply(q)">{{ q }}</button>
            </div>
          </div>

          <div v-if="filteredDanmaku.length && filteredDanmaku.length < 4" class="chat-sparse">
            <b>互动刚刚开始</b>
            <p>和主播聊聊你正在听的歌，或者点一首想听的。</p>
          </div>
        </div>

        <div v-if="wsState === 'disconnected'" class="conn-banner" role="alert">
          <span>弹幕连接已断开，消息可能延迟。</span>
          <button @click="reconnectWs">重新连接</button>
        </div>

        <div class="chat-quickbar" aria-label="快捷弹幕">
          <span class="quickbar-label">快捷</span>
          <button v-for="q in quickReplies" :key="q" @click="useQuickReply(q)">{{ q }}</button>
        </div>

        <div class="chat-composer">
          <div class="composer-field">
            <el-icon class="composer-icon" aria-hidden="true"><ChatDotRound /></el-icon>
            <input
              v-model="danmakuText"
              type="text"
              maxlength="100"
              class="composer-input"
              :placeholder="userStore.isLogin ? '发个弹幕，和大家一起听…' : '登录后参与弹幕互动'"
              aria-label="输入弹幕内容"
              @keyup.enter="sendDanmaku"
            />
            <span class="composer-count">{{ danmakuText.length }}/100</span>
            <button
              class="send-btn"
              :disabled="!danmakuText.trim()"
              aria-label="发送弹幕"
              @click="sendDanmaku"
            >发送</button>
          </div>
          <div class="composer-foot">
            <span v-if="!userStore.isLogin">登录后参与互动</span>
            <span v-else-if="wsState !== 'connected'">连接恢复后可发送</span>
            <span v-else>按 Enter 发送</span>
            <router-link to="/vip" class="vip-hint">
              <el-icon><Medal /></el-icon>
              会员弹幕颜色与入场特效
            </router-link>
          </div>
        </div>
      </aside>
    </div>

    <el-dialog
      v-model="coin.visible"
      class="coin-recharge-dialog"
      width="520px"
      :close-on-click-modal="false"
      aria-label="酷猫币充值"
      @closed="resetCoinDialog"
    >
      <template #header>
        <div class="coin-modal-head">
          <div>
            <span>酷猫币充值</span>
            <h2>{{ coin.qr ? '扫码支付' : '选择充值金额' }}</h2>
          </div>
          <strong>{{ formatNumber(balance) }} <small>币</small></strong>
        </div>
      </template>

      <div v-if="coin.loading" class="coin-loading" role="status">
        <span class="coin-spinner" aria-hidden="true"></span>
        正在加载充值套餐
      </div>

      <div v-else-if="coin.qr" class="coin-qr">
        <img :src="coin.qr" alt="微信支付二维码" />
        <strong>请使用微信扫一扫</strong>
        <span>{{ coin.statusText }}</span>
      </div>

      <div v-else class="coin-modal-body">
        <div class="coin-package-grid" role="radiogroup" aria-label="选择酷猫币套餐">
          <button
            v-for="pkg in coinPackages"
            :key="pkg.id"
            type="button"
            class="coin-package"
            :class="{ selected: coin.packageId === pkg.id }"
            role="radio"
            :aria-checked="coin.packageId === pkg.id"
            @click="coin.packageId = pkg.id; coin.error = ''"
          >
            <strong>{{ formatNumber(pkg.coinAmount) }}</strong>
            <span>酷猫币</span>
            <em>¥{{ formatPrice(pkg.price) }}</em>
          </button>
        </div>

        <div class="coin-method-title">支付方式</div>
        <div class="coin-methods" role="radiogroup" aria-label="选择支付方式">
          <button
            type="button"
            class="coin-method"
            :class="{ selected: coin.method === 'WECHAT' }"
            role="radio"
            :aria-checked="coin.method === 'WECHAT'"
            :disabled="!channelAvailable('WECHAT')"
            @click="coin.method = 'WECHAT'; coin.error = ''"
          >
            <span class="pay-logo wechat">微</span>
            <span><b>微信支付</b><small>{{ channelHint('WECHAT') }}</small></span>
            <i class="method-dot" aria-hidden="true"></i>
          </button>
          <button
            type="button"
            class="coin-method"
            :class="{ selected: coin.method === 'ALIPAY' }"
            role="radio"
            :aria-checked="coin.method === 'ALIPAY'"
            :disabled="!channelAvailable('ALIPAY')"
            @click="coin.method = 'ALIPAY'; coin.error = ''"
          >
            <span class="pay-logo alipay">支</span>
            <span><b>支付宝</b><small>{{ channelHint('ALIPAY') }}</small></span>
            <i class="method-dot" aria-hidden="true"></i>
          </button>
        </div>

        <p v-if="channels.mockEnabled && ((coin.method === 'WECHAT' && !channels.wechatEnabled) || (coin.method === 'ALIPAY' && !channels.alipayEnabled))" class="coin-mock-note">当前选择的渠道未配置，将使用本地模拟支付；支付成功后酷猫币立即到账。</p>
        <p v-if="coin.error" class="coin-error" role="alert">{{ coin.error }}</p>
      </div>

      <template #footer>
        <div class="coin-modal-foot">
          <span>{{ coin.qr ? '支付完成后自动刷新余额' : '充值仅用于直播礼物，不支持提现' }}</span>
          <div>
            <button type="button" class="coin-cancel" @click="coin.visible = false">{{ coin.qr ? '关闭' : '取消' }}</button>
            <button v-if="!coin.qr" type="button" class="coin-submit" :disabled="coin.submitting || !selectedCoinPackage" @click="submitCoinRecharge">
              {{ coin.submitting ? '创建订单中…' : coinSubmitLabel }}
            </button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>

  <div v-else-if="loadError" class="km-page live-state-page">
    <div class="load-error">
      <span class="error-mark" aria-hidden="true">
        <svg viewBox="0 0 48 48"><circle cx="24" cy="24" r="17" fill="none" stroke="rgba(251,113,133,.65)" stroke-width="1.6" /><path d="M24 15v11M24 32h.01" stroke="#fb7185" stroke-width="2" stroke-linecap="round" /></svg>
      </span>
      <h1>直播间暂时无法打开</h1>
      <p>没有取到房间信息，请检查网络后重试。</p>
      <button class="km-btn km-btn-primary" @click="load">重新加载</button>
    </div>
  </div>

  <div v-else class="km-page live-skeleton" aria-label="直播间加载中">
    <div class="sk-line short"></div>
    <div class="sk-stage"></div>
    <div class="sk-line"></div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import Hls from 'hls.js'
import {
  ArrowLeft, ArrowRight, ChatDotRound, Medal, Plus, Present,
  RefreshRight, Share, Star, View, VideoPlay
} from '@element-plus/icons-vue'
import { liveApi, vipApi } from '@/api'
import { useUserStore } from '@/stores/user'
import flvjs from 'flv.js'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const room = ref(null)
const gifts = ref([])
const balance = ref(0)
const coinPackages = ref([])
const channels = reactive({ alipayEnabled: false, wechatEnabled: false, mockEnabled: true })
const coin = reactive({
  visible: false,
  loading: false,
  submitting: false,
  packageId: null,
  method: 'WECHAT',
  qr: '',
  orderNo: '',
  statusText: '',
  error: '',
  timer: null
})
const danmakuList = ref([])
const danmakuText = ref('')
const dmListRef = ref(null)
const wsState = ref('connecting')
const videoError = ref(false)
const videoLoading = ref(true)
const followed = ref(false)
const loading = ref(true)
const loadError = ref(false)
const chatFilter = ref('ALL')
const videoRef = ref(null)
let ws = null
let heartbeat = null
let hls = null
let flvPlayer = null

const chatTabs = [
  { value: 'ALL', label: '全部' },
  { value: 'TEXT', label: '聊天' },
  { value: 'GIFT', label: '礼物' }
]

const quickReplies = ['晚上好，主播', '这首歌好好听', '点一首老歌']

const wsConnected = computed(() => wsState.value === 'connected')
const wsLabel = computed(() => ({
  connected: '已连接',
  connecting: '连接中',
  disconnected: '已断开'
}[wsState.value] || '连接中'))

const realtimeOnline = computed(() => Number(room.value?.realtimeOnline) || 0)
const giftCount = computed(() => danmakuList.value.filter((d) => d.type === 'GIFT').length)
const sceneLabel = computed(() => {
  if (!room.value) return '音乐现场'
  return room.value.scene === 'OUTDOOR' ? (room.value.location || '户外直播') : '室内直播'
})
const signalLabel = computed(() => {
  if (room.value?.status !== 'LIVING') return '未开播'
  if (videoError.value) return '信号中断'
  if (videoLoading.value) return '连接中'
  return '画面正常'
})
const signalTone = computed(() => {
  if (room.value?.status !== 'LIVING') return 'is-offline'
  if (videoError.value) return 'is-error'
  if (videoLoading.value) return 'is-connecting'
  return 'is-good'
})
const statusNarrative = computed(() => {
  if (room.value?.status !== 'LIVING') return '主播未开播，关注后可收到提醒'
  if (videoError.value) return '推流暂未就绪，可点击画面重试'
  if (videoLoading.value) return '正在接入直播画面'
  return '直播画面与弹幕均已连接'
})

const filteredDanmaku = computed(() => {
  if (chatFilter.value === 'ALL') return danmakuList.value
  return danmakuList.value.filter((d) => d.type === chatFilter.value)
})

const selectedCoinPackage = computed(() => coinPackages.value.find((pkg) => pkg.id === coin.packageId) || null)
const coinSubmitLabel = computed(() => {
  if (!selectedCoinPackage.value) return '选择充值金额'
  return `${coin.method === 'WECHAT' ? '微信' : '支付宝'}支付 ¥${formatPrice(selectedCoinPackage.value.price)}`
})

function formatNumber(n) {
  return new Intl.NumberFormat('zh-CN').format(Number(n) || 0)
}

function formatPrice(n) {
  return Number(n || 0).toFixed(2)
}

function channelAvailable(method) {
  return method === 'WECHAT' ? channels.wechatEnabled || channels.mockEnabled : channels.alipayEnabled || channels.mockEnabled
}

function channelHint(method) {
  const enabled = method === 'WECHAT' ? channels.wechatEnabled : channels.alipayEnabled
  return enabled ? '安全跳转支付' : '未配置 · 模拟支付'
}

function formatTime(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function giftIcon(gift) {
  return ({
    鲜花: 'Sunny',
    音符: 'Headset',
    爱心: 'StarFilled',
    火箭: 'Promotion',
    城堡: 'OfficeBuilding'
  })[gift?.name] || 'Present'
}

function giftSentence(content = '') {
  return content.replace(/^.*?送出\s*/, '送出 ')
}

async function load() {
  loading.value = true
  loadError.value = false
  try {
    const res = await liveApi.room(route.params.id)
    room.value = res.data
    videoLoading.value = room.value?.status === 'LIVING'
    videoError.value = false
    if (room.value?.status === 'LIVING') {
      await nextTick()
      attachVideo()
    }
  } catch (e) {
    loadError.value = true
    room.value = null
  } finally {
    loading.value = false
  }
}

async function loadGifts() {
  try {
    const g = await liveApi.gifts()
    gifts.value = g.data || []
  } catch (e) {
    gifts.value = []
  }
  if (!userStore.isLogin) return
  try {
    const b = await liveApi.balance()
    balance.value = b.data?.balance ?? b.data ?? 0
  } catch (e) {
    balance.value = 0
  }
}

function followAnchor() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return
  }
  followed.value = !followed.value
  ElMessage.success(followed.value ? '关注成功' : '已取消关注')
}

async function likeRoom() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await liveApi.like(room.value.id)
    room.value.likeCount = Number(res.data) || 0
  } catch (e) {
    /* request interceptor reports the error */
  }
}

function destroyHls() {
  if (hls) {
    try { hls.destroy() } catch (e) { /* ignore */ }
    hls = null
  }
}

function destroyFlv() {
  if (flvPlayer) {
    try {
      flvPlayer.pause()
      flvPlayer.unload()
      flvPlayer.detachMediaElement()
      flvPlayer.destroy()
    } catch (e) { /* ignore */ }
    flvPlayer = null
  }
}

function destroyVideoPlayers() {
  destroyFlv()
  destroyHls()
}

function normalizeStreamUrl(url) {
  if (!url) return url
  try {
    const parsed = new URL(url, location.origin)
    const localHosts = ['127.0.0.1', 'localhost']
    if (localHosts.includes(parsed.hostname) && !localHosts.includes(location.hostname)) {
      parsed.hostname = location.hostname
    }
    return parsed.toString()
  } catch (e) {
    return url
  }
}

function attachHls(video, url) {
  if (Hls.isSupported()) {
    hls = new Hls({
      enableWorker: true,
      lowLatencyMode: true,
      backBufferLength: 30
    })
    hls.loadSource(url)
    hls.attachMedia(video)
    hls.on(Hls.Events.MANIFEST_PARSED, () => {
      videoLoading.value = false
      video.play?.().catch(() => {})
    })
    hls.on(Hls.Events.ERROR, (_event, data) => {
      if (!data.fatal) return
      if (data.type === Hls.ErrorTypes.MEDIA_ERROR) {
        hls.recoverMediaError()
        return
      }
      onVideoError()
    })
    return
  }
  // Safari / iOS 原生支持 HLS，其他浏览器使用 hls.js。
  if (video.canPlayType('application/vnd.apple.mpegurl')) {
    video.src = url
    video.addEventListener('loadedmetadata', () => {
      videoLoading.value = false
      video.play?.().catch(() => {})
    }, { once: true })
    return
  }
  onVideoError()
}

function attachVideo() {
  const video = videoRef.value
  const hlsUrl = normalizeStreamUrl(room.value?.pullUrl)
  if (!video || !hlsUrl) {
    onVideoError()
    return
  }
  destroyVideoPlayers()
  video.removeAttribute('src')
  videoLoading.value = true
  videoError.value = false
  // 优先使用 HTTP-FLV：延迟通常 1~3 秒，明显低于 HLS；失败时自动降级 HLS。
  if (flvjs.isSupported()) {
    try {
      const flvUrl = hlsUrl.replace(/\.m3u8(\?.*)?$/i, '.flv$1')
      flvPlayer = flvjs.createPlayer({
        type: 'flv',
        url: flvUrl,
        isLive: true,
        hasAudio: true
      }, {
        enableStashBuffer: false,
        stashInitialSize: 128
      })
      flvPlayer.attachMediaElement(video)
      flvPlayer.load()
      flvPlayer.play?.().catch(() => {})
      flvPlayer.on(flvjs.Events.ERROR, () => {
        destroyFlv()
        attachHls(video, hlsUrl)
      })
      return
    } catch (e) {
      destroyFlv()
    }
  }
  attachHls(video, hlsUrl)
}

function onVideoError() {
  videoError.value = true
  videoLoading.value = false
}

function retryVideo() {
  videoError.value = false
  videoLoading.value = true
  nextTick(attachVideo)
}

function connectWs() {
  if (ws) {
    try { ws.close() } catch (e) { /* ignore */ }
    ws = null
  }
  wsState.value = 'connecting'
  const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
  const token = localStorage.getItem('km_token') || ''
  ws = new WebSocket(`${protocol}://${location.host}/ws/live?roomId=${route.params.id}&token=${token}`)
  ws.onopen = () => {
    wsState.value = 'connected'
    heartbeat = setInterval(() => ws.readyState === WebSocket.OPEN && ws.send('ping'), 25000)
  }
  ws.onmessage = (e) => {
    if (e.data === 'pong') return
    try {
      const msg = JSON.parse(e.data)
      if (msg.type === 'ROOM_METRICS') {
        if (msg.onlineCount != null) {
          room.value.realtimeOnline = Number(msg.onlineCount) || 0
          room.value.onlineCount = Number(msg.onlineCount) || 0
        }
        if (msg.totalViewers != null) room.value.totalViewers = Number(msg.totalViewers) || 0
        if (msg.peakOnline != null) room.value.peakOnline = Number(msg.peakOnline) || 0
        if (msg.likeCount != null) room.value.likeCount = Number(msg.likeCount) || 0
        if (msg.giftIncome != null) room.value.giftIncome = Number(msg.giftIncome) || 0
        return
      }
      if (msg.type === 'STREAM_STATUS') {
        if (msg.content === 'ONLINE') {
          videoError.value = false
          videoLoading.value = true
          window.setTimeout(() => attachVideo(), 700)
        } else {
          destroyVideoPlayers()
          onVideoError()
        }
        return
      }
      danmakuList.value.push(msg)
      if (danmakuList.value.length > 200) danmakuList.value.shift()
      nextTick(() => {
        if (dmListRef.value) dmListRef.value.scrollTop = dmListRef.value.scrollHeight
      })
    } catch (err) {
      /* ignore malformed messages */
    }
  }
  ws.onclose = () => {
    wsState.value = 'disconnected'
    if (heartbeat) clearInterval(heartbeat)
  }
  ws.onerror = () => {
    wsState.value = 'disconnected'
  }
}

function reconnectWs() {
  connectWs()
}

function sendDanmaku() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return
  }
  if (!danmakuText.value.trim()) return
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    ElMessage.warning('弹幕连接未就绪')
    return
  }
  ws.send(JSON.stringify({ content: danmakuText.value.trim() }))
  danmakuText.value = ''
}

function useQuickReply(text) {
  danmakuText.value = text
}

async function sendGift(gift) {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await liveApi.sendGift({ roomId: route.params.id, giftId: gift.id, count: 1 })
    ElMessage.success(`已送出 ${gift.name}`)
    await loadGifts()
  } catch (e) {
    /* request interceptor reports the error */
  }
}

async function openCoinRecharge() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  coin.visible = true
  coin.loading = true
  coin.error = ''
  coin.qr = ''
  coin.orderNo = ''
  coin.statusText = ''
  try {
    const [packageRes, channelRes] = await Promise.all([
      coinPackages.value.length ? Promise.resolve(null) : vipApi.coinPackages(),
      vipApi.payChannels()
    ])
    if (packageRes) coinPackages.value = packageRes.data || []
    Object.assign(channels, channelRes.data || {})
    if (!coinPackages.value.some((pkg) => pkg.id === coin.packageId)) {
      coin.packageId = coinPackages.value[0]?.id || null
    }
    coin.method = channels.wechatEnabled || !channels.alipayEnabled ? 'WECHAT' : 'ALIPAY'
    if (!channelAvailable(coin.method)) {
      coin.method = coin.method === 'WECHAT' ? 'ALIPAY' : 'WECHAT'
    }
  } catch (e) {
    coin.error = e?.response?.data?.message || e?.message || '充值信息加载失败，请稍后重试'
  } finally {
    coin.loading = false
  }
}

function stopCoinPolling() {
  if (coin.timer) {
    clearInterval(coin.timer)
    coin.timer = null
  }
}

function resetCoinDialog() {
  stopCoinPolling()
  coin.loading = false
  coin.submitting = false
  coin.qr = ''
  coin.orderNo = ''
  coin.statusText = ''
  coin.error = ''
}

async function finishCoinRecharge() {
  stopCoinPolling()
  await loadGifts()
  ElMessage.success(`充值成功，当前余额 ${formatNumber(balance.value)} 酷猫币`)
  coin.visible = false
}

function startCoinPolling(orderNo) {
  stopCoinPolling()
  coin.statusText = '等待扫码支付…'
  coin.timer = setInterval(async () => {
    try {
      const res = await vipApi.payStatus(orderNo)
      if (res.data?.status === 'PAID') {
        await finishCoinRecharge()
      } else if (res.data?.status === 'CLOSED') {
        stopCoinPolling()
        coin.statusText = '订单已关闭，请重新充值'
      }
    } catch (e) {
      /* keep polling; interceptor reports transport errors */
    }
  }, 2500)
}

async function submitCoinRecharge() {
  if (!selectedCoinPackage.value) return
  coin.submitting = true
  coin.error = ''
  try {
    const res = await vipApi.createPayment({
      packageId: selectedCoinPackage.value.id,
      payType: coin.method
    })
    const data = res.data
    if (data.mock) {
      await ElMessageBox.confirm(
        `当前为本地模拟支付，确认支付 ¥${formatPrice(data.amount)} 购买 ${formatNumber(selectedCoinPackage.value.coinAmount)} 酷猫币？`,
        '模拟支付',
        { type: 'warning', confirmButtonText: '模拟支付成功', cancelButtonText: '取消' }
      )
      const paid = await vipApi.mockPay(data.orderNo)
      if (paid.data?.status === 'PAID') await finishCoinRecharge()
      return
    }
    if (data.payType === 'ALIPAY') {
      window.location.href = data.payUrl
      return
    }
    if (data.payType === 'WECHAT') {
      coin.qr = data.qrCode
      coin.orderNo = data.orderNo
      startCoinPolling(data.orderNo)
    }
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    coin.error = e?.response?.data?.message || e?.message || '充值下单失败，请稍后重试'
  } finally {
    coin.submitting = false
  }
}

async function shareRoom() {
  const url = window.location.href
  try {
    if (navigator.share) {
      await navigator.share({ title: room.value?.title || '酷猫音乐直播间', url })
    } else {
      await navigator.clipboard.writeText(url)
      ElMessage.success('直播间链接已复制')
    }
  } catch (e) {
    if (e?.name !== 'AbortError') ElMessage.warning('复制失败，请手动复制浏览器地址')
  }
}

onMounted(async () => {
  await load()
  if (!room.value) return
  await loadGifts()
  connectWs()
})

onBeforeUnmount(() => {
  destroyVideoPlayers()
  if (ws) ws.close()
  if (heartbeat) clearInterval(heartbeat)
})
</script>

<style scoped>
.live-page { padding-top: 12px; }

.live-topline {
  display: flex;
  align-items: center;
  gap: 9px;
  min-height: 24px;
  margin-bottom: 10px;
  font-size: 12px;
  color: var(--km-text-faint);
}
.back-link { display: inline-flex; align-items: center; gap: 4px; color: var(--km-text-dim); transition: color .16s; }
.back-link:hover { color: var(--km-text); }
.topline-sep { opacity: .5; }
.topline-current { color: var(--km-text-dim); }
.room-code { margin-left: auto; font-variant-numeric: tabular-nums; letter-spacing: .02em; }

.live-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) clamp(330px, 26vw, 400px);
  grid-template-areas:
    "stage chat"
    "assets assets";
  gap: 18px;
  align-items: stretch;
}
.live-main { display: contents; }
.stage-card { grid-area: stage; }
.asset-band {
  grid-area: assets;
  display: grid;
  grid-template-columns: minmax(280px, .82fr) minmax(0, 1.18fr);
  gap: 14px;
  align-items: stretch;
}
.replay-bar { grid-column: 1 / -1; }

/* ---------------- 主舞台 ---------------- */
.stage-card {
  background: var(--km-card);
  border: 1px solid var(--km-border);
  border-radius: var(--km-radius);
  overflow: hidden;
  position: relative;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, .05);
}
.stage-media {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  min-height: 0;
  background: #050407;
  overflow: hidden;
}
.stage-video {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: contain;
  background: #050407;
}
.stage-scrim {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(180deg, rgba(5, 4, 7, .72) 0%, transparent 24%), /* taste-skill-ignore: neutral scrim over live video */
    linear-gradient(0deg, rgba(5, 4, 7, .56) 0%, transparent 20%); /* taste-skill-ignore: neutral scrim over live video */
}

.stage-top {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 15px;
  z-index: 2;
  pointer-events: none;
}
.stage-top > * { pointer-events: auto; }
.anchor-chip {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
  padding: 5px 11px 5px 5px;
  border-radius: 12px;
  background: rgba(9, 8, 12, .72);
  border: 1px solid rgba(255, 255, 255, .12);
}
.anchor-avatar { flex-shrink: 0; background: #2a2136; color: #f0abfc; font-size: 13px; }
.anchor-copy { min-width: 0; }
.anchor-name-row { display: flex; align-items: center; gap: 6px; }
.anchor-name {
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.anchor-role {
  font-size: 11px;
  line-height: 16px;
  padding: 0 5px;
  border-radius: 4px;
  color: #f5d0fe;
  background: rgba(216, 180, 254, .16);
}
.anchor-sub { font-size: 11px; color: rgba(255, 255, 255, .55); margin-top: 1px; max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.stage-status { display: flex; align-items: center; gap: 7px; flex-wrap: wrap; justify-content: flex-end; }
.live-state, .signal-state, .viewer-state {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 28px;
  padding: 0 9px;
  border-radius: 8px;
  background: rgba(9, 8, 12, .7);
  border: 1px solid rgba(255, 255, 255, .11);
  color: rgba(255, 255, 255, .8);
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
}
.live-state { color: #fecdd3; border-color: rgba(251, 113, 133, .25); background: rgba(80, 12, 25, .52); }
.live-state i { width: 5px; height: 5px; border-radius: 50%; background: #fb7185; animation: km-pulse 1.6s infinite; }
.live-state.off { color: var(--km-text-dim); background: rgba(9, 8, 12, .7); border-color: rgba(255, 255, 255, .11); }
.live-state.off i { background: var(--km-text-faint); animation: none; }
.signal-state i { width: 5px; height: 5px; border-radius: 50%; background: #8b8497; }
.signal-state.is-good { color: #bbf7d0; }
.signal-state.is-good i { background: #4ade80; }
.signal-state.is-error { color: #fecdd3; }
.signal-state.is-error i { background: #fb7185; }
.signal-state.is-connecting { color: #fde68a; }
.signal-state.is-connecting i { background: #fbbf24; }
.viewer-state b { font-size: 11px; font-weight: 600; font-variant-numeric: tabular-nums; }
.stage-icon-btn {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, .12);
  background: rgba(9, 8, 12, .7);
  color: rgba(255, 255, 255, .75);
  cursor: pointer;
  transition: color .16s, background .16s;
}
.stage-icon-btn:hover { color: #fff; background: rgba(255, 255, 255, .12); }

.stage-caption {
  position: absolute;
  left: 16px;
  bottom: 12px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  max-width: calc(100% - 140px);
  padding: 5px 9px;
  border-radius: 6px;
  color: rgba(255, 255, 255, .82);
  background: rgba(5, 4, 7, .62);
  font-size: 11px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.caption-dot { width: 5px; height: 5px; border-radius: 50%; background: #fb7185; flex-shrink: 0; }

/* 空态 / 断流 */
.media-state {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 50px 26px 30px;
  text-align: center;
  background: #07060a;
}
.signal-art { width: 98px; height: 98px; margin-bottom: 2px; opacity: .95; }
.signal-art svg { width: 100%; height: 100%; display: block; }
.state-title { margin: 0; font-size: 20px; line-height: 1.25; font-weight: 600; letter-spacing: -.02em; color: #fff; }
.state-copy { margin: 0; max-width: 420px; font-size: 13px; line-height: 1.6; color: var(--km-text-dim); }
.state-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; justify-content: center; margin-top: 8px; }
.state-actions .km-btn { height: 34px; font-size: 12px; }

.loading-track { width: 160px; height: 3px; border-radius: 3px; background: rgba(255, 255, 255, .08); overflow: hidden; margin-top: 8px; }
.loading-track span { display: block; width: 38%; height: 100%; border-radius: inherit; background: var(--km-accent); animation: loading-slide 1.5s ease-in-out infinite; }
@keyframes loading-slide { 0% { transform: translateX(-100%); } 100% { transform: translateX(360%); } }
.tech-details { margin-top: 4px; font-size: 11px; color: var(--km-text-faint); max-width: 90%; }
.tech-details summary { cursor: pointer; color: var(--km-text-faint); }
.tech-details summary:hover { color: var(--km-text-dim); }
.tech-details code { display: block; max-width: 460px; margin-top: 6px; padding: 6px 9px; border-radius: 6px; background: rgba(255, 255, 255, .04); color: var(--km-text-dim); font-size: 11px; line-height: 1.5; word-break: break-all; }
.state-hint { margin: 2px 0 0; max-width: 420px; color: var(--km-text-faint); font-size: 11px; line-height: 1.5; }

/* 信息条 */
.stage-info {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 14px 18px 12px;
}
.stage-copy { min-width: 0; }
.stage-meta-line { display: flex; align-items: center; gap: 7px; margin: 6px 0 0; color: var(--km-text-faint); font-size: 11px; flex-wrap: wrap; }
.meta-category { color: #f5d0fe; background: rgba(216, 180, 254, .1); border: 1px solid rgba(216, 180, 254, .16); border-radius: 4px; padding: 2px 6px; }
.meta-location { color: #fde68a; background: rgba(251, 191, 36, .09); border-radius: 4px; padding: 2px 6px; }
.meta-status { color: var(--km-text-faint); }
.stage-title {
  margin: 0;
  font-size: 24px;
  line-height: 1.28;
  font-weight: 700;
  letter-spacing: -.03em;
  color: var(--km-text);
  overflow-wrap: anywhere;
}
.stage-desc { margin: 6px 0 0; font-size: 12px; line-height: 1.55; color: var(--km-text-faint); max-width: 640px; }
.stage-actions { display: flex; align-items: center; gap: 7px; flex-shrink: 0; padding-top: 4px; }
.follow-btn, .share-btn, .like-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background .16s, border-color .16s, color .16s;
}
.follow-btn { border: 1px solid var(--km-accent-line); background: rgba(216, 180, 254, .1); color: #f5eaff; }
.follow-btn:hover { background: rgba(216, 180, 254, .16); }
.follow-btn.followed { background: transparent; border-color: var(--km-border); color: var(--km-text-dim); }
.share-btn { border: 1px solid var(--km-border); background: transparent; color: var(--km-text-dim); }
.share-btn:hover { color: var(--km-text); border-color: var(--km-border-strong); }
.like-btn { border: 1px solid rgba(251, 113, 133, .26); background: rgba(251, 113, 133, .08); color: #ff9fb0; }
.like-btn:hover:not(:disabled) { background: rgba(251, 113, 133, .16); }
.like-btn:disabled { opacity: .45; cursor: not-allowed; }

/* 实时指标 */
.stage-metrics {
  display: flex;
  align-items: center;
  gap: 0;
  border-top: 1px solid var(--km-border);
  padding: 0 18px;
  min-height: 58px;
}
.metric-cell { display: flex; flex-direction: column; gap: 3px; min-width: 92px; padding-right: 24px; margin-right: 24px; border-right: 1px solid var(--km-border); }
.metric-label { font-size: 11px; color: var(--km-text-faint); }
.metric-value { font-size: 17px; line-height: 1.2; font-weight: 600; color: var(--km-text); font-variant-numeric: tabular-nums; }
.metric-note { display: flex; align-items: center; gap: 7px; margin-left: auto; color: var(--km-text-faint); font-size: 11px; }
.mini-signal i { display: block; width: 5px; height: 5px; border-radius: 50%; background: var(--km-text-faint); }
.mini-signal.is-good i { background: #4ade80; }
.mini-signal.is-error i { background: #fb7185; }
.mini-signal.is-connecting i { background: #fbbf24; }
.tabular { font-variant-numeric: tabular-nums; }

/* ---------------- 资产卡 ---------------- */
.wallet-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  grid-template-areas:
    'head balance'
    'actions actions';
  gap: 12px 18px;
  padding: 16px 18px;
  border-radius: var(--km-radius);
  border: 1px solid var(--km-border);
  background: #100d16;
  position: relative;
  overflow: hidden;
}
.wallet-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--km-grad);
  opacity: .72;
}
.wallet-head { grid-area: head; display: flex; align-items: flex-start; gap: 10px; min-width: 0; }
.wallet-mark { width: 30px; height: 30px; flex-shrink: 0; display: grid; place-items: center; border-radius: 9px; background: rgba(216, 180, 254, .1); color: #e9d5ff; }
.wallet-mark svg { width: 21px; height: 21px; display: block; }
.wallet-label { margin: 0; font-size: 13px; font-weight: 600; color: var(--km-text); line-height: 1.4; }
.wallet-note { margin: 3px 0 0; font-size: 11px; color: var(--km-text-faint); line-height: 1.5; }
.wallet-balance { grid-area: balance; display: flex; align-items: baseline; gap: 4px; align-self: center; }
.balance-value { font-size: 32px; line-height: 1; font-weight: 600; letter-spacing: -.04em; color: #fff; font-variant-numeric: tabular-nums; }
.balance-unit { font-size: 11px; color: var(--km-text-faint); }
.wallet-actions { grid-area: actions; display: flex; align-items: center; gap: 12px; border-top: 1px solid var(--km-border); padding-top: 11px; }
.wallet-recharge { height: 34px; padding: 0 16px; border-radius: 9px; font-size: 12px; }
.wallet-pay { font-size: 11px; color: var(--km-text-faint); margin-left: -5px; white-space: nowrap; }
.wallet-login { font-size: 11px; color: var(--km-text-faint); transition: color .16s; }
.wallet-login:hover { color: var(--km-text-dim); }

/* ---------------- 礼物 ---------------- */
.gift-panel { padding: 16px 18px 14px; border: 1px solid var(--km-border); border-radius: var(--km-radius); background: var(--km-card); }
.gift-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.gift-title { margin: 0; font-size: 14px; font-weight: 600; color: var(--km-text); }
.gift-sub { margin: 3px 0 0; font-size: 11px; color: var(--km-text-faint); }
.gift-balance { font-size: 11px; color: var(--km-text-faint); padding-top: 2px; white-space: nowrap; }
.gift-list { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 7px; }
.gift-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 9px 6px 8px;
  border-radius: 10px;
  border: 1px solid var(--km-border);
  background: rgba(255, 255, 255, .025);
  color: var(--km-text-dim);
  cursor: pointer;
  transition: border-color .16s, background .16s, transform .16s;
}
.gift-item:hover { border-color: rgba(216, 180, 254, .28); background: rgba(216, 180, 254, .06); transform: translateY(-1px); }
.gift-icon { display: grid; place-items: center; width: 28px; height: 28px; border-radius: 8px; background: rgba(255, 255, 255, .05); color: #e9d5ff; font-size: 15px; }
.gift-name { font-size: 11px; color: var(--km-text); line-height: 1.3; }
.gift-price { display: inline-flex; align-items: center; gap: 2px; font-size: 11px; color: var(--km-text-faint); font-variant-numeric: tabular-nums; }
.gift-price i { font-style: normal; color: var(--km-accent); font-size: 11px; }
.gift-loading { grid-column: 1 / -1; padding: 10px 0; color: var(--km-text-faint); font-size: 11px; }

.replay-bar {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 11px 14px;
  border-radius: 12px;
  background: var(--km-card);
  border: 1px solid var(--km-border);
  font-size: 12px;
  color: var(--km-text-dim);
}
.replay-ico { color: var(--km-accent); display: inline-flex; }
.replay-link { margin-left: auto; display: inline-flex; align-items: center; gap: 3px; color: var(--km-accent); font-weight: 600; font-size: 12px; }

/* ---------------- 弹幕 ---------------- */
.chat-panel {
  grid-area: chat;
  align-self: stretch;
  display: flex;
  flex-direction: column;
  min-height: 480px;
  background: var(--km-card);
  border: 1px solid var(--km-border);
  border-radius: var(--km-radius);
  overflow: hidden;
}
.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 15px 16px 13px;
  border-bottom: 1px solid var(--km-border);
}
.chat-title-row { display: flex; align-items: center; gap: 7px; min-width: 0; }
.chat-title { margin: 0; font-size: 15px; font-weight: 600; letter-spacing: -.01em; color: var(--km-text); }
.chat-count { display: inline-flex; align-items: center; justify-content: center; min-width: 19px; height: 18px; padding: 0 5px; border-radius: 5px; background: rgba(255, 255, 255, .06); color: var(--km-text-faint); font-size: 11px; font-variant-numeric: tabular-nums; }
.conn-state { display: inline-flex; align-items: center; gap: 5px; font-size: 11px; color: var(--km-text-faint); white-space: nowrap; }
.conn-state i { width: 5px; height: 5px; border-radius: 50%; background: var(--km-text-faint); }
.conn-state.connected { color: #bbf7d0; }
.conn-state.connected i { background: #4ade80; }
.conn-state.connecting { color: #fde68a; }
.conn-state.connecting i { background: #fbbf24; animation: km-pulse 1.3s infinite; }
.conn-state.disconnected { color: #fecdd3; }
.conn-state.disconnected i { background: #fb7185; }

.room-notice { padding: 10px 16px 11px; border-bottom: 1px solid var(--km-border); background: rgba(255, 255, 255, .018); }
.notice-label { display: block; margin-bottom: 4px; font-size: 11px; color: var(--km-text-faint); }
.room-notice p { margin: 0; color: var(--km-text-dim); font-size: 11px; line-height: 1.55; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

.chat-tabs { display: flex; align-items: center; gap: 3px; padding: 7px 10px 0; border-bottom: 1px solid var(--km-border); }
.chat-tab { position: relative; height: 31px; padding: 0 8px; border: 0; background: transparent; color: var(--km-text-faint); font-size: 11px; cursor: pointer; transition: color .16s; }
.chat-tab:hover { color: var(--km-text-dim); }
.chat-tab.active { color: var(--km-text); font-weight: 600; }
.chat-tab.active::after { content: ''; position: absolute; left: 8px; right: 8px; bottom: -1px; height: 2px; border-radius: 2px; background: var(--km-accent); }
.chat-tab-note { margin-left: auto; padding-right: 3px; font-size: 11px; color: var(--km-text-faint); }

.chat-summary { display: flex; align-items: center; gap: 0; padding: 0 14px; min-height: 34px; border-bottom: 1px solid var(--km-border); color: var(--km-text-faint); font-size: 11px; }
.chat-summary span { display: inline-flex; align-items: baseline; gap: 3px; padding-right: 10px; margin-right: 10px; border-right: 1px solid var(--km-border); }
.chat-summary span:last-child { border-right: 0; padding-right: 0; margin-right: 0; }
.chat-summary b { color: var(--km-text-dim); font-weight: 600; font-size: 12px; }

.chat-topics { display: flex; flex-direction: column; align-items: flex-start; gap: 4px; padding: 9px 14px 10px; border-bottom: 1px solid var(--km-border); background: rgba(255, 255, 255, .015); }
.topic-label { color: var(--km-text-faint); font-size: 11px; margin-bottom: 1px; }
.chat-topics button { padding: 0; border: 0; background: transparent; color: #d8b4fe; font-size: 11px; line-height: 1.5; cursor: pointer; text-align: left; }
.chat-topics button:hover { color: #f0abfc; }

.chat-list { flex: 1; min-height: 0; overflow-y: auto; display: flex; flex-direction: column; gap: 9px; padding: 12px 14px 14px; scrollbar-width: thin; }
.chat-message { display: flex; gap: 8px; align-items: flex-start; }
.chat-avatar { flex-shrink: 0; background: #261d31; color: #e9d5ff; font-size: 11px; }
.chat-body { flex: 1; min-width: 0; }
.chat-meta { display: flex; align-items: center; gap: 5px; min-width: 0; }
.chat-name { font-size: 11px; font-weight: 600; color: #c4b5fd; max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.chat-name.vip { color: #fcd34d; }
.vip-chip { font-size: 11px; line-height: 13px; padding: 0 3px; border-radius: 3px; background: rgba(251, 191, 36, .12); color: #fcd34d; }
.chat-time { margin-left: auto; font-size: 11px; color: var(--km-text-faint); font-variant-numeric: tabular-nums; }
.chat-text { margin: 2px 0 0; font-size: 12px; line-height: 1.55; color: rgba(247, 244, 252, .86); word-break: break-word; }

.chat-gift { display: flex; align-items: flex-start; gap: 8px; padding: 8px 9px; border-radius: 8px; background: rgba(216, 180, 254, .06); border: 1px solid rgba(216, 180, 254, .1); }
.gift-bullet { display: grid; place-items: center; width: 24px; height: 24px; flex-shrink: 0; border-radius: 7px; background: rgba(216, 180, 254, .12); color: #e9d5ff; font-size: 13px; }
.chat-gift p { margin: 2px 0 0; font-size: 11px; color: var(--km-text-dim); line-height: 1.5; }
.chat-gift b { color: #f5d0fe; font-weight: 600; }
.chat-system { display: flex; align-items: center; gap: 8px; padding: 1px 2px; color: var(--km-text-faint); font-size: 11px; }
.system-line { width: 12px; height: 1px; background: var(--km-border-strong); flex-shrink: 0; }

.chat-sparse { margin-top: 6px; padding: 10px 11px; border-radius: 8px; border: 1px dashed var(--km-border-strong); background: rgba(255, 255, 255, .018); }
.chat-sparse b { display: block; color: var(--km-text-dim); font-size: 11px; font-weight: 600; }
.chat-sparse p { margin: 3px 0 0; color: var(--km-text-faint); font-size: 11px; line-height: 1.55; }

.chat-empty { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; padding: 26px 10px; gap: 6px; }
.empty-mark { width: 48px; height: 48px; margin-bottom: 3px; }
.empty-mark svg { width: 100%; height: 100%; }
.chat-empty b { font-size: 12px; font-weight: 600; color: var(--km-text-dim); }
.chat-empty p { margin: 0; font-size: 11px; color: var(--km-text-faint); line-height: 1.5; }
.quick-replies { display: flex; flex-wrap: wrap; gap: 5px; justify-content: center; margin-top: 7px; }
.quick-replies button { height: 26px; padding: 0 9px; border-radius: 6px; border: 1px solid var(--km-border); background: transparent; color: var(--km-text-dim); font-size: 11px; cursor: pointer; transition: border-color .16s, color .16s; }
.quick-replies button:hover { color: var(--km-text); border-color: var(--km-border-strong); }

.conn-banner { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin: 0 12px 8px; padding: 7px 9px; border-radius: 7px; background: rgba(251, 113, 133, .08); border: 1px solid rgba(251, 113, 133, .16); color: #fecdd3; font-size: 11px; line-height: 1.4; }
.conn-banner button { border: 0; background: transparent; color: #fda4af; font-size: 11px; font-weight: 600; cursor: pointer; white-space: nowrap; }

.chat-quickbar { display: flex; align-items: center; gap: 5px; padding: 7px 12px; border-top: 1px solid var(--km-border); overflow-x: auto; scrollbar-width: none; }
.chat-quickbar::-webkit-scrollbar { display: none; }
.quickbar-label { flex-shrink: 0; color: var(--km-text-faint); font-size: 11px; }
.chat-quickbar button { flex-shrink: 0; height: 24px; padding: 0 8px; border: 1px solid var(--km-border); border-radius: 5px; background: transparent; color: var(--km-text-dim); font-size: 11px; cursor: pointer; white-space: nowrap; }
.chat-quickbar button:hover { color: var(--km-text); border-color: var(--km-border-strong); }
.chat-composer { border-top: 1px solid var(--km-border); padding: 10px 12px 11px; background: #0c0a10; }
.composer-field { display: flex; align-items: center; gap: 7px; min-height: 38px; padding: 0 5px 0 10px; border-radius: 9px; border: 1px solid var(--km-border); background: rgba(255, 255, 255, .035); transition: border-color .16s; }
.composer-field:focus-within { border-color: var(--km-accent-line); }
.composer-icon { color: var(--km-text-faint); font-size: 14px; flex-shrink: 0; }
.composer-input { flex: 1; min-width: 0; height: 34px; padding: 0; border: 0; outline: none; background: transparent; color: var(--km-text); font-size: 12px; }
.composer-input::placeholder { color: var(--km-text-faint); }
.composer-count { font-size: 11px; color: var(--km-text-faint); font-variant-numeric: tabular-nums; white-space: nowrap; }
.send-btn { height: 29px; padding: 0 13px; border: 0; border-radius: 7px; background: var(--km-accent); color: #16091f; font-size: 11px; font-weight: 600; cursor: pointer; transition: filter .16s, opacity .16s; }
.send-btn:hover:not(:disabled) { filter: brightness(1.08); }
.send-btn:disabled { opacity: .35; cursor: not-allowed; }
.composer-foot { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-top: 7px; color: var(--km-text-faint); font-size: 11px; }
.vip-hint { display: inline-flex; align-items: center; gap: 3px; color: #d8b4fe; font-size: 11px; }
.vip-hint:hover { color: #f0abfc; }

/* ---------------- 错误 / 骨架 ---------------- */
.live-state-page { min-height: 60vh; display: grid; place-items: center; }
.load-error { display: flex; flex-direction: column; align-items: center; text-align: center; gap: 9px; padding: 40px; }
.error-mark { width: 48px; height: 48px; }
.error-mark svg { width: 100%; height: 100%; }
.load-error h1 { margin: 0; font-size: 20px; font-weight: 600; }
.load-error p { margin: 0; color: var(--km-text-faint); font-size: 12px; }
.live-skeleton { display: flex; flex-direction: column; gap: 12px; }
.sk-line, .sk-stage { border-radius: 12px; background: rgba(255, 255, 255, .05); }
.sk-line { height: 18px; width: 42%; }
.sk-line.short { width: 160px; height: 12px; }
.sk-stage { aspect-ratio: 16 / 7; background: #0f0d14; border: 1px solid var(--km-border); }

/* ---- 可读性层级 ---- */
.stage-desc, .wallet-note, .gift-sub, .gift-balance, .notice-label, .room-notice p,
.chat-summary, .chat-tab, .chat-gift p, .chat-system, .chat-empty p, .chat-sparse p,
.composer-foot, .vip-hint, .chat-quickbar button, .gift-name, .topic-label,
.chat-topics button, .chat-empty b { font-size: 13px; }
.chat-text, .composer-input { font-size: 13px; }
.metric-label, .anchor-sub, .stage-meta-line, .chat-name, .chat-time, .chat-count, .conn-state,
.chat-tab-note, .gift-price, .wallet-login, .tech-details, .state-copy { font-size: 12px; }
.stage-desc { font-size: 13px; }
.anchor-sub, .stage-meta-line, .metric-label, .chat-tab, .chat-summary,
.chat-name, .chat-text, .chat-gift p, .chat-system, .chat-empty p,
.room-notice p, .composer-input, .chat-quickbar button, .gift-name { font-size: 12px; }
.chat-empty b { font-size: 13px; }
.chat-text, .composer-input { font-size: 13px; }
.chat-empty p, .composer-foot, .vip-hint { font-size: 11px; }

@media (max-width: 1180px) {
  .live-workspace { grid-template-columns: minmax(0, 1fr) 320px; gap: 14px; }
  .stage-title { font-size: 21px; }
  .metric-cell { min-width: 78px; padding-right: 14px; margin-right: 14px; }
  .chat-panel { min-height: 500px; }
}

@media (max-width: 980px) {
  .live-workspace { grid-template-columns: 1fr; grid-template-areas: "stage" "chat" "assets"; }
  .chat-panel { position: static; height: 560px; max-height: none; min-height: 0; }
  .stage-metrics { flex-wrap: wrap; gap: 8px 0; padding-top: 10px; padding-bottom: 10px; }
  .metric-note { width: 100%; margin-left: 0; padding-top: 3px; }
}

@media (max-width: 720px) {
  .live-page { padding-top: 12px; }
  .live-topline { margin-bottom: 10px; }
  .stage-media { aspect-ratio: 4 / 3; max-height: 54vh; min-height: 250px; }
  .stage-top { padding: 10px; flex-direction: column; gap: 8px; }
  .stage-status { justify-content: flex-start; }
  .stage-icon-btn { margin-left: auto; }
  .anchor-role, .signal-state { display: none; }
  .stage-info { flex-direction: column; padding: 14px 14px 12px; gap: 10px; }
  .stage-title { font-size: 19px; }
  .stage-actions { width: 100%; }
  .follow-btn, .share-btn, .like-btn { flex: 1; justify-content: center; height: 34px; }
  .stage-metrics { padding: 10px 14px; }
  .metric-cell { flex: 1; min-width: 0; padding-right: 10px; margin-right: 10px; }
  .metric-value { font-size: 15px; }
  .wallet-card { grid-template-columns: 1fr; grid-template-areas: 'head' 'balance' 'actions'; padding: 15px; }
  .wallet-balance { align-self: auto; }
  .balance-value { font-size: 28px; }
  .gift-list { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .gift-balance { display: none; }
  .media-state { padding: 40px 16px 24px; gap: 8px; }
  .signal-art { width: 74px; height: 74px; }
  .state-title { font-size: 17px; }
  .state-copy { font-size: 12px; }
  .state-actions { margin-top: 4px; }
  .state-actions .km-btn { height: 32px; font-size: 11px; }
  .stage-caption { display: none; }
  .chat-panel { height: 560px; }
  .asset-band { grid-template-columns: 1fr; }
  .chat-head { padding: 13px 14px 11px; }
  .room-notice { padding: 9px 14px 10px; }
  .chat-list { padding: 10px 12px 12px; }
  .chat-composer { padding: 9px 10px 10px; }
}

@media (max-width: 420px) {
  .stage-status .viewer-state { display: none; }
  .metric-label { font-size: 11px; }
  .composer-count { display: none; }
  .vip-hint { font-size: 11px; }
  .composer-foot > span { font-size: 11px; }
}
/* ---- 最终字号校准 ---- */
.state-copy, .gift-name, .chat-summary, .chat-summary span, .chat-tab-note { font-size: 13px; }
.chat-summary b { font-size: 13px; }
.composer-foot, .vip-hint, .gift-price, .chat-count, .conn-state, .chat-time { font-size: 13px; }
.live-topline, .anchor-sub, .stage-meta-line, .metric-label, .chat-tab,
.chat-name, .conn-state, .wallet-login { font-size: 13px; }
.chat-count, .chat-time, .gift-price { font-size: 11px; }
.composer-foot, .vip-hint { font-size: 12px; }
@media (max-width: 720px) {
  .state-hint { display: none; }
}
</style>

<style>
.coin-recharge-dialog {
  background: #fff !important;
  border: 1px solid #ece7f2 !important;
  border-radius: 16px !important;
  box-shadow: 0 24px 70px rgba(40, 22, 63, .16) !important;
}
.coin-recharge-dialog .el-dialog__header { padding: 20px 20px 0; margin: 0; }
.coin-recharge-dialog .el-dialog__body { padding: 18px 20px 16px; color: #1d1726; }
.coin-recharge-dialog .el-dialog__footer { padding: 0 20px 18px; }
.coin-recharge-dialog .el-dialog__headerbtn .el-dialog__close { color: #8b8195; }
.coin-recharge-dialog .el-dialog__headerbtn:hover .el-dialog__close { color: #4f465a; }
.coin-modal-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.coin-modal-head span { color: #6f657b; font-size: 11px; }
.coin-modal-head h2 { margin: 4px 0 0; color: #1d1726; font-size: 21px; font-weight: 650; letter-spacing: -.03em; }
.coin-modal-head strong { color: #1d1726; font-size: 21px; font-weight: 600; font-variant-numeric: tabular-nums; }
.coin-modal-head small { color: #776d83; font-size: 11px; font-weight: 400; }
.coin-package-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 9px; }
.coin-package { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 2px 8px; align-items: end; padding: 12px; border: 1px solid #e6deee; border-radius: 10px; background: #fff; color: #1d1726; text-align: left; cursor: pointer; transition: border-color .16s, background .16s, box-shadow .16s; }
.coin-package:hover { border-color: #d3bce6; background: #fdfbff; }
.coin-package.selected { border-color: transparent; background: var(--km-grad); color: #fff; box-shadow: 0 8px 20px rgba(112, 53, 160, .16); }
.coin-package strong { font-size: 24px; line-height: 1; font-weight: 650; font-variant-numeric: tabular-nums; }
.coin-package > span { color: #6f657b; font-size: 10px; }
.coin-package.selected > span { color: rgba(255, 255, 255, .8); }
.coin-package em { grid-column: 1 / -1; margin-top: 7px; color: #776d83; font-size: 11px; font-style: normal; }
.coin-package.selected em { color: #fff; }
.coin-method-title { margin: 19px 0 8px; color: #6f657b; font-size: 11px; }
.coin-methods { display: grid; grid-template-columns: 1fr 1fr; gap: 9px; }
.coin-method { position: relative; display: flex; align-items: center; gap: 9px; min-height: 54px; padding: 9px 10px; border: 1px solid #e6deee; border-radius: 10px; background: #fff; color: #1d1726; text-align: left; cursor: pointer; transition: border-color .16s, background .16s; }
.coin-method:hover:not(:disabled) { border-color: #d3bce6; background: #fdfbff; }
.coin-method.selected { border-color: var(--km-accent); background: #faf4ff; }
.coin-method:disabled { opacity: .45; cursor: not-allowed; }
.coin-method > span:nth-child(2) { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.coin-method b { font-size: 12px; font-weight: 600; }
.coin-method small { color: #776d83; font-size: 10px; }
.pay-logo { width: 30px; height: 30px; flex: none; display: grid; place-items: center; border-radius: 8px; color: #fff; font-size: 14px; font-weight: 700; }
.pay-logo.wechat { background: #07c160; }
.pay-logo.alipay { background: #1677ff; }
.method-dot { position: absolute; right: 9px; top: 9px; width: 6px; height: 6px; border-radius: 50%; background: var(--km-accent); opacity: 0; }
.coin-method.selected .method-dot { opacity: 1; }
.coin-mock-note { margin: 12px 0 0; color: #8a5c00; font-size: 10.5px; line-height: 1.55; }
.coin-error { margin: 11px 0 0; color: #b4233a; font-size: 11px; line-height: 1.5; }
.coin-loading { display: flex; align-items: center; justify-content: center; gap: 8px; min-height: 240px; color: #6f657b; font-size: 12px; }
.coin-spinner { width: 18px; height: 18px; border: 2px solid #eadcf6; border-top-color: var(--km-accent); border-radius: 50%; animation: coin-spin .8s linear infinite; }
@keyframes coin-spin { to { transform: rotate(360deg); } }
.coin-modal-foot { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.coin-modal-foot > span { color: #776d83; font-size: 10.5px; }
.coin-modal-foot > div { display: flex; align-items: center; gap: 7px; margin-left: auto; }
.coin-cancel { height: 34px; padding: 0 10px; border: 0; background: transparent; color: #6f657b; font-size: 12px; cursor: pointer; }
.coin-submit { height: 38px; padding: 0 14px; border: 0; border-radius: 999px; background: var(--km-grad); color: #fff; font-size: 12px; font-weight: 650; cursor: pointer; }
.coin-submit:disabled { opacity: .5; cursor: not-allowed; }
.coin-qr { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 10px 0 14px; }
.coin-qr img { width: 236px; height: 236px; padding: 8px; border-radius: 12px; background: #fff; }
.coin-qr strong { color: #1d1726; font-size: 13px; }
.coin-qr span { color: #776d83; font-size: 11px; }
@media (max-width: 540px) {
  .coin-recharge-dialog { width: calc(100vw - 24px) !important; margin-top: 6vh; }
  .coin-modal-foot { align-items: flex-start; flex-direction: column; }
  .coin-modal-foot > div, .coin-submit { width: 100%; }
  .coin-modal-foot > div { margin-left: 0; }
  .coin-submit { flex: 1; }
  .coin-modal-foot > span { order: 2; }
}
@media (prefers-reduced-motion: reduce) { .coin-spinner { animation: none; } }
</style>
