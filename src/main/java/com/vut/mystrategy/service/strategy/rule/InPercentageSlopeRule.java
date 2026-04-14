package com.vut.mystrategy.service.strategy.rule;

import org.ta4j.core.Indicator;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.CombineIndicator;
import org.ta4j.core.indicators.helpers.PreviousValueIndicator;
import org.ta4j.core.num.NaN;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

//Giải thích: indicator này dùng để tính 2 index (hiện tại & trước đó)
//xem sự thay đổi giữa quá khứ và hiện tại
public class InPercentageSlopeRule extends AbstractRule {
    private final Indicator<Num> ref;
    private final PreviousValueIndicator prev;
    private final Num minPercentage;
    private final Num maxPercentage;

    public InPercentageSlopeRule(Indicator<Num> ref, Num minPercentage) {
        this(ref, 1, minPercentage, NaN.NaN);
    }

    public InPercentageSlopeRule(Indicator<Num> ref, int nthPrevious, Num maxPercentage) {
        this(ref, nthPrevious, NaN.NaN, maxPercentage);
    }

    public InPercentageSlopeRule(Indicator<Num> ref, Num minPercentage, Num maxPercentage) {
        this(ref, 1, minPercentage, maxPercentage);
    }

    public InPercentageSlopeRule(Indicator<Num> ref, int nthPrevious, Num minPercentage, Num maxPercentage) {
        this.ref = ref;
        this.prev = new PreviousValueIndicator(ref, nthPrevious);
        this.minPercentage = minPercentage;
        this.maxPercentage = maxPercentage;
    }

    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        CombineIndicator diff = CombineIndicator.minus(this.ref, this.prev);
        Num percentageChange = diff.getValue(index).dividedBy(this.prev.getValue(index))
                .multipliedBy(diff.numOf(100));
        boolean minSlopeSatisfied = this.minPercentage.isNaN() || percentageChange.isGreaterThanOrEqual(this.minPercentage);
        boolean maxSlopeSatisfied = this.maxPercentage.isNaN() || percentageChange.isLessThanOrEqual(this.maxPercentage);
        boolean isNaN = this.minPercentage.isNaN() && this.maxPercentage.isNaN();
        boolean satisfied = minSlopeSatisfied && maxSlopeSatisfied && !isNaN;
        this.traceIsSatisfied(index, satisfied);
        return satisfied;
    }
}
