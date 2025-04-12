package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.candles.BearishEngulfingIndicator;
import org.ta4j.core.rules.BooleanIndicatorRule;

@Slf4j
public class BearishEngulfingRule {
    // So sánh taker buyer và taker seller
    // taker buy - taker sell > threshold percentage
    public static Rule buildRule(BarSeries barSeries) {
        BearishEngulfingIndicator bearishEngulfingIndicator = new BearishEngulfingIndicator(barSeries);
        Rule rule = new BooleanIndicatorRule(bearishEngulfingIndicator);

        LogMessage.printRuleDebugMessage(log, barSeries.getEndIndex(),
                "BearishEngulfingRule: " + bearishEngulfingIndicator.getValue(barSeries.getEndIndex()));
        return new LoggingRule(rule, "BearishEngulfingRule", log);
    }
}
