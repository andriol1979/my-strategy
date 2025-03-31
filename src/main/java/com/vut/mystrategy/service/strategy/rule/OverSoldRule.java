package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.UnderIndicatorRule;

@Slf4j
public class OverSoldRule {

    public static Rule buildRule(BarSeries barSeries) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);

        StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(barSeries, 14);
        RSIIndicator rsiIndicator = new RSIIndicator(closePrice, 14);
        Rule overBoughtRule = new UnderIndicatorRule(stochasticOscillK, 20)
                .and(new UnderIndicatorRule(rsiIndicator, 30));
        LogMessage.printObjectLogMessage(log, overBoughtRule, "OverBoughtRule");
        return overBoughtRule;
    }
}
