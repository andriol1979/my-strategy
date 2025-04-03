package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.service.strategy.indicator.TakerBuyVolumeIndicator;
import com.vut.mystrategy.service.strategy.indicator.TakerSellVolumeIndicator;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.HMAIndicator;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

@Slf4j
public class TakerBuySellVolumeRule {
    // So sánh taker buyer và taker seller
    // isOverRule = true -> taker buyer > taker seller = true
    public static Rule buildRule(BarSeries barSeries, boolean isOverRule) {
        TakerBuyVolumeIndicator takerBuyVolumeIndicator = new TakerBuyVolumeIndicator(barSeries);
        TakerSellVolumeIndicator takerSellVolumeIndicator = new TakerSellVolumeIndicator(barSeries);
        HMAIndicator takerBuyHMA = new HMAIndicator(takerBuyVolumeIndicator, 21);
        HMAIndicator takerSellHMA = new HMAIndicator(takerSellVolumeIndicator, 21);
        Rule takerBuySellVolumeRule = isOverRule
                ? new OverIndicatorRule(takerBuyHMA, takerSellHMA)
                : new UnderIndicatorRule(takerBuyHMA, takerSellHMA);

        LogMessage.printRuleDebugMessage(log, barSeries.getEndIndex(),
                "TakerBuySellVolumeRule - TakerBuyHMA: " + takerBuyHMA.getValue(barSeries.getEndIndex()) +
                " - TakerSellHMA: " + takerSellHMA.getValue(barSeries.getEndIndex()));
        String ruleName = "TakerBuySellVolumeRule - " +
                (isOverRule ? "Over Rule" : "Under Rule");
        return new LoggingRule(takerBuySellVolumeRule, ruleName, log);
    }
}
