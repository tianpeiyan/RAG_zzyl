package com.zzyl.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.zzyl.base.PageResponse;
import com.zzyl.base.ResponseResult;
import com.zzyl.dto.UserDto;
import com.zzyl.service.UserService;
import com.zzyl.vo.UserVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
@Api(tags = "用户管理")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/page/{pageNum}/{pageSize}")
    @ApiOperation(value = "用户分页", notes = "用户分页")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageNum", value = "页码", required = true, dataType = "Integer", paramType = "path"),
            @ApiImplicitParam(name = "pageSize", value = "每页数量", required = true, dataType = "Integer", paramType = "path"),
            @ApiImplicitParam(name = "userDto", value = "用户DTO对象", required = true, dataType = "UserDto")
    })
    public ResponseResult<PageResponse<UserVo>> findUserPage(@PathVariable("pageNum") int pageNum,
                                                             @PathVariable("pageSize") int pageSize,
                                                             @RequestBody UserDto userDto) {
        PageResponse<UserVo> pageResponse = userService.findUserPage(userDto, pageNum, pageSize);
        return ResponseResult.success(pageResponse);
    }

    @PostMapping("/list")
    @ApiOperation(value = "用户列表", notes = "用户列表")
    @ApiImplicitParam(name = "userDto", value = "用户DTO对象", required = true, dataType = "UserDto")
    public ResponseResult<List<UserVo>> findUserList(@RequestBody UserDto userDto) {
        List<UserVo> userList = userService.findUserList(userDto);
        return ResponseResult.success(userList);
    }

    @PutMapping
    @ApiOperation(value = "添加用户", notes = "添加用户")
    @ApiImplicitParam(name = "userDto", value = "用户DTO对象", required = true, dataType = "UserDto")
    public ResponseResult<Boolean> addUser(@RequestBody UserDto userDto) {
        return ResponseResult.success(userService.addUser(userDto));
    }

    @PatchMapping
    @ApiOperation(value = "修改用户", notes = "修改用户")
    @ApiImplicitParam(name = "userDto", value = "用户DTO对象", required = true, dataType = "UserDto")
    public ResponseResult<Boolean> updateUser(@RequestBody UserDto userDto) {
        return ResponseResult.success(userService.updateUser(userDto));
    }

    @DeleteMapping("/remove/{userIds}")
    @ApiOperation(value = "删除用户", notes = "删除用户")
    @ApiImplicitParam(name = "userIds", value = "用户ID集合", required = true, dataType = "String", paramType = "path")
    public ResponseResult<Boolean> deleteUser(@PathVariable("userIds") String userIds) {
        Set<Long> ids = Arrays.stream(userIds.split(","))
                .map(Long::valueOf)
                .collect(Collectors.toSet());
        return ResponseResult.success(userService.deleteUser(ids));
    }

    @PutMapping("/is-enable/{id}/{dataState}")
    @ApiOperation(value = "启用/禁用用户", notes = "启用/禁用用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "用户ID", required = true, dataType = "Long", paramType = "path"),
            @ApiImplicitParam(name = "dataState", value = "状态", required = true, dataType = "String", paramType = "path")
    })
    public ResponseResult<Boolean> enableUser(@PathVariable("id") Long id,
                                              @PathVariable("dataState") String dataState) {
        return ResponseResult.success(userService.enableUser(id, dataState));
    }

    @PostMapping("/reset-passwords/{userId}")
    @ApiOperation(value = "重置密码", notes = "重置密码")
    @ApiImplicitParam(name = "userId", value = "用户ID", required = true, dataType = "Long", paramType = "path")
    public ResponseResult<Boolean> resetPassword(@PathVariable("userId") Long userId) {
        return ResponseResult.success(userService.resetPassword(userId));
    }

    @GetMapping("/current-user")
    @ApiOperation(value = "获取当前用户信息", notes = "获取当前用户信息")
    public ResponseResult<UserVo> getCurrentUser() {
        return ResponseResult.success(userService.getCurrentUser());
    }
}
