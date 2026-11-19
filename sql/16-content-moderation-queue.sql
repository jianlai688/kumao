-- ============================================================
-- 评论 / 直播弹幕 AI 自动审核与人工复核队列
-- 状态：0 已通过并公开展示；1 已驳回/屏蔽；2 AI 不建议通过，等待管理员复核
-- 说明：本脚本可重复执行。
-- ============================================================
USE `kumao_music`;

ALTER TABLE `song_comment`
    MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0
        COMMENT '0 已通过 1 已驳回/屏蔽 2 待管理员复核';

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'song_comment'
      AND COLUMN_NAME = 'ai_decision'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE `song_comment` ADD COLUMN `ai_decision` VARCHAR(20) DEFAULT NULL COMMENT ''PASS/REVIEW/REJECT'' AFTER `status`',
    'SELECT 1'
);
PREPARE moderation_stmt FROM @ddl;
EXECUTE moderation_stmt;
DEALLOCATE PREPARE moderation_stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'song_comment'
      AND COLUMN_NAME = 'ai_risk_score'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE `song_comment` ADD COLUMN `ai_risk_score` INT DEFAULT NULL COMMENT ''AI 风险分 0-100'' AFTER `ai_decision`',
    'SELECT 1'
);
PREPARE moderation_stmt FROM @ddl;
EXECUTE moderation_stmt;
DEALLOCATE PREPARE moderation_stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'song_comment'
      AND COLUMN_NAME = 'ai_source'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE `song_comment` ADD COLUMN `ai_source` VARCHAR(30) DEFAULT NULL COMMENT ''AI_MODEL/LOCAL_RULE'' AFTER `ai_risk_score`',
    'SELECT 1'
);
PREPARE moderation_stmt FROM @ddl;
EXECUTE moderation_stmt;
DEALLOCATE PREPARE moderation_stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'song_comment'
      AND COLUMN_NAME = 'ai_reasons'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE `song_comment` ADD COLUMN `ai_reasons` VARCHAR(1000) DEFAULT NULL COMMENT ''AI 审核依据'' AFTER `ai_source`',
    'SELECT 1'
);
PREPARE moderation_stmt FROM @ddl;
EXECUTE moderation_stmt;
DEALLOCATE PREPARE moderation_stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'live_danmaku'
      AND COLUMN_NAME = 'status'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE `live_danmaku` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT ''0 已通过 1 已驳回/屏蔽 2 待管理员复核'' AFTER `type`',
    'SELECT 1'
);
PREPARE moderation_stmt FROM @ddl;
EXECUTE moderation_stmt;
DEALLOCATE PREPARE moderation_stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'live_danmaku'
      AND COLUMN_NAME = 'ai_decision'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE `live_danmaku` ADD COLUMN `ai_decision` VARCHAR(20) DEFAULT NULL COMMENT ''PASS/REVIEW/REJECT'' AFTER `status`',
    'SELECT 1'
);
PREPARE moderation_stmt FROM @ddl;
EXECUTE moderation_stmt;
DEALLOCATE PREPARE moderation_stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'live_danmaku'
      AND COLUMN_NAME = 'ai_risk_score'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE `live_danmaku` ADD COLUMN `ai_risk_score` INT DEFAULT NULL COMMENT ''AI 风险分 0-100'' AFTER `ai_decision`',
    'SELECT 1'
);
PREPARE moderation_stmt FROM @ddl;
EXECUTE moderation_stmt;
DEALLOCATE PREPARE moderation_stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'live_danmaku'
      AND COLUMN_NAME = 'ai_source'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE `live_danmaku` ADD COLUMN `ai_source` VARCHAR(30) DEFAULT NULL COMMENT ''AI_MODEL/LOCAL_RULE'' AFTER `ai_risk_score`',
    'SELECT 1'
);
PREPARE moderation_stmt FROM @ddl;
EXECUTE moderation_stmt;
DEALLOCATE PREPARE moderation_stmt;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'live_danmaku'
      AND COLUMN_NAME = 'ai_reasons'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE `live_danmaku` ADD COLUMN `ai_reasons` VARCHAR(1000) DEFAULT NULL COMMENT ''AI 审核依据'' AFTER `ai_source`',
    'SELECT 1'
);
PREPARE moderation_stmt FROM @ddl;
EXECUTE moderation_stmt;
DEALLOCATE PREPARE moderation_stmt;

SET @index_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'song_comment'
      AND INDEX_NAME = 'idx_status_time'
);
SET @ddl = IF(
    @index_exists = 0,
    'ALTER TABLE `song_comment` ADD INDEX `idx_status_time` (`status`, `create_time`)',
    'SELECT 1'
);
PREPARE moderation_stmt FROM @ddl;
EXECUTE moderation_stmt;
DEALLOCATE PREPARE moderation_stmt;

SET @index_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'kumao_music'
      AND TABLE_NAME = 'live_danmaku'
      AND INDEX_NAME = 'idx_status_time'
);
SET @ddl = IF(
    @index_exists = 0,
    'ALTER TABLE `live_danmaku` ADD INDEX `idx_status_time` (`status`, `create_time`)',
    'SELECT 1'
);
PREPARE moderation_stmt FROM @ddl;
EXECUTE moderation_stmt;
DEALLOCATE PREPARE moderation_stmt;
