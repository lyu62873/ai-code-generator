package com.leyu.aicodegenerator;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.leyu.aicodegenerator.mapper")
public class AiCodeGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeGeneratorApplication.class, args);
    }

}
