package com.vut.mystrategy.component.binance.starter;

import com.vut.mystrategy.component.binance.BinanceFutureRestApiClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RenewListenKeyScheduler {
    private final BinanceFutureRestApiClient binanceFutureRestApiClient;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    public RenewListenKeyScheduler(BinanceFutureRestApiClient binanceFutureRestApiClient) {
        this.binanceFutureRestApiClient = binanceFutureRestApiClient;
    }

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(binanceFutureRestApiClient::extendListenKey,
                50, 50, TimeUnit.MINUTES
        );
    }
}
