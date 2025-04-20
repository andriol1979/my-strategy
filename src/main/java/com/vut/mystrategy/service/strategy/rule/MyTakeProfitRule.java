package com.vut.mystrategy.service.strategy.rule;

import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.rules.StopGainRule;

@Slf4j
public class MyTakeProfitRule {

    public static Rule buildRule(ClosePriceIndicator closePrice, DecimalNum targetProfit) {
        Rule rule = new StopGainRule(closePrice, targetProfit.multipliedBy(closePrice.numOf(100)));
        return new LoggingRule(rule, "MyTakeProfitRule", log, "");
    }
}
