package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.configuration.SymbolConfigManager;
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
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;

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
        Rule overSold = OverSoldRule.buildRule(barSeries);
        Rule overBought = OverBoughtRule.buildRule(barSeries);

        // Entry rule: EMA ngắn vượt lên EMA dài
        Rule entryRuleEMA = EMACrossUpRule.buildRule(barSeries);
        Rule entryRule = entryRuleEMA.and(overSold);

        //--------------------------------------------------------------------------------

        // Exit rule: EMA ngắn giảm xuống dưới EMA dài
        Rule exitRuleEMA = EMACrossDownRule.buildRule(barSeries);
        Rule stopLossRule = MyStopLossRule.buildRule(closePrice, DecimalNum.valueOf(symbolConfig.getStopLoss()));
        Rule takeProfitRule = MyTakeProfitRule.buildRule(closePrice, DecimalNum.valueOf(symbolConfig.getTargetProfit()));
        Rule exitRule = (exitRuleEMA.and(overBought)).or(stopLossRule).or(takeProfitRule);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public Strategy buildShortStrategy(BarSeries barSeries, SymbolConfig symbolConfig) {
        if (barSeries == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        Rule overSold = OverSoldRule.buildRule(barSeries);
        Rule overBought = OverBoughtRule.buildRule(barSeries);

        Rule entryRuleEMA = EMACrossDownRule.buildRule(barSeries);
        Rule entryRule = entryRuleEMA.and(overBought);

        //------------------------------------------------------------------------------------------------

        Rule exitRuleEMA = EMACrossUpRule.buildRule(barSeries);
        Rule stopLossRule = MyStopLossRule.buildRule(closePrice, DecimalNum.valueOf(symbolConfig.getStopLoss()));
        Rule takeProfitRule = MyTakeProfitRule.buildRule(closePrice, DecimalNum.valueOf(symbolConfig.getTargetProfit()));
        Rule exitRule = (exitRuleEMA.and(overSold)).or(stopLossRule).or(takeProfitRule);

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
