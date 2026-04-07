package com.vut.mystrategy.service.strategy.rule;

import org.ta4j.core.Indicator;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

public class RecentBarsCrossDownRule extends AbstractRule {
    private final Indicator<Num> up;       // thường là close
    private final Indicator<Num> low;      // thường là EMA
    private final int lookback;

    public RecentBarsCrossDownRule(Indicator<Num> up, Indicator<Num> low, int lookback) {
        this.up = up;
        this.low = low;
        this.lookback = lookback;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        int start = Math.max(1, index - lookback); // tránh index < 1
        for (int i = index; i >= start; i--) {
            Num prevUp = up.getValue(i - 1);
            Num prevLow = low.getValue(i - 1);
            Num currUp = up.getValue(i);
            Num currLow = low.getValue(i);

            if (prevUp.isGreaterThan(prevLow) && currUp.isLessThan(currLow)) {
                return true; // đã cắt xuống tại nến này
            }
        }
        return false;
    }
}
