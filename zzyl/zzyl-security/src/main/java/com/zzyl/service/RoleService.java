package com.zzyl.service;

import com.zzyl.base.PageResponse;
import com.zzyl.dto.RoleDto;
import com.zzyl.entity.Role;
import com.zzyl.vo.RoleVo;

import java.util.List;
import java.util.Set;

public interface RoleService {

    /**
     * 分页查询角色
     * @param roleDto
     * @param pageNum
     * @param pageSize
     * @return
     */
    PageResponse<Role> findRolePage(RoleDto roleDto, int pageNum, int pageSize);

    /**
     * 查询角色列表
     * @param roleDto
     * @return
     */
    List<Role> findRoleList(RoleDto roleDto);

    /**
     * 创建角色
     * @param roleDto
     * @return
     */
    RoleVo createRole(RoleDto roleDto);

    /**
     * 修改角色
     * @param roleDto
     * @return
     */
    Boolean updateRole(RoleDto roleDto);

    /**
     * 删除角色
     * @param roleId
     * @return
     */
    Boolean deleteRole(Long roleId);

    /**
     * 查询角色拥有的资源ID
     * @param roleId
     * @return
     */
    Set<String> findCheckedResources(Long roleId);
}
