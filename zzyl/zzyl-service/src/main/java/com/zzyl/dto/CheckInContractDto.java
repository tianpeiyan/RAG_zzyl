package com.zzyl.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "入住合同信息")
public class CheckInContractDto {

    @ApiModelProperty(value = "合同名称")
    private String name;

    @ApiModelProperty(value = "签约日期")
    private LocalDateTime signDate;

    @ApiModelProperty(value = "丙方名称")
    private String memberName;

    @ApiModelProperty(value = "丙方手机号")
    private String memberPhone;

    @ApiModelProperty(value = "合同PDF链接")
    private String pdfUrl;
}
