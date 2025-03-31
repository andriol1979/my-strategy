package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;

@Slf4j
public class EMACrossDownRule {

    // The bias is bearish when the shorter-moving average moves cross below the longer
    // moving average.
    public static Rule buildRule(BarSeries barSeries) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        EMAIndicator shortEma = new EMAIndicator(closePrice, 9);
        EMAIndicator longEma = new EMAIndicator(closePrice, 21);
        LogMessage.printRuleDebugMessage(log, barSeries.getEndIndex(),
                "ShortEMA: " + shortEma.getValue(barSeries.getEndIndex()) +
                " - LongEMA: " + longEma.getValue(barSeries.getEndIndex()));
        return new LoggingRule(new CrossedDownIndicatorRule(shortEma, longEma), "EMACrossDownRule", log);
    }
}
