package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;

@Slf4j
public class OverSoldRule {

    public static Rule buildRule(BarSeries barSeries) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);

        StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(barSeries, 21);
        RSIIndicator rsiIndicator = new RSIIndicator(closePrice, 9);
        Rule overBoughtRule = new CrossedUpIndicatorRule(stochasticOscillK, 20)
                .and(new OverIndicatorRule(rsiIndicator, 40));
        LogMessage.printRuleDebugMessage(log, barSeries.getEndIndex(),
                "StochasticOscillK: " + stochasticOscillK.getValue(barSeries.getEndIndex()) +
                        " - RsiIndicator: " + rsiIndicator.getValue(barSeries.getEndIndex()));
        return new LoggingRule(overBoughtRule, "OverSoldRule", log);
    }
}
