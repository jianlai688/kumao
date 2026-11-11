-- ============================================================
-- 酷猫音乐平台 AI 模型配置
-- 三类能力分别指定模型：REVIEW 审核 / TEXT 文本 / VISION 识别图片
-- API Key 由 review-service 加密后存储，接口只返回掩码
-- ============================================================
USE `kumao_music`;

CREATE TABLE IF NOT EXISTS `ai_model_config` (
    `id`             BIGINT       NOT NULL,
    `model_type`     VARCHAR(20)  NOT NULL COMMENT 'REVIEW/TEXT/VISION',
    `provider`       VARCHAR(40)  NOT NULL DEFAULT 'deepseek' COMMENT 'deepseek/dashscope/zhipu/moonshot/openai/custom',
    `model_name`     VARCHAR(120)          DEFAULT NULL,
    `base_url`       VARCHAR(255)          DEFAULT NULL COMMENT 'OpenAI 兼容接口根地址',
    `api_key`        VARCHAR(1000)         DEFAULT NULL COMMENT 'AES-GCM 密文，enc:v1: 前缀',
    `temperature`    DECIMAL(3, 2) NOT NULL DEFAULT 0.10,
    `timeout_seconds` INT          NOT NULL DEFAULT 30,
    `max_retries`    INT          NOT NULL DEFAULT 2,
    `enabled`        TINYINT      NOT NULL DEFAULT 0,
    `remark`         VARCHAR(255)          DEFAULT NULL,
    `update_by`      BIGINT                DEFAULT NULL,
    `update_by_name` VARCHAR(100)          DEFAULT NULL,
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_model_type` (`model_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 模型配置';

INSERT INTO `ai_model_config`
(`id`, `model_type`, `provider`, `model_name`, `base_url`, `api_key`, `temperature`,
 `timeout_seconds`, `max_retries`, `enabled`, `remark`)
SELECT 1, 'REVIEW', 'deepseek', 'deepseek-chat', 'https://api.deepseek.com', NULL, 0.10, 30, 2, 0,
       '内容与资质审核建议'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ai_model_config` WHERE `model_type` = 'REVIEW');

INSERT INTO `ai_model_config`
(`id`, `model_type`, `provider`, `model_name`, `base_url`, `api_key`, `temperature`,
 `timeout_seconds`, `max_retries`, `enabled`, `remark`)
SELECT 2, 'TEXT', 'deepseek', 'deepseek-chat', 'https://api.deepseek.com', NULL, 0.30, 30, 2, 0,
       '标题、简介、文案等文本生成'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ai_model_config` WHERE `model_type` = 'TEXT');

INSERT INTO `ai_model_config`
(`id`, `model_type`, `provider`, `model_name`, `base_url`, `api_key`, `temperature`,
 `timeout_seconds`, `max_retries`, `enabled`, `remark`)
SELECT 3, 'VISION', 'dashscope', 'qwen-vl-max', 'https://dashscope.aliyuncs.com/compatible-mode/v1', NULL, 0.10, 30, 2, 0,
       '封面、广告素材、证件图片识别'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ai_model_config` WHERE `model_type` = 'VISION');
