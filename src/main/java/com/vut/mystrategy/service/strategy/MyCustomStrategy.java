package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.service.strategy.rule.*;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.*;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.NaN;

@Slf4j
public class MyCustomStrategy extends MyStrategyBase {

    @Override
    public Strategy buildLongStrategy(BarSeries barSeries, SymbolConfig symbolConfig) {
        if (barSeries == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        Rule entryRule = EMAUpTrendRule.buildRule(barSeries)
                .and(VolumeSlopeRule.buildRule(barSeries, DecimalNum.valueOf(10.0), NaN.NaN))
                        .and(BuyOverSellVolumeRule.buildRule(barSeries, DecimalNum.valueOf(10.0)));
        Rule exitRule = EMACrossDownRule.buildRule(barSeries)
                .and(VolumeSlopeRule.buildRule(barSeries, NaN.NaN, DecimalNum.valueOf(-5.0)))
                        .and(BuyUnderSellVolumeRule.buildRule(barSeries, DecimalNum.valueOf(5.0)));

        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        Rule stopLossRule = MyStopLossRule.buildRule(closePrice, DecimalNum.valueOf(symbolConfig.getStopLoss()));
        Rule takeProfitRule = MyTakeProfitRule.buildRule(closePrice, DecimalNum.valueOf(symbolConfig.getTargetProfit()));
        return new BaseStrategy(entryRule, exitRule.xor(stopLossRule).xor(takeProfitRule));
    }

    @Override
    public Strategy buildShortStrategy(BarSeries barSeries, SymbolConfig symbolConfig) {
        if (barSeries == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        Rule entryRule = EMADownTrendRule.buildRule(barSeries)
                .and(VolumeSlopeRule.buildRule(barSeries, DecimalNum.valueOf(10.0), NaN.NaN))
                .and(BuyUnderSellVolumeRule.buildRule(barSeries, DecimalNum.valueOf(10.0)));
        Rule exitRule = EMACrossUpRule.buildRule(barSeries)
                .and(VolumeSlopeRule.buildRule(barSeries, NaN.NaN, DecimalNum.valueOf(-5.0)))
                .and(BuyOverSellVolumeRule.buildRule(barSeries, DecimalNum.valueOf(5.0)));
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        Rule stopLossRule = MyStopLossRule.buildRule(closePrice, DecimalNum.valueOf(symbolConfig.getStopLoss()));
        Rule takeProfitRule = MyTakeProfitRule.buildRule(closePrice, DecimalNum.valueOf(symbolConfig.getTargetProfit()));
        return new BaseStrategy(entryRule, exitRule.xor(stopLossRule).xor(takeProfitRule));
    }
}
