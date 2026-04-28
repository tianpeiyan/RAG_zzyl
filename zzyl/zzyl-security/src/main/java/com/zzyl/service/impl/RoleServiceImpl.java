package com.zzyl.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zzyl.base.PageResponse;
import com.zzyl.constant.SuperConstant;
import com.zzyl.dto.RoleDto;
import com.zzyl.entity.Role;
import com.zzyl.entity.RoleResource;
import com.zzyl.mapper.RoleMapper;
import com.zzyl.mapper.RoleResourceMapper;
import com.zzyl.mapper.UserRoleMapper;
import com.zzyl.service.RoleService;
import com.zzyl.vo.RoleVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RoleResourceMapper roleResourceMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    /**
     * 分页查询角色
     * @param roleDto
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageResponse<Role> findRolePage(RoleDto roleDto, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Role> roleList = roleMapper.selectList(roleDto);
        Page<Role> page = (Page<Role>) roleList;
        return PageResponse.of(page, Role.class);
    }

    /**
     * 查询角色列表
     * @param roleDto
     * @return
     */
    @Override
    public List<Role> findRoleList(RoleDto roleDto) {
        return roleMapper.selectList(roleDto);
    }

    /**
     * 创建角色
     * @param roleDto
     * @return
     */
    @Transactional
    @Override
    public RoleVo createRole(RoleDto roleDto) {
        Role role = BeanUtil.toBean(roleDto, Role.class);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.insert(role);

        // 保存角色资源关联
        saveRoleResources(role.getId(), roleDto.getCheckedResourceNos());

        return BeanUtil.toBean(role, RoleVo.class);
    }

    /**
     * 修改角色
     * @param roleDto
     * @return
     */
    @Transactional
    @Override
    public Boolean updateRole(RoleDto roleDto) {
        Role role = BeanUtil.toBean(roleDto, Role.class);
        role.setUpdateTime(LocalDateTime.now());
        int count = roleMapper.updateByPrimaryKeySelective(role);
        if (count == 0) {
            throw new RuntimeException("修改角色失败");
        }

        // 如果传了 checkedResourceNos，则更新资源关联
        if (roleDto.getCheckedResourceNos() != null) {
            // 先删除旧的关联
            roleResourceMapper.deleteByRoleId(role.getId());
            // 保存新的关联
            saveRoleResources(role.getId(), roleDto.getCheckedResourceNos());
        }

        return true;
    }

    /**
     * 删除角色
     * @param roleId
     * @return
     */
    @Transactional
    @Override
    public Boolean deleteRole(Long roleId) {
        // 检查是否有用户关联此角色
        int userCount = userRoleMapper.countByRoleId(roleId);
        if (userCount > 0) {
            throw new RuntimeException("当前角色被用户关联，无法删除");
        }

        // 删除角色资源关联
        roleResourceMapper.deleteByRoleId(roleId);
        // 删除角色
        int count = roleMapper.deleteByPrimaryKey(roleId);
        if (count == 0) {
            throw new RuntimeException("删除角色失败");
        }
        return true;
    }

    /**
     * 查询角色拥有的资源ID
     * @param roleId
     * @return
     */
    @Override
    public Set<String> findCheckedResources(Long roleId) {
        List<String> resourceNos = roleResourceMapper.selectResourceNosByRoleId(roleId);
        return new HashSet<>(resourceNos);
    }

    private void saveRoleResources(Long roleId, String[] resourceNos) {
        if (resourceNos != null && resourceNos.length > 0) {
            List<RoleResource> list = new ArrayList<>();
            for (String resourceNo : resourceNos) {
                RoleResource roleResource = new RoleResource();
                roleResource.setRoleId(roleId);
                roleResource.setResourceNo(resourceNo);
                roleResource.setDataState(SuperConstant.DATA_STATE_0);
                roleResource.setCreateTime(LocalDateTime.now());
                roleResource.setUpdateTime(LocalDateTime.now());
                list.add(roleResource);
            }
            roleResourceMapper.batchInsert(list);
        }
    }
}
