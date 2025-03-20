package com.vut.mystrategy.service;

import com.vut.mystrategy.configuration.SymbolConfigManager;
import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.model.SmaPrice;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.model.binance.TradeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class SimpleMovingAverageCalculator {

    private final SymbolConfigManager symbolConfigManager;
    private final RedisClientService redisClientService;
    private final SmaTrendAnalyzer smaTrendAnalyzer;
    private final Integer redisStorageMaxSize;

    @Autowired
    public SimpleMovingAverageCalculator(SymbolConfigManager symbolConfigManager,
                                         RedisClientService redisClientService,
                                         SmaTrendAnalyzer smaTrendAnalyzer,
                                         @Qualifier("redisStorageMaxSize") Integer redisStorageMaxSize) {
        this.symbolConfigManager = symbolConfigManager;
        this.redisClientService = redisClientService;
        this.smaTrendAnalyzer = smaTrendAnalyzer;
        this.redisStorageMaxSize = redisStorageMaxSize;
    }

    @Async("calculateSmaPriceAsync")
    public void calculateSmaPrice(String exchangeName, String symbol) {
        SymbolConfig symbolConfig = symbolConfigManager.getSymbolConfig(exchangeName, symbol);
        //Increase counter and get new value
        String counterKey = KeyUtility.getSmaCounterRedisKey(exchangeName, symbol);
        Long counter = redisClientService.incrementCounter(counterKey);
        if (counter == null) counter = 0L;
        if(counter < symbolConfig.getSmaPeriod() || counter % symbolConfig.getSmaPeriod() != 0) {
            return;
        }

        // reset counter
        redisClientService.resetCounter(counterKey);
        // calculate average price and store redis
        // formula: half-overlapping SMA 10 old tradeEvent + 10 new tradeEvent
        String tradeEventRedisKey = KeyUtility.getTradeEventRedisKey(exchangeName, symbol);
        int tradeEventListSize = symbolConfig.getSmaPeriod() * 2;
        List<TradeEvent> groupTradeEventList = redisClientService.getDataList(tradeEventRedisKey,
                0, tradeEventListSize - 1, TradeEvent.class);
        if(groupTradeEventList.size() < tradeEventListSize) {
            log.warn("Ignore calculating SMA price for exchange because TradeEvent list items = {} < {}",
                    groupTradeEventList.size(), tradeEventListSize);
            return;
        }
        BigDecimal top = Calculator.getMaxPrice(groupTradeEventList, TradeEvent::getPriceAsBigDecimal);
        BigDecimal bottom = Calculator.getMinPrice(groupTradeEventList, TradeEvent::getPriceAsBigDecimal);
        BigDecimal avgPrice = Calculator.getAveragePrice(groupTradeEventList, TradeEvent::getPriceAsBigDecimal);
        if(avgPrice != null) {
            String smaPriceRedisKey = KeyUtility.getSmaPriceRedisKey(exchangeName, symbol);
            SmaPrice averagePrice = SmaPrice.builder()
                    .exchangeName(exchangeName)
                    .symbol(symbol)
                    .price(avgPrice)
                    .topPrice(top)
                    .bottomPrice(bottom)
                    .timestamp(System.currentTimeMillis())
                    .build();

            redisClientService.saveDataAsList(smaPriceRedisKey, averagePrice, redisStorageMaxSize);
            LogMessage.printInsertRedisLogMessage(log, smaPriceRedisKey, averagePrice);

            //call method calculating SMA trend
            smaTrendAnalyzer.analyzeSmaTrend(exchangeName, symbol);
        }
    }
}
