<template>
  <view class="km-screen profile">
    <km-nav-bar title="个人信息">
      <view class="ibtn" @click="editing = !editing">
        <text class="ibtn-t">{{ editing ? '取消' : '编辑' }}</text>
      </view>
    </km-nav-bar>

    <!-- 头部 -->
    <view class="hero">
      <view class="ava-wrap" @click="pickAvatar">
        <image class="ava" :src="avatarOf(form.avatar)" mode="aspectFill" />
        <view v-if="editing" class="ava-mask"><text>更换头像</text></view>
      </view>
      <view class="hero-copy">
        <text class="nick">{{ form.nickname || '未设置昵称' }}</text>
        <text class="acc">@{{ user.username || '-' }}</text>
        <view class="badges">
          <text v-if="isVip" class="badge gold">VIP</text>
          <text v-if="hasRole('MUSICIAN')" class="badge">音乐人</text>
          <text v-if="hasRole('ANCHOR')" class="badge">主播</text>
          <text v-if="hasRole('ADVERTISER')" class="badge">广告主</text>
        </view>
      </view>
    </view>

    <!-- 详细资料 -->
    <view class="card">
      <view class="row">
        <text class="k">用户 ID</text>
        <text class="v mono">{{ user.id || user.userId || '-' }}</text>
      </view>
      <view class="row">
        <text class="k">手机号</text>
        <text class="v">{{ user.phone || '未绑定' }}</text>
      </view>
      <view class="row">
        <text class="k">注册时间</text>
        <text class="v">{{ (user.createTime || '').slice(0, 10) || '-' }}</text>
      </view>
    </view>

    <view class="card">
      <view class="row">
        <text class="k">昵称</text>
        <input v-if="editing" v-model="form.nickname" class="input" maxlength="20" placeholder="请输入昵称" />
        <text v-else class="v">{{ form.nickname || '未设置' }}</text>
      </view>

      <picker v-if="editing" mode="selector" :range="genderLabels" :value="form.gender" @change="onGender">
        <view class="row">
          <text class="k">性别</text>
          <text class="v">{{ genderLabels[form.gender] }} ›</text>
        </view>
      </picker>
      <view v-else class="row">
        <text class="k">性别</text>
        <text class="v">{{ genderLabels[form.gender] }}</text>
      </view>

      <picker v-if="editing" mode="date" :value="form.birthday || '2000-01-01'" start="1940-01-01" end="2020-12-31" @change="onBirthday">
        <view class="row">
          <text class="k">生日</text>
          <text class="v">{{ form.birthday || '未设置' }} ›</text>
        </view>
      </picker>
      <view v-else class="row">
        <text class="k">生日</text>
        <text class="v">{{ form.birthday || '未设置' }}</text>
      </view>

      <view class="row">
        <text class="k">地区</text>
        <input v-if="editing" v-model="form.region" class="input" maxlength="30" placeholder="如 广东 深圳" />
        <text v-else class="v">{{ form.region || '未设置' }}</text>
      </view>

      <view class="row">
        <text class="k">邮箱</text>
        <input v-if="editing" v-model="form.email" class="input" maxlength="60" placeholder="name@example.com" />
        <text v-else class="v">{{ form.email || '未设置' }}</text>
      </view>
    </view>

    <view class="card">
      <view class="row col">
        <text class="k">个性签名</text>
        <textarea
          v-if="editing"
          v-model="form.intro"
          class="textarea"
          maxlength="120"
          placeholder="介绍一下自己吧"
        />
        <text v-else class="intro">{{ form.intro || '这个人很神秘，什么都没写' }}</text>
      </view>
    </view>

    <view v-if="editing" class="save-bar">
      <view class="save" @click="save">{{ saving ? '保存中…' : '保存资料' }}</view>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import KmNavBar from '@/components/km-nav-bar.vue'
import { api } from '@/api'
import { getApiBase } from '@/utils/server'
import { avatarOf } from '@/utils/media'

const user = ref(uni.getStorageSync('km_user') || {})
const editing = ref(false)
const saving = ref(false)
const isVip = ref(false)

const genderLabels = ['保密', '男', '女']

const form = reactive({
  nickname: '',
  avatar: '',
  gender: 0,
  birthday: '',
  region: '',
  email: '',
  intro: ''
})

const roles = computed(() => String(user.value.roles || '').split(',').filter(Boolean))
function hasRole(role) {
  return roles.value.includes(role)
}

function fill(u) {
  form.nickname = u.nickname || ''
  form.avatar = u.avatar || ''
  form.gender = Number(u.gender || 0)
  form.birthday = u.birthday || ''
  form.region = u.region || ''
  form.email = u.email || ''
  form.intro = u.intro || ''
}

async function load() {
  const res = await api.me().catch(() => null)
  if (res && res.data) {
    user.value = { ...user.value, ...res.data }
    fill(user.value)
    uni.setStorageSync('km_user', user.value)
  } else {
    fill(user.value)
  }
  api.vipStatus().then((r) => { isVip.value = !!(r && r.data && r.data.vip) }).catch(() => {})
}

function onGender(e) {
  form.gender = Number(e.detail.value) || 0
}
function onBirthday(e) {
  form.birthday = e.detail.value
}

function pickAvatar() {
  if (!editing.value) return
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: async (r) => {
      const path = r.tempFilePaths && r.tempFilePaths[0]
      if (!path) return
      uni.showLoading({ title: '上传中…', mask: true })
      try {
        const token = uni.getStorageSync('km_token')
        const res = await new Promise((resolve, reject) => {
          uni.uploadFile({
            url: `${getApiBase()}/file/upload?bizType=avatar`,
            filePath: path,
            name: 'file',
            header: { Authorization: `Bearer ${token}` },
            success: (resp) => {
              try { resolve(JSON.parse(resp.data)) } catch (e) { reject(new Error('返回解析失败')) }
            },
            fail: () => reject(new Error('上传失败'))
          })
        })
        if (res && res.code === 200 && res.data) {
          form.avatar = res.data.url || res.data.path || res.data.fileUrl || ''
          uni.showToast({ title: '头像已上传', icon: 'none' })
        } else {
          uni.showToast({ title: (res && res.message) || '上传失败', icon: 'none' })
        }
      } catch (e) {
        uni.showToast({ title: e.message || '上传失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    }
  })
}

async function save() {
  if (!form.nickname || !form.nickname.trim()) {
    uni.showToast({ title: '昵称不能为空', icon: 'none' })
    return
  }
  if (form.email && !/^[\w.+-]+@[\w-]+(\.[\w-]+)+$/.test(form.email)) {
    uni.showToast({ title: '邮箱格式不正确', icon: 'none' })
    return
  }
  saving.value = true
  try {
    await api.updateMe({
      nickname: form.nickname.trim(),
      avatar: form.avatar,
      gender: form.gender,
      birthday: form.birthday,
      region: form.region,
      email: form.email,
      intro: form.intro
    })
    const res = await api.me().catch(() => null)
    if (res && res.data) {
      user.value = { ...user.value, ...res.data }
      uni.setStorageSync('km_user', user.value)
      fill(user.value)
    }
    editing.value = false
    uni.showToast({ title: '已保存', icon: 'success' })
  } catch (e) {
    /* 统一提示 */
  } finally {
    saving.value = false
  }
}

onLoad(load)
</script>

<style scoped>
.profile { padding-bottom: calc(var(--window-bottom, 0px) + 160rpx); }
.ibtn { padding: 0 22rpx; height: 56rpx; border-radius: 999rpx; background: rgba(255,255,255,.06); display: flex; align-items: center; }
.ibtn-t { font-size: 22rpx; color: var(--km-accent); font-weight: 700; }

.hero { display: flex; align-items: center; gap: 22rpx; padding: 26rpx 26rpx 20rpx; }
.ava-wrap { position: relative; width: 132rpx; height: 132rpx; border-radius: 50%; overflow: hidden; flex: none; }
.ava { width: 100%; height: 100%; background: #1b1a21; }
.ava-mask {
  position: absolute; inset: 0; background: rgba(0,0,0,.55);
  display: flex; align-items: center; justify-content: center;
  font-size: 18rpx; color: #fff;
}
.hero-copy { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6rpx; }
.nick { font-size: 34rpx; font-weight: 800; color: #f7f1e8; }
.acc { font-size: 21rpx; color: rgba(247,241,232,.4); }
.badges { display: flex; gap: 8rpx; margin-top: 4rpx; flex-wrap: wrap; }
.badge { font-size: 17rpx; padding: 2rpx 10rpx; border-radius: 5rpx; background: rgba(255,255,255,.1); color: rgba(247,241,232,.7); }
.badge.gold { background: rgba(245,180,91,.18); color: #f5cf9a; }

.card { margin: 0 24rpx 18rpx; padding: 6rpx 20rpx; border-radius: 18rpx; background: rgba(255,255,255,.035); border: 1rpx solid rgba(255,255,255,.05); }
.row { display: flex; align-items: center; gap: 16rpx; min-height: 92rpx; border-bottom: 1rpx solid rgba(255,255,255,.05); }
.row:last-child { border-bottom: 0; }
.row.col { flex-direction: column; align-items: stretch; justify-content: center; padding: 18rpx 0; gap: 10rpx; }
.k { width: 150rpx; flex: none; font-size: 23rpx; color: rgba(247,241,232,.45); }
.row.col .k { width: auto; }
.v { flex: 1; font-size: 24rpx; color: #f3f4f6; text-align: right; }
.mono { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 21rpx; }
.input { flex: 1; text-align: right; font-size: 24rpx; color: #f3f4f6; }
.textarea {
  width: 100%; min-height: 130rpx; background: rgba(255,255,255,.05);
  border-radius: 12rpx; padding: 14rpx 16rpx; box-sizing: border-box;
  font-size: 23rpx; color: #f3f4f6;
}
.intro { font-size: 23rpx; color: rgba(247,241,232,.72); line-height: 1.6; }

.save-bar { position: fixed; left: 0; right: 0; bottom: 0; padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom)); background: rgba(10,9,13,.9); backdrop-filter: blur(10px); }
.save {
  height: 88rpx; line-height: 88rpx; text-align: center; border-radius: 16rpx;
  background: linear-gradient(135deg,#fbd08a,#de823c); color: #22150a; font-weight: 800; font-size: 25rpx;
}
</style>

