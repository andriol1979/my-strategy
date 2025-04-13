package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.SymbolConfig;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

@Slf4j
public class EMACrossUpRule {

    // The bias is bullish when the shorter-moving average moves cross above the longer
    // moving average.
    public static Rule buildRule(BarSeries barSeries, SymbolConfig symbolConfig) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        EMAIndicator shortEma = new EMAIndicator(closePrice, symbolConfig.getEmaShortPeriod());
        EMAIndicator longEma = new EMAIndicator(closePrice, symbolConfig.getEmaLongPeriod());
        LogMessage.printRuleDebugMessage(log, barSeries.getEndIndex(),
                "ShortEMA: " + shortEma.getValue(barSeries.getEndIndex()) +
                " - LongEMA: " + longEma.getValue(barSeries.getEndIndex()));
        return new LoggingRule(new CrossedUpIndicatorRule(shortEma, longEma), "EMACrossUpRule",
                log, Calculator.WEIGHT_NUMBER_MEDIUM);
    }
}
