package com.c2c.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient // 开启 Nacos 注册
@MapperScan("com.c2c.user.mapper") // 扫描 Mapper 接口所在包
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
        System.out.println("====== 用户服务(User-Service)启动成功 ======");
    }
}