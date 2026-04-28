
package com.zzyl.controller;

import com.fasterxml.jackson.databind.ser.Serializers;
import com.zzyl.base.PageResponse;
import com.zzyl.base.ResponseResult;
import com.zzyl.dto.NursingElderDto;
import com.zzyl.service.ElderService;
import com.zzyl.vo.ElderVo;
import com.zzyl.vo.RetreatVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 客户老人关联 Controller
 */
@RestController
@RequestMapping("/elder")
@Api(tags = "老人")
public class ElderController extends BaseController {

    @Resource
    private ElderService elderService;

    /**
     * 列表
     */
    @GetMapping("/selectList")
    @ApiOperation(value = "列表")
    public ResponseResult selectList() {
        List<ElderVo> elderVos = elderService.selectList();
        return success(elderVos);
    }


    @GetMapping("/selectByIdCard")
    @ApiOperation(value = "身份证号", notes = "身份证号")
    public ResponseResult selectByIdCard(@RequestParam String  idCard) {
        return success(elderService.selectByIdCard(idCard));
    }

    @GetMapping("/pageQuery")
    @ApiOperation(value = "分页查询")
    public ResponseResult<PageResponse<ElderVo>> pageQuery(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "idCardNo", required = false) String idCardNo,
            @RequestParam(value = "status", required = false) Integer status
    ) {
        return success(elderService.pageQuery(pageNum, pageSize, name, idCardNo, status));
    }

    @GetMapping("/checkInInfo/{id}")
    @ApiOperation(value = "入住信息", notes = "入住信息")
    public ResponseResult<RetreatVo> checkInInfo(@PathVariable Long id) {
        return success(elderService.getCheckInInfo(id));
    }

    @PutMapping("/setNursing")
    @ApiOperation(value = "设置护理员", notes = "设置护理员")
    public ResponseResult setNursing(@RequestBody List<NursingElderDto> nursingElderDtos) {
        elderService.setNursing(nursingElderDtos);
        return success();
    }
}


