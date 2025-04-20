package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.SymbolConfig;
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
    public static Rule buildRule(BarSeries barSeries, SymbolConfig symbolConfig) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        EMAIndicator shortEma = new EMAIndicator(closePrice, symbolConfig.getEmaShortPeriod());
        EMAIndicator longEma = new EMAIndicator(closePrice, symbolConfig.getEmaLongPeriod());

        String debugMessage = LogMessage.buildDebugMessage(shortEma, "ShortEMA", barSeries.getEndIndex()) +
                " - " + LogMessage.buildDebugMessage(longEma, "LongEMA", barSeries.getEndIndex());
        return new LoggingRule(new CrossedDownIndicatorRule(shortEma, longEma), "EMACrossDownRule",
                log, debugMessage);
    }
}
