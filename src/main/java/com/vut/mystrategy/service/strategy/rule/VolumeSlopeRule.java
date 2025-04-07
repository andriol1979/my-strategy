package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.HMAIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.Num;

@Slf4j
public class VolumeSlopeRule {
    // Số dương -> volume trend tăng
    // Số âm -> volume trend giảm
    public static Rule buildRule(BarSeries barSeries, Num minPercentage, Num maxPercentage) {
        VolumeIndicator volumeIndicator = new VolumeIndicator(barSeries);
        HMAIndicator volumeHMA = new HMAIndicator(volumeIndicator, 21);

        Rule volumeUp = new InPercentageSlopeRule(volumeHMA, minPercentage, maxPercentage);
        LogMessage.printRuleDebugMessage(log, barSeries.getEndIndex(),
                "VolumeSlopeRule: " + volumeHMA.getValue(barSeries.getEndIndex()));
        return new LoggingRule(volumeUp, "VolumeSlopeRule", log);
    }
}
