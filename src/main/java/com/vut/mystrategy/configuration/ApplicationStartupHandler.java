package com.vut.mystrategy.configuration;

import com.vut.mystrategy.component.binance.starter.SymbolConfigManager;
import com.vut.mystrategy.component.binance.starter.BinanceExchangeInfoConfig;
import com.vut.mystrategy.service.RedisClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApplicationStartupHandler {

    private final SymbolConfigManager symbolConfigManager;
    private final RedisClientService redisClientService;
    private final BinanceExchangeInfoConfig binanceExchangeInfoConfig;

    @Autowired
    public ApplicationStartupHandler(SymbolConfigManager symbolConfigManager,
                                     RedisClientService redisClientService,
                                     BinanceExchangeInfoConfig binanceExchangeInfoConfig) {
        this.symbolConfigManager = symbolConfigManager;
        this.redisClientService = redisClientService;
        this.binanceExchangeInfoConfig = binanceExchangeInfoConfig;
    }

    @EventListener
    public void onApplicationStart(ContextRefreshedEvent event) {
    }
}
