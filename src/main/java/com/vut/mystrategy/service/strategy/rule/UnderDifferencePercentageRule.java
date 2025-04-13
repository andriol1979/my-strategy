package com.vut.mystrategy.service.strategy.rule;

import org.ta4j.core.Indicator;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.CombineIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

/*
* ((indicatorLeft - indicatorRight) / indicatorRight * 100) <= thresholdPercentage
*/
public class UnderDifferencePercentageRule extends AbstractRule {

    private final Indicator<Num> indicatorLeft;
    private final Indicator<Num> indicatorRight;
    private final Num thresholdPercentage;

    public UnderDifferencePercentageRule(Indicator<Num> indicatorLeft, Indicator<Num> indicatorRight,
                                         Num thresholdPercentage) {
        this.indicatorLeft = indicatorLeft;
        this.indicatorRight = indicatorRight;
        this.thresholdPercentage = thresholdPercentage;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        CombineIndicator diff = CombineIndicator.minus(this.indicatorLeft, this.indicatorRight);
        Num percentageChange = diff.getValue(index).dividedBy(this.indicatorRight.getValue(index))
                .multipliedBy(diff.numOf(100));
        boolean satisfied = percentageChange.isLessThanOrEqual(this.thresholdPercentage);
        this.traceIsSatisfied(index, satisfied);
        return satisfied;
    }
}
