package com.example.yuca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class YucaApplication {

	public static void main(String[] args) {
		SpringApplication.run(YucaApplication.class, args);
	}

}
