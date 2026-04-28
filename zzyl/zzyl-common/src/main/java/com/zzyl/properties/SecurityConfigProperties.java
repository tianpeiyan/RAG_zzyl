package com.zzyl.properties;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户资源白名单配置文件
 */

@Configuration
@ConfigurationProperties(prefix = "zzyl.framework.security")
@Data
public class SecurityConfigProperties implements Serializable {
     //白名单列表
     List<String> publicAccessUrls = new ArrayList<>();

     //静态资源列表
     List<String> ignoreUrls = new ArrayList<>();

}