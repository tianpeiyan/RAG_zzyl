package com.zzyl.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.zzyl.base.ResponseResult;
import com.zzyl.dto.ResourceDto;
import com.zzyl.service.ResourceService;
import com.zzyl.utils.UserThreadLocal;
import com.zzyl.vo.MenuVo;
import com.zzyl.vo.ResourceVo;
import com.zzyl.vo.TreeVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资源前端控制器
 */
@Slf4j
@Api(tags = "资源管理")
@RestController
@RequestMapping("/resource")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @PostMapping("/list")
    @ApiOperation(value = "资源列表", notes = "资源列表")
    @ApiImplicitParam(name = "resourceDto", value = "资源DTO对象", required = true, dataType = "ResourceDto")
    @ApiOperationSupport(includeParameters = {"resourceDto.parentResourceNo", "resourceDto.resourceType"})
    public ResponseResult<List<ResourceVo>> resourceList(@RequestBody ResourceDto resourceDto) {
        List<ResourceVo> resourceVoList = resourceService.findResourceList(resourceDto);
        return ResponseResult.success(resourceVoList);
    }

    @PostMapping("/tree")
    @ApiOperation(value = "资源树形", notes = "资源树形")
    @ApiImplicitParam(name = "resourceDto", value = "资源DTO对象", required = true, dataType = "ResourceDto")
    @ApiOperationSupport(includeParameters = {"resourceDto.label"})
    public ResponseResult<TreeVo> resourceTreeVo(@RequestBody ResourceDto resourceDto) {
        TreeVo treeVo = resourceService.resourceTreeVo(resourceDto);
        return ResponseResult.success(treeVo);
    }

    @PutMapping
    @ApiOperation(value = "资源添加", notes = "资源添加")
    @ApiImplicitParam(name = "resourceDto", value = "资源DTO对象", required = true, dataType = "ResourceDto")
    @ApiOperationSupport(includeParameters = {"resourceDto.dataState"
            , "resourceDto.icon"
            , "resourceDto.parentResourceNo"
            , "resourceDto.requestPath"
            , "resourceDto.resourceName"
            , "resourceDto.resourceType"
            , "resourceDto.sortNo"})
    public ResponseResult<ResourceVo> createResource(@RequestBody ResourceDto resourceDto) {
        resourceService.createResource(resourceDto);
        return ResponseResult.success();
    }

    @PatchMapping
    @ApiOperation(value = "资源修改", notes = "资源修改")
    @ApiImplicitParam(name = "resourceDto", value = "资源DTO对象", required = true, dataType = "ResourceDto")
    @ApiOperationSupport(includeParameters = {"resourceDto.dataState"
            , "resourceDto.icon"
            , "resourceDto.parentResourceNo"
            , "resourceDto.requestPath"
            , "resourceDto.resourceName"
            , "resourceDto.resourceType"
            , "resourceDto.sortNo"
            , "resourceDto.resourceNo"})
    public ResponseResult updateResource(@RequestBody ResourceDto resourceDto) {
        resourceService.updateResource(resourceDto);
        return ResponseResult.success();
    }

    @DeleteMapping("/{resourceNo}")
    @ApiOperation(value = "资源删除", notes = "资源删除")
    public ResponseResult deleteResource(@PathVariable("resourceNo") String resourceNo) {
        resourceService.deleteResource(resourceNo);
        return ResponseResult.success();
    }

    @PostMapping("/enable")
    @ApiOperation(value = "资源启用禁用", notes = "资源启用禁用")
    public ResponseResult enable(@RequestBody ResourceDto resourceDto) {
        resourceService.enable(resourceDto);
        return ResponseResult.success();
    }

    /**
     * @return
     *  动态菜单（根据用户id，实现不同用户可访问资源不同）
     */
    @GetMapping("/menus")
    @ApiOperation(value = "左侧菜单", notes = "左侧菜单")
    public ResponseResult<List<MenuVo>> menus() {
        Long userId = UserThreadLocal.getMgtUserId();
        List<MenuVo> menus = resourceService.menus(userId);
        return ResponseResult.success(menus);
    }
}
