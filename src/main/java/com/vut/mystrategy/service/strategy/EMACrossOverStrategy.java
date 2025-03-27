package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.BarSeriesLoader;
import com.vut.mystrategy.model.KlineIntervalEnum;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

//Document: https://www.binance.com/vi/square/post/15977867888993

@Slf4j
public class EMACrossOverStrategy {
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
        RSIIndicator rsiIndicator = new RSIIndicator(closePrice, 14);

        // Entry rule: EMA ngắn vượt lên EMA dài
        Rule entryRule = new CrossedUpIndicatorRule(shortEma, longEma)
                .and(new UnderIndicatorRule(stochasticOscillK, 20))
                .and(new UnderIndicatorRule(rsiIndicator, 30));
        // Exit rule: EMA ngắn giảm xuống dưới EMA dài
        Rule exitRule = new CrossedDownIndicatorRule(shortEma, longEma)
                .and(new OverIndicatorRule(stochasticOscillK, 80))
                .and(new OverIndicatorRule(rsiIndicator, 70)); // Signal 1

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
