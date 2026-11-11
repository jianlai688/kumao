import request from './request'

export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
  smsCode: (phone) => request.get('/auth/sms-code', { params: { phone } }),
  logout: () => request.post('/auth/logout'),
  kickOthers: () => request.post('/auth/kick-others'),
  resetPassword: (data) => request.post('/auth/password/reset', data),
  changePassword: (data) => request.post('/auth/password/change', data)
}

export const userApi = {
  me: () => request.get('/users/me'),
  updateMe: (data) => request.put('/users/me', data),
  loginLogs: (params) => request.get('/users/me/login-logs', { params }),
  applyAnchor: (data) => request.post('/users/anchor/apply', data),
  myAnchorApply: () => request.get('/users/anchor/my'),
  adminPage: (params) => request.get('/users/admin/page', { params }),
  updateStatus: (id, status) => request.put(`/users/admin/${id}/status`, null, { params: { status } }),
  adminLoginLogs: (params) => request.get('/users/admin/login-logs', { params }),
  anchorApplies: (params) => request.get('/users/admin/anchor-applies', { params }),
  auditAnchor: (id, approved, reason) =>
    request.post(`/users/admin/anchor-applies/${id}/audit`, null, { params: { approved, reason } }),
  statistics: () => request.get('/users/admin/statistics'),
  grantRole: (id, role) => request.put(`/users/admin/${id}/roles`, null, { params: { role } })
}

export const musicApi = {
  songs: (params) => request.get('/music/songs', { params }),
  song: (id) => request.get(`/music/songs/${id}`),
  lyric: (id) => request.get(`/music/songs/${id}/lyric`),
  songMv: (id) => request.get(`/music/songs/${id}/mv`),
  regions: () => request.get('/music/songs/regions'),
  createSong: (data) => request.post('/music/songs/admin', data),
  updateSong: (id, data) => request.put(`/music/songs/admin/${id}`, data),
  updateSongStatus: (id, status) => request.put(`/music/songs/admin/${id}/status`, null, { params: { status } }),
  deleteSong: (id) => request.delete(`/music/songs/admin/${id}`),
  singers: (params) => request.get('/music/singers', { params }),
  singer: (id) => request.get(`/music/singers/${id}`),
  singerSongs: (id) => request.get(`/music/singers/${id}/songs`),
  mySinger: () => request.get('/music/singers/mine'),
  applySinger: (data) => request.post('/music/singers/apply', data),
  adminSingers: (params) => request.get('/music/singers/admin/page', { params }),
  auditSinger: (id, approved, reason) => request.post(`/music/singers/admin/${id}/audit`, null, { params: { approved, reason } }),
  createSinger: (data) => request.post('/music/singers/admin', data),
  updateSinger: (id, data) => request.put(`/music/singers/admin/${id}`, data),
  deleteSinger: (id) => request.delete(`/music/singers/admin/${id}`),
  hot: (limit = 20) => request.get('/music/rank/hot', { params: { limit } }),
  newSongs: (limit = 20) => request.get('/music/rank/new', { params: { limit } }),
  rising: (limit = 20) => request.get('/music/rank/rising', { params: { limit } }),
  mvs: (params) => request.get('/music/mv', { params }),
  playMv: (id) => request.get(`/music/mv/${id}/play`),
  myMvs: (params) => request.get('/music/mv/mine', { params }),
  publishMv: (data) => request.post('/music/mv/publish', data),
  updateMyMv: (id, data) => request.put(`/music/mv/mine/${id}`, data),
  setMyMvStatus: (id, status) => request.put(`/music/mv/mine/${id}/status`, null, { params: { status } }),
  deleteMyMv: (id) => request.delete(`/music/mv/mine/${id}`),
  adminMvs: (params) => request.get('/music/mv/admin/page', { params }),
  auditMv: (id, approved, reason) => request.post(`/music/mv/admin/${id}/audit`, null, { params: { approved, reason } }),
  createMv: (data) => request.post('/music/mv/admin', data),
  updateMv: (id, data) => request.put(`/music/mv/admin/${id}`, data),
  updateMvStatus: (id, status) => request.put(`/music/mv/admin/${id}/status`, null, { params: { status } }),
  deleteMv: (id) => request.delete(`/music/mv/admin/${id}`),
  banners: () => request.get('/music/banners')
}

export const playlistApi = {
  publicPage: (params) => request.get('/playlist/playlists/public', { params }),
  publicDetail: (id) => request.get(`/playlist/playlists/public/${id}`),
  mine: (params) => request.get('/playlist/playlists/mine', { params }),
  collected: (params) => request.get('/playlist/playlists/collected', { params }),
  detail: (id) => request.get(`/playlist/playlists/${id}`),
  create: (data) => request.post('/playlist/playlists', data),
  update: (id, data) => request.put(`/playlist/playlists/${id}`, data),
  remove: (id) => request.delete(`/playlist/playlists/${id}`),
  addSong: (id, songId) => request.post(`/playlist/playlists/${id}/songs/${songId}`),
  removeSong: (id, songId) => request.delete(`/playlist/playlists/${id}/songs/${songId}`),
  collect: (id) => request.post(`/playlist/playlists/${id}/collect`),
  cancelCollect: (id) => request.delete(`/playlist/playlists/${id}/collect`),
  adminPage: (params) => request.get('/playlist/playlists/admin/page', { params }),
  adminDelete: (id) => request.delete(`/playlist/playlists/admin/${id}`)
}

export const commentApi = {
  bySong: (songId, params) => request.get(`/comment/comments/song/${songId}`, { params }),
  replies: (id, params) => request.get(`/comment/comments/${id}/replies`, { params }),
  create: (data) => request.post('/comment/comments', data),
  remove: (id) => request.delete(`/comment/comments/${id}`),
  count: (songId) => request.get(`/comment/comments/song/${songId}/count`),
  adminPage: (params) => request.get('/comment/comments/admin/page', { params }),
  adminStatus: (id, status) => request.put(`/comment/comments/admin/${id}/status`, null, { params: { status } }),
  adminDelete: (id) => request.delete(`/comment/comments/admin/${id}`)
}

export const likeApi = {
  like: (songId) => request.post(`/comment/likes/${songId}`),
  unlike: (songId) => request.delete(`/comment/likes/${songId}`),
  mine: (params) => request.get('/comment/likes/mine', { params }),
  status: (songId) => request.get(`/comment/likes/${songId}/status`)
}

export const recordApi = {
  submit: (data) => request.post('/comment/play-records', data),
  mine: (params) => request.get('/comment/play-records/mine', { params }),
  recent: (limit = 20) => request.get('/comment/play-records/recent', { params: { limit } }),
  clear: () => request.delete('/comment/play-records/mine')
}

export const fileApi = {
  upload: (file, bizType = 'common', options = {}) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/file/files/upload', form, {
      params: { bizType },
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: options.onUploadProgress,
      signal: options.signal
    })
  },
  remove: (id) => request.delete(`/file/files/${id}`),
  adminPage: (params) => request.get('/file/files/admin/page', { params })
}

export const liveApi = {
  square: (params) => request.get('/live/live/rooms/square', { params }),
  categories: () => request.get('/live/live/rooms/categories'),
  scenes: () => request.get('/live/live/rooms/scenes'),
  room: (id) => request.get(`/live/live/rooms/${id}`),
  online: (id) => request.get(`/live/live/rooms/${id}/online`),
  create: (data) => request.post('/live/live/rooms', data),
  update: (id, data) => request.put(`/live/live/rooms/${id}`, data),
  pushUrl: (id) => request.get(`/live/live/rooms/${id}/push-url`),
  start: (id) => request.post(`/live/live/rooms/${id}/start`),
  like: (id) => request.post(`/live/live/rooms/${id}/like`),
  stop: (id) => request.post(`/live/live/rooms/${id}/stop`),
  remove: (id) => request.delete(`/live/live/rooms/${id}`),
  mine: (params) => request.get('/live/live/rooms/mine', { params }),
  adminPage: (params) => request.get('/live/live/rooms/admin/page', { params }),
  ban: (id, banned, reason) => request.post(`/live/live/rooms/admin/${id}/ban`, null, { params: { banned, reason } }),
  statistics: () => request.get('/live/live/rooms/admin/statistics'),
  danmakuHistory: (roomId, params) => request.get(`/live/live/danmaku/history/${roomId}`, { params }),
  adminDanmaku: (params) => request.get('/live/live/danmaku/admin/page', { params }),
  deleteDanmaku: (id) => request.delete(`/live/live/danmaku/admin/${id}`),
  gifts: () => request.get('/live/live/gifts'),
  balance: () => request.get('/live/live/gifts/balance'),
  sendGift: (data) => request.post('/live/live/gifts/send', data),
  received: (params) => request.get('/live/live/gifts/received', { params }),
  giftSummary: () => request.get('/live/live/gifts/received/summary'),
  adminGifts: (params) => request.get('/live/live/gifts/admin/page', { params }),
  createGift: (data) => request.post('/live/live/gifts/admin', data),
  updateGift: (id, data) => request.put(`/live/live/gifts/admin/${id}`, data),
  deleteGift: (id) => request.delete(`/live/live/gifts/admin/${id}`)
}

export const searchApi = {
  search: (params) => request.get('/search/search', { params }),
  hotKeywords: () => request.get('/search/search/hot-keywords'),
  monitor: () => request.get('/search/search/admin/monitor'),
  sync: (docs) => request.post('/search/search/admin/sync', { docs })
}

export const vipApi = {
  packages: () => request.get('/vip/vip/packages'),
  status: () => request.get('/vip/vip/status'),
  claimAdReward: (planId, watchedSeconds = 15) => request.post('/vip/vip/reward/ad', { planId, watchedSeconds }),
  open: (data) => request.post('/vip/vip/open', data),
  orders: (params) => request.get('/vip/vip/orders', { params }),
  permission: (type) => request.get('/vip/vip/permission', { params: { type } }),
  payChannels: () => request.get('/vip/vip/pay/channels'),
  coinPackages: () => request.get('/vip/vip/pay/coin/packages'),
  createPayment: (data) => request.post('/vip/vip/pay/create', data),
  createAdPayment: (planId, payType) => request.post('/vip/vip/pay/ad/create', { planId, payType }),
  payStatus: (orderNo) => request.get(`/vip/vip/pay/status/${orderNo}`),
  mockPay: (orderNo) => request.post(`/vip/vip/pay/mock/${orderNo}/success`),
  members: (params) => request.get('/vip/vip/admin/members', { params }),
  adminOrders: (params) => request.get('/vip/vip/admin/orders', { params }),
  statistics: () => request.get('/vip/vip/admin/statistics'),
  createPackage: (data) => request.post('/vip/vip/admin/packages', data),
  updatePackage: (id, data) => request.put(`/vip/vip/admin/packages/${id}`, data),
  deletePackage: (id) => request.delete(`/vip/vip/admin/packages/${id}`)
}

export const musicianApi = {
  apply: (data) => request.post('/musician/musician/apply', data),
  mine: () => request.get('/musician/musician/mine'),
  page: (params) => request.get('/musician/musician/page', { params }),
  homepage: (id) => request.get(`/musician/musician/${id}/homepage`),
  follow: (id) => request.post(`/musician/musician/${id}/follow`),
  unfollow: (id) => request.delete(`/musician/musician/${id}/follow`),
  works: (params) => request.get('/musician/musician/works', { params }),
  createWork: (data) => request.post('/musician/musician/works', data),
  updateWork: (id, data) => request.put(`/musician/musician/works/${id}`, data),
  offlineWork: (id) => request.post(`/musician/musician/works/${id}/offline`),
  onlineWork: (id) => request.post(`/musician/musician/works/${id}/online`),
  deleteWork: (id) => request.delete(`/musician/musician/works/${id}`),
  albums: () => request.get('/musician/musician/albums'),
  setAlbumStatus: (id, status) => request.post(`/musician/musician/albums/${id}/status`, null, { params: { status } }),
  createAlbum: (data) => request.post('/musician/musician/albums', data),
  updateAlbum: (id, data) => request.put(`/musician/musician/albums/${id}`, data),
  deleteAlbum: (id) => request.delete(`/musician/musician/albums/${id}`),
  statistics: (period) => request.get('/musician/musician/statistics', { params: { period } }),
  trend: (period) => request.get('/musician/musician/statistics/trend', { params: { period } }),
  rank: () => request.get('/musician/musician/statistics/rank'),
  fansRegions: () => request.get('/musician/musician/statistics/region'),
  incomeSummary: () => request.get('/musician/musician/income/summary'),
  incomes: (params) => request.get('/musician/musician/income', { params }),
  settle: () => request.post('/musician/musician/income/settle'),
  withdraws: (params) => request.get('/musician/musician/withdraws', { params }),
  withdraw: (amount, account, payType) => request.post('/musician/musician/withdraws', null, { params: { amount, account, payType } }),
  adminPage: (params) => request.get('/musician/musician/admin/page', { params }),
  audit: (id, approved, reason) => request.post(`/musician/musician/admin/${id}/audit`, null, { params: { approved, reason } }),
  revoke: (id, reason) => request.post(`/musician/musician/admin/${id}/revoke`, null, { params: { reason } }),
  adminWorks: (params) => request.get('/musician/musician/admin/works', { params }),
  auditWork: (id, approved, reason) => request.post(`/musician/musician/admin/works/${id}/audit`, null, { params: { approved, reason } }),
  adminIncome: () => request.get('/musician/musician/admin/income/summary'),
  adminWithdraws: (params) => request.get('/musician/musician/admin/withdraws', { params }),
  auditWithdraw: (id, approved, remark) => request.post(`/musician/musician/admin/withdraws/${id}/audit`, null, { params: { approved, remark } })
}

export const adApi = {
  active: (slot) => request.get('/ad/ad/active', { params: { slot } }),
  report: (planId, action, region, hour) => request.post(`/ad/ad/report/${planId}`, null, { params: { action, region, hour } }),
  apply: (data) => request.post('/ad/ad/apply', data),
  mine: () => request.get('/ad/ad/mine'),
  materials: (params) => request.get('/ad/ad/materials', { params }),
  createMaterial: (data) => request.post('/ad/ad/materials', data),
  updateMaterial: (id, data) => request.put(`/ad/ad/materials/${id}`, data),
  offlineMaterial: (id) => request.post(`/ad/ad/materials/${id}/offline`),
  deleteMaterial: (id) => request.delete(`/ad/ad/materials/${id}`),
  pricing: () => request.get('/ad/ad/pricing'),
  plans: (params) => request.get('/ad/ad/plans', { params }),
  createPlan: (data) => request.post('/ad/ad/plans', data),
  planAction: (id, action) => request.post(`/ad/ad/plans/${id}/${action}`),
  statistics: () => request.get('/ad/ad/statistics'),
  rank: () => request.get('/ad/ad/statistics/rank'),
  region: () => request.get('/ad/ad/statistics/region'),
  hourly: () => request.get('/ad/ad/statistics/hourly'),
  adminPage: (params) => request.get('/ad/ad/admin/page', { params }),
  audit: (id, approved, reason) => request.post(`/ad/ad/admin/${id}/audit`, null, { params: { approved, reason } }),
  revoke: (id, reason) => request.post(`/ad/ad/admin/${id}/revoke`, null, { params: { reason } }),
  adminMaterials: (params) => request.get('/ad/ad/admin/materials', { params }),
  auditMaterial: (id, approved, reason) => request.post(`/ad/ad/admin/materials/${id}/audit`, null, { params: { approved, reason } }),
  adminPlans: (params) => request.get('/ad/ad/admin/plans', { params }),
  auditPlan: (id, approved, reason) => request.post(`/ad/ad/admin/plans/${id}/audit`, null, { params: { approved, reason } }),
  adminRevenue: () => request.get('/ad/ad/admin/revenue/summary')
}

export const reviewApi = {
  status: () => request.get('/admin/ai/status', { silent: true }),
  models: () => request.get('/admin/ai/models', { silent: true }),
  saveModel: (type, data) => request.put(`/admin/ai/models/${type}`, data, { silent: true }),
  testModel: (type, data) => request.post(`/admin/ai/models/${type}/test`, data, { silent: true }),
  listModels: (type, data) => request.post(`/admin/ai/models/${type}/models`, data, { silent: true }),
  autoStatus: () => request.get('/admin/ai/auto/status', { silent: true }),
  autoTasks: (params) => request.get('/admin/ai/auto/tasks', { params, silent: true }),
  runAuto: () => request.post('/admin/ai/auto/run'),
  resolveAutoTask: (category, itemId) => request.delete(`/admin/ai/auto/tasks/${category}/${itemId}`, { silent: true }),
  analyze: (data) => request.post('/admin/ai/review', data, { silent: true }),
  log: (data) => request.post('/admin/ai/log', data),
  logs: (params) => request.get('/admin/ai/logs', { params }),
  stats: () => request.get('/admin/ai/stats')
}

