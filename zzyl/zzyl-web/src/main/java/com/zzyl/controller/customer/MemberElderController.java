package com.zzyl.controller.customer;

import com.zzyl.base.PageResponse;
import com.zzyl.base.ResponseResult;
import com.zzyl.dto.MemberElderDto;
import com.zzyl.service.MemberElderService;
import com.zzyl.vo.MemberElderVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer/memberElder")
@Api(tags = "客户老人关联")
public class MemberElderController {

    @Autowired
    private MemberElderService memberElderService;

    @PostMapping("/add")
    @ApiOperation("绑定老人")
    public ResponseResult add(@RequestBody MemberElderDto memberElderDto) {
        memberElderService.add(memberElderDto);
        return ResponseResult.success();
    }

    @GetMapping("/list-by-page")
    @ApiOperation("分页查询绑定老人列表")
    public ResponseResult<PageResponse<MemberElderVo>> listByPage() {
        List<MemberElderVo> list = memberElderService.list();
        return ResponseResult.success(PageResponse.of(list));
    }

    @GetMapping("/my")
    @ApiOperation("查询所有绑定老人")
    public ResponseResult<List<MemberElderVo>> my() {
        List<MemberElderVo> list = memberElderService.list();
        return ResponseResult.success(list);
    }

    @DeleteMapping("/deleteById")
    @ApiOperation("解绑老人")
    public ResponseResult deleteById(@RequestParam Long id) {
        memberElderService.deleteById(id);
        return ResponseResult.success();
    }
}
