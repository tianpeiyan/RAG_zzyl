package com.zzyl.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel(description = "入住配置信息")
public class CheckInConfigVo {

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "床位费用")
    private BigDecimal bedCost;

    @ApiModelProperty(value = "床位ID")
    private Long bedId;

    @ApiModelProperty(value = "床位号")
    private String bedNumber;

    @ApiModelProperty(value = "入住开始时间")
    private LocalDateTime checkInStartTime;

    @ApiModelProperty(value = "入住结束时间")
    private LocalDateTime checkInEndTime;

    @ApiModelProperty(value = "房间编号")
    private String code;

    @ApiModelProperty(value = "房间ID")
    private Long roomId;

    @ApiModelProperty(value = "楼层ID")
    private Long floorId;

    @ApiModelProperty(value = "楼层名称")
    private String floorName;

    @ApiModelProperty(value = "费用开始时间")
    private LocalDateTime costStartTime;

    @ApiModelProperty(value = "费用结束时间")
    private LocalDateTime costEndTime;

    @ApiModelProperty(value = "押金")
    private BigDecimal depositAmount;

    @ApiModelProperty(value = "政府补贴")
    private BigDecimal governmentSubsidy;

    @ApiModelProperty(value = "医保支付")
    private BigDecimal medicalInsurancePayment;

    @ApiModelProperty(value = "护理费用")
    private BigDecimal nursingCost;

    @ApiModelProperty(value = "护理等级ID")
    private Long nursingLevelId;

    @ApiModelProperty(value = "护理等级名称")
    private String nursingLevelName;

    @ApiModelProperty(value = "其他费用")
    private BigDecimal otherCost;
}
