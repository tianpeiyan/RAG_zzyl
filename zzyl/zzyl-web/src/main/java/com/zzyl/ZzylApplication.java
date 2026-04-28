package com.zzyl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  //开启任务调度
public class ZzylApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZzylApplication.class, args);
	}
}
