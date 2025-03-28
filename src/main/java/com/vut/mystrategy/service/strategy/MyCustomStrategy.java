package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.helper.BarSeriesLoader;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.KlineIntervalEnum;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.HMAIndicator;
import org.ta4j.core.indicators.RecentSwingHighIndicator;
import org.ta4j.core.indicators.RecentSwingLowIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.indicators.pivotpoints.DeMarkPivotPointIndicator;
import org.ta4j.core.indicators.pivotpoints.DeMarkReversalIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.List;

@Slf4j
public class MyCustomStrategy {
    public static Strategy buildStrategy(BarSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        // Tìm swing high (kháng cự) và swing low (hỗ trợ) trong 10 nến gần nhất
        RecentSwingHighIndicator swingHigh = new RecentSwingHighIndicator(series, 30);
        RecentSwingLowIndicator swingLow = new RecentSwingLowIndicator(series, 30);

        Num resistanceValue = swingHigh.getValue(series.getEndIndex());
        Num supportValue = swingLow.getValue(series.getEndIndex());
        System.out.println("Swing High Resistance: " + resistanceValue + ", Swing Low Support: " + supportValue);


        // Entry rule: EMA ngắn vượt lên EMA dài
        Rule entryRule = new OverIndicatorRule(closePrice, resistanceValue);
        // Exit rule: EMA ngắn giảm xuống dưới EMA dài
        Rule exitRule = new UnderIndicatorRule(closePrice, supportValue);

        return new BaseStrategy(entryRule, exitRule);
    }
}
