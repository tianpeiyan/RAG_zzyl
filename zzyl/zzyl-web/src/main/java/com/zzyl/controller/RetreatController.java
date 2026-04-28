package com.zzyl.controller;

import com.zzyl.base.PageResponse;
import com.zzyl.base.ResponseResult;
import com.zzyl.dto.RetreatApplyDto;
import com.zzyl.service.RetreatService;
import com.zzyl.vo.RetreatVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/retreat")
@Api(tags = "退住管理")
public class RetreatController extends BaseController {

    @Resource
    private RetreatService retreatService;

    @PostMapping("/apply")
    @ApiOperation("发起退住申请")
    public ResponseResult apply(@RequestBody RetreatApplyDto retreatApplyDto) {
        retreatService.apply(retreatApplyDto);
        return success();
    }

    @GetMapping("/pageQuery")
    @ApiOperation("分页查询")
    public ResponseResult<PageResponse<RetreatVo>> pageQuery(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "elderName", required = false) String name,
            @RequestParam(value = "elderIdCardNo", required = false) String idCard,
            @RequestParam(value = "retreatStartTime", required = false) String startTime,
            @RequestParam(value = "retreatEndTime", required = false) String endTime
    ) {
        return success(retreatService.pageQuery(pageNum, pageSize, name, idCard, startTime, endTime));
    }

    @GetMapping("/detail/{id}")
    @ApiOperation("获取详情")
    public ResponseResult<RetreatVo> detail(@PathVariable Long id) {
        return success(retreatService.detail(id));
    }
}
