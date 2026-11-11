-- ============================================================
-- 酷猫音乐平台 MV 发布升级脚本
-- 适用场景：已按 01~09 初始化过数据库
-- 说明：音乐人和认证歌手可以在工作台提交 MV，管理员审核后上架
-- ============================================================
USE `kumao_music`;

ALTER TABLE `mv`
    ADD COLUMN `musician_id`        BIGINT       DEFAULT NULL AFTER `singer_name`,
    ADD COLUMN `uploader_user_id`  BIGINT       DEFAULT NULL AFTER `musician_id`,
    ADD COLUMN `audit_status`      VARCHAR(20)  NOT NULL DEFAULT 'APPROVED' COMMENT 'PENDING/APPROVED/REJECTED' AFTER `status`,
    ADD COLUMN `reject_reason`     VARCHAR(500) DEFAULT NULL AFTER `audit_status`,
    ADD COLUMN `audit_time`        VARCHAR(32)  DEFAULT NULL AFTER `reject_reason`,
    ADD KEY `idx_uploader` (`uploader_user_id`),
    ADD KEY `idx_audit` (`audit_status`);
