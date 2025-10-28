package com.example.healthai.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.example.healthai.**.repository")
public class MyBatisConfig {
}
