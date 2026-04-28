package com.zzyl.controller;

import com.zzyl.base.PageResponse;
import com.zzyl.base.ResponseResult;
import com.zzyl.dto.NursingTaskDto;
import com.zzyl.service.NursingTaskService;
import com.zzyl.vo.NursingTaskVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/nursingTask")
@Api(tags = "护理任务管理")
public class NursingTaskController {

    @Autowired
    private NursingTaskService nursingTaskService;

    @GetMapping("/page")
    @ApiOperation("分页查询护理任务")
    public ResponseResult<PageResponse<NursingTaskVo>> getByPage(
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String elderName,
            Long nurseId,
            Long projectId,
            String startTime,
            String endTime,
            Integer status) {
        Integer currentPage = pageNum != null ? pageNum : (page != null ? page : 1);
        return ResponseResult.success(nursingTaskService.getByPage(currentPage, pageSize, elderName, nurseId, projectId, startTime, endTime, status));
    }

    @PutMapping("/do")
    @ApiOperation("执行任务")
    public ResponseResult executeTask(@RequestBody NursingTaskDto nursingTaskDto) {
        nursingTaskService.executeTask(nursingTaskDto);
        return ResponseResult.success();
    }

    @PutMapping("/cancel")
    @ApiOperation("取消任务")
    public ResponseResult cancelTask(@RequestBody NursingTaskDto nursingTaskDto) {
        nursingTaskService.cancelTask(nursingTaskDto);
        return ResponseResult.success();
    }

    @PutMapping("/updateTime")
    @ApiOperation("改期")
    public ResponseResult updateTime(@RequestBody NursingTaskDto nursingTaskDto) {
        nursingTaskService.updateTaskTime(nursingTaskDto);
        return ResponseResult.success();
    }

    @GetMapping
    @ApiOperation("任务详情")
    public ResponseResult<NursingTaskVo> getById(Long id) {
        return ResponseResult.success(nursingTaskService.getById(id));
    }
}
