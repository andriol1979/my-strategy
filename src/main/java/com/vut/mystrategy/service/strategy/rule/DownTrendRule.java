package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.SymbolConfig;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.trend.DownTrendIndicator;
import org.ta4j.core.rules.BooleanIndicatorRule;

@Slf4j
public class DownTrendRule {
    // So sánh taker buyer và taker seller
    // taker buy - taker sell > threshold percentage
    public static Rule buildRule(BarSeries barSeries, SymbolConfig symbolConfig) {
        DownTrendIndicator downTrendIndicator = new DownTrendIndicator(barSeries, symbolConfig.getEmaLongTermPeriod());
        Rule rule = new BooleanIndicatorRule(downTrendIndicator);

        String debugMessage = LogMessage.buildDebugMessage(downTrendIndicator, "", barSeries.getEndIndex());
        return new LoggingRule(rule, "DownTrendRule", log, debugMessage);
    }
}
