package com.zzyl.dto;

import com.zzyl.base.BaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@ApiModel("修改密码DTO")
public class PasswordDto extends BaseDto {

    @ApiModelProperty(value = "新密码")
    private String pw;

    @ApiModelProperty(value = "旧密码")
    private String oldPw;



}