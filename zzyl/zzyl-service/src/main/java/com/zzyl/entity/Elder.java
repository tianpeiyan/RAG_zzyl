package com.zzyl.entity;

import com.zzyl.base.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 老人实体类
 */
@Data
public class Elder extends BaseEntity {

    /**
     * 姓名
     */
    private String name;

    /**
     * 头像
     */
    private String image;

    /**
     * 状态（0：禁用，1:启用  2:请假 3:退住中 4入住中 5已退住）
     */
    private Integer status;

    /**
     * 身份证号
     */
    private String idCardNo;

    /**
     * 手机号
     */
    private String phone;

    @ApiModelProperty(value = "性别")
    private String sex;

    /**
     * 身份证国徽面
     */
    private String idCardNationalEmblemImg;

    /**
     * 身份证人像面
     */
    private String idCardPortraitImg;

    /**
     * 家庭住址
     */
    private String address;

    /**
     * 出生日期
     */
    private String birthday;

    /**
     * 床位编号
     */
    private String bedNumber;

    /**
     * 床位ID
     */
    private Long bedId;


}

