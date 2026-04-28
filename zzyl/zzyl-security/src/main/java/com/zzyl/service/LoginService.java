package com.zzyl.service;

import com.zzyl.dto.LoginDto;
import com.zzyl.vo.UserVo;

public interface LoginService {

    /**
     * 用户登录
     */
    UserVo login(LoginDto loginDto);


}
