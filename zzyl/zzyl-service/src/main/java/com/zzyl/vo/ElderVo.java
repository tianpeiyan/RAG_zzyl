package com.zzyl.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zzyl.base.BaseVo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 老人实体类
 */
@Data
@ApiModel(description = "老人实体类")
public class ElderVo extends BaseVo {

    /**
     * 姓名
     */
    @ApiModelProperty(value = "姓名")
    private String name;

    /**
     * 头像
     */
    @ApiModelProperty(value = "头像")
    private String image;

    /**
     * 状态
     */
    @ApiModelProperty(value = "状态")
    private Integer status;

    /**
     * 身份证号
     */
    @ApiModelProperty(value = "身份证号")
    private String idCardNo;

    /**
     * 手机号
     */
    @ApiModelProperty(value = "手机号")
    private String phone;

    /**
     * 欠费金额
     */
    @ApiModelProperty(value = "欠费金额")
    private BigDecimal arrearsAmount;

    /**
     * 支付截止时间
     */
    @ApiModelProperty(value = "支付截止时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentDeadline;

    @ApiModelProperty(value = "入住开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkInStartTime;

    @ApiModelProperty(value = "入住结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkInEndTime;

    @ApiModelProperty(value = "费用开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime costStartTime;

    @ApiModelProperty(value = "费用结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime costEndTime;

    @ApiModelProperty(value = "护理等级名称")
    private String nursingLevelName;

    @ApiModelProperty(value = "床位名称")
    private String bedName;

    @ApiModelProperty(value = "护理员姓名")
    private String nursingAssistantName;
}


