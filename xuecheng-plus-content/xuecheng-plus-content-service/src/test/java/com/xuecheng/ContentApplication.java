package com.xuecheng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author zengweichuan
 * @description 内容管理服务启动类
 * @date 2024/3/24
 */
@EnableFeignClients(basePackages={"com.xuecheng.content.feignclient"})
@SpringBootApplication
public class ContentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentApplication.class,args);
        System.out.println("项目启动完毕!");
    }
}
