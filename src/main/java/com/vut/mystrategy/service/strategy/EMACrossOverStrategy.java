package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.service.strategy.rule.OverBoughtRule;
import com.vut.mystrategy.service.strategy.rule.OverSoldRule;
import com.vut.mystrategy.service.strategy.rule.PriceNearResistanceRule;
import com.vut.mystrategy.service.strategy.rule.PriceNearSupportRule;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.*;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.rules.*;

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

        // The bias is bullish when the shorter-moving average moves above the longer
        // moving average.
        // The bias is bearish when the shorter-moving average moves below the longer
        // moving average.
        EMAIndicator shortEma = new EMAIndicator(closePrice, 9);
        EMAIndicator longEma = new EMAIndicator(closePrice, 21);
        Rule overSold = OverSoldRule.buildRule(barSeries);
        Rule overBought = OverBoughtRule.buildRule(barSeries);

        // Entry rule: EMA ngắn vượt lên EMA dài
        Rule entryRuleEMA = new XorRule(new OverIndicatorRule(shortEma, longEma),
                new CrossedUpIndicatorRule(shortEma, longEma));
        Rule priceBreakOutRule = PriceNearResistanceRule.buildRule(barSeries, symbolConfig.getResistanceThreshold());
        Rule entryRule = new XorRule(entryRuleEMA.and(overSold), priceBreakOutRule);

        //--------------------------------------------------------------------------------

        // Exit rule: EMA ngắn giảm xuống dưới EMA dài
        Rule exitRuleEMA = new XorRule(new UnderIndicatorRule(shortEma, longEma),
                new CrossedDownIndicatorRule(shortEma, longEma));
        Rule stopLossRule = new StopLossRule(closePrice, DecimalNum.valueOf(symbolConfig.getStopLoss()));
        Rule exitRule = exitRuleEMA.and(overBought).and(stopLossRule);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public Strategy buildShortStrategy(BarSeries barSeries, SymbolConfig symbolConfig) {
        if (barSeries == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);

        // The bias is bullish when the shorter-moving average moves above the longer
        // moving average.
        // The bias is bearish when the shorter-moving average moves below the longer
        // moving average.
        EMAIndicator shortEma = new EMAIndicator(closePrice, 9);
        EMAIndicator longEma = new EMAIndicator(closePrice, 21);
        Rule overSold = OverSoldRule.buildRule(barSeries);
        Rule overBought = OverBoughtRule.buildRule(barSeries);

        // Entry rule: EMA ngắn vượt lên EMA dài
        Rule entryRuleEMA = new XorRule(new UnderIndicatorRule(shortEma, longEma),
                new CrossedDownIndicatorRule(shortEma, longEma));
        Rule priceBounceRule = PriceNearSupportRule.buildRule(barSeries, symbolConfig.getSupportThreshold());
        Rule entryRule = new XorRule(entryRuleEMA.and(overBought), priceBounceRule);

        //------------------------------------------------------------------------------------------------

        // Exit rule: EMA ngắn giảm xuống dưới EMA dài
        Rule exitRuleEMA = new XorRule(new OverIndicatorRule(shortEma, longEma),
                new CrossedUpIndicatorRule(shortEma, longEma));
        Rule stopLossRule = new StopLossRule(closePrice, DecimalNum.valueOf(symbolConfig.getStopLoss()));
        Rule exitRule = exitRuleEMA.and(overSold).and(stopLossRule);

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }
}
