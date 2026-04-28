package com.zzyl.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "入住老人信息")
public class CheckInElderVo {

    @ApiModelProperty(value = "一寸照片")
    private String oneInchPhoto;

    @ApiModelProperty(value = "身份证人像面")
    private String idCardPortraitImg;

    @ApiModelProperty(value = "身份证国徽面")
    private String idCardNationalEmblemImg;

    @ApiModelProperty(value = "家庭住址")
    private String address;

    @ApiModelProperty(value = "出生日期")
    private String birthday;

    @ApiModelProperty(value = "身份证号")
    private String idCardNo;

    @ApiModelProperty(value = "姓名")
    private String name;

    @ApiModelProperty(value = "手机号")
    private String phone;

    @ApiModelProperty(value = "性别")
    private String sex;
}
