package com.vut.mystrategy.service;

import com.vut.mystrategy.configuration.SymbolConfigManager;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.model.BarSeriesLoader;
import com.vut.mystrategy.model.KlineIntervalEnum;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.model.binance.KlineEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.util.List;

@Slf4j
@Service
public class SimpleMovingAverageCalculator {

    private final SymbolConfigManager symbolConfigManager;
    private final RedisClientService redisClientService;
    private final SmaTrendAnalyzer smaTrendAnalyzer;
    private final CounterPeriodService counterPeriodService;
    private final Integer redisStorageMaxSize;

    @Autowired
    public SimpleMovingAverageCalculator(SymbolConfigManager symbolConfigManager,
                                         RedisClientService redisClientService,
                                         SmaTrendAnalyzer smaTrendAnalyzer,
                                         CounterPeriodService counterPeriodService,
                                         @Qualifier("redisStorageMaxSize") Integer redisStorageMaxSize) {
        this.symbolConfigManager = symbolConfigManager;
        this.redisClientService = redisClientService;
        this.smaTrendAnalyzer = smaTrendAnalyzer;
        this.counterPeriodService = counterPeriodService;
        this.redisStorageMaxSize = redisStorageMaxSize;
    }

    @Async("calculateSmaIndicatorAsync")
    public void calculateSmaIndicatorAsync(String exchangeName, String symbol, SymbolConfig symbolConfig) {

        //Increase counter and get new value
        String counterKey = KeyUtility.getSmaCounterRedisKey(exchangeName, symbol);
        if(!counterPeriodService.checkCounterPeriod(counterKey, symbolConfig.getSmaPeriod())) {
            return;
        }
        // calculate average price and store redis
        String klineRedisKey = KeyUtility.getKlineRedisKey(exchangeName, symbol,
                KlineIntervalEnum.fromValue(symbolConfig.getSmaKlineInterval()));
        List<KlineEvent> klineEvents = redisClientService.getDataList(klineRedisKey, 0,
                symbolConfig.getSmaPeriod(), KlineEvent.class);
        //Load BarSeries
        BarSeries barSeries = BarSeriesLoader.loadFromKlineEvents(klineEvents);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        //Calculate SMA based on SMA period in config
        SMAIndicator smaIndicator = new SMAIndicator(closePrice, klineEvents.size());

        //save SMA indicator to Redis
        String smaIndicatorRedisKey = KeyUtility.getSmaIndicatorRedisKey(exchangeName, symbol);
        redisClientService.saveDataAsList(smaIndicatorRedisKey, smaIndicator, redisStorageMaxSize);
        LogMessage.printInsertRedisLogMessage(log, smaIndicatorRedisKey, smaIndicator);
    }
}
