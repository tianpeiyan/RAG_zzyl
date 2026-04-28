package com.zzyl.mapper;

import com.zzyl.dto.RoleDto;
import com.zzyl.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Role record);

    int insertSelective(Role record);

    Role selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Role record);

    int updateByPrimaryKey(Role record);

    /**
     * 这是Mybatis Generator拓展插件生成的方法(请勿删除).
     * This method corresponds to the database table sys_role
     *
     * @mbg.generated
     * @author hewei
     */
    int batchInsert(@Param("list") List<Role> list);

    /**
     * 多条件查询角色列表
     * @param roleDto
     * @return
     */
    List<Role> selectList(RoleDto roleDto);
}