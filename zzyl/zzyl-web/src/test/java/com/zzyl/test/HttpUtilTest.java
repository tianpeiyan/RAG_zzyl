package com.zzyl.test;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class HttpUtilTest {


    /**
     * get和post请求
     */


    @Test
    public void testGet(){
        //发起get请求
        String result = HttpUtil.get("https://www.baidu.com");
        System.out.println(result);
    }

    @Test
    public void testGetProjectList(){
        String url = "http://localhost:9995/nursing_project";
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("pageNum",1);
        paramMap.put("pageSize",10);

        String result = HttpUtil.get(url, paramMap);
        System.out.println(result);
    }

    @Test
    public void testGetProjectList2(){
        String url = "http://localhost:9995/nursing_project";
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("pageNum",1);
        paramMap.put("pageSize",10);

        HttpResponse response = HttpUtil.createRequest(Method.GET, url)
                .form(paramMap).execute();
        if(response.getStatus() == 200){
            System.out.println(response.body());
        }
    }

    @Test
    public void testPost(){
        String url = "http://localhost:9995/nursing_project";
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", "护理项目测试3211违反");
        paramMap.put("orderNo", 1);
        paramMap.put("unit", "次");
        paramMap.put("price", 10.00);
        paramMap.put("image", "https://yjy-slwl-oss.oss-cn-hangzhou.aliyuncs.com/ae7cf766-fb7b-49ff-a73c-c86c25f280e1.png");
        paramMap.put("nursingRequirement", "无特殊要求");
        paramMap.put("status", 1);
       /* String result = HttpUtil.post(url, JSONUtil.toJsonStr(paramMap));
        System.out.println(result);*/

        HttpResponse response = HttpUtil.createRequest(Method.POST, url).body(JSONUtil.toJsonStr(paramMap)).execute();
        if(response.getStatus() == 200){
            System.out.println(response.body());
        }

    }

    /**
     * 云市场控制台  https://market.console.aliyun.com/imageconsole/index.htm#/?_k=tyrj3l
     */

    @Test
    public void testWeater() {
        String url = "https://aliv18.data.moji.com/whapi/json/alicityweather/forecast24hours";
        String appcode = "d0bbfea3151e4d92a3e10781dcd8bc1b";
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "APPCODE " + appcode);
        //根据API的要求，定义相对应的Content-Type
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        Map<String, Object> bodys = new HashMap<String, Object>();
        bodys.put("cityId", "2");

        //发起请求
        HttpResponse response = HttpUtil.createRequest(Method.POST, url).addHeaders(headers).form(bodys).execute();
        if(response.getStatus() == 200){
            System.out.println(response.body());
        }

    }
}
