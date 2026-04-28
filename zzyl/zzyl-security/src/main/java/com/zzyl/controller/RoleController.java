package com.zzyl.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.zzyl.base.PageResponse;
import com.zzyl.base.ResponseResult;
import com.zzyl.dto.RoleDto;
import com.zzyl.entity.Role;
import com.zzyl.service.RoleService;
import com.zzyl.vo.RoleVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 角色前端控制器
 */
@Slf4j
@Api(tags = "角色管理")
@RestController
@RequestMapping("/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @PostMapping("/page/{pageNum}/{pageSize}")
    @ApiOperation(value = "角色分页", notes = "角色分页")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roleDto", value = "角色DTO对象", required = true, dataType = "RoleDto"),
            @ApiImplicitParam(paramType = "path", name = "pageNum", value = "页码", example = "1", dataType = "Integer"),
            @ApiImplicitParam(paramType = "path", name = "pageSize", value = "每页条数", example = "10", dataType = "Integer")
    })
    @ApiOperationSupport(includeParameters = {"roleDto.roleName", "roleDto.dataState"})
    public ResponseResult<PageResponse<Role>> findRolePage(
            @RequestBody RoleDto roleDto,
            @PathVariable("pageNum") int pageNum,
            @PathVariable("pageSize") int pageSize) {
        PageResponse<Role> pageResponse = roleService.findRolePage(roleDto, pageNum, pageSize);
        return ResponseResult.success(pageResponse);
    }

    @PostMapping("/list")
    @ApiOperation(value = "角色列表", notes = "角色列表")
    @ApiImplicitParam(name = "roleDto", value = "角色DTO对象", required = true, dataType = "RoleDto")
    public ResponseResult<List<Role>> roleList(@RequestBody RoleDto roleDto) {
        List<Role> list = roleService.findRoleList(roleDto);
        return ResponseResult.success(list);
    }
    
    // 兼容前端可能的调用
    @PostMapping("/init-roles")
    @ApiOperation(value = "角色下拉列表", notes = "角色下拉列表")
    public ResponseResult<List<Role>> initRoles() {
        List<Role> list = roleService.findRoleList(new RoleDto());
        return ResponseResult.success(list);
    }

    @PutMapping
    @ApiOperation(value = "角色添加", notes = "角色添加")
    @ApiImplicitParam(name = "roleDto", value = "角色DTO对象", required = true, dataType = "RoleDto")
    @ApiOperationSupport(includeParameters = {
            "roleDto.roleName",
            "roleDto.label",
            "roleDto.sortNo",
            "roleDto.dataState",
            "roleDto.remark",
            "roleDto.checkedResourceNos"
    })
    public ResponseResult<RoleVo> createRole(@RequestBody RoleDto roleDto) {
        RoleVo roleVo = roleService.createRole(roleDto);
        return ResponseResult.success(roleVo);
    }

    @PatchMapping
    @ApiOperation(value = "角色修改", notes = "角色修改")
    @ApiImplicitParam(name = "roleDto", value = "角色DTO对象", required = true, dataType = "RoleDto")
    @ApiOperationSupport(includeParameters = {
            "roleDto.id",
            "roleDto.roleName",
            "roleDto.label",
            "roleDto.sortNo",
            "roleDto.dataState",
            "roleDto.remark",
            "roleDto.checkedResourceNos"
    })
    public ResponseResult<Boolean> updateRole(@RequestBody RoleDto roleDto) {
        Boolean flag = roleService.updateRole(roleDto);
        return ResponseResult.success(flag);
    }

    @DeleteMapping("/{roleId}")
    @ApiOperation(value = "角色删除", notes = "角色删除")
    public ResponseResult<Boolean> deleteRole(@PathVariable("roleId") Long roleId) {
        return ResponseResult.success(roleService.deleteRole(roleId));
    }

    @GetMapping("/find-checked-resources/{roleId}")
    @ApiOperation(value = "查询角色拥有的资源ID", notes = "查询角色拥有的资源ID")
    public ResponseResult<Set<String>> findCheckedResources(@PathVariable("roleId") Long roleId) {
        Set<String> resourceNos = roleService.findCheckedResources(roleId);
        return ResponseResult.success(resourceNos);
    }
}
