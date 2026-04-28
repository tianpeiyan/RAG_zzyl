package com.zzyl.controller.customer;

import com.zzyl.base.ResponseResult;
import com.zzyl.dto.UserLoginRequestDto;
import com.zzyl.service.MemberService;
import com.zzyl.vo.LoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer/user")
public class CustomerUserController {

    @Autowired
    private MemberService memberService;

    @PostMapping("/login")
    public ResponseResult<LoginVo> login(@RequestBody UserLoginRequestDto userLoginRequestDto){
        LoginVo loginVo = memberService.login(userLoginRequestDto);
        return ResponseResult.success(loginVo);
    }
}
