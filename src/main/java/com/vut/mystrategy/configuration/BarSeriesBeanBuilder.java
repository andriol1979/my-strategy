package com.vut.mystrategy.configuration;

import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.model.SymbolConfig;
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

    private void buildBarSeriesFromConfig() {
        List<SymbolConfig> symbolConfigList = symbolConfigManager.getActiveSymbolConfigsList();
        symbolConfigList.forEach(symbolConfig -> {
            symbolConfig.getFeedKlineIntervals().forEach(klineInterval -> {
                String key = KeyUtility.getBarSeriesMapKey(symbolConfig.getExchangeName(),
                        symbolConfig.getSymbol(), klineInterval);
                BarSeries series = new BaseBarSeriesBuilder()
                        .withName(key)
                        .withMaxBarCount(500)
                        .withNumTypeOf(DecimalNum.class)
                        .build();
                barSeriesMap.put(key, series);
            });
        });
    }
}
