package com.vut.mystrategy.service;

import com.vut.mystrategy.configuration.SymbolConfigManager;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.model.KlineIntervalEnum;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.model.binance.BinanceFutureLotSizeResponse;
import com.vut.mystrategy.model.binance.KlineEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KlineEventService {

    private final SymbolConfigManager symbolConfigManager;
    private final SimpleMovingAverageCalculator simpleMovingAverageCalculator;
    private final ExponentialMovingAverageCalculator exponentialMovingAverageCalculator;
    private final RedisClientService redisClientService;
    private final Integer redisStorageMaxSize;

    @Autowired
    public KlineEventService(SymbolConfigManager symbolConfigManager,
                             SimpleMovingAverageCalculator simpleMovingAverageCalculator,
                             ExponentialMovingAverageCalculator exponentialMovingAverageCalculator,
                             RedisClientService redisClientService,
                             @Qualifier("redisStorageMaxSize") Integer redisStorageMaxSize) {
        this.symbolConfigManager = symbolConfigManager;
        this.simpleMovingAverageCalculator = simpleMovingAverageCalculator;
        this.exponentialMovingAverageCalculator = exponentialMovingAverageCalculator;
        this.redisClientService = redisClientService;
        this.redisStorageMaxSize = redisStorageMaxSize;
    }

    // Lưu KlineEvent vào Redis List when isClosed = true
    @Async("binanceWebSocketAsync")
    public void saveKlineEvent(String exchangeName, String symbol, KlineEvent klineEvent) {
        if(klineEvent.getKlineData() == null || !klineEvent.getKlineData().isClosed()) {
            //make sure the close price in kline is the final price -> isClosed = true
            return;
        }
        KlineIntervalEnum klineEnum = KlineIntervalEnum.fromValue(klineEvent.getKlineData().getInterval());
        String klineRedisKey = KeyUtility.getKlineRedisKey(exchangeName, symbol, klineEnum);
        redisClientService.saveDataAsList(klineRedisKey, klineEvent, redisStorageMaxSize);
        LogMessage.printInsertRedisLogMessage(log, klineRedisKey, klineEvent);

        SymbolConfig symbolConfig = symbolConfigManager.getSymbolConfig(exchangeName, symbol);
        //calculate SMA base on KlineEvent saved only for SMA Kline interval in config
        if(klineEnum.getValue().equals(symbolConfig.getSmaKlineInterval())) {
            simpleMovingAverageCalculator.calculateSmaIndicatorAsync(exchangeName, symbol, symbolConfig);
        }

        //calculate EMA base on KlineEvent saved only for EMA Kline interval in config
        if(klineEnum.getValue().equals(symbolConfig.getEmaKlineInterval())) {
            exponentialMovingAverageCalculator.calculateShortEmaIndicatorAsync(exchangeName, symbol, symbolConfig);
            exponentialMovingAverageCalculator.calculateLongEmaIndicatorAsync(exchangeName, symbol, symbolConfig);
        }
    }

    public void saveFutureLotSize(String exchangeName, String symbol, BinanceFutureLotSizeResponse futureLotSize) {
        String futureLotSizeRedisKey = KeyUtility.getFutureLotSizeRedisKey(exchangeName, symbol);
        redisClientService.saveDataAsSingle(futureLotSizeRedisKey, futureLotSize);
    }

    public BinanceFutureLotSizeResponse getBinanceFutureLotSizeFilter(String symbol) {
        String lotSizeRedisKey = KeyUtility.getFutureLotSizeRedisKey(Constant.EXCHANGE_NAME_BINANCE, symbol);
        return redisClientService.getDataAsSingle(lotSizeRedisKey, BinanceFutureLotSizeResponse.class);
    }
}
