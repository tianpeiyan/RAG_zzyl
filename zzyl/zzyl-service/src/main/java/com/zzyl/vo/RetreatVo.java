package com.zzyl.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "退住申请VO")
public class RetreatVo {

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "退住编号")
    private String retreatCode;

    @ApiModelProperty(value = "老人姓名")
    private String name;

    @ApiModelProperty(value = "老人身份证号")
    private String idCardNo;

    @ApiModelProperty(value = "入住床位")
    private String bedNumber;

    @ApiModelProperty(value = "入住床位(前端兼容)")
    private String bedNo;

    @ApiModelProperty(value = "护理等级名称")
    private String nursingLevelName;

    @ApiModelProperty(value = "入住开始时间")
    private LocalDateTime checkInStartTime;

    @ApiModelProperty(value = "入住结束时间")
    private LocalDateTime checkInEndTime;

    @ApiModelProperty(value = "退住时间")
    private LocalDateTime checkOutTime;

    @ApiModelProperty(value = "申请人")
    private String applicat;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "退住原因")
    private String reason;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "合同URL")
    private String contractUrl;

    @ApiModelProperty(value = "合同名称")
    private String contractName;

    @ApiModelProperty(value = "费用开始时间")
    private LocalDateTime costStartTime;

    @ApiModelProperty(value = "费用结束时间")
    private LocalDateTime costEndTime;
    
    @ApiModelProperty(value = "手机号")
    private String phone;
    
    @ApiModelProperty(value = "家庭住址")
    private String address;

    @ApiModelProperty(value = "护理员姓名")
    private String nursingName;
}
