package com.vut.mystrategy.configuration;

import com.vut.mystrategy.configuration.feeddata.binance.BinanceExchangeInfoConfig;
import com.vut.mystrategy.model.KlineIntervalEnum;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.service.RedisClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
