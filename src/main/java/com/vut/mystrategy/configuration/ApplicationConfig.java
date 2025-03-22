package com.vut.mystrategy.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ta4j.core.indicators.SMAIndicator;

@Configuration
public class ApplicationConfig {
    @Value("${spring.data.redis.host}")
    private String redisHost;
    @Value("${spring.data.redis.port}")
    private String redisPort;
    @Value("${spring.data.redis.password}")
    private String redisPassword;
    @Value("${redis.storage.max-size}")
    private String redisStorageMaxSize;

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

    @Bean(name = "redisStorageMaxSize")
    public Integer redisStorageMaxSizeBean() {
        return Integer.valueOf(redisStorageMaxSize);
    }
}
