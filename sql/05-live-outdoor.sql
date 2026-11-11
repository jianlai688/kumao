-- ============================================================
-- 酷猫音乐平台 户外直播升级脚本
-- 适用场景：已按 01~03 初始化过数据库
-- 说明：全新部署直接执行 01-schema.sql 即可，本脚本无需再执行
-- ============================================================
USE `kumao_music`;

ALTER TABLE `live_room`
    ADD COLUMN `scene`    VARCHAR(20)  NOT NULL DEFAULT 'INDOOR' COMMENT 'INDOOR 室内 / OUTDOOR 户外' AFTER `category`,
    ADD COLUMN `location` VARCHAR(200)          DEFAULT NULL COMMENT '户外直播地点' AFTER `scene`,
    ADD KEY `idx_scene` (`scene`);
