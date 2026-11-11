package com.kumao.review.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_model_config")
public class AiModelConfig extends BaseEntity {

    /** REVIEW / TEXT / VISION */
    private String modelType;
    /** deepseek / dashscope / zhipu / moonshot / openai / custom */
    private String provider;
    private String modelName;
    private String baseUrl;
    /** AES-GCM 密文，enc:v1: 前缀；兼容历史明文 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String apiKey;
    private BigDecimal temperature;
    private Integer timeoutSeconds;
    private Integer maxRetries;
    private Boolean enabled;
    private String remark;
    private Long updateBy;
    private String updateByName;
}
