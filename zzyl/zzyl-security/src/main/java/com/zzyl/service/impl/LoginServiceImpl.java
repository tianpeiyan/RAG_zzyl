package com.zzyl.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.json.JSONUtil;
import com.zzyl.constant.CacheConstants;
import com.zzyl.dto.LoginDto;
import com.zzyl.entity.Resource;
import com.zzyl.entity.User;
import com.zzyl.enums.BasicEnum;
import com.zzyl.exception.BaseException;
import com.zzyl.mapper.UserMapper;
import com.zzyl.properties.JwtTokenManagerProperties;
import com.zzyl.properties.SecurityConfigProperties;
import com.zzyl.service.LoginService;
import com.zzyl.utils.JwtUtil;
import com.zzyl.utils.ObjectUtil;
import com.zzyl.vo.UserVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtTokenManagerProperties jwtTokenManagerProperties;
    @Autowired
    private SecurityConfigProperties SecurityConfigProperties;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    /**
     * 用户登录
     */
    @Override
    public UserVo login(LoginDto loginDto) {
        User user = userMapper.selectByUsername(loginDto);
        //判断用户是否存在
        if(ObjectUtil.isEmpty(user)){
            throw new BaseException(BasicEnum.LOGIN_FAIL);
        }
        //判断账号是否禁用
        if(user.getDataState().equals("1")){
            throw new BaseException(BasicEnum.Data_State_Exception);
        }
        //判断密码是否正确
        if(!BCrypt.checkpw(loginDto.getPassword(), user.getPassword())){
            throw new BaseException(BasicEnum.Password_Error);
        }
        //密码脱敏
        user.setPassword("");

        //获取用户资源列表和白名单列表
        List<Resource> userResourceList = userMapper.selectResourceById(user.getId());
        List<String> publicAccessUrls = SecurityConfigProperties.getPublicAccessUrls();

        List<String> urlList = userResourceList.stream().map(Resource::getRequestPath).collect(Collectors.toList());
        urlList.addAll(publicAccessUrls);


        //封装Vo
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user,userVo);
        //生成token
        Map<String,Object> claims = new HashMap<>();
        claims.put("currentUser", JSONUtil.toJsonStr(userVo));

        String token = JwtUtil.createJWT(jwtTokenManagerProperties.getBase64EncodedSecretKey(), jwtTokenManagerProperties.getTtl(), claims);

        //设置redis过期时间
        int title = jwtTokenManagerProperties.getTtl() / 1000;
        //将资源列表存入redis
        redisTemplate.opsForValue().set(CacheConstants.PUBLIC_ACCESS_URLS + user.getId(), JSONUtil.toJsonStr(urlList),title, TimeUnit.SECONDS);

        //封装Vo并返回
        userVo.setUserToken(token);

        return userVo;
    }



}
