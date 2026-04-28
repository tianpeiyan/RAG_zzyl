package com.zzyl.dto;

import com.zzyl.base.BaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "护理任务DTO")
public class NursingTaskDto extends BaseDto {

    @ApiModelProperty(value = "任务ID")
    private Long id;

    @ApiModelProperty(value = "预计执行时间")
    private LocalDateTime estimatedServerTime;

    @ApiModelProperty(value = "备注")
    private String mark;

    @ApiModelProperty(value = "任务图片")
    private String taskImage;

    @ApiModelProperty(value = "取消原因")
    private String reason;
}
