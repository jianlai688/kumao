/** 广告位编码 -> 中文名（与后端 AdService.SLOT_NAMES 保持一致） */
export const AD_SLOT_LABELS = {
  SPLASH: '开屏广告',
  HOME_BANNER: '首页 Banner',
  LIVE: '直播页',
  PLAYER: '播放页',
  VIP_REWARD: 'VIP 激励视频'
}

export function adSlotLabel(slot) {
  if (!slot) return '未知版位'
  return AD_SLOT_LABELS[slot] || slot
}
