package com.zzyl.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单下单DTO
 */
@Data
@ApiModel(description = "订单下单参数")
public class OrderDto {

    @ApiModelProperty(value = "项目ID", required = true)
    private Long projectId;

    @ApiModelProperty(value = "老人ID", required = true)
    private Long elderId;

    @ApiModelProperty(value = "金额", required = true)
    private BigDecimal amount;

    @ApiModelProperty(value = "预计到达时间", required = true)
    private LocalDateTime estimatedArrivalTime;

    @ApiModelProperty(value = "备注")
    private String remark;
}
