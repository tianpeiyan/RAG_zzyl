package com.zzyl.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 下单前校验参数
 */
@Data
@ApiModel(description = "下单前校验参数")
public class OrderCheckDto {

    @ApiModelProperty(value = "老人ID", required = true)
    private Long elderId;

    @ApiModelProperty(value = "老人姓名（前端展示用）")
    private String name;
}

