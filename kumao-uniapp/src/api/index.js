import { getApiBase } from '@/utils/server'

export { getApiBase } from '@/utils/server'

export function request(options) {
  // 每次请求都重新取基址：设置页修改服务器地址后无需重启 App。
  const BASE_URL = getApiBase()
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('km_token')
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'X-Device-Type': 'APP',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      success: (res) => {
        const body = res.data
        if (body && body.code === 200) {
          resolve(body)
        } else if (body && body.code === 401) {
          if (!options.silent) {
            uni.removeStorageSync('km_token')
            uni.navigateTo({ url: '/pages/login/login' })
          }
          reject(body)
        } else {
          if (!options.silent) uni.showToast({ title: (body && body.message) || '请求失败', icon: 'none' })
          reject(body)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}

export const api = {
  login: (data) => request({ url: '/auth/login', method: 'POST', data }),
  register: (data) => request({ url: '/auth/register', method: 'POST', data }),
  smsCode: (phone) => request({ url: `/auth/sms-code?phone=${phone}` }),
  me: () => request({ url: '/users/me' }),
  updateMe: (data) => request({ url: '/users/me', method: 'PUT', data }),
  cancelAccount: () => request({ url: '/auth/cancel', method: 'POST' }),
  banners: () => request({ url: '/music/banners' }),
  homeSettings: () => request({ url: '/music/home/settings', silent: true }),
  hot: (limit = 20) => request({ url: `/music/rank/hot?limit=${limit}` }),
  newSongs: (limit = 20) => request({ url: `/music/rank/new?limit=${limit}` }),
  rising: (limit = 20) => request({ url: `/music/rank/rising?limit=${limit}` }),
  songs: (params) => request({ url: `/music/songs?${toQuery(params)}` }),
  song: (id) => request({ url: `/music/songs/${id}` }),
  lyric: (id) => request({ url: `/music/songs/${id}/lyric` }),
  singers: (params) => request({ url: `/music/singers?${toQuery(params)}` }),
  playlists: (params) => request({ url: `/playlist/playlists/public?${toQuery(params)}` }),
  playlist: (id) => request({ url: `/playlist/playlists/${id}` }),
  likes: (params) => request({ url: `/comment/likes/mine?${toQuery(params)}` }),
  like: (songId) => request({ url: `/comment/likes/${songId}`, method: 'POST' }),
  unlike: (songId) => request({ url: `/comment/likes/${songId}`, method: 'DELETE' }),
  playRecord: (data) => request({ url: '/comment/play-records', method: 'POST', data, silent: true }),
  liveSquare: (params) => request({ url: `/live/live/rooms/square?${toQuery(params)}` }),
  liveRoom: (id) => request({ url: `/live/live/rooms/${id}` }),
  liveMine: (params) => request({ url: `/live/live/rooms/mine?${toQuery(params)}` }),
  livePushUrl: (id) => request({ url: `/live/live/rooms/${id}/push-url` }),
  liveStart: (id) => request({ url: `/live/live/rooms/${id}/start`, method: 'POST' }),
  liveStop: (id) => request({ url: `/live/live/rooms/${id}/stop`, method: 'POST' }),
  likeLive: (id) => request({ url: `/live/live/rooms/${id}/like`, method: 'POST' }),
  danmakuHistory: (id, params) => request({ url: `/live/live/danmaku/history/${id}?${toQuery(params)}` }),
  gifts: () => request({ url: '/live/live/gifts' }),
  sendGift: (data) => request({ url: '/live/live/gifts/send', method: 'POST', data }),
  search: (keyword) => request({ url: `/search/search?keyword=${encodeURIComponent(keyword)}` }),
  vipPackages: () => request({ url: '/vip/vip/packages' }),
  vipStatus: () => request({ url: '/vip/vip/status' }),
  openVip: (data) => request({ url: '/vip/vip/open', method: 'POST', data }),
  musicianPage: (params) => request({ url: `/musician/musician/page?${toQuery(params)}` }),

  /* ---- 榜单 / 歌手 / MV（与 kumao-web 对齐） ---- */
  singer: (id) => request({ url: `/music/singers/${id}` }),
  singerSongs: (id) => request({ url: `/music/singers/${id}/songs` }),
  mvs: (params) => request({ url: `/music/mv?${toQuery(params)}` }),
  mv: (id) => request({ url: `/music/mv/${id}` }),
  playMv: (id) => request({ url: `/music/mv/${id}/play` }),

  /* ---- 互动 ---- */
  likeStatus: (songId) => request({ url: `/comment/likes/${songId}/status`, silent: true }),
  songPurchaseStatus: (songId) => request({ url: `/music/songs/${songId}/purchase-status`, silent: true }),  purchaseSong: (songId) => request({ url: `/music/songs/${songId}/purchase`, method: 'POST' }),
  commentCount: (songId) => request({ url: `/comment/comments/song/${songId}/count`, silent: true }),
  comments: (songId, params) => request({ url: `/comment/comments/song/${songId}?${toQuery(params)}`, silent: true }),
  createComment: (data) => request({ url: '/comment/comments', method: 'POST', data }),
  recentPlays: (limit = 20) => request({ url: `/comment/play-records/recent?limit=${limit}` }),

  /* ---- 直播 ---- */
  liveCategories: () => request({ url: '/live/live/rooms/categories' }),

  /* ---- 我的音乐 ---- */
  playlistsMine: (params) => request({ url: `/playlist/playlists/mine?${toQuery(params)}` }),
  playlistsCollected: (params) => request({ url: `/playlist/playlists/collected?${toQuery(params)}` }),
  createPlaylist: (data) => request({ url: '/playlist/playlists', method: 'POST', data }),
  publishPlaylist: (id, isPublic) => request({ url: `/playlist/playlists/${id}/publish?publicFlag=${isPublic}`, method: 'PUT' }),
  addSongToPlaylist: (id, songId) => request({ url: `/playlist/playlists/${id}/songs/${songId}`, method: 'POST' }),
  removeSongFromPlaylist: (id, songId) => request({ url: `/playlist/playlists/${id}/songs/${songId}`, method: 'DELETE' }),
  collectPlaylist: (id) => request({ url: `/playlist/playlists/${id}/collect`, method: 'POST' }),
  cancelCollectPlaylist: (id) => request({ url: `/playlist/playlists/${id}/collect`, method: 'DELETE' }),

  /* ---- 广告 / 激励会员 ---- */
  adsActive: (slot) => request({ url: `/ad/ad/active${slot ? '?slot=' + encodeURIComponent(slot) : ''}` }),
  adReport: (planId, action) => request({ url: `/ad/ad/report/${planId}?action=${encodeURIComponent(action)}`, method: 'POST', silent: true }),
  claimAdReward: (data) => request({ url: '/vip/vip/reward/ad', method: 'POST', data }),

  /* ---- 角色个人资料 ---- */
  anchorMy: () => request({ url: '/users/anchor/my' }),
  updateAnchorProfile: (data) => request({ url: '/users/anchor/profile', method: 'PUT', data }),
  updateMusicianProfile: (data) => request({ url: '/musician/musician/mine', method: 'PUT', data }),
  advertiserMine: () => request({ url: '/ad/ad/mine', silent: true }),
  updateAdvertiserProfile: (data) => request({ url: '/ad/ad/mine', method: 'PUT', data }),
  /* ---- 音乐人作品管理 ---- */
  musicianAlbums: () => request({ url: '/musician/musician/albums' }),
  createMusicianAlbum: (data) => request({ url: '/musician/musician/albums', method: 'POST', data }),
  updateMusicianAlbum: (id, data) => request({ url: `/musician/musician/albums/${id}`, method: 'PUT', data }),
  updateMusicianAlbumStatus: (id, status) => request({ url: `/musician/musician/albums/${id}/status?status=${status}`, method: 'POST' }),
  updateMusicianWork: (id, data) => request({ url: `/musician/musician/works/${id}`, method: 'PUT', data }),
  offlineMusicianWork: (id) => request({ url: `/musician/musician/works/${id}/offline`, method: 'POST' }),
  onlineMusicianWork: (id) => request({ url: `/musician/musician/works/${id}/online`, method: 'POST' }),
  deleteMusicianWork: (id) => request({ url: `/musician/musician/works/${id}`, method: 'DELETE' }),
  mvsMine: (params) => request({ url: `/music/mv/mine?${toQuery(params)}` }),
  updateMvMine: (id, data) => request({ url: `/music/mv/mine/${id}`, method: 'PUT', data }),
  updateMvMineStatus: (id, status) => request({ url: `/music/mv/mine/${id}/status?status=${status}`, method: 'PUT' }),
  deleteMvMine: (id) => request({ url: `/music/mv/mine/${id}`, method: 'DELETE' }),
  /* ---- 音乐人 ---- */
  musicianHomepage: (id) => request({ url: `/musician/musician/${id}/homepage` }),
  musicianFollow: (id) => request({ url: `/musician/musician/${id}/follow`, method: 'POST' }),
  musicianUnfollow: (id) => request({ url: `/musician/musician/${id}/follow`, method: 'DELETE' }),
  musicianMine: () => request({ url: '/musician/musician/mine', silent: true }),
  musicianApply: (data) => request({ url: '/musician/musician/apply', method: 'POST', data }),
  musicianWorks: (params) => request({ url: `/musician/musician/works?${toQuery(params)}` })
}

function toQuery(params = {}) {
  return Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${k}=${encodeURIComponent(v)}`)
    .join('&')
}









