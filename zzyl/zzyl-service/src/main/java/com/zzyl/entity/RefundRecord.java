package com.zzyl.entity;

import com.zzyl.base.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 退款记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "退款记录")
public class RefundRecord extends BaseEntity {

    @ApiModelProperty(value = "交易系统订单号")
    private Long tradingOrderNo;

    @ApiModelProperty(value = "业务系统订单号")
    private Long productOrderNo;

    @ApiModelProperty(value = "本次退款订单号")
    private Long refundNo;

    @ApiModelProperty(value = "商户号")
    private Long enterpriseId;

    @ApiModelProperty(value = "退款渠道")
    private String tradingChannel;

    @ApiModelProperty(value = "退款状态：0-未知错误 1-退款中，2-成功, 3-失败")
    private Integer refundStatus;

    @ApiModelProperty(value = "返回编码")
    private String refundCode;

    @ApiModelProperty(value = "返回信息")
    private String refundMsg;

    @ApiModelProperty(value = "备注")
    private String memo;

    @ApiModelProperty(value = "本次退款金额")
    private BigDecimal refundAmount;

    @ApiModelProperty(value = "原订单金额")
    private BigDecimal total;

    @ApiModelProperty(value = "1 前台 2后台")
    private Integer createType;
}
