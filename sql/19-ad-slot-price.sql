-- 广告位日单价配置（管理员可调整）
-- 新建投流计划按下表价格计费；历史计划已固化金额，不受影响。
USE `kumao_music`;

CREATE TABLE IF NOT EXISTS `ad_slot_price` (
    `slot`        VARCHAR(30)   NOT NULL COMMENT 'SPLASH/HOME_BANNER/LIVE/PLAYER/VIP_REWARD',
    `name`        VARCHAR(50)   NOT NULL,
    `daily_price` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '每日单价（元）',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`slot`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '广告位日单价';

INSERT INTO `ad_slot_price` (`slot`, `name`, `daily_price`) VALUES
    ('SPLASH', '开屏广告', 800.00),
    ('HOME_BANNER', '首页 Banner', 520.00),
    ('LIVE', '直播页', 300.00),
    ('PLAYER', '播放页', 200.00),
    ('VIP_REWARD', 'VIP 激励视频', 100.00)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);
