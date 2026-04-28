package com.zzyl.dto;

import com.zzyl.base.BaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("客户老人关联Dto")
public class MemberElderDto extends BaseDto {

    @ApiModelProperty("老人姓名")
    private String name;

    @ApiModelProperty("老人身份证号")
    private String idCard;

}
