package com.vut.mystrategy.service.strategy;

import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.*;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.*;

@Slf4j
public class MyCustomStrategy extends MyStrategyBase {
    private ClosePriceIndicator closePrice;
    private VolumeIndicator volume;
    private EMAIndicator shortEmaVolume;
    private EMAIndicator longEmaVolume;

    private EMAIndicator shortEmaClosePrice;
    private EMAIndicator longEmaClosePrice;

    private StochasticOscillatorKIndicator stochasticOscillK;

    public MyCustomStrategy(BarSeries barSeries, TradingRecord tradingRecord) {
        super(barSeries, tradingRecord);
    }

    @Override
    public Strategy buildLongStrategy(BarSeries barSeries) {
        if (barSeries == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        closePrice = new ClosePriceIndicator(barSeries);
        volume = new VolumeIndicator(barSeries);

        shortEmaVolume = new EMAIndicator(closePrice, 9);
        longEmaVolume = new EMAIndicator(closePrice, 21);

        shortEmaClosePrice = new EMAIndicator(closePrice, 9);
        longEmaClosePrice = new EMAIndicator(closePrice, 21);

        stochasticOscillK = new StochasticOscillatorKIndicator(barSeries, 14);

        // Tìm swing high (kháng cự) và swing low (hỗ trợ) trong 10 nến gần nhất
        HighestValueIndicator resistanceLevel = new HighestValueIndicator(closePrice, 21);
        LowestValueIndicator supportLevel = new LowestValueIndicator(closePrice, 21);
        log.info("Highest Indicator: {}", resistanceLevel);
        log.info("Lowest Indicator: {}", supportLevel);

        /*
         BUY - LONG Bounce
         1. Volume tăng
         2. StochasticOscillatorKIndicator: %K < 20 → Quá bán (Oversold)
            - Điều này có nghĩa là giá hiện tại đang gần mức thấp nhất trong khoảng thời gian quan sát.
            - Khi chỉ báo bắt đầu tăng trở lại trên 20, có thể xem là tín hiệu mua vào vì giá có khả năng phục hồi.
         3. Giá chạm support
         */
        Num tolerance = closePrice.numOf(0.001); // Biên độ 1%
        // Support - 1%
        Indicator<Num> supportMinusTolerance = new TransformIndicator(supportLevel, s -> s.minus(s.multipliedBy(tolerance)));
        // Support + 1%
        Indicator<Num> supportPlusTolerance = new TransformIndicator(supportLevel, s -> s.plus(s.multipliedBy(tolerance)));
        Rule priceNearSupport = new InPipeRule(closePrice, supportMinusTolerance, supportPlusTolerance);

        Rule entryRuleBounce = //new OverIndicatorRule(shortEmaVolume, longEmaVolume)
                (new CrossedUpIndicatorRule(stochasticOscillK, 20))
                .and(priceNearSupport);

        /*
         BUY - LONG Breakout
         1. Volume tăng
         2. StochasticOscillatorKIndicator: %K > 80 → Quá mua (Overbought)
            - Nghĩa là giá đang gần mức cao nhất trong khoảng thời gian quan sát.
            - Khi chỉ báo bắt đầu giảm xuống dưới 80, có thể coi là tín hiệu bán ra vì giá có khả năng giảm.
                => nếu giá không giảm xuống 80 thì coi là đà tăng tiếp diễn -> breakout
         3. Giá chạm resistance
         */

        // Resistance - 1%
        Indicator<Num> resistanceMinusTolerance = new TransformIndicator(supportLevel, s -> s.minus(s.multipliedBy(tolerance)));
        // Resistance + 1%
        Indicator<Num> resistancePlusTolerance = new TransformIndicator(supportLevel, s -> s.plus(s.multipliedBy(tolerance)));
        Rule priceNearResistance = new InPipeRule(closePrice, resistanceMinusTolerance, resistancePlusTolerance);
        Rule entryRuleBreakout = new OverIndicatorRule(shortEmaVolume, longEmaVolume)
                .and(new OverIndicatorRule(stochasticOscillK, 80))
                .and(priceNearResistance);

        //--------------------------------------------------------------------------------------------------

        /*
         EXIT - LONG
         1. Volume giảm
         2. StochasticOscillatorKIndicator: %K > 80 → Quá mua (Overbought)
            - Nghĩa là giá đang gần mức cao nhất trong khoảng thời gian quan sát.
            - Khi chỉ báo bắt đầu giảm xuống dưới 80, có thể coi là tín hiệu bán ra vì giá có khả năng giảm.
         3. EMA ngắn giảm xuống dưới EMA dài
         */
        Rule exitRule = new CrossedDownIndicatorRule(shortEmaVolume, longEmaVolume)
                .and(new CrossedDownIndicatorRule(stochasticOscillK, 80))
                .and(new CrossedDownIndicatorRule(shortEmaClosePrice, longEmaClosePrice));

        return new BaseStrategy(new OrRule(entryRuleBounce, entryRuleBreakout), exitRule);
    }

    @Override
    public Strategy buildShortStrategy() {
        return null;
    }
}
