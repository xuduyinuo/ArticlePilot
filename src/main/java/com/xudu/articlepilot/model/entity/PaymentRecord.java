package com.xudu.articlepilot.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录表
 * @TableName payment_record
 */
@TableName(value ="payment_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecord implements Serializable {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField(value = "userId")
    private Long userId;

    /**
     * Stripe Checkout Session ID
     */
    @TableField(value = "stripeSessionId")
    private String stripeSessionId;

    /**
     * Stripe 支付意向ID
     */
    @TableField(value = "stripePaymentIntentId")
    private String stripePaymentIntentId;

    /**
     * 金额（美元）
     */
    @TableField(value = "amount")
    private BigDecimal amount;

    /**
     * 货币
     */
    @TableField(value = "currency")
    private String currency;

    /**
     * 状态：PENDING/SUCCEEDED/FAILED/REFUNDED
     */
    @TableField(value = "status")
    private String status;

    /**
     * 产品类型：VIP_PERMANENT
     */
    @TableField(value = "productType")
    private String productType;

    /**
     * 描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 退款时间
     */
    @TableField(value = "refundTime")
    private LocalDateTime refundTime;

    /**
     * 退款原因
     */
    @TableField(value = "refundReason")
    private String refundReason;

    /**
     * 创建时间
     */
    @TableField(value = "createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "updateTime")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}