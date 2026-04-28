package com.zzyl.vo;

import com.zzyl.base.BaseVo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(description = "护理任务VO")
public class NursingTaskVo extends BaseVo {

    @ApiModelProperty(value = "护理员ID")
    private Long nursingId;

    @ApiModelProperty(value = "任务类型 1:计划外 2:计划内")
    private Integer taskType;

    @ApiModelProperty(value = "任务状态 1:待执行 2:已执行 3:已取消")
    private Integer status;

    @ApiModelProperty(value = "老人ID")
    private Long elderId;

    @ApiModelProperty(value = "老人姓名")
    private String elderName;

    @ApiModelProperty(value = "老人身份证号")
    private String idCardNo;

    @ApiModelProperty(value = "老人头像")
    private String image;

    @ApiModelProperty(value = "性别")
    private String sex;

    @ApiModelProperty(value = "护理等级")
    private String lname;

    @ApiModelProperty(value = "护理员姓名列表")
    private List<String> nursingName;

    @ApiModelProperty(value = "床位号")
    private String bedNumber;

    @ApiModelProperty(value = "项目ID")
    private Long projectId;

    @ApiModelProperty(value = "项目名称")
    private String projectName;

    @ApiModelProperty(value = "预计执行时间")
    private LocalDateTime estimatedServerTime;

    @ApiModelProperty(value = "实际执行时间")
    private LocalDateTime realServerTime;

    @ApiModelProperty(value = "任务图片")
    private String taskImage;

    @ApiModelProperty(value = "取消原因")
    private String cancelReason;

    @ApiModelProperty(value = "关联单据编号")
    private String relNo;

    @ApiModelProperty(value = "备注")
    private String mark;
}
