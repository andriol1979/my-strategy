package com.vut.mystrategy.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class AsyncModuleManager {

    private final AtomicBoolean webSocketReady = new AtomicBoolean(false);
    private final AtomicBoolean averagePriceReady = new AtomicBoolean(false);

    public boolean isWebSocketReady() {
        return webSocketReady.get();
    }

    public boolean isAveragePriceReady() {
        return averagePriceReady.get();
    }

    public boolean isAllModulesReady() {
        return webSocketReady.get() && averagePriceReady.get();
    }

    public void markWebSocketReady() {
        webSocketReady.set(true);
    }

    public void markAveragePriceReady() {
        averagePriceReady.set(true);
    }

    public void markWebSocketDisconnected() {
        webSocketReady.set(false);
    }
}
