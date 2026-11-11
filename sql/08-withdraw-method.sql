-- ============================================================
-- 酷猫音乐平台 提现方式升级脚本
-- 适用场景：已按 01~07 初始化过数据库，需要将提现收款方式限制为支付宝 / 微信
-- 全新部署直接执行 01-schema.sql 即可，本脚本无需再执行
-- 说明：广告主只投流、不产生收益，ad_withdraw 已随广告收益一并移除
-- ============================================================
USE `kumao_music`;

ALTER TABLE `musician_withdraw`
    ADD COLUMN `pay_type` VARCHAR(20) DEFAULT NULL COMMENT 'ALIPAY/WECHAT' AFTER `account`;