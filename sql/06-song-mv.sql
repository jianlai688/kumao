-- ============================================================
-- 歌曲与 MV 关联升级脚本
-- 适用场景：已按 01~03 初始化过数据库
-- ============================================================
USE `kumao_music`;

ALTER TABLE `mv`
    ADD COLUMN `song_id` BIGINT DEFAULT NULL COMMENT '关联歌曲ID' AFTER `id`,
    ADD KEY `idx_song` (`song_id`);

UPDATE `mv` SET `song_id` = 3001 WHERE `id` = 4001;
UPDATE `mv` SET `song_id` = 3003 WHERE `id` = 4002;
UPDATE `mv` SET `song_id` = 3002 WHERE `id` = 4003;
