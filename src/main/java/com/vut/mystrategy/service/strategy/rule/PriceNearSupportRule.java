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

        // Tìm swing high (kháng cự) và swing low (hỗ trợ) trong 10 nến gần nhất
        LowestValueIndicator supportLevel = new LowestValueIndicator(closePrice, 21);
        LogMessage.printObjectLogMessage(log, supportLevel, "");
        threshold = threshold == null ? BigDecimal.valueOf(0.001) : threshold;
        Num tolerance = closePrice.numOf(threshold); // Biên độ 1%
        // Support - 1%
        Indicator<Num> supportMinusTolerance = new TransformIndicator(supportLevel, s -> s.minus(s.multipliedBy(tolerance)));
        // Support + 1%
        Indicator<Num> supportPlusTolerance = new TransformIndicator(supportLevel, s -> s.plus(s.multipliedBy(tolerance)));
        Rule priceNearSupport = new InPipeRule(closePrice, supportMinusTolerance, supportPlusTolerance);
        LogMessage.printObjectLogMessage(log, priceNearSupport, "PriceNearSupport");
        return priceNearSupport;
    }
}
