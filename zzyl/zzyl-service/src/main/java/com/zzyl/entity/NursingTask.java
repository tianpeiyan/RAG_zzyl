package com.zzyl.entity;

import com.zzyl.base.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 护理任务实体类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(description = "护理任务")
public class NursingTask extends BaseEntity {

    @ApiModelProperty(value = "护理员ID")
    private Long nursingId;

    @ApiModelProperty(value = "任务类型 1:计划外 2:计划内")
    private Integer taskType;

    @ApiModelProperty(value = "任务状态 1:待执行 2:已执行 3:已取消")
    private Integer status;

    @ApiModelProperty(value = "老人ID")
    private Long elderId;

    @ApiModelProperty(value = "项目ID")
    private Long projectId;

    @ApiModelProperty(value = "床位号")
    private String bedNumber;

    @ApiModelProperty(value = "预计执行时间")
    private LocalDateTime estimatedServerTime;

    @ApiModelProperty(value = "实际执行时间")
    private LocalDateTime realServerTime;

    @ApiModelProperty(value = "执行记录")
    private String mark;

    @ApiModelProperty(value = "任务图片")
    private String taskImage;

    @ApiModelProperty(value = "取消原因")
    private String cancelReason;

    @ApiModelProperty(value = "关联单据编号")
    private String relNo;
}
