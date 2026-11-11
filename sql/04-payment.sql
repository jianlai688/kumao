-- ============================================================
-- 酷猫音乐平台 支付功能升级脚本
-- 适用场景：已按 01~03 初始化过数据库，需要补充支付相关字段
-- 说明：全新部署直接执行 01-schema.sql 即可，本脚本无需再执行
-- ============================================================
USE `kumao_music`;

ALTER TABLE `vip_order`
    MODIFY COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID/PAID/CLOSED',
    ADD COLUMN `transaction_id`  VARCHAR(64)  DEFAULT NULL COMMENT '第三方交易号' AFTER `pay_type`,
    ADD COLUMN `pay_expire_time` DATETIME     DEFAULT NULL COMMENT '支付超时时间' AFTER `pay_time`,
    ADD COLUMN `notify_time`     DATETIME     DEFAULT NULL COMMENT '回调到达时间' AFTER `pay_expire_time`,
    ADD COLUMN `remark`          VARCHAR(255) DEFAULT NULL AFTER `expire_time`,
    ADD KEY `idx_status_expire` (`status`, `pay_expire_time`);

-- 支付回调原始报文留痕，便于对账与排障
CREATE TABLE IF NOT EXISTS `pay_notify_log` (
    `id`             BIGINT      NOT NULL,
    `channel`        VARCHAR(20) NOT NULL COMMENT 'ALIPAY/WECHAT',
    `order_no`       VARCHAR(64)          DEFAULT NULL,
    `transaction_id` VARCHAR(64)          DEFAULT NULL,
    `success`        TINYINT     NOT NULL DEFAULT 0,
    `message`        VARCHAR(255)         DEFAULT NULL,
    `raw_body`       TEXT,
    `create_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_no`),
    KEY `idx_create` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '支付回调日志';

-- 酷猫币充值：套餐补充币数字段，并记录支付成功后的入账流水
ALTER TABLE `vip_package`
    ADD COLUMN `coin_amount` INT DEFAULT NULL COMMENT 'COIN类型套餐包含的酷猫币数量' AFTER `days`;

CREATE TABLE IF NOT EXISTS `live_coin_recharge` (
    `id`            BIGINT       NOT NULL,
    `user_id`       BIGINT       NOT NULL,
    `order_no`      VARCHAR(64)  NOT NULL,
    `amount`        INT          NOT NULL DEFAULT 0,
    `balance_after` INT          NOT NULL DEFAULT 0,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '酷猫币充值入账记录';

INSERT INTO `vip_package` (`id`, `name`, `type`, `days`, `coin_amount`, `price`, `original_price`, `description`, `status`, `sort`)
VALUES
(12004, '100 酷猫币', 'COIN', 0, 100, 10.00, NULL, '直播送礼，即刻到账', 1, 10),
(12005, '500 酷猫币', 'COIN', 0, 500, 50.00, NULL, '直播送礼，即刻到账', 1, 11),
(12006, '1000 酷猫币', 'COIN', 0, 1000, 100.00, NULL, '直播送礼，即刻到账', 1, 12),
(12007, '2000 酷猫币', 'COIN', 0, 2000, 200.00, NULL, '直播送礼，即刻到账', 1, 13)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `coin_amount` = VALUES(`coin_amount`), `price` = VALUES(`price`), `description` = VALUES(`description`), `status` = VALUES(`status`), `sort` = VALUES(`sort`);
