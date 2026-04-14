package com.vut.mystrategy.service.strategy.rule;

import org.ta4j.core.Indicator;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

public class RecentBarsCrossUpRule extends AbstractRule {
    private final Indicator<Num> up;       // close
    private final Indicator<Num> low;      // ema50
    private final int lookback;

    public RecentBarsCrossUpRule(Indicator<Num> up, Indicator<Num> low, int lookback) {
        this.up = up;
        this.low = low;
        this.lookback = lookback;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        int start = Math.max(1, index - lookback); // start from index-4
        for (int i = index; i >= start; i--) {
            Num prevUp = up.getValue(i - 1);
            Num prevLow = low.getValue(i - 1);
            Num currUp = up.getValue(i);
            Num currLow = low.getValue(i);

            if (prevUp.isLessThan(prevLow) && currUp.isGreaterThan(currLow)) {
                return true; // cross up happened here
            }
        }
        return false;
    }
}
