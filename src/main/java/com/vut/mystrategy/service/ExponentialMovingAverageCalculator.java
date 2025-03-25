package com.vut.mystrategy.service;

import com.vut.mystrategy.helper.BarSeriesLoader;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.model.*;
import com.vut.mystrategy.model.binance.KlineEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.util.List;

@Slf4j
@Service
public class ExponentialMovingAverageCalculator {

    private final RedisClientService redisClientService;
    private final CounterPeriodService counterPeriodService;
    private final Integer redisStorageMaxSize;

    @Autowired
    public ExponentialMovingAverageCalculator(CounterPeriodService counterPeriodService,
                                              RedisClientService redisClientService,
                                              @Qualifier("redisStorageMaxSize") Integer redisStorageMaxSize) {
        this.counterPeriodService = counterPeriodService;
        this.redisClientService = redisClientService;
        this.redisStorageMaxSize = redisStorageMaxSize;
    }

    @Async("calculateShortEmaIndicatorAsync")
    public void calculateShortEmaIndicatorAsync(String exchangeName, String symbol, SymbolConfig symbolConfig) {
        int indicatorPeriod = symbolConfig.getEmaShortPeriod();
        calculateEmaIndicator(exchangeName, symbol, indicatorPeriod, symbolConfig.getEmaKlineInterval());
    }

    @Async("calculateLongEmaIndicatorAsync")
    public void calculateLongEmaIndicatorAsync(String exchangeName, String symbol, SymbolConfig symbolConfig) {
        int indicatorPeriod = symbolConfig.getEmaLongPeriod();
        calculateEmaIndicator(exchangeName, symbol, indicatorPeriod, symbolConfig.getEmaKlineInterval());
    }

    private void calculateEmaIndicator(String exchangeName, String symbol,
                                               int indicatorPeriod, String emaKlineInterval) {
        String emaIndicatorRedisKey = KeyUtility.getEmaIndicatorRedisKey(exchangeName, symbol, indicatorPeriod);
        //Increase counter and get new value
        String counterKey = KeyUtility.getIndicatorPeriodCounterRedisKey(emaIndicatorRedisKey);
        if(!counterPeriodService.checkCounterPeriod(counterKey, indicatorPeriod)) {
            return;
        }
        // calculate average price and store redis
        String klineRedisKey = KeyUtility.getKlineRedisKey(exchangeName, symbol,
                KlineIntervalEnum.fromValue(emaKlineInterval));
        List<KlineEvent> klineEvents = redisClientService.getDataList(klineRedisKey, 0,
                indicatorPeriod, KlineEvent.class);
        //Load BarSeries
        BarSeries barSeries = BarSeriesLoader.loadFromKlineEvents(klineEvents);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        //Calculate EMA based on short EMA period in config
        EMAIndicator emaIndicator = new EMAIndicator(closePrice, klineEvents.size());

        //save EMA indicator to Redis
        redisClientService.saveDataAsList(emaIndicatorRedisKey, emaIndicator, redisStorageMaxSize);
        LogMessage.printInsertRedisLogMessage(log, emaIndicatorRedisKey, emaIndicator);
    }
}
