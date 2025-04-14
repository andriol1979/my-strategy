package com.vut.mystrategy.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
public class RestTemplateConfig {
    @Bean("restTemplate")
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Optionally add timeout or interceptors
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5s
        factory.setReadTimeout(10000); // 10s
        restTemplate.setRequestFactory(factory);

        // Optionally add logging interceptor or retry
        // restTemplate.setInterceptors(List.of(new LoggingInterceptor()));
        log.info("RestTemplate configured");
        return restTemplate;
    }
}
