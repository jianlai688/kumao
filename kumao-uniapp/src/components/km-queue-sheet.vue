<template>
  <view v-if="visible" class="mask" @click="close">
    <view class="sheet" @click.stop>
      <view class="grab"></view>

      <view class="head">
        <text class="title">播放列表</text>
        <text class="count">{{ state.queue.length }} 首</text>
        <view class="sp"></view>
        <view class="clear" @click="onClear">
          <text>清空</text>
        </view>
        <view class="close" @click="close">
          <km-icon name="close" :size="14" color="rgba(247,241,232,.6)" />
        </view>
      </view>

      <scroll-view class="list" scroll-y :show-scrollbar="false">
        <view
          v-for="(s, i) in state.queue"
          :key="(s.songId || s.id) + '-' + i"
          :class="['qrow', { on: i === state.index }]"
          @click="pick(i)"
        >
          <text class="no">{{ i + 1 }}</text>
          <image class="cv" :src="coverOf(s.coverUrl)" mode="aspectFill" />
          <view class="meta">
            <text class="n">{{ s.name }}</text>
            <text class="a">{{ s.singerName || '未知歌手' }}</text>
          </view>
          <view v-if="i === state.index" class="eq">
            <view class="b b1"></view>
            <view class="b b2"></view>
            <view class="b b3"></view>
          </view>
        </view>

        <view v-if="!state.queue.length" class="tip">
          播放列表还是空的，去发现音乐挑一首吧
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
import KmIcon from '@/components/km-icon.vue'
import { usePlayer } from '@/utils/player'
import { coverOf } from '@/utils/media'

const props = defineProps({ visible: { type: Boolean, default: false } })
const emit = defineEmits(['update:visible'])

const { state, playAt, clear } = usePlayer()

function close() {
  emit('update:visible', false)
}

function pick(i) {
  if (i === state.index) {
    close()
    return
  }
  playAt(i)
  close()
}

function onClear() {
  close()
  clear()
  uni.showToast({ title: '已清空播放列表', icon: 'none' })
}
</script>

<style scoped>
.mask {
  position: fixed; inset: 0; z-index: 300;
  background: rgba(4, 3, 6, .62);
  display: flex; align-items: flex-end;
}
.sheet {
  width: 100%;
  max-height: 76vh;
  background: #121116;
  border-radius: 36rpx 36rpx 0 0;
  border-top: 1rpx solid rgba(255, 255, 255, .08);
  padding: 12rpx 0 calc(env(safe-area-inset-bottom) + 20rpx);
  display: flex; flex-direction: column;
  animation: up .24s ease-out;
}
@keyframes up { from { transform: translateY(100%); } to { transform: translateY(0); } }
.grab { width: 64rpx; height: 8rpx; border-radius: 8rpx; background: rgba(255, 255, 255, .16); margin: 6rpx auto 10rpx; }
.head { display: flex; align-items: center; gap: 10rpx; padding: 8rpx 36rpx 14rpx; }
.title { font-size: 30rpx; font-weight: 800; color: #f7f1e8; }
.count { font-size: 20rpx; color: rgba(247, 241, 232, .34); }
.sp { flex: 1; }
.clear { padding: 6rpx 14rpx; border-radius: 999rpx; border: 1rpx solid rgba(255, 255, 255, .1); }
.clear text { font-size: 18rpx; color: rgba(247, 241, 232, .45); }
.close {
  width: 52rpx; height: 52rpx; border-radius: 50%;
  background: rgba(255, 255, 255, .06);
  display: flex; align-items: center; justify-content: center;
}
.list { height: 52vh; padding: 0 28rpx; }
.qrow {
  display: flex; align-items: center; gap: 16rpx;
  height: 92rpx;
  padding: 0 10rpx;
  border-radius: 20rpx;
}
.qrow + .qrow { border-top: 1rpx solid rgba(255, 255, 255, .04); }
.qrow.on { background: rgba(245, 180, 91, .08); }
.no { width: 30rpx; text-align: center; font-size: 20rpx; color: rgba(247, 241, 232, .24); font-style: italic; }
.qrow.on .no { color: var(--km-accent); font-weight: 700; }
.cv { width: 60rpx; height: 60rpx; border-radius: 16rpx; background: #1a1c21; flex-shrink: 0; }
.meta { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4rpx; }
.n { font-size: 23rpx; color: rgba(247, 241, 232, .86); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.qrow.on .n { color: var(--km-accent); font-weight: 600; }
.a { font-size: 18rpx; color: rgba(247, 241, 232, .32); }
.eq { display: flex; align-items: flex-end; gap: 4rpx; height: 26rpx; margin-right: 8rpx; }
.b { width: 5rpx; border-radius: 3rpx; background: linear-gradient(180deg, var(--km-accent), #e08b3e); animation: bounce 1s infinite ease-in-out; }
.b1 { height: 12rpx; animation-delay: 0s; }
.b2 { height: 22rpx; animation-delay: .18s; }
.b3 { height: 16rpx; animation-delay: .34s; }
@keyframes bounce { 0%, 100% { transform: scaleY(.5); } 50% { transform: scaleY(1); } }
.tip { padding: 60rpx 0; text-align: center; font-size: 20rpx; color: rgba(247, 241, 232, .26); }
</style>


