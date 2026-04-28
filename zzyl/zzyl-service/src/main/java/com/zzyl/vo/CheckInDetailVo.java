package com.zzyl.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(description = "入住详情")
public class CheckInDetailVo {

    @ApiModelProperty(value = "老人信息")
    private CheckInElderVo checkInElderVo;

    @ApiModelProperty(value = "家属信息列表")
    private List<ElderFamilyVo> elderFamilyVoList;

    @ApiModelProperty(value = "合同信息")
    private CheckInContractVo checkInContractVo;

    @ApiModelProperty(value = "入住配置信息")
    private CheckInConfigVo checkInConfigVo;
}
