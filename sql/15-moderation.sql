-- ============================================================
-- 内容安全：用户禁言字段（管理 / 风控使用，AI 审核不再自动禁言）
-- 字段为 null 或早于当前时间表示未禁言
-- ============================================================
USE `kumao_music`;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'sys_user'
      AND COLUMN_NAME = 'muted_until'
);

SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE `sys_user` ADD COLUMN `muted_until` DATETIME NULL COMMENT ''禁言截止时间（管理/风控）'' AFTER `last_login_time`',
    'SELECT 1'
);

PREPARE moderation_stmt FROM @ddl;
EXECUTE moderation_stmt;
DEALLOCATE PREPARE moderation_stmt;
