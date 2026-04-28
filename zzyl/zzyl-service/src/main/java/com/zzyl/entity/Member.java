package com.zzyl.entity;

import com.zzyl.base.BaseEntity;
import lombok.*;

/**
* <p>
* member 实体类
* </p>
*
* @author yuhon
* @since 2024-01-08 10:20:55
*/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Member extends BaseEntity {

    /**
    * 手机号
    */
    private String phone;

    /**
    * 名称
    */
    private String name;

    /**
    * 头像
    */
    private String avatar;

    /**
    * OpenID
    */
    private String openId;

    /**
    * 性别(0:男，1:女)
    */
    private Integer gender;

}
