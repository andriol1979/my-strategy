package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.HMAIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.rules.InSlopeRule;

@Slf4j
public class VolumeSlopeRule {
    // Số dương -> volume trend tăng
    // Số âm -> volume trend giảm
    public static Rule buildRule(BarSeries barSeries, double minSlope, double maxSlope) {
        VolumeIndicator volumeIndicator = new VolumeIndicator(barSeries);
        HMAIndicator volumeHMA = new HMAIndicator(volumeIndicator, 21);

        Rule volumeUp = new InSlopeRule(volumeHMA, DecimalNum.valueOf(minSlope), DecimalNum.valueOf(maxSlope));
        LogMessage.printRuleDebugMessage(log, barSeries.getEndIndex(),
                "VolumeSlopeRule: " + volumeHMA.getValue(barSeries.getEndIndex()));
        return new LoggingRule(volumeUp, "VolumeSlopeRule", log);
    }
}
