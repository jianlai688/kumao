-- ============================================================
-- 酷猫音乐平台 AI 审核工作台
-- 说明：AI 只保存建议与最终人工结论，不改变各业务原有审核状态机
-- ============================================================
USE `kumao_music`;

CREATE TABLE IF NOT EXISTS `review_log` (
    `id`           BIGINT       NOT NULL,
    `category`     VARCHAR(40)  NOT NULL COMMENT 'ANCHOR_APPLY/MUSICIAN/ADVERTISER/AD_MATERIAL/AD_PLAN/SONG/MV/COMMENT/LIVE_ROOM/DANMAKU/SINGER',
    `item_id`      VARCHAR(64)           DEFAULT NULL,
    `item_title`   VARCHAR(300)          DEFAULT NULL,
    `decision`     VARCHAR(20)  NOT NULL COMMENT 'APPROVED/REJECTED',
    `admin_reason` VARCHAR(1000)         DEFAULT NULL,
    `ai_decision`  VARCHAR(20)           DEFAULT NULL,
    `ai_risk_score` INT                   DEFAULT NULL,
    `ai_source`    VARCHAR(30)           DEFAULT NULL COMMENT 'AI_MODEL/LOCAL_RULE',
    `ai_reasons`   VARCHAR(2000)         DEFAULT NULL,
    `admin_id`     BIGINT                DEFAULT NULL,
    `admin_name`   VARCHAR(100)          DEFAULT NULL,
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_review_category_decision` (`category`, `decision`),
    KEY `idx_review_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI辅助审核日志';
