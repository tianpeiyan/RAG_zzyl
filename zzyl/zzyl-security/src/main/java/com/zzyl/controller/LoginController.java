package com.zzyl.controller;

import com.zzyl.base.ResponseResult;
import com.zzyl.dto.LoginDto;
import com.zzyl.service.LoginService;
import com.zzyl.vo.UserVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/security")
@Api(tags = "用户登录")
@Slf4j
public class LoginController {

    @Autowired
    private LoginService loginService;
    /**
     * 用户登录
     */
    @PostMapping("/login")
    @ApiOperation("用户登录")
    public ResponseResult<UserVo> login(@RequestBody LoginDto loginDto) {
        log.info("用户登录:{}", loginDto);
        UserVo userVo = loginService.login(loginDto);
        return ResponseResult.success(userVo);
    }

}
