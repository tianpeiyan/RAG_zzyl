package com.zzyl.intercept;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.AntPathMatcher;
import cn.hutool.json.JSONUtil;
import com.zzyl.constant.CacheConstants;
import com.zzyl.constant.Constants;
import com.zzyl.enums.BasicEnum;
import com.zzyl.exception.BaseException;
import com.zzyl.properties.JwtTokenManagerProperties;
import com.zzyl.properties.SecurityConfigProperties;
import com.zzyl.utils.JwtUtil;
import com.zzyl.utils.ObjectUtil;
import com.zzyl.utils.StringUtils;
import com.zzyl.utils.UserThreadLocal;
import com.zzyl.vo.UserVo;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;



@Component
public class UserTokenInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtTokenManagerProperties jwtTokenManagerProperties;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Autowired
    private SecurityConfigProperties securityConfigProperties;

    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //判断当前请求是否是handler()
        if(!(handler instanceof HandlerMethod)){
            return true;
        }

        //获取token
        String token = request.getHeader(Constants.USER_TOKEN);
        if(StringUtils.isEmpty(token)){
            throw new BaseException(BasicEnum.LOGIN_LOSE_EFFICACY);
        }
        //解析token
        Map<String, Object> claims = JwtUtil.parseJWT(jwtTokenManagerProperties.getBase64EncodedSecretKey(), token);
        if(ObjectUtil.isEmpty(claims)){
            throw new BaseException(BasicEnum.LOGIN_LOSE_EFFICACY);
        }
        //获取用户数据
        String userJson = String.valueOf(claims.get("currentUser"));
        UserVo userVo = JSONUtil.toBean(userJson, UserVo.class);
        //从redis中获取url列表
        String key = CacheConstants.PUBLIC_ACCESS_URLS + userVo.getId();
        String urlJson = redisTemplate.opsForValue().get(key);
        List<String> urlList = null;
        if(StringUtils.isNotEmpty(urlJson)){
            urlList = JSONUtil.toList(urlJson, String.class);
        }
        //获取当前请求路径
        String targetUrl = request.getMethod() + request.getRequestURI();

        //匹配当前路径是否在urllist集合中
        if (ObjectUtil.isNotEmpty(urlList)) {
            for (String url : urlList) {
                if (antPathMatcher.match(url, targetUrl)) {
                    //存储当前线程中
                    UserThreadLocal.setSubject(userJson);
                    return true;
                }
            }
        }

        List<String> publicAccessUrls = securityConfigProperties.getPublicAccessUrls();
        if (ObjectUtil.isNotEmpty(publicAccessUrls)) {
            for (String url : publicAccessUrls) {
                if (antPathMatcher.match(url, targetUrl)) {
                    UserThreadLocal.setSubject(userJson);
                    return true;
                }
            }
        }

        throw new BaseException(BasicEnum.SECURITY_ACCESSDENIED_FAIL);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserThreadLocal.removeSubject();
    }
}
