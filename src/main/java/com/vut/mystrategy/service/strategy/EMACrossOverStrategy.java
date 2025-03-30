package com.vut.mystrategy.service.strategy;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.*;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

//Document: https://www.binance.com/vi/square/post/15977867888993

@Slf4j
@NoArgsConstructor
public class EMACrossOverStrategy extends MyStrategyBase {

    @Override
    public Strategy buildLongStrategy(BarSeries barSeries) {
        if (barSeries == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);

        // The bias is bullish when the shorter-moving average moves above the longer
        // moving average.
        // The bias is bearish when the shorter-moving average moves below the longer
        // moving average.
        EMAIndicator shortEma = new EMAIndicator(closePrice, 9);
        EMAIndicator longEma = new EMAIndicator(closePrice, 21);
        StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(barSeries, 14);
        RSIIndicator rsiIndicator = new RSIIndicator(closePrice, 14);

        // Entry rule: EMA ngắn vượt lên EMA dài
        Rule entryRule = new CrossedUpIndicatorRule(shortEma, longEma)
//                .and(new UnderIndicatorRule(stochasticOscillK, 20));
                .and(new UnderIndicatorRule(rsiIndicator, 30));
        // Exit rule: EMA ngắn giảm xuống dưới EMA dài
        Rule exitRule = new CrossedDownIndicatorRule(shortEma, longEma)
//                .and(new OverIndicatorRule(stochasticOscillK, 80));
                .and(new OverIndicatorRule(rsiIndicator, 70)); // Signal 1

        return new BaseStrategy(this.getClass().getSimpleName(), entryRule, exitRule);
    }

    @Override
    public Strategy buildShortStrategy(BarSeries barSeries) {
        if (barSeries == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
        // Các chỉ báo cần thiết
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        EMAIndicator shortEma = new EMAIndicator(closePrice, 9);  // EMA ngắn hạn
        EMAIndicator longEma = new EMAIndicator(closePrice, 21);  // EMA dài hạn
        RecentSwingHighIndicator swingHigh = new RecentSwingHighIndicator(barSeries, 21); // Swing high trong 30 nến
        RecentSwingLowIndicator swingLow = new RecentSwingLowIndicator(barSeries, 21);

        // Điều kiện bán khống:
        // 1. Short EMA cắt xuống Long EMA (xu hướng giảm bắt đầu)
        // 2. Giá hiện tại nằm dưới swing high (phá vỡ kháng cự hoặc quay đầu từ đỉnh)
        Rule shortEntryRule = new UnderIndicatorRule(shortEma, longEma) // EMA crossover giảm
                .and(new UnderIndicatorRule(closePrice, swingHigh));         // Giá dưới swing high

        // Thoát short khi short EMA cắt lên long EMA (xu hướng tăng bắt đầu)
        // và giá vượt qua swing low (hỗ trợ bị phá)
        Rule shortExitRule = new CrossedUpIndicatorRule(shortEma, longEma)
                .and(new OverIndicatorRule(closePrice, swingLow));

        return new BaseStrategy(this.getClass().getSimpleName(), shortEntryRule, shortExitRule);
    }
}
