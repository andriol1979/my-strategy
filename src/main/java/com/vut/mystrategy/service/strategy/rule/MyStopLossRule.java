package com.vut.mystrategy.service.strategy.rule;

import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.rules.StopLossRule;

@Slf4j
public class MyStopLossRule {

    public static Rule buildRule(ClosePriceIndicator closePrice, DecimalNum stopLoss) {
        return new LoggingRule(new StopLossRule(closePrice, stopLoss.multipliedBy(closePrice.numOf(100))), "MyStopLossRule", log);
    }
}
