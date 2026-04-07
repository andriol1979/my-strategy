package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.component.binance.starter.SymbolConfigManager;
import com.vut.mystrategy.helper.BarSeriesLoader;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.KlineIntervalEnum;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.service.strategy.rule.*;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.io.IOException;

//Document: https://www.binance.com/vi/square/post/15977867888993

@Slf4j
@NoArgsConstructor
public class EMACrossOverStrategy extends MyStrategyBase {

    @Override
    public Strategy buildLongStrategy(BarSeries barSeries, SymbolConfig symbolConfig) {
        if (barSeries == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(barSeries, 14);
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        Rule overSold = new CrossedUpIndicatorRule(stochasticOscillK, 20);
//                .and(new RecentBarsUnderRule(rsi, DecimalNum.valueOf(30), 5));
        // Entry rule: EMA ngắn vượt lên EMA dài
        EMAIndicator shortEma = new EMAIndicator(closePrice, symbolConfig.getEmaShortPeriod());
        EMAIndicator longEma = new EMAIndicator(closePrice, symbolConfig.getEmaLongPeriod());
        Rule crossUpRecent = new RecentBarsCrossUpRule(shortEma, longEma, 5);
        Rule entryRule = new OverIndicatorRule(closePrice, shortEma)
                .and(crossUpRecent)
                .and(overSold);
        //--------------------------------------------------------------------------------

        // Exit rule: EMA ngắn giảm xuống dưới EMA dài
//        Rule overBought = OverBoughtRule.buildRule(barSeries);
        Rule exitRuleEMA = EMACrossDownRule.buildRule(barSeries, symbolConfig);
        Rule takeProfitRule = MyTakeProfitRule.buildRule(closePrice, DecimalNum.valueOf(symbolConfig.getTargetProfit()));
        Rule exitRule = (exitRuleEMA).or(takeProfitRule);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public Strategy buildShortStrategy(BarSeries barSeries, SymbolConfig symbolConfig) {
        if (barSeries == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(barSeries, 14);
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        Rule overBought = new CrossedDownIndicatorRule(stochasticOscillK, 80);
//                .and(new RecentBarsOverRule(rsi, DecimalNum.valueOf(70), 5));

        EMAIndicator shortEma = new EMAIndicator(closePrice, symbolConfig.getEmaShortPeriod());
        EMAIndicator longEma = new EMAIndicator(closePrice, symbolConfig.getEmaLongPeriod());
        Rule crossDownRecent = new RecentBarsCrossDownRule(shortEma, longEma, 5);
        Rule entryRule = new UnderIndicatorRule(closePrice, shortEma)
                .and(crossDownRecent)
                .and(overBought);

        //------------------------------------------------------------------------------------------------

        Rule exitRuleEMA = EMACrossUpRule.buildRule(barSeries, symbolConfig);
        Rule takeProfitRule = MyTakeProfitRule.buildRule(closePrice, DecimalNum.valueOf(symbolConfig.getTargetProfit()));
        Rule exitRule = (exitRuleEMA).or(takeProfitRule);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    public static void main(String[] args) throws IOException {
        String exchangeName = Constant.EXCHANGE_NAME_BINANCE;
        String symbol = "btcusdt";
        KlineIntervalEnum intervalEnum = KlineIntervalEnum.FIFTEEN_MINUTES;
        SymbolConfigManager symbolConfigManager = new SymbolConfigManager();
        symbolConfigManager.loadSymbolConfigs();
        SymbolConfig symbolConfig = symbolConfigManager.getSymbolConfig(exchangeName, symbol);
        BarSeries series = BarSeriesLoader.loadFromDatabase(exchangeName, symbol, intervalEnum);
        Strategy strategy = new EMACrossOverStrategy().buildLongStrategy(series, symbolConfig);
        BarSeriesManager seriesManager = new BarSeriesManager(series);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        //print strategy
        LogMessage.printStrategyAnalysis(log, series, tradingRecord);
    }
}
