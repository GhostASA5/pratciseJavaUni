package com.example.cachepractice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CacheComparisonApplication {
    public static void main(String[] args) {
        SpringApplication.run(CacheComparisonApplication.class, args);
    }
}

