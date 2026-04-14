package com.vut.mystrategy.service.strategy.rule;

import org.ta4j.core.Indicator;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.ConstantIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

public class RecentBarsOverRule extends AbstractRule {
    private final Indicator<Num> first;
    private final Indicator<Num> second;
    private final int lookback;

    public RecentBarsOverRule(Indicator<Num> indicator, Num threshold, int lookback) {
        this(indicator, new ConstantIndicator<>(indicator.getBarSeries(), threshold), lookback);
    }

    public RecentBarsOverRule(Indicator<Num> first, Indicator<Num> second, int lookback) {
        this.first = first;
        this.second = second;
        this.lookback = lookback;
    }

    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        int start = Math.max(1, index - lookback); // tránh index < 1
        for (int i = index; i >= start; i--) {
            boolean satisfied = this.first.getValue(i).isGreaterThan(this.second.getValue(i));
            this.traceIsSatisfied(index, satisfied);
            if(satisfied) {
                return true;
            }
        }
        return false;
    }
}
