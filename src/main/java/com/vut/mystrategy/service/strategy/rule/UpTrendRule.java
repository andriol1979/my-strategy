package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.SymbolConfig;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.trend.UpTrendIndicator;
import org.ta4j.core.rules.BooleanIndicatorRule;

@Slf4j
public class UpTrendRule {
    // So sánh taker buyer và taker seller
    // taker buy - taker sell > threshold percentage
    public static Rule buildRule(BarSeries barSeries, SymbolConfig symbolConfig) {
        UpTrendIndicator upTrendIndicator = new UpTrendIndicator(barSeries, symbolConfig.getEmaLongTermPeriod());
        Rule rule = new BooleanIndicatorRule(upTrendIndicator);

        LogMessage.printRuleDebugMessage(log, barSeries.getEndIndex(),
                "UpTrendRule: " + upTrendIndicator.getValue(barSeries.getEndIndex()));
        return new LoggingRule(rule, "UpTrendRule", log);
    }
}
