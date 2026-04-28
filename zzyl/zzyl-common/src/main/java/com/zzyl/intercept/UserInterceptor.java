package com.zzyl.intercept;

import cn.hutool.core.map.MapUtil;
import com.zzyl.constant.Constants;
import com.zzyl.enums.BasicEnum;
import com.zzyl.exception.BaseException;
import com.zzyl.properties.JwtTokenManagerProperties;
import com.zzyl.utils.JwtUtil;
import com.zzyl.utils.ObjectUtil;
import com.zzyl.utils.StringUtils;
import com.zzyl.utils.UserThreadLocal;
import io.jsonwebtoken.Claims;
import org.assertj.core.util.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class UserInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtTokenManagerProperties jwtTokenManagerProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //判断当前请求是否是handler()
        if(!(handler instanceof HandlerMethod)){
            return true;
        }

        //获取token
        String token = request.getHeader(Constants.USER_TOKEN);
        if(StringUtils.isEmpty(token)){
            throw new BaseException(BasicEnum.SECURITY_ACCESSDENIED_FAIL);
        }
        //解析token
        Map<String, Object> claims = JwtUtil.parseJWT(jwtTokenManagerProperties.getBase64EncodedSecretKey(), token);
        if(ObjectUtil.isEmpty(claims)){
            throw new BaseException(BasicEnum.SECURITY_ACCESSDENIED_FAIL);
        }
        //Long userId = (Long) claims.get(Constants.JWT_USERID);
        Long userId = MapUtil.get(claims, Constants.JWT_USERID, Long.class);
        if(ObjectUtil.isEmpty(userId)){
            throw new BaseException(BasicEnum.SECURITY_ACCESSDENIED_FAIL);
        }
        //把数据存储到线程中
        UserThreadLocal.set(userId);

        return true;

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserThreadLocal.remove();
    }
}
