package com.zzyl.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zzyl.base.PageResponse;
import com.zzyl.constant.SuperConstant;
import com.zzyl.dto.UserDto;
import com.zzyl.entity.User;
import com.zzyl.entity.UserRole;
import com.zzyl.entity.Role;
import com.zzyl.mapper.UserMapper;
import com.zzyl.mapper.UserRoleMapper;
import com.zzyl.mapper.RoleMapper;
import com.zzyl.service.UserService;
import com.zzyl.utils.NoProcessing;
import com.zzyl.utils.UserThreadLocal;
import com.zzyl.vo.UserVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RoleMapper roleMapper;



    @Override
    public PageResponse<UserVo> findUserPage(UserDto userDto, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<User> userList = userMapper.selectList(userDto);
        Page<User> page = (Page<User>) userList;
        PageResponse<UserVo> pageResponse = PageResponse.of(page, UserVo.class);
        fillUserRoles(pageResponse.getRecords());
        return pageResponse;
    }

    private void fillUserRoles(List<UserVo> userVos) {
        if (CollUtil.isEmpty(userVos)) {
            return;
        }
        for (UserVo userVo : userVos) {
            List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userVo.getId());
            if (CollUtil.isNotEmpty(roleIds)) {
                Set<String> roleVoIds = roleIds.stream().map(String::valueOf).collect(Collectors.toSet());
                userVo.setRoleVoIds(roleVoIds);

                Set<String> roleLabels = new HashSet<>();
                for (Long roleId : roleIds) {
                    Role role = roleMapper.selectByPrimaryKey(roleId);
                    if (role != null) {
                        roleLabels.add(role.getRoleName());
                    }
                }
                userVo.setRoleLabels(roleLabels);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean addUser(UserDto userDto) {
        // Check if username exists
        UserVo existingUser = userMapper.selectByName(userDto.getUsername());
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // Check if email exists
        if (ObjectUtil.isNotEmpty(userDto.getEmail())) {
            User existingEmail = userMapper.selectByEmail(userDto.getEmail());
            if (existingEmail != null) {
                throw new RuntimeException("邮箱已存在");
            }
        }

        User user = BeanUtil.toBean(userDto, User.class);
        // Set default password
        user.setPassword(BCrypt.hashpw("888itcast.CN764%", BCrypt.gensalt()));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setIsDelete(0); // Not deleted
        user.setDataState(SuperConstant.DATA_STATE_0);

        userMapper.insert(user);

        // Assign roles
        assignRoles(user.getId(), userDto.getRoleVoIds());

        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateUser(UserDto userDto) {
        User user = userMapper.selectByPrimaryKey(userDto.getId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // Check if email is being changed and if it exists
        if (ObjectUtil.isNotEmpty(userDto.getEmail()) && !userDto.getEmail().equals(user.getEmail())) {
            User existingEmail = userMapper.selectByEmail(userDto.getEmail());
            if (existingEmail != null) {
                throw new RuntimeException("邮箱已存在");
            }
        }

        BeanUtil.copyProperties(userDto, user, "password", "username", "createTime");
        user.setUpdateTime(LocalDateTime.now());
        
        userMapper.updateByPrimaryKeySelective(user);

        // Update roles
        if (userDto.getRoleVoIds() != null) {
            userRoleMapper.deleteByUserId(user.getId());
            assignRoles(user.getId(), userDto.getRoleVoIds());
        }

        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deleteUser(Set<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return false;
        }
        for (Long userId : userIds) {
            User user = userMapper.selectByPrimaryKey(userId);
            if (user != null && "0".equals(user.getDataState())) {
                throw new RuntimeException("用户 " + user.getUsername() + " 处于启用状态，无法删除");
            }
            userMapper.deleteByPrimaryKey(userId);
            userRoleMapper.deleteByUserId(userId);
        }
        return true;
    }

    @Override
    public Boolean enableUser(Long id, String dataState) {
        User user = userMapper.selectByPrimaryKey(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setDataState(dataState);
        user.setUpdateTime(LocalDateTime.now());
        return userMapper.updateByPrimaryKeySelective(user) > 0;
    }

    @Override
    public UserVo getCurrentUser() {
        Long userId = UserThreadLocal.getMgtUserId();
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        UserVo userVo = BeanUtil.toBean(user, UserVo.class);

        // Populate roleVoIds
        List<UserRole> userRoles = userRoleMapper.selectByUserId(userId);
        if (CollUtil.isNotEmpty(userRoles)) {
            Set<String> roleIds = userRoles.stream().map(ur -> String.valueOf(ur.getRoleId())).collect(Collectors.toSet());
            userVo.setRoleVoIds(roleIds);
        }
        return userVo;
    }

    @Override
    public Boolean resetPassword(Long userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPassword(BCrypt.hashpw("888itcast.CN764%",BCrypt.gensalt()));
        user.setUpdateTime(LocalDateTime.now());
        return userMapper.updateByPrimaryKeySelective(user) > 0;
    }

    @Override
    public List<UserVo> findUserList(UserDto userDto) {
         if (userDto != null && StrUtil.isNotBlank(userDto.getDeptNo())) {
             userDto.setDeptNo(NoProcessing.processString(userDto.getDeptNo()));
         }
         if (userDto != null && StrUtil.isBlank(userDto.getDataState())) {
             userDto.setDataState(SuperConstant.DATA_STATE_0);
         }
         List<User> userList = userMapper.selectList(userDto);
         List<UserVo> userVos = BeanUtil.copyToList(userList, UserVo.class);
         fillUserRoles(userVos);
         return userVos;
    }

    private void assignRoles(Long userId, Set<String> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return;
        }
        List<UserRole> userRoles = new ArrayList<>();
        for (String roleIdStr : roleIds) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(Long.parseLong(roleIdStr));
            userRole.setCreateTime(LocalDateTime.now());
            userRole.setUpdateTime(LocalDateTime.now());
            userRole.setDataState(SuperConstant.DATA_STATE_0);
            userRoles.add(userRole);
        }
        if (CollUtil.isNotEmpty(userRoles)) {
            userRoleMapper.batchInsert(userRoles);
        }
    }
}
