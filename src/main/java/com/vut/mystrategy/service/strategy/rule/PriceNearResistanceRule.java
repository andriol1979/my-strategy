package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighestValueIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.InPipeRule;

import java.math.BigDecimal;

@Slf4j
public class PriceNearResistanceRule {

    public static Rule buildRule(BarSeries barSeries, BigDecimal threshold) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);

        // Tìm swing high (kháng cự) và swing low (hỗ trợ) trong 10 nến gần nhất
        HighestValueIndicator resistanceLevel = new HighestValueIndicator(closePrice, 21);
        LogMessage.printRuleDebugMessage(log, barSeries.getEndIndex(),
                "Resistance Level: " + resistanceLevel.getValue(barSeries.getEndIndex()));
        threshold = threshold == null ? BigDecimal.valueOf(0.001) : threshold;
        Num tolerance = closePrice.numOf(threshold); // Biên độ 1%
        // Resistance - 1%
        Indicator<Num> resistanceMinusTolerance = new TransformIndicator(resistanceLevel, s -> s.minus(s.multipliedBy(tolerance)));
        // Resistance + 1%
        Indicator<Num> resistancePlusTolerance = new TransformIndicator(resistanceLevel, s -> s.plus(s.multipliedBy(tolerance)));
        Rule priceNearResistance = new InPipeRule(closePrice, resistancePlusTolerance, resistanceMinusTolerance);
        LogMessage.printRuleDebugMessage(log, barSeries.getEndIndex(),
                "ClosePrice: " + closePrice.getValue(barSeries.getEndIndex()) +
                " - ResistancePlusTolerance: " + resistancePlusTolerance.getValue(barSeries.getEndIndex()) +
                " - ResistanceMinusTolerance: " + resistanceMinusTolerance.getValue(barSeries.getEndIndex()));
        return new LoggingRule(priceNearResistance, "PriceNearResistanceRule", log);
    }
}
