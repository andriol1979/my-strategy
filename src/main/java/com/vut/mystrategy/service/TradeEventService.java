package com.vut.mystrategy.service;

import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.model.binance.BinanceFutureLotSizeResponse;
import com.vut.mystrategy.model.binance.TradeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class TradeEventService {

    private final SimpleMovingAverageCalculator simpleMovingAverageCalculator;
    private final ExponentialMovingAverageCalculator exponentialMovingAverageCalculator;
    private final SumVolumeCalculator sumVolumeCalculator;
    private final RedisClientService redisClientService;
    private final Integer redisStorageMaxSize;

    @Autowired
    public TradeEventService(SimpleMovingAverageCalculator simpleMovingAverageCalculator,
                             ExponentialMovingAverageCalculator exponentialMovingAverageCalculator,
                             SumVolumeCalculator sumVolumeCalculator,
                             RedisClientService redisClientService,
                             @Qualifier("redisStorageMaxSize") Integer redisStorageMaxSize) {
        this.simpleMovingAverageCalculator = simpleMovingAverageCalculator;
        this.exponentialMovingAverageCalculator = exponentialMovingAverageCalculator;
        this.sumVolumeCalculator = sumVolumeCalculator;
        this.redisClientService = redisClientService;
        this.redisStorageMaxSize = redisStorageMaxSize;
    }

    // Lưu TradeEvent vào Redis List
    @Async("binanceWebSocketAsync")
    public void saveTradeEvent(String exchangeName, String symbol, TradeEvent tradeEvent) {
        if(checkDuplicateTradeEvent(exchangeName, symbol, tradeEvent)) {
            log.warn("Duplicate TradeEvent Id detected. Ignored TradeEvent Id: {}", tradeEvent.getTradeId());
            return;
        }
        String tradeEventRedisKey = KeyUtility.getTradeEventRedisKey(exchangeName, symbol);
        redisClientService.saveDataAsList(tradeEventRedisKey, tradeEvent, redisStorageMaxSize);
        LogMessage.printInsertRedisLogMessage(log, tradeEventRedisKey, tradeEvent);

        //Sum bull/bear volumes into temp_sum_volume
        sumVolumeCalculator.calculateTempSumVolume(exchangeName, symbol, tradeEvent);

        //Increase counter and calculate SMA
        simpleMovingAverageCalculator.calculateSmaPrice(exchangeName, symbol);

        //Calculate SHORT EMA price based on trade event but waiting first SMA is saved
        exponentialMovingAverageCalculator.calculateShortEmaPrice(exchangeName, symbol, tradeEvent);
    }

    //Get first trade event
    public Optional<TradeEvent> getNewestTradeEvent(String exchangeName, String symbol) {
        String tradeEventRedisKey = KeyUtility.getTradeEventRedisKey(exchangeName, symbol);
        TradeEvent tradeEvent = redisClientService.getDataByIndex(tradeEventRedisKey, 0, TradeEvent.class);
        return tradeEvent == null ? Optional.empty() : Optional.of(tradeEvent);
    }

    public void saveFutureLotSize(String exchangeName, String symbol, BinanceFutureLotSizeResponse futureLotSize) {
        String futureLotSizeRedisKey = KeyUtility.getFutureLotSizeRedisKey(exchangeName, symbol);
        redisClientService.saveDataAsSingle(futureLotSizeRedisKey, futureLotSize);
    }

    public BinanceFutureLotSizeResponse getBinanceFutureLotSizeFilter(String symbol) {
        String lotSizeRedisKey = KeyUtility.getFutureLotSizeRedisKey(Constant.EXCHANGE_NAME_BINANCE, symbol);
        return redisClientService.getDataAsSingle(lotSizeRedisKey, BinanceFutureLotSizeResponse.class);
    }

    private boolean checkDuplicateTradeEvent(String exchangeName, String symbol, TradeEvent tradeEvent) {
        String tradeEventIdRedisKey = KeyUtility.getTradeEventIdRedisKey(exchangeName, symbol);
        List<Long> tradeEventIds = redisClientService.getDataList(tradeEventIdRedisKey, 0, -1, Long.class);
        if(tradeEventIds != null && tradeEventIds.contains(tradeEvent.getTradeId())) {
            return true;
        }
        redisClientService.saveDataAsList(tradeEventIdRedisKey, tradeEvent.getTradeId(), redisStorageMaxSize);
        return false;
    }
}
