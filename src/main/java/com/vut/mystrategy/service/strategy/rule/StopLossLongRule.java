package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.Calculator;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.LowestValueIndicator;

@Slf4j
public class StopLossLongRule {

    //close price < support level
    public static Rule buildRule(BarSeries barSeries) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        LowestValueIndicator supportLevel = new LowestValueIndicator(closePrice, 9);
        Rule rule = new UnderDifferencePercentageRule(closePrice, supportLevel, closePrice.numOf(0.1));

        return new LoggingRule(rule, "StopLossLongRule", log, Calculator.WEIGHT_NUMBER_HIGH);
    }
}
