package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.helper.BarSeriesLoader;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.KlineIntervalEnum;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.rules.*;

@Slf4j
public class BollingerBandsStrategy {
    public static Strategy buildStrategy(BarSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        final ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        final EMAIndicator avg14 = new EMAIndicator(closePrice, 14);
        final StandardDeviationIndicator sdi4 = new StandardDeviationIndicator(closePrice, 14);
        final BollingerBandsMiddleIndicator middleBBand = new BollingerBandsMiddleIndicator(avg14);
        final BollingerBandsUpperIndicator upperBBand = new BollingerBandsUpperIndicator(middleBBand, sdi4);
        final BollingerBandsLowerIndicator lowerBBand = new BollingerBandsLowerIndicator(middleBBand, sdi4);
        final SMAIndicator sma200 = new SMAIndicator(closePrice, 200);

        final Rule entryRule = new CrossedUpIndicatorRule(closePrice, lowerBBand)
                .and(new CrossedDownIndicatorRule(lowerBBand, sma200))
                .or(new CrossedUpIndicatorRule(lowerBBand, sma200));

        final Rule exitRule = new CrossedDownIndicatorRule(closePrice, upperBBand)
                .or(new StopLossRule(closePrice, 4));

        return new BaseStrategy("Bollinger Bands Breakout", entryRule, exitRule);
    }

    public static void main(String[] args) {
        BarSeries series = BarSeriesLoader.loadFromDatabase(Constant.EXCHANGE_NAME_BINANCE, "trxusdt", KlineIntervalEnum.FIVE_MINUTES);
        Strategy strategy = buildStrategy(series);
        BarSeriesManager seriesManager = new BarSeriesManager(series);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        //print strategy
        LogMessage.printStrategyAnalysis(log, series, tradingRecord);
    }
}
