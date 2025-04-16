package com.vut.mystrategy.configuration;

import com.vut.mystrategy.component.binance.starter.SymbolConfigManager;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.service.strategy.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.DecimalNum;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
public class BarSeriesBeanBuilder {
    private final SymbolConfigManager symbolConfigManager;

    private static final Map<String, BarSeries> barSeriesMap = new ConcurrentHashMap<>();
    private static final Map<String, TradingRecord> tradingRecordsdMap = new ConcurrentHashMap<>();

    private static final Map<String, MyStrategyBase> myStrategyBaseMap = new ConcurrentHashMap<>();

    public BarSeriesBeanBuilder(SymbolConfigManager symbolConfigManager) {
        this.symbolConfigManager = symbolConfigManager;
    }

    @Bean("barSeriesMap")
    public Map<String, BarSeries> barSeriesMap() {
        log.info("Loading Bar Series Map");
        buildBarSeriesFromConfig();
        log.info("Loaded total {} BarSeries into Map", barSeriesMap.size());
        return barSeriesMap;
    }

    @Bean("tradingRecordsdMap")
    public Map<String, TradingRecord> tradingRecordsdMap() {
        log.info("Loading TradingRecord Map");
        barSeriesMap().keySet().forEach(key -> {
            tradingRecordsdMap.put(key, new BaseTradingRecord());
        });
        log.info("Loaded total {} TradingRecord into Map", barSeriesMap.size());
        return tradingRecordsdMap;
    }

    @Bean("myStrategyBaseMap")
    public Map<String, MyStrategyBase> myStrategyBaseMap() {
        log.info("Loading MyStrategyBase Map");
        myStrategyBaseMap.put(EMACrossOverStrategy.class.getSimpleName(), new EMACrossOverStrategy());
        myStrategyBaseMap.put(VolumeStrategy.class.getSimpleName(), new VolumeStrategy());
        myStrategyBaseMap.put(MyCustomStrategy.class.getSimpleName(), new MyCustomStrategy());
        myStrategyBaseMap.put(ScoreThresholdStrategy.class.getSimpleName(), new ScoreThresholdStrategy());
        //Add more strategies here

        log.info("Loaded total {} MyStrategyBase into Map", myStrategyBaseMap.size());
        return myStrategyBaseMap;
    }

    private void buildBarSeriesFromConfig() {
        List<SymbolConfig> symbolConfigList = symbolConfigManager.getActiveSymbolConfigsList();
        symbolConfigList.forEach(symbolConfig -> {
            symbolConfig.getFeedKlineIntervals().forEach(klineInterval -> {
                String barSeriesMapKey = KeyUtility.getBarSeriesMapKey(symbolConfig.getExchangeName(),
                        symbolConfig.getSymbol(), klineInterval);
                BarSeries series = buildBarSeries(barSeriesMapKey);
                barSeriesMap.put(barSeriesMapKey, series);
            });
        });
    }

    public static BarSeries buildBarSeries(String barSeriesMapKey) {
        return new BaseBarSeriesBuilder()
                .withName(barSeriesMapKey)
                .withMaxBarCount(500)
                .withNumTypeOf(DecimalNum.class)
                .build();
    }
}
