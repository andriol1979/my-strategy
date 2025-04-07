package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.service.strategy.indicator.TakerBuyVolumeIndicator;
import com.vut.mystrategy.service.strategy.indicator.TakerSellVolumeIndicator;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.HMAIndicator;
import org.ta4j.core.num.Num;

@Slf4j
public class BuyUnderSellVolumeRule {
    // So sánh taker buyer và taker seller
    // taker buy - taker sell > threshold percentage
    public static Rule buildRule(BarSeries barSeries, Num thresholdPercentage) {
        TakerBuyVolumeIndicator takerBuyVolumeIndicator = new TakerBuyVolumeIndicator(barSeries);
        TakerSellVolumeIndicator takerSellVolumeIndicator = new TakerSellVolumeIndicator(barSeries);
        HMAIndicator hmaIndicatorLeft = new HMAIndicator(takerSellVolumeIndicator, 21);
        HMAIndicator hmaIndicatorRight = new HMAIndicator(takerBuyVolumeIndicator, 21);

        Rule rule = new OverDifferencePercentageRule(hmaIndicatorLeft, hmaIndicatorRight, thresholdPercentage);

        LogMessage.printRuleDebugMessage(log, barSeries.getEndIndex(),
                "BuyUnderSellVolumeRule - Left-TakerSellHMA: " + hmaIndicatorLeft.getValue(barSeries.getEndIndex()) +
                " - Right-TakerBuyHMA: " + hmaIndicatorRight.getValue(barSeries.getEndIndex()));
        return new LoggingRule(rule, "BuyUnderSellVolumeRule", log);
    }
}
