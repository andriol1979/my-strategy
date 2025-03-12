package com.vut.mystrategy.service;

import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.model.binance.BinanceFutureLotSizeResponse;
import com.vut.mystrategy.model.binance.TradeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class TradeEventService {

    private final PriceTrendingMonitor priceTrendingMonitor;
    private final RedisClientService redisClientService;
    private final Integer redisTradeEventMaxSize;

    @Autowired
    public TradeEventService(PriceTrendingMonitor priceTrendingMonitor,
                             RedisClientService redisClientService,
                             @Qualifier("redisTradeEventMaxSize") Integer redisTradeEventMaxSize) {
        this.priceTrendingMonitor = priceTrendingMonitor;
        this.redisClientService = redisClientService;
        this.redisTradeEventMaxSize = redisTradeEventMaxSize;
    }

    // Lưu TradeEvent vào Redis List
    @Async("binanceWebSocketAsync")
    public void saveTradeEvent(String exchangeName, String symbol, TradeEvent tradeEvent) {
        String tradeEventRedisKey = Utility.getTradeEventRedisKey(exchangeName, symbol);
        redisClientService.saveDataAsList(tradeEventRedisKey, tradeEvent, redisTradeEventMaxSize - 1);
        LogMessage.printInsertRedisLogMessage(log, tradeEventRedisKey, tradeEvent);

        //After save trade event success -> calculate average price and store redis
        priceTrendingMonitor.calculateAveragePrice(exchangeName, symbol);
    }

    //Get first trade event
    public Optional<TradeEvent> getNewestTradeEvent(String exchangeName, String symbol) {
        String tradeEventRedisKey = Utility.getTradeEventRedisKey(exchangeName, symbol);
        TradeEvent tradeEvent = redisClientService.getDataByIndex(tradeEventRedisKey, 0, TradeEvent.class);
        return tradeEvent == null ? Optional.empty() : Optional.of(tradeEvent);
    }

    public void saveFutureLotSize(String exchangeName, String symbol, BinanceFutureLotSizeResponse futureLotSize) {
        String futureLotSizeRedisKey = Utility.getFutureLotSizeRedisKey(exchangeName, symbol);
        redisClientService.saveDataAsSingle(futureLotSizeRedisKey, futureLotSize);
    }

    public Optional<BinanceFutureLotSizeResponse> getBinanceFutureLotSizeFilter(String symbol) {
        String lotSizeRedisKey = Utility.getFutureLotSizeRedisKey(Constant.EXCHANGE_NAME_BINANCE, symbol);
        BinanceFutureLotSizeResponse lotSizeResponse = redisClientService.getDataAsSingle(lotSizeRedisKey, BinanceFutureLotSizeResponse.class);
        return lotSizeResponse == null ? Optional.empty() : Optional.of(lotSizeResponse);
    }
}
