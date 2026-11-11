-- ============================================================
-- 酷猫音乐平台 广告投放计费升级脚本
-- 适用场景：已按 01~10 初始化过数据库
-- 说明：广告计划按「版位日单价 × 投放天数」计费，支付成功后才可审核投放
-- ============================================================
USE `kumao_music`;

ALTER TABLE `ad_plan`
    ADD COLUMN `days`             INT           NOT NULL DEFAULT 1 AFTER `target_audience`,
    ADD COLUMN `daily_price`      DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER `days`,
    ADD COLUMN `amount`           DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER `daily_price`,
    ADD COLUMN `payment_status`   VARCHAR(20)   NOT NULL DEFAULT 'PAID' COMMENT 'UNPAID/PAID' AFTER `amount`,
    ADD COLUMN `payment_order_no` VARCHAR(64)            DEFAULT NULL AFTER `payment_status`,
    ADD COLUMN `paid_time`        DATETIME               DEFAULT NULL AFTER `payment_order_no`;
