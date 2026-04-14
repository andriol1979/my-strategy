package com.vut.mystrategy.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
    @Value("${spring.data.redis.host}")
    private String redisHost;
    @Value("${spring.data.redis.port}")
    private String redisPort;
    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Bean(name = "redisHost")
    public String redisHostBean() {
        return redisHost;
    }

    @Bean(name = "redisPort")
    public Integer redisPortBean() {
        return Integer.valueOf(redisPort);
    }

    @Bean(name = "redisPassword")
    public String redisPasswordBean() {
        return redisPassword;
    }
}
