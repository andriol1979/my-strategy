package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

@Slf4j
public class OverBoughtRule {
//    Stochastic K cắt lên > 80 (quá mua)
    public static Rule buildRule(BarSeries barSeries) {
        StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(barSeries, 14);
        Rule overBoughtRule = new CrossedUpIndicatorRule(stochasticOscillK, 75);
        LogMessage.printRuleDebugMessage(log, barSeries.getEndIndex(),
                "StochasticOscillK: " + stochasticOscillK.getValue(barSeries.getEndIndex()));
        return new LoggingRule(overBoughtRule, "OverBoughtRule", log);
    }
}
