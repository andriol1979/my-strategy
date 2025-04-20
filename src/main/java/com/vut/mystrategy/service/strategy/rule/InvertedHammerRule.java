package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.candles.InvertedHammerIndicator;
import org.ta4j.core.rules.BooleanIndicatorRule;

@Slf4j
public class InvertedHammerRule {
    public static Rule buildRule(BarSeries barSeries) {
        InvertedHammerIndicator invertedHammerIndicator = new InvertedHammerIndicator(barSeries);
        Rule rule = new BooleanIndicatorRule(invertedHammerIndicator);

        String debugMessage = LogMessage.buildDebugMessage(invertedHammerIndicator, "", barSeries.getEndIndex());
        return new LoggingRule(rule, "InvertedHammerRule", log, debugMessage);
    }
}
