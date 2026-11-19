-- ============================================================
-- 主播 / 音乐人 / 广告商实名认证：身份证号及正反面照片
-- 说明：本脚本可重复执行。
-- ============================================================
USE `kumao_music`;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'anchor_apply'
      AND COLUMN_NAME = 'id_card_front_url'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE `anchor_apply` ADD COLUMN `id_card_front_url` VARCHAR(800) DEFAULT NULL COMMENT ''身份证人像面照片'' AFTER `id_card`',
    'SELECT 1');
PREPARE id_stmt FROM @ddl;
EXECUTE id_stmt;
DEALLOCATE PREPARE id_stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'anchor_apply'
      AND COLUMN_NAME = 'id_card_back_url'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE `anchor_apply` ADD COLUMN `id_card_back_url` VARCHAR(800) DEFAULT NULL COMMENT ''身份证国徽面照片'' AFTER `id_card_front_url`',
    'SELECT 1');
PREPARE id_stmt FROM @ddl;
EXECUTE id_stmt;
DEALLOCATE PREPARE id_stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'musician'
      AND COLUMN_NAME = 'id_card'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE `musician` ADD COLUMN `id_card` VARCHAR(64) DEFAULT NULL AFTER `real_name`',
    'SELECT 1');
PREPARE id_stmt FROM @ddl;
EXECUTE id_stmt;
DEALLOCATE PREPARE id_stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'musician'
      AND COLUMN_NAME = 'id_card_front_url'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE `musician` ADD COLUMN `id_card_front_url` VARCHAR(800) DEFAULT NULL COMMENT ''身份证人像面照片'' AFTER `id_card`',
    'SELECT 1');
PREPARE id_stmt FROM @ddl;
EXECUTE id_stmt;
DEALLOCATE PREPARE id_stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'musician'
      AND COLUMN_NAME = 'id_card_back_url'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE `musician` ADD COLUMN `id_card_back_url` VARCHAR(800) DEFAULT NULL COMMENT ''身份证国徽面照片'' AFTER `id_card_front_url`',
    'SELECT 1');
PREPARE id_stmt FROM @ddl;
EXECUTE id_stmt;
DEALLOCATE PREPARE id_stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'advertiser'
      AND COLUMN_NAME = 'id_card'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE `advertiser` ADD COLUMN `id_card` VARCHAR(64) DEFAULT NULL AFTER `real_name`',
    'SELECT 1');
PREPARE id_stmt FROM @ddl;
EXECUTE id_stmt;
DEALLOCATE PREPARE id_stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'advertiser'
      AND COLUMN_NAME = 'id_card_front_url'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE `advertiser` ADD COLUMN `id_card_front_url` VARCHAR(800) DEFAULT NULL COMMENT ''身份证人像面照片'' AFTER `id_card`',
    'SELECT 1');
PREPARE id_stmt FROM @ddl;
EXECUTE id_stmt;
DEALLOCATE PREPARE id_stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'advertiser'
      AND COLUMN_NAME = 'id_card_back_url'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE `advertiser` ADD COLUMN `id_card_back_url` VARCHAR(800) DEFAULT NULL COMMENT ''身份证国徽面照片'' AFTER `id_card_front_url`',
    'SELECT 1');
PREPARE id_stmt FROM @ddl;
EXECUTE id_stmt;
DEALLOCATE PREPARE id_stmt;
