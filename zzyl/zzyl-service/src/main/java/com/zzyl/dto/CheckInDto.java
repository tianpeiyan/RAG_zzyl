package com.zzyl.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(description = "入住申请DTO")
public class CheckInDto {

    @ApiModelProperty(value = "老人信息")
    private CheckInElderDto checkInElderDto;

    @ApiModelProperty(value = "家属信息列表")
    private List<ElderFamilyDto> elderFamilyDtoList;

    @ApiModelProperty(value = "合同信息")
    private CheckInContractDto checkInContractDto;

    @ApiModelProperty(value = "入住配置信息")
    private CheckInConfigDto checkInConfigDto;
}
