-- ============================================================
-- 酷猫音乐平台 数据库结构（单库按模块分表，共 10 个数据模块）
-- MySQL 8.0
-- ============================================================
CREATE DATABASE IF NOT EXISTS `kumao_music`
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `kumao_music`;

-- ------------------------------------------------------------
-- 1. user-service：用户 / 登录记录 / 主播申请
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`              BIGINT       NOT NULL COMMENT '主键',
    `username`        VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`        VARCHAR(120) NOT NULL COMMENT 'BCrypt 密码',
    `phone`           VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    `nickname`        VARCHAR(64)           DEFAULT NULL COMMENT '昵称',
    `avatar`          VARCHAR(500)          DEFAULT NULL COMMENT '头像',
    `intro`           VARCHAR(500)          DEFAULT NULL COMMENT '简介',
    `roles`           VARCHAR(200) NOT NULL DEFAULT 'USER' COMMENT '角色，逗号分隔',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '0 正常 1 禁用',
    `third_type`      VARCHAR(20)           DEFAULT NULL COMMENT 'QQ/WECHAT',
    `third_open_id`   VARCHAR(128)          DEFAULT NULL COMMENT '第三方 openId',
    `login_policy`    VARCHAR(20)  NOT NULL DEFAULT 'SHARED' COMMENT 'SHARED 共存 / EXCLUSIVE 互斥',
    `last_login_time` DATETIME              DEFAULT NULL,
    `muted_until`     DATETIME              DEFAULT NULL COMMENT 'AI 审核违规禁言截止时间',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_phone` (`phone`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户';

CREATE TABLE IF NOT EXISTS `user_login_log` (
    `id`         BIGINT       NOT NULL,
    `user_id`    BIGINT       NOT NULL,
    `device`     VARCHAR(20)  NOT NULL COMMENT 'PC/APP/DESKTOP/ADMIN',
    `ip`         VARCHAR(64)           DEFAULT NULL,
    `user_agent` VARCHAR(500)          DEFAULT NULL,
    `login_type` VARCHAR(20)           DEFAULT NULL,
    `token_id`   VARCHAR(64)           DEFAULT NULL,
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '登录记录';

CREATE TABLE IF NOT EXISTS `anchor_apply` (
    `id`            BIGINT       NOT NULL,
    `user_id`       BIGINT       NOT NULL,
    `real_name`     VARCHAR(64)           DEFAULT NULL,
    `id_card`       VARCHAR(64)           DEFAULT NULL,
    `live_category` VARCHAR(32)           DEFAULT NULL,
    `intro`         VARCHAR(500)          DEFAULT NULL,
    `cover_url`     VARCHAR(500)          DEFAULT NULL,
    `status`        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    `reject_reason` VARCHAR(500)          DEFAULT NULL,
    `audit_by`      BIGINT                DEFAULT NULL,
    `audit_time`    VARCHAR(32)           DEFAULT NULL,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '主播申请';

-- ------------------------------------------------------------
-- 2. music-service：歌手 / 歌曲 / 专辑 / MV / 轮播
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `singer` (
    `id`         BIGINT       NOT NULL,
    `name`       VARCHAR(128) NOT NULL,
    `avatar`     VARCHAR(500)          DEFAULT NULL,
    `gender`     VARCHAR(10)           DEFAULT NULL,
    `region`     VARCHAR(20)           DEFAULT NULL COMMENT '国内/国外/港台/欧美/日韩',
    `intro`      VARCHAR(1000)         DEFAULT NULL,
    `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '0待审核/1已认证/2未通过',
    `musician_id` BIGINT                DEFAULT NULL COMMENT '关联音乐人',
    `user_id`     BIGINT                DEFAULT NULL COMMENT '申请用户',
    `reject_reason` VARCHAR(500)        DEFAULT NULL,
    `audit_time`  VARCHAR(32)           DEFAULT NULL,
    `fans_count` BIGINT       NOT NULL DEFAULT 0,
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`),
    KEY `idx_user` (`user_id`),
    UNIQUE KEY `uk_musician` (`musician_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '歌手';

CREATE TABLE IF NOT EXISTS `album` (
    `id`          BIGINT       NOT NULL,
    `name`        VARCHAR(128) NOT NULL,
    `singer_id`   BIGINT                DEFAULT NULL,
    `singer_name` VARCHAR(128)          DEFAULT NULL,
    `cover_url`   VARCHAR(500)          DEFAULT NULL,
    `intro`       VARCHAR(1000)         DEFAULT NULL,
    `release_date` DATE                 DEFAULT NULL,
    `status`      TINYINT      NOT NULL DEFAULT 1,
    `musician_id` BIGINT                DEFAULT NULL,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '专辑';

CREATE TABLE IF NOT EXISTS `song` (
    `id`            BIGINT       NOT NULL,
    `name`          VARCHAR(200) NOT NULL,
    `singer_id`     BIGINT                DEFAULT NULL,
    `singer_name`   VARCHAR(128)          DEFAULT NULL,
    `album_id`      BIGINT                DEFAULT NULL,
    `album_name`    VARCHAR(128)          DEFAULT NULL,
    `cover_url`     VARCHAR(500)          DEFAULT NULL,
    `audio_url`     VARCHAR(500)          DEFAULT NULL,
    `lyric_url`     VARCHAR(2000)         DEFAULT NULL,
    `duration`      INT          NOT NULL DEFAULT 0 COMMENT '秒',
    `language`      VARCHAR(20)           DEFAULT NULL COMMENT '国语/粤语/英语/日语/韩语',
    `style`         VARCHAR(20)           DEFAULT NULL COMMENT '流行/摇滚/民谣/电子/说唱/古风',
    `region`        VARCHAR(20)           DEFAULT NULL COMMENT '国内/国外/粤语',
    `vip_only`      TINYINT(1)   NOT NULL DEFAULT 0,
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '0 待审核 1 已上架 2 已下架',
    `play_count`    BIGINT       NOT NULL DEFAULT 0,
    `like_count`    BIGINT       NOT NULL DEFAULT 0,
    `comment_count` BIGINT       NOT NULL DEFAULT 0,
    `release_date`  DATE                  DEFAULT NULL,
    `musician_id`   BIGINT                DEFAULT NULL,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_singer` (`singer_id`),
    KEY `idx_region` (`region`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '歌曲';

CREATE TABLE IF NOT EXISTS `mv` (
    `id`          BIGINT       NOT NULL,
    `song_id`     BIGINT                DEFAULT NULL COMMENT '关联歌曲ID',
    `name`        VARCHAR(200) NOT NULL,
    `singer_id`   BIGINT                DEFAULT NULL,
    `singer_name` VARCHAR(128)          DEFAULT NULL,
    `musician_id` BIGINT                DEFAULT NULL,
    `uploader_user_id` BIGINT            DEFAULT NULL,
    `cover_url`   VARCHAR(500)          DEFAULT NULL,
    `video_url`   VARCHAR(500)          DEFAULT NULL,
    `duration`    INT          NOT NULL DEFAULT 0,
    `quality`     VARCHAR(10)  NOT NULL DEFAULT 'HD',
    `vip_only`    TINYINT(1)   NOT NULL DEFAULT 0,
    `status`      TINYINT      NOT NULL DEFAULT 1,
    `audit_status` VARCHAR(20) NOT NULL DEFAULT 'APPROVED' COMMENT 'PENDING/APPROVED/REJECTED',
    `reject_reason` VARCHAR(500)         DEFAULT NULL,
    `audit_time`  VARCHAR(32)           DEFAULT NULL,
    `play_count`  BIGINT       NOT NULL DEFAULT 0,
    `description` VARCHAR(1000)         DEFAULT NULL,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_song` (`song_id`),
    KEY `idx_uploader` (`uploader_user_id`),
    KEY `idx_audit` (`audit_status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'MV';

CREATE TABLE IF NOT EXISTS `banner` (
    `id`         BIGINT       NOT NULL,
    `title`      VARCHAR(128)          DEFAULT NULL,
    `image_url`  VARCHAR(500)          DEFAULT NULL,
    `link_url`   VARCHAR(500)          DEFAULT NULL,
    `sort`       INT          NOT NULL DEFAULT 0,
    `status`     TINYINT      NOT NULL DEFAULT 1,
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '轮播图';

-- ------------------------------------------------------------
-- 3. playlist-service：歌单 / 歌单歌曲 / 收藏
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `playlist` (
    `id`            BIGINT       NOT NULL,
    `name`          VARCHAR(200) NOT NULL,
    `cover_url`     VARCHAR(500)          DEFAULT NULL,
    `description`   VARCHAR(1000)         DEFAULT NULL,
    `user_id`       BIGINT       NOT NULL,
    `creator_name`  VARCHAR(64)           DEFAULT NULL,
    `is_public`     TINYINT      NOT NULL DEFAULT 1,
    `song_count`    INT          NOT NULL DEFAULT 0,
    `play_count`    BIGINT       NOT NULL DEFAULT 0,
    `collect_count` BIGINT       NOT NULL DEFAULT 0,
    `status`        TINYINT      NOT NULL DEFAULT 1,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '歌单';

CREATE TABLE IF NOT EXISTS `playlist_song` (
    `id`          BIGINT       NOT NULL,
    `playlist_id` BIGINT       NOT NULL,
    `song_id`     BIGINT       NOT NULL,
    `song_name`   VARCHAR(200)          DEFAULT NULL,
    `singer_name` VARCHAR(128)          DEFAULT NULL,
    `cover_url`   VARCHAR(500)          DEFAULT NULL,
    `audio_url`   VARCHAR(500)          DEFAULT NULL,
    `duration`    INT          NOT NULL DEFAULT 0,
    `sort`        INT          NOT NULL DEFAULT 0,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_playlist` (`playlist_id`),
    KEY `idx_song` (`song_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '歌单歌曲';

CREATE TABLE IF NOT EXISTS `playlist_collect` (
    `id`          BIGINT   NOT NULL,
    `playlist_id` BIGINT   NOT NULL,
    `user_id`     BIGINT   NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pl_user` (`playlist_id`, `user_id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '歌单收藏';

-- ------------------------------------------------------------
-- 4. comment-service：评论 / 红心 / 播放记录
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `song_comment` (
    `id`             BIGINT       NOT NULL,
    `song_id`        BIGINT       NOT NULL,
    `user_id`        BIGINT       NOT NULL,
    `nickname`       VARCHAR(64)           DEFAULT NULL,
    `avatar`         VARCHAR(500)          DEFAULT NULL,
    `content`        VARCHAR(1000) NOT NULL,
    `parent_id`      BIGINT       NOT NULL DEFAULT 0,
    `reply_user_id`  BIGINT                DEFAULT NULL,
    `reply_nickname` VARCHAR(64)           DEFAULT NULL,
    `like_count`     INT          NOT NULL DEFAULT 0,
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '0 正常 1 已屏蔽',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_song` (`song_id`, `status`),
    KEY `idx_parent` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '歌曲评论';

CREATE TABLE IF NOT EXISTS `song_like` (
    `id`          BIGINT       NOT NULL,
    `user_id`     BIGINT       NOT NULL,
    `song_id`     BIGINT       NOT NULL,
    `song_name`   VARCHAR(200)          DEFAULT NULL,
    `singer_name` VARCHAR(128)          DEFAULT NULL,
    `cover_url`   VARCHAR(500)          DEFAULT NULL,
    `audio_url`   VARCHAR(500)          DEFAULT NULL,
    `duration`    INT          NOT NULL DEFAULT 0,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_song` (`user_id`, `song_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '我喜欢';

CREATE TABLE IF NOT EXISTS `play_record` (
    `id`             BIGINT       NOT NULL,
    `user_id`        BIGINT       NOT NULL,
    `song_id`        BIGINT       NOT NULL,
    `song_name`      VARCHAR(200)          DEFAULT NULL,
    `singer_name`    VARCHAR(128)          DEFAULT NULL,
    `cover_url`      VARCHAR(500)          DEFAULT NULL,
    `device`         VARCHAR(20)           DEFAULT NULL COMMENT 'PC/APP',
    `duration`       INT          NOT NULL DEFAULT 0,
    `played_seconds` INT          NOT NULL DEFAULT 0,
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_song` (`song_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '播放记录';

-- ------------------------------------------------------------
-- 5. file-service：文件记录
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `file_record` (
    `id`            BIGINT       NOT NULL,
    `original_name` VARCHAR(255)          DEFAULT NULL,
    `object_key`    VARCHAR(500) NOT NULL,
    `url`           VARCHAR(800)          DEFAULT NULL,
    `bucket`        VARCHAR(64)           DEFAULT NULL,
    `content_type`  VARCHAR(120)          DEFAULT NULL,
    `size`          BIGINT       NOT NULL DEFAULT 0,
    `file_type`     VARCHAR(20)           DEFAULT NULL COMMENT 'IMAGE/AUDIO/VIDEO/LYRIC',
    `biz_type`      VARCHAR(40)           DEFAULT NULL,
    `uploader_id`   BIGINT                DEFAULT NULL,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_uploader` (`uploader_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文件记录';

-- ------------------------------------------------------------
-- 6. live-service：直播间 / 弹幕 / 礼物 / 虚拟币
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `live_room` (
    `id`            BIGINT       NOT NULL,
    `title`         VARCHAR(200) NOT NULL,
    `intro`         VARCHAR(1000)         DEFAULT NULL,
    `cover_url`     VARCHAR(500)          DEFAULT NULL,
    `anchor_id`     BIGINT       NOT NULL,
    `anchor_name`   VARCHAR(64)           DEFAULT NULL,
    `anchor_avatar` VARCHAR(500)          DEFAULT NULL,
    `category`      VARCHAR(20)           DEFAULT NULL COMMENT '弹唱/电台/翻唱',
    `scene`         VARCHAR(20)  NOT NULL DEFAULT 'INDOOR' COMMENT 'INDOOR 室内 / OUTDOOR 户外',
    `location`      VARCHAR(200)          DEFAULT NULL COMMENT '户外直播地点',
    `status`        VARCHAR(20)  NOT NULL DEFAULT 'NOT_STARTED',
    `live_started_at` DATETIME            DEFAULT NULL COMMENT '本场直播开始时间',
    `push_url`      VARCHAR(500)          DEFAULT NULL,
    `pull_url`      VARCHAR(500)          DEFAULT NULL,
    `replay_url`    VARCHAR(500)          DEFAULT NULL,
    `online_count`  BIGINT       NOT NULL DEFAULT 0,
    `peak_online`   BIGINT       NOT NULL DEFAULT 0,
    `like_count`    BIGINT       NOT NULL DEFAULT 0,
    `gift_income`   BIGINT       NOT NULL DEFAULT 0,
    `banned`        TINYINT      NOT NULL DEFAULT 0,
    `ban_reason`    VARCHAR(500)          DEFAULT NULL,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_anchor` (`anchor_id`),
    KEY `idx_status` (`status`),
    KEY `idx_scene` (`scene`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '直播间';

CREATE TABLE IF NOT EXISTS `live_danmaku` (
    `id`          BIGINT       NOT NULL,
    `room_id`     BIGINT       NOT NULL,
    `user_id`     BIGINT                DEFAULT NULL,
    `nickname`    VARCHAR(64)           DEFAULT NULL,
    `content`     VARCHAR(500)          DEFAULT NULL,
    `color`       VARCHAR(20)           DEFAULT NULL,
    `vip`         TINYINT(1)   NOT NULL DEFAULT 0,
    `type`        VARCHAR(20)  NOT NULL DEFAULT 'TEXT',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_room_time` (`room_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '弹幕';

CREATE TABLE IF NOT EXISTS `live_gift` (
    `id`          BIGINT       NOT NULL,
    `name`        VARCHAR(64)  NOT NULL,
    `icon`        VARCHAR(500)          DEFAULT NULL,
    `price`       INT          NOT NULL DEFAULT 0,
    `effect`      VARCHAR(200)          DEFAULT NULL,
    `status`      TINYINT      NOT NULL DEFAULT 1,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '礼物配置';

CREATE TABLE IF NOT EXISTS `live_gift_record` (
    `id`          BIGINT      NOT NULL,
    `room_id`     BIGINT      NOT NULL,
    `gift_id`     BIGINT      NOT NULL,
    `gift_name`   VARCHAR(64)          DEFAULT NULL,
    `gift_icon`   VARCHAR(500)         DEFAULT NULL,
    `price`       INT         NOT NULL DEFAULT 0,
    `count`       INT         NOT NULL DEFAULT 1,
    `total_price` INT         NOT NULL DEFAULT 0,
    `sender_id`   BIGINT               DEFAULT NULL,
    `sender_name` VARCHAR(64)          DEFAULT NULL,
    `anchor_id`   BIGINT               DEFAULT NULL,
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_anchor` (`anchor_id`),
    KEY `idx_room` (`room_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '礼物记录';

CREATE TABLE IF NOT EXISTS `live_coin` (
    `id`          BIGINT   NOT NULL,
    `user_id`     BIGINT   NOT NULL,
    `balance`     INT      NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '虚拟币账户';

CREATE TABLE IF NOT EXISTS `live_coin_recharge` (
    `id`            BIGINT       NOT NULL,
    `user_id`       BIGINT       NOT NULL,
    `order_no`      VARCHAR(64)  NOT NULL,
    `amount`        INT          NOT NULL DEFAULT 0,
    `balance_after` INT          NOT NULL DEFAULT 0,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '酷猫币充值入账记录';

-- ------------------------------------------------------------
-- 7. vip-service：套餐 / 订单 / 会员
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `vip_package` (
    `id`             BIGINT        NOT NULL,
    `name`           VARCHAR(64)   NOT NULL,
    `type`           VARCHAR(20)   NOT NULL COMMENT 'MONTH/QUARTER/YEAR/COIN',
    `days`           INT           NOT NULL DEFAULT 30,
    `coin_amount`    INT                    DEFAULT NULL COMMENT 'COIN类型套餐包含的酷猫币数量',
    `price`          DECIMAL(10,2) NOT NULL DEFAULT 0,
    `original_price` DECIMAL(10,2)          DEFAULT NULL,
    `description`    VARCHAR(500)           DEFAULT NULL,
    `status`         TINYINT       NOT NULL DEFAULT 1,
    `sort`           INT           NOT NULL DEFAULT 0,
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'VIP / 酷猫币充值套餐';

CREATE TABLE IF NOT EXISTS `vip_order` (
    `id`              BIGINT        NOT NULL,
    `order_no`        VARCHAR(64)   NOT NULL,
    `user_id`         BIGINT        NOT NULL,
    `package_id`      BIGINT        NOT NULL,
    `package_name`    VARCHAR(64)            DEFAULT NULL,
    `days`            INT           NOT NULL DEFAULT 0,
    `amount`          DECIMAL(10,2) NOT NULL DEFAULT 0,
    `status`          VARCHAR(20)   NOT NULL DEFAULT 'UNPAID',
    `pay_type`        VARCHAR(20)            DEFAULT NULL COMMENT 'ALIPAY / WECHAT / MOCK / AD_REWARD',
    `transaction_id`  VARCHAR(64)            DEFAULT NULL COMMENT '第三方交易号',
    `pay_time`        DATETIME               DEFAULT NULL COMMENT '支付成功时间',
    `pay_expire_time` DATETIME               DEFAULT NULL COMMENT '支付超时时间',
    `notify_time`     DATETIME               DEFAULT NULL COMMENT '回调到达时间',
    `expire_time`     DATETIME               DEFAULT NULL COMMENT '会员到期时间',
    `remark`          VARCHAR(255)           DEFAULT NULL,
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status_expire` (`status`, `pay_expire_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'VIP 订单';

CREATE TABLE IF NOT EXISTS `vip_member` (
    `id`              BIGINT      NOT NULL,
    `user_id`         BIGINT      NOT NULL,
    `level`           VARCHAR(20) NOT NULL DEFAULT 'VIP',
    `start_time`      DATETIME             DEFAULT NULL,
    `expire_time`     DATETIME             DEFAULT NULL,
    `status`          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `total_days`      INT         NOT NULL DEFAULT 0,
    `danmaku_color`   VARCHAR(20)          DEFAULT NULL,
    `entrance_effect` VARCHAR(64)          DEFAULT NULL,
    `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'VIP 会员';

-- ------------------------------------------------------------
-- 8. musician-service：音乐人 / 作品 / 专辑 / 粉丝 / 收益
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `musician` (
    `id`               BIGINT       NOT NULL,
    `user_id`          BIGINT       NOT NULL,
    `stage_name`       VARCHAR(128) NOT NULL COMMENT '艺名',
    `real_name`        VARCHAR(64)           DEFAULT NULL,
    `style`            VARCHAR(64)           DEFAULT NULL,
    `intro`            VARCHAR(1000)         DEFAULT NULL,
    `avatar`           VARCHAR(500)          DEFAULT NULL,
    `representative`   VARCHAR(1000)         DEFAULT NULL COMMENT '代表作说明',
    `status`           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    `reject_reason`    VARCHAR(500)          DEFAULT NULL,
    `audit_by`         BIGINT                DEFAULT NULL,
    `audit_time`       VARCHAR(32)           DEFAULT NULL,
    `fans_count`       BIGINT       NOT NULL DEFAULT 0,
    `total_play_count` BIGINT       NOT NULL DEFAULT 0,
    `total_like_count` BIGINT       NOT NULL DEFAULT 0,
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`          TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '音乐人';

CREATE TABLE IF NOT EXISTS `musician_work` (
    `id`            BIGINT       NOT NULL,
    `musician_id`   BIGINT       NOT NULL,
    `user_id`       BIGINT       NOT NULL,
    `name`          VARCHAR(200) NOT NULL,
    `album_id`      BIGINT                DEFAULT NULL,
    `album_name`    VARCHAR(128)          DEFAULT NULL,
    `cover_url`     VARCHAR(500)          DEFAULT NULL,
    `audio_url`     VARCHAR(500)          DEFAULT NULL,
    `lyric_url`     VARCHAR(2000)         DEFAULT NULL,
    `duration`      INT          NOT NULL DEFAULT 0,
    `language`      VARCHAR(20)           DEFAULT NULL,
    `style`         VARCHAR(20)           DEFAULT NULL,
    `vip_only`      TINYINT(1)   NOT NULL DEFAULT 0,
    `release_date`  DATE                  DEFAULT NULL,
    `status`        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    `reject_reason` VARCHAR(500)          DEFAULT NULL,
    `song_id`       BIGINT                DEFAULT NULL COMMENT '同步到 music-service 的歌曲ID',
    `play_count`    BIGINT       NOT NULL DEFAULT 0,
    `like_count`    BIGINT       NOT NULL DEFAULT 0,
    `comment_count` BIGINT       NOT NULL DEFAULT 0,
    `share_count`   BIGINT       NOT NULL DEFAULT 0,
    `collect_count` BIGINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_musician` (`musician_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '音乐人作品';

CREATE TABLE IF NOT EXISTS `musician_album` (
    `id`          BIGINT       NOT NULL,
    `musician_id` BIGINT       NOT NULL,
    `user_id`     BIGINT       NOT NULL,
    `name`        VARCHAR(128) NOT NULL,
    `cover_url`   VARCHAR(500)          DEFAULT NULL,
    `intro`       VARCHAR(1000)         DEFAULT NULL,
    `release_date` DATE                DEFAULT NULL,
    `song_count`  INT          NOT NULL DEFAULT 0,
    `status`      TINYINT      NOT NULL DEFAULT 1,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_musician` (`musician_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '音乐人专辑';

CREATE TABLE IF NOT EXISTS `musician_fans` (
    `id`          BIGINT   NOT NULL,
    `musician_id` BIGINT   NOT NULL,
    `user_id`     BIGINT   NOT NULL,
    `region`      VARCHAR(40)       DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_music_user` (`musician_id`, `user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '音乐人粉丝';

CREATE TABLE IF NOT EXISTS `musician_income` (
    `id`          BIGINT      NOT NULL,
    `musician_id` BIGINT      NOT NULL,
    `user_id`     BIGINT      NOT NULL,
    `type`        VARCHAR(20) NOT NULL COMMENT 'PLAY/GIFT/WITHDRAW',
    `amount`      INT         NOT NULL DEFAULT 0,
    `description` VARCHAR(500)         DEFAULT NULL,
    `status`      VARCHAR(20)          DEFAULT NULL,
    `biz_id`      BIGINT               DEFAULT NULL,
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_musician` (`musician_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '音乐人收益';

CREATE TABLE IF NOT EXISTS `musician_withdraw` (
    `id`          BIGINT      NOT NULL,
    `musician_id` BIGINT      NOT NULL,
    `user_id`     BIGINT      NOT NULL,
    `amount`      INT         NOT NULL DEFAULT 0,
    `account`     VARCHAR(200)         DEFAULT NULL,
    `pay_type`    VARCHAR(20)          DEFAULT NULL COMMENT 'ALIPAY/WECHAT',
    `status`      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `remark`      VARCHAR(500)         DEFAULT NULL,
    `audit_time`  VARCHAR(32)          DEFAULT NULL,
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_musician` (`musician_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '音乐人提现';

CREATE TABLE IF NOT EXISTS `musician_play_stat` (
    `id`            BIGINT   NOT NULL,
    `musician_id`   BIGINT   NOT NULL,
    `work_id`       BIGINT            DEFAULT NULL,
    `stat_date`     DATE     NOT NULL,
    `play_count`    BIGINT   NOT NULL DEFAULT 0,
    `like_count`    BIGINT   NOT NULL DEFAULT 0,
    `comment_count` BIGINT   NOT NULL DEFAULT 0,
    `share_count`   BIGINT   NOT NULL DEFAULT 0,
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_music_date` (`musician_id`, `stat_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '音乐人播放统计';

-- ------------------------------------------------------------
-- 9. ad-service：广告制作人 / 素材 / 投放 / 统计 / 收益
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `advertiser` (
    `id`             BIGINT       NOT NULL,
    `user_id`        BIGINT       NOT NULL,
    `company_name`   VARCHAR(128) NOT NULL COMMENT '艺名/主体名称',
    `real_name`      VARCHAR(64)           DEFAULT NULL,
    `ad_type`        VARCHAR(64)           DEFAULT NULL,
    `intro`          VARCHAR(1000)         DEFAULT NULL,
    `avatar`         VARCHAR(500)          DEFAULT NULL,
    `representative` VARCHAR(1000)         DEFAULT NULL,
    `status`         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    `reject_reason`  VARCHAR(500)          DEFAULT NULL,
    `audit_by`       BIGINT                DEFAULT NULL,
    `audit_time`     VARCHAR(32)           DEFAULT NULL,
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '广告制作人';

CREATE TABLE IF NOT EXISTS `ad_material` (
    `id`             BIGINT       NOT NULL,
    `advertiser_id`  BIGINT       NOT NULL,
    `user_id`        BIGINT       NOT NULL,
    `name`           VARCHAR(200) NOT NULL,
    `type`           VARCHAR(20)  NOT NULL COMMENT 'AUDIO/VIDEO/IMAGE_TEXT',
    `cover_url`      VARCHAR(500)          DEFAULT NULL,
    `file_url`       VARCHAR(500)          DEFAULT NULL,
    `content`        VARCHAR(2000)         DEFAULT NULL,
    `status`         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    `reject_reason`  VARCHAR(500)          DEFAULT NULL,
    `exposure_count` BIGINT       NOT NULL DEFAULT 0,
    `click_count`    BIGINT       NOT NULL DEFAULT 0,
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_advertiser` (`advertiser_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '广告素材';

CREATE TABLE IF NOT EXISTS `ad_plan` (
    `id`             BIGINT      NOT NULL,
    `advertiser_id`  BIGINT      NOT NULL,
    `user_id`        BIGINT      NOT NULL,
    `material_id`    BIGINT      NOT NULL,
    `material_name`  VARCHAR(200)         DEFAULT NULL,
    `slot`           VARCHAR(30) NOT NULL COMMENT 'SPLASH/HOME_BANNER/LIVE/PLAYER',
    `start_time`     DATETIME             DEFAULT NULL,
    `end_time`       DATETIME             DEFAULT NULL,
    `target_audience` VARCHAR(50)         DEFAULT NULL,
    `days`           INT         NOT NULL DEFAULT 1,
    `daily_price`    DECIMAL(10,2) NOT NULL DEFAULT 0,
    `amount`         DECIMAL(12,2) NOT NULL DEFAULT 0,
    `payment_status` VARCHAR(20) NOT NULL DEFAULT 'PAID' COMMENT 'UNPAID/PAID',
    `payment_order_no` VARCHAR(64)        DEFAULT NULL,
    `paid_time`      DATETIME             DEFAULT NULL,
    `status`         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `reject_reason`  VARCHAR(500)         DEFAULT NULL,
    `exposure_count` BIGINT      NOT NULL DEFAULT 0,
    `click_count`    BIGINT      NOT NULL DEFAULT 0,
    `create_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_advertiser` (`advertiser_id`),
    KEY `idx_status_slot` (`status`, `slot`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '广告投放计划';

CREATE TABLE IF NOT EXISTS `ad_stat_daily` (
    `id`             BIGINT   NOT NULL,
    `advertiser_id`  BIGINT   NOT NULL,
    `material_id`    BIGINT            DEFAULT NULL,
    `plan_id`        BIGINT            DEFAULT NULL,
    `stat_date`      DATE     NOT NULL,
    `exposure_count` BIGINT   NOT NULL DEFAULT 0,
    `click_count`    BIGINT   NOT NULL DEFAULT 0,
    `region`         VARCHAR(40)       DEFAULT NULL,
    `hour_slot`      INT               DEFAULT NULL,
    `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_adv_date` (`advertiser_id`, `stat_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '广告日统计';

-- ------------------------------------------------------------
-- 11. vip-service：支付回调日志
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `pay_notify_log` (
    `id`             BIGINT      NOT NULL,
    `channel`        VARCHAR(20) NOT NULL COMMENT 'ALIPAY/WECHAT',
    `order_no`       VARCHAR(64)          DEFAULT NULL,
    `transaction_id` VARCHAR(64)          DEFAULT NULL,
    `success`        TINYINT     NOT NULL DEFAULT 0,
    `message`        VARCHAR(255)         DEFAULT NULL,
    `raw_body`       TEXT,
    `create_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_no`),
    KEY `idx_create` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '支付回调日志';

CREATE TABLE IF NOT EXISTS `review_log` (
    `id`           BIGINT       NOT NULL,
    `category`     VARCHAR(40)  NOT NULL COMMENT 'ANCHOR_APPLY/MUSICIAN/ADVERTISER/AD_MATERIAL/AD_PLAN/SONG/MV/COMMENT/LIVE_ROOM/DANMAKU/SINGER',
    `item_id`      VARCHAR(64)           DEFAULT NULL,
    `item_title`   VARCHAR(300)          DEFAULT NULL,
    `decision`     VARCHAR(20)  NOT NULL COMMENT 'APPROVED/REJECTED',
    `admin_reason` VARCHAR(1000)         DEFAULT NULL,
    `ai_decision`  VARCHAR(20)           DEFAULT NULL,
    `ai_risk_score` INT                   DEFAULT NULL,
    `ai_source`    VARCHAR(30)           DEFAULT NULL COMMENT 'DEEPSEEK/LOCAL_RULE',
    `ai_reasons`   VARCHAR(2000)         DEFAULT NULL,
    `admin_id`     BIGINT                DEFAULT NULL,
    `admin_name`   VARCHAR(100)          DEFAULT NULL,
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_review_category_decision` (`category`, `decision`),
    KEY `idx_review_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI辅助审核日志';
