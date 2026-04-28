package com.zzyl.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "入住申请列表VO")
public class CheckInVo {

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "老人姓名")
    private String elderName;

    @ApiModelProperty(value = "老人身份证号")
    private String elderIdCardNo;

    @ApiModelProperty(value = "入住床位")
    private String bedNumber;

    @ApiModelProperty(value = "护理等级")
    private String nursingLevelName;

    @ApiModelProperty(value = "入住开始时间")
    private LocalDateTime checkInStartTime;

    @ApiModelProperty(value = "入住结束时间")
    private LocalDateTime checkInEndTime;

    @ApiModelProperty(value = "创建人")
    private String applicat;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
