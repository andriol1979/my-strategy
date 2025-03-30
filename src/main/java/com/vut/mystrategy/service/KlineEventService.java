package com.vut.mystrategy.service;

import com.vut.mystrategy.configuration.SymbolConfigManager;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.BarSeriesLoader;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.model.binance.BinanceFutureLotSizeResponse;
import com.vut.mystrategy.model.binance.KlineEvent;
import com.vut.mystrategy.service.strategy.MyStrategyBase;
import com.vut.mystrategy.service.strategy.MyStrategyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;

import java.util.Map;

@Service
@Slf4j
public class KlineEventService {

    private final SymbolConfigManager symbolConfigManager;
    private final RedisClientService redisClientService;
    private final MyStrategyManager myStrategyManager;
    private final Map<String, BarSeries> barSeriesMap;
    private final Map<String, TradingRecord> tradingRecordsdMap;
    private final Map<String, MyStrategyBase> myStrategyBaseMap;

    @Value("${warm-up-bar-size}")
    private int warmUpBarSize;

    @Autowired
    public KlineEventService(SymbolConfigManager symbolConfigManager,
                             RedisClientService redisClientService,
                             MyStrategyManager myStrategyManager,
                             @Qualifier("barSeriesMap") Map<String, BarSeries> barSeriesMap,
                             @Qualifier("tradingRecordsdMap") Map<String, TradingRecord> tradingRecordsdMap,
                             @Qualifier("myStrategyBaseMap") Map<String, MyStrategyBase> myStrategyBaseMap) {
        this.symbolConfigManager = symbolConfigManager;
        this.redisClientService = redisClientService;
        this.myStrategyManager = myStrategyManager;
        this.barSeriesMap = barSeriesMap;
        this.tradingRecordsdMap = tradingRecordsdMap;
        this.myStrategyBaseMap = myStrategyBaseMap;
    }

    // Lưu KlineEvent vào Redis List when isClosed = true
    @Async("binanceWebSocketAsync")
    public void feedKlineEvent(String myStrategyMapKey, String exchangeName, KlineEvent klineEvent) {
        if(klineEvent.getKlineData() == null || !klineEvent.getKlineData().isClosed()) {
            //make sure the close price in kline is the final price -> isClosed = true
            return;
        }
        String symbol = klineEvent.getSymbol();
        // KlineEvent always has isClosed = true
        String mapKey = KeyUtility.getBarSeriesMapKey(exchangeName, symbol, klineEvent.getKlineData().getInterval());
        //get BarSeries from bean and put new bar into barSeries
        Bar newBar = BarSeriesLoader.convertKlineEventToBar(klineEvent);
        barSeriesMap.get(mapKey).addBar(newBar);
        LogMessage.printObjectLogMessage(log, newBar, " BarSeries name: " + barSeriesMap.get(mapKey).getName());

        //Warm-up time -> DO NOT run strategy if BarSeries does not contain {warmUpBarSize} bars
        if(barSeriesMap.get(mapKey).isEmpty() || barSeriesMap.get(mapKey).getBarCount() < warmUpBarSize) {
            // In WarmUp time
            return;
        }
        //Load and run strategy
        SymbolConfig symbolConfig = symbolConfigManager.getSymbolConfig(exchangeName, symbol);
        myStrategyManager.runStrategy(barSeriesMap.get(mapKey),tradingRecordsdMap.get(mapKey),
                myStrategyBaseMap.get(myStrategyMapKey), symbolConfig);
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
