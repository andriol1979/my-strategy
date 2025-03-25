package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.BarSeriesLoader;
import com.vut.mystrategy.model.KlineIntervalEnum;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

@Slf4j
public class MovingMomentumStrategy {
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
        EMAIndicator longEma = new EMAIndicator(closePrice, 21);

        StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(series, 14);

        MACDIndicator macd = new MACDIndicator(closePrice, 9, 26);
        EMAIndicator emaMacd = new EMAIndicator(macd, 18);

        // Entry rule
        Rule entryRule = new OverIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedDownIndicatorRule(stochasticOscillK, 20)) // Signal 1
                .and(new OverIndicatorRule(macd, emaMacd)); // Signal 2

        // Exit rule
        Rule exitRule = new UnderIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedUpIndicatorRule(stochasticOscillK, 80)) // Signal 1
                .and(new UnderIndicatorRule(macd, emaMacd)); // Signal 2

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
