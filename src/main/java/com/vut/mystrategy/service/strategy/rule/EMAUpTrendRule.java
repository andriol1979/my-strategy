package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.OverIndicatorRule;

@Slf4j
public class EMAUpTrendRule {

    public static Rule buildRule(BarSeries barSeries) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        EMAIndicator shortEma = new EMAIndicator(closePrice, 9);
        EMAIndicator longEma = new EMAIndicator(closePrice, 21);
        EMAIndicator longTermEma = new EMAIndicator(closePrice, 50);
        LogMessage.printRuleDebugMessage(log, barSeries.getEndIndex(),
                "ShortEMA: " + shortEma.getValue(barSeries.getEndIndex()) +
                        " - LongEMA: " + longEma.getValue(barSeries.getEndIndex()) +
                        " - LongTermEMA: " + longTermEma.getValue(barSeries.getEndIndex()));
        Rule rule = new OverIndicatorRule(closePrice, shortEma)
                .and(new OverIndicatorRule(shortEma, longEma))
                .and(new OverIndicatorRule(longEma, longTermEma));
        return new LoggingRule(rule, "EMAUpTrendRule", log);
    }
}
