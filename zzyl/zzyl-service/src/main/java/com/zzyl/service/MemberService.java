package com.zzyl.service;

import com.zzyl.dto.UserLoginRequestDto;
import com.zzyl.vo.LoginVo;

public interface MemberService {

    /**
     * 小程序登录
     * @param userLoginRequestDto
     * @return
     */
    public LoginVo login(UserLoginRequestDto userLoginRequestDto);
}
