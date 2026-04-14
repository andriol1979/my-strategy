package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.service.strategy.rule.BuyOverSellVolumeRule;
import com.vut.mystrategy.service.strategy.rule.VolumeSlopeRule;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.NaN;

@Slf4j
@NoArgsConstructor
public class VolumeStrategy extends MyStrategyBase {

    @Override
    public Strategy buildLongStrategy(BarSeries barSeries, SymbolConfig symbolConfig) {
        //Volume tăng và volume buy > volume sell
        Rule entryRule = VolumeSlopeRule.buildRule(barSeries, DecimalNum.valueOf(5.0), NaN.NaN)
                .and(BuyOverSellVolumeRule.buildRule(barSeries, DecimalNum.valueOf(10.0)));

        Rule exitRule = VolumeSlopeRule.buildRule(barSeries, NaN.NaN, DecimalNum.valueOf(-5.0))
                .and(BuyOverSellVolumeRule.buildRule(barSeries, DecimalNum.valueOf(10.0)));

        return new BaseStrategy(entryRule, exitRule);
    }

    @Override
    public Strategy buildShortStrategy(BarSeries barSeries, SymbolConfig symbolConfig) {
        return null;
    }
}
