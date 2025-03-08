package com.vut.mystrategy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MyStrategyApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyStrategyApplication.class, args);
    }

}
