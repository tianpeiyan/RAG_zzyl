package com.zzyl.test;

import com.zzyl.ZzylApplication;
import com.zzyl.service.WechatService;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ZzylApplication.class)
@Ignore
public class WechatServiceImplTest {

    @Autowired
    private WechatService wechatService;

    @Test
    public void testGetOpenid(){
        String openid = wechatService.getOpenid("0e3RqL000Si200yuYiV1RqL0a");
        System.out.println(openid);
    }

    @Test
    public void testGetPhone(){
        String phone = wechatService.getPhone("cba679021a073f85557a31f80b54a90db8c30f63c8df9066f5b878");
        System.out.println(phone);
    }
}
