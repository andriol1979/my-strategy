package com.vut.mystrategy.service;

import com.vut.mystrategy.configuration.DataFetcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public abstract class AbstractSignalMonitor {

    protected final Map<String, AbstractOrderService> orderServices;
    protected final TradingSignalAnalyzer tradingSignalAnalyzer;
    protected final RedisClientService redisClientService;
    protected final AbstractOrderManager orderManager;

    @Value("${analyze-scheduler-initial-delay}")
    private long analyzeSchedulerInitialDelay;

    @Autowired
    public AbstractSignalMonitor(Map<String, AbstractOrderService> orderServices,
                                 TradingSignalAnalyzer tradingSignalAnalyzer,
                                 RedisClientService redisClientService,
                                 AbstractOrderManager orderManager) {
        this.orderServices = orderServices;
        this.tradingSignalAnalyzer = tradingSignalAnalyzer;
        this.redisClientService = redisClientService;
        this.orderManager = orderManager;
    }

    public abstract void monitorSignal(DataFetcher dataFetcher);
}
