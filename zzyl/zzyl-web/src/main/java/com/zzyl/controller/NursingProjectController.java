package com.zzyl.controller;


import com.zzyl.base.PageResponse;
import com.zzyl.base.ResponseResult;
import com.zzyl.dto.NursingProjectDto;
import com.zzyl.service.NursingProjectService;
import com.zzyl.vo.NursingProjectVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/nursing_project")
@Api(tags = "护理项目相关接口")
public class NursingProjectController extends BaseController {

    @Autowired
    private NursingProjectService nursingProjectService;

    @GetMapping
    @ApiOperation("分页条件查询护理项目")
    public ResponseResult selectByPage(@ApiParam(value = "护理项目名称") String name, Integer status, Integer pageNum, Integer pageSize){
        PageResponse pageResponse = nursingProjectService.selectByPage(name, status, pageNum, pageSize);
        return success(pageResponse);
    }

    @PostMapping
    public ResponseResult add(@RequestBody NursingProjectDto nursingProjectDto){
        nursingProjectService.add(nursingProjectDto);
        return success();
    }

    @GetMapping("/{id}")
    public ResponseResult<NursingProjectVo> getById(@PathVariable("id") Long id){
        NursingProjectVo nursingProjectVo = nursingProjectService.findById(id);
        return success(nursingProjectVo);
    }

    @PutMapping
    public ResponseResult update(@RequestBody NursingProjectDto nursingProjectDto){
        nursingProjectService.update(nursingProjectDto);
        return success();
    }

    @PutMapping("/{id}/status/{status}")
    public ResponseResult isEnable(@PathVariable("id") Long id,@PathVariable("status") Integer status){
        nursingProjectService.isEnable(id,status);
        return success();
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除护理项目信息")
    public ResponseResult deleteById(@PathVariable("id") Long id) {
        nursingProjectService.deleteById(id);
        return ResponseResult.success();
    }

    @GetMapping("/all")
    public ResponseResult<List<NursingProjectVo>> getAll(){
        return success(nursingProjectService.selectAll());
    }
}
