package com.xudu.articlepilot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
@MapperScan("com.xudu.articlepilot.mapper")
public class ArticlePilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArticlePilotApplication.class, args);
    }

}
