package com.zzyl.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * @author itheima
 */
@Data
@ApiModel("用户角色VO")
public class UserRoleVo {

    /**
     * 用户真名
     */
    private String userName;
    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 用户id
     */
    private Long id;
}
