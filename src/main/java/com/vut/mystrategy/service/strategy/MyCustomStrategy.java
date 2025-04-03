package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.service.strategy.rule.*;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.*;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;

@Slf4j
public class MyCustomStrategy extends MyStrategyBase {

    @Override
    public Strategy buildLongStrategy(BarSeries barSeries, SymbolConfig symbolConfig) {
        if (barSeries == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        Rule entryRule = EMACrossUpRule.buildRule(barSeries)
                .and(VolumeSlopeRule.buildRule(barSeries, 500, 100000))
                        .and(TakerBuySellVolumeRule.buildRule(barSeries, true));
        Rule exitRule = EMACrossDownRule.buildRule(barSeries)
                .and(VolumeSlopeRule.buildRule(barSeries, -100000, -200))
                        .and(TakerBuySellVolumeRule.buildRule(barSeries, false));

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

        Rule entryRule = EMACrossDownRule.buildRule(barSeries)
                .and(VolumeSlopeRule.buildRule(barSeries, 500, 100000))
                .and(TakerBuySellVolumeRule.buildRule(barSeries, false));
        Rule exitRule = EMACrossUpRule.buildRule(barSeries)
                .and(VolumeSlopeRule.buildRule(barSeries, -100000, -200))
                .and(TakerBuySellVolumeRule.buildRule(barSeries, true));
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        Rule stopLossRule = MyStopLossRule.buildRule(closePrice, DecimalNum.valueOf(symbolConfig.getStopLoss()));
        Rule takeProfitRule = MyTakeProfitRule.buildRule(closePrice, DecimalNum.valueOf(symbolConfig.getTargetProfit()));
        return new BaseStrategy(entryRule, exitRule.xor(stopLossRule).xor(takeProfitRule));
    }
}
