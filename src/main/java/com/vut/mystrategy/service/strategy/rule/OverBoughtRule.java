package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.OverIndicatorRule;

@Slf4j
public class OverBoughtRule {

    public static Rule buildRule(BarSeries barSeries) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);

        StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(barSeries, 14);
        RSIIndicator rsiIndicator = new RSIIndicator(closePrice, 14);
        Rule overSoldRule = new OverIndicatorRule(stochasticOscillK, 80)
                .and(new OverIndicatorRule(rsiIndicator, 70));
        LogMessage.printObjectLogMessage(log, overSoldRule, "OverSoldRule");
        return overSoldRule;
    }
}
