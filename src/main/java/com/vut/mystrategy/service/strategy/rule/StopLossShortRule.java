package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.Calculator;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighestValueIndicator;

@Slf4j
public class StopLossShortRule {

    //close price > resistance level
    public static Rule buildRule(BarSeries barSeries) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        HighestValueIndicator resistanceLevel = new HighestValueIndicator(closePrice, 9);
        Rule rule = new OverDifferencePercentageRule(closePrice, resistanceLevel, closePrice.numOf(0.1));

        return new LoggingRule(rule, "StopLossShortRule", log, Calculator.WEIGHT_NUMBER_HIGH);
    }
}
