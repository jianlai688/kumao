-- ============================================================
-- 主播收款账户绑定字段
-- 适用场景：已按 01~19 初始化过数据库
-- 全新部署直接执行 01-schema.sql 即可，本脚本无需再执行
-- ============================================================
USE `kumao_music`;

ALTER TABLE `anchor_apply`
    ADD COLUMN `pay_type` VARCHAR(20) DEFAULT NULL COMMENT 'ALIPAY/WECHAT' AFTER `cover_url`,
    ADD COLUMN `pay_account` VARCHAR(200) DEFAULT NULL COMMENT '收款账号' AFTER `pay_type`,
    ADD COLUMN `pay_name` VARCHAR(64) DEFAULT NULL COMMENT '收款人姓名' AFTER `pay_account`;
