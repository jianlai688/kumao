<script>
export default {
  onLaunch() {
    const token = uni.getStorageSync('km_token')
    if (token) {
      console.log('已登录')
    }
  }
}
</script>

<style>
/* ============================================================
   酷猫音乐 · APP 端设计系统（v2）
   深炭黑打底 + 琥珀金高光 + 玻璃层 + 细颗粒
   与 kumao-web 共用同一套后端与数据
   ============================================================ */
page {
  background-color: #08070a;
  color: #f7f1e8;
  font-family: -apple-system, 'PingFang SC', 'HarmonyOS Sans SC', 'Noto Sans SC', 'Microsoft YaHei', sans-serif;
  --km-accent: #f5b45b;
  --km-accent-deep: #e08b3e;
  --km-accent-soft: rgba(245, 180, 91, .13);
  --km-accent-line: rgba(245, 180, 91, .28);
  --km-grad: linear-gradient(140deg, #fbd08a 0%, #efa254 48%, #de823c 100%);
  --km-bg: #08070a;
  --km-card: #121116;
  --km-card-2: #17161d;
  --km-line: rgba(255, 255, 255, .075);
  --km-text: #f7f1e8;
  --km-dim: rgba(247, 241, 232, .58);
  --km-faint: rgba(247, 241, 232, .34);
  --km-ghost: rgba(247, 241, 232, .18);
  --km-rose: #ee7f93;
  --km-radius: 32rpx;
}

view, text, image, scroll-view, swiper, swiper-item {
  box-sizing: border-box;
}

/* ---------- 通用卡片 ---------- */
.km-card {
  background: var(--km-card);
  border: 1rpx solid var(--km-line);
  border-radius: var(--km-radius);
  padding: 24rpx;
  margin: 20rpx 24rpx;
}

.km-title {
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  letter-spacing: -0.5rpx;
  margin: 26rpx 24rpx 14rpx;
}

.km-muted { color: var(--km-dim); font-size: 24rpx; }
.km-faint { color: var(--km-faint); font-size: 22rpx; }

.km-row { display: flex; align-items: center; }

.km-cover {
  width: 100rpx;
  height: 100rpx;
  border-radius: 18rpx;
  background: #1a1c21;
}

/* ---------- 按钮 ---------- */
.km-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 68rpx;
  padding: 0 34rpx;
  border-radius: 999rpx;
  background: var(--km-grad);
  color: #2b1a08;
  font-size: 26rpx;
  font-weight: 800;
}

.km-btn.ghost {
  background: rgba(255, 255, 255, .06);
  border: 1rpx solid rgba(255, 255, 255, .12);
  color: var(--km-text);
}

/* ---------- 胶囊 ---------- */
.km-chip {
  display: inline-flex;
  align-items: center;
  height: 54rpx;
  padding: 0 24rpx;
  margin-right: 12rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(255, 255, 255, .1);
  background: rgba(255, 255, 255, .03);
  color: rgba(247, 241, 232, .58);
  font-size: 24rpx;
  white-space: nowrap;
}

.km-chip.active {
  background: var(--km-grad);
  border-color: transparent;
  color: #2b1a08;
  font-weight: 800;
}

/* ---------- 分区标题 ---------- */
.km-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-right: 24rpx;
}

.km-more { color: var(--km-faint); font-size: 23rpx; }

/* ---------- 页面骨架 ---------- */
.km-screen {
  min-height: 100vh;
  background:
    radial-gradient(120% 46% at 8% -8%, rgba(245, 180, 91, .16), transparent 62%),
    radial-gradient(90% 38% at 96% 6%, rgba(238, 127, 147, .10), transparent 66%),
    #08070a;
}

.km-h1 {
  font-size: 48rpx;
  font-weight: 900;
  letter-spacing: -1rpx;
  color: var(--km-text);
}

.km-sub {
  font-size: 22rpx;
  color: var(--km-faint);
  letter-spacing: .4rpx;
}

.km-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  padding: 110rpx 0;
  color: var(--km-faint);
  font-size: 24rpx;
}

.km-glass {
  background: rgba(255, 255, 255, .055);
  border: 1rpx solid rgba(255, 255, 255, .09);
  backdrop-filter: blur(20rpx);
}
</style>
