package com.zzyl.service;

import com.zzyl.base.PageResponse;
import com.zzyl.dto.UserDto;
import com.zzyl.vo.UserVo;

import java.util.List;
import java.util.Set;

/**
 * 用户业务接口
 */
public interface UserService {

    /**
     * 分页查询用户列表
     * @param userDto 查询条件
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResponse<UserVo> findUserPage(UserDto userDto, int pageNum, int pageSize);

    /**
     * 新增用户
     * @param userDto 用户信息
     * @return 成功/失败
     */
    Boolean addUser(UserDto userDto);

    /**
     * 修改用户
     * @param userDto 用户信息
     * @return 成功/失败
     */
    Boolean updateUser(UserDto userDto);

    /**
     * 删除用户
     * @param userIds 用户ID集合
     * @return 成功/失败
     */
    Boolean deleteUser(Set<Long> userIds);

    /**
     * 启用/禁用用户
     * @param id 用户ID
     * @param dataState 状态
     * @return 成功/失败
     */
    Boolean enableUser(Long id, String dataState);

    /**
     * 重置密码
     * @param userId 用户ID
     * @return 成功/失败
     */
    Boolean resetPassword(Long userId);

    /**
     * 查询所有用户
     * @param userDto 查询条件
     * @return 用户列表
     */
    List<UserVo> findUserList(UserDto userDto);

    /**
     * 获取当前登录用户信息
     * @return 用户信息
     */
    UserVo getCurrentUser();
}
