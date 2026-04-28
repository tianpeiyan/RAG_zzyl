package com.zzyl.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyTask {

//    @Scheduled(fixedRate = 2000) //固定速率，每隔2秒执行一次方法
    @Scheduled(cron = "0 1-30 * * * ?")
    public void task(){
        System.out.println(LocalDateTime.now());
    }
}
