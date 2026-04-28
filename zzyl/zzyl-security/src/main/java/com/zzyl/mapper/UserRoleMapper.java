package com.zzyl.mapper;

import com.zzyl.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserRoleMapper {

    int deleteByPrimaryKey(Long id);

    int insert(UserRole record);

    int insertSelective(UserRole record);

    UserRole selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserRole record);

    int updateByPrimaryKey(UserRole record);

    /**
     * 这是Mybatis Generator拓展插件生成的方法(请勿删除).
     * This method corresponds to the database table sys_user_role
     *
     * @mbg.generated
     * @author hewei
     */
    int batchInsert(@Param("list") List<UserRole> list);
    /**
     * 根据角色ID查询关联的用户数量
     * @param roleId 角色ID
     * @return 关联的用户数量
     */
    int countByRoleId(Long roleId);

    /**
     * 根据用户ID删除关联
     * @param userId
     */
    void deleteByUserId(Long userId);

    /**
     * 根据用户ID查询关联的角色ID集合
     * @param userId 用户ID
     * @return 角色ID集合
     */
    List<Long> selectRoleIdsByUserId(Long userId);

    List<UserRole> selectByUserId(Long userId);
}