package com.vut.mystrategy.service;

import com.vut.mystrategy.configuration.BarSeriesBeanBuilder;
import com.vut.mystrategy.component.binance.starter.SymbolConfigManager;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.BarSeriesLoader;
import com.vut.mystrategy.model.MyStrategyBaseBar;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.model.binance.KlineEvent;
import com.vut.mystrategy.service.strategy.MyStrategyBase;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;

@Service
@Slf4j
public class KlineEventService {

    private final SymbolConfigManager symbolConfigManager;
    private final MyStrategyManager myStrategyManager;
    private final Map<String, BarSeries> barSeriesMap;
    private final Map<String, TradingRecord> tradingRecordsdMap;
    private final Map<String, MyStrategyBase> myStrategyBaseMap;

    @Value("${warm-up-bar-size}")
    private int warmUpBarSize;

    @Autowired
    public KlineEventService(SymbolConfigManager symbolConfigManager,
                             MyStrategyManager myStrategyManager,
                             @Qualifier("barSeriesMap") Map<String, BarSeries> barSeriesMap,
                             @Qualifier("tradingRecordsdMap") Map<String, TradingRecord> tradingRecordsdMap,
                             @Qualifier("myStrategyBaseMap") Map<String, MyStrategyBase> myStrategyBaseMap) {
        this.symbolConfigManager = symbolConfigManager;
        this.myStrategyManager = myStrategyManager;
        this.barSeriesMap = barSeriesMap;
        this.tradingRecordsdMap = tradingRecordsdMap;
        this.myStrategyBaseMap = myStrategyBaseMap;
    }

    // Lưu KlineEvent vào Redis List when isClosed = true
    @Async("binanceWebSocketAsync")
    public void feedKlineEvent(String myStrategyMapKey, String exchangeName, KlineEvent klineEvent) {
        if(klineEvent.getKlineData() == null) {
            log.warn("Kline event has no data. Exchange {}", exchangeName);
            return;
        }
        String symbol = klineEvent.getSymbol();
        // KlineEvent always has isClosed = true
        String barSeriesMapKey = KeyUtility.getBarSeriesMapKey(exchangeName, symbol, klineEvent.getKlineData().getInterval());
        //get BarSeries from bean and put new bar into barSeries
        addBar(barSeriesMapKey, klineEvent);

        //Warm-up time -> DO NOT run strategy if BarSeries does not contain {warmUpBarSize} bars
        if(barSeriesMap.get(barSeriesMapKey).isEmpty() ||
                barSeriesMap.get(barSeriesMapKey).getBarCount() < warmUpBarSize) {
            // In WarmUp time
            return;
        }
        //Load and run strategy
        SymbolConfig symbolConfig = symbolConfigManager.getSymbolConfig(exchangeName, symbol);
        myStrategyMapKey = StringUtils.isEmpty(myStrategyMapKey)
                ? symbolConfig.getStrategyName() : myStrategyMapKey;
        myStrategyManager.runStrategy(barSeriesMap.get(barSeriesMapKey),tradingRecordsdMap.get(barSeriesMapKey),
                myStrategyBaseMap.get(myStrategyMapKey), symbolConfig);
    }

    private void addBar(String barSeriesMapKey, KlineEvent klineEvent) {
        // add closed Bar into bar series
        boolean replace = false;
        MyStrategyBaseBar newBar = (MyStrategyBaseBar) BarSeriesLoader.convertKlineEventToBar(klineEvent);

        if(barSeriesMap.get(barSeriesMapKey).getBarCount() > 0) {
            MyStrategyBaseBar lastBar = (MyStrategyBaseBar) barSeriesMap.get(barSeriesMapKey).getLastBar();
            replace = !lastBar.isClosed();
            //Compare eventTime between lastBar and newBar
            ZonedDateTime lastBarTime = lastBar.getEndTime();
            ZonedDateTime newBarTime = newBar.getEndTime();
            //MaxAllowGap = 2 lần bar time period
            Duration maxAllowedGap = newBar.getTimePeriod().multipliedBy(2);
            // Tính khoảng cách thời gian giữa lastBar và newBar
            Duration timeGap = Duration.between(lastBarTime, newBarTime);

            if (timeGap.compareTo(maxAllowedGap) > 0) {
                log.warn("Time gap between lastBar {} and newBar {} exceeds threshold {}. Emptying BarSeries.",
                        lastBarTime, newBarTime, maxAllowedGap);
                //New bar series to make it is empty
                barSeriesMap.remove(barSeriesMapKey);
                barSeriesMap.put(barSeriesMapKey, BarSeriesBeanBuilder.buildBarSeries(barSeriesMapKey));
                //TODO: can fetch history kline event here
            }
        }

        //Add new bar with replace or not
        barSeriesMap.get(barSeriesMapKey).addBar(newBar, replace);
        if(newBar.isClosed() && barSeriesMap.get(barSeriesMapKey).getBarCount() < warmUpBarSize) {
            log.info("BarSeries {} received {} bar(s). Continue warm up time in: {} bar(s) before running strategy",
                    barSeriesMapKey, barSeriesMap.get(barSeriesMapKey).getBarCount(), warmUpBarSize);
        }
    }
}
