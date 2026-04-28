package com.zzyl.mapper;

import com.zzyl.entity.RoleResource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleResourceMapper {

    int deleteByPrimaryKey(Long id);

    int insert(RoleResource record);

    int insertSelective(RoleResource record);

    RoleResource selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(RoleResource record);

    int updateByPrimaryKey(RoleResource record);

    /**
     * 这是Mybatis Generator拓展插件生成的方法(请勿删除).
     * This method corresponds to the database table sys_role_resource
     *
     * @mbg.generated
     * @author hewei
     */
    int batchInsert(@Param("list") List<RoleResource> list);

    /**
     * 根据角色ID删除角色资源关联
     * @param roleId
     * @return
     */
    int deleteByRoleId(Long roleId);

    /**
     * 根据角色ID查询资源编号列表
     * @param roleId
     * @return
     */
    List<String> selectResourceNosByRoleId(Long roleId);
}