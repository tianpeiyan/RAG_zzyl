package com.zzyl.mapper;

import com.zzyl.dto.LoginDto;
import com.zzyl.dto.UserDto;
import com.zzyl.entity.Resource;
import com.zzyl.entity.User;
import com.zzyl.vo.UserVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {

    int deleteByPrimaryKey(Long id);

    int insert(User record);

    int insertSelective(User record);

    User selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(User record);

    int updateByPrimaryKey(User record);

    List<User> selectList(UserDto userDto);

    @Select("select * from sys_user where username = #{username}")
    UserVo selectByName(String username);

    @Select("select * from sys_user where email = #{email}")
    User selectByEmail(String email);


    /**
     * 这是Mybatis Generator拓展插件生成的方法(请勿删除).
     * This method corresponds to the database table sys_user
     *
     * @mbg.generated
     * @author hewei
     */
    int batchInsert(@Param("list") List<User> list);

    /**
     * 根据用户名查询用户
     * @param loginDto
     * @return
     */
    @Select("select * from sys_user where username = #{username}")
    User selectByUsername(LoginDto loginDto);

    /**
     * 根据用户id查询用户可访问资源
     */
    List<Resource> selectResourceById(Long id);
}