package com.vut.mystrategy.service;

import com.vut.mystrategy.configuration.SymbolConfigManager;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.BarSeriesLoader;
import com.vut.mystrategy.model.KlineIntervalEnum;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.model.binance.BinanceFutureLotSizeResponse;
import com.vut.mystrategy.model.binance.KlineEvent;
import com.vut.mystrategy.service.strategy.EMACrossOverStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.num.DecimalNum;

import java.util.List;

@Service
@Slf4j
public class KlineEventService {

    private final SymbolConfigManager symbolConfigManager;
    private final RedisClientService redisClientService;
    private final Integer redisStorageMaxSize;

    @Autowired
    public KlineEventService(SymbolConfigManager symbolConfigManager,
                             RedisClientService redisClientService,
                             @Qualifier("redisStorageMaxSize") Integer redisStorageMaxSize) {
        this.symbolConfigManager = symbolConfigManager;
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
        //build and run strategy
        if(klineEnum.getValue().equals(symbolConfig.getSmaKlineInterval())) {
            String klineRedisKey1 = KeyUtility.getKlineRedisKey(exchangeName, symbol,
                    KlineIntervalEnum.fromValue(symbolConfig.getSmaKlineInterval()));
            List<KlineEvent> klineEvents = redisClientService.getDataList(klineRedisKey1,
                    -100, -1, KlineEvent.class);
            //Load BarSeries
            BarSeries barSeries = BarSeriesLoader.loadFromKlineEvents(klineEvents);

            // Building the trading strategy - EMACrossOver
            Strategy strategy = EMACrossOverStrategy.buildStrategy(barSeries);

            // Running the strategy
            BarSeriesManager seriesManager = new BarSeriesManager(barSeries);
            TradingRecord tradingRecord = seriesManager.run(strategy);
            LogMessage.printObjectLogMessage(log, tradingRecord);
            log.info("Number of positions for the strategy: {}", tradingRecord.getPositionCount());

            int endIndex = barSeries.getEndIndex(); // Lấy chỉ số của bar cuối cùng
            Bar endBar = barSeries.getBar(endIndex); // lấy Bar của index cuối cùng
            if (strategy.shouldEnter(endIndex)) {
                // Our strategy should enter
                log.info("Strategy should ENTER on {}", endIndex);
                boolean entered = tradingRecord.enter(endIndex, endBar.getClosePrice(), DecimalNum.valueOf(symbolConfig.getOrderVolume()));
                if (entered) {
                    Trade entry = tradingRecord.getLastEntry();
                    log.info("Entered on {} (price={}, amount={})", entry.getIndex(), entry.getNetPrice().doubleValue(), entry.getAmount().doubleValue());
                }
            }
            else if (strategy.shouldExit(endIndex)) {
                // Our strategy should exit
                log.info("Strategy should EXIT on {}", endIndex);
                boolean exited = tradingRecord.exit(endIndex, endBar.getClosePrice(), DecimalNum.valueOf(symbolConfig.getOrderVolume()));
                if (exited) {
                    Trade exit = tradingRecord.getLastExit();
                    log.info("Exited on {} (price={}, amount={})", exit.getIndex(), exit.getNetPrice().doubleValue(), exit.getAmount().doubleValue());
                }
            }

            //print strategy
            LogMessage.printStrategyAnalysis(log, barSeries, tradingRecord);
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
