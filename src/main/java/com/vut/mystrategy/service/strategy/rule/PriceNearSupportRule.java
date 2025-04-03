package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.LowestValueIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.InPipeRule;

import java.math.BigDecimal;

@Slf4j
public class PriceNearSupportRule {

    public static Rule buildRule(BarSeries barSeries, BigDecimal threshold) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        LowestValueIndicator supportLevel = new LowestValueIndicator(closePrice, 21);
        LogMessage.printRuleDebugMessage(log, barSeries.getEndIndex(),
                "Support Level: " + supportLevel.getValue(barSeries.getEndIndex()));
        threshold = threshold == null ? BigDecimal.valueOf(0.001) : threshold;
        Num tolerance = closePrice.numOf(threshold); // Biên độ 1%
        // Support - 1%
        Indicator<Num> supportMinusTolerance = new TransformIndicator(supportLevel, s -> s.minus(s.multipliedBy(tolerance)));
        // Support + 1%
        Indicator<Num> supportPlusTolerance = new TransformIndicator(supportLevel, s -> s.plus(s.multipliedBy(tolerance)));
        Rule priceNearSupport = new InPipeRule(closePrice, supportMinusTolerance, supportPlusTolerance);
        LogMessage.printRuleDebugMessage(log, barSeries.getEndIndex(),
                "ClosePrice: " + closePrice.getValue(barSeries.getEndIndex()) +
                " - SupportPlusTolerance: " + supportPlusTolerance.getValue(barSeries.getEndIndex()) +
                " - SupportMinusTolerance: " + supportMinusTolerance.getValue(barSeries.getEndIndex()));
        return new LoggingRule(priceNearSupport, "PriceNearSupportRule", log);
    }
}
