-- ============================================================
-- 酷猫音乐平台 歌手认证升级脚本
-- 适用场景：已按 01~08 初始化过数据库
-- 说明：歌手认证只允许已通过认证的音乐人申请，管理员审核后转为认证歌手
-- ============================================================
USE `kumao_music`;

ALTER TABLE `singer`
    MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0待审核/1已认证/2未通过',
    ADD COLUMN `musician_id`  BIGINT       DEFAULT NULL COMMENT '关联音乐人' AFTER `status`,
    ADD COLUMN `user_id`      BIGINT       DEFAULT NULL COMMENT '申请用户' AFTER `musician_id`,
    ADD COLUMN `reject_reason` VARCHAR(500) DEFAULT NULL AFTER `user_id`,
    ADD COLUMN `audit_time`   VARCHAR(32)  DEFAULT NULL AFTER `reject_reason`,
    ADD KEY `idx_user` (`user_id`),
    ADD UNIQUE KEY `uk_musician` (`musician_id`);
