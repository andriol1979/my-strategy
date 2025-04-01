package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.OverIndicatorRule;

@Slf4j
public class MACDOverRule {

    // Buy Signal (MACD Cross Up Signal Line)
    public static Rule buildRule(BarSeries barSeries) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        EMAIndicator signalLine = new EMAIndicator(macd, 9);
        LogMessage.printRuleDebugMessage(log, barSeries.getEndIndex(),
                "ShortEMA: " + macd.getShortTermEma().getValue(barSeries.getEndIndex()) +
                        " - LongEMA: " + macd.getLongTermEma().getValue(barSeries.getEndIndex()) +
                        " - MACD: " + macd.getValue(barSeries.getEndIndex()) +
                        "SignalLineEMA: " + signalLine.getValue(barSeries.getEndIndex()));
        return new LoggingRule(new OverIndicatorRule(macd, signalLine), "MACDOverRule", log);
    }
}
