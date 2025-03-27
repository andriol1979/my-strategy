package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.helper.BarSeriesLoader;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.KlineIntervalEnum;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.HMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

@Slf4j
public class MyCustomStrategy {
    public static Strategy buildStrategy(BarSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // The bias is bullish when the shorter-moving average moves above the longer
        // moving average.
        // The bias is bearish when the shorter-moving average moves below the longer
        // moving average.
        EMAIndicator shortEma = new EMAIndicator(closePrice, 9);
        EMAIndicator longEma = new EMAIndicator(closePrice, 50);
        HMAIndicator hma = new HMAIndicator(closePrice, 21);

        // Entry rule: EMA ngắn vượt lên EMA dài
        Rule entryRule = new OverIndicatorRule(shortEma, longEma)
                .and(new OverIndicatorRule(hma, longEma));
        // Exit rule: EMA ngắn giảm xuống dưới EMA dài
        Rule exitRule = new UnderIndicatorRule(shortEma, longEma)
                .and(new UnderIndicatorRule(hma, shortEma));

        return new BaseStrategy(entryRule, exitRule);
    }

    public static void main(String[] args) {
//        BarSeries series = BarSeriesLoader.loadFromCsv("backtest/1000SHIBUSDT_Binance_futures_UM_hour.csv");
        BarSeries series = BarSeriesLoader.loadFromDatabase(Constant.EXCHANGE_NAME_BINANCE, "btcusdt", KlineIntervalEnum.FIVE_MINUTES);
        Strategy strategy = buildStrategy(series);
        BarSeriesManager seriesManager = new BarSeriesManager(series);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        //print strategy
        LogMessage.printStrategyAnalysis(log, series, tradingRecord);
    }
}
