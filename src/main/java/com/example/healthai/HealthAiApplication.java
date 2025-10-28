package com.example.healthai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.healthai.auth.config.JwtProperties;
import com.example.healthai.auth.config.RefreshTokenProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, RefreshTokenProperties.class})
public class HealthAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(HealthAiApplication.class, args);
	}

}
