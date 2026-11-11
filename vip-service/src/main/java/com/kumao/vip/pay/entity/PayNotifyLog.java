package com.kumao.vip.pay.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付回调日志
 */
@Data
@TableName("pay_notify_log")
public class PayNotifyLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String channel;
    private String orderNo;
    private String transactionId;
    private Integer success;
    private String message;
    private String rawBody;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
