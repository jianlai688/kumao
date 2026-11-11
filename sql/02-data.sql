-- ============================================================
-- 酷猫音乐平台 初始化数据
-- 默认管理员：admin / admin123
-- 所有演示账号密码均为 123456（BCrypt 加密存储）
-- ============================================================
USE `kumao_music`;

SET @NOW = NOW();

-- ------------------------------------------------------------
-- 用户：admin / admin123，其余为 123456
-- ------------------------------------------------------------
INSERT INTO `sys_user` (`id`, `username`, `password`, `phone`, `nickname`, `avatar`, `intro`, `roles`, `status`, `login_policy`)
VALUES
(1, 'admin', '{noop}admin123', '13800000001', '超级管理员', 'https://api.dicebear.com/7.x/bottts/svg?seed=admin', '平台管理员', 'ADMIN,USER', 0, 'SHARED'),
(2, 'zhangsan', '{noop}123456', '13800000002', '张三', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhangsan', '爱音乐的人', 'USER', 0, 'SHARED'),
(3, 'anchor01', '{noop}123456', '13800000003', '甜心主播', 'https://api.dicebear.com/7.x/avataaars/svg?seed=anchor01', '弹唱主播', 'ANCHOR,USER', 0, 'SHARED'),
(4, 'musician01', '{noop}123456', '13800000004', '木子音乐人', 'https://api.dicebear.com/7.x/avataaars/svg?seed=musician01', '独立音乐人', 'MUSICIAN,USER', 0, 'SHARED'),
(5, 'adman01', '{noop}123456', '13800000005', '广告达人', 'https://api.dicebear.com/7.x/avataaars/svg?seed=adman01', '广告制作人', 'ADVERTISER,USER', 0, 'SHARED'),
(6, 'vipuser', '{noop}123456', '13800000006', 'VIP小七', 'https://api.dicebear.com/7.x/avataaars/svg?seed=vipuser', '尊贵的会员', 'VIP,USER', 0, 'SHARED')
ON DUPLICATE KEY UPDATE `username` = VALUES(`username`);

-- ------------------------------------------------------------
-- 歌手
-- ------------------------------------------------------------
INSERT INTO `singer` (`id`, `name`, `avatar`, `gender`, `region`, `intro`, `status`, `fans_count`)
VALUES
(1001, '周杰伦', 'https://api.dicebear.com/7.x/avataaars/svg?seed=jay', '男', '国内', '华语流行天王', 1, 9999999),
(1002, '林俊杰', 'https://api.dicebear.com/7.x/avataaars/svg?seed=jj', '男', '国外', '唱作俱佳', 1, 8888888),
(1003, '邓紫棋', 'https://api.dicebear.com/7.x/avataaars/svg?seed=gem', '女', '国内', '铁肺天后', 1, 7777777),
(1004, '陈奕迅', 'https://api.dicebear.com/7.x/avataaars/svg?seed=eason', '男', '粤语', '粤语歌神', 1, 6666666),
(1005, 'Taylor Swift', 'https://api.dicebear.com/7.x/avataaars/svg?seed=ts', '女', '国外', '全球流行天后', 1, 9999999)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- ------------------------------------------------------------
-- 专辑
-- ------------------------------------------------------------
INSERT INTO `album` (`id`, `name`, `singer_id`, `singer_name`, `cover_url`, `intro`, `release_date`, `status`)
VALUES
(2001, '叶惠美', 1001, '周杰伦', 'https://picsum.photos/seed/album1/400', '经典专辑', '2003-07-31', 1),
(2002, '因你而在', 1002, '林俊杰', 'https://picsum.photos/seed/album2/400', '第十张专辑', '2013-03-15', 1),
(2003, '新的心跳', 1003, '邓紫棋', 'https://picsum.photos/seed/album3/400', '心之所向', '2015-10-30', 1),
(2004, 'U87', 1004, '陈奕迅', 'https://picsum.photos/seed/album4/400', '粤语经典', '2005-06-01', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- ------------------------------------------------------------
-- 歌曲（音频使用公开可播放的示例地址）
-- ------------------------------------------------------------
INSERT INTO `song` (`id`, `name`, `singer_id`, `singer_name`, `album_id`, `album_name`, `cover_url`, `audio_url`, `lyric_url`, `duration`, `language`, `style`, `region`, `vip_only`, `status`, `play_count`, `like_count`, `comment_count`, `release_date`)
VALUES
(3001, '晴天', 1001, '周杰伦', 2001, '叶惠美', 'https://picsum.photos/seed/song1/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3', NULL, 269, '国语', '流行', '国内', 0, 1, 1280000, 32000, 1200, '2003-07-31'),
(3002, '江南', 1002, '林俊杰', 2002, '因你而在', 'https://picsum.photos/seed/song2/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3', NULL, 264, '国语', '流行', '国外', 0, 1, 980000, 21000, 800, '2013-03-15'),
(3003, '泡沫', 1003, '邓紫棋', 2003, '新的心跳', 'https://picsum.photos/seed/song3/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3', NULL, 252, '国语', '流行', '国内', 1, 1, 1560000, 45000, 2000, '2015-10-30'),
(3004, '富士山下', 1004, '陈奕迅', 2004, 'U87', 'https://picsum.photos/seed/song4/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3', NULL, 258, '粤语', '流行', '粤语', 1, 1, 760000, 18000, 600, '2005-06-01'),
(3005, '稻香', 1001, '周杰伦', 2001, '叶惠美', 'https://picsum.photos/seed/song5/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3', NULL, 223, '国语', '民谣', '国内', 0, 1, 1100000, 29000, 900, '2008-10-15'),
(3006, 'Love Story', 1005, 'Taylor Swift', NULL, NULL, 'https://picsum.photos/seed/song6/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3', NULL, 235, '英语', '流行', '国外', 0, 1, 880000, 26000, 700, '2008-11-11'),
(3007, '一路向北', 1001, '周杰伦', 2001, '叶惠美', 'https://picsum.photos/seed/song7/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3', NULL, 268, '国语', '摇滚', '国内', 1, 1, 690000, 15000, 400, '2005-11-01'),
(3008, '光年之外', 1003, '邓紫棋', 2003, '新的心跳', 'https://picsum.photos/seed/song8/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3', NULL, 235, '国语', '流行', '国内', 0, 1, 1450000, 39000, 1600, '2016-12-30'),
(3009, '十年', 1004, '陈奕迅', 2004, 'U87', 'https://picsum.photos/seed/song9/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3', NULL, 205, '国语', '流行', '粤语', 0, 1, 830000, 20000, 550, '2003-04-16'),
(3010, '曹操', 1002, '林俊杰', 2002, '因你而在', 'https://picsum.photos/seed/song10/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3', NULL, 250, '国语', '古风', '国外', 0, 1, 720000, 17000, 500, '2006-02-17')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- 演示歌词（LRC 文本直接存入 lyric_url，LyricService 支持解析）
UPDATE `song`
SET `lyric_url` = CONCAT(
    '[00:00.00]作词：酷猫音乐\n',
    '[00:02.00]作曲：酷猫音乐\n',
    '[00:06.00]《', `name`, '》\n',
    '[00:11.00]风穿过城市的街角\n',
    '[00:17.00]把心事唱给你听\n',
    '[00:23.00]灯光落在旧琴弦上\n',
    '[00:29.00]每一个音符都是想念\n',
    '[00:35.00]如果时间可以慢一点\n',
    '[00:41.00]让我多陪你一程\n',
    '[00:47.00]就让旋律代替语言\n',
    '[00:53.00]在夜里轻轻回响\n',
    '[00:59.00]—— 酷猫音乐 · 演示歌词 ——'
)
WHERE `id` BETWEEN 3001 AND 3010;

-- ------------------------------------------------------------
-- MV
-- ------------------------------------------------------------
INSERT INTO `mv` (`id`, `song_id`, `name`, `singer_id`, `singer_name`, `cover_url`, `video_url`, `duration`, `quality`, `vip_only`, `status`, `play_count`, `description`)
VALUES
(4001, 3001, '晴天 MV', 1001, '周杰伦', 'https://picsum.photos/seed/mv1/640', 'https://www.w3schools.com/html/mov_bbb.mp4', 269, 'HD', 0, 1, 120000, '经典 MV'),
(4002, 3003, '泡沫 MV', 1003, '邓紫棋', 'https://picsum.photos/seed/mv2/640', 'https://www.w3schools.com/html/mov_bbb.mp4', 252, 'UHD', 1, 1, 98000, '高清 VIP 专享'),
(4003, 3002, '江南 MV', 1002, '林俊杰', 'https://picsum.photos/seed/mv3/640', 'https://www.w3schools.com/html/mov_bbb.mp4', 264, 'HD', 0, 1, 76000, '经典 MV')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- ------------------------------------------------------------
-- 轮播图
-- ------------------------------------------------------------
INSERT INTO `banner` (`id`, `title`, `image_url`, `link_url`, `sort`, `status`)
VALUES
(5001, '新歌首发 · 晴天', 'https://picsum.photos/seed/banner1/1200/400', '/song/3001', 1, 1),
(5002, 'VIP 会员特惠', 'https://picsum.photos/seed/banner2/1200/400', '/vip', 2, 1),
(5003, '音乐直播节', 'https://picsum.photos/seed/banner3/1200/400', '/live', 3, 1)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);

-- ------------------------------------------------------------
-- 歌单
-- ------------------------------------------------------------
INSERT INTO `playlist` (`id`, `name`, `cover_url`, `description`, `user_id`, `creator_name`, `is_public`, `song_count`, `play_count`, `collect_count`, `status`)
VALUES
(6001, '华语流行精选', 'https://picsum.photos/seed/pl1/400', '最好听的华语流行', 2, 'zhangsan', 1, 5, 12000, 320, 1),
(6002, '粤语经典', 'https://picsum.photos/seed/pl2/400', '经典粤语歌曲合集', 2, 'zhangsan', 1, 2, 8000, 210, 1),
(6003, '我的私藏', 'https://picsum.photos/seed/pl3/400', '私有歌单', 2, 'zhangsan', 0, 2, 0, 0, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

INSERT INTO `playlist_song` (`id`, `playlist_id`, `song_id`, `song_name`, `singer_name`, `cover_url`, `audio_url`, `duration`, `sort`)
VALUES
(7001, 6001, 3001, '晴天', '周杰伦', 'https://picsum.photos/seed/song1/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3', 269, 1),
(7002, 6001, 3002, '江南', '林俊杰', 'https://picsum.photos/seed/song2/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3', 264, 2),
(7003, 6001, 3003, '泡沫', '邓紫棋', 'https://picsum.photos/seed/song3/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3', 252, 3),
(7004, 6001, 3008, '光年之外', '邓紫棋', 'https://picsum.photos/seed/song8/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3', 235, 4),
(7005, 6001, 3009, '十年', '陈奕迅', 'https://picsum.photos/seed/song9/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3', 205, 5),
(7006, 6002, 3004, '富士山下', '陈奕迅', 'https://picsum.photos/seed/song4/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3', 258, 1),
(7007, 6002, 3009, '十年', '陈奕迅', 'https://picsum.photos/seed/song9/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3', 205, 2),
(7008, 6003, 3005, '稻香', '周杰伦', 'https://picsum.photos/seed/song5/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3', 223, 1),
(7009, 6003, 3010, '曹操', '林俊杰', 'https://picsum.photos/seed/song10/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3', 250, 2)
ON DUPLICATE KEY UPDATE `song_name` = VALUES(`song_name`);

-- ------------------------------------------------------------
-- 评论
-- ------------------------------------------------------------
INSERT INTO `song_comment` (`id`, `song_id`, `user_id`, `nickname`, `avatar`, `content`, `parent_id`, `like_count`, `status`)
VALUES
(8001, 3001, 2, '张三', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhangsan', '这首歌是我的青春！', 0, 128, 0),
(8002, 3001, 6, 'VIP小七', 'https://api.dicebear.com/7.x/avataaars/svg?seed=vipuser', '前奏一响就回到了那个夏天', 0, 66, 0),
(8003, 3003, 2, '张三', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhangsan', '泡沫真的好听', 0, 45, 0),
(8004, 3004, 6, 'VIP小七', 'https://api.dicebear.com/7.x/avataaars/svg?seed=vipuser', '粤语歌永远的神', 0, 33, 0)
ON DUPLICATE KEY UPDATE `content` = VALUES(`content`);

-- ------------------------------------------------------------
-- 我喜欢
-- ------------------------------------------------------------
INSERT INTO `song_like` (`id`, `user_id`, `song_id`, `song_name`, `singer_name`, `cover_url`, `audio_url`, `duration`)
VALUES
(9001, 2, 3001, '晴天', '周杰伦', 'https://picsum.photos/seed/song1/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3', 269),
(9002, 2, 3003, '泡沫', '邓紫棋', 'https://picsum.photos/seed/song3/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3', 252)
ON DUPLICATE KEY UPDATE `song_name` = VALUES(`song_name`);

-- ------------------------------------------------------------
-- 礼物配置
-- ------------------------------------------------------------
INSERT INTO `live_gift` (`id`, `name`, `icon`, `price`, `effect`, `status`)
VALUES
(10001, '鲜花', '🌹', 1, 'flower-burst', 1),
(10002, '音符', '🎵', 5, 'note-float', 1),
(10003, '爱心', '❤️', 10, 'heart-beat', 1),
(10004, '火箭', '🚀', 100, 'rocket-fullscreen', 1),
(10005, '城堡', '🏰', 520, 'castle-scene', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- ------------------------------------------------------------
-- 直播间
-- ------------------------------------------------------------
INSERT INTO `live_room` (`id`, `title`, `intro`, `cover_url`, `anchor_id`, `anchor_name`, `category`, `status`, `online_count`, `peak_online`, `gift_income`, `banned`)
VALUES
(11001, '深夜弹唱｜一起听老歌', '每晚八点，弹唱陪你入睡', 'https://picsum.photos/seed/live1/800/450', 3, 'anchor01', '弹唱', 'LIVING', 128, 356, 1200, 0),
(11002, '电台｜午夜心情树洞', '聊聊今天的心事', 'https://picsum.photos/seed/live2/800/450', 3, 'anchor01', '电台', 'LIVING', 56, 120, 300, 0),
(11003, '翻唱｜经典粤语金曲', '经典粤语歌翻唱', 'https://picsum.photos/seed/live3/800/450', 3, 'anchor01', '翻唱', 'NOT_STARTED', 0, 0, 0, 0)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);

-- ------------------------------------------------------------
-- VIP 套餐
-- ------------------------------------------------------------
INSERT INTO `vip_package` (`id`, `name`, `type`, `days`, `coin_amount`, `price`, `original_price`, `description`, `status`, `sort`)
VALUES
(12001, '月卡会员', 'MONTH', 30, NULL, 18.00, 25.00, '畅享高音质与 VIP 专属 MV', 1, 1),
(12002, '季卡会员', 'QUARTER', 90, NULL, 48.00, 75.00, '季度会员，立省 27 元', 1, 2),
(12003, '年卡会员', 'YEAR', 365, NULL, 168.00, 300.00, '全年畅享，超值之选', 1, 3),
(12004, '100 酷猫币', 'COIN', 0, 100, 10.00, NULL, '直播送礼，即刻到账', 1, 10),
(12005, '500 酷猫币', 'COIN', 0, 500, 50.00, NULL, '直播送礼，即刻到账', 1, 11),
(12006, '1000 酷猫币', 'COIN', 0, 1000, 100.00, NULL, '直播送礼，即刻到账', 1, 12),
(12007, '2000 酷猫币', 'COIN', 0, 2000, 200.00, NULL, '直播送礼，即刻到账', 1, 13)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- ------------------------------------------------------------
-- 音乐人
-- ------------------------------------------------------------
INSERT INTO `musician` (`id`, `user_id`, `stage_name`, `real_name`, `style`, `intro`, `avatar`, `representative`, `status`, `fans_count`, `total_play_count`, `total_like_count`)
VALUES
(13001, 4, '木子', '李木子', '民谣', '独立民谣音乐人，用歌声记录生活。', 'https://api.dicebear.com/7.x/avataaars/svg?seed=musician01', '《城市夜归人》', 'APPROVED', 1250, 56000, 3200),
(13002, 5, '小行星', '王小星', '电子', '电子音乐制作人', 'https://api.dicebear.com/7.x/avataaars/svg?seed=musician02', '《星际漫游》', 'PENDING', 0, 0, 0)
ON DUPLICATE KEY UPDATE `stage_name` = VALUES(`stage_name`);

INSERT INTO `musician_work` (`id`, `musician_id`, `user_id`, `name`, `album_name`, `cover_url`, `audio_url`, `duration`, `language`, `style`, `vip_only`, `release_date`, `status`, `play_count`, `like_count`, `comment_count`, `share_count`, `collect_count`)
VALUES
(14001, 13001, 4, '城市夜归人', '城市夜归人', 'https://picsum.photos/seed/work1/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3', 240, '国语', '民谣', 0, '2025-05-20', 'APPROVED', 22000, 1500, 88, 60, 320),
(14002, 13001, 4, '晚风告白', '城市夜归人', 'https://picsum.photos/seed/work2/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3', 215, '国语', '民谣', 1, '2025-08-01', 'APPROVED', 9800, 760, 42, 30, 180),
(14003, 13001, 4, '未审核的歌', NULL, 'https://picsum.photos/seed/work3/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3', 200, '国语', '民谣', 0, '2026-01-01', 'PENDING', 0, 0, 0, 0, 0)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

INSERT INTO `musician_album` (`id`, `musician_id`, `user_id`, `name`, `cover_url`, `intro`, `release_date`, `song_count`, `status`)
VALUES
(15001, 13001, 4, '城市夜归人', 'https://picsum.photos/seed/album5/400', '首张个人专辑', '2025-05-20', 2, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

INSERT INTO `musician_income` (`id`, `musician_id`, `user_id`, `type`, `amount`, `description`, `status`)
VALUES
(16001, 13001, 4, 'PLAY', 220, '播放收益结算', 'SUCCESS'),
(16002, 13001, 4, 'GIFT', 700, '直播礼物收益结算', 'SUCCESS')
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);

-- 音乐人粉丝（演示：粉丝地域分布）
INSERT INTO `musician_fans` (`id`, `musician_id`, `user_id`, `region`)
VALUES
(15501, 13001, 2, '华东'),
(15502, 13001, 3, '华南'),
(15503, 13001, 6, '华北'),
(15504, 13001, 1, '华东'),
(15505, 13001, 5, '西南')
ON DUPLICATE KEY UPDATE `region` = VALUES(`region`);

-- 近 30 天播放趋势（演示数据）
INSERT INTO `musician_play_stat` (`id`, `musician_id`, `work_id`, `stat_date`, `play_count`, `like_count`, `comment_count`, `share_count`)
VALUES
(16101, 13001, 14001, DATE_SUB(CURDATE(), INTERVAL 6 DAY), 820, 40, 3, 2),
(16102, 13001, 14001, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 910, 52, 4, 3),
(16103, 13001, 14001, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 1050, 61, 5, 4),
(16104, 13001, 14001, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 1230, 78, 6, 5),
(16105, 13001, 14001, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 1180, 70, 4, 6),
(16106, 13001, 14001, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 1420, 88, 7, 8),
(16107, 13001, 14001, CURDATE(), 1680, 102, 9, 11)
ON DUPLICATE KEY UPDATE `play_count` = VALUES(`play_count`);

-- ------------------------------------------------------------
-- 广告制作人
-- ------------------------------------------------------------
INSERT INTO `advertiser` (`id`, `user_id`, `company_name`, `real_name`, `ad_type`, `intro`, `avatar`, `representative`, `status`)
VALUES
(17001, 5, '星辰广告工作室', '王大星', '音频广告', '专注音乐类音频广告制作', 'https://api.dicebear.com/7.x/avataaars/svg?seed=adman01', '某品牌开屏音频', 'APPROVED'),
(17002, 2, '张三创意', '张三', '视频广告', '创意视频广告', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhangsan', '某饮料视频广告', 'PENDING')
ON DUPLICATE KEY UPDATE `company_name` = VALUES(`company_name`);

INSERT INTO `ad_material` (`id`, `advertiser_id`, `user_id`, `name`, `type`, `cover_url`, `file_url`, `content`, `status`, `exposure_count`, `click_count`)
VALUES
(18001, 17001, 5, '夏日音乐节音频广告', 'AUDIO', 'https://picsum.photos/seed/ad1/400', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3', '这个夏天，一起来音乐节！', 'APPROVED', 52000, 2600),
(18002, 17001, 5, '耳机产品视频广告', 'VIDEO', 'https://picsum.photos/seed/ad2/400', 'https://www.w3schools.com/html/mov_bbb.mp4', '沉浸式音质体验', 'APPROVED', 38000, 1500),
(18003, 17001, 5, '会员福利图文广告', 'IMAGE_TEXT', 'https://picsum.photos/seed/ad3/400', NULL, '开通会员立享 8 折', 'PENDING', 0, 0)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

INSERT INTO `ad_plan` (`id`, `advertiser_id`, `user_id`, `material_id`, `material_name`, `slot`, `start_time`, `end_time`, `target_audience`, `days`, `daily_price`, `amount`, `payment_status`, `status`, `exposure_count`, `click_count`)
VALUES
(19001, 17001, 5, 18001, '夏日音乐节音频广告', 'SPLASH', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 'ALL', 31, 800.00, 24800.00, 'PAID', 'RUNNING', 52000, 2600),
(19002, 17001, 5, 18002, '耳机产品视频广告', 'HOME_BANNER', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 'VIP', 31, 520.00, 16120.00, 'PAID', 'RUNNING', 38000, 1500)
ON DUPLICATE KEY UPDATE `material_name` = VALUES(`material_name`);

-- 广告地域/时段统计（演示数据）
INSERT INTO `ad_stat_daily` (`id`, `advertiser_id`, `material_id`, `plan_id`, `stat_date`, `exposure_count`, `click_count`, `region`, `hour_slot`)
VALUES
(20101, 17001, 18001, 19001, CURDATE(), 8200, 410, '华东', 9),
(20102, 17001, 18001, 19001, CURDATE(), 7600, 380, '华南', 12),
(20103, 17001, 18001, 19001, CURDATE(), 5400, 270, '华北', 15),
(20104, 17001, 18001, 19001, CURDATE(), 3200, 160, '西南', 18),
(20105, 17001, 18001, 19001, CURDATE(), 2100, 105, '其他', 21),
(20106, 17001, 18002, 19002, CURDATE(), 6100, 305, '华东', 10),
(20107, 17001, 18002, 19002, CURDATE(), 4800, 240, '华南', 14),
(20108, 17001, 18002, 19002, CURDATE(), 3600, 180, '华北', 20)
ON DUPLICATE KEY UPDATE `exposure_count` = VALUES(`exposure_count`);
