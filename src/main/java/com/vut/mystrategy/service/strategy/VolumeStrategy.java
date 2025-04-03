package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.service.strategy.rule.TakerBuySellVolumeRule;
import com.vut.mystrategy.service.strategy.rule.VolumeSlopeRule;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;

@Slf4j
@NoArgsConstructor
public class VolumeStrategy extends MyStrategyBase {

    @Override
    public Strategy buildLongStrategy(BarSeries barSeries, SymbolConfig symbolConfig) {
        //Volume tăng và volume buy > volume sell
        Rule entryRule = VolumeSlopeRule.buildRule(barSeries, 300, 100000)
                .and(TakerBuySellVolumeRule.buildRule(barSeries, true));

        Rule exitRule = VolumeSlopeRule.buildRule(barSeries, -100000, -50)
                .and(TakerBuySellVolumeRule.buildRule(barSeries, false));

        return new BaseStrategy(entryRule, exitRule);
    }

    @Override
    public Strategy buildShortStrategy(BarSeries barSeries, SymbolConfig symbolConfig) {
        return null;
    }
}
