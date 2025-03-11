package com.vut.mystrategy.service;

import com.vut.mystrategy.configuration.RedisEmbeddedConfig;
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
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class TradeEventService {

    private final RedisEmbeddedConfig redisConfig;
    private final PriceTrendingMonitor priceTrendingMonitor;
    private final RedisClientService redisClientService;
    private final Integer redisTradeEventMaxSize;

    @Autowired
    public TradeEventService(RedisEmbeddedConfig redisConfig,
                             PriceTrendingMonitor priceTrendingMonitor,
                             RedisClientService redisClientService,
                             @Qualifier("redisTradeEventMaxSize") Integer redisTradeEventMaxSize) {
        this.redisConfig = redisConfig;
        this.priceTrendingMonitor = priceTrendingMonitor;
        this.redisClientService = redisClientService;
        this.redisTradeEventMaxSize = redisTradeEventMaxSize;
    }

    // Lưu TradeEvent vào Redis List
    @Async("binanceWebSocketAsync")
    public void saveTradeEvent(String exchangeName, String symbol, TradeEvent tradeEvent) {
        String tradeEventRedisKey = Utility.getTradeEventRedisKey(exchangeName, symbol);
        // Gọi Redis qua executeWithRetry
        redisConfig.executeWithRetry(() -> {
            Jedis jedis = redisConfig.getJedis();
            //save trade event
            redisClientService.saveDataAsList(jedis, tradeEventRedisKey, tradeEvent, redisTradeEventMaxSize - 1);
            log.info(LogMessage.printLogMessage("Inserted TradeEvent to Redis. Symbol: {} - Price: {}"), symbol, tradeEvent.getPrice());

            //After save trade event success -> calculate average price and store redis
            priceTrendingMonitor.calculateAveragePrice(jedis, exchangeName, symbol);
            return null;
        });
    }

    // Get TradeEvent list
    public List<TradeEvent> getTradeEvents(String exchangeName, String symbol) {
        String tradeEventRedisKey = Utility.getTradeEventRedisKey(exchangeName, symbol);
        // Dùng executeWithRetry để lấy danh sách JSON từ Redis
        return redisConfig.executeWithRetry(() -> {
            Jedis jedis = redisConfig.getJedis();
            return redisClientService.getDataList(jedis, tradeEventRedisKey, 0, -1, TradeEvent.class);
        });
    }

    //Get first trade event
    public Optional<TradeEvent> getNewestTradeEvent(String exchangeName, String symbol) {
        String tradeEventRedisKey = Utility.getTradeEventRedisKey(exchangeName, symbol);
        // Dùng executeWithRetry để lấy JSON từ Redis
        TradeEvent tradeEvent = redisConfig.executeWithRetry(() -> {
            Jedis jedis = redisConfig.getJedis();
            return redisClientService.getDataByKeyAndIndex(jedis, tradeEventRedisKey, 0, TradeEvent.class);
        });
        return tradeEvent == null ? Optional.empty() : Optional.of(tradeEvent);
    }

    public void saveFutureLotSize(String exchangeName, String symbol, BinanceFutureLotSizeResponse futureLotSize) {
        String futureLotSizeRedisKey = Utility.getFutureLotSizeRedisKey(exchangeName, symbol);
        // Gọi Redis qua executeWithRetry
        redisConfig.executeWithRetry(() -> {
            Jedis jedis = redisConfig.getJedis();
            redisClientService.saveDataAsSingle(jedis, futureLotSizeRedisKey, futureLotSize);
            return null;
        });
    }

    public Optional<BinanceFutureLotSizeResponse> getBinanceFutureLotSizeFilter(String symbol) {
        String lotSizeRedisKey = Utility.getFutureLotSizeRedisKey(Constant.EXCHANGE_NAME_BINANCE, symbol);
        BinanceFutureLotSizeResponse lotSizeResponse = redisConfig.executeWithRetry(() -> {
            Jedis jedis = redisConfig.getJedis();
            return redisClientService.getDataByKey(jedis, lotSizeRedisKey, BinanceFutureLotSizeResponse.class);
        });
        return lotSizeResponse == null ? Optional.empty() : Optional.of(lotSizeResponse);
    }
}
