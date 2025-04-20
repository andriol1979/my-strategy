package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.SymbolConfig;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.OverIndicatorRule;

@Slf4j
public class EMAUpTrendRule {

    public static Rule buildRule(BarSeries barSeries, SymbolConfig symbolConfig) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        EMAIndicator shortEma = new EMAIndicator(closePrice, symbolConfig.getEmaShortPeriod());
        EMAIndicator longEma = new EMAIndicator(closePrice, symbolConfig.getEmaLongPeriod());
        EMAIndicator longTermEma = new EMAIndicator(closePrice, symbolConfig.getEmaLongTermPeriod());
        String debugMessage = LogMessage.buildDebugMessage(shortEma, "ShortEMA", barSeries.getEndIndex()) +
                " - " + LogMessage.buildDebugMessage(longEma, "LongEMA", barSeries.getEndIndex()) +
                " - " + LogMessage.buildDebugMessage(longTermEma, "LongTermEMA", barSeries.getEndIndex());
        Rule rule = new OverIndicatorRule(closePrice, shortEma)
                .and(new OverIndicatorRule(shortEma, longEma))
                .and(new OverIndicatorRule(longEma, longTermEma));
        return new LoggingRule(rule, "EMAUpTrendRule", log, debugMessage);
    }
}
