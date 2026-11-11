<template>
  <view class="km-screen list-page">
    <km-nav-bar title="音乐人">
      <view class="ibtn" @click="goApply">
        <km-icon name="plus" :size="18" color="rgba(247,241,232,.82)" />
      </view>
    </km-nav-bar>

    <view class="head">
      <text class="km-sub">入驻创作者，把作品放到这里</text>
    </view>

    <view class="rows">
      <view v-for="m in list" :key="m.id" class="row" @click="open(m)">
        <image class="ava" :src="avatarOf(m.avatar || m.avatarUrl)" mode="aspectFill" />
        <view class="meta">
          <view class="l1">
            <text class="name">{{ m.nickname || m.name || '音乐人' }}</text>
            <text v-if="m.verified" class="badge">认证</text>
          </view>
          <text class="intro">{{ m.intro || m.slogan || '这个人很神秘，还没有简介' }}</text>
        </view>
        <text class="fans">{{ formatCount(m.followerCount || m.fansCount) }}<text class="unit">粉丝</text></text>
      </view>
    </view>

    <view v-if="!list.length" class="km-empty">
      <km-icon name="mic" :size="34" color="rgba(247,241,232,.22)" />
      <text>{{ loading ? '正在加载音乐人…' : '还没有入驻的音乐人' }}</text>
    </view>

    <view class="apply" @click="goApply">
      <view class="apply-copy">
        <text class="apply-t">成为酷猫音乐人</text>
        <text class="apply-s">上传作品，获得专属主页与收益</text>
      </view>
      <km-icon name="chevron-right" :size="14" color="rgba(245,180,91,.9)" />
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import KmNavBar from '@/components/km-nav-bar.vue'
import KmIcon from '@/components/km-icon.vue'
import { api } from '@/api'
import { avatarOf, formatCount } from '@/utils/media'

const list = ref([])
const loading = ref(true)

function open(m) { uni.navigateTo({ url: `/pages/musician/detail?id=${m.id}` }) }
function goApply() {
  if (!uni.getStorageSync('km_token')) {
    uni.navigateTo({ url: '/pages/login/login' })
    return
  }
  uni.navigateTo({ url: '/pages/musician/apply' })
}

function statusText(s) {
  const map = { PENDING: '审核中', APPROVED: '已通过', REJECTED: '未通过', REVOKED: '已撤销' }
  return map[s] || s || '未知'
}

onMounted(async () => {
  const res = await api.musicianPage({ pageNum: 1, pageSize: 20 }).catch(() => null)
  list.value = (res && res.data && res.data.records) || []
  loading.value = false
})
</script>

<style scoped>
.list-page { padding-bottom: calc(var(--window-bottom, 0px) + 40rpx); }
.ibtn {
  width: 56rpx; height: 56rpx; border-radius: 50%;
  background: rgba(255, 255, 255, .055);
  border: 1rpx solid rgba(255, 255, 255, .07);
  display: flex; align-items: center; justify-content: center;
}
.head { padding: 6rpx 36rpx 0; display: flex; flex-direction: column; gap: 10rpx; }
.rows { padding: 22rpx 26rpx 0; }
.row {
  display: flex; align-items: center; gap: 20rpx;
  padding: 20rpx 14rpx;
  border-radius: 26rpx;
}
.ava { width: 96rpx; height: 96rpx; border-radius: 50%; background: #1a1c21; border: 1rpx solid rgba(255, 255, 255, .08); }
.meta { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8rpx; }
.l1 { display: flex; align-items: center; gap: 10rpx; }
.name { font-size: 26rpx; font-weight: 600; color: #f7f1e8; }
.badge {
  font-size: 15rpx; color: #f5b45b;
  border: 1rpx solid rgba(245, 180, 91, .3);
  border-radius: 5rpx; padding: 1rpx 6rpx;
}
.intro { font-size: 19rpx; color: rgba(247, 241, 232, .34); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.fans { font-size: 22rpx; color: rgba(247, 241, 232, .5); }
.unit { font-size: 16rpx; color: rgba(247, 241, 232, .28); margin-left: 4rpx; }
.apply {
  margin: 30rpx 32rpx 0;
  padding: 26rpx 24rpx;
  border-radius: 28rpx;
  background: linear-gradient(100deg, rgba(245, 180, 91, .14), rgba(245, 180, 91, .03));
  border: 1rpx solid rgba(245, 180, 91, .16);
  display: flex; align-items: center; gap: 16rpx;
  position: relative;
}
.apply-copy { flex: 1; display: flex; flex-direction: column; gap: 8rpx; }
.apply-t { font-size: 25rpx; font-weight: 700; color: rgba(247, 241, 232, .9); }
.apply-s { font-size: 18rpx; color: rgba(247, 241, 232, .36); }
</style>




