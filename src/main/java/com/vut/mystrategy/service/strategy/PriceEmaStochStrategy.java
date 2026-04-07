package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.KlineIntervalEnum;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.component.binance.starter.SymbolConfigManager;
import com.vut.mystrategy.helper.BarSeriesLoader;
import com.vut.mystrategy.service.strategy.rule.MyTakeProfitRule;
import com.vut.mystrategy.service.strategy.rule.RecentBarsCrossDownRule;
import com.vut.mystrategy.service.strategy.rule.RecentBarsCrossUpRule;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.rules.*;
import org.ta4j.core.num.Num;

import java.io.IOException;

@Slf4j
@NoArgsConstructor
public class PriceEmaStochStrategy extends MyStrategyBase {

    @Override
    public Strategy buildLongStrategy(BarSeries series, SymbolConfig config) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        HighPriceIndicator highPrice = new HighPriceIndicator(series);
        LowPriceIndicator lowPrice = new LowPriceIndicator(series);

        // EMA Indicators
        EMAIndicator ema50 = new EMAIndicator(closePrice, 50);
        EMAIndicator ema100 = new EMAIndicator(closePrice, 100);

        // Stochastic Oscillator
        StochasticOscillatorKIndicator stochasticK = new StochasticOscillatorKIndicator(series, 5);
        SMAIndicator smoothK = new SMAIndicator(stochasticK, 3);

        // MACD Histogram
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        EMAIndicator macdSignal = new EMAIndicator(macd, 9);
        Indicator<Num> macdHistogram = CombineIndicator.minus(macd, macdSignal);

        // Entry Conditions
        Rule crossUpRecent = new RecentBarsCrossUpRule(closePrice, ema50, 5);
        Rule crossDownRecent = new RecentBarsCrossDownRule(closePrice, ema50, 5);
        Rule touchLowDownRecent = new RecentBarsCrossDownRule(lowPrice, ema50, 5);
        Rule touchHighUpRecent = new RecentBarsCrossUpRule(highPrice, ema50, 5);
        Rule touchUpRecent = touchLowDownRecent.or(touchHighUpRecent);

        //long_cond = ((conditionOver or conditionUnder or touch)  and src[0] >= out50 and close > out50 and  (cu) and out50 > out100 and hist>=0)
        Rule entryRule = crossUpRecent.or(touchUpRecent)
                .and(new OverIndicatorRule(ema50, ema100))
                .and(new CrossedUpIndicatorRule(smoothK, 20))
                .and(new OverIndicatorRule(macdHistogram, series.numOf(0)));
        Rule exitRule = MyTakeProfitRule.buildRule(closePrice, DecimalNum.valueOf(config.getTargetProfit()));

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public Strategy buildShortStrategy(BarSeries series, SymbolConfig config) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        HighPriceIndicator highPrice = new HighPriceIndicator(series);
        LowPriceIndicator lowPrice = new LowPriceIndicator(series);

        // EMA Indicators
        EMAIndicator ema50 = new EMAIndicator(closePrice, 50);
        EMAIndicator ema100 = new EMAIndicator(closePrice, 100);

        // Stochastic Oscillator
        StochasticOscillatorKIndicator stochasticK = new StochasticOscillatorKIndicator(series, 5);
        SMAIndicator smoothK = new SMAIndicator(stochasticK, 3);

        // MACD Histogram
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        EMAIndicator macdSignal = new EMAIndicator(macd, 9);
        Indicator<Num> macdHistogram = CombineIndicator.minus(macd, macdSignal);

        // Entry Conditions
        Rule crossUpRecent = new RecentBarsCrossUpRule(closePrice, ema50, 5);
        Rule crossDownRecent = new RecentBarsCrossDownRule(closePrice, ema50, 5);
        Rule touchLowDownRecent = new RecentBarsCrossDownRule(lowPrice, ema50, 5);
        Rule touchHighUpRecent = new RecentBarsCrossUpRule(highPrice, ema50, 5);
        Rule touchUpRecent = touchLowDownRecent.or(touchHighUpRecent);

        //long_cond = ((conditionOver or conditionUnder or touch)  and src[0] >= out50 and close > out50 and  (cu) and out50 > out100 and hist>=0)
        Rule entryRule = crossDownRecent.or(touchUpRecent)
                .and(new UnderIndicatorRule(ema50, ema100))
                .and(new CrossedDownIndicatorRule(smoothK, 80))
                .and(new UnderIndicatorRule(macdHistogram, series.numOf(0)));
        Rule exitRule = MyTakeProfitRule.buildRule(closePrice, DecimalNum.valueOf(config.getTargetProfit()));

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    public static void main(String[] args) throws IOException {
        String exchange = Constant.EXCHANGE_NAME_BINANCE;
        String symbol = "bnbusdt";
        KlineIntervalEnum interval = KlineIntervalEnum.FIVE_MINUTES;

        SymbolConfigManager symbolConfigManager = new SymbolConfigManager();
        symbolConfigManager.loadSymbolConfigs();
        SymbolConfig config = symbolConfigManager.getSymbolConfig(exchange, symbol);

        BarSeries series = BarSeriesLoader.loadFromDatabase(exchange, symbol, interval);
        Strategy strategy = new PriceEmaStochStrategy().buildLongStrategy(series, config);
        BarSeriesManager manager = new BarSeriesManager(series);
        TradingRecord tradingRecord = manager.run(strategy);

        LogMessage.printStrategyAnalysis(log, series, tradingRecord);
    }
}
