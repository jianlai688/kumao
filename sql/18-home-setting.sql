-- 首页展示设置（单行配置，id 固定为 1）
-- 用于「发现音乐」页顶部今日主打位的自动轮播控制
CREATE TABLE IF NOT EXISTS `home_setting` (
    `id`            BIGINT   NOT NULL COMMENT '主键，固定 1',
    `hero_autoplay` TINYINT  NOT NULL DEFAULT 1 COMMENT '今日主打是否自动轮播：1 开 0 关',
    `hero_interval` INT      NOT NULL DEFAULT 5 COMMENT '自动切换间隔（秒）',
    `hero_count`    INT      NOT NULL DEFAULT 3 COMMENT '参与轮播的歌曲条数（取热歌榜前 N）',
    `create_time`   DATETIME          DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME          DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '首页展示设置';

INSERT INTO `home_setting` (`id`, `hero_autoplay`, `hero_interval`, `hero_count`)
VALUES (1, 1, 5, 3)
ON DUPLICATE KEY UPDATE `id` = `id`;
