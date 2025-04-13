package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;

@Slf4j
public class OverSoldRule {
//    Stochastic K cắt xuống < 20 (quá bán)
    public static Rule buildRule(BarSeries barSeries) {
        StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(barSeries, 14);
        Rule overSoldRule = new CrossedDownIndicatorRule(stochasticOscillK, 25);
        LogMessage.printRuleDebugMessage(log, barSeries.getEndIndex(),
                "StochasticOscillK: " + stochasticOscillK.getValue(barSeries.getEndIndex()));
        return new LoggingRule(overSoldRule, "OverSoldRule", log);
    }
}
