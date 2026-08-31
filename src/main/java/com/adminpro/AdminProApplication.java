package com.adminpro;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.adminpro.mapper")
public class AdminProApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminProApplication.class, args);
    }
}
