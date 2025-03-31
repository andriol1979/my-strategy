package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.service.strategy.rule.*;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.*;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;

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
//        Rule priceBreakOutRule = PriceNearResistanceRule.buildRule(barSeries, symbolConfig.getResistanceThreshold());
//        Rule entryRule = new XorRule(entryRuleEMA.and(overSold), priceBreakOutRule);
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
//        Rule priceBounceRule = PriceNearSupportRule.buildRule(barSeries, symbolConfig.getSupportThreshold());
//        Rule entryRule = new XorRule(entryRuleEMA.and(overBought), priceBounceRule);
        Rule entryRule = entryRuleEMA.and(overBought);

        //------------------------------------------------------------------------------------------------

        Rule exitRuleEMA = EMACrossUpRule.buildRule(barSeries);
        Rule stopLossRule = MyStopLossRule.buildRule(closePrice, DecimalNum.valueOf(symbolConfig.getStopLoss()));
        Rule takeProfitRule = MyTakeProfitRule.buildRule(closePrice, DecimalNum.valueOf(symbolConfig.getTargetProfit()));
        Rule exitRule = (exitRuleEMA.and(overSold)).or(stopLossRule).or(takeProfitRule);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }
}
