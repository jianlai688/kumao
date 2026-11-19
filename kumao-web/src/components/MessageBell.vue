<template>
  <el-popover
    v-model:visible="open"
    placement="bottom-end"
    :width="360"
    trigger="click"
    popper-class="km-msg-pop"
  >
    <template #reference>
      <button
        class="icon-btn bell-btn"
        :aria-label="unreadCount ? `消息，${unreadCount} 条未读` : '消息'"
        title="消息"
      >
        <KmIcon name="bell" :size="18" />
        <span v-if="unreadCount" class="bell-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
      </button>
    </template>

    <div class="msg-panel">
      <div class="msg-head">
        <div>
          <b>消息</b>
          <span v-if="unreadCount">{{ unreadCount }} 条未读</span>
        </div>
        <button v-if="unreadCount" type="button" class="msg-read-all" @click="markAllRead">全部已读</button>
      </div>

      <div class="msg-tabs" role="tablist" aria-label="消息分类">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          type="button"
          :class="{ active: activeTab === tab.value }"
          @click="activeTab = tab.value"
        >
          {{ tab.label }}
          <span v-if="countByTab(tab.value)">{{ countByTab(tab.value) }}</span>
        </button>
      </div>

      <div class="msg-list">
        <button
          v-for="message in filteredMessages"
          :key="message.id"
          type="button"
          :class="['msg-item', { unread: !message.read }]"
          @click="openMessage(message)"
        >
          <span class="msg-icon" :class="message.type.toLowerCase()">
            <KmIcon :name="messageIcon(message)" :size="15" />
          </span>
          <span class="msg-copy">
            <strong>{{ message.title }}</strong>
            <small>{{ message.content }}</small>
            <time>{{ formatTime(message.time) }}</time>
          </span>
          <i v-if="!message.read" class="msg-unread-dot" aria-label="未读"></i>
        </button>
        <p v-if="!filteredMessages.length" class="msg-empty">暂无消息</p>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import KmIcon from '@/components/KmIcon.vue'

const router = useRouter()
const userStore = useUserStore()
const open = ref(false)
const activeTab = ref('ALL')
const messages = ref([])

const tabs = [
  { value: 'ALL', label: '全部' },
  { value: 'INTERACT', label: '互动' },
  { value: 'SYSTEM', label: '系统' }
]

const storageKey = computed(() => `km_messages_v1_${userStore.user?.userId || 'guest'}`)
const unreadCount = computed(() => messages.value.filter((item) => !item.read).length)
const filteredMessages = computed(() => {
  const list = activeTab.value === 'ALL'
    ? messages.value
    : messages.value.filter((item) => item.type === activeTab.value)
  return [...list].sort((a, b) => Number(b.time) - Number(a.time))
})

function seedMessages() {
  const now = Date.now()
  const items = [
    {
      id: 'welcome',
      type: 'SYSTEM',
      title: '欢迎来到酷猫音乐',
      content: '完善个人资料，发现更适合你的音乐和现场。',
      time: now - 1000 * 60 * 12,
      read: false,
      link: '/mine',
      icon: 'sparkle'
    },
    {
      id: 'live',
      type: 'INTERACT',
      title: '直播预告',
      content: '今晚 22:00「午夜电台」开播，去直播广场看看。',
      time: now - 1000 * 60 * 80,
      read: false,
      link: '/live',
      icon: 'live'
    }
  ]
  if (userStore.hasRole('VIP')) {
    items.push({
      id: 'vip',
      type: 'SYSTEM',
      title: 'VIP 会员权益已生效',
      content: '无损音质、专属弹幕与会员标识已开启。',
      time: now - 1000 * 60 * 60 * 5,
      read: true,
      link: '/vip',
      icon: 'crown'
    })
  }
  if (userStore.hasRole('ANCHOR')) {
    items.push({
      id: 'anchor-settlement',
      type: 'SYSTEM',
      title: '直播收益已结算',
      content: '最近一场直播的礼物收益已结算，可前往礼物收益查看。',
      time: now - 1000 * 60 * 60 * 26,
      read: false,
      link: '/studio/earnings',
      icon: 'crown'
    })
  }
  if (userStore.hasRole('MUSICIAN')) {
    items.push({
      id: 'musician-review',
      type: 'SYSTEM',
      title: '音乐人作品提醒',
      content: '你的作品数据已更新，去音乐人工作台查看。',
      time: now - 1000 * 60 * 60 * 30,
      read: true,
      link: '/studio/musician',
      icon: 'mic'
    })
  }
  return items
}

function loadMessages() {
  try {
    const raw = localStorage.getItem(storageKey.value)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) {
        messages.value = parsed
        return
      }
    }
  } catch (e) { /* ignore broken cache */ }
  messages.value = seedMessages()
  persist()
}

function persist() {
  try {
    localStorage.setItem(storageKey.value, JSON.stringify(messages.value))
  } catch (e) { /* ignore quota errors */ }
}

function markRead(message) {
  if (message.read) return
  message.read = true
  persist()
}

function markAllRead() {
  messages.value.forEach((item) => { item.read = true })
  persist()
}

function openMessage(message) {
  markRead(message)
  open.value = false
  if (message.link) router.push(message.link)
}

function countByTab(tab) {
  if (tab === 'ALL') return messages.value.filter((item) => !item.read).length
  return messages.value.filter((item) => item.type === tab && !item.read).length
}

function messageIcon(message) {
  return message.icon || (message.type === 'INTERACT' ? 'heart' : 'bell')
}

function formatTime(value) {
  const time = Number(value)
  if (!time) return ''
  const diff = Date.now() - time
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / 3600000)} 小时前`
  const date = new Date(time)
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

watch(storageKey, loadMessages, { immediate: true })
</script>

<style scoped>
.bell-btn {
  position: relative;
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--km-text-dim);
  cursor: pointer;
  transition: color .16s, background .16s;
}
.bell-btn:hover { color: #1d1726; background: #f8f4fb; }
.bell-badge {
  position: absolute;
  top: 3px;
  right: 2px;
  min-width: 14px;
  height: 14px;
  padding: 0 3px;
  border-radius: 999px;
  background: #e11d48;
  color: #fff;
  font-size: 9px;
  font-weight: 700;
  line-height: 14px;
  text-align: center;
  box-shadow: 0 0 0 2px var(--km-bg-soft);
}
.msg-panel { color: var(--km-text); }
.msg-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 2px 2px 10px; }
.msg-head > div { display: flex; align-items: baseline; gap: 8px; }
.msg-head b { font-size: 15px; font-weight: 800; }
.msg-head span { color: var(--km-text-faint); font-size: 11px; }
.msg-read-all { border: 0; background: transparent; color: var(--km-brand-violet); font: inherit; font-size: 11px; cursor: pointer; }
.msg-read-all:hover { text-decoration: underline; }
.msg-tabs { display: flex; gap: 6px; padding: 0 2px 9px; border-bottom: 1px solid var(--km-border); }
.msg-tabs button { height: 25px; padding: 0 10px; border: 1px solid transparent; border-radius: 999px; background: transparent; color: var(--km-text-faint); font: inherit; font-size: 11px; cursor: pointer; }
.msg-tabs button.active { background: var(--km-brand-violet-soft); color: var(--km-brand-violet-dark); font-weight: 700; }
.msg-tabs button span { margin-left: 4px; font-size: 9px; opacity: .75; }
.msg-list { max-height: 360px; overflow: auto; padding: 6px 0 0; }
.msg-item { position: relative; display: flex; align-items: flex-start; gap: 9px; width: 100%; padding: 10px 6px; border: 0; border-radius: 9px; background: transparent; color: inherit; text-align: left; cursor: pointer; }
.msg-item:hover { background: #faf7fd; }
.msg-item.unread { background: #fdfbff; }
.msg-icon { width: 28px; height: 28px; flex: none; display: grid; place-items: center; border-radius: 8px; background: #f4ecfb; color: var(--km-brand-violet); }
.msg-icon.interact { background: #fff0f4; color: #e11d48; }
.msg-icon.system { background: #f4ecfb; color: var(--km-brand-violet); }
.msg-copy { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 2px; }
.msg-copy strong { overflow: hidden; color: var(--km-text); font-size: 12px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.msg-copy small { overflow: hidden; color: var(--km-text-dim); font-size: 11px; line-height: 1.5; text-overflow: ellipsis; white-space: nowrap; }
.msg-copy time { color: var(--km-text-faint); font-size: 10px; }
.msg-unread-dot { width: 6px; height: 6px; flex: none; margin-top: 6px; border-radius: 50%; background: #e11d48; }
.msg-empty { margin: 0; padding: 34px 0; color: var(--km-text-faint); font-size: 12px; text-align: center; }
</style>
