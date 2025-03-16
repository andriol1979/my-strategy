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
    @Value("${redis.trade-event.max-size}")
    private String redisTradeEventMaxSize;

    @Value("${sma-period}")
    private String smaPeriod;

    @Value("${ema-short-period}")
    private String emaShortPeriod;
    @Value("${ema-long-period}")
    private String emaLongPeriod;

    @Value("${sum-volume-period}")
    private String sumVolumePeriod;
    @Value("${sum-volume-taker-weight}")
    private String sumVolumeTakerWeight;
    @Value("${sum-volume-maker-weight}")
    private String sumVolumeMakerWeight;

    @Value("${base-trend-sma-period}")
    private String baseTrendSmaPeriod;
    @Value("${base-trend-divergence-volume-period}")
    private String baseTrendDivergenceVolumePeriod;

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

    @Bean(name = "redisTradeEventMaxSize")
    public Integer redisTradeEventMaxSizeBean() {
        return Integer.valueOf(redisTradeEventMaxSize);
    }

    @Bean(name = "smaPeriod")
    public Integer smaPeriodBean() {
        return Integer.valueOf(smaPeriod);
    }

    @Bean(name = "emaShortPeriod")
    public Integer emaShortPeriodBean() {
        return Integer.valueOf(emaShortPeriod);
    }

    @Bean(name = "emaLongPeriod")
    public Integer emaLongPeriodBean() {
        return Integer.valueOf(emaLongPeriod);
    }

    @Bean(name = "sumVolumePeriod")
    public Integer sumVolumePeriodBean() {
        return Integer.valueOf(sumVolumePeriod);
    }

    @Bean(name = "sumVolumeTakerWeight")
    public Double sumVolumeTakerWeightBean() {
        return Double.valueOf(sumVolumeTakerWeight);
    }

    @Bean(name = "sumVolumeMakerWeight")
    public Double sumVolumeMakerWeightBean() {
        return Double.valueOf(sumVolumeMakerWeight);
    }

    @Bean(name = "baseTrendSmaPeriod")
    public Integer baseTrendSmaPeriodBean() {
        return Integer.valueOf(baseTrendSmaPeriod);
    }

    @Bean(name = "baseTrendDivergenceVolumePeriod")
    public Integer baseTrendDivergenceVolumePeriodBean() {
        return Integer.valueOf(baseTrendDivergenceVolumePeriod);
    }
}
