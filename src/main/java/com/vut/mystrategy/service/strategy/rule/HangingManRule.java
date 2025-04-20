package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.candles.HangingManIndicator;
import org.ta4j.core.rules.BooleanIndicatorRule;

@Slf4j
public class HangingManRule {
    // So sánh taker buyer và taker seller
    // taker buy - taker sell > threshold percentage
    public static Rule buildRule(BarSeries barSeries) {
        HangingManIndicator hangingManIndicator = new HangingManIndicator(barSeries);
        Rule rule = new BooleanIndicatorRule(hangingManIndicator);

        String debugMessage = LogMessage.buildDebugMessage(hangingManIndicator, "", barSeries.getEndIndex());
        return new LoggingRule(rule, "HangingManRule", log, debugMessage);
    }
}
