package com.zzyl.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "退住申请DTO")
public class RetreatApplyDto {

    @ApiModelProperty(value = "老人ID")
    private Long elderId;

    @ApiModelProperty(value = "退住时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkOutTime;

    @ApiModelProperty(value = "退住原因")
    private String reason;

    @ApiModelProperty(value = "备注")
    private String remark;
}
