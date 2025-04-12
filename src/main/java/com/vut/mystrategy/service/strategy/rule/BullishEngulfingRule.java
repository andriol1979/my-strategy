package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.candles.BullishEngulfingIndicator;
import org.ta4j.core.rules.BooleanIndicatorRule;

@Slf4j
public class BullishEngulfingRule {
    // So sánh taker buyer và taker seller
    // taker buy - taker sell > threshold percentage
    public static Rule buildRule(BarSeries barSeries) {
        BullishEngulfingIndicator bullishEngulfingIndicator = new BullishEngulfingIndicator(barSeries);
        Rule rule = new BooleanIndicatorRule(bullishEngulfingIndicator);

        LogMessage.printRuleDebugMessage(log, barSeries.getEndIndex(),
                "BullishEngulfingRule: " + bullishEngulfingIndicator.getValue(barSeries.getEndIndex()));
        return new LoggingRule(rule, "BullishEngulfingRule", log);
    }
}
