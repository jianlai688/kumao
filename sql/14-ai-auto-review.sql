-- ============================================================
-- 酷猫音乐平台 AI 自动审核任务
-- 说明：调度器自动拉取待审内容，AI 通过的低风险内容自动处理；
--       AI 未通过或需要人工确认的项目进入管理员复核队列。
-- ============================================================
USE `kumao_music`;

CREATE TABLE IF NOT EXISTS `ai_review_task` (
    `id`             BIGINT       NOT NULL,
    `category`       VARCHAR(40)  NOT NULL COMMENT 'ANCHOR_APPLY/MUSICIAN/ADVERTISER/AD_MATERIAL/AD_PLAN/SONG/MV/COMMENT/LIVE_ROOM/DANMAKU/SINGER',
    `item_id`        VARCHAR(64)  NOT NULL,
    `item_title`     VARCHAR(300)          DEFAULT NULL,
    `item_content`   VARCHAR(1000)         DEFAULT NULL,
    `decision`       VARCHAR(20)           DEFAULT NULL COMMENT 'PASS/REVIEW/REJECT',
    `risk_score`     INT                   DEFAULT NULL,
    `reasons`        VARCHAR(2000)         DEFAULT NULL,
    `signals`        VARCHAR(2000)         DEFAULT NULL COMMENT 'JSON 数组',
    `source`         VARCHAR(30)           DEFAULT NULL COMMENT 'AI_MODEL/LOCAL_RULE',
    `provider`       VARCHAR(40)           DEFAULT NULL,
    `model`          VARCHAR(120)          DEFAULT NULL,
    `status`         VARCHAR(30)  NOT NULL DEFAULT 'PENDING_ADMIN' COMMENT 'PENDING_ADMIN/AUTO_APPROVED/APPROVAL_FAILED/RESOLVED',
    `approval_error` VARCHAR(500)          DEFAULT NULL,
    `attempts`       INT          NOT NULL DEFAULT 1,
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_review_item` (`category`, `item_id`),
    KEY `idx_ai_review_status` (`status`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 自动审核任务';
