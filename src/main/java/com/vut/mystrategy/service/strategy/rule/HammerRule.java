package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.candles.HammerIndicator;
import org.ta4j.core.rules.BooleanIndicatorRule;

@Slf4j
public class HammerRule {
    public static Rule buildRule(BarSeries barSeries) {
        HammerIndicator hammerIndicator = new HammerIndicator(barSeries);
        Rule rule = new BooleanIndicatorRule(hammerIndicator);

        String debugMessage = LogMessage.buildDebugMessage(hammerIndicator, "", barSeries.getEndIndex());
        return new LoggingRule(rule, "HammerRule", log, debugMessage);
    }
}
