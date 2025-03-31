package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.model.SymbolConfig;
import lombok.NoArgsConstructor;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

@NoArgsConstructor
public abstract class MyStrategyBase {
    public abstract Strategy buildLongStrategy(BarSeries barSeries, SymbolConfig symbolConfig);
    public abstract Strategy buildShortStrategy(BarSeries barSeries, SymbolConfig symbolConfig);
}
