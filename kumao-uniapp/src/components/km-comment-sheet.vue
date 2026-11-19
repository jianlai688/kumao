<template>
  <view v-if="visible" class="mask" @click="close">
    <view class="sheet" @click.stop>
      <view class="grab"></view>

      <view class="head">
        <text class="title">评论</text>
        <text class="count">{{ total }}</text>
        <view class="sp"></view>
        <view class="close" @click="close">
          <km-icon name="close" :size="14" color="rgba(247,241,232,.6)" />
        </view>
      </view>

      <scroll-view class="list" scroll-y :show-scrollbar="false">
        <view v-for="c in list" :key="c.id" class="item">
          <image class="ava" :src="avatarOf(c.avatar || c.avatarUrl)" mode="aspectFill" />
          <view class="body">
            <view class="row1">
              <text class="nick">{{ c.nickname || c.username || '酷猫用户' }}</text>
              <text class="floor">{{ ago(c.createTime) }}</text>
            </view>
            <text class="content">{{ c.content }}</text>
            <view class="row2">
              <text v-if="c.replyNickname" class="reply">回复 @{{ c.replyNickname }}</text>
              <view class="like">
                <km-icon name="heart" :size="14" color="rgba(247,241,232,.32)" />
                <text class="lc">{{ c.likeCount || 0 }}</text>
              </view>
            </view>
          </view>
        </view>

        <view v-if="loading && !list.length" class="tip">正在加载评论…</view>
        <view v-else-if="!list.length" class="tip">还没有评论，来说两句吧</view>
        <view v-else-if="hasMore" class="more" @click="loadMore">加载更多</view>
        <view v-else class="tip end">— 已经到底啦 —</view>
      </scroll-view>

      <view class="inputbar">
        <input
          v-model="text"
          class="input"
          :placeholder="placeholder"
          placeholder-class="ph"
          confirm-type="send"
          :maxlength="200"
          @confirm="send"
        />
        <view :class="['send', { off: !text.trim() || sending }]" @click="send">{{ sending ? '发送中' : '发送' }}</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'
import KmIcon from '@/components/km-icon.vue'
import { api } from '@/api'
import { avatarOf } from '@/utils/media'

const props = defineProps({
  visible: { type: Boolean, default: false },
  songId: { type: [String, Number], default: '' },
  placeholder: { type: String, default: '说点什么…' }
})

const emit = defineEmits(['update:visible', 'count'])

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 20
const hasMore = ref(false)
const loading = ref(false)
const text = ref('')
const sending = ref(false)

function close() {
  emit('update:visible', false)
}

function ago(t) {
  if (!t) return ''
  const d = new Date(String(t).replace(/-/g, '/').replace('T', ' '))
  const diff = (Date.now() - d.getTime()) / 1000
  if (isNaN(diff)) return ''
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  if (diff < 86400 * 7) return Math.floor(diff / 86400) + ' 天前'
  return `${d.getMonth() + 1} 月 ${d.getDate()} 日`
}

// 雪花 ID 超过 JS Number 安全整数范围（2^53-1），后端已按字符串下发，
// 这里必须保持字符串原样提交，任何 Number()/parseInt() 转换都会丢精度导致「歌曲不存在」。
function resolveSongId() {
  const raw = props.songId
  if (raw === undefined || raw === null) return ''
  const id = String(raw).trim()
  return /^\d+$/.test(id) ? id : ''
}

async function load(reset) {
  const songId = resolveSongId()
  if (!songId) return
  if (reset) {
    pageNum.value = 1
    hasMore.value = false
    list.value = []
  }
  loading.value = true
  const res = await api.comments(songId, { pageNum: pageNum.value, pageSize }).catch(() => null)
  const data = (res && res.data) || {}
  const records = data.records || (Array.isArray(data) ? data : [])
  list.value = reset ? records : list.value.concat(records)
  total.value = data.total != null ? data.total : list.value.length
  hasMore.value = list.value.length < total.value
  loading.value = false
  emit('count', total.value)
}

function loadMore() {
  if (loading.value || !hasMore.value) return
  pageNum.value += 1
  load(false)
}


function send() {
  const content = text.value.trim()
  if (!content || sending.value) return
  if (!uni.getStorageSync('km_token')) {
    uni.showToast({ title: '登录后才能评论', icon: 'none' })
    setTimeout(() => {
      close()
      uni.navigateTo({ url: '/pages/login/login' })
    }, 600)
    return
  }
  sending.value = true
  const songId = resolveSongId()
  if (!songId) {
    sending.value = false
    uni.showToast({ title: '歌曲信息还没加载好，请稍后再试', icon: 'none' })
    return
  }
  api.createComment({ songId, content })
    .then(async (res) => {
      const message = String((res && res.message) || '')
      text.value = ''
      if (message.includes('复核')) {
        uni.showToast({ title: '已提交管理员复核，通过后展示', icon: 'none' })
        return
      }
      await load(true)
      uni.showToast({ title: message || '评论已发布', icon: 'none' })
    })
    .catch(() => {})
    .finally(() => { sending.value = false })
}

watch(
  () => props.visible,
  (v) => {
    if (v) load(true)
  }
)
</script>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  z-index: 300;
  background: rgba(4, 3, 6, .62);
  display: flex;
  align-items: flex-end;
}
.sheet {
  width: 100%;
  max-height: 78vh;
  background: #121116;
  border-radius: 36rpx 36rpx 0 0;
  border-top: 1rpx solid rgba(255, 255, 255, .08);
  padding: 12rpx 0 calc(env(safe-area-inset-bottom) + 16rpx);
  display: flex;
  flex-direction: column;
  animation: up .24s ease-out;
}
@keyframes up {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}
.grab { width: 64rpx; height: 8rpx; border-radius: 8rpx; background: rgba(255, 255, 255, .16); margin: 6rpx auto 10rpx; }
.head { display: flex; align-items: center; gap: 10rpx; padding: 8rpx 36rpx 16rpx; }
.title { font-size: 30rpx; font-weight: 800; color: #f7f1e8; }
.count { font-size: 20rpx; color: rgba(247, 241, 232, .34); }
.sp { flex: 1; }
.close {
  width: 52rpx; height: 52rpx; border-radius: 50%;
  background: rgba(255, 255, 255, .06);
  display: flex; align-items: center; justify-content: center;
}
.list { height: 46vh; padding: 0 30rpx; }
.item { display: flex; gap: 18rpx; padding: 18rpx 0; border-bottom: 1rpx solid rgba(255, 255, 255, .045); }
.ava { width: 64rpx; height: 64rpx; border-radius: 50%; background: #1a1c21; flex-shrink: 0; }
.body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8rpx; }
.row1 { display: flex; align-items: center; gap: 12rpx; }
.nick { font-size: 21rpx; color: rgba(247, 241, 232, .55); }
.floor { font-size: 17rpx; color: rgba(247, 241, 232, .24); }
.content { font-size: 25rpx; line-height: 1.55; color: rgba(247, 241, 232, .86); word-break: break-all; }
.row2 { display: flex; align-items: center; gap: 14rpx; margin-top: 2rpx; }
.reply { font-size: 18rpx; color: rgba(245, 180, 91, .7); }
.like { margin-left: auto; display: flex; align-items: center; gap: 6rpx; }
.lc { font-size: 17rpx; color: rgba(247, 241, 232, .32); }
.lc.on { color: var(--km-accent); }
.tip { padding: 40rpx 0; text-align: center; font-size: 20rpx; color: rgba(247, 241, 232, .28); }
.more { padding: 24rpx 0; text-align: center; font-size: 20rpx; color: rgba(245, 180, 91, .8); }
.end { color: rgba(247, 241, 232, .18); }
.inputbar {
  display: flex; align-items: center; gap: 14rpx;
  margin: 10rpx 30rpx 0;
  padding: 10rpx 10rpx 10rpx 22rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, .06);
  border: 1rpx solid rgba(255, 255, 255, .08);
}
.input { flex: 1; height: 56rpx; font-size: 23rpx; color: #f7f1e8; }
.ph { color: rgba(247, 241, 232, .28); }
.send {
  height: 52rpx; padding: 0 24rpx; border-radius: 999rpx;
  background: linear-gradient(140deg, #fbd08a, #e08b3e);
  color: #2b1a08; font-size: 20rpx; font-weight: 800;
  display: flex; align-items: center;
}
.send.off { opacity: .45; }
</style>



