package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.UnderIndicatorRule;

@Slf4j
public class MACDUnderRule {

    // Sell Signal (MACD Cross Down Signal Line)
    public static Rule buildRule(BarSeries barSeries) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);

        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        EMAIndicator signalLine = new EMAIndicator(macd, 9);
        LogMessage.printRuleDebugMessage(log, barSeries.getEndIndex(),
                "ShortEMA: " + macd.getShortTermEma().getValue(barSeries.getEndIndex()) +
                        " - LongEMA: " + macd.getLongTermEma().getValue(barSeries.getEndIndex()) +
                        " - MACD: " + macd.getValue(barSeries.getEndIndex()) +
                        "SignalLineEMA: " + signalLine.getValue(barSeries.getEndIndex()));
        return new LoggingRule(new UnderIndicatorRule(macd, signalLine), "MACDUnderRule", log);
    }
}
